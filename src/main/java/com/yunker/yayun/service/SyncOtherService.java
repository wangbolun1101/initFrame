package com.yunker.yayun.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.yunker.yayun.controller.CommonController;
import com.yunker.yayun.oaPackage.WorkflowRequestTableField;
import com.yunker.yayun.util.*;
import mypackage.IDOWebService;
import mypackage.IDOWebServiceSoap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.xml.ws.Holder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 定时同步crm->oa同步数据
 */
@EnableScheduling
@Service
public class SyncOtherService extends CommonController {

    @Autowired
    private HttpClientUtil httpClientUtil;
    @Autowired
    private QueryServer queryServer;
    @Autowired
    private ValueToLabelUtil valueToLabelUtil;
    @Autowired
    private LabelToValueUtil labelToValueUtil;
    @Autowired
    private CustomApi customApi;

    //实例化接口
//    private WorkflowService workflowService = new WorkflowService();
//    private WorkflowServicePortType workflowServiceHttpPort = workflowService.getWorkflowServiceHttpPort();


    private final Long NEIBUENTITYTYPE = 1243042240053584L;//内部领用
    private final Long BEIHUOENTITYTYPE = 1243042140111260L;//分装备货
    private final long BELONGID = 1332135646396821L;

    //实例化接口
    private IDOWebService ST = new IDOWebService();
    private IDOWebServiceSoap idoWebServiceSoap = ST.getIDOWebServiceSoap();
    private String userId = "crm";//用户名
    private String pswd = "Crm123456";//密码
    private String config = "LIVE_HXSW";//账套

    /**
     * 获取token
     *
     * @return
     * @throws Exception
     */
    public String getToken() throws Exception {
        if (!this.httpClientUtil.instanceTargetToken()) {
            throw new Exception("验证Token出错！");
        }
        String token = this.httpClientUtil.getAccessToken().toString();
        if (token.contains("error_code")) {
            throw new Exception("验证Token出错！");
        }
        return token;
    }


