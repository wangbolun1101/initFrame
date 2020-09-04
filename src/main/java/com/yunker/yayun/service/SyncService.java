package com.yunker.yayun.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yunker.yayun.controller.CommonController;
import com.yunker.yayun.log.ModuleOutputLogger;
import com.yunker.yayun.util.HttpClientUtil;
import com.yunker.yayun.util.QueryServer;
import mypackage.IDOWebService;
import mypackage.IDOWebServiceSoap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.xml.ws.Holder;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 客户同步 CRM->ERP
 */
@Service
@EnableScheduling
public class SyncService extends CommonController {
    @Autowired
    HttpClientUtil httpClientUtil;
    @Autowired
    QueryServer queryServer;


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
    //实例化接口
    private IDOWebService ST = new IDOWebService();
    private IDOWebServiceSoap idoWebServiceSoap = ST.getIDOWebServiceSoap();


    private String userId="crm";//用户名
    private String pswd="Crm123456";//密码
    private String config="LIVE_HXSW";//密码

    /**
     * 同步条款、资信额度
     */
    @Scheduled(cron = "0 0/5 * * * ? ")
    public void syncAccount2(){
        try {
            System.out.println("同步条款、资信额度--同步开始");
            ModuleOutputLogger.autoSyncCredit.info("同步条款、资信额度--同步开始");
            String CRMtoken = getToken();
            //调用接口,获取Sessiontoken
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
            String sql="select id,customItem201__c,customItem204__c,customItem206__c from account where customItem201__c is not null and customItem232__c = 2";
            Map map=new HashMap();
            map.put("xoql",sql);
            String post = httpClientUtil.post(CRMtoken, "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
            System.out.println("同步条款、资信额度--查询数据："+post);
            ModuleOutputLogger.autoSyncCredit.info("同步条款、资信额度--查询数据："+post);
            JSONObject object = JSONObject.parseObject(post);
            JSONArray jsonArray = object.getJSONObject("data").getJSONArray("records");
            if (jsonArray.size()<1){
                return;
            }
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                long accountId = jsonObject.getLongValue("id");
                String CustNum = jsonObject.getString("customItem201__c");
                if (StringUtils.isBlank(CustNum)){
                    continue;
                }
                String result = idoWebServiceSoap.loadJson(ERPtoken, "SLCustomers", "CreditLimit,TermsCode", "CustNum = '"+CustNum+"' and CustSeq='0'", "CustNum DESC", "", -1);
                System.out.println("同步条款、资信额度--ERP查询结果："+result);
                ModuleOutputLogger.autoSyncCredit.info("同步条款、资信额度--ERP查询结果："+result);
                JSONObject resultJson = JSONObject.parseObject(result);
                JSONArray Items = resultJson.getJSONArray("Items");
                if (Items.size()<1){
                    continue;
                }
                JSONObject jsonObject1 = Items.getJSONObject(0);
                String ID = jsonObject1.getString("ID");

                Float CreditLimit =getFloatData(jsonObject,"customItem204__c");//信用额度
                String TermsCode =getData(jsonObject,"customItem206__c");//条款
                JSONObject excute2 = super.provisionJson;
                TermsCode=excute2.getString(TermsCode);
                JSONObject dataObject=new JSONObject();
                BigDecimal bigDecimal = new BigDecimal(CreditLimit + "");
                dataObject.put("CreditLimit",bigDecimal.doubleValue());
                dataObject.put("TermsCode",TermsCode);

                JSONArray jsonArray1 = JSONArray.parseArray("[\"CreditLimit\",\"TermsCode\"]");
                String [] dataArray= jsonArray1.toArray(new String[0]);
                JSONArray allItems=new JSONArray();
                JSONArray items = getItems(dataObject, dataArray, jsonArray1);
                allItems.add(items);



                String slCustomers = updateData(ERPtoken, "SLCustomers", allItems, jsonArray1,ID);//更新客户
                System.out.println("同步条款、资信额度--ERP更新结果："+slCustomers);
                ModuleOutputLogger.autoSyncCredit.info("同步条款、资信额度--ERP更新结果："+slCustomers);

                //更新CRM客户
                JSONObject jsonObject2=new JSONObject();
                jsonObject2.put("id",accountId);
                jsonObject2.put("customItem232__c",1);
                String post1 = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/account/update", jsonObject2.toString());
            }

            System.out.println("同步条款、资信额度--同步结束");
            ModuleOutputLogger.autoSyncCredit.info("同步条款、资信额度--同步结束");
        }catch (Exception e){
            e.printStackTrace();
            System.err.println(e.getMessage());
            ModuleOutputLogger.autoSyncCreditError.error(e.getMessage());
        }
    }

