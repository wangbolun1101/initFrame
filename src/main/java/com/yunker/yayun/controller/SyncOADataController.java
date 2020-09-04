package com.yunker.yayun.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yunker.yayun.config.FlowErrorCode;
import com.yunker.yayun.log.ModuleOutputLogger;
import com.yunker.yayun.oaPackage.*;
import com.yunker.yayun.util.*;
import mypackage.IDOWebService;
import mypackage.IDOWebServiceSoap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.rpc.ServiceException;
import javax.xml.ws.Holder;
import java.awt.geom.Area;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * crm->oa同步数据
 */

@Controller
@RequestMapping("/syncOAData")
@CrossOrigin(origins = "https://login.xiaoshouyi.com", maxAge = 3600)
public class SyncOADataController extends CommonController{

    @Autowired
    private HttpClientUtil httpClientUtil;
    @Autowired
    private QueryServer queryServer;
    @Autowired
    private WorkFlowUtil workFlowUtil;

    //实例化接口

    //实例化接口
    private IDOWebService ST = new IDOWebService();
    private IDOWebServiceSoap idoWebServiceSoap = ST.getIDOWebServiceSoap();
    private String userId="crm";//用户名
    private String pswd="Crm123456";//密码
    private String config="LIVE_HXSW";//密码
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

    @RequestMapping("/readXML")
    @ResponseBody
    public String readXML(@RequestBody JSONObject jsonObject){
        try {
            String xml = jsonObject.getString("xml");
            JSONArray jsonArray = XmlUtil.unPackageXML(xml);
            return sendJson(jsonArray, true);
        } catch (Exception e) {
            return sendJson("解析异常："+e.getMessage(), false);
        }
    }
    @RequestMapping("/readApproveXML")
    @ResponseBody
    public String readApproveXML(@RequestBody JSONObject jsonObject){
        try {
            String xml = jsonObject.getString("xml");
            JSONArray jsonArray = XmlUtil.unPackageApproveXML(xml);
            return sendJson(jsonArray, true);
        } catch (Exception e) {
            return sendJson("解析异常："+e.getMessage(), false);
        }
    }
    @RequestMapping("/test1")
    @ResponseBody
    public String test1(){
        try {
            queryServer.getContract("原料销售合同（外销）", 1333844347208080L);
            return "";
        } catch (Exception e) {
            return sendJson("解析异常："+e.getMessage(), false);
        }
    }
    @PostMapping("/readCSV")
    @ResponseBody
    public String readCSV(MultipartFile file){
        try {
            JSONArray jsonArray = CSVUtil.csv(file);
            return sendJson(jsonArray, true);
        } catch (Exception e) {
            return sendJson("解析异常："+e.getMessage(), false);
        }
    }
    @RequestMapping("/readOldXML")
    @ResponseBody
    public String readOldXML(@RequestBody JSONObject jsonObject){
        try {
            String xml = jsonObject.getString("xml");
            JSONArray jsonArray = XmlUtil.unPackageXMLOld(xml);
            return sendJson(jsonArray, true);
        } catch (Exception e) {
            return sendJson("解析异常："+e.getMessage(), false);
        }
    }
    @RequestMapping("/test")
    @ResponseBody
    public String test(){
        try {
            String xml = JsonReader.excutetest("报文.xml");
            JSONArray jsonArray = XmlUtil.unPackageXMLOld(xml);
            return sendJson(jsonArray, true);
        } catch (Exception e) {
            return sendJson("解析异常："+e.getMessage(), false);
        }
    }

    /**
     * 根据查询的物料和批次，显示标准及指标信息
     * @param jsonObject
     * @return
     */
    @RequestMapping("/getOverDateData")
    @ResponseBody
    public String getOverDateData(@RequestBody JSONObject jsonObject){
        String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
        String custnum = jsonObject.getString("custnum");//客户编号
        String bu = jsonObject.getString("bu");//事业部
        String term = jsonObject.getString("term");//条款
        String uf_overlimit3 = jsonObject.getString("uf_overlimit3");//该客户该事业部在销售易里提交状态的预估单总金额
        Holder<String> pa = new Holder<>("<Parameters><Parameter>" + custnum + "</Parameter>" +
                "<Parameter>"+bu+"</Parameter>" +
                "<Parameter>"+term+"</Parameter>" +
                "<Parameter>"+uf_overlimit3+"</Parameter>" +
                "<Parameter ByRef=\"Y\"></Parameter></Parameters>");
        String customNum = getCustomNum(ERPtoken, pa,"hxsp_calc_ecocredit_crm");
        System.out.println(customNum);
        return customNum;
    }

