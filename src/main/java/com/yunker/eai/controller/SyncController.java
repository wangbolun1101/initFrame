package com.yunker.eai.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yunker.eai.util.HttpClientUtil;
import com.yunker.eai.log.ModuleOutputLogger;
import com.yunker.eai.util.QueryServer;
import mypackage.IDOWebService;
import mypackage.IDOWebServiceSoap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.xml.ws.Holder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 全量同步入口类
 */

@Controller
@CrossOrigin(origins = "https://login.xiaoshouyi.com", maxAge = 3600)
@RequestMapping("/syncData")
public class SyncController extends CommonController{

    @Autowired
    private HttpClientUtil httpClientUtil;
    @Autowired
    QueryServer queryServer;

    private JSONObject returnObject;
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
    private String config="LIVE_HXSW";//账套

    @GetMapping("/getToken")
    @ResponseBody
    public String getCRMToken(){
        try {
            String token = getToken();
            return sendJson(token, true,"");
        } catch (Exception e) {
            e.printStackTrace();
            return sendJson("", false,e.getMessage());
        }

    }

    /**
     * 同步客户 CRM->ERP
     */
    @RequestMapping("/syncAccount")
    @ResponseBody
    public JSONObject syncAccount(@RequestBody JSONObject param){

       try {
           Long accountId=param.getLongValue("accountId");
           System.out.println("客户id："+accountId);
           ModuleOutputLogger.handSyncAccount.info("客户id："+accountId);
           String CRMtoken = getToken();
           //调用接口,获取Sessiontoken
           String sql="select id,ownerId.employeeCode,ownerId.managerId,accountName,customItem181__c,customItem195__c,fState,customItem197__c,fCity,fDistrict,customItem210__c,customItem186__c,entityType,customItem199__c,zipCode,customItem205__c,customItem204__c,customItem206__c,customItem184__c,customItem190__c,customItem156__c,level,customItem233__c,customItem207__c,customItem208__c,customItem209__c,customItem211__c,customItem212__c,customItem213__c,customItem214__c,customItem218__c,customItem194__c from account where customItem201__c is null and approvalStatus = 3";
           Map map=new HashMap();
           map.put("xoql",sql);
           String post = httpClientUtil.post(CRMtoken, "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
           System.out.println(post);
           JSONObject object = JSONObject.parseObject(post);
           JSONArray jsonArray = object.getJSONObject("data").getJSONArray("records");
           if (jsonArray.size()<1){
               return null;
           }
           JSONObject jsonObject = jsonArray.getJSONObject(0);
           Map<Integer,String>ERPMap=new HashMap<>();
           Map<Long,JSONArray>ERPDataMap=new HashMap<>();
           String fieldsByBelongId = queryServer.getFieldsByBelongId(1340810097180997L);
           JSONObject fieldsObject1 = JSONObject.parseObject(fieldsByBelongId);
           JSONArray fields = fieldsObject1.getJSONArray("fields");
           for (int i = 0; i < fields.size(); i++) {
               JSONObject fieldsJsonObject = fields.getJSONObject(i);
               String propertyname = fieldsJsonObject.getString("propertyname");
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
           String ERPSql="select id,customItem3__c,customItem4__c from customEntity63__c where customItem2__c is null and customItem5__c<>'同步完成' and customItem3__c is not null and customItem4__c="+accountId;
           String bySql1 = queryServer.getBySql(ERPSql);
           JSONArray allERP = queryServer.findAll(getToken(), bySql1, ERPSql);
           for (int i = 0; i < allERP.size(); i++) {
               JSONObject jsonObject1 = allERP.getJSONObject(i);
               Long customItem4__c = jsonObject1.getLong("customItem4__c");
               Integer customItem3__c = jsonObject1.getInteger("customItem3__c");
               String s = ERPMap.get(customItem3__c);
               jsonObject1.put("customItem3__c", s);
               JSONArray jsonArray1 = ERPDataMap.get(customItem4__c);
               if (jsonArray1==null){
                   jsonArray1=new JSONArray();
               }
               jsonArray1.add(jsonObject1);
               ERPDataMap.put(customItem4__c, jsonArray1);
           }




           String EndUserType=getArrayToData(jsonObject,"customItem190__c");//最终用户类型

           long entityType = jsonObject.getLongValue("entityType");
           String accountType=entityType==7845639L?"F":"T";//客户类型 原料客户 T/终端客户 F
           if (StringUtils.isBlank(EndUserType)){
               return null;
           }
           String countryType=EndUserType.contains("集团外")?"T":EndUserType.contains("集团内")?"F":"";//集团外国内 T 集团内部部门客户  F
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
                       return null;//todo 终端客户暂不同步
                   }
                   String firstStr="J";//原料客户 T/终端客户 F
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
           String ReservedField1 = jsonObject.getString("customItem194__c");//纳税识别号


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
           dataObject.put("ReservedField1",ReservedField1);

           String ERPtoken1 = idoWebServiceSoap.createSessionToken(userId, pswd, config);
           String customNum = getCustomNum(ERPtoken1, startStr);
           int indexStart = customNum.indexOf("</Parameter><Parameter ByRef=\"Y\">")+"</Parameter><Parameter ByRef=\"Y\">".length();
           int indexEnd = customNum.indexOf("</Parameter></Parameters>");
           String CustNum=customNum.substring(indexStart,indexEnd);//客户编号
           JSONArray ERPDataArray = ERPDataMap.get(accountId);
           for (int j = 0; j < ERPDataArray.size(); j++) {
               JSONObject ERPDataObject = ERPDataArray.getJSONObject(j);
               String ERPConfig = ERPDataObject.getString("customItem3__c");
               Long ERPDataId = ERPDataObject.getLong("id");

               //调用接口,获取Sessiontoken
               String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, ERPConfig);
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
           String s = updateAccountERPNo(accountId, CustNum);

//           //同步分事业部信用额度
//           if (StringUtils.isNotBlank(bu)){
//               String[] buSplit = bu.split("-");
//               JSONObject otherCreditObject=new JSONObject();
//               otherCreditObject.put("custnum", CustNum);
//               otherCreditObject.put("bu", buSplit.length>0?buSplit[0]:null);
//               otherCreditObject.put("custgrade", "D");
//               otherCreditObject.put("custterm", "C00");
//               otherCreditObject.put("custcredit", 0);
//               ModuleOutputLogger.autoSyncAccount.info("分事业部信用额度同步参数："+otherCreditObject);
//               JSONArray otherCreditFields = JSONArray.parseArray("[\"custnum\",\"bu\",\"custgrade\",\"custterm\",\"custcredit\"]");
//               String[] otherCreditFieldsArray= otherCreditFields.toArray(new String[0]);
//
//               JSONArray otherCreditItems = getItems(otherCreditObject, otherCreditFieldsArray, otherCreditFields);//拼接数据
//               JSONArray allotherCreditItems=new JSONArray();
//               allotherCreditItems.add(otherCreditItems);
//               String otherCredits = addData(ERPtoken, "HXCustCredits", allotherCreditItems, otherCreditFields);//同步收货地址
//               ModuleOutputLogger.autoSyncAccount.info("分事业部信用额度同步结果："+otherCredits);
//           }
//
//
//           String adsSql="select id from customEntity7__c where customItem1__c="+accountId+" and customItem8__c<>'同步成功'";
//           Map map1=new HashMap();
//           map1.put("q",adsSql);
//           String post1 = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/query", map1);
//           System.out.println("客户下未同步收货地址："+post1);
//           ModuleOutputLogger.handSyncAccount.info("客户下未同步收货地址："+post1);
//           JSONObject object1 = JSONObject.parseObject(post1);
//           JSONArray records = object1.getJSONArray("records");
//           for (int i = 0; i < records.size(); i++) {
//               JSONObject jsonObject1 = records.getJSONObject(i);
//               jsonObject1.put("addressId",jsonObject1.getLongValue("id"));
//               String s = syncAaddress(jsonObject1);
//           }
//           String adsRectSql="select id from customEntity9__c where customItem1__c="+accountId+" and customItem8__c<>'同步成功'";
//           Map map2=new HashMap();
//           map2.put("q",adsRectSql);
//           String post2 = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/query", map2);
//           System.out.println("客户下未同步收票地址："+post2);
//           ModuleOutputLogger.handSyncAccount.info("客户下未同步收票地址："+post2);
//           JSONObject object2 = JSONObject.parseObject(post2);
//           JSONArray records1 = object2.getJSONArray("records");
//           for (int i = 0; i < records1.size(); i++) {
//               JSONObject jsonObject1 = records1.getJSONObject(i);
//               jsonObject1.put("addressId",jsonObject1.getLongValue("id"));
//               String s = syncReceiptAddress(jsonObject1);
//           }
//           String BankSql="select id from customEntity6__c where customItem1__c="+accountId+" and customItem19__c<>'同步成功'";
//           Map map3=new HashMap();
//           map3.put("q",BankSql);
//           String post3 = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/query", map3);
//           System.out.println("客户下未同步账户信息："+post3);
//           ModuleOutputLogger.handSyncAccount.info("客户下未同步账户信息："+post3);
//           JSONObject object3 = JSONObject.parseObject(post3);
//           JSONArray records2 = object3.getJSONArray("records");
//           for (int i = 0; i < records2.size(); i++) {
//               JSONObject jsonObject1 = records2.getJSONObject(i);
//               jsonObject1.put("bankId",jsonObject1.getLongValue("id"));
//               String s = syncBankAccount(jsonObject1);
//           }
//           String contactSql="select id from contact where accountId="+accountId+" and customItem161__c<>'同步成功'";
//           Map map4=new HashMap();
//           map4.put("q",BankSql);
//           String post4 = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/query", map4);
//           System.out.println("客户下未同步联系人："+post4);
//           ModuleOutputLogger.handSyncAccount.info("客户下未同步联系人："+post4);
//           JSONObject object4 = JSONObject.parseObject(post4);
//           JSONArray records3 = object4.getJSONArray("records");
//           for (int i = 0; i < records3.size(); i++) {
//               JSONObject jsonObject1 = records3.getJSONObject(i);
//               jsonObject1.put("contactId",jsonObject1.getLongValue("id"));
//               String s = syncContact(jsonObject1);
//           }

           return null;
       }catch (Exception e){
            e.printStackTrace();
           System.err.println(e.getMessage());
           ModuleOutputLogger.handSyncAccountError.error(e.getMessage());
           return null;
       }
    }

    /**
     * 同步收货地址 CRM->ERP
     * @param param
     * @return
     */
    @RequestMapping("/syncAddress")
    @ResponseBody
    public String syncAaddress(@RequestBody JSONObject param){
        Long addressId=param.getLong("addressId");
        System.out.println("收货地址编号："+addressId);
        ModuleOutputLogger.handSyncContact.info("收货地址编号："+addressId);
        Integer CustSeq =null;
        try {

            String sql="select customItem1__c.customItem233__c,customItem1__c.customItem201__c,name,customItem2__c,customItem3__c,customItem4__c,customItem5__c,customItem6__c,customItem7__c,customItem12__c from customEntity7__c where id="+addressId;
            Map map=new HashMap();
            map.put("xoql",sql);
            String CRMResult = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
            JSONObject CRMObject = JSONObject.parseObject(CRMResult);
            JSONArray CRMJsonArray = CRMObject.getJSONObject("data").getJSONArray("records");
            if (CRMJsonArray.size()<1){
                return null;
            }
            JSONObject jsonObject = CRMJsonArray.getJSONObject(0);
            String ERPConfig = jsonObject.getString("customItem12__c");//账套
            //调用接口,获取Sessiontoken
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, ERPConfig);
            String CustNum = getData(jsonObject,"customItem1__c.customItem201__c");//客户编号
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
            System.out.println("同步结果："+slCustomers);
            ModuleOutputLogger.handSyncContact.info("同步结果："+slCustomers);
            if (slCustomers==null){
                JSONObject object=new JSONObject();
                object.put("id",addressId);
                object.put("customItem8__c","同步成功");
                String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", object.toString());
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            ModuleOutputLogger.handSyncContactError.error(e.getMessage());
            JSONObject object=new JSONObject();
            object.put("id",addressId);
            object.put("customItem8__c","同步失败");
            object.put("erp_id__c",CustSeq);
            try {
                String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", object.toString());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return  null;
    }

    /**
     * 同步收票地址 CRM->ERP
     * @param param
     * @return
     */
    @RequestMapping("/syncReceiptAddress")
    @ResponseBody
    public String syncReceiptAddress(@RequestBody JSONObject param){
        Long addressId=param.getLongValue("addressId");
        System.out.println("收票地址编号："+addressId);
        ModuleOutputLogger.handSyncReceiptAddress.info("收票地址编号："+addressId);
        try {

            String sql="select customItem1__c.customItem201__c,name,customItem2__c,customItem3__c,customItem4__c,customItem5__c,customItem6__c,customItem7__c,customItem12__c from customEntity9__c where id="+addressId;
            Map map=new HashMap();
            map.put("xoql",sql);
            String CRMResult = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
            System.out.println("收票地址查询结果："+CRMResult);
            ModuleOutputLogger.handSyncReceiptAddress.info("收票地址查询结果："+CRMResult);
            JSONObject CRMObject = JSONObject.parseObject(CRMResult);
            JSONArray CRMJsonArray = CRMObject.getJSONObject("data").getJSONArray("records");
            if (CRMJsonArray.size()<1){
                return null;
            }
            JSONObject jsonObject = CRMJsonArray.getJSONObject(0);
            String ERPConfig = jsonObject.getString("customItem12__c");//账套
            //调用接口,获取Sessiontoken
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, ERPConfig);
            String DropShipNo = getData(jsonObject,"customItem1__c.customItem201__c");//客户编号
            /*
                根据客户编号查询收货人地址，确认地址编号
             */
            String result = idoWebServiceSoap.loadJson(ERPtoken, "SLShipTos", "DropSeq", "DropShipNo = '"+DropShipNo+"'", "DropShipNo DESC", "", -1);
            System.out.println("查询客户编号："+result);
            ModuleOutputLogger.handSyncReceiptAddress.info("查询客户编号："+result);
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
            System.out.println("同步结果："+slCustomers);
            ModuleOutputLogger.handSyncReceiptAddress.info("同步结果："+slCustomers);
            if (slCustomers==null){
                JSONObject object=new JSONObject();
                object.put("id",addressId);
                object.put("customItem8__c","同步成功");
                object.put("erp_id__c",DropSeq);
                String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", object.toString());
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            ModuleOutputLogger.handSyncReceiptAddressError.error(e.getMessage());
            JSONObject object=new JSONObject();
            object.put("id",addressId);
            object.put("customItem8__c","同步失败");
            try {
                String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", object.toString());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return  null;
    }

    /**
     * 同步联系人 CRM->ERP
     * @param param
     * @return
     */
    @RequestMapping("/syncContact")
    @ResponseBody
    public String syncContact(@RequestBody JSONObject param){
        Long contactId=param.getLongValue("contactId");
        System.out.println("联系人编号："+contactId);
        ModuleOutputLogger.handSyncContact.info("联系人编号："+contactId);
        try {
            /*联系人数据start*/
            //调用接口,获取Sessiontoken
            String ERPtoken = idoWebServiceSoap.createSessionToken("crm", "Crm123456", "LIVE_HXSW");

            String sql="select accountId.customItem201__c,customItem160__c,customItem158__c,customItem159__c,customItem150__c,phone,mobile,email from contact where id="+contactId;
            Map map=new HashMap();
            map.put("xoql",sql);
            String CRMResult = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
            System.out.println("联系人查询结果："+CRMResult);
            ModuleOutputLogger.handSyncContact.info("联系人查询结果："+CRMResult);
            JSONObject CRMObject = JSONObject.parseObject(CRMResult);
            JSONArray CRMJsonArray = CRMObject.getJSONObject("data").getJSONArray("records");
            if (CRMJsonArray.size()<1){
                return null;
            }
            JSONObject jsonObject = CRMJsonArray.getJSONObject(0);
            String ContactID=getData(jsonObject,"customItem160__c");//联系人编号 "自增列，可直接由CRM生成的客户编号，直接推到ERP"
            String LName=getData(jsonObject,"customItem158__c");//姓
            String FName=getData(jsonObject,"customItem159__c");//名
            String JobTitle=getArrayToData(jsonObject,"customItem150__c");//职务
            String OfficePhone=getData(jsonObject,"phone");//电话
            String MobilePhone=getData(jsonObject,"mobile");//手机
            String Email=getData(jsonObject,"email");//电子邮件
            JSONObject dataObject=new JSONObject();
            dataObject.put("ContactID",ContactID);
            dataObject.put("LName",LName);
            dataObject.put("FName",FName);
            dataObject.put("JobTitle",JobTitle);
            dataObject.put("OfficePhone",OfficePhone);
            dataObject.put("MobilePhone",MobilePhone);
            dataObject.put("Email",Email);

            JSONArray contractFields = JSONArray.parseArray("[\"ContactID\",\"LName\",\"FName\",\"JobTitle\",\"OfficePhone\",\"MobilePhone\",\"Email\"]");
            String[] fields= contractFields.toArray(new String[0]);

            JSONArray items = getItems(dataObject, fields, contractFields);
            JSONArray allItems=new JSONArray();
            allItems.add(items);

            String slCustomers = addData(ERPtoken, "SLContacts", allItems, contractFields);//同步联系人
            /*联系人数据end*/
            /*联系人客户关联start*/
            String CustNum=getData(jsonObject,"accountId.customItem201__c");;//客户编号
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
            System.out.println("同步结果："+sCustomerContract);
            ModuleOutputLogger.handSyncContact.info("同步结果："+sCustomerContract);
            /*联系人客户关联end*/
            JSONObject object=new JSONObject();
            object.put("id",contactId);
            object.put("customItem161__c","同步成功");
            String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/contact/update", object.toString());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            ModuleOutputLogger.handSyncContactError.error(e.getMessage());
            JSONObject object=new JSONObject();
            object.put("id",contactId);
            object.put("customItem161__c","同步失败");
            try {
                String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/contact/update", object.toString());
            } catch (Exception e1) {
                ModuleOutputLogger.handSyncContactError.error(e1.getMessage());
                e1.printStackTrace();
            }
        }
        return  null;
    }

    @RequestMapping("/syncBankAccount")
    @ResponseBody
    public String syncBankAccount(@RequestBody JSONObject param){
        Long bankId=param.getLongValue("bankId");
        System.out.println("账号信息编号："+bankId);
        ModuleOutputLogger.handSyncBankInfo.info("账号信息编号："+bankId);
        try{
           //调用接口,获取Sessiontoken
           String ERPtoken = idoWebServiceSoap.createSessionToken("crm", "Crm123456", "LIVE_HXSW");

           String sql="select customItem1__c.customItem195__c,customItem1__c.customItem201__c,customItem4__c,customItem5__c,customItem6__c,customItem21__c,customItem15__c,customItem14__c,customItem9__c,customItem11__c from customEntity6__c where id="+bankId;
           Map map=new HashMap();
           map.put("xoql",sql);
           String CRMResult = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
            System.out.println("账号信息查询结果："+CRMResult);
            ModuleOutputLogger.handSyncBankInfo.info("账号信息查询结果："+CRMResult);
           JSONObject CRMObject = JSONObject.parseObject(CRMResult);
           JSONArray CRMJsonArray = CRMObject.getJSONObject("data").getJSONArray("records");
           if (CRMJsonArray.size()<1){
               return null;
           }

           JSONObject jsonObject = CRMJsonArray.getJSONObject(0);
           String cusUf_Bank=getData(jsonObject,"customItem4__c");//开户行
            String country = getSplitData(jsonObject,"customItem1__c.customItem195__c");//国家
           String cusUf_BankAcct=getData(jsonObject,"customItem5__c");//银行账户
           String cusUF_ReservedField1=getData(jsonObject,"customItem21__c");//纳税人识别号
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
            String Charfld3=getData(jsonObject,"customItem7__c");//开票电话 后加
            String Charfld2=getData(jsonObject,"customItem8__c");//开票地址 后加

           String CustNum = getData(jsonObject, "customItem1__c.customItem201__c");//客户编号
            System.out.println("客户编号："+CustNum);
            ModuleOutputLogger.handSyncBankInfo.info("客户编号："+CustNum);
           String result = idoWebServiceSoap.loadJson(ERPtoken, "SLCustomers", "cusUf_Bank,cusUf_BankAcct,cusUF_ReservedField1,cusUf_Note,InvCategory,PayType,TaxCode1,BankCode,Charfld3,Charfld2", "CustNum = '"+CustNum+"' and CustSeq='0'", "CustNum DESC", "", -1);
            System.out.println("ERP查询结果："+result);
            ModuleOutputLogger.handSyncBankInfo.info("ERP查询结果："+result);
           JSONObject resultJson = JSONObject.parseObject(result);
           JSONArray Items = resultJson.getJSONArray("Items");
           if (Items.size()<1){
               return null;
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
            System.out.println("更新结果："+slCustomers);
            ModuleOutputLogger.handSyncBankInfo.info("更新结果："+slCustomers);
            JSONObject object1=new JSONObject();
            object1.put("id",bankId);
            object1.put("customItem19__c","同步成功");
            String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", object1.toString());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            ModuleOutputLogger.handSyncBankInfoError.error(e.getMessage());
            JSONObject object=new JSONObject();
            object.put("id",bankId);
            object.put("customItem19__c","同步失败");
            try {
                String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", object.toString());
            } catch (Exception e1) {
                e1.printStackTrace();
                ModuleOutputLogger.handSyncBankInfoError.error(e1.getMessage());
            }

        }
       return null;
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
        ModuleOutputLogger.handSyncAccount.info("最新客户编号："+pa.value);
        return  pa.value;
    }

    /**
     * 更新客户编号 ERP->CRM
     * @param accountId
     * @param customNum
     * @throws Exception
     */
    public void updateCustomerNo(Long accountId,String customNum) throws Exception {
        JSONObject object=new JSONObject();
        object.put("id",accountId);
        object.put("customItem201__c",customNum);
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/account/update", object.toString());
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
        returnObject=object;
        /*JSONArray jsonArray=new JSONArray();
        jsonArray.add(object);
        String xmlData = XmlUtil.packageXML(toKen, Properties);
        SaveDataSet.UpdateDataSet updateDataSet = new SaveDataSet.UpdateDataSet();
        List<Object> any1 = updateDataSet.getAny();
        any1.add(xmlData);
        SaveDataSetResponse.SaveDataSetResult saveDataSetResult = idoWebServiceSoap.saveDataSet(toKen, new SaveDataSet.UpdateDataSet(), true, xmlData, "", "");
        List<Object> any = saveDataSetResult.getAny();*/
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
    public String updateData(String toKen, String IDOName, JSONArray Properties, JSONArray PropertyList,String ID){
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
        returnObject=object;
        String result = idoWebServiceSoap.saveJson(toKen,object.toJSONString(),"" , "", "");
        return result;
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
     * 更新客户编号 ERP->CRM
     * @param customNum
     * @throws Exception
     */
    public String updateAccountERPNo(Long accountId,String customNum) throws Exception {
        JSONObject object=new JSONObject();
        object.put("id",accountId);
        object.put("customItem201__c",customNum);
        String post = queryServer.updateAccount(object);
        return post;
    }
}