    public void syncAccount1() {
        try {
            System.out.println("更新客户--同步开始");
            String CRMtoken = getToken();
            //                                                                       注册地址           国家                    省洲                             公司成立日期       经营渠道                    线上/线下         邮政编码   客户类型         资信额度          条款              信用冻结          最终用户类型      区域                   货币
            String sql = "select id,customItem201__c,ownerId.employeeCode,ownerId.managerId,accountName,customItem181__c,customItem195__c,fState,customItem197__c,fCity,fDistrict,customItem210__c,customItem186__c,entityType,customItem199__c,zipCode,customItem205__c,customItem204__c,customItem206__c,customItem184__c,customItem190__c,customItem156__c,level,customItem233__c,customItem207__c,customItem208__c,customItem209__c,customItem211__c,customItem212__c,customItem213__c,customItem214__c,customItem218__c from account where customItem237__c=1 and approvalStatus = 3";
            Map map = new HashMap();
            map.put("xoql", sql);
            String post = httpClientUtil.post(CRMtoken, "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
            System.out.println("同步客户--查询结果：" + post);
            JSONObject object = JSONObject.parseObject(post);
            JSONArray jsonArray = object.getJSONObject("data").getJSONArray("records");
            if (jsonArray.size() < 1) {
                return;
            }
            Map<Integer, String> ERPMap = new HashMap<>();
            Map<Long, JSONArray> ERPDataMap = new HashMap<>();
            String fieldsByBelongId = queryServer.getFieldsByBelongId(1327054207746378L);
            JSONObject object1 = JSONObject.parseObject(fieldsByBelongId);
            JSONArray fields = object1.getJSONArray("fields");
            for (int i = 0; i < fields.size(); i++) {
                JSONObject jsonObject = fields.getJSONObject(i);
                String propertyname = jsonObject.getString("propertyname");
                if ("customItem3__c".equals(propertyname)) {
                    JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                    for (int j = 0; j < selectitem.size(); j++) {
                        JSONObject jsonObject1 = selectitem.getJSONObject(j);
                        Integer value = jsonObject1.getInteger("value");
                        String lable = jsonObject1.getString("label");
                        ERPMap.put(value, lable);
                    }
                }
            }

            //查询erp账套
            String ERPSql = "select id,customItem3__c,customItem4__c,customItem2__c from customEntity57__c where customItem2__c is null and customItem5__c<>'同步完成' and customItem3__c is not null";
            String bySql1 = queryServer.getBySql(ERPSql);
            JSONArray allERP = queryServer.findAll(getToken(), bySql1, ERPSql);
            for (int i = 0; i < allERP.size(); i++) {
                JSONObject jsonObject = allERP.getJSONObject(i);
                Long customItem4__c = jsonObject.getLong("customItem4__c");
                Integer customItem3__c = jsonObject.getInteger("customItem3__c");
                String s = ERPMap.get(customItem3__c);
                jsonObject.put("customItem3__c", s);
                JSONArray jsonArray1 = ERPDataMap.get(customItem4__c);
                if (jsonArray1 == null) {
                    jsonArray1 = new JSONArray();
                }
                jsonArray1.add(jsonObject);
                ERPDataMap.put(customItem4__c, jsonArray1);
            }

            Map<Long, String> dataMap = new HashMap<>();
            String SQL = "select id from account where customItem201__c is null and approvalStatus = 3";
            String bySql = queryServer.getBySql(SQL);
            JSONArray all = queryServer.findAll(getToken(), bySql, SQL);
            for (int i = 0; i < all.size(); i++) {
                JSONObject jsonObject = all.getJSONObject(i);
                long id = jsonObject.getLongValue("id");
                dataMap.put(id, "true");
            }

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                long accountId = jsonObject.getLongValue("id");
                String status = dataMap.get(accountId);
                if (!"true".equals(status)) {
                    continue;
                }
                String EndUserType = getArrayToData(jsonObject, "customItem190__c");//最终用户类型
                if (StringUtils.isBlank(EndUserType)) {
                    continue;
                }
                long entityType = jsonObject.getLongValue("entityType");
                String accountType = entityType == 7845639L ? "F" : "T";//客户类型 原料客户 T/终端客户 F
                if (StringUtils.isBlank(accountType)) {
                    continue;
                }
                String countryType = EndUserType.contains("集团外国") ? "T" : EndUserType.contains("集团内") ? "F" : "";//集团外国内 T 集团内部部门客户  F
                String State = "";//省州
                String Country = getSplitData(jsonObject, "customItem195__c");//国家
                JSONObject excute5 = super.countryJson;
                String sCountry = excute5.getString(Country);

                String flag = "";//国内省 T 国外 F
                if ("CHN".equals(Country)) {
                    flag = "T";
                    if (entityType == 749177010471194L || entityType == 7845639L) {
                        State = getArrayToData(jsonObject, "fState");
                    } else {
                        State = getArrayToData(jsonObject, "customItem197__c");
                    }
                } else {
                    State = getArrayToData(jsonObject, "customItem197__c");
                    flag = "F";
                }
                JSONObject excute = super.provinceJson;
                State = excute.getString(State);
                JSONObject excute3 = super.countryHeadJson;
                String StateSpilt = excute3.getString(State);
                String startStr = "";//客户编码头部省份标记
                if ("T".equals(flag)) {
                    if ("T".equals(countryType)) {//集团外国内
                        if (entityType == 7845639L) {
                            continue;//todo 终端客户暂不同步
                        }
                        String firstStr = "J";//原料客户 T/终端客户 F  //"F".equals(accountType)?"H":
                        startStr = firstStr + StateSpilt;
                    } else if ("F".equals(countryType)) {
                        startStr = "N" + StateSpilt;
                    }
                } else if ("F".equals(flag)) {//国外客户
                    startStr = "K" + sCountry;
                }

                Integer CustSeq = 0; //地址编号
                String Name = getData(jsonObject, "accountName");//客户名称
                String customItem181__c = getData(jsonObject, "customItem181__c");
                String Addr_1 = "";//注册地址1
                String Addr_2 = "";//注册地址2
                String Addr_3 = "";//注册地址3
                String Addr_4 = "";//注册地址4
                if (customItem181__c.length() > 50) {
                    Addr_1 = customItem181__c.substring(0, 50);
                    if (customItem181__c.length() > 100) {
                        Addr_2 = customItem181__c.substring(50, 100);
                        if (customItem181__c.length() > 150) {
                            Addr_3 = customItem181__c.substring(100, 150);
                            Addr_4 = customItem181__c.substring(150, customItem181__c.length());
                        } else {
                            Addr_3 = customItem181__c.substring(100, customItem181__c.length());
                        }
                    } else {
                        Addr_2 = customItem181__c.substring(50, customItem181__c.length());
                    }
                } else {
                    Addr_1 = customItem181__c;
                }
                String City = getArrayToData(jsonObject, "fCity");//城市
                String County = getArrayToData(jsonObject, "fDistrict");//县
                String uf_custype = getArrayToData(jsonObject, "customItem205__c");//客户类别
                String uf_saleway = getArrayToData(jsonObject, "customItem186__c");//经营渠道
                String uf_domfor = entityType == 749179695907026L ? "境外" : entityType == 749177010471194L ? "境内" : "其他";//境内/境外
                String uf_ifonline = getArrayToData(jsonObject, "customItem199__c");//线上/线下
                String uf_org1 = getArrayToData(jsonObject, "customItem207__c");//一级机构类型
                String uf_org2 = getArrayToData(jsonObject, "customItem208__c");//二级机构类型
                String uf_org3 = getArrayToData(jsonObject, "customItem209__c");//三级机构类型
                String uf_createdate = geDateData(jsonObject, "customItem210__c");//公司成立日期
                String uf_regfund = getData(jsonObject, "customItem211__c");//注册资金
                String uf_salescale = getData(jsonObject, "customItem212__c");//经营规模
                String uf_lawsuit = getArrayToData(jsonObject, "customItem213__c");//近三年诉讼
                String uf_ifstop = getArrayToData(jsonObject, "customItem214__c");//是否停止合作
                String Zip = getData(jsonObject, "zipCode");//邮编
                String level = getArrayToData(jsonObject, "level");
                String CustType = level != null ? level.substring(0, 1) : null;//客户类型
                Float CreditLimit = getFloatData(jsonObject, "customItem204__c");//信用额度
                String TermsCode = getData(jsonObject, "customItem206__c");//条款
                JSONObject excute2 = super.provisionJson;
                TermsCode = excute2.getString(TermsCode);
                String CreditHold = jsonObject.getString("customItem184__c");//信用冻结
                String TerritoryCode = getSplitData(jsonObject, "customItem156__c");//区域

                Integer ShowInDropDownList = 1;//ERP是否显示 该字段CRM不必显示，仅同步用

                String CurrCode = getArrayToData(jsonObject, "customItem233__c");//货币

                JSONObject excute4 = super.moneyJson;
                CurrCode = excute4.getString(CurrCode);

                String bu = getArrayToData(jsonObject, "customItem218__c");//事业部编号

                JSONObject dataObject = new JSONObject();
                if ("CNY".equals(CurrCode)) {
                    dataObject.put("BankCode", "100");
                } else if ("USD".equals(CurrCode)) {
                    dataObject.put("BankCode", "141");
                } else if ("EUR".equals(CurrCode)) {
                    dataObject.put("BankCode", "171");
                } else if ("JPY".equals(CurrCode)) {
                    dataObject.put("BankCode", "151");
                }
                JSONObject excute1 = super.lastAccountTypeJson;
                EndUserType = excute1.getString(EndUserType);//最终用户类型

                dataObject.put("CustSeq", CustSeq);
                dataObject.put("Name", Name);
                dataObject.put("Addr_1", Addr_1);
                dataObject.put("Addr_2", Addr_2);
                dataObject.put("Addr_3", Addr_3);
                dataObject.put("Addr_4", Addr_4);
                dataObject.put("Country", Country);
                dataObject.put("State", State);
                dataObject.put("City", City);
                dataObject.put("County", County);
                dataObject.put("Zip", Zip);
                dataObject.put("CustType", CustType);
                dataObject.put("CreditLimit", CreditLimit);
                dataObject.put("TermsCode", TermsCode);
                dataObject.put("CreditHold", CreditHold);
                dataObject.put("EndUserType", EndUserType);
                dataObject.put("TerritoryCode", TerritoryCode);
                dataObject.put("ShowInDropDownList", ShowInDropDownList);
                dataObject.put("CusShipmentApprovalRequired", 1);
                dataObject.put("IncludeTaxInPrice", 1);
                dataObject.put("CurrCode", CurrCode);
                if ("T".equals(flag)) {//国内
                    dataObject.put("TaxCode1", "13");
                } else if ("F".equals(flag)) {//国外
                    dataObject.put("TaxCode1", "0");
                }
                dataObject.put("uf_custype", uf_custype);
                dataObject.put("uf_saleway", uf_saleway);
                dataObject.put("uf_domfor", uf_domfor);
                dataObject.put("uf_ifonline", uf_ifonline);
                dataObject.put("uf_org1", uf_org1);
                dataObject.put("uf_org2", uf_org2);
                dataObject.put("uf_org3", uf_org3);
                dataObject.put("uf_createdate", uf_createdate);
                dataObject.put("uf_regfund", uf_regfund);
                dataObject.put("uf_salescale", uf_salescale);
                dataObject.put("uf_lawsuit", uf_lawsuit);
                dataObject.put("uf_ifstop", uf_ifstop);

//                JSONArray jsonArray1 = JSONArray.parseArray("[\"CustNum\",\"CustSeq\",\"Name\",\"Addr_1\",\"Addr_2\",\"Addr_3\",\"Addr_4\",\"Country\",\"State\",\"City\",\"County\",\"Zip\",\"CustType\",\"CreditLimit\",\"TermsCode\",\"CreditHold\",\"EndUserType\",\"TerritoryCode\",\"SalesTeamID\",\"cusUf_GlobalId\",\"ShowInDropDownList\",\"CusShipmentApprovalRequired\",\"IncludeTaxInPrice\",\"CurrCode\",\"TaxCode1\"]");

                JSONArray ERPDataArray = ERPDataMap.get(accountId);
                Boolean updateStatus = true;
                for (int j = 0; j < ERPDataArray.size(); j++) {
                    JSONObject ERPDataObject = ERPDataArray.getJSONObject(j);
                    String ERPConfig = ERPDataObject.getString("customItem3__c");
                    String CustNum = jsonObject.getString("customItem2__c");
                    Long ERPDataId = ERPDataObject.getLong("id");
                    //调用接口,获取Sessiontoken
                    String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, ERPConfig);
                    StringBuilder stringBuilder = new StringBuilder();
                    Set<Map.Entry<String, Object>> entries = dataObject.entrySet();
                    entries.iterator().forEachRemaining(entriesMap -> {
                        String key = entriesMap.getKey();
                        if (stringBuilder.length() == 0) {
                            stringBuilder.append(key);
                        } else {
                            stringBuilder.append("," + key);
                        }
                    });
                    String result = idoWebServiceSoap.loadJson(ERPtoken, "SLCustomers", stringBuilder.toString(), " CustNum = '" + CustNum + "'", "", "", -1);
                    JSONObject resultJson = JSONObject.parseObject(result);
                    JSONArray Items = resultJson.getJSONArray("Items");
                    if (Items.size() < 1) {
                        continue;
                    }
                    JSONObject jsonObject1 = Items.getJSONObject(0);
                    String ID = jsonObject1.getString("ID");


                    JSONArray allItems = getAllItems(dataObject);
                    JSONArray propertyList = getPropertyList(dataObject);


                    String slCustomers = updateData(ERPtoken, "SLCustomers", allItems, propertyList, ID);//更新客户
                    //更新CRM客户编号
                    if (slCustomers != null) {
                        updateStatus = false;
                    }

                }

                if (updateStatus) {
                    updateCustomerEditStatus(accountId);
                }

                System.out.println("更新客户--同步结束");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
    }

    /**
     * 根据合同创建订单
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void createOrder() {
        try {
            StringBuilder sb = new StringBuilder();
            JSONArray contracts = getContracts();
            for (int i = 0; i < contracts.size(); i++) {
                JSONObject jsonObject = contracts.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                if (sb.length() == 0) {
                    sb.append(id);
                } else {
                    sb.append("," + id);
                }
            }
//            String fieldsByBelongId = queryServer.getFieldsByBelongId(1L);
            String fieldsByBelongId_order = queryServer.getFieldsByBelongId(35L);
            Map<Long, JSONArray> contractInfo = getContractInfo(sb.toString());
            Map<Long, String> exisitOrderMap = getExisitOrder(sb.toString());
            for (int i = 0; i < contracts.size(); i++) {
                JSONObject contractJSON = new JSONObject();
                JSONObject jsonObject = contracts.getJSONObject(i);
                //comment,customItem148__c,customItem166__c,customItem151__c,accountId,customItem206__c,customItem207__c,customItem208__c,customItem209__c
                Long contractId = jsonObject.getLong("id");
                Long entityType = jsonObject.getLong("entityType");
                Long ownerId = jsonObject.getLong("ownerId");
                String comment = jsonObject.getString("comment__c");//备注
                Long customItem148__c = jsonObject.getLong("customItem148__c__c");//价格表
                Long customItem166__c = jsonObject.getLong("customItem166__c__c");//收货地址
                String customItem89__c = jsonObject.getString("customItem89__c");//ERP账套
                String customItem179__c = labelToValueUtil.replace(fieldsByBelongId_order, "customItem179__c", customItem89__c, "selectitem");
                Long accountId = jsonObject.getLong("accountId__c");//客户
                Long customItem206__c = jsonObject.getLong("customItem206__c");//收票地址
//                Long customItem207__c = jsonObject.getLong("customItem207__c");//信用额度与账期
                Integer customItem74__c = jsonObject.getInteger("customItem74__c");//发货地
                Integer customItem209__c = jsonObject.getInteger("customItem209__c");//票据类型
//                Integer status = jsonObject.getInteger("status");//状态

                String custnum = jsonObject.getString("customItem215__c");//客户编号
                String customItem211__c = jsonObject.getString("customItem211__c");//条款
                String term =customItem211__c;
//                        provisionJson.getString(customItem211__c);
                Double uf_overlimit3 = jsonObject.getDouble("customItem205__c");//总金额
                String bu = jsonObject.getString("customItem212__c");//事业部

                Long customItem93__c = jsonObject.getLong("customItem93__c");//事业部
                Long customItem94__c = jsonObject.getLong("customItem94__c");//账套事业部账期


//                Integer customItem212__c = jsonObject.getInteger("customItem212__c");//事业部
//                String replace = valueToLabelUtil.replace(fieldsByBelongId, "customItem218__c", customItem212__c + "", "selectitem");
//                String bu = getArrayToData(replace);
                String s = exisitOrderMap.get(contractId);
                if (StringUtils.isNotBlank(s)) {
                    contractJSON.put("id", contractId);
                    contractJSON.put("customItem204__c", true);
                    queryServer.updateCustomizeById(contractJSON);
                    continue;
                }
                contractJSON.put("id", contractId);
//                if (status==null||status.intValue()!=2){
//                    contractJSON.put("status", 2);
//                }

                JSONObject orderJSON = new JSONObject();
                orderJSON.put("entityType", 7845644L);
                orderJSON.put("ownerId", ownerId);
                orderJSON.put("accountId", accountId);
                orderJSON.put("customItem209__c", contractId);
                orderJSON.put("priceId", customItem148__c);
                orderJSON.put("comment", comment);
                orderJSON.put("customItem176__c", customItem166__c);
                orderJSON.put("customItem177__c", customItem206__c);
//                orderJSON.put("customItem178__c", customItem207__c);
                orderJSON.put("customItem179__c", customItem179__c);
                orderJSON.put("customItem180__c", customItem74__c);
                orderJSON.put("customItem193__c", customItem209__c);
                orderJSON.put("customItem203__c", customItem206__c);
                orderJSON.put("customItem204__c", customItem206__c);
                orderJSON.put("customItem217__c",customItem94__c);
                orderJSON.put("customItem216__c",customItem93__c);

                Long orderId = queryServer.createOrder(orderJSON);
                if (orderId != null && orderId != 0) {
                    JSONArray orderDetailArray = new JSONArray();
                    //customItem1__c,customItem3__c,customItem6__c,customItem13__c,customItem15__c,customItem6__c
                    JSONArray jsonArray = contractInfo.get(contractId);
                    if (jsonArray == null) {
                        jsonArray = new JSONArray();
                    }
                    for (int j = 0; j < jsonArray.size(); j++) {
                        JSONObject jsonObject1 = jsonArray.getJSONObject(j);
                        Long customItem3__c = jsonObject1.getLong("customItem3__c");//价格表明细
                        Double customItem6__c = jsonObject1.getDouble("customItem6__c");//价格表价格
                        Double customItem14__c = jsonObject1.getDouble("customItem14__c");//总价
                        Double customItem13__c = jsonObject1.getDouble("customItem13__c");//数量
                        String customItem15__c = jsonObject1.getString("customItem24__c");//备注
                        Long productId = jsonObject1.getLong("productId");
                        JSONObject orderDetailJSON = new JSONObject();
                        orderDetailJSON.put("entityType", BEIHUOENTITYTYPE);
                        orderDetailJSON.put("orderId", orderId);
                        orderDetailJSON.put("priceBookEntryId", customItem3__c);
                        orderDetailJSON.put("quantity", customItem13__c);
                        orderDetailJSON.put("productId", productId);
                        orderDetailJSON.put("comment", customItem15__c);
                        orderDetailJSON.put("unitPrice", customItem6__c);
                        orderDetailJSON.put("ownerId", ownerId);
//                        orderDetailArray.add(orderDetailJSON);
                        Long orderDeatil = queryServer.createOrderTeatil(orderDetailJSON);
                    }
                    contractJSON.put("customItem204__c", true);
                }
                if (contractJSON.containsKey("customItem204__c") || contractJSON.containsKey("status")) {
                    queryServer.updateCustomizeById(contractJSON);
                }
                ////////////更新审批状态/////////////
                //获取流程id
                Long flowId = getFlowId(contractId);
                if (flowId != null && flowId != 0) {
                    agreeApprove(flowId);
                }

                String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
                Holder<String> pa = new Holder<>("<Parameters><Parameter>" + custnum + "</Parameter>" +
                        "<Parameter>" + bu + "</Parameter>" +
                        "<Parameter>" + term + "</Parameter>" +
                        "<Parameter>" + uf_overlimit3 + "</Parameter>" +
                        "<Parameter ByRef=\"Y\"></Parameter></Parameters>");
                String customNum = getCustomNum(ERPtoken, pa, "hxsp_calc_ecocredit_crm");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void agreeApprove(Long approvalId) throws Exception {
        String url = "https://api.xiaoshouyi.com/data/v1/objects/approval/agree";
        JSONObject object = new JSONObject();
        object.put("approvalId", approvalId);
        object.put("approvalUserId", 0);
        object.put("comments", "OA审批通过后，自动审批通过");
        String post = httpClientUtil.post(getToken(), url, object.toJSONString());
        System.out.println("OA审批通过后，自动审批通过 ========> " + post);
    }

    private Long getFlowId(Long contractId) throws Exception {
        int pageNo = 1;
        int pageSize = 25;
        String url = "https://api.xiaoshouyi.com/data/v1/objects/approval/approvals?pageNo=" + pageNo + "&pageSize=" + pageSize + "&userId=743527959707865";
        Long id = null;
        String s = httpClientUtil.get(getToken(), url, null);
        JSONObject object = JSONObject.parseObject(s);
        Integer totalSize = object.getInteger("totalSize");
        JSONArray records = object.getJSONArray("records");
        if (totalSize > pageSize) {
            while (pageNo * pageSize < totalSize) {
                pageNo++;
                //获取符合条件的数组
                String result = httpClientUtil.get(getToken(), url, null);
                JSONArray jsonArray = JSONObject.parseObject(result).getJSONArray("records");
                records.addAll(jsonArray);
            }
        }
        for (int i = 0; i < records.size(); i++) {
            JSONObject jsonObject = records.getJSONObject(i);
            Long belongId = jsonObject.getLong("belongId");
            Long dataId = jsonObject.getLong("dataId");
            Long folwId = jsonObject.getLong("id");
            if (belongId.longValue() != BELONGID) {
                continue;
            }
            if (dataId.longValue() != contractId.longValue()) {
                continue;
            } else {
                id = folwId;
            }

        }
        return id;
    }

    private Map<Long, String> getExisitOrder(String ids) throws Exception {
        Map<Long, String> map = new HashMap<>();
        if (StringUtils.isBlank(ids)) {
            return map;
        }
        String sqlPrefix = "select id,customItem209__c from _order where customItem209__c in(";
        String sqlsuffix = ")";
        JSONArray dataMoreIds = getDataMoreIds(ids, sqlPrefix, sqlsuffix);
        for (int i = 0; i < dataMoreIds.size(); i++) {
            JSONObject jsonObject = dataMoreIds.getJSONObject(i);
            Long contractId = jsonObject.getLong("customItem209__c");
            map.put(contractId, "success");
        }
        return map;
    }

    private Map<Long, JSONArray> getContractInfo(String ids) throws Exception {
        Map<Long, JSONArray> contractInfoMap = new HashMap<>();
        if (StringUtils.isBlank(ids)) {
            return contractInfoMap;
        }
        Map<Long, Long> productIdMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        String sqlPrefix = "select customItem25__c,customItem3__c,customItem6__c,customItem13__c,customItem24__c,customItem14__c from customEntity54__c where customItem25__c in(";
        String sqlsuffix = ")";
        JSONArray dataMoreIds = getDataMoreIds(ids, sqlPrefix, sqlsuffix);
        for (int i = 0; i < dataMoreIds.size(); i++) {
            JSONObject jsonObject = dataMoreIds.getJSONObject(i);
            Long priceBookEntryId = jsonObject.getLong("customItem3__c");
            if (sb.length() == 0) {
                sb.append(priceBookEntryId + "");
            } else {
                sb.append("," + priceBookEntryId);
            }
        }
        String string = sb.toString();
        if (StringUtils.isNotBlank(string)) {
            sqlPrefix = "select id,productId from priceBookEntry where id in(";
            sqlsuffix = ")";
            JSONArray productIds = getDataMoreIds(string, sqlPrefix, sqlsuffix);
            for (int i = 0; i < productIds.size(); i++) {
                JSONObject jsonObject = productIds.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                Long productId = jsonObject.getLong("productId");
                productIdMap.put(id, productId);
            }
        }
        for (int i = 0; i < dataMoreIds.size(); i++) {
            JSONObject jsonObject = dataMoreIds.getJSONObject(i);
            Long priceBookEntryId = jsonObject.getLong("customItem3__c");
            Long contractId = jsonObject.getLong("customItem25__c");
            Long productId = productIdMap.get(priceBookEntryId);
            jsonObject.put("productId", productId);
            JSONArray jsonArray = contractInfoMap.get(contractId);
            if (jsonArray == null) {
                jsonArray = new JSONArray();
            }
            jsonArray.add(jsonObject);
            contractInfoMap.put(contractId, jsonArray);
        }
        return contractInfoMap;
    }

    private JSONArray getContracts() throws Exception {
        String sql = "select id,ownerId,entityType,comment__c,customItem148__c__c,customItem166__c__c,accountId__c,customItem206__c,customItem74__c,customItem209__c,customItem211__c,customItem205__c,customItem89__c,customItem215__c,customItem212__c from customEntity62__c where customItem47__c='归档' and customItem202__c__c=0 and (customItem204__c<>1 or customItem204__c is null)";
        String bySql = queryServer.getBySql(sql);
        JSONArray all = queryServer.findAll(getToken(), bySql, sql);
        return all;
    }

    @Scheduled(cron = "0/5 * * * * ?")
    public void syncOrder() {
        try {
            String fieldsByBelongId_order = queryServer.getFieldsByBelongId(35L);
            String fieldsByBelongId_account = queryServer.getFieldsByBelongId(1L);
            StringBuilder sb = new StringBuilder();

            JSONArray errorArray = new JSONArray();
            JSONArray detailArray = new JSONArray();
            //language=SQL
            String sql = "select id,customItem192__c,customItem212__c,customItem213__c,customItem214__c,createdAt,customItem182__c,customItem186__c,customItem210__c,TermsCode__c,customItem193__c,comment,customItem194__c,customItem211__c,customItem196__c,customItem197__c,customItem198__c,customItem199__c,customItem202__c,customItem200__c,customItem218__c,customItem191__c from _order where (customItem201__c<>'同步成功' or customItem201__c is null or customItem201__c='') and poStatus=2 AND createdAt>1594051200000";
            String bySql = queryServer.getBySql(sql);
            JSONArray all = queryServer.findAll(getToken(), bySql, sql);
            if (all.size()==0){
                System.out.println("无数据，不进行处理");
                return;
            }
            for (int i = 0; i < all.size(); i++) {
                JSONObject jsonObject = all.getJSONObject(i);
                Long orderId = jsonObject.getLong("id");//订单id
                if (sb.length() == 0) {
                    sb.append(orderId + "");
                } else {
                    sb.append("," + orderId);
                }
            }
            System.out.println("订单ID"+sb.toString());
            Map<Long, JSONArray> orderProductInfo = getOrderProductInfo(sb.toString());
            for (int i = 0; i < all.size(); i++) {
                JSONObject jsonObject = all.getJSONObject(i);
                Long orderId = jsonObject.getLong("id");//订单id
                Integer moneyType = jsonObject.getInteger("customItem182__c");
                String replace_moneyType = valueToLabelUtil.replace(fieldsByBelongId_account, "customItem233__c", moneyType + "", "selectitem");
                String money = moneyJson.getString(replace_moneyType);
                money = StringUtils.isBlank(money) ? "CNY" : money;


                String customItem192__c = jsonObject.getString("customItem192__c");//采购合同
                String createdAt = jsonObject.getString("createdAt");//创建日期
                createdAt = createdAt.substring(0, 10);
                String customItem186__c = jsonObject.getString("customItem186__c");//客户编号
                if (StringUtils.isBlank(customItem186__c)) {
                    errorArray = addToError(errorArray, "客户编号为空", orderId);
                    continue;
                }
                String customItem187__c = jsonObject.getString("customItem210__c");//erp_id（收货地址）
//                if (StringUtils.isBlank(customItem187__c)) {
//                    errorArray=addToError(errorArray,"收货地址id为空",orderId);
//                    continue;
//                }
                String TermsCodeData = jsonObject.getString("TermsCode__c");//条款
                Integer customItem193__c = jsonObject.getInteger("customItem193__c");//票据类型
                String replace_coUf_NoteType = valueToLabelUtil.replace(fieldsByBelongId_order, "customItem193__c", customItem193__c + "", "selectitem");
                if (StringUtils.isBlank(replace_coUf_NoteType)) {
                    errorArray = addToError(errorArray, "未匹配到票据类型", orderId);
                    continue;
                }
                String comment = jsonObject.getString("comment");//备注

                Integer customItem194__c = jsonObject.getInteger("customItem194__c");//订单类别
                String replace_TakenBy = valueToLabelUtil.replace(fieldsByBelongId_order, "customItem194__c", customItem194__c + "", "selectitem");
                if (StringUtils.isBlank(replace_TakenBy)) {
                    errorArray = addToError(errorArray, "未匹配到订单类别", orderId);
                    continue;
                }
                String customItem195__c = jsonObject.getString("customItem211__c");//销售员
                if (StringUtils.isBlank(customItem195__c)) {
                    errorArray = addToError(errorArray, "销售员为空", orderId);
                    continue;
                }
                String customItem196__c = jsonObject.getString("customItem196__c");//部门
                if (StringUtils.isBlank(customItem196__c)) {
                    errorArray = addToError(errorArray, "部门为空", orderId);
                    continue;
                }
                String customItem197__c = jsonObject.getString("customItem197__c"); //需要发货批准
                int customItem197__c_int = StringUtils.isBlank(customItem197__c) ? 1 : Integer.valueOf(customItem197__c);
                String customItem198__c = jsonObject.getString("customItem198__c"); //提前发货
                int customItem198__c_int = StringUtils.isBlank(customItem198__c) ? 1 : Integer.valueOf(customItem198__c);
                String customItem199__c = jsonObject.getString("customItem199__c"); //部分发货
                int customItem199__c_int = StringUtils.isBlank(customItem199__c) ? 1 : Integer.valueOf(customItem199__c);
                String customItem202__c = jsonObject.getString("customItem202__c"); //部分发货
                int customItem202__c_int = StringUtils.isBlank(customItem202__c) ? 0 : Integer.valueOf(customItem202__c);

                String customItem200__c = jsonObject.getString("customItem200__c");//税码
                if (StringUtils.isBlank(customItem200__c)) {
                    errorArray = addToError(errorArray, "税码为空", orderId);
                    continue;
                }
                String customItem218__c = jsonObject.getString("customItem218__c");//送票地址
//                if (StringUtils.isBlank(customItem190__c)){
//                    errorArray=addToError(errorArray,"送票地址id为空",orderId);
//                    continue;
//                }
                Integer customItem191__c = jsonObject.getInteger("customItem191__c");//最终用户类型
                String customItem212__c = jsonObject.getString("customItem212__c");//订单站点
                String customItem214__c = jsonObject.getString("customItem214__c");//订单站点
                String customItem213__c = jsonObject.getString("customItem213__c");//账套（合同）

                String replace_EndUserType = valueToLabelUtil.replace(fieldsByBelongId_account, "customItem190__c", customItem191__c + "", "selectitem");
                if (StringUtils.isBlank(replace_EndUserType)) {
                    errorArray = addToError(errorArray, "最终用户类型为空", orderId);
                    continue;
                }
                JSONObject lastAccountTypeJson = super.lastAccountTypeJson;//最终用户类型数据
                String EndUserType_erp = lastAccountTypeJson.getString(replace_EndUserType);
                if (StringUtils.isBlank(EndUserType_erp)) {
                    errorArray = addToError(errorArray, "未匹配到最终用户类型码值", orderId);
                    continue;
                }


                //调用接口,获取Sessiontoken
                String ERPtoken = idoWebServiceSoap.createSessionToken("crm", "Crm123456", customItem213__c);

                String string = coNumJson.getString(customItem213__c);
                if (StringUtils.isBlank(string)) {
                    errorArray = addToError(errorArray, "未匹配到订单编号前缀", orderId);
                    continue;
                }
                Holder<String> pa = new Holder<>("<Parameters><Parameter>" + string + "</Parameter><Parameter ByRef=\"Y\"></Parameter></Parameters>");
                String hxGenCoNum = getCustomNum(ERPtoken, pa, "HXGenCoNum");
                int indexStart = hxGenCoNum.indexOf("</Parameter><Parameter ByRef=\"Y\">") + "</Parameter><Parameter ByRef=\"Y\">".length();
                int indexEnd = hxGenCoNum.indexOf("</Parameter></Parameters>");
                String CoNum = hxGenCoNum.substring(indexStart, indexEnd);//客户编号
//                CoNum="SF20013179";   // todo 订单编号 调用接口查询
                String CustPo = StringUtils.isBlank(customItem192__c) ? "无" : customItem192__c;   //采购合同
                String OrderDate = createdAt;   //订单日期
                String Stat = "O";   //状态 默认O
                String Type = "R";   //订单类型 默认R
                String CustNum = customItem186__c;   //客户编号
                String CustSeq = customItem187__c;   //收货地
                String TermsCode = TermsCodeData;   //条款
                String coUf_NoteType = replace_coUf_NoteType;   //票据类型
                String coUf_Note = comment;   //备注
                String TakenBy = replace_TakenBy;   //订单类别
                String Slsman = customItem195__c;   //销售员
                String coUf_dept = customItem196__c;   //部门
                int CoShipmentApprovalRequired = customItem197__c_int;   //需要发货批准
                int ShipEarly = customItem198__c_int;   //提前发货
                int ShipPartial = customItem199__c_int;   //部分发货
                int CreditHold = customItem202__c_int;   //信用冻结
                String TaxCode1 = customItem200__c;   //税码
                String DeliverTo = customItem218__c;   //送货单地址
                String InvoiceTo = customItem218__c;   //发票邮寄地址
                String EndUserType = EndUserType_erp;   //最终用户类型

                JSONObject dataObject = new JSONObject();
                dataObject.put("CoNum", CoNum);
                dataObject.put("CustPo", CustPo);
                dataObject.put("OrderDate", OrderDate);
                dataObject.put("Stat", Stat);
                dataObject.put("Type", Type);
                dataObject.put("CustNum", CustNum);
                dataObject.put("CustSeq", CustSeq);
                dataObject.put("TermsCode", TermsCode);
                dataObject.put("coUf_NoteType", coUf_NoteType);
                dataObject.put("coUf_Note", coUf_Note);
                dataObject.put("TakenBy", TakenBy);
                dataObject.put("Slsman", Slsman);
                dataObject.put("coUf_dept", coUf_dept);
                dataObject.put("CoShipmentApprovalRequired", CoShipmentApprovalRequired);
                dataObject.put("ShipEarly", ShipEarly);
                dataObject.put("ShipPartial", ShipPartial);
                dataObject.put("CreditHold", CreditHold);
                dataObject.put("TaxCode1", TaxCode1);
//                dataObject.put("DeliverTo", DeliverTo);
//                dataObject.put("InvoiceTo", InvoiceTo);
                dataObject.put("DeliverTo", 0);
                dataObject.put("InvoiceTo", 0);
                dataObject.put("EndUserType", EndUserType);
                System.out.println(JSONObject.toJSONString(dataObject, SerializerFeature.WriteMapNullValue));
                JSONArray allItems = getAllItems(dataObject);
                JSONArray propertyList = getPropertyList(dataObject);
                String slCustomers = addData(ERPtoken, "SLCos", allItems, propertyList);//同步客户
//                String slCustomers = null;//同步客户
                if (slCustomers == null) {
                    JSONObject object = new JSONObject();
                    object.put("id", orderId);
                    object.put("customItem201__c", "同步成功");
                    object.put("po", CoNum);
                    String post = queryServer.updateOrder(object);
                    /////////////同步订单明细///////////
                    JSONArray jsonArray = orderProductInfo.get(orderId);
                    for (int j = 0; j < jsonArray.size(); j++) {
                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                        Long id = jsonObject1.getLong("id");
                        String customItem182__c = jsonObject1.getString("customItem182__c");//客户编号
                        String customItem183__c = jsonObject1.getString("customItem183__c");//物料
                        Double unitPrice = jsonObject1.getDouble("unitPrice");//单价
                        Integer quantity = jsonObject1.getInteger("quantity");//数量
                        Double discount = jsonObject1.getDouble("discount");//折扣
                        String customItem184__c = jsonObject1.getString("customItem184__c");//规格
                        String customItem185__c = jsonObject1.getString("customItem185__c");//规格
                        int customItem185__c_int = StringUtils.isBlank(customItem185__c) ? 0 : Integer.valueOf(customItem185__c);
                        Double priceTotal = jsonObject1.getDouble("priceTotal");//总价格
                        String customItem189__c = jsonObject1.getString("customItem189__c");//备注
//                        Double customItem186__c1 = jsonObject1.getDouble("customItem186__c");//内容量
//                        String customItem187__c_OrderProduct = jsonObject1.getString("customItem187__c");//包装模板
//                        Double customItem188__c1 = jsonObject1.getDouble("customItem188__c");//包装数
                        String customItem191__c1 = jsonObject1.getString("customItem191__c");//批次
                        String customItem192__c1 = jsonObject1.getString("customItem192__c");//分包标志
                        String customItem186__c1 = jsonObject1.getString("customItem186__c");//计量单位
                        String customItem193__c_1 = jsonObject1.getString("customItem193__c");//物料说明
                        int customItem192__c1_int = StringUtils.isBlank(customItem192__c1) ? 0 : Integer.valueOf(customItem192__c1);
                        //校验批次是否存在
                        Boolean aBoolean = checkCoiUf_LotIsExist(ERPtoken, customItem191__c1);
                        if (!aBoolean) {
                            JSONObject object1 = new JSONObject();
                            object1.put("id", id);
                            object1.put("customItem190__c", "批次不存在");
                            detailArray.add(object1);
                            continue;
                        }

                        priceTotal = priceTotal == null ? 0 : priceTotal;
                        String CoNum_OrderProduct = CoNum;//订单号
                        String CoCustNum = customItem182__c;//客户编号
                        int CoLine = ++j;//订单行
                        String Item = customItem183__c;//物料
                        String Description = customItem193__c_1;//物料
                        Double PriceConv = unitPrice;//单价
                        Integer QtyOrderedConv = quantity;//订购量
                        Double Disc = discount;//折扣
                        String coiUf_StandNum = customItem184__c;//规格
                        String Stat_OrderProduct = "O";//状态 默认写死
                        int InvoiceHold = customItem185__c_int;//发货冻结
                        String AdrCurrCode = money;//货币
                        Double DerExtPrice = priceTotal;//总价格
                        String coiUf_Lot = customItem191__c1;//批次
                        String coiUf_Note = customItem189__c;//备注
                        int coiUf_rework = customItem192__c1_int;//分包标志
                        String UM = customItem186__c1;//分包标志
                        String Whse_replace = getArrayToData(customItem214__c);
                        String Whse = StringUtils.isBlank(Whse_replace) ? "MAIN" : Whse_replace;//仓库
                        String ShipSite = customItem212__c;//站点
//                        String oHXCoitemPacages_pt_num =customItem187__c_OrderProduct;//包装模板
//                        Double oHXCoitemPacages_matl_qty =customItem186__c1;//内容量
//                        Double oHXCoitemPacages_qty_package =customItem188__c1;//包装数

                        JSONObject dataDetailObject = new JSONObject();
                        dataDetailObject.put("CoNum", CoNum_OrderProduct);
                        dataDetailObject.put("CoCustNum", CoCustNum);
                        dataDetailObject.put("CoLine", CoLine);
                        dataDetailObject.put("Item", Item);
                        dataDetailObject.put("Description", Description);
                        dataDetailObject.put("PriceConv", PriceConv);
                        dataDetailObject.put("QtyOrderedConv", QtyOrderedConv);
                        dataDetailObject.put("Disc", Disc);
                        dataDetailObject.put("coiUf_StandNum", coiUf_StandNum);
                        dataDetailObject.put("Stat", Stat_OrderProduct);
                        dataDetailObject.put("InvoiceHold", InvoiceHold);
                        dataDetailObject.put("AdrCurrCode", AdrCurrCode);
                        dataDetailObject.put("DerExtPrice", DerExtPrice);
                        dataDetailObject.put("coiUf_Lot", coiUf_Lot);
                        dataDetailObject.put("coiUf_Note", coiUf_Note);
                        dataDetailObject.put("coiUf_rework", coiUf_rework);
                        dataDetailObject.put("UM", UM);
                        dataDetailObject.put("Whse", Whse);
                        dataDetailObject.put("ShipSite", ShipSite);
//                        dataDetailObject.put("oHXCoitemPacages.pt_num", oHXCoitemPacages_pt_num);
//                        dataDetailObject.put("oHXCoitemPacages.matl_qty", oHXCoitemPacages_matl_qty);
//                        dataDetailObject.put("oHXCoitemPacages.qty_package", oHXCoitemPacages_qty_package);
                        JSONArray allItems1 = getAllItems(dataDetailObject);
                        JSONArray propertyList1 = getPropertyList(dataDetailObject);
                        String slCoitems = addData(ERPtoken, "SLCoitems", allItems1, propertyList1);
                        if (slCoitems == null) {
                            JSONObject object1 = new JSONObject();
                            object1.put("id", id);
                            object1.put("customItem190__c", "已同步");
                            detailArray.add(object1);
                        }
                    }
                }
            }
            if (errorArray.size() > 0) {
                splitExcect(errorArray, "updateOrder");
            }
            if (detailArray.size() > 0) {
                splitExcect(detailArray, "updateOrderProduct");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Boolean checkCoiUf_LotIsExist(String ERPtoken, String coiUf_Lot) {
        String result = idoWebServiceSoap.loadJson(ERPtoken, "SLLots", "Lot", " Lot = '" + coiUf_Lot + "'", "", "", -1);
        System.out.println("ERP查询结果：" + result);
        JSONObject resultJson = JSONObject.parseObject(result);
        JSONArray Items = resultJson.getJSONArray("Items");
        if (Items.size() < 1) {
            return false;
        } else {
            return true;
        }
    }

    private Map<Long, JSONArray> getOrderProductInfo(String ids) throws Exception {
        Map<Long, JSONArray> map = new HashMap<>();
        String sqlPrefix = "select id,orderId,customItem182__c,customItem183__c,unitPrice,quantity,discount,customItem184__c,customItem185__c,priceTotal,customItem189__c,customItem191__c,customItem192__c,customItem193__c,customItem186__c from orderProduct where orderId in(";
        String sqlsuffix = ")";
        JSONArray dataMoreIds = getDataMoreIds(ids, sqlPrefix, sqlsuffix);
        for (int i = 0; i < dataMoreIds.size(); i++) {
            JSONObject jsonObject = dataMoreIds.getJSONObject(i);
            Long orderId = jsonObject.getLong("orderId");
            JSONArray jsonArray = map.get(orderId);
            if (jsonArray == null) {
                jsonArray = new JSONArray();
            }
            jsonArray.add(jsonObject);
            map.put(orderId, jsonArray);
        }
        return map;
    }

    private JSONArray getDataMoreIds(String ids, String sqlPrefix, String sqlsuffix) throws Exception {
        JSONArray all3 = new JSONArray();
        String sql = "";
        while (true) {
            if (ids.length() > 800) {
                int index = 800;
                String substring = ids.substring(index, index + 1);
                while (!",".equals(substring)) {
                    index++;
                    substring = ids.substring(index, index + 1);
                }
                String substring1 = ids.substring(0, index);
                sql = sqlPrefix + substring1 + sqlsuffix;
                String bySql3 = queryServer.getBySql(sql);
                JSONArray allresult = queryServer.findAll(getToken(), bySql3, sql);
                all3.addAll(allresult);
                ids = ids.substring(index + 1, ids.length());
            } else {
                    sql = sqlPrefix + ids + sqlsuffix;
                    String bySql3 = queryServer.getBySql(sql);
                    JSONArray allresult = queryServer.findAll(getToken(), bySql3, sql);
                    all3.addAll(allresult);
                    break;
            }
        }
        return all3;
    }

    public void splitExcect(JSONArray dataAray, String url) throws Exception {
        if (dataAray.size() < 24000) {
            JSONObject object1 = new JSONObject();
            object1.put("data", dataAray);
            customApi.executeEntity(object1, url);
        } else {
            JSONArray requestArray = new JSONArray();
            int j = 1;
            for (int i = 0; i < dataAray.size(); i++) {
                if (j == 1) {
                    requestArray = new JSONArray();
                }
                JSONObject customEntity116__c = dataAray.getJSONObject(i);
                requestArray.add(customEntity116__c);
                if (j == 24000) {
                    j = 0;
                    JSONObject object1 = new JSONObject();
                    object1.put("data", requestArray);
                    customApi.executeEntity(object1, url);
                }
                j++;
            }
            if (j < 24000 && j > 1) {
                JSONObject object1 = new JSONObject();
                object1.put("data", requestArray);
                customApi.executeEntity(object1, url);
            }
        }
    }

    private JSONArray getPropertyList(JSONObject dataObject) {
        JSONArray propertyList = new JSONArray();
        Set<Map.Entry<String, Object>> entries = dataObject.entrySet();
        entries.forEach(map -> {
            String key = map.getKey();
            propertyList.add(key);
        });
        return propertyList;
    }

    /**
     * 拼接参数
     *
     * @param dataObject
     * @return
     */
    public JSONArray getAllItems(JSONObject dataObject) {
        JSONArray allItems = new JSONArray();
        JSONArray Items = new JSONArray();
        Set<Map.Entry<String, Object>> entries = dataObject.entrySet();
        entries.forEach(map -> {
            String fieldName = map.getKey();
            Object data = map.getValue();
            JSONObject object1 = new JSONObject();
            object1.put("Property", data);
            if ("CustNum".equals(fieldName) || "CustSeq".equals(fieldName) || "ref_num".equals(fieldName) || "DropShipNo".equals(fieldName) || "DropSeq".equals(fieldName) || "ContactID".equals(fieldName)) {
                object1.put("Updated", false);
            } else {
                object1.put("Updated", true);
            }
            Items.add(object1);
        });
        allItems.add(Items);
        return allItems;
    }

    private JSONArray addToError(JSONArray errorArray, String message, Long orderId) {
        JSONObject object = new JSONObject();
        object.put("id", orderId);
        object.put("customItem201__c", message);
        errorArray.add(object);
        return errorArray;
    }


    public String getData(JSONObject dataObject, String fieldName) {
        String data = null;
        try {
            data = dataObject.getString(fieldName);
        } catch (Exception e) {
            data = null;
        }
        return data;
    }

    public String getArrayToData(String replace) {
        String data = null;
        try {
            String[] split = replace.split("-");
            data = split[0];
        } catch (Exception e) {
            data = null;
        }
        return data;
    }

    public String getSplitData(JSONObject dataObject, String fieldName) {
        String data = null;
        try {
            String[] daataSplit = dataObject.getJSONArray(fieldName).toArray(new String[0]);
            for (int i = 0; i < daataSplit.length; i++) {
                String dataString = daataSplit[i];
                if (StringUtils.isBlank(dataString) || dataString.contains("停用")) {
                    continue;
                }
                if (dataString.contains("\t")) {
                    data = dataString.split("\t")[0];
                } else if (dataString.contains("_")) {
                    data = dataString.split("_")[0];
                } else {
                    data = dataString.split(" ")[0];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            data = null;
        }
        return data;
    }

    public Float getFloatData(JSONObject dataObject, String fieldName) {
        Float data = 0F;
        try {
            data = dataObject.getFloatValue(fieldName);
        } catch (Exception e) {
            data = 0F;
        }
        return data;
    }

    public String geDateData(JSONObject dataObject, String fieldName) {
        Calendar instance = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String dataTime = null;
        try {
            Long dataLong = dataObject.getLongValue(fieldName);
            instance.setTimeInMillis(dataLong);
            Date date = instance.getTime();
            dataTime = df.format(date);
        } catch (Exception e) {
            dataTime = null;
        }
        return dataTime;
    }

    /**
     * 保存方法
     *
     * @param toKen
     * @param IDOName
     * @param Properties
     * @param PropertyList
     * @return
     */
    public String addData(String toKen, String IDOName, JSONArray Properties, JSONArray PropertyList) {
        JSONObject object = new JSONObject();
        object.put("IDOName", IDOName);
        JSONArray Items = new JSONArray();
        for (int i = 0; i < Properties.size(); i++) {
            JSONObject object1 = new JSONObject();
            object1.put("Properties", Properties.getJSONArray(i));
            object1.put("EditStatus", 3);
            Items.add(object1);
        }
        object.put("Items", Items);
        object.put("PropertyList", PropertyList);
        System.out.println(JSONObject.toJSONString(object, SerializerFeature.WriteMapNullValue));
        String result = idoWebServiceSoap.saveJson(toKen, JSONObject.toJSONString(object, SerializerFeature.WriteMapNullValue), "", "", "");
        return result;

    }

    /**
     * 更新方法
     *
     * @param toKen
     * @param IDOName
     * @param Properties
     * @param PropertyList
     * @return
     */
    public String updateData(String toKen, String IDOName, JSONArray Properties, JSONArray PropertyList, String ID) {
        JSONObject object = new JSONObject();
        object.put("IDOName", IDOName);
        JSONArray Items = new JSONArray();
        for (int i = 0; i < Properties.size(); i++) {
            JSONObject object1 = new JSONObject();
            object1.put("Properties", Properties.getJSONArray(i));
            object1.put("EditStatus", 0);
            object1.put("ID", ID);
            Items.add(object1);
        }
        object.put("Items", Items);
        object.put("PropertyList", PropertyList);
        System.out.println(object.toJSONString());
        String result = idoWebServiceSoap.saveJson(toKen, object.toJSONString(), "", "", "");
        return result;
    }

    /**
     * 调用存储过程
     *
     * @param token
     * @return
     */
    public String getCustomNum(String token, Holder<String> pa, String methodName) {
        Holder<Object> pa2 = new Holder<>();
        idoWebServiceSoap.callMethod(token, "SP!", methodName, pa, pa2);
        //输出返回结果
        System.err.println(pa.value);
        return pa.value;
    }

    public String getArrayToData(JSONObject dataObject, String fieldName) {
        String data = null;
        try {
            data = (String) dataObject.getJSONArray(fieldName).toArray()[0];
        } catch (Exception e) {
            data = null;
        }
        return data;
    }

    /**
     * 更新客户编号 ERP->CRM
     *
     * @param ERPDataId
     * @throws Exception
     */
    public String updateCustomerEditStatus(Long ERPDataId) throws Exception {
        JSONObject object = new JSONObject();
        object.put("id", ERPDataId);
        object.put("customItem237__c", true);
        String post = queryServer.updateCustomizeByIdNoThrowException(object);
        return post;
    }
}