    @RequestMapping("/getAllSelects")
    @ResponseBody
    public String getAllSelects(){
        try {
            JSONObject responseJSON=new JSONObject();
            JSONArray itemAndLot = getItemAndLot();
            JSONArray itemArray=new JSONArray();
            JSONArray lotArray=new JSONArray();
            for (int i = 0; i < itemAndLot.size(); i++) {
                JSONObject itemObject=new JSONObject();
                JSONObject lotObject=new JSONObject();
                JSONObject jsonObject = itemAndLot.getJSONObject(i);
                String customItem130__c = jsonObject.getString("customItem130__c");
                String customItem129__c = jsonObject.getString("customItem129__c");
                if (StringUtils.isNotBlank(customItem130__c)){
                    itemObject.put("item", customItem130__c);
                    itemArray.add(itemObject);
                }
                if (StringUtils.isNotBlank(customItem129__c)){
                    lotObject.put("lot", customItem129__c);
                    lotArray.add(lotObject);
                }
            }
            JSONArray customerStandards = getCustomerStandards();
            JSONArray enterpriseStandards = getEnterpriseStandards();
            responseJSON.put("item",itemArray);
            responseJSON.put("lot",lotArray);
            responseJSON.put("customerStandards",customerStandards);
            responseJSON.put("enterpriseStandards",enterpriseStandards);
            return sendJson(responseJSON, true);
        } catch (Exception e) {
            return sendJson("查询异常："+e.getMessage(), false);
        }
    }

