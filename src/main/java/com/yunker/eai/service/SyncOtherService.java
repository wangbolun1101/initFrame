package com.yunker.eai.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.yunker.eai.controller.CommonController;
import com.yunker.eai.log.ModuleOutputLogger;
import com.yunker.eai.oaPackage.AnyType2AnyTypeMapEntry;
import com.yunker.eai.oaPackage.HXCRMServiceLocator;
import com.yunker.eai.oaPackage.HXCRMServicePortType;
import com.yunker.eai.util.*;
import lombok.extern.slf4j.Slf4j;
import mypackage.IDOWebService;
import mypackage.IDOWebServiceSoap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.xml.rpc.ServiceException;
import javax.xml.ws.Holder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 定时同步crm->oa同步数据
 */
@Slf4j
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
    @Autowired
    private BulkAPI bulkAPI;

    //实例化接口
//    private WorkflowService workflowService = new WorkflowService();
//    private WorkflowServicePortType workflowServiceHttpPort = workflowService.getWorkflowServiceHttpPort();


    private final Long NEIBUENTITYTYPE = 1243042240053584L;//内部领用
    private final Long BEIHUOENTITYTYPE = 7845758L;//默认业务类型
    private final long BELONGID = 1332135646396821L;

    private final long systemId = 1185240L;

    //实例化接口
    private IDOWebService ST = new IDOWebService();
    private IDOWebServiceSoap idoWebServiceSoap = ST.getIDOWebServiceSoap();
    private String userId = "crm";//用户名
    private String pswd = "Crm123456";//密码
    private String config = "LIVE_HXSW";//账套
    DateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:dd");
    DateFormat df=new SimpleDateFormat("yyyy-MM-dd");
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




    @Scheduled(cron = "0 0/5 * * * ? ")
    public void approve() throws Exception {
        syncOrderWhenOverApprove();
        updateCreditWhenApprove();
    }


    /**
     * 超频超期审批通过后，将关联订单生效并同步到ERP
     */
    public void syncOrderWhenOverApprove() throws Exception {
        String sql="select id,customItem35__c from customEntity28__c where customItem47__c='归档' and customItem37__c =0 and customItem35__c is not null";
        String bySql = queryServer.getBySql(sql);
        JSONArray all = queryServer.findAll(getToken(), bySql, sql);
        for (int i = 0; i < all.size(); i++) {
            JSONObject jsonObject = all.getJSONObject(i);
            Long orderId = jsonObject.getLong("customItem35__c");
            JSONObject orderObject = new JSONObject();
            orderObject.put("id", orderId);
            orderObject.put("customItem224__c", true);
            String s = queryServer.updateOrder(orderObject);
            log.info("更新超期超额审批状态 ======> "+s);
            //生效订单
            queryServer.takeEffectOrder(orderId);
        }
    }

    /**
     * 赊销条款（账期）审批通过后，修改账套事业部账期条款及资信额度
     */
    public void updateCreditWhenApprove() throws Exception {
        String sql="select id,customItem38__c,customItem11__c,customItem19__c,customItem36__c,customItem37__c,customItem42__c from customEntity29__c where customItem47__c='归档' and customItem48__c = true and customItem38__c is not null";
        String bySql = queryServer.getBySql(sql);
        JSONArray all = queryServer.findAll(getToken(), bySql, sql);
        for (int i = 0; i < all.size(); i++) {
            JSONObject jsonObject = all.getJSONObject(i);
            Long customItem38__c = jsonObject.getLong("customItem38__c");//原账套事业部账期
            Long customItem11__c = jsonObject.getLong("customItem11__c");//客户
            Long customItem42__c = jsonObject.getLong("customItem42__c");//新账期
            Long customItem36__c = jsonObject.getLong("customItem36__c");//事业部
            Long customItem37__c = jsonObject.getLong("customItem37__c");//ERP客户账套
            Double customItem19__c = jsonObject.getDouble("customItem19__c");//客户新赊销账额
            if (customItem38__c!=null&&customItem38__c!=0){
                JSONObject Object = new JSONObject();
                Object.put("id", customItem38__c);
                Object.put("customItem3__c", customItem19__c);
                Object.put("customItem8__c", customItem42__c);
                try {
                    queryServer.updateCustomizeById(Object);
                } catch (Exception e) {
                    log.info("更新事业部账期条款及资信额度 ======> "+e.getMessage());
                }
            }else {
                JSONObject object = new JSONObject();
                object.put("entityType", 1214917399855445L);
                object.put("customItem3__c", customItem19__c);
                object.put("customItem6__c", customItem36__c);
                object.put("customItem8__c", customItem42__c);
                object.put("customItem9__c", customItem11__c);
                object.put("customItem13__c", customItem37__c);
                queryServer.createCustomize(object, 1214918522061203L);
            }
        }
    }



    @Scheduled(cron = "0 0 0/2 * * ?")