    @Scheduled(cron = "0 0/5 * * * ? ")
    public void syncAccount1(){
        try {
            System.out.println("同步客户--同步开始");
            ModuleOutputLogger.autoSyncAccount.info("同步客户--同步开始");
            JSONObject excute8 = super.lastAccountTypeJson;
            String EndUserType1=excute8.getString("集团外国内-美元");//最终用户类型
            System.out.println("校验json文件读取是否成功："+EndUserType1);
            ModuleOutputLogger.autoSyncAccount.info("校验json文件读取是否成功："+EndUserType1);
            String CRMtoken = getToken();
            //                                                                       注册地址           国家                    省洲                             公司成立日期       经营渠道                    线上/线下         邮政编码   客户类型         资信额度          条款              信用冻结          最终用户类型      区域                   货币
            String sql="select id,customItem201__c,ownerId.employeeCode,ownerId.managerId,accountName,customItem181__c,customItem195__c,fState,customItem197__c,fCity,fDistrict,customItem210__c,customItem186__c,entityType,customItem199__c,zipCode,customItem205__c,customItem204__c,customItem206__c,customItem184__c,customItem190__c,customItem156__c,level,customItem233__c,customItem207__c,customItem208__c,customItem209__c,customItem211__c,customItem212__c,customItem213__c,customItem214__c,customItem218__c from account where customItem221__c >0 and approvalStatus = 3";
            Map map=new HashMap();
            map.put("xoql",sql);
            String post = httpClientUtil.post(CRMtoken, "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
            System.out.println("同步客户--查询结果："+post);
            ModuleOutputLogger.autoSyncAccount.info("同步客户--查询结果："+post);
            JSONObject object = JSONObject.parseObject(post);
            JSONArray jsonArray = object.getJSONObject("data").getJSONArray("records");
            if (jsonArray.size()<1){
                return;
            }
            Map<Integer,String>ERPMap=new HashMap<>();
            Map<Long,JSONArray>ERPDataMap=new HashMap<>();
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
            String ERPSql="select id,customItem3__c,customItem4__c from customEntity63__c where customItem2__c is null and customItem5__c<>'同步完成' and customItem3__c is not null";
            String bySql1 = queryServer.getBySql(ERPSql);
            JSONArray allERP = queryServer.findAll(getToken(), bySql1, ERPSql);
            for (int i = 0; i < allERP.size(); i++) {
                JSONObject jsonObject = allERP.getJSONObject(i);
                Long customItem4__c = jsonObject.getLong("customItem4__c");
                Integer customItem3__c = jsonObject.getInteger("customItem3__c");
                String s = ERPMap.get(customItem3__c);
                jsonObject.put("customItem3__c", s);
                JSONArray jsonArray1 = ERPDataMap.get(customItem4__c);
                if (jsonArray1==null){
                    jsonArray1=new JSONArray();
                }
                jsonArray1.add(jsonObject);
                ERPDataMap.put(customItem4__c, jsonArray1);
            }

            Map<Long,String> dataMap=new HashMap<>();
            String SQL="select id from account where customItem201__c is null and approvalStatus = 3";
            String bySql = getBySql(SQL);
            JSONArray all = queryServer.findAll(getToken(), bySql, SQL);
            for (int i = 0; i < all.size(); i++) {
                JSONObject jsonObject = all.getJSONObject(i);
                long id = jsonObject.getLongValue("id");
                dataMap.put(id,"true");
            }

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                long accountId = jsonObject.getLongValue("id");
                String status = dataMap.get(accountId);
                if (!"true".equals(status)){
                    continue;
                }
                String EndUserType=getArrayToData(jsonObject,"customItem190__c");//最终用户类型
                if (StringUtils.isBlank(EndUserType)){
                    continue;
                }
                long entityType = jsonObject.getLongValue("entityType");
                String accountType=entityType==7845639L?"F":"T";//客户类型 原料客户 T/终端客户 F
                if (StringUtils.isBlank(accountType)){
                    continue;
                }
                String countryType=EndUserType.contains("集团外国")?"T":EndUserType.contains("集团内")?"F":"";//集团外国内 T 集团内部部门客户  F
                String State="";//省州
                String Country=getSplitData(jsonObject,"customItem195__c");//国家
                JSONObject excute5 = super.countryJson;
                String sCountry=excute5.getString(Country);

                String flag="";//国内省 T 国外 F
                if ("CHN".equals(Country)){
                    flag="T";
                    if(entityType==749177010471194L||entityType==7845639L){
                        State = getArrayToData(jsonObject,"fState");
                    }else{
                        State = getArrayToData(jsonObject,"customItem197__c");
                    }
                }else{
                    State = getArrayToData(jsonObject,"customItem197__c");
                    flag="F";
                }
                JSONObject excute = super.provinceJson;
                State=excute.getString(State);
                JSONObject excute3 = super.countryHeadJson;
                String StateSpilt = excute3.getString(State);
                String startStr="";//客户编码头部省份标记
                if ("T".equals(flag)){
                    if ("T".equals(countryType)){//集团外国内
                        if (entityType==7845639L){
                            continue;//todo 终端客户暂不同步
                        }
                        String firstStr="J";//原料客户 T/终端客户 F  //"F".equals(accountType)?"H":
                        startStr=firstStr+StateSpilt;
                    }else if ("F".equals(countryType)){
                        startStr="N"+StateSpilt;
                    }
                }else if ("F".equals(flag)){//国外客户
                    startStr="K"+sCountry;
                }

                Integer CustSeq=0; //地址编号
                String Name=getData(jsonObject,"accountName");//客户名称
                String customItem181__c = getData(jsonObject, "customItem181__c");
                String Addr_1="";//注册地址1
                String Addr_2="";//注册地址2
                String Addr_3="";//注册地址3
                String Addr_4="";//注册地址4
                if (customItem181__c.length()>50){
                    Addr_1=customItem181__c.substring(0,50);
                    if (customItem181__c.length()>100){
                        Addr_2=customItem181__c.substring(50,100);
                        if (customItem181__c.length()>150){
                            Addr_3=customItem181__c.substring(100,150);
                            Addr_4=customItem181__c.substring(150,customItem181__c.length());
                        }else {
                            Addr_3=customItem181__c.substring(100,customItem181__c.length());
                        }
                    }else{
                        Addr_2=customItem181__c.substring(50,customItem181__c.length());
                    }
                }else{
                    Addr_1=customItem181__c;
                }
                String City = getArrayToData(jsonObject,"fCity");//城市
                String County=getArrayToData(jsonObject,"fDistrict");//县
                String uf_custype = getArrayToData(jsonObject,"customItem205__c");//客户类别
                String uf_saleway=getArrayToData(jsonObject,"customItem186__c");//经营渠道
                String uf_domfor=entityType==749179695907026L?"境外":entityType==749177010471194L?"境内":"其他";//境内/境外
                String uf_ifonline=getArrayToData(jsonObject,"customItem199__c");//线上/线下
                String uf_org1=getArrayToData(jsonObject,"customItem207__c");//一级机构类型
                String uf_org2=getArrayToData(jsonObject,"customItem208__c");//二级机构类型
                String uf_org3=getArrayToData(jsonObject,"customItem209__c");//三级机构类型
                String uf_createdate=geDateData(jsonObject,"customItem210__c");//公司成立日期
                String uf_regfund=getData(jsonObject,"customItem211__c");//注册资金
                String uf_salescale=getData(jsonObject,"customItem212__c");//经营规模
                String uf_lawsuit=getArrayToData(jsonObject,"customItem213__c");//近三年诉讼
                String uf_ifstop=getArrayToData(jsonObject,"customItem214__c");//是否停止合作
                String Zip=getData(jsonObject,"zipCode");//邮编
                String level = getArrayToData(jsonObject, "level");
                String CustType=level!=null?level.substring(0,1):null;//客户类型
                Float CreditLimit =getFloatData(jsonObject,"customItem204__c");//信用额度
                String TermsCode =getData(jsonObject,"customItem206__c");//条款
                JSONObject excute2 = super.provisionJson;
                TermsCode=excute2.getString(TermsCode);
                String CreditHold =jsonObject.getString("customItem184__c");//信用冻结
                String TerritoryCode=getSplitData(jsonObject,"customItem156__c");//区域

                Integer ShowInDropDownList=1;//ERP是否显示 该字段CRM不必显示，仅同步用

                String CurrCode=getArrayToData(jsonObject,"customItem233__c");//货币

                JSONObject excute4 = super.moneyJson;
                CurrCode=excute4.getString(CurrCode);

                String bu = getArrayToData(jsonObject, "customItem218__c");//事业部编号

                JSONObject dataObject=new JSONObject();
                if("CNY".equals(CurrCode)){
                    dataObject.put("BankCode","100");
                }else if("USD".equals(CurrCode)){
                    dataObject.put("BankCode","141");
                }else if("EUR".equals(CurrCode)){
                    dataObject.put("BankCode","171");
                }else if("JPY".equals(CurrCode)){
                    dataObject.put("BankCode","151");
                }
                JSONObject excute1 = super.lastAccountTypeJson;
                EndUserType=excute1.getString(EndUserType);//最终用户类型

                dataObject.put("CustSeq",CustSeq);
                dataObject.put("Name",Name);
                dataObject.put("Addr_1",Addr_1);
                dataObject.put("Addr_2",Addr_2);
                dataObject.put("Addr_3",Addr_3);
                dataObject.put("Addr_4",Addr_4);
                dataObject.put("Country",Country);
                dataObject.put("State",State);
                dataObject.put("City",City);
                dataObject.put("County",County);
                dataObject.put("Zip",Zip);
                dataObject.put("CustType",CustType);
                dataObject.put("CreditLimit",CreditLimit);
                dataObject.put("TermsCode",TermsCode);
                dataObject.put("CreditHold",CreditHold);
                dataObject.put("EndUserType",EndUserType);
                dataObject.put("TerritoryCode",TerritoryCode);
                dataObject.put("ShowInDropDownList",ShowInDropDownList);
                dataObject.put("CusShipmentApprovalRequired",1);
                dataObject.put("IncludeTaxInPrice",1);
                dataObject.put("CurrCode",CurrCode);
                if ("T".equals(flag)){//国内
                    dataObject.put("TaxCode1","13");
                }else if ("F".equals(flag)){//国外
                    dataObject.put("TaxCode1","0");
                }
                dataObject.put("uf_custype",uf_custype);
                dataObject.put("uf_saleway",uf_saleway);
                dataObject.put("uf_domfor",uf_domfor);
                dataObject.put("uf_ifonline",uf_ifonline);
                dataObject.put("uf_org1",uf_org1);
                dataObject.put("uf_org2",uf_org2);
                dataObject.put("uf_org3",uf_org3);
                dataObject.put("uf_createdate",uf_createdate);
                dataObject.put("uf_regfund",uf_regfund);
                dataObject.put("uf_salescale",uf_salescale);
                dataObject.put("uf_lawsuit",uf_lawsuit);
                dataObject.put("uf_ifstop",uf_ifstop);

//                JSONArray jsonArray1 = JSONArray.parseArray("[\"CustNum\",\"CustSeq\",\"Name\",\"Addr_1\",\"Addr_2\",\"Addr_3\",\"Addr_4\",\"Country\",\"State\",\"City\",\"County\",\"Zip\",\"CustType\",\"CreditLimit\",\"TermsCode\",\"CreditHold\",\"EndUserType\",\"TerritoryCode\",\"SalesTeamID\",\"cusUf_GlobalId\",\"ShowInDropDownList\",\"CusShipmentApprovalRequired\",\"IncludeTaxInPrice\",\"CurrCode\",\"TaxCode1\"]");

                JSONArray ERPDataArray = ERPDataMap.get(accountId);
                for (int j = 0; j < ERPDataArray.size(); j++) {
                    JSONObject ERPDataObject = ERPDataArray.getJSONObject(j);
                    String ERPConfig = ERPDataObject.getString("customItem3__c");
                    Long ERPDataId = ERPDataObject.getLong("id");

                    //调用接口,获取Sessiontoken
                    String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, ERPConfig);

                    String customNum = getCustomNum(ERPtoken, startStr);
                    int indexStart = customNum.indexOf("</Parameter><Parameter ByRef=\"Y\">")+"</Parameter><Parameter ByRef=\"Y\">".length();
                    int indexEnd = customNum.indexOf("</Parameter></Parameters>");
                    String CustNum=customNum.substring(indexStart,indexEnd);//客户编号

                    String SalesTeamID=CustNum;//销售团队
                    String cusUf_GlobalId=CustNum;//集团ID

                    dataObject.put("CustNum",CustNum);
                    dataObject.put("SalesTeamID",SalesTeamID);
                    dataObject.put("cusUf_GlobalId",cusUf_GlobalId);

                    JSONArray allItems = getAllItems(dataObject);
                    JSONArray propertyList = getPropertyList(dataObject);


                    /*销售团队客户所有人start*/
                    JSONObject userObject=new JSONObject();
                    String ref_num = getData(jsonObject, "ownerId.employeeCode");//客户所有人员工人力资源工号
                    JSONObject  TeamMemberObject=new JSONObject();
                    userObject.put("SalesTeamID",SalesTeamID);
                    TeamMemberObject.put("SalesTeamID",SalesTeamID);
                    userObject.put("Name",Name);
                    TeamMemberObject.put("RefNum",StringUtils.isNotBlank(ref_num)?("   "+Long.valueOf(ref_num)):0L);

                    JSONArray allUserArray=getAllItems(userObject);
                    JSONArray userFields=getPropertyList(userObject);

                    JSONArray alTeamMembers=getAllItems(TeamMemberObject);
                    JSONArray TeamMembers=getPropertyList(TeamMemberObject);
                    /*销售团队客户所有人end*/

                    /*销售团队直属上级start*/
                    JSONObject userObjec1t=new JSONObject();
                    Long managerId = StringUtils.isNotBlank(getData(jsonObject,"ownerId.managerId"))?Long.valueOf(getData(jsonObject,"ownerId.managerId")):0L;//直属上级员工人力资源工号
                    String ref_num1=getManagerId(managerId);
                    if (StringUtils.isNotBlank(ref_num1)){
                        userObjec1t.put("SalesTeamID",SalesTeamID);
                        userObjec1t.put("RefNum",StringUtils.isNotBlank(ref_num1)?("   "+Long.valueOf(ref_num1)):0L);

                        JSONArray userArray2 = getAllItems(userObjec1t);
                        alTeamMembers.addAll(userArray2);
                    }
                    /*销售团队直属上级end*/


                    /*CustToGlobals start*/
                    JSONObject  GlobalsObject=new JSONObject();
                    GlobalsObject.put("GlobalId",SalesTeamID);
                    GlobalsObject.put("GlobalName",Name);
                    JSONArray allGlobalsArray=getAllItems(GlobalsObject);
                    JSONArray GlobalsUserFields = getPropertyList(GlobalsObject);
                    /*CustToGlobals end*/


                    String slCustomers = addData(ERPtoken, "SLCustomers", allItems, propertyList);//同步客户
                    //更新CRM客户编号
                    String s = updateCustomerERPNo(ERPDataId, CustNum);
                    System.out.println("同步成功后回显客户编号："+s);
                    ModuleOutputLogger.autoSyncAccount.info("同步客户--同步成功后回显客户编号："+s);
                    String userResult = addData(ERPtoken, "SLSalesTeams", allUserArray, userFields);//同步销售团队
                    String TeamMembersResult = addData(ERPtoken, "SLSalesTeamMembers", alTeamMembers, TeamMembers);//同步销售团队
                    String GlobalsResult = addData(ERPtoken, "CustToGlobals", allGlobalsArray, GlobalsUserFields);//同步Globals

                    //同步分事业部信用额度
                    if (StringUtils.isNotBlank(bu)){
                        String[] buSplit = bu.split("-");
                        JSONObject otherCreditObject=new JSONObject();
                        otherCreditObject.put("custnum", CustNum);
                        otherCreditObject.put("bu", buSplit.length>0?buSplit[0]:null);
                        otherCreditObject.put("custgrade", "D");
                        otherCreditObject.put("custterm", "C00");
                        otherCreditObject.put("custcredit", 0);
                        ModuleOutputLogger.autoSyncAccount.info("分事业部信用额度同步参数："+otherCreditObject);
                        JSONArray otherCreditFields = JSONArray.parseArray("[\"custnum\",\"bu\",\"custgrade\",\"custterm\",\"custcredit\"]");
                        String[] otherCreditFieldsArray= otherCreditFields.toArray(new String[0]);

                        JSONArray otherCreditItems = getItems(otherCreditObject, otherCreditFieldsArray, otherCreditFields);//拼接数据
                        JSONArray allotherCreditItems=new JSONArray();
                        allotherCreditItems.add(otherCreditItems);
                        String otherCredits = addData(ERPtoken, "HXCustCredits", allotherCreditItems, otherCreditFields);//同步收货地址
                        ModuleOutputLogger.autoSyncAccount.info("分事业部信用额度同步结果："+otherCredits);
                    }
                }


                System.out.println("同步客户--同步结束");
                ModuleOutputLogger.autoSyncAccount.info("同步客户--同步结束");
            }

        }catch (Exception e){
            e.printStackTrace();
            System.err.println(e.getMessage());
            ModuleOutputLogger.autoSyncAccountError.error(e.getMessage());
        }
    }
    /**
     * 同步收货地址 CRM->ERP
     * @return
     */
    @Scheduled(cron = "0 0/5 * * * ? ")
    public void syncAaddress1(){
        Long AddressId=0L;
        Integer CustSeq=null;
        try {
            System.out.println("同步收货地址--同步开始");
            ModuleOutputLogger.autoSyncAddress.info("同步收货地址--同步开始");

            String sql="select id,customItem1__c.customItem233__c,customItem1__c.customItem201__c,name,customItem2__c,customItem3__c,customItem4__c,customItem5__c,customItem6__c,customItem7__c,customItem12__c from customEntity7__c where customItem8__c<>'同步成功'";
            Map map=new HashMap();
            map.put("xoql",sql);
            String CRMResult = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
            System.out.println("同步收货地址--查询结果："+CRMResult);
            ModuleOutputLogger.autoSyncAddress.info("同步收货地址--查询结果："+CRMResult);
            JSONObject CRMObject = JSONObject.parseObject(CRMResult);
            JSONArray CRMJsonArray = CRMObject.getJSONObject("data").getJSONArray("records");
            if (CRMJsonArray.size()<1){
                return;
            }

            for (int i = 0; i < CRMJsonArray.size(); i++) {
                JSONObject jsonObject = CRMJsonArray.getJSONObject(i);
                String ERPConfig = jsonObject.getString("customItem12__c");//账套
                //调用接口,获取Sessiontoken
                String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, ERPConfig);
                String CustNum = getData(jsonObject,"customItem1__c.customItem201__c");//客户编号
                if (StringUtils.isBlank(CustNum)){
                    continue;
                }

                long addressId = jsonObject.getLongValue("id");
                AddressId=addressId;
            /*
                根据客户编号查询收货人地址，确认地址编号
             */
                String result = idoWebServiceSoap.loadJson(ERPtoken, "SLCustomers", "CustSeq", "CustNum = '"+CustNum+"' and CustSeq <> '0'", "CustSeq DESC", "", -1);
                JSONObject resultJson = JSONObject.parseObject(result);
                JSONArray Items = resultJson.getJSONArray("Items");

                CustSeq=Items.size()+1;//地址编号
                String Name = getData(jsonObject,"name");//地址名称
                String customItem4__c = getData(jsonObject, "customItem4__c");//详细地址
                String Addr_1="";//注册地址1
                String Addr_2="";//注册地址2
                String Addr_3="";//注册地址3
                String Addr_4="";//注册地址4
                if (customItem4__c.length()>50){
                    Addr_1=customItem4__c.substring(0,50);
                    if (customItem4__c.length()>100){
                        Addr_2=customItem4__c.substring(50,100);
                        if (customItem4__c.length()>150){
                            Addr_3=customItem4__c.substring(100,150);
                            Addr_4=customItem4__c.substring(150,customItem4__c.length());
                        }else {
                            Addr_3=customItem4__c.substring(100,customItem4__c.length());
                        }
                    }else{
                        Addr_2=customItem4__c.substring(50,customItem4__c.length());
                    }
                }else{
                    Addr_1=customItem4__c;
                }
                String Country = getSplitData(jsonObject,"customItem5__c");//国家
                String State = getArrayToData(jsonObject,"customItem6__c");//省州
                JSONObject excute = super.provinceJson;
                State=excute.getString(State);
                String City = getArrayToData(jsonObject,"customItem7__c");//城市
                //县	 County todo 系统暂无该字段
                //邮编	Zip todo 系统暂无该字段
                String Contact_2 = getData(jsonObject,"customItem2__c");//收货联系人
                String Phone_2 = getData(jsonObject,"customItem3__c");//收货电话
                String CurrCode = getArrayToData(jsonObject,"customItem1__c.customItem233__c");//货币
                JSONObject excute4 = super.moneyJson;
                CurrCode=excute4.getString(CurrCode);
                JSONObject dataObject=new JSONObject();
                dataObject.put("CustNum",CustNum);
                dataObject.put("CustSeq",CustSeq);
                dataObject.put("Name",Name);
                dataObject.put("Addr_1",Addr_1);
                dataObject.put("Addr_2",Addr_2);
                dataObject.put("Addr_3",Addr_3);
                dataObject.put("Addr_4",Addr_4);
                dataObject.put("Country",Country);
                dataObject.put("State",State);
                dataObject.put("City",City);
            /*dataObject.put("County",County);//todo 系统暂无该字段
            dataObject.put("Zip",Zip);*///todo 系统暂无该字段
                dataObject.put("Contact_2",Contact_2);
                dataObject.put("Phone_2",Phone_2);
                dataObject.put("CurrCode",CurrCode);
                //字段
                JSONArray addressFields = JSONArray.parseArray("[\"CustNum\",\"CustSeq\",\"Name\",\"Addr_1\",\"Addr_2\",\"Addr_3\",\"Addr_4\",\"Country\",\"State\",\"City\",\"Contact_2\",\"Phone_2\",\"CurrCode\"]");//"County","Zip",
                String[] addressFieldsArray= addressFields.toArray(new String[0]);

                JSONArray items = getItems(dataObject, addressFieldsArray, addressFields);//拼接数据
                JSONArray allItems=new JSONArray();
                allItems.add(items);

                String slCustomers = addData(ERPtoken, "SLCustomers", allItems, addressFields);//同步收货地址
                System.out.println("同步收货地址--同步结果："+slCustomers);
                ModuleOutputLogger.autoSyncAddress.info("同步收货地址--同步结果："+slCustomers);
                if (slCustomers==null){
                    JSONObject object=new JSONObject();
                    object.put("id",addressId);
                    object.put("customItem8__c","同步成功");
                    object.put("erp_id__c",CustSeq);
                    String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", object.toString());
                }
                System.out.println("同步收货地址--同步结束");
                ModuleOutputLogger.autoSyncAddress.info("同步收货地址--同步结束");
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            ModuleOutputLogger.autoSyncAddressError.error(e.getMessage());
            JSONObject object=new JSONObject();
            object.put("id",AddressId);
            object.put("customItem8__c","同步失败");
            try {
                String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", object.toString());
            } catch (Exception e1) {
                e1.printStackTrace();
                ModuleOutputLogger.autoSyncAddressError.error(e1.getMessage());
            }
        }
        return ;
    }

    /**
     * 同步收票地址 CRM->ERP
     * @return
     */
    @Scheduled(cron = "0 0/5 * * * ? ")
    public void syncReceiptAddress1(){
        Long AddressId=0L;
        try {
            System.out.println("同步收票地址--同步开始");
            ModuleOutputLogger.autoSyncReceiotAddress.info("同步收票地址--同步开始");

            String sql="select id,customItem1__c.customItem201__c,name,customItem2__c,customItem3__c,customItem4__c,customItem5__c,customItem6__c,customItem7__c,customItem12__c from customEntity9__c where customItem8__c<>'同步成功'";
            Map map=new HashMap();
            map.put("xoql",sql);
            String CRMResult = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
            System.out.println("同步收票地址--查询结果："+CRMResult);
            ModuleOutputLogger.autoSyncReceiotAddress.info("同步收票地址--查询结果："+CRMResult);
            JSONObject CRMObject = JSONObject.parseObject(CRMResult);
            JSONArray CRMJsonArray = CRMObject.getJSONObject("data").getJSONArray("records");
            if (CRMJsonArray.size()<1){
                return;
            }
            for (int i = 0; i < CRMJsonArray.size(); i++) {
                JSONObject jsonObject = CRMJsonArray.getJSONObject(i);
                String ERPConfig = jsonObject.getString("customItem12__c");//账套
                //调用接口,获取Sessiontoken
                String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, ERPConfig);
                String DropShipNo = getData(jsonObject,"customItem1__c.customItem201__c");//客户编号
                if (StringUtils.isBlank(DropShipNo)){
                    continue;
                }
                long addressId = jsonObject.getLongValue("id");
                AddressId=addressId;
            /*
                根据客户编号查询收货人地址，确认地址编号
             */
                String result = idoWebServiceSoap.loadJson(ERPtoken, "SLShipTos", "DropSeq", "DropShipNo = '"+DropShipNo+"'", "DropShipNo DESC",
                        "", -1);
                System.out.println("同步收票地址--查询客户编号："+result);
                ModuleOutputLogger.autoSyncReceiotAddress.info("同步收票地址--查询客户编号："+result);
                JSONObject resultJson = JSONObject.parseObject(result);
                JSONArray Items = resultJson.getJSONArray("Items");

                Integer DropSeq=Items.size();//地址编号
                String Name = getData(jsonObject,"name");//地址名称
                String customItem4__c = getData(jsonObject, "customItem4__c");//详细地址
                String addr_1 = "";//详细地址1
                String addr_2 = "";//详细地址2
                String addr_3 = "";//详细地址3
                String addr_4 = "";//详细地址4
                if (customItem4__c.length()>50){
                    addr_1=customItem4__c.substring(0,50);
                    if (customItem4__c.length()>100){
                        addr_2=customItem4__c.substring(50,100);
                        if (customItem4__c.length()>150){
                            addr_3=customItem4__c.substring(100,150);
                            addr_4=customItem4__c.substring(150,customItem4__c.length());
                        }else {
                            addr_3=customItem4__c.substring(100,customItem4__c.length());
                        }
                    }else{
                        addr_2=customItem4__c.substring(50,customItem4__c.length());
                    }
                }else{
                    addr_1=customItem4__c;
                }
                String Country = getSplitData(jsonObject,"customItem5__c");//国家
                String State = getArrayToData(jsonObject,"customItem6__c");//省州
                JSONObject excute = super.provinceJson;
                State=excute.getString(State);
                String City = getArrayToData(jsonObject,"customItem7__c");//城市
                //县	 County todo 系统暂无该字段
                //邮编	Zip todo 系统暂无该字段
                String Contact = getData(jsonObject,"customItem2__c");//发票联系人
                String Phone = getData(jsonObject,"customItem3__c");//发票电话

                JSONObject dataObject=new JSONObject();
                dataObject.put("DropShipNo",DropShipNo);
                dataObject.put("DropSeq",DropSeq);
                dataObject.put("Name",Name);
                dataObject.put("addr_1",addr_1);
                dataObject.put("addr_2",addr_2);
                dataObject.put("addr_3",addr_3);
                dataObject.put("addr_4",addr_4);
                dataObject.put("Country",Country);
                dataObject.put("State",State);
                dataObject.put("City",City);
//          dataObject.put("County",County); todo 系统暂无该字段
//          dataObject.put("Zip",Zip); todo 系统暂无该字段
                dataObject.put("Contact",Contact);
                dataObject.put("Phone",Phone);

                JSONArray receiptAddressFields = JSONArray.parseArray("[\"DropShipNo\",\"DropSeq\",\"Name\",\"addr_1\",\"addr_2\",\"addr_3\",\"addr_4\",\"Country\",\"State\",\"City\",\"Contact\",\"Phone\"]");//"County","Zip",
                String[] fields= receiptAddressFields.toArray(new String[0]);

                JSONArray items = getItems(dataObject, fields, receiptAddressFields);//拼接数据
                JSONArray allItems=new JSONArray();
                allItems.add(items);

                String slCustomers = addData(ERPtoken, "SLShipTos", allItems, receiptAddressFields);//同步收票地址
                System.out.println("同步收票地址--同步结果："+slCustomers);
                ModuleOutputLogger.autoSyncReceiotAddress.info("同步收票地址--同步结果："+slCustomers);
                if (slCustomers==null){
                    JSONObject object=new JSONObject();
                    object.put("id",addressId);
                    object.put("customItem8__c","同步成功");
                    object.put("erp_id__c",DropSeq);
                    String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", object.toString());
                }
                System.out.println("同步收票地址--同步结束");
                ModuleOutputLogger.autoSyncReceiotAddress.info("同步收票地址--同步结束");
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            ModuleOutputLogger.autoSyncReceiotAddressError.error(e.getMessage());
            JSONObject object=new JSONObject();
            object.put("id",AddressId);
            object.put("customItem8__c","同步失败");
            try {
                String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", object.toString());
            } catch (Exception e1) {
                e1.printStackTrace();
                ModuleOutputLogger.autoSyncReceiotAddressError.error(e1.getMessage());
            }
        }
        return ;
    }
    /**
     * 同步联系人 CRM->ERP
     * @return
     */
    @Scheduled(cron = "0 0 0/2 * * ?")
    public void syncContact1(){
        Long sContactId=0L;
        try {
            System.out.println("同步联系人--同步开始");
            ModuleOutputLogger.autoSyncContact.info("同步联系人--同步开始");
            /*联系人数据start*/
            //调用接口,获取Sessiontoken
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);

//            String sql="select id,accountId.customItem201__c,contactName,customItem160__c,customItem158__c,customItem159__c,customItem150__c,phone,mobile,email from contact where customItem161__c<>'同步成功' ";
            String sql="select id,accountId.customItem201__c,contactName,customItem160__c,customItem150__c,phone,mobile,email from contact where customItem161__c<>'同步成功' and accountId.customItem201__c is not null";
            Map map=new HashMap();
            map.put("xoql",sql);
            String CRMResult = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
            System.out.println("同步联系人--查询结果："+CRMResult);
            ModuleOutputLogger.autoSyncContact.info("同步联系人--查询结果："+CRMResult);
            JSONObject CRMObject = JSONObject.parseObject(CRMResult);
            JSONArray CRMJsonArray = CRMObject.getJSONObject("data").getJSONArray("records");
            if (CRMJsonArray.size()<1){
                return;
            }
            for (int i = 0; i < CRMJsonArray.size(); i++) {
                JSONObject jsonObject = CRMJsonArray.getJSONObject(i);
                String CustNum=getData(jsonObject,"accountId.customItem201__c");;//客户编号
                if (StringUtils.isBlank(CustNum)){
                    ModuleOutputLogger.autoSyncContact.info("未查询到客户编号："+CustNum);
                    continue;
                }
                long contactId = jsonObject.getLongValue("id");
                sContactId=contactId;
                String ContactID=getData(jsonObject,"customItem160__c");//联系人编号 "自增列，可直接由CRM生成的客户编号，直接推到ERP"
                String contactName=getData(jsonObject,"contactName");//姓名
                if (StringUtils.isBlank(contactName)){
                    System.out.println("联系人姓名 不能为空");
                    ModuleOutputLogger.autoSyncContact.info("联系人姓名 不能为空");
                    continue;
                }
                String JobTitle=getArrayToData(jsonObject,"customItem150__c");//职务
                String OfficePhone=getData(jsonObject,"phone");//电话
                String MobilePhone=getData(jsonObject,"mobile");//手机
                String Email=getData(jsonObject,"email");//电子邮件
                JSONObject dataObject=new JSONObject();
                dataObject.put("ContactID",ContactID);
                contactName=contactName.trim();
                if (contactName.length()<=15){
                    dataObject.put("LName",contactName);
                    dataObject.put("FName",contactName);
                }else{
                    String[] split = contactName.split(" ");
                    if (split.length==1){
                        String s = split[0];
                        s=s.substring(0,15);
                        dataObject.put("LName",s);
                        dataObject.put("FName",s);
                    }else if (split.length>=2){
                        String s1 = split[0];
                        String s2 = split[1];
                        String LName="无";
                        String FName="无";
                        if (StringUtils.isBlank(s1)&&StringUtils.isNotBlank(s2)){
                            LName = s2.length()>15?(s2.substring(0,15)):s2;
                            FName = s2.length()>15?(s2.substring(0,15)):s2;
                        }else if(StringUtils.isBlank(s2)&&StringUtils.isNotBlank(s1)){
                            LName=s1.length()>15?(s1.substring(0,15)):s1;
                            FName=s1.length()>15?(s1.substring(0,15)):s1;
                        }else if (StringUtils.isNotBlank(s1)&&StringUtils.isNotBlank(s2)){
                            LName=s1.length()>15?(s1.substring(0,15)):s1;
                            FName=s2.length()>15?(s2.substring(0,15)):s2;
                        }
                        dataObject.put("LName",LName);
                        dataObject.put("FName",FName);
                    }
                }
                dataObject.put("JobTitle",JobTitle);
                dataObject.put("OfficePhone",StringUtils.isBlank(OfficePhone)?MobilePhone:OfficePhone);
                dataObject.put("MobilePhone",MobilePhone);
                dataObject.put("Email",Email);

                JSONArray contractFields = JSONArray.parseArray("[\"ContactID\",\"LName\",\"FName\",\"JobTitle\",\"OfficePhone\",\"MobilePhone\",\"Email\"]");
                String[] fields= contractFields.toArray(new String[0]);

                JSONArray items = getItems(dataObject, fields, contractFields);
                JSONArray allItems=new JSONArray();
                allItems.add(items);

                String slCustomers = addData(ERPtoken, "SLContacts", allItems, contractFields);//同步联系人
                System.out.println("同步联系人--同步结果："+slCustomers);
                ModuleOutputLogger.autoSyncContact.info("同步联系人--同步结果："+slCustomers);
                /*联系人数据end*/
                /*联系人客户关联start*/
                Integer CustSeq=0;//客户地址编号
                Long ContactId=Long.valueOf(ContactID);//联系人编号

                JSONObject DataObject=new JSONObject();
                DataObject.put("CustNum",CustNum);
                DataObject.put("CustSeq",CustSeq);
                DataObject.put("ContactId",ContactId);

                JSONArray CustomerContractArray = JSONArray.parseArray("[\"CustNum\",\"CustSeq\",\"ContactId\"]");
                String[] CustomerContractFields= CustomerContractArray.toArray(new String[0]);

                JSONArray CustomerContractitems = getItems(DataObject, CustomerContractFields, CustomerContractArray);
                JSONArray CustomerContractAllItems=new JSONArray();
                CustomerContractAllItems.add(CustomerContractitems);

                String sCustomerContract = addData(ERPtoken, "SLCustomerContacts", CustomerContractAllItems, CustomerContractArray);//同步联系人客户关联
                System.out.println("同步联系人客户关联--同步结果："+sCustomerContract);
                ModuleOutputLogger.autoSyncContact.info("同步联系人客户关联--同步结果："+sCustomerContract);
                /*联系人客户关联end*/
                JSONObject object=new JSONObject();
                object.put("id",contactId);
                object.put("customItem161__c","同步成功");
                String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/contact/update", object.toString());
            }
            System.out.println("同步联系人--同步结束");
            ModuleOutputLogger.autoSyncContact.info("同步联系人--同步结束");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            ModuleOutputLogger.autoSyncContactError.error("联系人同步失败："+e.getMessage());
            JSONObject object=new JSONObject();
            object.put("id",sContactId);
            object.put("customItem161__c","同步失败");
            try {
                String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/contact/update", object.toString());
            } catch (Exception e1) {
                e1.printStackTrace();
                ModuleOutputLogger.autoSyncContactError.error(e1.getMessage());
            }
        }
        return ;
    }

    @Scheduled(cron = "0 0/5 * * * ? ")
    public void syncBankAccount1(){
        Long BankId=0L;
        try{
            System.out.println("同步账户信息--同步开始");
            ModuleOutputLogger.autoSyncBankInfo.info("同步账户信息--同步开始");
            //调用接口,获取Sessiontoken
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);

            String sql="select id,customItem1__c.customItem195__c,customItem1__c.customItem201__c,customItem4__c,customItem5__c,customItem6__c,customItem16__c,customItem15__c,customItem14__c,customItem9__c,customItem11__c,customItem7__c,customItem8__c from customEntity6__c where customItem19__c<>'同步成功'";
            Map map=new HashMap();
            map.put("xoql",sql);
            String CRMResult = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
            System.out.println("同步账户信息--查询结果："+CRMResult);
            ModuleOutputLogger.autoSyncBankInfo.info("同步账户信息--查询结果："+CRMResult);
            JSONObject CRMObject = JSONObject.parseObject(CRMResult);
            JSONArray CRMJsonArray = CRMObject.getJSONObject("data").getJSONArray("records");
            if (CRMJsonArray.size()<1){
                return;
            }
            for (int i = 0; i < CRMJsonArray.size(); i++) {
                JSONObject jsonObject = CRMJsonArray.getJSONObject(i);
                String CustNum = getData(jsonObject, "customItem1__c.customItem201__c");//客户编号
                String country = getSplitData(jsonObject,"customItem1__c.customItem195__c");//国家
                if (StringUtils.isBlank(CustNum)){
                    continue;
                }
                long bankId = jsonObject.getLongValue("id");
                BankId=bankId;

                String cusUf_Bank=getData(jsonObject,"customItem4__c");//开户行
                String cusUf_BankAcct=getData(jsonObject,"customItem5__c");//银行账户
                String cusUF_ReservedField1=getData(jsonObject,"customItem6__c");//纳税人识别号
                String cusUf_Note=getData(jsonObject,"customItem16__c");//开票特殊要求
//           String InvCategory=getArrayToData(jsonObject,"customItem15__c");//发票类别
                String InvCategory="DefaultCategory";//发票类别
                String PayType=getArrayToData(jsonObject,"customItem14__c");//付款类型
                String TaxCode1=getArrayToData(jsonObject,"customItem9__c");//税率
                JSONObject excute2 = super.taxCodeJson;
                TaxCode1=excute2.getString(TaxCode1);
                if (StringUtils.isBlank(TaxCode1)){
                    if ("CHN".equals(country)){
                        TaxCode1="13";
                    }else{
                        TaxCode1="0";
                    }
                }
                String BankCode=getArrayToData(jsonObject,"customItem11__c");//银行码
                JSONObject excute1 = super.bankCodeJson;
                BankCode=excute1.getString(BankCode);
                if(StringUtils.isBlank(BankCode)){
                    BankCode="BK1";
                }
                String Charfld3=getData(jsonObject,"customItem7__c");//开票电话 后加
                String Charfld2=getData(jsonObject,"customItem8__c");//开票地址 后加



                String result = idoWebServiceSoap.loadJson(ERPtoken, "SLCustomers", "cusUf_Bank,cusUf_BankAcct,cusUF_ReservedField1,cusUf_Note,InvCategory,PayType,TaxCode1,BankCode,Charfld3,Charfld2", "CustNum = '"+CustNum+"' and CustSeq='0'", "CustNum DESC", "", -1);
                System.out.println("同步账户信息--ERP查询结果："+result);
                ModuleOutputLogger.autoSyncBankInfo.info("同步账户信息--ERP查询结果："+result);
                JSONObject resultJson = JSONObject.parseObject(result);
                JSONArray Items = resultJson.getJSONArray("Items");
                if (Items.size()<1){
                    continue;
                }
                JSONObject object = super.payTypeJson;
                JSONObject jsonObject1 = Items.getJSONObject(0);
                String ID = jsonObject1.getString("ID");
                JSONObject dataObject=new JSONObject();
                dataObject.put("cusUf_Bank",cusUf_Bank);
                dataObject.put("cusUf_BankAcct",cusUf_BankAcct);
                dataObject.put("cusUF_ReservedField1",cusUF_ReservedField1);
                dataObject.put("cusUf_Note",cusUf_Note);
                dataObject.put("InvCategory",InvCategory);
                dataObject.put("PayType",object.getString(PayType));
                dataObject.put("TaxCode1",TaxCode1);
                dataObject.put("BankCode",BankCode);
                dataObject.put("Charfld3",Charfld3);
                dataObject.put("Charfld2",Charfld2);

                JSONArray fieldArray=JSONArray.parseArray("[\"cusUf_Bank\",\"cusUf_BankAcct\",\"cusUF_ReservedField1\",\"cusUf_Note\",\"InvCategory\",\"PayType\",\"TaxCode1\",\"BankCode\",\"Charfld3\",\"Charfld2\"]");
                String[] fields= fieldArray.toArray(new String[0]);

                JSONArray items = getItems(dataObject, fields, fieldArray);
                JSONArray allItems=new JSONArray();
                allItems.add(items);

                String slCustomers = updateData(ERPtoken, "SLCustomers", allItems, fieldArray, ID);
                System.out.println("同步账户信息--更新结果："+slCustomers);
                ModuleOutputLogger.autoSyncBankInfo.info("同步账户信息--更新结果："+slCustomers);
                JSONObject object1=new JSONObject();
                object1.put("id",bankId);
                object1.put("customItem19__c","同步成功");
                String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", object1.toString());
            }
            System.out.println("同步账户信息--同步结束");
            ModuleOutputLogger.autoSyncBankInfo.info("同步账户信息--同步结束");

        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            ModuleOutputLogger.autoSyncBankInfoError.error(e.getMessage());
            JSONObject object=new JSONObject();
            object.put("id",BankId);
            object.put("customItem19__c","同步失败");
            try {
                String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", object.toString());
            } catch (Exception e1) {
                e1.printStackTrace();
                ModuleOutputLogger.autoSyncBankInfoError.error(e1.getMessage());
            }

        }
        return;
    }

    /**
     * 获取最新ERP客户编号
     * @param token
     * @param startStr
     * @return
     */
    public String getCustomNum(String token,String startStr){
        Holder<String> pa = new Holder<>("<Parameters><Parameter>"+startStr+"</Parameter><Parameter ByRef=\"Y\"></Parameter></Parameters>");
        Holder<Object> pa2 = new Holder<>();
        idoWebServiceSoap.callMethod(token, "SP!", "HXGenCustNum", pa, pa2);
        //输出返回结果
        System.err.println(pa.value);
        ModuleOutputLogger.autoSyncAccount.info("最新客户编号："+pa.value);
        return  pa.value;
    }

    /**
     * 更新客户编号 ERP->CRM
     * @param ERPDataId
     * @param customNum
     * @throws Exception
     */
    public String updateCustomerERPNo(Long ERPDataId,String customNum) throws Exception {
        JSONObject object=new JSONObject();
        object.put("id",ERPDataId);
        object.put("customItem2__c",customNum);
        object.put("customItem5__c  ","同步完成");
        String post = queryServer.updateCustomizeByIdNoThrowException(object);
        return post;
    }

    /**
     * 查询员工人力资源工号
     * @param managerId
     * @return
     * @throws Exception
     */
    public String getManagerId(Long managerId) throws Exception {
        String sql="select employeeCode from user where id="+managerId;
        Map map=new HashMap();
        map.put("q",sql);
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/query", map);
        JSONObject object = JSONObject.parseObject(post);
        JSONArray records = object.getJSONArray("records");
        if (records.size()==0){
            return "";
        }
        String employeeCode = records.getJSONObject(0).getString("employeeCode");
        return employeeCode;
    }

    /**
     * 拼接参数
     * @param dataObject
     * @param fields
     * @param fieldArray
     * @return
     */
    public JSONArray getItems(JSONObject dataObject,String[] fields,JSONArray fieldArray){
        JSONArray Items=new JSONArray();
        for (int i = 0; i < dataObject.size(); i++) {
            String fieldName = fields[i];
            Object data = dataObject.get(fieldName);
            JSONObject object1=new JSONObject();
            object1.put("Property",data);
            if (data==null||"".equals(data+"")){
                fieldArray.remove(fieldName);
                continue;
            }
            if ("CustNum".equals(fieldName)||"CustSeq".equals(fieldName)||"ref_num".equals(fieldName)||"DropShipNo".equals(fieldName)||"DropSeq".equals(fieldName)||"ContactID".equals(fieldName)){
                object1.put("Updated",false);
            }else{
                object1.put("Updated",true);
            }
            Items.add(object1);
        }
        return Items;
    }



    private JSONArray getPropertyList(JSONObject dataObject) {
        JSONArray propertyList=new JSONArray();
        Set<Map.Entry<String, Object>> entries = dataObject.entrySet();
        entries.forEach(map->{
            String key = map.getKey();
            propertyList.add(key);
        });
        return propertyList;
    }

    /**
     * 拼接参数
     * @param dataObject
     * @return
     */
    public JSONArray getAllItems(JSONObject dataObject){
        JSONArray allItems=new JSONArray();
        JSONArray Items=new JSONArray();
        Set<Map.Entry<String, Object>> entries = dataObject.entrySet();
        entries.forEach(map->{
            String fieldName = map.getKey();
            Object data = map.getValue();
            JSONObject object1=new JSONObject();
            object1.put("Property",data);
            if ("CustNum".equals(fieldName)||"CustSeq".equals(fieldName)||"ref_num".equals(fieldName)||"DropShipNo".equals(fieldName)||"DropSeq".equals(fieldName)||"ContactID".equals(fieldName)){
                object1.put("Updated",false);
            }else{
                object1.put("Updated",true);
            }
            Items.add(object1);
        });
        allItems.add(Items);
        return allItems;
    }


    public String getData(JSONObject dataObject,String fieldName){
        String data=null;
        try {
            data = dataObject.getString(fieldName);
        }catch (Exception e){
            data=null;
        }
        return data;
    }
    public String getArrayToData(JSONObject dataObject,String fieldName){
        String data=null;
        try {
            data =(String)dataObject.getJSONArray(fieldName).toArray()[0];
        }catch (Exception e){
            data=null;
        }
        return data;
    }
    public String getSplitData(JSONObject dataObject,String fieldName){
        String data=null;
        try {
            String [] daataSplit = dataObject.getJSONArray(fieldName).toArray(new String[0]);
            for (int i = 0; i < daataSplit.length; i++) {
                String dataString = daataSplit[i];
                if (StringUtils.isBlank(dataString)||dataString.contains("停用")){
                    continue;
                }
                if (dataString.contains("\t")){
                    data = dataString.split("\t")[0];
                }else if(dataString.contains("_")){
                    data = dataString.split("_")[0];
                }else{
                    data = dataString.split(" ")[0];
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            data=null;
        }
        return data;
    }
    public Float getFloatData(JSONObject dataObject,String fieldName){
        Float data=0F;
        try {
            data =dataObject.getFloatValue(fieldName);
        }catch (Exception e){
            data=0F;
        }
        return data;
    }
    public String geDateData(JSONObject dataObject,String fieldName){
        Calendar instance = Calendar.getInstance();
        DateFormat df=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String dataTime=null;
        try {
            Long dataLong =dataObject.getLongValue(fieldName);
            instance.setTimeInMillis(dataLong);
            Date date = instance.getTime();
            dataTime=df.format(date);
        }catch (Exception e){
            dataTime=null;
        }
        return dataTime;
    }

    /**
     * 保存方法
     * @param toKen
     * @param IDOName
     * @param Properties
     * @param PropertyList
     * @return
     */
    public String addData(String toKen, String IDOName, JSONArray Properties, JSONArray PropertyList){
        JSONObject object=new JSONObject();
        object.put("IDOName",IDOName);
        JSONArray Items = new JSONArray();
        for (int i = 0; i < Properties.size(); i++) {
            JSONObject object1=new JSONObject();
            object1.put("Properties",Properties.getJSONArray(i));
            object1.put("EditStatus",3);
            Items.add(object1);
        }
        object.put("Items",Items);
        object.put("PropertyList",PropertyList);
        String result = idoWebServiceSoap.saveJson(toKen,object.toJSONString(),"" , "", "");
        return result;
    }
    /**
     * 更新方法
     * @param toKen
     * @param IDOName
     * @param Properties
     * @param PropertyList
     * @return
     */
    public String updateData(String toKen, String IDOName, JSONArray Properties, JSONArray PropertyList, String ID){
        JSONObject object=new JSONObject();
        object.put("IDOName",IDOName);
        JSONArray Items = new JSONArray();
        for (int i = 0; i < Properties.size(); i++) {
            JSONObject object1=new JSONObject();
            object1.put("Properties",Properties.getJSONArray(i));
            object1.put("EditStatus",0);
            object1.put("ID",ID);
            Items.add(object1);
        }
        object.put("Items",Items);
        object.put("PropertyList",PropertyList);
        String result = idoWebServiceSoap.saveJson(toKen,object.toJSONString(),"" , "", "");
        return result;
    }
    /**
     * 查询公共方法
     */
    public String getBySql(String sql) throws Exception {
        Map map=new HashMap();
        map.put("q",sql);
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/query", map);
        JSONObject object1 = JSONObject.parseObject(post);
        if (post.contains("error_code")){
            String message = object1.getString("message");
            String code = object1.getString("error_code");
            throw new Exception("错误码："+code+"  "+message);
        }
        return post;
    }


}