    /**
     * 根据查询的物料和批次，显示标准及指标信息
     * @param jsonObject
     * @return
     */
    @RequestMapping("/getOtherInfo")
    @ResponseBody
    public String getOtherInfo(@RequestBody JSONObject jsonObject){
        String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
        String Item = jsonObject.getString("Item");
        String Lot = jsonObject.getString("Lot");
        String Whse = jsonObject.getString("Whse");
        String Loc = jsonObject.getString("Loc");
        String where="";
        if (StringUtils.isNotBlank(Item)){
            where = "Item='"+Item+"'";
        }
        if (StringUtils.isNotBlank(Lot)) {
            if (StringUtils.isNotBlank(where)){
                where+=" and Lot='"+Lot+"'";
            }else {
                where+="Lot='"+Lot+"'";
            }
        }
        if (StringUtils.isNotBlank(Whse)) {
            Whse=Whse.split("-")[0];
            if (StringUtils.isNotBlank(where)){
                where+=" and Whse='"+Whse+"'";
            }else {
                where+="Whse='"+Whse+"'";
            }
        }
        if (StringUtils.isNotBlank(Loc)) {
            if (StringUtils.isNotBlank(where)){
                where+=" and Loc='"+Loc+"'";
            }else {
                where+="Loc='"+Loc+"'";
            }
        }


        String result = idoWebServiceSoap.loadJson(ERPtoken, "HXLotLocSpecifics", "Description,MatlQty,QtyPackage,HasReceived", where, "Specific ASC", "", -1);
        System.out.println(result);
        JSONObject resultJson = JSONObject.parseObject(result);
        JSONArray Items = resultJson.getJSONArray("Items");
        JSONArray propertyArray=new JSONArray();
        for (int i = 0; i < Items.size(); i++) {
            JSONObject jsonObject1 = Items.getJSONObject(i);
            JSONArray properties = jsonObject1.getJSONArray("Properties");
            JSONObject propertieObject=new JSONObject();
            for (int j = 0; j < properties.size(); j++) {
                JSONObject jsonObject2 = properties.getJSONObject(j);
                if (j==0){
                    String property_0 = jsonObject2.getString("Property");
                    propertieObject.put("StandardCode", property_0);
                }else {
                    String property_1 = jsonObject2.getString("Property");
                    propertieObject.put("Description", property_1);
                }
            }
            propertyArray.add(propertieObject);
        }
        String lotFeatureSp = getLotFeatureSp(Item, Lot);//待处理
        return result;

    }
    /**
     * 根据查询的物料和批次，显示标准及指标信息
     * @param jsonObject
     * @return
     */
    @RequestMapping("/getMainData")
    @ResponseBody
    public String getMainData(@RequestBody JSONObject jsonObject){
        String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
        String BItem = jsonObject.getString("BItem");
        String EItem = jsonObject.getString("EItem");
        String BLot = jsonObject.getString("BLot");
        String ELot = jsonObject.getString("ELot");
        Double BFeature18 = jsonObject.getDouble("BFeature18");
        Double EFeature18 = jsonObject.getDouble("EFeature18");
        Double BFeature16 = jsonObject.getDouble("BFeature16");
        Double EFeature16 = jsonObject.getDouble("EFeature16");
        Double BFeature53 = jsonObject.getDouble("BFeature53");
        Double EFeature53 = jsonObject.getDouble("EFeature53");
        Double BFeature19 = jsonObject.getDouble("BFeature19");
        Double EFeature19 = jsonObject.getDouble("EFeature19");
        Double BFeature43 = jsonObject.getDouble("BFeature43");
        Double EFeature43 = jsonObject.getDouble("EFeature43");
        Double BFeature5 = jsonObject.getDouble("BFeature5");
        Double EFeature5 = jsonObject.getDouble("EFeature5");
        String Site = jsonObject.getString("Site");
        String CustomerStandard = jsonObject.getString("CustomerStandard");
        String EnterpriseStandard = jsonObject.getString("EnterpriseStandard");
        Double BQtyOnHand = jsonObject.getDouble("BQtyOnHand");
        Double EQtyOnHand = jsonObject.getDouble("EQtyOnHand");
        String lot = jsonObject.getString("lot");
//        String whse = jsonObject.getString("whse");
        Holder<String> pa = new Holder<>("<Parameters><Parameter>" + BItem + "</Parameter>" +
                "<Parameter>"+EItem+"</Parameter>" +
                "<Parameter>"+BLot+"</Parameter>" +
                "<Parameter>"+ELot+"</Parameter>" +
                "<Parameter>"+BFeature18+"</Parameter>" +
                "<Parameter>"+EFeature18+"</Parameter>" +
                "<Parameter>"+BFeature16+"</Parameter>" +
                "<Parameter>"+EFeature16+"</Parameter>" +
                "<Parameter>"+BFeature53+"</Parameter>" +
                "<Parameter>"+EFeature53+"</Parameter>" +
                "<Parameter>"+BFeature19+"</Parameter>" +
                "<Parameter>"+EFeature19+"</Parameter>" +
                "<Parameter>"+BFeature43+"</Parameter>" +
                "<Parameter>"+EFeature43+"</Parameter>" +
                "<Parameter>"+BFeature5+"</Parameter>" +
                "<Parameter>"+EFeature5+"</Parameter>" +
                "<Parameter>"+Site+"</Parameter>" +
                "<Parameter>"+CustomerStandard+"</Parameter>" +
                "<Parameter>"+EnterpriseStandard+"</Parameter>" +
                "<Parameter>"+BQtyOnHand+"</Parameter>" +
                "<Parameter>"+EQtyOnHand+"</Parameter>" +
                "<Parameter>"+lot+"</Parameter>" +
//                "<Parameter>"+whse+"</Parameter>" +
                "<Parameter ByRef=\"Y\"></Parameter></Parameters>");
        String customNum = getCustomNum(ERPtoken, pa,"HXItemLotFeatureSearchSp");
        System.out.println(customNum);
        return customNum;
    }
    @RequestMapping("/test2")
    @ResponseBody
    public String getLotFeatureSp(String Item,String Lot){
        String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
        Holder<String> pa = new Holder<>("<Parameters><Parameter>"+Item+"</Parameter><Parameter>"+Lot+"</Parameter><Parameter ByRef=\"Y\"></Parameter></Parameters>");
        String customNum = getCustomNum(ERPtoken, pa,"HXCLM_GetLotFeatureSp");
        System.out.println(customNum);
        return customNum;
    }
    /**
     * 调用存储过程
     * @param token
     * @return
     */
    public String getCustomNum(String token,Holder<String> pa,String methodName){
        Holder<Object> pa2 = new Holder<>();
        idoWebServiceSoap.callMethod(token, "SP!", methodName, pa, pa2);
        //输出返回结果
        System.err.println(pa.value);
        return  pa.value;
    }




