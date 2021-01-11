package com.yunker.eai.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.yunker.eai.config.FlowErrorCode;
import com.yunker.eai.entity.Credit;
import com.yunker.eai.log.ModuleOutputLogger;
import com.yunker.eai.oaPackage.WorkflowRequestInfo;
import com.yunker.eai.oaPackage.WorkflowServiceLocator;
import com.yunker.eai.oaPackage.WorkflowServicePortType;
import com.yunker.eai.service.HXCRMOAService;
import com.yunker.eai.util.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mypackage.IDOWebService;
import mypackage.IDOWebServiceSoap;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.IOUtils;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.entity.result.ExcelImportResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.ws.Holder;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/syncOAData")
@CrossOrigin(origins = "https://login.xiaoshouyi.com", maxAge = 3600)
public class SyncHX extends CommonController {

//    @Autowired
//    private SyncOtherService syncOtherService;

    @Autowired
    private HttpClientUtil httpClientUtil;
    @Autowired
    private QueryServer queryServer;
    @Autowired
    private WorkFlowUtil workFlowUtil;
    @Autowired
    private BulkAPI bulkAPI;
    @Autowired
    private HXCRMOAService service;

    //同步OA账号
    @RequestMapping("/syncOa")
    @ResponseBody
    public void syncoa() throws Exception {
        service.HXCRMoa();
    }


    //实例化接口
    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    private DateFormat df1 = new SimpleDateFormat("yyyyMMdd");
    private IDOWebService ST = new IDOWebService();
    private IDOWebServiceSoap idoWebServiceSoap = ST.getIDOWebServiceSoap();
    private String userId = "crm";//用户名
    private String pswd = "Crm123456";//密码
    private String config = "LIVE_HXSW";//密码

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
    @RequestMapping("/initCustNum")
    @ResponseBody
    public void initCustNum() throws Exception {

        JSONArray jsonArray = new JSONArray();
//        jsonArray.add("LIVE_SDUSA");
//        jsonArray.add("LIVE_HXYLQX");
//        jsonArray.add("LIVE_HXLMD");
//        jsonArray.add("LIVE_SDHY");
//        jsonArray.add("LIVE_BJHY");
        jsonArray.add("LIVE_HXSW");


        Map<String, JSONArray> ERPDataMap = new HashMap<>();
        Map<String, Long> accountMap = new HashMap<>();
        Map<String, Map<String, JSONArray>> IteamDataMap = new HashMap<>();
        Map<String, Long> codeMap = new HashMap<>();

        //查询CRM中所有客户
        String sql = "select id,accountName,customItem201__c from account where customItem240__c=1";
        JSONObject byXoqlSimple = queryServer.getByXoqlSimple(sql);
        JSONArray allByXoqlSample = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple, sql);
        for (int i = 0; i < allByXoqlSample.size(); i++) {
            JSONObject jsonObject = allByXoqlSample.getJSONObject(i);
            Long id = jsonObject.getLong("id");
            String accountName = jsonObject.getString("accountName");
            accountMap.put(accountName, id);
        }

        //查询ERP客户
        for (int i = 0; i < jsonArray.size(); i++) {
            String config = jsonArray.getString(i);
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
            String result = idoWebServiceSoap.loadJson(ERPtoken, "SLCustomers", "Name,CustNum", "", "", "", 300000);
            JSONObject resultJson = JSONObject.parseObject(result);
            JSONArray propertyList = resultJson.getJSONArray("PropertyList");

            JSONArray Items = resultJson.getJSONArray("Items");
            JSONArray propertyArray = new JSONArray();
            for (int j = 0; j < Items.size(); j++) {
                JSONObject jsonObject1 = Items.getJSONObject(j);
                JSONArray properties = jsonObject1.getJSONArray("Properties");
                JSONObject propertieObject = new JSONObject();
                for (int k = 0; k < properties.size(); k++) {
                    JSONObject jsonObject2 = properties.getJSONObject(k);
                    String property_0 = jsonObject2.getString("Property");
                    if(jsonObject2.getString("Property").equals("深圳香蜜丽格医疗美容诊所")){
                        String s = jsonObject2.getString("Property");
                    }
                    String string = propertyList.getString(k);
                    propertieObject.put(string, property_0);
                }
                propertyArray.add(propertieObject);
            }
            //ERPDataMap.put(config, propertyArray);
        }