//    @Scheduled(cron = "0/5 * * * * ?")
    public void updateOrderOverDate(){
//        String sql="select id,customItem186__c,customItem222__c,TermsCode__c,amount,customItem213__c from _order";
        String sql="select id,customItem186__c,customItem222__c,TermsCode__c,amount,customItem213__c from _order where (customItem205__c is null and customItem206__c is null and customItem207__c is null and customItem208__c is null) or customItem201__c <> '同步成功' or customItem201__c is null";
        try {
            String bySql = queryServer.getBySql(sql);
            JSONArray all = queryServer.findAll(getToken(), bySql, sql);
            for (int i = 0; i < all.size(); i++) {
                try {
                    JSONObject jsonObject = all.getJSONObject(i);
                    Long id = jsonObject.getLong("id");
                    String custnum = jsonObject.getString("customItem186__c");//客户编号
                    String bu = jsonObject.getString("customItem222__c");//事业部编号
                    String customItem223__c = jsonObject.getString("TermsCode__c");//条款
                    String customItem213__c = jsonObject.getString("customItem213__c");//账套
                    String string = provisionJson.getString(customItem223__c);
                    string=StringUtils.isBlank(string)?"":string;
                    String term =string;
                    Double uf_overlimit3 = jsonObject.getDouble("amount");//金额
                    String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, customItem213__c);
                    Holder<String> pa = new Holder<>("<Parameters><Parameter>" + custnum + "</Parameter>" +
                            "<Parameter>" + bu + "</Parameter>" +
                            "<Parameter>" + term + "</Parameter>" +
                            "<Parameter>" + uf_overlimit3 + "</Parameter>" +
                            "<Parameter ByRef=\"Y\"></Parameter></Parameters>");
                    String customNum = getCustomNum(ERPtoken, pa, "hxsp_calc_ecocredit_crm");
                    JSONArray jsonArray = XmlUtil.unPackageMAIN(customNum);
                    for (int j = 0; j < jsonArray.size(); j++) {
                        JSONObject orderJSON = new JSONObject();
                        JSONObject jsonObject1 = jsonArray.getJSONObject(j);
                        Double uf_overlimit = jsonObject1.getDouble("Uf_overlimit");//事业部剩余额度
                        Double uf_overterm = jsonObject1.getDouble("Uf_overterm");//超期天数
                        Double Uf_overdate = jsonObject1.getDouble("Uf_overdate");//最大逾期天数
                        Double Uf_overdateamount = jsonObject1.getDouble("Uf_overdateamount");//逾期金额
                        orderJSON.put("id", id);
                        orderJSON.put("customItem205__c", uf_overlimit);
                        orderJSON.put("customItem206__c", uf_overterm.intValue());
                        orderJSON.put("customItem207__c", Uf_overdate.intValue());
                        orderJSON.put("customItem208__c", Uf_overdateamount);
                        String s = queryServer.updateOrder(orderJSON);
                        log.info("更新订单超期超频："+s);
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 更新客户纳税人识别号
     */
    @Scheduled(cron = "0 30 17 * * ?")
    public void upateAccountTaxpyerId() throws Exception {
        JSONArray jsonArray=new JSONArray();
        String sql="select id,accountName from account where srcFlg=1 and customItem194__c is null";
        JSONObject byXoqlSimple = queryServer.getByXoqlSimple(sql);
        JSONArray allByXoqlSample = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple, sql);
        String token = getToken();
        for (int i = 0; i < allByXoqlSample.size(); i++) {
            JSONObject jsonObject = allByXoqlSample.getJSONObject(i);
            Long id = jsonObject.getLong("id");
            String accountName = jsonObject.getString("accountName");
            String enterPriseInfo = queryServer.getEnterPriseInfo(accountName, token);
            if (StringUtils.isNotBlank(enterPriseInfo)){
                JSONObject object=new JSONObject();
                object.put("id",id);
                object.put("customItem194__c",enterPriseInfo);
                jsonArray.add(object);
            }
        }
        bulkAPI.createDataTaskJob(jsonArray, "account", "update");
    }

    @Scheduled(cron = "0 1/5 * * * ?")
    public void getOrderProductStatus(){
        JSONArray jsonArray = new JSONArray();
        String sql="select id,customItem171__c,customItem172__c,customItem195__c,customItem183__c,customItem196__c from orderProduct where (customItem171__c is null or customItem172__c is null) and customItem195__c not like 'SO%'";
        try {
            String bySql = queryServer.getBySql(sql);
            JSONArray all = queryServer.findAll(getToken(), bySql, sql);
            for (int i = 0; i < all.size(); i++) {
                JSONObject jsonObject = all.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                String po = jsonObject.getString("customItem195__c");//dd订单编号
                String productNo = jsonObject.getString("customItem183__c");//物料编号
                String ERPConfig = jsonObject.getString("customItem196__c");//账套
                String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, ERPConfig);
                String result = idoWebServiceSoap.loadJson(ERPtoken, "SLCoitems","CoNum,CoLine,Item,DerQtyShippedConv,DerQtyInvoicedConv", " CoNum = '" + po + "' and Item = '"+productNo+"'", "", "", 3000);
                JSONObject resultJson = JSONObject.parseObject(result);
                JSONArray Items = resultJson.getJSONArray("Items");
                if (Items.size() < 1) {
                    continue;
                }
                for (int j = 0; j < Items.size(); j++) {
                    JSONObject jsonObject1 = Items.getJSONObject(j);
                    JSONArray properties = jsonObject1.getJSONArray("Properties");
                    JSONObject propertieObject=new JSONObject();
                    for (int k = 0; k < properties.size(); k++) {
                        JSONObject jsonObject2 = properties.getJSONObject(k);
                        String property_0 = jsonObject2.getString("Property");
                        String string = properties.getString(k);
                        propertieObject.put(string, property_0);
                    }
                    Object derQtyShippedConv = propertieObject.get("DerQtyShippedConv");
                    Object derQtyInvoicedConv = propertieObject.get("DerQtyInvoicedConv");
                    JSONObject paramObject = new JSONObject();
                    paramObject.put("id", id);
                    paramObject.put("customItem171__c", derQtyInvoicedConv);
                    paramObject.put("customItem172__c", derQtyShippedConv);
                    jsonArray.add(paramObject);
                    break;
                }
            }
            if (jsonArray.size()>0){
                bulkAPI.createDataTaskJob(jsonArray, "orderProduct", "update");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Scheduled(cron = "0 2/5 * * * ?")
    public void getAOApproveStatus(){
        Map<String,JSONObject>map = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        JSONArray jsonArray = new JSONArray();
        try {
            HXCRMServicePortType hxcrmServiceHttpPort = new HXCRMServiceLocator().getHXCRMServiceHttpPort();

            //查询所有未归档oa同步审批数据
            try {
                String sql="select id,customItem1__c,customItem2__c,customItem3__c,customItem5__c from customEntity48__c where customItem5__c is null or customItem5__c = false";
                String bySql = queryServer.getBySql(sql);
                JSONArray all = queryServer.findAll(getToken(), bySql, sql);
                for (int i = 0; i < all.size(); i++) {
                    JSONObject jsonObject = all.getJSONObject(i);
                    String requestid = jsonObject.getString("customItem2__c");
                    map.put(requestid, jsonObject);
                    if (sb.length()==0){
                        sb.append(requestid);
                    }else {
                        sb.append(","+requestid);
                    }
                }
                if (sb.length()>0){
                    AnyType2AnyTypeMapEntry[][] wfStatusByIdList = hxcrmServiceHttpPort.getWFStatusByIdList(sb.toString());
                    for (AnyType2AnyTypeMapEntry[] anyType2AnyTypeMapEntries : wfStatusByIdList) {
                        JSONObject object=new JSONObject();
                        String date = "";
                        String time = "";
                        for (AnyType2AnyTypeMapEntry anyType2AnyTypeMapEntry : anyType2AnyTypeMapEntries) {
                            String key = (String) anyType2AnyTypeMapEntry.getKey();
                            Object value = anyType2AnyTypeMapEntry.getValue();
                            if ("lastoperatedate".equals(key)){
                                date = (String) value;
                            }else if ("lastoperatetime".equals(key)){
                                time = (String) value;
                            }else {
                                object.put(key, value);
                            }
                        }
                        try {
                            object.put("lastDate", dateFormat.parse(date+" "+time).getTime());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        jsonArray.add(object);
                    }
                }
                for (int i = 0; i < jsonArray.size(); i++) {
                    try {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String requestId = jsonObject.getString("requestid");
                        Integer workflowType = jsonObject.getInteger("workflowtype");
                        Long lastDate = jsonObject.getLong("lastDate");
                        JSONObject object = map.get(requestId);
                        Long dataId = Long.valueOf(object.getString("customItem1__c"));
                        Long OAId = Long.valueOf(object.getString("id"));
                        Long belongId = Long.valueOf(object.getString("customItem3__c"));
                        if (dataId==null||dataId==0){
                            continue;
                        }
                        if (workflowType.intValue()==0){//OA审批拒绝或退回 更新crm状态
                            JSONObject dataObject = new JSONObject();
                            JSONObject OAObject = new JSONObject();
                            dataObject.put("id", dataId);
                            dataObject.put("customItem47__c", "退回");
                            queryServer.updateCustomizeById(dataObject);
                            OAObject.put("id", OAId);
                            OAObject.put("customItem5__c", true);
                            queryServer.updateCustomizeById(OAObject);
                            JSONObject customInfo = queryServer.getCustomInfo(dataId);

                            JSONObject messageObject =new JSONObject();
                            JSONArray receivers = new JSONArray();
                            JSONObject receiverObject =new JSONObject();
                            JSONArray mergeFields = new JSONArray();
                            JSONObject mergeFieldObject =new JSONObject();
                            receiverObject.put("id", customInfo.getLong("ownerId"));
                            receiverObject.put("type", 1);
                            receivers.addAll(receivers);
                            mergeFieldObject.put("belongId", belongId);
                            mergeFieldObject.put("objectId", dataId);
                            mergeFieldObject.put("type", 1);
                            mergeFields.add(mergeFieldObject);

                            messageObject.put("publisherId", systemId);
                            messageObject.put("noticeType", 1);
                            messageObject.put("belongId", belongId);
                            messageObject.put("objectId", dataId);
                            messageObject.put("mergeFieldsIndex", 0);
                            messageObject.put("receivers", receivers);
                            messageObject.put("mergeFields", mergeFields);
                            messageObject.put("content", "您提交的审批被拒绝，点击{arg0}进行查看！");
                            queryServer.sendMessage(messageObject);
                            Long flowId = queryServer.getFlowId(dataId, belongId);
                            queryServer.notAgreeApprove(flowId);

                        }else if (workflowType.intValue()==1){//审批中 略过
                            continue;
                        }else if (workflowType.intValue()==2){//审批通过 更新数据状态及OA同步模块数据状态
                            JSONObject dataObject = new JSONObject();
                            JSONObject OAObject = new JSONObject();
                            dataObject.put("id", dataId);
                            dataObject.put("customItem47__c", "归档");
                            queryServer.updateCustomizeById(dataObject);
                            OAObject.put("id", OAId);
                            OAObject.put("customItem5__c", true);
                            queryServer.updateCustomizeById(OAObject);
                            //审批通过
                            Long flowId = queryServer.getFlowId(dataId, belongId);
                            queryServer.agreeApprove(flowId);
                            if (belongId.longValue()==1248875077353801L){//新物料价格审批通过后
                                StringBuilder productSb = new StringBuilder();
                                JSONObject customInfo = queryServer.getCustomInfo(dataId);
                                //查询新物料价格审批明细
                                String newPriceEntry = "select customItem1__c,customItem6__c from customEntity42__c where customItem8__c="+dataId;
                                String bySql2 = queryServer.getBySql(newPriceEntry);
                                JSONArray all1 = queryServer.findAll(getToken(), bySql2, newPriceEntry);
                                if (all1.size()==0){
                                    continue;
                                }
                                for (int j = 0; j < all1.size(); j++) {
                                    JSONObject jsonObject1 = all1.getJSONObject(j);
                                    Long productId = jsonObject1.getLong("customItem1__c");
                                    if (productSb.length()==0){
                                        productSb.append(productId);
                                    }else {
                                        productSb.append(","+productId);
                                    }
                                }

                                Long accountId = customInfo.getLong("customItem8__c");
                                Long ERPId = customInfo.getLong("customItem21__c");
                                Integer bz = customInfo.getInteger("customItem22__c");//币种
                                Long ownerId = customInfo.getLong("ownerId");
                                Long dimDepart = customInfo.getLong("dimDepart");
                                String ERP = customInfo.getString("customItem20__c");//账套
                                Integer customItem23__c = customInfo.getInteger("customItem23__c");//申请类型
                                customItem23__c=customItem23__c==null?0:customItem23__c;
                                //根据客户及erp账套查询价格表
                                String priceSql="select id from priceBook where customItem1__c="+accountId+" and customItem3__c="+ERPId;
                                String bySql1 = queryServer.getBySql(priceSql);
                                JSONObject object1 = JSONObject.parseObject(bySql1);
                                JSONArray records = object1.getJSONArray("records");
                                if (records.size()==0){//价格表不存在，创建
                                    //todo 创建
                                    //创建价格表
                                    JSONObject priceInfoJson=new JSONObject();
                                    priceInfoJson.put("name", ERP);
                                    priceInfoJson.put("entityType", 101065558L);
                                    priceInfoJson.put("ownerId", ownerId);
                                    priceInfoJson.put("enableFlg", 1);
                                    priceInfoJson.put("customItem1__c", accountId);
                                    priceInfoJson.put("customItem3__c", ERPId);
                                    Long priceBookId = queryServer.createPriceBook(priceInfoJson);
                                    //创建价格表明细
                                    for (int j = 0; j < all1.size(); j++) {
                                        JSONObject jsonObject2 = all1.getJSONObject(j);
                                        Long productId = jsonObject2.getLong("customItem1__c");
                                        Double price = jsonObject2.getDouble("customItem6__c");
                                        JSONObject priceDetailJson = new JSONObject();
                                        priceDetailJson.put("entityType", 101065557L);
                                        priceDetailJson.put("priceBookId", priceBookId);
                                        priceDetailJson.put("customItem1__c", lastDate);
                                        priceDetailJson.put("productId", productId);
                                        priceDetailJson.put("bookPrice", price);
                                        priceDetailJson.put("productPrice", price);
                                        priceDetailJson.put("syncFlg", 2);
                                        priceDetailJson.put("enableFlg", 1);
                                        priceDetailJson.put("customItem3__c", bz);
                                        priceDetailJson.put("dimDepart", dimDepart);
                                        queryServer.createPriceBookEntry(priceDetailJson);
                                    }

                                }else {
                                    JSONObject jsonObject1 = records.getJSONObject(0);
                                    Long id = jsonObject1.getLong("id");
                                    //根据查询对应价格表明细，存在则更新，不存在则创建
                                    Map<Long, Long> priceBookEntrysMap = getPriceBookEntrys(id, productSb.toString());
                                    for (int j = 0; j < all1.size(); j++) {
                                        JSONObject jsonObject2 = all1.getJSONObject(j);
                                        Long productId = jsonObject2.getLong("customItem1__c");
                                        Double price = jsonObject2.getDouble("customItem6__c");
                                        Long priceBookEntryId = priceBookEntrysMap.get(productId);
                                        if (priceBookEntryId!=null&&priceBookEntryId!=0){//存在，更新
                                            JSONObject param = new JSONObject();
                                            param.put("id", priceBookEntryId);
                                            param.put("customItem1__c", lastDate);
                                            param.put("bookPrice", price);
                                            param.put("productPrice", price);
                                            queryServer.updatePriceBookEntry(priceBookEntryId, param);
                                        }else {
                                            //创建价格表明细
                                            JSONObject priceDetailJson = new JSONObject();
                                            priceDetailJson.put("entityType", 101065557L);
                                            priceDetailJson.put("priceBookId", id);
                                            priceDetailJson.put("customItem1__c", lastDate);
                                            priceDetailJson.put("productId", productId);
                                            priceDetailJson.put("bookPrice", price);
                                            priceDetailJson.put("productPrice", price);
                                            priceDetailJson.put("syncFlg", 2);
                                            priceDetailJson.put("enableFlg", 1);
                                            priceDetailJson.put("customItem3__c", bz);
                                            priceDetailJson.put("dimDepart", dimDepart);
                                            queryServer.createPriceBookEntry(priceDetailJson);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    private Map<Long, Long> getPriceBookEntrys(Long id, String productIds) throws Exception {
        Map<Long,Long>map=new HashMap<>();
        String sql="select id,productId from priceBookEntry where priceBookId="+id+" and productId in("+productIds+")";
        String bySql = queryServer.getBySql(sql);
        JSONArray all = queryServer.findAll(getToken(), bySql, sql);
        for (int i = 0; i < all.size(); i++) {
            JSONObject jsonObject = all.getJSONObject(i);
            Long priceBookEntryId = jsonObject.getLong("id");
            Long productId = jsonObject.getLong("productId");
            map.put(productId, priceBookEntryId);
        }
        return map;
    }


    /**
     * 客户修改后同步到ERP
     */
//    @Scheduled(cron = "0/5 * * * * ?")
    @Scheduled(cron = "0 3/5 * * * ?")
    public void syncAccount1() {
        try {
            System.out.println("更新客户--同步开始");
            String CRMtoken = getToken();
            //                                                                       注册地址           国家                    省洲                             公司成立日期       经营渠道                    线上/线下         邮政编码   客户类型         资信额度          条款              信用冻结          最终用户类型      区域                   货币
            String sql = "select id,customItem201__c,ownerId.employeeCode,ownerId.managerId,accountName,customItem181__c,customItem195__c,fState,customItem197__c,fCity,fDistrict,customItem210__c,customItem186__c,entityType,customItem199__c,zipCode,customItem205__c,customItem204__c,customItem206__c,customItem184__c,customItem190__c,customItem156__c,level,customItem233__c,customItem207__c,customItem208__c,customItem209__c,customItem211__c,customItem212__c,customItem213__c,customItem214__c,customItem218__c,customItem194__c from account where customItem237__c=1 and customItem193__c = 1";
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
            String fieldsByBelongId = queryServer.getFieldsByBelongId(1340810097180997L);
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
            String ERPSql = "select id,customItem3__c,customItem4__c from customEntity63__c where (customItem5__c='同步完成' or customItem5__c='已完成') and customItem3__c is not null";
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
            String SQL = "select id from account where customItem201__c is null and customItem193__c = 1";
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
                if ("true".equals(status)) {
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
                String CustNum = jsonObject.getString("customItem201__c");//客户编号


                Integer ShowInDropDownList = 1;//ERP是否显示 该字段CRM不必显示，仅同步用

                String CurrCode = getArrayToData(jsonObject, "customItem233__c");//货币

                JSONObject excute4 = super.moneyJson;
                CurrCode = excute4.getString(CurrCode);

                String bu = getArrayToData(jsonObject, "customItem218__c");//事业部编号
                String ReservedField1 = jsonObject.getString("customItem194__c");//纳税识别号

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
                dataObject.put("ReservedField1",ReservedField1);

//                JSONArray jsonArray1 = JSONArray.parseArray("[\"CustNum\",\"CustSeq\",\"Name\",\"Addr_1\",\"Addr_2\",\"Addr_3\",\"Addr_4\",\"Country\",\"State\",\"City\",\"County\",\"Zip\",\"CustType\",\"CreditLimit\",\"TermsCode\",\"CreditHold\",\"EndUserType\",\"TerritoryCode\",\"SalesTeamID\",\"cusUf_GlobalId\",\"ShowInDropDownList\",\"CusShipmentApprovalRequired\",\"IncludeTaxInPrice\",\"CurrCode\",\"TaxCode1\"]");

                JSONArray ERPDataArray = ERPDataMap.get(accountId);
                Boolean updateStatus = true;
                for (int j = 0; j < ERPDataArray.size(); j++) {
                    JSONObject ERPDataObject = ERPDataArray.getJSONObject(j);
                    String ERPConfig = ERPDataObject.getString("customItem3__c");
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
//    @Scheduled(cron = "0/5 * * * * ?")
    @Scheduled(cron = "0 4/5 * * * ?")
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
                Long customItem213__c = jsonObject.getLong("customItem213__c");//ERP客户账套
                String customItem89__c = jsonObject.getString("customItem89__c");//ERP账套
                String customItem179__c = labelToValueUtil.replace(fieldsByBelongId_order, "customItem179__c", customItem89__c, "selectitem");
                Long accountId = jsonObject.getLong("accountId__c");//客户
                Long customItem206__c = jsonObject.getLong("customItem206__c");//收票地址
//                Long customItem207__c = jsonObject.getLong("customItem207__c");//信用额度与账期
                Integer customItem74__c = jsonObject.getInteger("customItem74__c");//发货地
                Integer customItem209__c = jsonObject.getInteger("customItem209__c");//票据类型
//                Integer status = jsonObject.getInteger("status");//状态

                String custnum = jsonObject.getString("customItem215__c");//客户编号
                String customItem211__c = jsonObject.getString("customItem102__c");//条款
                String string = provisionJson.getString(customItem211__c);
                string=StringUtils.isBlank(string)?"":string;
                String term =string;
//                        provisionJson.getString(customItem211__c);
                Double uf_overlimit3 = jsonObject.getDouble("customItem205__c");//总金额
                String bu = jsonObject.getString("customItem212__c");//事业部

//                Long customItem93__c = jsonObject.getLong("customItem93__c");//事业部
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
                orderJSON.put("customItem216__c",customItem213__c);

                try {
                    String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, customItem89__c);
                    Holder<String> pa = new Holder<>("<Parameters><Parameter>" + custnum + "</Parameter>" +
                            "<Parameter>" + bu + "</Parameter>" +
                            "<Parameter>" + term + "</Parameter>" +
                            "<Parameter>" + uf_overlimit3 + "</Parameter>" +
                            "<Parameter ByRef=\"Y\"></Parameter></Parameters>");
                    String customNum = getCustomNum(ERPtoken, pa, "hxsp_calc_ecocredit_crm");
                    JSONArray jsonArray = XmlUtil.unPackageMAIN(customNum);
                    for (int j = 0; j < jsonArray.size(); j++) {
                        JSONObject jsonObject1 = jsonArray.getJSONObject(j);
                        Double uf_overlimit = jsonObject1.getDouble("Uf_overlimit");//事业部剩余额度
                        Double uf_overterm = jsonObject1.getDouble("Uf_overterm");//超期天数
                        Double Uf_overdate = jsonObject1.getDouble("Uf_overdate");//最大逾期天数
                        Double Uf_overdateamount = jsonObject1.getDouble("Uf_overdateamount");//逾期金额
                        orderJSON.put("customItem205__c", uf_overlimit);
                        orderJSON.put("customItem206__c", uf_overterm.intValue());
                        orderJSON.put("customItem207__c", Uf_overdate.intValue());
                        orderJSON.put("customItem208__c", Uf_overdateamount);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

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
                        Long id = jsonObject1.getLong("id");//合同产品明细id
                        Long customItem3__c = jsonObject1.getLong("customItem3__c");//价格表明细
                        Double customItem6__c = jsonObject1.getDouble("customItem6__c");//价格表价格
                        Double customItem14__c = jsonObject1.getDouble("customItem14__c");//总价
                        Double customItem13__c = jsonObject1.getDouble("customItem13__c");//数量
                        String customItem15__c = jsonObject1.getString("customItem24__c");//备注
                        String customItem17__c = jsonObject1.getString("customItem17__c");//批次

                        String customItem41__c = jsonObject1.getString("customItem41__c");//外包规格1
                        String customItem42__c = jsonObject1.getString("customItem42__c");//外包模板1
                        String customItem43__c = jsonObject1.getString("customItem43__c");//外包规格2
                        String customItem44__c = jsonObject1.getString("customItem44__c");//外包模板2
                        String customItem45__c = jsonObject1.getString("customItem45__c");//内包规格1
                        String customItem46__c = jsonObject1.getString("customItem46__c");//内包模板1
                        String customItem47__c = jsonObject1.getString("customItem47__c");//标签规格1
                        String customItem48__c = jsonObject1.getString("customItem48__c");//标签模板1
                        String customItem49__c = jsonObject1.getString("customItem49__c");//标签规格2
                        String customItem50__c = jsonObject1.getString("customItem50__c");//标签模板2

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
                        orderDetailJSON.put("customItem191__c", customItem17__c);

                        orderDetailJSON.put("customItem205__c", id);
                        orderDetailJSON.put("customItem219__c", customItem41__c);
                        orderDetailJSON.put("customItem207__c", customItem42__c);
                        orderDetailJSON.put("customItem220__c", customItem43__c);
                        orderDetailJSON.put("customItem208__c", customItem44__c);
                        orderDetailJSON.put("customItem218__c", customItem45__c);
                        orderDetailJSON.put("customItem204__c", customItem46__c);
                        orderDetailJSON.put("customItem221__c", customItem47__c);
                        orderDetailJSON.put("customItem209__c", customItem48__c);
                        orderDetailJSON.put("customItem222__c", customItem49__c);
                        orderDetailJSON.put("customItem217__c", customItem50__c);

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
        String sqlPrefix = "select id,customItem25__c,customItem3__c,customItem6__c,customItem13__c,customItem24__c,customItem14__c,customItem17__c,customItem41__c,customItem42__c,customItem43__c,customItem44__c,customItem45__c,customItem46__c,customItem47__c,customItem48__c,customItem49__c,customItem50__c from customEntity54__c where customItem25__c in(";
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
        String sql = "select id,ownerId,customItem213__c,entityType,comment__c,customItem148__c__c,customItem166__c__c,accountId__c,customItem206__c,customItem74__c,customItem209__c,customItem102__c,customItem205__c,customItem89__c,customItem215__c,customItem212__c,customItem94__c from customEntity62__c where customItem47__c='归档' and customItem202__c__c=0 and (customItem204__c<>1 or customItem204__c is null)";
        String bySql = queryServer.getBySql(sql);
        JSONArray all = queryServer.findAll(getToken(), bySql, sql);
        return all;
    }

//    @Scheduled(cron = "0/5 * * * * ?")
    @Scheduled(cron = "0 0/5 * * * ?")
    public void syncOrder() {
        try {
            String fieldsByBelongId_order = queryServer.getFieldsByBelongId(35L);
            String fieldsByBelongId_account = queryServer.getFieldsByBelongId(1L);
            StringBuilder sb = new StringBuilder();

            JSONArray errorArray = new JSONArray();
            JSONArray detailArray = new JSONArray();
            //language=SQL
            String sql = "select id,customItem176__c,customItem192__c,customItem212__c,customItem213__c,customItem214__c,createdAt,customItem182__c,customItem186__c,customItem210__c,TermsCode__c,customItem193__c,comment,customItem194__c,customItem211__c,customItem196__c,customItem197__c,customItem198__c,customItem199__c,customItem202__c,customItem200__c,customItem218__c,customItem191__c,customItem222__c,amount,accountId,customItem224__c from _order where (customItem201__c<>'同步成功' or customItem201__c is null or customItem201__c='') and poStatus=2 AND createdAt>1594051200000";
            String bySql = queryServer.getBySql(sql);
            JSONArray all = queryServer.findAll(getToken(), bySql, sql);
            if (all.size()==0){
                System.out.println("无数据，不进行处理");
                return;
            }
            for (int i = 0; i < all.size(); i++) {
                JSONObject jsonObject = all.getJSONObject(i);
                String customItem213__c = jsonObject.getString("customItem213__c");//账套
                Long id = jsonObject.getLong("id");
                Long accountId = jsonObject.getLong("accountId");//客户id
                String custnum = jsonObject.getString("customItem186__c");//客户编号
                String term = jsonObject.getString("TermsCode__c");//条款
                String bu = jsonObject.getString("customItem222__c");//事业部编号
                Double uf_overlimit3 = jsonObject.getDouble("amount");//金额
                Boolean customItem224__c = jsonObject.getBoolean("customItem224__c");//超期超频是否审批通过
                customItem224__c = customItem224__c==null?false:customItem224__c;

                //查询该客户其他未生效订单金额
                String orderAmountSql = "select id,amount from _order where accountId = "+accountId+" and id<>"+id+" and (customItem201__c<>'同步成功' or customItem201__c is null or customItem201__c='') and poStatus=1 limit 0,300";
                String bySql1 = queryServer.getBySql(orderAmountSql);
                JSONObject object = JSONObject.parseObject(bySql1);
                JSONArray records = object.getJSONArray("records");
                for (int j = 0; j < records.size(); j++) {
                    JSONObject jsonObject1 = records.getJSONObject(j);
                    uf_overlimit3 += jsonObject1.getDouble("amount");
                }

                String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, customItem213__c);
                Holder<String> pa = new Holder<>("<Parameters><Parameter>" + custnum + "</Parameter>" +
                        "<Parameter>" + bu + "</Parameter>" +
                        "<Parameter>" + term + "</Parameter>" +
                        "<Parameter>" + uf_overlimit3 + "</Parameter>" +
                        "<Parameter ByRef=\"Y\"></Parameter></Parameters>");
                String customNum = getCustomNum(ERPtoken, pa, "hxsp_calc_ecocredit_crm");
                JSONArray jsonArray = XmlUtil.unPackageMAIN(customNum);
                Double uf_overlimit = 0D;//事业部剩余额度
                Double uf_overterm = 0D;//超期天数
                Double Uf_overdate = 0D;//最大逾期天数
                Double Uf_overdateamount = 0D;//逾期金额
                for (int j = 0; j < jsonArray.size(); j++) {
                    JSONObject jsonObject1 = jsonArray.getJSONObject(j);
                    uf_overlimit = jsonObject1.getDouble("Uf_overlimit");
                    uf_overterm = jsonObject1.getDouble("Uf_overterm");
                    Uf_overdate = jsonObject1.getDouble("Uf_overdate");
                    Uf_overdateamount = jsonObject1.getDouble("Uf_overdateamount");
                }
                if (!"C00".equals(term)&&!"现结".equals(term)&&!"现款".equals(term)){
                    if (uf_overlimit==null||uf_overterm==null||Uf_overdate==null||Uf_overdateamount==null||uf_overlimit<0||uf_overterm>0||Uf_overdate>0||Uf_overdateamount>0){
                        if (!customItem224__c) {
                            all.remove(jsonObject);
                            i--;
                            queryServer.deactivationOrder(id);
                        }
                        JSONObject orderJSON = new JSONObject();
                        orderJSON.put("customItem205__c", uf_overlimit);
                        orderJSON.put("customItem206__c", uf_overterm.intValue());
                        orderJSON.put("customItem207__c", Uf_overdate.intValue());
                        orderJSON.put("customItem208__c", Uf_overdateamount);
                        orderJSON.put("id", id);
                        if (!customItem224__c){
                            orderJSON.put("customItem201__c", "超期超频无法生效同步");
                        }
                        queryServer.updateOrder(orderJSON);
                        for (int j = 0; j < records.size(); j++) {
                            JSONObject jsonObject1 = records.getJSONObject(j);
                            Long id1 = jsonObject1.getLong("id");
                            JSONObject orderJSONOther = new JSONObject();
                            orderJSONOther.put("customItem205__c", uf_overlimit);
                            orderJSONOther.put("customItem206__c", uf_overterm.intValue());
                            orderJSONOther.put("customItem207__c", Uf_overdate.intValue());
                            orderJSONOther.put("customItem208__c", Uf_overdateamount);
                            orderJSONOther.put("id", id1);
                            queryServer.updateOrder(orderJSONOther);
                        }
                    }
                }else {
                    if (Uf_overdate==null||Uf_overdateamount==null||Uf_overdate>0||Uf_overdateamount>0){
                        if (!customItem224__c) {
                            all.remove(jsonObject);
                            i--;
                            queryServer.deactivationOrder(id);
                        }
                        JSONObject orderJSON = new JSONObject();
                        orderJSON.put("customItem205__c", uf_overlimit);
                        orderJSON.put("customItem206__c", uf_overterm.intValue());
                        orderJSON.put("customItem207__c", Uf_overdate.intValue());
                        orderJSON.put("customItem208__c", Uf_overdateamount);
                        orderJSON.put("id", id);
                        if (!customItem224__c) {
                            orderJSON.put("customItem201__c", "超期超频无法生效同步");
                        }
                        queryServer.updateOrder(orderJSON);
                        for (int j = 0; j < records.size(); j++) {
                            JSONObject jsonObject1 = records.getJSONObject(j);
                            Long id1 = jsonObject1.getLong("id");
                            JSONObject orderJSONOther = new JSONObject();
                            orderJSONOther.put("customItem205__c", uf_overlimit);
                            orderJSONOther.put("customItem206__c", uf_overterm.intValue());
                            orderJSONOther.put("customItem207__c", Uf_overdate.intValue());
                            orderJSONOther.put("customItem208__c", Uf_overdateamount);
                            orderJSONOther.put("id", id1);
                            queryServer.updateOrder(orderJSONOther);
                        }
                    }
                }
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
                String CustSeqStr=null;
                Long customItem176__c = jsonObject.getLong("customItem176__c");//收货地址
                String customItem187__c = jsonObject.getString("customItem210__c");//erp_id（收货地址）
//                if (StringUtils.isBlank(customItem187__c)) {
//                    errorArray=addToError(errorArray,"收货地址id为空",orderId);
//                    continue;
//                }
                if (StringUtils.isBlank(customItem187__c)&&customItem176__c!=null&&customItem176__c!=0){
                    String addressSql="select id,name,customItem12__c,customItem13__c from customEntity7__c where id="+customItem176__c;
                    String bySql1 = queryServer.getBySql(addressSql);
                    if (StringUtils.isNotBlank(bySql1)){
                        JSONObject object = JSONObject.parseObject(bySql1);
                        JSONArray records = object.getJSONArray("records");
                        for (int j = 0; j < records.size(); j++) {
                            JSONObject jsonObject1 = records.getJSONObject(j);
                            Long id = jsonObject1.getLong("id");
                            String name = jsonObject1.getString("name");
                            String customItem12__c = jsonObject1.getString("customItem12__c");//账套
                            String customItem13__c = jsonObject1.getString("customItem13__c");//客户编码
                            if (StringUtils.isBlank(customItem12__c)){
                                log.error("收货地址无ERP账套信息");
                                break;
                            }
                            if (StringUtils.isBlank(customItem13__c)){
                                log.error("收货地址无客户编码信息");
                                break;
                            }
                            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, customItem12__c);
                            String CustNum = customItem13__c;//客户编号
                            /*
                                根据客户编号查询收货人地址，确认地址编号
                             */

                            String result = idoWebServiceSoap.loadJson(ERPtoken, "SLCustomers", "CustSeq", "CustNum = '"+CustNum+"' and Name = '"+name+"'", "CustSeq DESC", "", -1);
                            JSONObject resultJson = JSONObject.parseObject(result);
                            JSONArray Items = resultJson.getJSONArray("Items");
                            for (int m = 0; m < Items.size(); m++) {
                                JSONObject ItemsObject = Items.getJSONObject(m);
                                JSONArray properties = ItemsObject.getJSONArray("Properties");
                                for (int k = 0; k < properties.size(); k++) {
                                    JSONObject jsonObject2 = properties.getJSONObject(k);
                                    CustSeqStr = jsonObject2.getString("Property");
                                }
                            }
                            if (StringUtils.isNotBlank(CustSeqStr)){
                                JSONObject object1=new JSONObject();
                                object1.put("id", id);
                                object1.put("erp_id__c", CustSeqStr);
                                queryServer.updateCustomizeById(object1);
                            }
                        }
                    }
                }else {
                    CustSeqStr=customItem187__c;
                }
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
                String CustSeq = CustSeqStr;   //收货地
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
                String OrigSite = customItem212__c; //源站点
                int IncludeTaxInPrice = 1;   //含税价格
                String Whse_replace = getArrayToData(customItem214__c);
                String Whse = StringUtils.isBlank(Whse_replace) ? "MAIN" : Whse_replace;//仓库
                Date parse = df.parse(OrderDate);
                Calendar instance = Calendar.getInstance();
                instance.setTime(parse);
                instance.add(Calendar.DAY_OF_YEAR, 10);
                Date time = instance.getTime();
                String DueDate = df.format(time);

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
                dataObject.put("IncludeTaxInPrice", IncludeTaxInPrice);
                dataObject.put("Whse", Whse);
                dataObject.put("OrigSite", OrigSite);
                System.out.println(JSONObject.toJSONString(dataObject, SerializerFeature.WriteMapNullValue));
                JSONArray allItems = getAllItems(dataObject);
                JSONArray propertyList = getPropertyList(dataObject);
                String slCustomers = null;//同步订单
                try {
                    slCustomers = addData(ERPtoken, "SLCos", allItems, propertyList);
                } catch (Exception e) {
                    e.printStackTrace();
                    JSONObject object = new JSONObject();
                    object.put("id", orderId);
                    object.put("customItem201__c", e.getMessage());
                    String post = queryServer.updateOrder(object);
                    continue;
                }
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
                        JSONObject jsonObject1 = jsonArray.getJSONObject(j);
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
                        String customItem189__c = jsonObject1.getString("comment");//备注
//                        Double customItem186__c1 = jsonObject1.getDouble("customItem186__c");//内容量
//                        String customItem187__c_OrderProduct = jsonObject1.getString("customItem187__c");//包装模板
//                        Double customItem188__c1 = jsonObject1.getDouble("customItem188__c");//包装数
                        String customItem191__c1 = jsonObject1.getString("customItem191__c");//批次
                        String customItem192__c1 = jsonObject1.getString("customItem192__c");//分包标志
                        String customItem186__c1 = jsonObject1.getString("customItem186__c");//计量单位
                        String customItem193__c_1 = jsonObject1.getString("customItem193__c");//物料说明


                        String bz_customItem219__c = jsonObject1.getString("customItem219__c");//外包规格1
                        String bz_customItem207__c = jsonObject1.getString("customItem207__c");//外包模板1
                        String bz_customItem215__c = jsonObject1.getString("customItem215__c");//外包装数1
                        String bz_customItem225__c = jsonObject1.getString("customItem225__c");//外包装小计1
                        String bz_customItem220__c = jsonObject1.getString("customItem220__c");//外包规格2
                        String bz_customItem208__c = jsonObject1.getString("customItem208__c");//外包模板2
                        String bz_customItem216__c = jsonObject1.getString("customItem216__c");//外包装数2
                        String bz_customItem226__c = jsonObject1.getString("customItem226__c");//外包装小计2
                        String bz_customItem218__c = jsonObject1.getString("customItem218__c");//内包规格
                        String bz_customItem204__c = jsonObject1.getString("customItem204__c");//内包模板
                        String bz_customItem214__c = jsonObject1.getString("customItem214__c");//内包装数
                        String bz_customItem227__c = jsonObject1.getString("customItem227__c");//内包装小计1
                        String bz_customItem221__c = jsonObject1.getString("customItem221__c");//标签规格1
                        String bz_customItem209__c = jsonObject1.getString("customItem209__c");//标签模板1
                        String bz_customItem223__c = jsonObject1.getString("customItem223__c");//标签装数1
                        String bz_customItem228__c = jsonObject1.getString("customItem228__c");//标签小计1
                        String bz_customItem222__c = jsonObject1.getString("customItem222__c");//标签规格2
                        String bz_customItem217__c = jsonObject1.getString("customItem217__c");//标签模板2
                        String bz_customItem224__c = jsonObject1.getString("customItem224__c");//标签装数2
                        String bz_customItem229__c = jsonObject1.getString("customItem229__c");//标签小计2

                        int customItem192__c1_int = StringUtils.isBlank(customItem192__c1) ? 0 : Integer.valueOf(customItem192__c1);
                        //校验批次是否存在
                        if (StringUtils.isNotBlank(customItem191__c1)){
                            Boolean aBoolean = checkCoiUf_LotIsExist(ERPtoken, customItem191__c1);
                            if (!aBoolean){
                                JSONObject object1=new JSONObject();
                                object1.put("id", id);
                                object1.put("customItem190__c", "批次不存在");
                                detailArray.add(object1);
                                continue;
                            }
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
                        String ShipSite = customItem212__c;//站点
                        String CoOrigSite = customItem212__c;//源站点
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
                        dataDetailObject.put("Disc", (100.00-Disc*100));
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
                        dataDetailObject.put("CoOrigSite", CoOrigSite);
                        dataDetailObject.put("DueDate", DueDate);
                        dataDetailObject.put("RefType", "J");
//                        dataDetailObject.put("oHXCoitemPacages.pt_num", oHXCoitemPacages_pt_num);
//                        dataDetailObject.put("oHXCoitemPacages.matl_qty", oHXCoitemPacages_matl_qty);
//                        dataDetailObject.put("oHXCoitemPacages.qty_package", oHXCoitemPacages_qty_package);
                        JSONArray allItems1 = getAllItems(dataDetailObject);
                        JSONArray propertyList1 = getPropertyList(dataDetailObject);
                        String slCoitems = null;
                        try {
                            slCoitems = addData(ERPtoken, "SLCoitems", allItems1, propertyList1);
                            if (slCoitems == null) {


                                //创建包装
                                JSONArray dataArray = new JSONArray();
                                int Sequence =0;
                                if (StringUtils.isNotBlank(bz_customItem207__c)){
                                    Sequence++;
                                    JSONObject dataObject1 = new JSONObject();
                                    dataObject1.put("Sequence", Sequence);//顺序
//                                dataObject1.put("co_num", CoNum);//客户订单编号 todo 暂不传
//                                dataObject1.put("co_line", CoLine);//客户订单行 todo 暂不传
//                                dataObject1.put("co_release", Sequence);//下达 todo 暂不传
//                                dataObject1.put("item", Item);//物料 todo 暂不传
//                                dataObject1.put("cust_num", CoCustNum);//客户编号 todo 暂不传
                                    dataObject1.put("specific", bz_customItem219__c);//规格
                                    dataObject1.put("pt_num", bz_customItem207__c);//包装模板号
                                    dataObject1.put("matl_qty", quantity);//内容量
                                    dataObject1.put("qty_package", bz_customItem215__c);//包装数
                                    dataObject1.put("DerSubTotal", bz_customItem225__c);//小计
                                    dataArray.add(dataObject1);
                                }
                                if (StringUtils.isNotBlank(bz_customItem208__c)){
                                    // bz_customItem220__c 外包规格2
                                    // bz_customItem208__c 外包模板2
                                    // bz_customItem216__c 外包装数2
                                    // bz_customItem226__c 外包装小计2
                                    Sequence++;
                                    JSONObject dataObject1 = new JSONObject();
                                    dataObject1.put("Sequence", Sequence);//顺序
//                                dataObject1.put("co_num", CoNum);//客户订单编号 todo 暂不传
//                                dataObject1.put("co_line", CoLine);//客户订单行 todo 暂不传
//                                dataObject1.put("co_release", Sequence);//下达 todo 暂不传
//                                dataObject1.put("item", Item);//物料 todo 暂不传
//                                dataObject1.put("cust_num", CoCustNum);//客户编号 todo 暂不传
                                    dataObject1.put("specific", bz_customItem220__c);//规格
                                    dataObject1.put("pt_num", bz_customItem208__c);//包装模板号
                                    dataObject1.put("matl_qty", quantity);//内容量
                                    dataObject1.put("qty_package", bz_customItem216__c);//包装数
                                    dataObject1.put("DerSubTotal", bz_customItem226__c);//小计
                                    dataArray.add(dataObject1);
                                }
                                if (StringUtils.isNotBlank(bz_customItem204__c)){

                                    // bz_customItem218__c //内包规格
                                    // bz_customItem204__c //内包模板
                                    // bz_customItem214__c //内包装数
                                    // bz_customItem227__c //内包装小计1
                                    Sequence++;
                                    JSONObject dataObject1 = new JSONObject();
                                    dataObject1.put("Sequence", Sequence);//顺序
//                                dataObject1.put("co_num", CoNum);//客户订单编号 todo 暂不传
//                                dataObject1.put("co_line", CoLine);//客户订单行 todo 暂不传
//                                dataObject1.put("co_release", Sequence);//下达 todo 暂不传
//                                dataObject1.put("item", Item);//物料 todo 暂不传
//                                dataObject1.put("cust_num", CoCustNum);//客户编号 todo 暂不传
                                    dataObject1.put("specific", bz_customItem218__c);//规格
                                    dataObject1.put("pt_num", bz_customItem204__c);//包装模板号
                                    dataObject1.put("matl_qty", quantity);//内容量
                                    dataObject1.put("qty_package", bz_customItem214__c);//包装数
                                    dataObject1.put("DerSubTotal", bz_customItem227__c);//小计
                                    dataArray.add(dataObject1);
                                }
                                if (StringUtils.isNotBlank(bz_customItem209__c)){

                                    // bz_customItem221__c //标签规格1
                                    // bz_customItem209__c //标签模板1
                                    // bz_customItem223__c //标签装数1
                                    // bz_customItem228__c //标签小计1
                                    Sequence++;
                                    JSONObject dataObject1 = new JSONObject();
                                    dataObject1.put("Sequence", Sequence);//顺序
//                                dataObject1.put("co_num", CoNum);//客户订单编号 todo 暂不传
//                                dataObject1.put("co_line", CoLine);//客户订单行 todo 暂不传
//                                dataObject1.put("co_release", Sequence);//下达 todo 暂不传
//                                dataObject1.put("item", Item);//物料 todo 暂不传
//                                dataObject1.put("cust_num", CoCustNum);//客户编号 todo 暂不传
                                    dataObject1.put("specific", bz_customItem221__c);//规格
                                    dataObject1.put("pt_num", bz_customItem209__c);//包装模板号
                                    dataObject1.put("matl_qty", quantity);//内容量
                                    dataObject1.put("qty_package", bz_customItem223__c);//包装数
                                    dataObject1.put("DerSubTotal", bz_customItem228__c);//小计
                                    dataArray.add(dataObject1);
                                }
                                if (StringUtils.isNotBlank(bz_customItem217__c)){

                                    // bz_customItem222__c //标签规格2
                                    // bz_customItem217__c //标签模板2
                                    // bz_customItem224__c //标签装数2
                                    // bz_customItem229__c //标签小计2
                                    Sequence++;
                                    JSONObject dataObject1 = new JSONObject();
                                    dataObject1.put("Sequence", Sequence);//顺序
//                                dataObject1.put("co_num", CoNum);//客户订单编号 todo 暂不传
//                                dataObject1.put("co_line", CoLine);//客户订单行 todo 暂不传
//                                dataObject1.put("co_release", Sequence);//下达 todo 暂不传
//                                dataObject1.put("item", Item);//物料 todo 暂不传
//                                dataObject1.put("cust_num", CoCustNum);//客户编号 todo 暂不传
                                    dataObject1.put("specific", bz_customItem222__c);//规格
                                    dataObject1.put("pt_num", bz_customItem217__c);//包装模板号
                                    dataObject1.put("matl_qty", quantity);//内容量
                                    dataObject1.put("qty_package", bz_customItem224__c);//包装数
                                    dataObject1.put("DerSubTotal", bz_customItem229__c);//小计
                                    dataArray.add(dataObject1);

                                    if (dataArray.size()>0){
                                        JSONArray allItems_bz = getAllItems(dataArray);
                                        JSONArray propertyList_bz = getPropertyList(dataArray.getJSONObject(0));
//                                    String hxCoitemPacages = addData(ERPtoken, "HXCoitemPacages", allItems_bz, propertyList_bz);
//                                    log.info("同步包装信息:"+hxCoitemPacages);
                                    }
                                }


                                JSONObject object1 = new JSONObject();
                                object1.put("id", id);
                                object1.put("customItem190__c", "已同步");
                                detailArray.add(object1);
                            }else {
                                JSONObject object1 = new JSONObject();
                                object1.put("id", id);
                                object1.put("customItem190__c", slCoitems);
                                detailArray.add(object1);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            JSONObject object1 = new JSONObject();
                            object1.put("id", id);
                            object1.put("customItem190__c", e.getMessage());
                            detailArray.add(object1);
                        }
                    }
                }else {
                    JSONObject object = new JSONObject();
                    object.put("id", orderId);
                    object.put("customItem201__c", slCustomers);
                    String post = queryServer.updateOrder(object);

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

//        @Scheduled(cron = "0/5 * * * * ?")
    @Scheduled(cron = "0 4/5 * * * ?")
    public void syncOrderProduct() {
        try {
//            String fieldsByBelongId_order = queryServer.getFieldsByBelongId(35L);
            String fieldsByBelongId_account = queryServer.getFieldsByBelongId(1L);
            StringBuilder sb = new StringBuilder();

            JSONArray detailArray = new JSONArray();
            //language=SQL
            String sql = "select id,po,customItem182__c,customItem213__c,customItem212__c,customItem214__c,customItem186__c,createdAt from _order where customItem201__c='同步成功' and customItem219__c>0 AND createdAt>1594051200000";
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
            Map<Long, JSONArray> orderProductInfo = getOrderProductInfoNoSync(sb.toString());
            for (int i = 0; i < all.size(); i++) {
                JSONObject jsonObject = all.getJSONObject(i);
                Long orderId = jsonObject.getLong("id");//订单id
                String CoNum = jsonObject.getString("po");
                String accountNum = jsonObject.getString("customItem186__c");//客户编号
                String customItem213__c = jsonObject.getString("customItem213__c");//账套（合同）
                String customItem212__c = jsonObject.getString("customItem212__c");//订单站点
                String customItem214__c = jsonObject.getString("customItem214__c");//订单站点
                Integer moneyType = jsonObject.getInteger("customItem182__c");
                String createdAt = jsonObject.getString("createdAt");//创建日期
                createdAt = createdAt.substring(0, 10);
                String replace_moneyType = valueToLabelUtil.replace(fieldsByBelongId_account, "customItem233__c", moneyType + "", "selectitem");
                String money = moneyJson.getString(replace_moneyType);
                money = StringUtils.isBlank(money) ? "CNY" : money;
                Date parse = df.parse(createdAt);
                Calendar instance = Calendar.getInstance();
                instance.setTime(parse);
                instance.add(Calendar.DAY_OF_YEAR, 10);
                Date time = instance.getTime();
                String DueDate = df.format(time);

                //调用接口,获取Sessiontoken
                String ERPtoken = idoWebServiceSoap.createSessionToken("crm", "Crm123456", customItem213__c);
                /////////////同步订单明细///////////
                //查询该订单下已同步订单明细
                int CoLineExisit=0;
                String result = idoWebServiceSoap.loadJson(ERPtoken, "SLCoitems", "CoNum,CoCustNum", "CoNum = '"+CoNum+"' and CoCustNum='"+accountNum+"'", "CoNum DESC", "", -1);
                System.out.println("订单下已同步订单明细查询结果："+result);
                JSONObject resultJson = JSONObject.parseObject(result);
                JSONArray Items = resultJson.getJSONArray("Items");
                CoLineExisit=Items.size();

                JSONArray jsonArray = orderProductInfo.get(orderId);
                for (int j = 0; j < jsonArray.size(); j++) {
                    JSONObject jsonObject1 = jsonArray.getJSONObject(j);
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
                    String customItem189__c = jsonObject1.getString("comment");//备注
//                        Double customItem186__c1 = jsonObject1.getDouble("customItem186__c");//内容量
//                        String customItem187__c_OrderProduct = jsonObject1.getString("customItem187__c");//包装模板
//                        Double customItem188__c1 = jsonObject1.getDouble("customItem188__c");//包装数
                    String customItem191__c1 = jsonObject1.getString("customItem191__c");//批次
                    String customItem192__c1 = jsonObject1.getString("customItem192__c");//分包标志
                    String customItem186__c1 = jsonObject1.getString("customItem186__c");//计量单位
                    String customItem193__c_1 = jsonObject1.getString("customItem193__c");//物料说明

                    String bz_customItem219__c = jsonObject1.getString("customItem219__c");//外包规格1
                    String bz_customItem207__c = jsonObject1.getString("customItem207__c");//外包模板1
                    String bz_customItem215__c = jsonObject1.getString("customItem215__c");//外包装数1
                    String bz_customItem225__c = jsonObject1.getString("customItem225__c");//外包装小计1
                    String bz_customItem220__c = jsonObject1.getString("customItem220__c");//外包规格2
                    String bz_customItem208__c = jsonObject1.getString("customItem208__c");//外包模板2
                    String bz_customItem216__c = jsonObject1.getString("customItem216__c");//外包装数2
                    String bz_customItem226__c = jsonObject1.getString("customItem226__c");//外包装小计2
                    String bz_customItem218__c = jsonObject1.getString("customItem218__c");//内包规格
                    String bz_customItem204__c = jsonObject1.getString("customItem204__c");//内包模板
                    String bz_customItem214__c = jsonObject1.getString("customItem214__c");//内包装数
                    String bz_customItem227__c = jsonObject1.getString("customItem227__c");//内包装小计1
                    String bz_customItem221__c = jsonObject1.getString("customItem221__c");//标签规格1
                    String bz_customItem209__c = jsonObject1.getString("customItem209__c");//标签模板1
                    String bz_customItem223__c = jsonObject1.getString("customItem223__c");//标签装数1
                    String bz_customItem228__c = jsonObject1.getString("customItem228__c");//标签小计1
                    String bz_customItem222__c = jsonObject1.getString("customItem222__c");//标签规格2
                    String bz_customItem217__c = jsonObject1.getString("customItem217__c");//标签模板2
                    String bz_customItem224__c = jsonObject1.getString("customItem224__c");//标签装数2
                    String bz_customItem229__c = jsonObject1.getString("customItem229__c");//标签小计2

                    int customItem192__c1_int = StringUtils.isBlank(customItem192__c1) ? 0 : Integer.valueOf(customItem192__c1);
                    //校验批次是否存在
                    if (StringUtils.isNotBlank(customItem191__c1)){
                        Boolean aBoolean = checkCoiUf_LotIsExist(ERPtoken, customItem191__c1);
                        if (!aBoolean){
                            JSONObject object1=new JSONObject();
                            object1.put("id", id);
                            object1.put("customItem190__c", "批次不存在");
                            detailArray.add(object1);
                            continue;
                        }
                    }

                    priceTotal = priceTotal == null ? 0 : priceTotal;
                    String CoNum_OrderProduct = CoNum;//订单号
                    String CoCustNum = customItem182__c;//客户编号
                    int CoLine = ++CoLineExisit;//订单行
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
                    dataDetailObject.put("Disc", (100.00-Disc*100));
                    dataDetailObject.put("coiUf_StandNum", coiUf_StandNum);
                    dataDetailObject.put("Stat", Stat_OrderProduct);
                    dataDetailObject.put("InvoiceHold", InvoiceHold);
                    dataDetailObject.put("AdrCurrCode", AdrCurrCode);
                    dataDetailObject.put("DerExtPrice", DerExtPrice);
                    dataDetailObject.put("coiUf_Lot", coiUf_Lot);
                    dataDetailObject.put("coiUf_Note", coiUf_Note);
                    dataDetailObject.put("coiUf_rework", coiUf_rework);
                    dataDetailObject.put("RefType", "J");
                    dataDetailObject.put("UM", UM);
                    dataDetailObject.put("Whse", Whse);
                    dataDetailObject.put("ShipSite", ShipSite);
                    dataDetailObject.put("CoOrigSite", ShipSite);
                    dataDetailObject.put("DueDate", DueDate);
//                        dataDetailObject.put("oHXCoitemPacages.pt_num", oHXCoitemPacages_pt_num);
//                        dataDetailObject.put("oHXCoitemPacages.matl_qty", oHXCoitemPacages_matl_qty);
//                        dataDetailObject.put("oHXCoitemPacages.qty_package", oHXCoitemPacages_qty_package);
                    JSONArray allItems1 = getAllItems(dataDetailObject);
                    JSONArray propertyList1 = getPropertyList(dataDetailObject);
                    String slCoitems = null;
                    try {
                        slCoitems = addData(ERPtoken, "SLCoitems", allItems1, propertyList1);
                        if (slCoitems == null) {
                            //创建包装
                            JSONArray dataArray = new JSONArray();
                            int Sequence =0;
                            if (StringUtils.isNotBlank(bz_customItem207__c)){
                                Sequence++;
                                JSONObject dataObject = new JSONObject();
                                dataObject.put("Sequence", Sequence);//顺序
//                                dataObject.put("co_num", Sequence);//客户订单编号 todo 暂不传
//                                dataObject.put("co_line", Sequence);//客户订单行 todo 暂不传
//                                dataObject.put("co_release", Sequence);//下达 todo 暂不传
//                                dataObject.put("item", Sequence);//物料 todo 暂不传
//                                dataObject.put("cust_num", Sequence);//客户编号 todo 暂不传
                                dataObject.put("specific", bz_customItem219__c);//规格
                                dataObject.put("pt_num", bz_customItem207__c);//包装模板号
                                dataObject.put("matl_qty", quantity);//内容量
                                dataObject.put("qty_package", bz_customItem215__c);//包装数
                                dataObject.put("DerSubTotal", bz_customItem225__c);//小计
                                dataArray.add(dataObject);
                            }
                            if (StringUtils.isNotBlank(bz_customItem208__c)){
                                // bz_customItem220__c 外包规格2
                                // bz_customItem208__c 外包模板2
                                // bz_customItem216__c 外包装数2
                                // bz_customItem226__c 外包装小计2
                                Sequence++;
                                JSONObject dataObject = new JSONObject();
                                dataObject.put("Sequence", Sequence);//顺序
//                                dataObject.put("co_num", Sequence);//客户订单编号 todo 暂不传
//                                dataObject.put("co_line", Sequence);//客户订单行 todo 暂不传
//                                dataObject.put("co_release", Sequence);//下达 todo 暂不传
//                                dataObject.put("item", Sequence);//物料 todo 暂不传
//                                dataObject.put("cust_num", Sequence);//客户编号 todo 暂不传
                                dataObject.put("specific", bz_customItem220__c);//规格
                                dataObject.put("pt_num", bz_customItem208__c);//包装模板号
                                dataObject.put("matl_qty", quantity);//内容量
                                dataObject.put("qty_package", bz_customItem216__c);//包装数
                                dataObject.put("DerSubTotal", bz_customItem226__c);//小计
                                dataArray.add(dataObject);
                            }
                            if (StringUtils.isNotBlank(bz_customItem204__c)){

                                // bz_customItem218__c //内包规格
                                // bz_customItem204__c //内包模板
                                // bz_customItem214__c //内包装数
                                // bz_customItem227__c //内包装小计1
                                Sequence++;
                                JSONObject dataObject = new JSONObject();
                                dataObject.put("Sequence", Sequence);//顺序
//                                dataObject.put("co_num", Sequence);//客户订单编号 todo 暂不传
//                                dataObject.put("co_line", Sequence);//客户订单行 todo 暂不传
//                                dataObject.put("co_release", Sequence);//下达 todo 暂不传
//                                dataObject.put("item", Sequence);//物料 todo 暂不传
//                                dataObject.put("cust_num", Sequence);//客户编号 todo 暂不传
                                dataObject.put("specific", bz_customItem218__c);//规格
                                dataObject.put("pt_num", bz_customItem204__c);//包装模板号
                                dataObject.put("matl_qty", quantity);//内容量
                                dataObject.put("qty_package", bz_customItem214__c);//包装数
                                dataObject.put("DerSubTotal", bz_customItem227__c);//小计
                                dataArray.add(dataObject);
                            }
                            if (StringUtils.isNotBlank(bz_customItem209__c)){

                                // bz_customItem221__c //标签规格1
                                // bz_customItem209__c //标签模板1
                                // bz_customItem223__c //标签装数1
                                // bz_customItem228__c //标签小计1
                                Sequence++;
                                JSONObject dataObject = new JSONObject();
                                dataObject.put("Sequence", Sequence);//顺序
//                                dataObject.put("co_num", Sequence);//客户订单编号 todo 暂不传
//                                dataObject.put("co_line", Sequence);//客户订单行 todo 暂不传
//                                dataObject.put("co_release", Sequence);//下达 todo 暂不传
//                                dataObject.put("item", Sequence);//物料 todo 暂不传
//                                dataObject.put("cust_num", Sequence);//客户编号 todo 暂不传
                                dataObject.put("specific", bz_customItem221__c);//规格
                                dataObject.put("pt_num", bz_customItem209__c);//包装模板号
                                dataObject.put("matl_qty", quantity);//内容量
                                dataObject.put("qty_package", bz_customItem223__c);//包装数
                                dataObject.put("DerSubTotal", bz_customItem228__c);//小计
                                dataArray.add(dataObject);
                            }
                            if (StringUtils.isNotBlank(bz_customItem217__c)){

                                // bz_customItem222__c //标签规格2
                                // bz_customItem217__c //标签模板2
                                // bz_customItem224__c //标签装数2
                                // bz_customItem229__c //标签小计2
                                Sequence++;
                                JSONObject dataObject = new JSONObject();
                                dataObject.put("Sequence", Sequence);//顺序
//                                dataObject.put("co_num", Sequence);//客户订单编号 todo 暂不传
//                                dataObject.put("co_line", Sequence);//客户订单行 todo 暂不传
//                                dataObject.put("co_release", Sequence);//下达 todo 暂不传
//                                dataObject.put("item", Sequence);//物料 todo 暂不传
//                                dataObject.put("cust_num", Sequence);//客户编号 todo 暂不传
                                dataObject.put("specific", bz_customItem222__c);//规格
                                dataObject.put("pt_num", bz_customItem217__c);//包装模板号
                                dataObject.put("matl_qty", quantity);//内容量
                                dataObject.put("qty_package", bz_customItem224__c);//包装数
                                dataObject.put("DerSubTotal", bz_customItem229__c);//小计
                                dataArray.add(dataObject);

                                if (dataArray.size()>0){
                                    JSONArray allItems_bz = getAllItems(dataArray);
                                    JSONArray propertyList_bz = getPropertyList(dataArray.getJSONObject(0));
//                                    String hxCoitemPacages = addData(ERPtoken, "HXCoitemPacages", allItems_bz, propertyList_bz);
//                                    log.info("同步包装信息:"+hxCoitemPacages);
                                }
                            }





                            JSONObject object1 = new JSONObject();
                            object1.put("id", id);
                            object1.put("customItem190__c", "已同步");
                            detailArray.add(object1);
                        }else {
                            JSONObject object1 = new JSONObject();
                            object1.put("id", id);
                            object1.put("customItem190__c", slCoitems);
                            detailArray.add(object1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JSONObject object1 = new JSONObject();
                        object1.put("id", id);
                        object1.put("customItem190__c", e.getMessage());
                        detailArray.add(object1);
                    }
                }
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
        String sqlPrefix = "select id,orderId,customItem182__c,customItem183__c,unitPrice,quantity,discount,customItem184__c,customItem185__c,priceTotal,comment,customItem191__c,customItem192__c,customItem193__c,customItem186__c from orderProduct where orderId in(";
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
    private Map<Long, JSONArray> getOrderProductInfoNoSync(String ids) throws Exception {
        Map<Long, JSONArray> map = new HashMap<>();
        String sqlPrefix = "select id,orderId,customItem182__c,customItem183__c,unitPrice,quantity,discount,customItem184__c,customItem185__c,priceTotal,comment,customItem191__c,customItem192__c,customItem193__c,customItem186__c,customItem219__c,customItem207__c,customItem215__c,customItem225__c,customItem220__c,customItem208__c,customItem216__c,customItem226__c,customItem218__c,customItem204__c,customItem214__c,customItem227__c,customItem221__c,customItem209__c,customItem223__c,customItem228__c,customItem222__c,customItem217__c,customItem224__c,customItem229__c from orderProduct where (customItem190__c is null or customItem190__c <> '已同步') and orderId in(";
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
        while (StringUtils.isNotBlank(ids)) {
            if (ids.length() > 500) {
                int index = 500;
                String substring = ids.substring(index, index + 1);
                while (!",".equals(substring)&&index<ids.length()){
                    index++;
                    if (index<ids.length()){
                        substring = ids.substring(index, index + 1);
                    }else {
                        substring = ids.substring(index, index);
                    }
                }
                String substring1 = ids.substring(0, index);
                sql = sqlPrefix + substring1 + sqlsuffix;
                String bySql3 = queryServer.getBySql(sql);
                JSONArray allresult = queryServer.findAll(getToken(), bySql3, sql);
                all3.addAll(allresult);
                if (index<ids.length()){
                    ids=ids.substring(index+1, ids.length());
                }else {
                    ids = ids.substring(index, ids.length());
                }
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
    /**
     * 拼接参数
     *
     * @param dataArray
     * @return
     */
    public JSONArray getAllItems(JSONArray dataArray) {
        JSONArray allItems = new JSONArray();
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject dataObject = dataArray.getJSONObject(i);
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
        }
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
        object.put("customItem237__c", false);
        String post = queryServer.updateAccount(object);
        return post;
    }
}