    public JSONArray getItemAndLot() throws Exception {
        String sql="select customItem130__c,customItem129__c from product";
        JSONObject bySql = queryServer.getByXoqlSimple(sql);
        JSONArray all = queryServer.getAllByXoqlSample(getToken(), bySql, sql);
        return all;
    }
    /**
     * 查询客户标准
     * @return
     */
    public JSONArray getCustomerStandards()throws Exception{
        JSONArray responseJSON=new JSONArray();
        String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
        String result = idoWebServiceSoap.loadJson(ERPtoken, "HXCustomerStandards", "StandardCode,Description", "", "Description ASC", "", -1);
        ModuleOutputLogger.handSyncBankInfo.info("ERP查询结果："+result);
        JSONObject resultJson = JSONObject.parseObject(result);
        JSONArray Items = resultJson.getJSONArray("Items");
        for (int i = 0; i < Items.size(); i++) {
            JSONObject jsonObject = Items.getJSONObject(i);
            JSONArray properties = jsonObject.getJSONArray("Properties");
            JSONObject propertieObject=new JSONObject();
            for (int j = 0; j < properties.size(); j++) {
                JSONObject jsonObject1 = properties.getJSONObject(j);
                if (j==0){
                    String property_0 = jsonObject1.getString("Property");
                    propertieObject.put("StandardCode", property_0);
                }else {
                    String property_1 = jsonObject1.getString("Property");
                    propertieObject.put("Description", property_1);
                }
            }
            responseJSON.add(propertieObject);
        }
        return responseJSON;
    }
    /**
     * 查询企业/行业标准
     * @return
     */
    public JSONArray getEnterpriseStandards()throws Exception{
        JSONArray responseJSON=new JSONArray();
        String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
        String result = idoWebServiceSoap.loadJson(ERPtoken, "HXEnterpriseStandards", "StandardCode,Description", "", "Description ASC", "", -1);
        System.out.println("ERP查询结果："+result);
        ModuleOutputLogger.handSyncBankInfo.info("ERP查询结果："+result);
        JSONObject resultJson = JSONObject.parseObject(result);
        JSONArray Items = resultJson.getJSONArray("Items");
        for (int i = 0; i < Items.size(); i++) {
            JSONObject jsonObject = Items.getJSONObject(i);
            JSONArray properties = jsonObject.getJSONArray("Properties");
            JSONObject propertieObject=new JSONObject();
            for (int j = 0; j < properties.size(); j++) {
                JSONObject jsonObject1 = properties.getJSONObject(j);
                if (j==0){
                    String property_0 = jsonObject1.getString("Property");
                    propertieObject.put("StandardCode", property_0);
                }else {
                    String property_1 = jsonObject1.getString("Property");
                    propertieObject.put("Description", property_1);
                }
            }
            responseJSON.add(propertieObject);
        }
        return responseJSON;
    }


    /**
     * 审批 CRM->OA
     * @param param
     * @return
     */
    @RequestMapping("/sync")
    @ResponseBody
    public String syncFreeSample(@RequestBody JSONObject param){
        Long dataId = param.getLong("dataId");
        String isContract = param.getString("isContract");//合同打印
        try {
            Integer creatorId = param.getInteger("creatorId");
            WorkflowRequestInfo workflowRequestInfo = workFlowUtil.packageXML(param);
            String result = addData(workflowRequestInfo,creatorId);
            //解析报文
            Long value = Long.valueOf(result);
            if (value<0){
                String errorMessage = FlowErrorCode.getErrorMessage(value);
                throw new Exception(errorMessage);
            }
            Long id = value;
            Long belongId = param.getLong("belongId");
            JSONObject object=new JSONObject();
            object.put("customItem1__c", dataId+"");
            object.put("customItem2__c", id+"");
            object.put("customItem3__c", belongId+"");
            queryServer.createCustomize(object, 1289974802579888L, getToken());
            JSONObject updateObject=new JSONObject();
            updateObject.put("id", dataId);
            updateObject.put("customItem46__c", id+"");
            updateObject.put("customItem44__c", "同步成功");
//            if ("1".equals(isContract)){
//                queryServer.updateContractById(updateObject);
//            }else {
            queryServer.updateCustomizeById(updateObject);
//            }
            return sendJson("同步成功", true);
        }catch (Exception e){
            e.printStackTrace();
            JSONObject updateObject=new JSONObject();
            updateObject.put("id", dataId);
            updateObject.put("customItem44__c", "同步失败："+e.getMessage());
            try {
//                if ("1".equals(isContract)){
//                    queryServer.updateContractById(updateObject);
//                }else {
                queryServer.updateCustomizeById(updateObject);
//                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            return sendJson("同步失败："+e.getMessage(), false);
        }
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
     * @param workflowRequestInfo
     * @return
     */
    public String addData(WorkflowRequestInfo workflowRequestInfo,Integer in1) throws Exception {
        WorkflowServicePortType workflowServiceHttpPort = new WorkflowServiceLocator().getWorkflowServiceHttpPort();
        String result = workflowServiceHttpPort.doCreateWorkflowRequest(workflowRequestInfo, in1);
//        String result ="";
        return result;

    }


}