        HashSet<Object> set = new HashSet<>();
        JSONArray updateNumArray = new JSONArray();
        for (Map.Entry<String, JSONArray> item : ERPDataMap.entrySet()) {
            JSONArray value = item.getValue();
            for (int i = 0; i < value.size(); i++) { //遍历所有ERP数据
                JSONObject jsonObject = value.getJSONObject(i);
                String Name = jsonObject.getString("Name");
                String CustNum = jsonObject.getString("CustNum");


                Long accountId = accountMap.get(Name);
                if (accountId == null || accountId == 0) {
                    continue;
                }
                else {
                    if (!set.add(accountId)) {
                        continue;
                    }
                    JSONObject updateObject = new JSONObject();
                    updateObject.put("id", accountId);
                    updateObject.put("customItem201__c", CustNum);
//                    updateObject.put("customItem240__c", "true");
                    updateNumArray.add(updateObject);

                }
            }
        }
        if (updateNumArray.size() > 0) {
            bulkAPI.createDataTaskJob(updateNumArray, "account", "update");
        }


    }


    /*
     * 初始化产品ERP->CRM
     * @author wangym
     * @date 2021/1/5 17:20
     *
     * */
    @RequestMapping("/initProduct")
    @ResponseBody
    public void initProduct() throws Exception {

        Map<String, JSONArray> ERPDataMap = new HashMap<>();

        JSONArray jsonArray = new JSONArray();
//        jsonArray.add("LIVE_SDUSA");
//        jsonArray.add("LIVE_HXYLQX");
//        jsonArray.add("LIVE_HXLMD");
//        jsonArray.add("LIVE_SDHY");
//        jsonArray.add("LIVE_BJHY");
        jsonArray.add("LIVE_HXSW");
//        jsonArray.add("LIVE_DYFST");


        JSONObject object = JSONObject.parseObject("{\n" +
                "  \"LIVE_SDUSA\":1,\n" +
                "  \"LIVE_HXYLQX\":2,\n" +
                "  \"LIVE_HXLMD\":3,\n" +
                "  \"LIVE_SDHY\":4,\n" +
                "  \"LIVE_BJHY\":5,\n" +
                "  \"LIVE_HXSW\":6\n" +
                "}");

        JSONObject spec = JSONObject.parseObject("{\n" +
                "  \"0-100g/瓶\":1,\n" +
                "  \"1-200g/瓶\":2,\n" +
                "  \"2-500g/袋\":3,\n" +
                "  \"3-1kg/袋\":4,\n" +
                "  \"4-5kg/袋\":5,\n" +
                "  \"5-10kg/袋\":6,\n" +
                "  \"6-50g/肖特瓶\":7,\n" +
                "  \"7-100g/肖特瓶\":8,\n" +
                "  \"8-1kg/溶液瓶\":9,\n" +
                "  \"9-20kg/桶\":10,\n" +
                "  \"10-1kg/广口塑料瓶\":11,\n" +
                "  \"11-5g/袋\":12,\n" +
                "  \"12-其他\":13,\n" +
                "}");

        JSONObject brand = JSONObject.parseObject("{\n" +
                "  \"润百颜\":1,\n" +
                "  \"润百颜护肤\":2,\n" +
                "  \"润百颜针剂\":3,\n" +
                "}");


        //查询ERP中物料
        for (int i = 0; i < jsonArray.size(); i++) {
            String config = jsonArray.getString(i);
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
            String result = idoWebServiceSoap.loadJson(ERPtoken, "SLItems", "Item,Description,itmUf_Specification,UM,itmUf_OwnerBu", "itmUf_OwnerBu='润百颜' or itmUf_OwnerBu='润百颜护肤' or itmUf_OwnerBu='润百颜针剂' ", "", "", 300000);
            JSONObject resultJson = JSONObject.parseObject(result);
            JSONArray propertyList = resultJson.getJSONArray("PropertyList");
            JSONArray Items = resultJson.getJSONArray("Items");
            JSONArray propertyArray = new JSONArray();
            for (int j = 0; j < Items.size(); j++) {
                JSONObject jsonObject1 = Items.getJSONObject(j);
                JSONArray properties = jsonObject1.getJSONArray("Properties");
                JSONObject propertieObject = new JSONObject();
                for (int k = 0; k < properties.size(); k++) {
                    JSONObject jsonObject2 = properties.getJSONObject(k);
                    String property_0 = jsonObject2.getString("Property");
                    String string = propertyList.getString(k);
                    propertieObject.put(string, property_0);
                }
                propertyArray.add(propertieObject);
            }
            ERPDataMap.put(config, propertyArray);
        }

//        for (int i = 0; i < jsonArray.size(); i++) {
//            String config = jsonArray.getString(i);
//            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
//            String result = idoWebServiceSoap.loadJson(ERPtoken, "HX_ItemSpecDesc", "item,description,Spec,SpecDesc", "", "", "", 300000);
//            JSONObject resultJson = JSONObject.parseObject(result);
//            JSONArray propertyList = resultJson.getJSONArray("PropertyList");
//            JSONArray Items = resultJson.getJSONArray("Items");
//            JSONArray propertyArray = new JSONArray();
//            for (int j = 0; j < Items.size(); j++) {
//                JSONObject jsonObject1 = Items.getJSONObject(j);
//                JSONArray properties = jsonObject1.getJSONArray("Properties");
//                JSONObject propertieObject = new JSONObject();
//                for (int k = 0; k < properties.size(); k++) {
//                    JSONObject jsonObject2 = properties.getJSONObject(k);
//                    String property_0 = jsonObject2.getString("Property");
//                    String string = propertyList.getString(k);
//                    propertieObject.put(string, property_0);
//                }
//                propertyArray.add(propertieObject);
//            }
//            ERPDataMap1.put(config, propertyArray);
//        }


        Map<Object, Long> proNumMap = new HashMap<>();

        HashMap<Object, Object> objectObjectHashMap = new HashMap<>();


        //查询CRM中产品
        String sql = "select id, priceUnit, enableStatus, productName, customItem130__c, customItem129__c, customItem134__c, customItem133__c, parentId, customItem139__c, customItem140__c from product  ";
        String bySql = queryServer.getBySql(sql);
        JSONArray all = queryServer.findAll(getToken(), bySql, sql);
        for (int i = 0; i < all.size(); i++) {
            JSONObject jsonObject = all.getJSONObject(i);
            Long id = jsonObject.getLong("id");
            String productName = jsonObject.getString("productName");
            String proNum = jsonObject.getString("customItem130__c");
//            String customItem139__c = jsonObject.getString("customItem139__c");
//            String customItem140__c = jsonObject.getString("customItem140__c");
//            String parentId = jsonObject.getString("parentId");
            proNumMap.put(proNum, id);

        }

        JSONArray productArray = new JSONArray();
        for (Map.Entry<String, JSONArray> item : ERPDataMap.entrySet()) {
            String config = item.getKey();
            Integer erpNo = object.getInteger(config);
            JSONArray value = item.getValue();
            for (int i = 0; i < value.size(); i++) {
                JSONObject jsonObject = value.getJSONObject(i);
                String Item = jsonObject.getString("Item");
                String Description = jsonObject.getString("Description");
                String itmUf_Specification = jsonObject.getString("itmUf_Specification");
                String UM = jsonObject.getString("UM");
                String itmUf_OwnerBu = jsonObject.getString("itmUf_OwnerBu");


                String customItem129__c = itmUf_Specification;
                Integer customItem143__c = brand.getInteger(itmUf_OwnerBu);
                String productName = Description;
                String customItem130__c = Item;
                String unit = UM;


                Long proId = proNumMap.get(customItem130__c);
                if (proId == null || proId == 0) {
                    JSONObject product = new JSONObject();
                    product.put("entityType", 7845905L);
                    product.put("productName", productName);
                    product.put("unit", unit);
                    product.put("customItem130__c", customItem130__c);
                    product.put("customItem129__c", customItem129__c);
                    product.put("customItem139__c", erpNo);
                    product.put("customItem141__c", 1);
                    product.put("customItem142__c", null);
                    product.put("customItem143__c", customItem143__c);
                    product.put("parentId", 1311359871189403L);
                    product.put("customItem140__c", 1);
                    product.put("enableStatus", 1);
                    product.put("priceUnit", "0");

                    productArray.add(product);
                }
            }
        }
        if (productArray.size() > 0) {
//            bulkAPI.createDataTaskJob(productArray, "product", "insert");
        }
    }


}
