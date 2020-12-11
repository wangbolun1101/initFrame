package com.yunker.yayun.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yunker.yayun.config.FlowErrorCode;
import com.yunker.yayun.entity.Address;
import com.yunker.yayun.entity.Credit;
import com.yunker.yayun.log.ModuleOutputLogger;
import com.yunker.yayun.oaPackage.WorkflowRequestInfo;
import com.yunker.yayun.oaPackage.WorkflowServiceLocator;
import com.yunker.yayun.oaPackage.WorkflowServicePortType;
import com.yunker.yayun.util.*;
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

/**
 * crm->oa同步数据
 */
@Slf4j
@Controller
@RequestMapping("/syncOAData")
@CrossOrigin(origins = "https://login.xiaoshouyi.com", maxAge = 3600)
public class SyncOADataController extends CommonController{

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

    //实例化接口

    //实例化接口
    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    private DateFormat df1 = new SimpleDateFormat("yyyyMMdd");
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


    @RequestMapping("/initCredit")
    @ResponseBody
    public String initCredit() throws Exception {
        JSONArray allArray = new JSONArray();
        Map<String,Long> existMap = new HashMap<>();
        Map<String,Long> accountMap = new HashMap<>();
        Map<String,Long> erpMap = new HashMap<>();
        Map<String,Long> departMap = new HashMap<>();
        Map<Long,Long> erpIDMap = new HashMap<>();
        Map<String,Long>zqMap = new HashMap<>();
        Map<String,Integer>levelMap = new HashMap<>();


        String SQL = "select id,customItem9__c,customItem13__c,customItem6__c from customEntity21__c";
        String bySql2 = queryServer.getBySql(SQL);
        JSONArray all2 = queryServer.findAll(getToken(), bySql2, SQL);
        for (int i = 0; i < all2.size(); i++) {
            JSONObject jsonObject = all2.getJSONObject(i);
            Long id = jsonObject.getLong("id");
            Long customItem9__c = jsonObject.getLong("customItem9__c");
            Long customItem13__c = jsonObject.getLong("customItem13__c");
            Long customItem6__c = jsonObject.getLong("customItem6__c");
            existMap.put(customItem9__c+","+customItem13__c+","+customItem6__c, id);
        }

        String departSql = "select id,customItem126__c from department";
        String bySqlDepart = queryServer.getBySql(departSql);
        JSONArray allDepart = queryServer.findAll(getToken(), bySqlDepart, departSql);
        for (int i = 0; i < allDepart.size(); i++) {
            JSONObject jsonObject = allDepart.getJSONObject(i);
            Long id = jsonObject.getLong("id");
            String customItem126__c = jsonObject.getString("customItem126__c");
            departMap.put(customItem126__c, id);
        }
        String accountSql = "select id,customItem201__c from account";
        String bySql = queryServer.getBySql(accountSql);
        JSONArray all = queryServer.findAll(getToken(), bySql, accountSql);
        for (int i = 0; i < all.size(); i++) {
            JSONObject jsonObject = all.getJSONObject(i);
            Long id = jsonObject.getLong("id");
            String customItem201__c = jsonObject.getString("customItem201__c");
            accountMap.put(customItem201__c, id);
        }
        String zqSql = "select id,name from customEntity33__c";
        String bySql1 = queryServer.getBySql(zqSql);
        JSONArray all1 = queryServer.findAll(getToken(), bySql1, zqSql);
        for (int i = 0; i < all1.size(); i++) {
            JSONObject jsonObject = all1.getJSONObject(i);
            Long id = jsonObject.getLong("id");
            String name = jsonObject.getString("name");
            zqMap.put(name, id);
        }

        String erpSql = "select id,customItem4__c,customItem2__c from customEntity63__c where customItem3__c=6";
        String bySqlERP = queryServer.getBySql(erpSql);
        JSONArray allERP = queryServer.findAll(getToken(), bySqlERP, erpSql);
        for (int i = 0; i < allERP.size(); i++) {
            JSONObject jsonObject = allERP.getJSONObject(i);
            Long id = jsonObject.getLong("id");
            Long customItem4__c = jsonObject.getLong("customItem4__c");
            String customItem2__c = jsonObject.getString("customItem2__c");
            erpMap.put(customItem2__c, customItem4__c);
            erpIDMap.put(customItem4__c, id);
        }
        String fieldsByBelongId = queryServer.getFieldsByBelongId(1214918522061203L);
        JSONObject object1 = JSONObject.parseObject(fieldsByBelongId);
        JSONArray fields = object1.getJSONArray("fields");
        for (int i = 0; i < fields.size(); i++) {
            JSONObject jsonObject = fields.getJSONObject(i);
            String propertyname = jsonObject.getString("propertyname");
            if ("customItem10__c".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Integer value = jsonObject1.getInteger("value");
                    String lable = jsonObject1.getString("label");
                    lable = lable.trim();
                    levelMap.put(lable, value);
                }
            }
        }

        JSONArray jsonArray = CSVUtil.csvSYBZQ();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String khbh = jsonObject.getString("khbh");
            String syb = jsonObject.getString("syb");
            Double customItem3__c = jsonObject.getDouble("xyed");
            customItem3__c=customItem3__c==null?0:customItem3__c;
            String khdj = jsonObject.getString("khdj");
            String name = jsonObject.getString("sybmc");
            khdj = khdj.trim();
            String tk = jsonObject.getString("tk");
            Long accountId = accountMap.get(khbh);
            if (accountId==null||accountId==0){
                accountId = erpMap.get(khbh);
            }
            Long customItem6__c = departMap.get(syb);
            Integer customItem10__c = levelMap.get(khdj);
            Long customItem8__c = zqMap.get(tk);
            Long customItem13__c = erpIDMap.get(accountId);

            if (accountId==null||accountId==0||customItem6__c==null||customItem6__c==0||customItem13__c==null||customItem13__c==0){
                continue;
            }
            Long aLong = existMap.get(accountId + "," + customItem13__c + "," + customItem6__c);
            if (aLong==null||aLong==0){
                JSONObject object = new JSONObject();
                object.put("name", name);
                object.put("customItem3__c", customItem3__c);
                object.put("customItem6__c", customItem6__c);
                object.put("customItem8__c", customItem8__c);
                object.put("customItem9__c", accountId);
                object.put("customItem13__c", customItem13__c);
                object.put("customItem10__c", customItem10__c);
                object.put("entityType", 1214917399855445L);
                allArray.add(object);
            }

        }

        bulkAPI.createDataTaskJob(allArray, "customEntity21__c", "insert");
        return null;
    }

    /*
     * @Description TODO 更新客户公海池
     * @author lucg.
     * @date 2020/12/4 10:57
     */
    @RequestMapping("updateAccountHightSea")
    public void updateAccountHightSea() throws Exception {
        String sql="select id from account where createdBy= 1185240 and createdAt<1607052000000 and createdAt>1607050200000";
        JSONObject byXoqlSimple = queryServer.getByXoqlSimple(sql);
        JSONArray allByXoqlSample = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple, sql);
        for (int i = 0; i < allByXoqlSample.size(); i++) {
            JSONObject jsonObject = allByXoqlSample.getJSONObject(i);
            jsonObject.put("highSeaId", 1512058636321132L);
        }
        bulkAPI.createDataTaskJob(allByXoqlSample, "account", "update");
    }

    /*
     * @Description TODO 更新为已同步状态
     * @author lucg.
     * @date 2020/12/4 10:57
     */
    @RequestMapping("updateERPStatus")
    public void updateERPStatus() throws Exception {
        JSONArray jsonArray = new JSONArray();
        String sql="select id,customItem4__c.customItem201__c from customEntity63__c where customItem2__c is null and customItem3__c=6 and createdAt>1606989300000 and createdAt<1606990260000";
        JSONObject byXoqlSimple = queryServer.getByXoqlSimple(sql);
        JSONArray allByXoqlSample = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple, sql);
        for (int i = 0; i < allByXoqlSample.size(); i++) {
            JSONObject jsonObject = allByXoqlSample.getJSONObject(i);
            String string = jsonObject.getString("customItem4__c.customItem201__c");
            Long id = jsonObject.getLong("id");
            if (StringUtils.isNotBlank(string)){
                JSONObject object = new JSONObject();
                object.put("id", id);
                object.put("customItem2__c", string);
                object.put("customItem5__c", "同步成功");
                jsonArray.add(object);
            }
        }
        bulkAPI.createDataTaskJob(jsonArray, "customEntity63__c", "update");
    }

    /*
     * @Description TODO 从ERP同步客户到CRM
     * @author lucg.
     * @date 2020/12/3 11:55
     */
    @RequestMapping("syncAccount")
    public void syncAccount() throws Exception {
        DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss.sss");

        Map<String, Integer> stateMap = new HashMap<>();
        Map<String, Integer> CityMap = new HashMap<>();
        Map<String, Integer> CountyMap = new HashMap<>();
        Map<String, Integer> CustTypeMap = new HashMap<>();
        Map<String, Integer> uf_salewayMap = new HashMap<>();
        Map<String, Integer> uf_ifonlineMap = new HashMap<>();
        Map<String, Integer> uf_org1Map = new HashMap<>();
        Map<String, Integer> uf_org2Map = new HashMap<>();
        Map<String, Integer> uf_org3Map = new HashMap<>();
        Map<String, Integer> uf_ifstopMap = new HashMap<>();
        Map<String, Integer> CountryMap = new HashMap<>();
        Map<String, Integer> TerritoryCodeMap = new HashMap<>();
        Map<String, Integer> uf_lawsuitMap = new HashMap<>();
        Map<String, Long> employeeMap = new HashMap<>();

        String fieldsByBelongId = queryServer.getFieldsByBelongId(1L);
        JSONObject object1 = JSONObject.parseObject(fieldsByBelongId);
        JSONArray fields = object1.getJSONArray("fields");
        for (int i = 0; i < fields.size(); i++) {
            JSONObject jsonObject = fields.getJSONObject(i);
            String propertyname = jsonObject.getString("propertyname");
            if ("fState".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Integer value = jsonObject1.getInteger("value");
                    String lable = jsonObject1.getString("label");
                    stateMap.put(lable, value);
                }
            }
            if ("fCity".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Integer value = jsonObject1.getInteger("value");
                    String lable = jsonObject1.getString("label");
                    CityMap.put(lable, value);
                }
            }
            if ("fDistrict".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Integer value = jsonObject1.getInteger("value");
                    String lable = jsonObject1.getString("label");
                    CountyMap.put(lable, value);
                }
            }
            if ("level".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Integer value = jsonObject1.getInteger("value");
                    String lable = jsonObject1.getString("label");
                    CustTypeMap.put(lable, value);
                }
            }
            if ("customItem186__c".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Integer value = jsonObject1.getInteger("value");
                    String lable = jsonObject1.getString("label");
                    uf_salewayMap.put(lable, value);
                }
            }
            if ("customItem199__c".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Integer value = jsonObject1.getInteger("value");
                    String lable = jsonObject1.getString("label");
                    uf_ifonlineMap.put(lable, value);
                }
            }
            if ("customItem207__c".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Integer value = jsonObject1.getInteger("value");
                    String lable = jsonObject1.getString("label");
                    uf_org1Map.put(lable, value);
                }
            }
            if ("customItem208__c".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Integer value = jsonObject1.getInteger("value");
                    String lable = jsonObject1.getString("label");
                    uf_org2Map.put(lable, value);
                }
            }
            if ("customItem209__c".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Integer value = jsonObject1.getInteger("value");
                    String lable = jsonObject1.getString("label");
                    uf_org3Map.put(lable, value);
                }
            }
            if ("customItem214__c".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Integer value = jsonObject1.getInteger("value");
                    String lable = jsonObject1.getString("label");
                    uf_ifstopMap.put(lable, value);
                }
            }
            if ("customItem213__c".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Integer value = jsonObject1.getInteger("value");
                    String lable = jsonObject1.getString("label");
                    uf_lawsuitMap.put(lable, value);
                }
            }
            if ("customItem195__c".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Boolean disabled = jsonObject1.getBoolean("disabled");
                    if (!disabled) {
                        Integer value = jsonObject1.getInteger("value");
                        String lable = jsonObject1.getString("label");
                        String data;
                        if (lable.contains("\t")) {
                            data = lable.split("\t")[0];
                        } else if (lable.contains("_")) {
                            data = lable.split("_")[0];
                        } else {
                            data = lable.split(" ")[0];
                        }
                        CountryMap.put(data, value);
                    }
                }
            }
            if ("customItem156__c".equals(propertyname)) {
                JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject jsonObject1 = selectitem.getJSONObject(j);
                    Boolean disabled = jsonObject1.getBoolean("disabled");
                    if (!disabled) {
                        Integer value = jsonObject1.getInteger("value");
                        String lable = jsonObject1.getString("label");
                        String data;
                        if (lable.contains("\t")) {
                            data = lable.split("\t")[0];
                        } else if (lable.contains("_")) {
                            data = lable.split("_")[0];
                        } else {
                            data = lable.split(" ")[0];
                        }
                        TerritoryCodeMap.put(data, value);
                    }
                }
            }
        }

        JSONObject object = JSONObject.parseObject("{\n" +
                "  \"LIVE_SDUSA\":1,\n" +
                "  \"LIVE_HXYLQX\":2,\n" +
                "  \"LIVE_HXLMD\":3,\n" +
                "  \"LIVE_SDHY\":4,\n" +
                "  \"LIVE_BJHY\":5,\n" +
                "  \"LIVE_HXSW\":6\n" +
                "}");
        JSONObject uf_domforObject = JSONObject.parseObject("{\n" +
                "  \"境内\":749177010471194,\n" +
                "  \"境外\":749179695907026,\n" +
                "  \"北京海御\":749177010471194,\n" +
                "  \"代理商\":1488102699106693\n" +
                "}");
        JSONObject CurrCodeObject = JSONObject.parseObject("{\n" +
                "  \"CNY\":1,\n" +
                "  \"EUR\":2,\n" +
                "  \"GBP\":3,\n" +
                "  \"HKD\":4,\n" +
                "  \"JPY\":5,\n" +
                "  \"USD\":6\n" +
                "}");


        JSONArray jsonArray = new JSONArray();
        jsonArray.add("LIVE_HXSW");
//        jsonArray.add("LIVE_HXLMD");
//        jsonArray.add("LIVE_BJHY");

        Map<String, Long> accountMap = new HashMap<>();
        Map<Long, Long> ownerMap = new HashMap<>();
        Map<String, Long> ERPMap = new HashMap<>();
        Map<Long, String> ERPMap1 = new HashMap<>();
        Map<String, JSONArray> ERPDataMap = new HashMap<>();
        Map<String, Map<String, JSONArray>> IteamDataMap = new HashMap<>();
        String sql = "select id,accountName,ownerId from account";
        JSONObject byXoqlSimple = queryServer.getByXoqlSimple(sql);
        JSONArray allByXoqlSample = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple, sql);
        for (int i = 0; i < allByXoqlSample.size(); i++) {
            JSONObject jsonObject = allByXoqlSample.getJSONObject(i);
            Long id = jsonObject.getLong("id");
            Long ownerId = jsonObject.getLong("ownerId");
            String accountName = jsonObject.getString("accountName");
            accountMap.put(accountName, id);
            ownerMap.put(id, ownerId);
        }

        String sql_erp = "select id,customItem3__c,customItem4__c,customItem2__c from customEntity63__c";
        JSONObject byXoqlSimple1 = queryServer.getByXoqlSimple(sql_erp);
        JSONArray allByXoqlSample1 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple1, sql_erp);
        for (int i = 0; i < allByXoqlSample1.size(); i++) {
            JSONObject jsonObject = allByXoqlSample1.getJSONObject(i);
            Long id = jsonObject.getLong("id");
            Integer customItem3__c = jsonObject.getInteger("customItem3__c");
            Long accountId = jsonObject.getLong("customItem4__c");
            String customItem2__c = jsonObject.getString("customItem2__c");

            ERPMap.put(accountId + "," + customItem3__c, id);
            if (StringUtils.isNotBlank(customItem2__c)) {
                ERPMap1.put(id, "success");
            }
        }

        String userSql = "select id,employeeCode from user";
        JSONObject byXoqlSimple2 = queryServer.getByXoqlSimple(userSql);
        JSONArray allByXoqlSample2 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple2, userSql);
        for (int i = 0; i < allByXoqlSample2.size(); i++) {
            JSONObject jsonObject = allByXoqlSample2.getJSONObject(i);
            Long id = jsonObject.getLong("id");
            String employeeCode = jsonObject.getString("employeeCode");
            employeeMap.put(employeeCode, id);
        }


        for (int i = 0; i < jsonArray.size(); i++) {
            String config = jsonArray.getString(i);
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
            String result = idoWebServiceSoap.loadJson(ERPtoken, "SLCustomers", "Name,ReservedField1,CustNum,State,City,County,CustType,uf_domfor,uf_saleway,uf_ifonline,uf_org1,uf_org2,uf_org3,uf_ifstop,Country,TerritoryCode,CurrCode,uf_createdate,uf_regfund,uf_salescale,uf_lawsuit", "", "", "", 300000);
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
        for (int i = 0; i < jsonArray.size(); i++) {
            Map<String, JSONArray> map = new HashMap<>();

            String config = jsonArray.getString(i);
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
            String result = idoWebServiceSoap.loadJson(ERPtoken, "SLSalesTeamMembers", "RefNum,DerFullName,SalesTeamID", "", "", "", 300000);
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
            for (int j = 0; j < propertyArray.size(); j++) {
                JSONObject jsonObject = propertyArray.getJSONObject(j);
                String salesTeamID = jsonObject.getString("SalesTeamID");
                salesTeamID = salesTeamID.trim();
                JSONArray jsonArray1 = map.get(salesTeamID);
                if (jsonArray1 == null) {
                    jsonArray1 = new JSONArray();
                }
                jsonArray1.add(jsonObject);
                map.put(salesTeamID, jsonArray1);
            }
            IteamDataMap.put(config, map);
        }

        JSONArray accountArray = new JSONArray();
        JSONArray ERPArray = new JSONArray();
        JSONArray ERPUpdateArray = new JSONArray();
        for (Map.Entry<String, JSONArray> item : ERPDataMap.entrySet()) {
            String config = item.getKey();
            Integer erpNo = object.getInteger(config);
            JSONArray value = item.getValue();
            for (int i = 0; i < value.size(); i++) {
                JSONObject jsonObject = value.getJSONObject(i);
                String Name = jsonObject.getString("Name");
                String CustNum = jsonObject.getString("CustNum");
                String ReservedField1 = jsonObject.getString("ReservedField1");
                ReservedField1 = ReservedField1.trim();
                String State = jsonObject.getString("State");
                String City = jsonObject.getString("City");
                String County = jsonObject.getString("County");
                String CustType = jsonObject.getString("CustType");
                String uf_domfor = jsonObject.getString("uf_domfor");
                String uf_saleway = jsonObject.getString("uf_saleway");
                String uf_ifonline = jsonObject.getString("uf_ifonline");
                String uf_org1 = jsonObject.getString("uf_org1");
                String uf_org2 = jsonObject.getString("uf_org2");
                String uf_org3 = jsonObject.getString("uf_org3");
                String uf_ifstop = jsonObject.getString("uf_ifstop");
                String Country = jsonObject.getString("Country");
                String TerritoryCode = jsonObject.getString("TerritoryCode");
                String CurrCode = jsonObject.getString("CurrCode");
                String uf_createdate = jsonObject.getString("uf_createdate");
                String uf_regfund = jsonObject.getString("uf_regfund");
                String uf_salescale = jsonObject.getString("uf_salescale");
                String uf_lawsuit = jsonObject.getString("uf_lawsuit");

                String string = provinceReverseJson.getString(State);
                Integer fState = stateMap.get(string);
                Integer fCity = CityMap.get(City);
                Integer fDistrict = CountyMap.get(County);
                Integer level = CustTypeMap.get(CustType);
                Long entityType = uf_domforObject.getLong(uf_domfor);
                Integer customItem186__c = uf_salewayMap.get(uf_saleway);
                Integer customItem199__c = uf_ifonlineMap.get(uf_ifonline);
                Integer customItem207__c = uf_org1Map.get(uf_org1);
                Integer customItem208__c = uf_org2Map.get(uf_org2);
                Integer customItem209__c = uf_org3Map.get(uf_org3);
                Integer customItem214__c = uf_ifstopMap.get(uf_ifstop);
                String string1 = countryReverseJson.getString(Country);
                if (entityType == null || entityType == 0) {
                    if ("CHN".equals(Country)) {
                        entityType = 749177010471194L;
                    } else {
                        entityType = 749179695907026L;
                    }
                }

                Integer customItem195__c = CountryMap.get(string1);
                Integer customItem156__c = TerritoryCodeMap.get(TerritoryCode);
                Integer customItem233__c = CurrCodeObject.getInteger(CurrCode);

                Date customItem210__c = new Date();
                if (StringUtils.isNotBlank(uf_createdate)) {
                    customItem210__c = df.parse(uf_createdate);
                }

                String customItem211__c = uf_regfund;
                String customItem212__c = uf_salescale;
                Integer customItem213__c = uf_lawsuitMap.get(uf_lawsuit);
                String customItem201__c = CustNum;
                Map<String, JSONArray> map = IteamDataMap.get(config);
                JSONArray jsonArray1 = map.get(CustNum);
//                JSONArray jsonArray1 = map.get(ReservedField1);
                Long userId = 0L;
                if (jsonArray1 != null && jsonArray1.size() > 0) {
                    JSONObject jsonObject1 = jsonArray1.getJSONObject(0);
                    String refNum = jsonObject1.getString("RefNum");
                    refNum = refNum.trim();
                    userId = employeeMap.get(refNum);
                }

                Long accountId = accountMap.get(Name);
                if (accountId == null || accountId == 0) {//不存在
                    //todo 创建客户
                    JSONObject account = new JSONObject();
                    account.put("entityType", entityType);
                    account.put("accountName", Name);
                    account.put("fState", fState);
                    account.put("fCity", fCity);
                    account.put("fDistrict", fDistrict);
                    account.put("level", level);
                    account.put("customItem186__c", customItem186__c);
                    account.put("customItem199__c", customItem199__c);
                    account.put("customItem207__c", customItem207__c);
                    account.put("customItem208__c", customItem208__c);
                    account.put("customItem209__c", customItem209__c);
                    account.put("customItem214__c", customItem214__c);
                    account.put("customItem195__c", customItem195__c);
                    account.put("customItem156__c", customItem156__c);
                    account.put("customItem233__c", customItem233__c);
                    if (StringUtils.isNotBlank(uf_createdate)) {
                        account.put("customItem210__c", customItem210__c);
                    }
                    account.put("customItem211__c", customItem211__c);
                    account.put("customItem212__c", customItem212__c);
                    account.put("customItem213__c", customItem213__c);
                    account.put("customItem201__c", customItem201__c);
                    if (userId != null && userId != 0) {
                        account.put("ownerId", userId);
                    }
                    accountArray.add(account);

                    accountMap.put(Name, 8888L);
                } else {
                    //判断账套是否存在
                    if (accountId != 8888L) {
                        Long erpId = ERPMap.get(accountId + "," + erpNo);
                        if (erpId == null || erpId == 0) {
                            //todo 创建账套
                            JSONObject erpObject = new JSONObject();
                            erpObject.put("entityType", 1340810097181017L);
                            erpObject.put("name", config);
                            erpObject.put("customItem3__c", erpNo);
                            erpObject.put("customItem4__c", accountId);
                            Long aLong = ownerMap.get(accountId);
                            erpObject.put("ownerId", aLong);
                            erpObject.put("customItem2__c", CustNum);
                            erpObject.put("customItem5__c", "同步成功");
                            ERPArray.add(erpObject);
                        } else {
                            String s = ERPMap1.get(erpId);
                            if (StringUtils.isBlank(s)) {
                                JSONObject erpObject = new JSONObject();
                                erpObject.put("id", erpId);
                                erpObject.put("customItem2__c", CustNum);
                                erpObject.put("customItem5__c", "同步成功");
                                ERPUpdateArray.add(erpObject);
                            }
                        }
                    }
                }
            }
        }

//        if (accountArray.size() > 0) {
//            bulkAPI.createDataTaskJob(accountArray, "account", "insert");
//        }
//        if (ERPArray.size() > 0) {
//            bulkAPI.createDataTaskJob(ERPArray, "customEntity63__c", "insert");
//        }
        if (ERPUpdateArray.size() > 0) {
            bulkAPI.createDataTaskJob(ERPUpdateArray, "customEntity63__c", "update");
        }
    }



        /*
         * @Description TODO 从erp同步收货地址到CRM
         * @author lucg.
         * @date 2020/12/3 11:54
         */
        @RequestMapping("updateAddress")
        public void updateAddress () throws Exception {
            JSONArray addArray = new JSONArray();
            JSONArray deleteArray = new JSONArray();
            JSONArray jsonArray = new JSONArray();
            jsonArray.add("LIVE_SDUSA");
//        jsonArray.add("LIVE_HXYLQX");
//        jsonArray.add("LIVE_HXLMD");
            jsonArray.add("LIVE_SDHY");
//        jsonArray.add("LIVE_BJHY");
            jsonArray.add("LIVE_HXSW");

            Map<String, JSONObject> accountMap = new HashMap<>();
            Map<String, JSONArray> ERPMap = new HashMap<>();
            Map<String, String> ERPDataMap = new HashMap<>();
            Map<String, String> addressExisitMap = new HashMap<>();
            Map<String, Integer> countryMap = new HashMap<>();
            Map<String, Integer> stateMap = new HashMap<>();
            Map<String, Integer> cityMap = new HashMap<>();

            String fieldsByBelongId = queryServer.getFieldsByBelongId(729747521339673L);
            JSONObject object1 = JSONObject.parseObject(fieldsByBelongId);
            JSONArray fields = object1.getJSONArray("fields");
            for (int i = 0; i < fields.size(); i++) {
                JSONObject jsonObject = fields.getJSONObject(i);
                String propertyname = jsonObject.getString("propertyname");
                if ("customItem5__c".equals(propertyname)) {
                    JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                    for (int j = 0; j < selectitem.size(); j++) {
                        JSONObject jsonObject1 = selectitem.getJSONObject(j);
                        Integer value = jsonObject1.getInteger("value");
                        String lable = jsonObject1.getString("label");
                        countryMap.put(lable, value);
                    }
                }
                if ("customItem6__c".equals(propertyname)) {
                    JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                    for (int j = 0; j < selectitem.size(); j++) {
                        JSONObject jsonObject1 = selectitem.getJSONObject(j);
                        Integer value = jsonObject1.getInteger("value");
                        String lable = jsonObject1.getString("label");
                        stateMap.put(lable, value);
                    }
                }
                if ("customItem7__c".equals(propertyname)) {
                    JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                    for (int j = 0; j < selectitem.size(); j++) {
                        JSONObject jsonObject1 = selectitem.getJSONObject(j);
                        Integer value = jsonObject1.getInteger("value");
                        String lable = jsonObject1.getString("label");
                        cityMap.put(lable, value);
                    }
                }
            }


            String accountSql = "select id,customItem2__c,customItem7__c,customItem4__c,customItem11__c  from customEntity63__c";
            String bySql = queryServer.getBySql(accountSql);
            JSONArray all = queryServer.findAll(getToken(), bySql, accountSql);
            for (int i = 0; i < all.size(); i++) {
                JSONObject jsonObject = all.getJSONObject(i);
                String customItem2__c = jsonObject.getString("customItem2__c");//客户编号
                String customItem7__c = jsonObject.getString("customItem7__c");//erp账套
                accountMap.put(customItem2__c + "," + customItem7__c, jsonObject);
            }
            Map<String, Long> allUsers = queryServer.getAllUsers();
            String sql = "select id,name,customItem1__c,customItem11__c,customItem13__c,customItem12__c from customEntity7__c";
            String bySql1 = queryServer.getBySql(sql);
            JSONArray addressArray = queryServer.findAll(getToken(), bySql1, sql);

            for (int i = 0; i < jsonArray.size(); i++) {
                String config = jsonArray.getString(i);
                String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
                String result = idoWebServiceSoap.loadJson(ERPtoken, "SLCustomers", "CustSeq,CustNum,Name,Addr_1,Addr_2,Addr_3,Addr_4,Country,State,City,Contact_2,Phone_2,CurrCode", "", "", "", 300000);
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
                ERPMap.put(config, propertyArray);
            }
            for (Map.Entry<String, JSONArray> item : ERPMap.entrySet()) {
                String config = item.getKey();
                JSONArray value = item.getValue();
                for (int i = 0; i < value.size(); i++) {
                    JSONObject jsonObject = value.getJSONObject(i);
                    String custSeq = jsonObject.getString("CustSeq");
                    String CustNum = jsonObject.getString("CustNum");
                    ERPDataMap.put(config + "," + CustNum + "," + custSeq, "success");
                }
            }
            ;
            for (int i = 0; i < addressArray.size(); i++) {
                JSONObject jsonObject = addressArray.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                String accountNum = jsonObject.getString("customItem13__c");
                String config = jsonObject.getString("customItem12__c");
                String custSeq = jsonObject.getString("erp_id__c");
                String s = ERPDataMap.get(config + "," + accountNum + "," + custSeq);
                addressExisitMap.put(config + "," + accountNum + "," + custSeq, "success");
                if (StringUtils.isBlank(s)) {
                    addressArray.remove(jsonObject);
                    i--;
                    JSONObject deleteObject = new JSONObject();
                    deleteObject.put("id", id);
                    deleteArray.add(deleteObject);
                }
            }
            for (Map.Entry<String, JSONArray> item : ERPMap.entrySet()) {
                String config = item.getKey();
                JSONArray value = item.getValue();
                for (int i = 0; i < value.size(); i++) {
                    JSONObject jsonObject = value.getJSONObject(i);
                    String custSeq = jsonObject.getString("CustSeq");
                    String CustNum = jsonObject.getString("CustNum");
                    String s = addressExisitMap.get(config + "," + CustNum + "," + custSeq);
                    if (StringUtils.isBlank(s)) {//不存在，创建
                        addressExisitMap.put(config + "," + CustNum + "," + custSeq, "success");
                        String addr_1 = jsonObject.getString("Addr_1");
                        String addr_2 = jsonObject.getString("Addr_2");
                        String addr_3 = jsonObject.getString("Addr_3");
                        String addr_4 = jsonObject.getString("Addr_4");
                        String country = jsonObject.getString("Country");
                        String State = jsonObject.getString("State");
                        String City = jsonObject.getString("City");
                        String Name = jsonObject.getString("Name");
                        String contact_2 = jsonObject.getString("Contact_2");
                        String Phone_2 = jsonObject.getString("Phone_2");
                        String string = provinceReverseJson.getString(State);
                        Integer stateInteger = stateMap.get(string);
                        Integer cityInteger = cityMap.get(City);
                        JSONObject accountERP = accountMap.get(CustNum + "," + config);
                        if (accountERP == null) {
                            continue;
                        }
                        Long erpId = accountERP.getLong("id");
                        String accountOwner = accountERP.getString("customItem11__c");
                        Long accountId = accountERP.getLong("customItem4__c");
                        Long accountOwnerId = allUsers.get(accountOwner);


                        Integer countryInteger = countryMap.get(country);
                        JSONObject addObject = new JSONObject();
                        addObject.put("customItem2__c", contact_2);
                        addObject.put("customItem3__c", Phone_2);
                        addObject.put("customItem4__c", addr_1 + addr_2 + addr_3 + addr_4);
                        addObject.put("customItem5__c", countryInteger);
                        addObject.put("customItem6__c", stateInteger);
                        addObject.put("customItem7__c", cityInteger);
                        addObject.put("erp_id__c", custSeq);
                        addObject.put("customItem8__c", "同步成功");
                        if (accountOwnerId != null && accountOwnerId != 0) {
                            addObject.put("ownerId", accountOwnerId);
                        }
                        addObject.put("customItem1__c", accountId);
                        addObject.put("customItem11__c", erpId);
                        addObject.put("entityType", 729741452263662L);
                        addObject.put("name", Name);
                        addArray.add(addObject);
                    }
                }
            }
            ;

            if (deleteArray.size() > 0) {
                bulkAPI.createDataTaskJob(deleteArray, "customEntity7__c", "delete");
            }
            if (addArray.size() > 0) {
                bulkAPI.createDataTaskJob(addArray, "customEntity7__c", "insert");
            }
        }


        /**
         * 收票地址数据处理
         * @return
         */
        @RequestMapping("/upateAccountTaxpyerId")
        @ResponseBody
        public String upateAccountTaxpyerId () throws Exception {
//        syncOtherService.upateAccountTaxpyerId();
            return "success";
        }

        /**
         * 收票地址数据处理
         * @return
         */
        @RequestMapping("/updateSuccess")
        @ResponseBody
        public String updateSuccess () throws Exception {
            String sql = "select id,customItem3__c,customItem4__c from customEntity63__c where customItem2__c is  not null and customItem5__c is null limit 0,300";
            String bySql = queryServer.getBySql(sql);
            JSONObject object = JSONObject.parseObject(bySql);
            JSONArray records = object.getJSONArray("records");
            for (int i = 0; i < records.size(); i++) {
                JSONObject jsonObject = records.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                JSONObject object1 = new JSONObject();
                object1.put("id", id);
                object1.put("customItem5__c", "同步成功");
                queryServer.updateCustomizeById(object1);
            }
            return null;
        }

        /**
         * 收票地址数据处理
         * @return
         */
        @RequestMapping("/ReceiptAddressData")
        @ResponseBody
        public String ReceiptAddressData () throws Exception {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date start = dateFormat.parse("2020-09-14 00:00:01");
            Date end = dateFormat.parse("2020-09-14 23:59:59");
            String ERPAccountSql = "select id from customEntity9__c where createdAt>" + start.getTime() + " and createdAt <" + end.getTime() + " and createdBy = 1185240";
            JSONObject byXoqlSimple3 = queryServer.getByXoqlSimple(ERPAccountSql);
            JSONArray allByXoqlSample3 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple3, ERPAccountSql);
            for (int i = 0; i < allByXoqlSample3.size(); i++) {
                JSONObject jsonObject = allByXoqlSample3.getJSONObject(i);
                jsonObject.put("customItem8__c", "同步成功");
            }
            bulkAPI.createDataTaskJob(allByXoqlSample3, "customEntity9__c", "update");
            return null;
        }
        /**
         * 收货地址数据处理
         * @return
         */
        @RequestMapping("/AddressData")
        @ResponseBody
        public String AddressData () throws Exception {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date start = dateFormat.parse("2020-09-17 00:00:01");
            Date end = dateFormat.parse("2020-09-17 23:59:59");
            String ERPAccountSql = "select id from customEntity7__c where createdAt>" + start.getTime() + " and createdAt <" + end.getTime() + " and createdBy = 1185240";
            JSONObject byXoqlSimple3 = queryServer.getByXoqlSimple(ERPAccountSql);
            JSONArray allByXoqlSample3 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple3, ERPAccountSql);
            for (int i = 0; i < allByXoqlSample3.size(); i++) {
                JSONObject jsonObject = allByXoqlSample3.getJSONObject(i);
                jsonObject.put("customItem8__c", "同步成功");
            }
            bulkAPI.createDataTaskJob(allByXoqlSample3, "customEntity7__c", "update");
            return null;
        }
        /**
         * 账户信息
         * @return
         */
        @RequestMapping("/BankInfo")
        @ResponseBody
        public String BankInfo () throws Exception {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date start = dateFormat.parse("2020-08-20 00:00:01");
            Date end = dateFormat.parse("2020-08-20 23:59:59");
            String ERPAccountSql = "select id from customEntity6__c where createdAt>" + start.getTime() + " and createdAt <" + end.getTime() + " and createdBy = 1185240";
            JSONObject byXoqlSimple3 = queryServer.getByXoqlSimple(ERPAccountSql);
            JSONArray allByXoqlSample3 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple3, ERPAccountSql);
            for (int i = 0; i < allByXoqlSample3.size(); i++) {
                JSONObject jsonObject = allByXoqlSample3.getJSONObject(i);
                jsonObject.put("customItem19__c", "同步成功");
            }
            bulkAPI.createDataTaskJob(allByXoqlSample3, "customEntity6__c", "update");
            return null;
        }


        /**
         * 导入收票地址
         * @return
         */
        @RequestMapping("/importReceiptAddress")
        @ResponseBody
        public String importReceiptAddress () throws Exception {
            Map<String, Long> allUsers = queryServer.getAllUsers();
            JSONArray paramArray = new JSONArray();
            Map<String, JSONObject> ERPAccountMap = new HashMap<>();
            Map<String, Long> addressMap = new HashMap<>();
            Map<String, Integer> CountryMap = new HashMap<>();
            Map<String, Integer> provinceMap = new HashMap<>();
            String ERPAccountSql = "select id,customItem4__c,customItem2__c,name,customItem10__c,customItem11__c from customEntity63__c";
            JSONObject byXoqlSimple3 = queryServer.getByXoqlSimple(ERPAccountSql);
            JSONArray allByXoqlSample3 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple3, ERPAccountSql);
            for (int i = 0; i < allByXoqlSample3.size(); i++) {
                JSONObject jsonObject = allByXoqlSample3.getJSONObject(i);
                String customItem2__c = jsonObject.getString("customItem2__c");
                ERPAccountMap.put(customItem2__c, jsonObject);
            }
            String fieldsByBelongId = queryServer.getFieldsByBelongId(746444327076125L);
            JSONObject object1 = JSONObject.parseObject(fieldsByBelongId);
            JSONArray fields = object1.getJSONArray("fields");
            for (int i = 0; i < fields.size(); i++) {
                JSONObject jsonObject = fields.getJSONObject(i);
                String propertyname = jsonObject.getString("propertyname");
                if ("customItem5__c".equals(propertyname)) {
                    JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                    for (int j = 0; j < selectitem.size(); j++) {
                        JSONObject jsonObject1 = selectitem.getJSONObject(j);
                        Integer value = jsonObject1.getInteger("value");
                        String lable = jsonObject1.getString("label");
                        String[] split = lable.split("_");
                        CountryMap.put(split[0], value);
                    }
                }
            }
            for (int i = 0; i < fields.size(); i++) {
                JSONObject jsonObject = fields.getJSONObject(i);
                String propertyname = jsonObject.getString("propertyname");
                if ("customItem6__c".equals(propertyname)) {
                    JSONArray selectitem = jsonObject.getJSONArray("selectitem");
                    for (int j = 0; j < selectitem.size(); j++) {
                        JSONObject jsonObject1 = selectitem.getJSONObject(j);
                        Integer value = jsonObject1.getInteger("value");
                        String lable = jsonObject1.getString("label");
                        provinceMap.put(lable, value);
                    }
                }
            }
            String addressSql = "select id,customItem1__c,customItem11__c from customEntity9__c";
            JSONObject byXoqlSimple = queryServer.getByXoqlSimple(addressSql);
            JSONArray allByXoqlSample = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple, addressSql);
            for (int i = 0; i < allByXoqlSample.size(); i++) {
                JSONObject jsonObject = allByXoqlSample.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                Long accountId = jsonObject.getLong("customItem1__c");
                Long erpId = jsonObject.getLong("customItem11__c");
                addressMap.put(accountId + "," + erpId, id);
            }

//        provinceReverseJson
            File file = new File("C:\\Users\\lucg\\Desktop\\华熙生物收票地址.xlsx");
            FileInputStream input = new FileInputStream(file);
            MultipartFile multipartFile = new MockMultipartFile("file", file.getName(), "text/plain", IOUtils.toByteArray(input));
            JSONArray successJsonArray = importExcel(multipartFile, Address.class);
            for (int i = 0; i < successJsonArray.size(); i++) {
                JSONObject jsonObject = successJsonArray.getJSONObject(i);
                String accountNo = jsonObject.getString("accountNo");
                String erp_id = jsonObject.getString("erpid");
                String address1 = jsonObject.getString("address1");
                String address2 = jsonObject.getString("address2");
                String province = jsonObject.getString("province");
                String country = jsonObject.getString("country");
                String contact = jsonObject.getString("contact");
                String phone = jsonObject.getString("phone");
                JSONObject ERPObject = ERPAccountMap.get(accountNo);
                if (ERPObject == null) {
                    continue;
                }
                Long accountId = ERPObject.getLong("customItem4__c");
                Long erpId = ERPObject.getLong("id");
                String accountName = ERPObject.getString("customItem10__c");
                String ownerName = ERPObject.getString("customItem11__c");
                String address = (StringUtils.isNotBlank(address1) && StringUtils.isNotBlank(address2)) ? address1 + ";" + address2 : (StringUtils.isNotBlank(address1) && StringUtils.isBlank(address2)) ? address1 : address2;
                Integer countryInt = CountryMap.get(country);
                String priovince_replacce = provinceReverseJson.getString(province);
                Integer provinceInt = provinceMap.get(priovince_replacce);
                Long aLong = addressMap.get(accountId + "," + erpId);
                if (aLong == null || aLong == 0) {
                    Long ownerId = allUsers.get(ownerName);
                    JSONObject paramObject = new JSONObject();
                    paramObject.put("entityType", 746444327076143L);
                    if (accountId != null && accountId != 0) {
                        paramObject.put("customItem1__c", accountId);
                    }
                    if (erpId != null && erpId != 0) {
                        paramObject.put("customItem11__c", erpId);
                    }
                    if (countryInt != null && countryInt != 0) {
                        paramObject.put("customItem5__c", countryInt);
                    }
                    if (provinceInt != null && provinceInt != 0) {
                        paramObject.put("customItem6__c", provinceInt);
                    }
                    if (ownerId != null && ownerId != 0) {
                        paramObject.put("ownerId", ownerId);
                    }
                    paramObject.put("customItem2__c", contact);
                    paramObject.put("customItem3__c", phone);
                    paramObject.put("erp_id__c", erp_id);
                    paramObject.put("name", accountName);
                    paramObject.put("customItem4__c", address);
                    paramArray.add(paramObject);
                }
            }
            bulkAPI.createDataTaskJob(paramArray, "customEntity9__c", "insert");

            return null;
        }
        /**
         * 导入价格表
         * @return
         */
        @RequestMapping("/importPriceBook")
        @ResponseBody
        public String importPriceBook () throws Exception {
            JSONArray priceBookArray = new JSONArray();
            JSONArray updatepriceBookEntryArray = new JSONArray();
            JSONArray insertpriceBookEntryArray = new JSONArray();
            JSONArray jsonArray = new JSONArray();
            jsonArray.add("LIVE_SDUSA");
            jsonArray.add("LIVE_SDHY");
            jsonArray.add("LIVE_HXSW");
            Map<String, Integer> hbMap = new HashMap<>();
            hbMap.put("CNY", 1);
            hbMap.put("EUR", 2);
            hbMap.put("HKD", 3);
            hbMap.put("JPY", 4);
            hbMap.put("USD", 5);
            hbMap.put("GBP", 6);
            Map<String, JSONArray> map = new HashMap<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                String config = jsonArray.getString(i);
                String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
                String result = idoWebServiceSoap.loadJson(ERPtoken, "SLItemCustPrices", "CustNum,Item,Uf_StandNum,ContPrice,AdrCurrCode,EffectDate,RecordDate", "", "", "", 300000);
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
                map.put(config, propertyArray);
            }
            Map<String, Long> priceBookMap = new HashMap<>();
            Map<String, Long> productMap = new HashMap<>();
            Map<String, Long> priceBookEntryMap = new HashMap<>();
            Map<String, JSONObject> ERPAccountMap = new HashMap<>();
            Map<Long, Long> SXDateMap = new HashMap<>();
            String priceBookSql = "select id,name,customItem1__c from priceBook";
            JSONObject byXoqlSimple = queryServer.getByXoqlSimple(priceBookSql);
            JSONArray allByXoqlSample = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple, priceBookSql);
            for (int i = 0; i < allByXoqlSample.size(); i++) {
                JSONObject jsonObject = allByXoqlSample.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                Long accountId = jsonObject.getLong("customItem1__c");
                String name = jsonObject.getString("name");
                priceBookMap.put(name + "," + accountId, id);
            }
            String productSql = "select id,productName from product";
            JSONObject byXoqlSimple1 = queryServer.getByXoqlSimple(productSql);
            JSONArray allByXoqlSample1 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple1, productSql);
            for (int i = 0; i < allByXoqlSample1.size(); i++) {
                JSONObject jsonObject = allByXoqlSample1.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                String productName = jsonObject.getString("productName");
                productMap.put(productName, id);
            }
            String priceBookEntrySql = "select id,productId,priceBookId,name,customItem1__c from priceBookEntry";
            JSONObject byXoqlSimple2 = queryServer.getByXoqlSimple(priceBookEntrySql);
            JSONArray allByXoqlSample2 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple2, priceBookEntrySql);
            for (int i = 0; i < allByXoqlSample2.size(); i++) {
                JSONObject jsonObject = allByXoqlSample2.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                Long productId = jsonObject.getLong("productId");
                Long priceBookId = jsonObject.getLong("priceBookId");
                String name = jsonObject.getString("name");
                Long customItem1__c = jsonObject.getLong("customItem1__c");
                priceBookEntryMap.put(name + "," + productId + "," + priceBookId, id);
                SXDateMap.put(id, customItem1__c == null ? 0L : customItem1__c);
            }
            String ERPAccountSql = "select id,customItem4__c,customItem2__c,name,customItem10__c from customEntity63__c";
            JSONObject byXoqlSimple3 = queryServer.getByXoqlSimple(ERPAccountSql);
            JSONArray allByXoqlSample3 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple3, ERPAccountSql);
            for (int i = 0; i < allByXoqlSample3.size(); i++) {
                JSONObject jsonObject = allByXoqlSample3.getJSONObject(i);
                String customItem2__c = jsonObject.getString("customItem2__c");
                String name = jsonObject.getString("name");
                ERPAccountMap.put(customItem2__c + "," + name, jsonObject);
            }
            for (Map.Entry<String, JSONArray> stringJSONArrayEntry : map.entrySet()) {
                String config = stringJSONArrayEntry.getKey();
                JSONArray array = stringJSONArrayEntry.getValue();
                for (int i = 0; i < array.size(); i++) {
                    JSONObject jsonObject = array.getJSONObject(i);
                    String custNum = jsonObject.getString("CustNum");
                    String item = jsonObject.getString("Item");
                    String uf_standNum = jsonObject.getString("Uf_StandNum");
                    String contPrice = jsonObject.getString("ContPrice");
                    String adrCurrCode = jsonObject.getString("AdrCurrCode");
                    String effectDate = jsonObject.getString("EffectDate");
                    String recordDate = jsonObject.getString("RecordDate");
                    Integer integer = hbMap.get(adrCurrCode);
                    String productName = item + "-" + uf_standNum;
                    JSONObject object = ERPAccountMap.get(custNum + "," + config);
                    if (object == null) {
                        continue;
                    }
                    Long accountId = object.getLong("customItem4__c");
                    Long ERPId = object.getLong("id");
                    String accountName = object.getString("customItem10__c");
                    Long productId = productMap.get(productName);
                    Long priceBookId = priceBookMap.get(config + "," + accountId);
                    Long priceBookEntryId = priceBookEntryMap.get(productName + "," + productId + "," + priceBookId);
                    if (productId == null || productId == 0) {
                        continue;
                    }
                    if (accountId == null || accountId == 0) {
                        continue;
                    }
                    if (priceBookId == null || priceBookId == 0) {
//                    JSONObject priceBookObject = new JSONObject();
//                    priceBookObject.put("entityType", 101065558);
//                    priceBookObject.put("name", config);
//                    priceBookObject.put("customItem1__c", accountId);
//                    priceBookObject.put("ownerId", 743527959707865L);
//                    priceBookObject.put("dimDepart", 724765962912012L);
//                    priceBookObject.put("enableFlg", 1);
//                    if (ERPId!=null&&ERPId!=0){
//                        priceBookObject.put("customItem3__c", ERPId);
//                    }
//                    priceBookMap.put(config+","+accountId, 8888888L);
//                    priceBookArray.add(priceBookObject);
                        continue;
                    }
                    if (priceBookEntryId != null && priceBookEntryId != 0) {
                        if (priceBookEntryId.longValue() == 88888L) {
                            continue;
                        }
                        Long aLong = SXDateMap.get(priceBookEntryId);
                        if (StringUtils.isNotBlank(effectDate)) {
                            effectDate = effectDate.split(" ")[0];
                            long time = df1.parse(effectDate).getTime();
                            if (time > aLong) {
                                //todo 更新
                                JSONObject priceBookEntryObject = new JSONObject();
                                priceBookEntryObject.put("id", priceBookEntryId);
                                if (integer != null && integer != 0) {
                                    priceBookEntryObject.put("customItem3__c", integer);
                                }
                                priceBookEntryObject.put("bookPrice", StringUtils.isBlank(contPrice) ? 0 : Double.valueOf(contPrice));
                                priceBookEntryObject.put("productPrice", StringUtils.isBlank(contPrice) ? 0 : Double.valueOf(contPrice));
                                priceBookEntryObject.put("customItem1__c", time);
                                priceBookEntryObject.put("remark", accountName);
                                updatepriceBookEntryArray.add(priceBookEntryObject);
                            }
                        }
                    } else {
                        //todo 创建
                        JSONObject priceBookEntryObject = new JSONObject();
                        // {"data":{"name":o,"priceBookId":newId,"productId":pIdR,"customItem3__c":i,"productPrice":ContPrice,"customItem1__c":EffectDate,"bookPrice":ContPrice,"enableFlg":"1","syncFlg":"1","remark":accountName}}
                        priceBookEntryObject.put("entityType", 101065557);
                        priceBookEntryObject.put("name", productName);
                        priceBookEntryObject.put("priceBookId", priceBookId);
                        priceBookEntryObject.put("productId", productId);
                        priceBookEntryObject.put("productPrice", StringUtils.isBlank(contPrice) ? 0 : Double.valueOf(contPrice));
                        priceBookEntryObject.put("bookPrice", StringUtils.isBlank(contPrice) ? 0 : Double.valueOf(contPrice));
                        priceBookEntryObject.put("enableFlg", 1);
                        priceBookEntryObject.put("syncFlg", 1);
                        priceBookEntryObject.put("remark", accountName);
                        priceBookEntryObject.put("customItem3__c", integer);
                        if (StringUtils.isNotBlank(effectDate)) {
                            long time = df1.parse(effectDate).getTime();
                            priceBookEntryObject.put("customItem1__c", time);
                        }
                        priceBookEntryMap.put(productName + "," + productId + "," + priceBookId, 88888L);
                        insertpriceBookEntryArray.add(priceBookEntryObject);
                    }
                }
            }
//        if (priceBookArray.size()>0){
//            bulkAPI.createDataTaskJob(priceBookArray, "priceBook", "insert");
//        }
            if (updatepriceBookEntryArray.size() > 0) {
                bulkAPI.createDataTaskJob(updatepriceBookEntryArray, "priceBookEntry", "update");
            }
//        if (insertpriceBookEntryArray.size()>0){
//            bulkAPI.createDataTaskJob(insertpriceBookEntryArray, "priceBookEntry", "insert");
//        }
            return null;
        }

        /**
         * 将有客户编码的客户创建到ERP客户账套模块
         * @return
         */
        @RequestMapping("/importCredit")
        @ResponseBody
        public String importCredit () throws Exception {
            JSONArray jsonArray = new JSONArray();
            JSONArray updateJsonArray = new JSONArray();
            Map<String, Long> accountMap = new HashMap<>();
            Map<String, Long> accountNoMap = new HashMap<>();
            Map<String, Long> creditMap = new HashMap<>();
            Map<String, Long> departmentMap = new HashMap<>();
            Map<String, Long> ZQMap = new HashMap<>();
            Map<String, Long> ERPMap = new HashMap<>();
            Map<String, Integer> levelMap = new HashMap<>();
            levelMap.put("A", 1);
            levelMap.put("B", 2);
            levelMap.put("C", 3);
            levelMap.put("D", 4);

            String sql = "select id,accountName,customItem201__c from account";
            JSONObject byXoqlSimple = queryServer.getByXoqlSimple(sql);
            JSONArray allByXoqlSample = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple, sql);
            for (int i = 0; i < allByXoqlSample.size(); i++) {
                JSONObject jsonObject = allByXoqlSample.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                String accountName = jsonObject.getString("accountName");
                String customItem201__c = jsonObject.getString("customItem201__c");
                accountMap.put(accountName, id);
                accountNoMap.put(customItem201__c, id);
            }

            String creditSql = "select id,name from customEntity21__c";
            JSONObject byXoqlSimple1 = queryServer.getByXoqlSimple(creditSql);
            JSONArray allByXoqlSample1 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple1, creditSql);
            for (int i = 0; i < allByXoqlSample1.size(); i++) {
                JSONObject jsonObject = allByXoqlSample1.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                String name = jsonObject.getString("name");
                creditMap.put(name, id);
            }

            String departSql = "select id,customItem126__c from department";
            String bySql = queryServer.getBySql(departSql);
            JSONArray all = queryServer.findAll(getToken(), bySql, departSql);
            for (int i = 0; i < all.size(); i++) {
                JSONObject jsonObject = all.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                String customItem126__c = jsonObject.getString("customItem126__c");
                departmentMap.put(customItem126__c, id);
            }
            String zqSql = "select id,name from customEntity33__c";
            JSONObject byXoqlSimple2 = queryServer.getByXoqlSimple(zqSql);
            JSONArray allByXoqlSample2 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple2, zqSql);
            for (int i = 0; i < allByXoqlSample2.size(); i++) {
                JSONObject jsonObject = allByXoqlSample2.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                String name = jsonObject.getString("name");
                ZQMap.put(name, id);
            }
            String ERPSql = "select id,customItem4__c,name from customEntity63__c";
            JSONObject byXoqlSimple3 = queryServer.getByXoqlSimple(ERPSql);
            JSONArray allByXoqlSample3 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple3, ERPSql);
            for (int i = 0; i < allByXoqlSample3.size(); i++) {
                JSONObject jsonObject = allByXoqlSample3.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                Long accountId = jsonObject.getLong("customItem4__c");
                String name = jsonObject.getString("name");
                ERPMap.put(accountId + "," + name, id);
            }
            File file = new File("C:\\Users\\lucg\\Desktop\\HXSW&SDHY&SDUSA事业部信用(1).xlsx");
            FileInputStream input = new FileInputStream(file);
            MultipartFile multipartFile = new MockMultipartFile("file", file.getName(), "text/plain", IOUtils.toByteArray(input));
            JSONArray successJsonArray = importExcel(multipartFile, Credit.class);
            for (int i = 0; i < successJsonArray.size(); i++) {
                JSONObject jsonObject = successJsonArray.getJSONObject(i);
                String zt = jsonObject.getString("zt");
                if ("SDFRD".equals(zt)) {
                    zt = "SDHY";
                }
                zt = "LIVE_" + zt;
                String accountNo = jsonObject.getString("accountNo");
                String accountName = jsonObject.getString("accountName");
                String syb = jsonObject.getString("syb");
                String sybName = jsonObject.getString("sybName");
                String zqtk = jsonObject.getString("zqtk");
                String tkDescribe = jsonObject.getString("tkDescribe");
                String accountLevel = jsonObject.getString("accountLevel");
                String creditLimit = jsonObject.getString("creditLimit");
                Long accountId = accountMap.get(accountName);
                if (accountId == null || accountId == 0) {
                    accountId = accountNoMap.get(accountNo);
                }
                Long erpId = ERPMap.get(accountId + "," + zt);
                Long sybId = departmentMap.get(syb);
                Long zqId = ZQMap.get(zqtk);
                Integer level = levelMap.get(accountLevel);
                Long creditId = creditMap.get(zt + syb + accountName);
                if (creditId == null || creditId == 0) {
                    JSONObject object = new JSONObject();
                    object.put("entityType", 1214917399855445L);
                    object.put("name", zt + syb + accountName);
                    object.put("customItem3__c", StringUtils.isBlank(creditLimit) ? 0 : Double.valueOf(creditLimit));
                    if (sybId != null && sybId != 0) {
                        object.put("customItem6__c", sybId);
                    }
                    if (zqId != null && zqId != 0) {
                        object.put("customItem8__c", zqId);
                    }
                    if (accountId != null && accountId != 0) {
                        object.put("customItem9__c", accountId);
                    }
                    if (erpId != null && erpId != 0) {
                        object.put("customItem13__c", erpId);
                    }
                    if (level != null && level != 0) {
                        object.put("customItem10__c", level);
                    }
                    jsonArray.add(object);
                } else {
                    JSONObject object = new JSONObject();
                    object.put("id", creditId);
                    object.put("customItem3__c", StringUtils.isBlank(creditLimit) ? 0 : Double.valueOf(creditLimit));
                    if (sybId != null && sybId != 0) {
                        object.put("customItem6__c", sybId);
                    }
                    if (zqId != null && zqId != 0) {
                        object.put("customItem8__c", zqId);
                    }
                    if (accountId != null && accountId != 0) {
                        object.put("customItem9__c", accountId);
                    }
                    if (erpId != null && erpId != 0) {
                        object.put("customItem13__c", erpId);
                    }
                    if (level != null && level != 0) {
                        object.put("customItem10__c", level);
                    }
                    updateJsonArray.add(object);
                }


            }
            if (jsonArray.size() > 0) {
                bulkAPI.createDataTaskJob(jsonArray, "customEntity21__c", "insert");
            }
            if (updateJsonArray.size() > 0) {
                bulkAPI.createDataTaskJob(updateJsonArray, "customEntity21__c", "update");
            }
            return null;
        }
        /**
         * 解析Excel
         *
         * @param file
         * @return
         */
        public <T > JSONArray importExcel(MultipartFile file, Class < ? > pojoClass){
            ImportParams importParams = new ImportParams();
            importParams.setHeadRows(1);
            importParams.setNeedVerfiy(true);
            String filename = file.getOriginalFilename();
            try {
                ExcelImportResult<T> result = ExcelImportUtil.importExcelVerify(file.getInputStream(), pojoClass, importParams);
                //成功导入
                List<T> list = result.getList();
                JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(list));
                return jsonArray;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        /**
         * 将有客户编码的客户创建到ERP客户账套模块
         * @return
         */
        @RequestMapping("/addAccountToERP")
        @ResponseBody
        public String addAccountToERP () throws Exception {
            JSONArray jsonArray = new JSONArray();
            Map<String, String> map = new HashMap<>();
            String sql = "select id,customItem201__c from account where customItem201__c is not null";
            JSONObject byXoqlSimple = queryServer.getByXoqlSimple(sql);
            JSONArray allByXoqlSample = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple, sql);
            String ERPSql = "select customItem4__c,customItem2__c from customEntity63__c";
            JSONObject byXoqlSimple1 = queryServer.getByXoqlSimple(ERPSql);
            JSONArray allByXoqlSample1 = queryServer.getAllByXoqlSample(getToken(), byXoqlSimple1, ERPSql);
            for (int i = 0; i < allByXoqlSample1.size(); i++) {
                JSONObject jsonObject = allByXoqlSample1.getJSONObject(i);
                Long accountID = jsonObject.getLong("customItem4__c");
                String customItem2__c = jsonObject.getString("customItem2__c");
                map.put(accountID + "," + customItem2__c, "success");
            }
            for (int i = 0; i < allByXoqlSample.size(); i++) {
                JSONObject jsonObject = allByXoqlSample.getJSONObject(i);
                Long id = jsonObject.getLong("id");
                String customItem201__c = jsonObject.getString("customItem201__c");
                String s = map.get(id + "," + customItem201__c);
                if (StringUtils.isBlank(s)) {
                    JSONObject object = new JSONObject();
                    object.put("entityType", 1340810097181017L);
                    object.put("customItem5__c", "同步完成");
                    object.put("customItem4__c", id);
                    object.put("name", "LIVE_HXSW");
                    object.put("customItem2__c", customItem201__c);
                    object.put("customItem3__c", 6);
                    jsonArray.add(object);
                }
            }
            bulkAPI.createDataTaskJob(jsonArray, "customEntity63__c", "insert");
            return null;
        }


        @RequestMapping("/readXML")
        @ResponseBody
        public String readXML (@RequestBody JSONObject jsonObject){
            try {
                String xml = jsonObject.getString("xml");
                JSONArray jsonArray = XmlUtil.unPackageXML(xml);
                return sendJson(jsonArray, true);
            } catch (Exception e) {
                return sendJson("解析异常：" + e.getMessage(), false);
            }
        }
        @RequestMapping("/readApproveXML")
        @ResponseBody
        public String readApproveXML (@RequestBody JSONObject jsonObject){
            try {
                String xml = jsonObject.getString("xml");
                JSONArray jsonArray = XmlUtil.unPackageApproveXML(xml);
                return sendJson(jsonArray, true);
            } catch (Exception e) {
                return sendJson("解析异常：" + e.getMessage(), false);
            }
        }
//    @RequestMapping("/test1")
//    @ResponseBody
//    public String test1(){
//        try {
//            JSONObject contractTemplete = queryServer.getContractTemplete("原料销售合同（外销）");
//            String fileName = contractTemplete.getString("fileName");
//            Long templateId = contractTemplete.getLongValue("id");
////            if (templateId==null||templateId==0){
////                map.put(false,"未查询到对应模板——"+templateName);
////                return map;
////            }
//            queryServer.getContract(, 1333844347208080L);
//            return "";
//        } catch (Exception e) {
//            return sendJson("解析异常："+e.getMessage(), false);
//        }
//    }
        @PostMapping("/readCSV")
        @ResponseBody
        public String readCSV (MultipartFile file){
            try {
                JSONArray jsonArray = CSVUtil.csv(file);
                return sendJson(jsonArray, true);
            } catch (Exception e) {
                return sendJson("解析异常：" + e.getMessage(), false);
            }
        }
        @RequestMapping("/readOldXML")
        @ResponseBody
        public String readOldXML (@RequestBody JSONObject jsonObject){
            try {
                String xml = jsonObject.getString("xml");
                JSONArray jsonArray = XmlUtil.unPackageXMLOld(xml);
                return sendJson(jsonArray, true);
            } catch (Exception e) {
                return sendJson("解析异常：" + e.getMessage(), false);
            }
        }
        @RequestMapping("/test")
        @ResponseBody
        public String test () {
            try {
                String xml = JsonReader.excutetest("报文.xml");
                JSONArray jsonArray = XmlUtil.unPackageXMLOld(xml);
                return sendJson(jsonArray, true);
            } catch (Exception e) {
                return sendJson("解析异常：" + e.getMessage(), false);
            }
        }

        /**
         * 根据查询的物料和批次，显示标准及指标信息
         * @param jsonObject
         * @return
         */
        @RequestMapping("/getOverDateData")
        @ResponseBody
        public String getOverDateData (@RequestBody JSONObject jsonObject){
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
            String custnum = jsonObject.getString("custnum");//客户编号
            String bu = jsonObject.getString("bu");//事业部
            String term = jsonObject.getString("term");//条款
            String uf_overlimit3 = jsonObject.getString("uf_overlimit3");//该客户该事业部在销售易里提交状态的预估单总金额
            Holder<String> pa = new Holder<>("<Parameters><Parameter>" + custnum + "</Parameter>" +
                    "<Parameter>" + bu + "</Parameter>" +
                    "<Parameter>" + term + "</Parameter>" +
                    "<Parameter>" + uf_overlimit3 + "</Parameter>" +
                    "<Parameter ByRef=\"Y\"></Parameter></Parameters>");
            String customNum = getCustomNum(ERPtoken, pa, "hxsp_calc_ecocredit_crm");
            System.out.println(customNum);
            return customNum;
        }

        @RequestMapping(value = "/getAllSelects", produces = {"application/json;charset=UTF-8"})
        @ResponseBody
        public String getAllSelects () {
            try {
                JSONObject responseJSON = new JSONObject();
                JSONArray itemAndLot = getItemAndLot();
                JSONArray itemArray = new JSONArray();
                JSONArray lotArray = new JSONArray();
                Set set = new HashSet();
                Set set1 = new HashSet();
                for (int i = 0; i < itemAndLot.size(); i++) {
                    JSONObject itemObject = new JSONObject();
                    JSONObject lotObject = new JSONObject();
                    JSONObject jsonObject = itemAndLot.getJSONObject(i);
                    String customItem130__c = jsonObject.getString("customItem130__c");
                    String customItem129__c = jsonObject.getString("customItem129__c");
                    if (StringUtils.isNotBlank(customItem130__c)) {
                        if (set.add(customItem130__c)) {
                            itemObject.put("item", customItem130__c);
                            itemArray.add(itemObject);
                        }
                    }
                    if (StringUtils.isNotBlank(customItem129__c)) {
                        if (set1.add(customItem129__c)) {
                            lotObject.put("lot", customItem129__c);
                            lotArray.add(lotObject);
                        }
                    }
                }
                JSONArray customerStandards = getCustomerStandards();
                JSONArray enterpriseStandards = getEnterpriseStandards();
                responseJSON.put("item", itemArray);
                responseJSON.put("lot", lotArray);
                responseJSON.put("customerStandards", customerStandards);
                responseJSON.put("enterpriseStandards", enterpriseStandards);
                return sendJson(responseJSON, true);
            } catch (Exception e) {
                return sendJson("查询异常：" + e.getMessage(), false);
            }
        }

        /**
         * 根据查询的物料和批次，显示标准及指标信息
         * @param jsonObject
         * @return
         */
        @RequestMapping(value = "/getOtherInfo", produces = {"application/json;charset=UTF-8"})
        @ResponseBody
        public String getOtherInfo (@RequestBody JSONObject jsonObject){
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
            String Item = jsonObject.getString("Item");
            String Lot = jsonObject.getString("Lot");
            String Whse = jsonObject.getString("Whse");
            String Loc = jsonObject.getString("Loc");
            String where = "";
            if (StringUtils.isNotBlank(Item)) {
                where = "Item='" + Item + "'";
            }
            if (StringUtils.isNotBlank(Lot)) {
                if (StringUtils.isNotBlank(where)) {
                    where += " and Lot='" + Lot + "'";
                } else {
                    where += "Lot='" + Lot + "'";
                }
            }
            if (StringUtils.isNotBlank(Whse)) {
                Whse = Whse.split("-")[0];
                if (StringUtils.isNotBlank(where)) {
                    where += " and Whse='" + Whse + "'";
                } else {
                    where += "Whse='" + Whse + "'";
                }
            }
            if (StringUtils.isNotBlank(Loc)) {
                if (StringUtils.isNotBlank(where)) {
                    where += " and Loc='" + Loc + "'";
                } else {
                    where += "Loc='" + Loc + "'";
                }
            }


            String result = idoWebServiceSoap.loadJson(ERPtoken, "HXLotLocSpecifics", "Description,MatlQty,QtyPackage,HasReceived", where, "Specific ASC", "", -1);
            System.out.println(result);
            JSONObject resultJson = JSONObject.parseObject(result);
            JSONArray Items = resultJson.getJSONArray("Items");
            JSONArray propertyArray = new JSONArray();
            for (int i = 0; i < Items.size(); i++) {
                JSONObject jsonObject1 = Items.getJSONObject(i);
                JSONArray properties = jsonObject1.getJSONArray("Properties");
                JSONObject propertieObject = new JSONObject();
                for (int j = 0; j < properties.size(); j++) {
                    JSONObject jsonObject2 = properties.getJSONObject(j);
                    if (j == 0) {
                        String property_0 = jsonObject2.getString("Property");
                        propertieObject.put("StandardCode", property_0);
                    } else {
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
        @RequestMapping(value = "/getMainData", produces = {"application/json;charset=UTF-8"})
        @ResponseBody
        public String getMainData (@RequestBody JSONObject jsonObject){
            String customNum = null;
            try {
                String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
                String BItem = jsonObject.getString("BItem");
                String EItem = jsonObject.getString("EItem");
                String BLot = jsonObject.getString("BLot");
                String ELot = jsonObject.getString("ELot");
                BigDecimal BFeature18 = jsonObject.getBigDecimal("BFeature18");
                BigDecimal EFeature18 = jsonObject.getBigDecimal("EFeature18");
                BigDecimal BFeature16 = jsonObject.getBigDecimal("BFeature16");
                BigDecimal EFeature16 = jsonObject.getBigDecimal("EFeature16");
                BigDecimal BFeature53 = jsonObject.getBigDecimal("BFeature53");
                BigDecimal EFeature53 = jsonObject.getBigDecimal("EFeature53");
                BigDecimal BFeature19 = jsonObject.getBigDecimal("BFeature19");
                BigDecimal EFeature19 = jsonObject.getBigDecimal("EFeature19");
                BigDecimal BFeature43 = jsonObject.getBigDecimal("BFeature43");
                BigDecimal EFeature43 = jsonObject.getBigDecimal("EFeature43");
                BigDecimal BFeature5 = jsonObject.getBigDecimal("BFeature5");
                BigDecimal EFeature5 = jsonObject.getBigDecimal("EFeature5");
                BigDecimal BQtyOnHand = jsonObject.getBigDecimal("BQtyOnHand");
                BigDecimal EQtyOnHand = jsonObject.getBigDecimal("EQtyOnHand");
                String Site = jsonObject.getString("Site");
                String CustomerStandard = jsonObject.getString("CustomerStandard");
                String EnterpriseStandard = jsonObject.getString("EnterpriseStandard");
                String lot = jsonObject.getString("lot");
                String whse = jsonObject.getString("whse");

//        BFeature18 = BFeature18==null?new BigDecimal("0"):BFeature18;
//        EFeature18 = EFeature18==null?new BigDecimal("0"):EFeature18;
//        BFeature16 = BFeature16==null?new BigDecimal("0"):BFeature16;
//        EFeature16 = EFeature16==null?new BigDecimal("0"):EFeature16;
//        BFeature53 = BFeature53==null?new BigDecimal("0"):BFeature53;
//        EFeature53 = EFeature53==null?new BigDecimal("0"):EFeature53;
//        BFeature19 = BFeature19==null?new BigDecimal("0"):BFeature19;
//        EFeature19 = EFeature19==null?new BigDecimal("0"):EFeature19;
//        BFeature43 = BFeature43==null?new BigDecimal("0"):BFeature43;
//        EFeature43 = EFeature43==null?new BigDecimal("0"):EFeature43;
//        BFeature5 = BFeature5 ==null?new BigDecimal("0"):BFeature5;
//        EFeature5 = EFeature5 ==null?new BigDecimal("0"):EFeature5;
//        BFeature5 = BQtyOnHand ==null?new BigDecimal("0"):BQtyOnHand;
//        EFeature5 = EQtyOnHand ==null?new BigDecimal("0"):EQtyOnHand;
                String str_BFeature18 = BFeature18 == null ? "" : BFeature18 + "";
                String str_EFeature18 = EFeature18 == null ? "" : EFeature18 + "";
                String str_BFeature16 = BFeature16 == null ? "" : BFeature16 + "";
                String str_EFeature16 = EFeature16 == null ? "" : EFeature16 + "";
                String str_BFeature53 = BFeature53 == null ? "" : BFeature53 + "";
                String str_EFeature53 = EFeature53 == null ? "" : EFeature53 + "";
                String str_BFeature19 = BFeature19 == null ? "" : BFeature19 + "";
                String str_EFeature19 = EFeature19 == null ? "" : EFeature19 + "";
                String str_BFeature43 = BFeature43 == null ? "" : BFeature43 + "";
                String str_EFeature43 = EFeature43 == null ? "" : EFeature43 + "";
                String str_BFeature5 = BFeature5 == null ? "" : BFeature5 + "";
                String str_EFeature5 = EFeature5 == null ? "" : EFeature5 + "";
                String str_BQtyOnHand = BQtyOnHand == null ? "" : BQtyOnHand + "";
                String str_EQtyOnHand = EQtyOnHand == null ? "" : EQtyOnHand + "";
                BItem = StringUtils.isBlank(BItem) ? "" : BItem;
                EItem = StringUtils.isBlank(EItem) ? "" : EItem;
                BLot = StringUtils.isBlank(BLot) ? "" : BLot;
                ELot = StringUtils.isBlank(ELot) ? "" : ELot;
                Site = StringUtils.isBlank(Site) ? "" : Site;
                CustomerStandard = StringUtils.isBlank(CustomerStandard) ? "" : CustomerStandard;
                EnterpriseStandard = StringUtils.isBlank(EnterpriseStandard) ? "" : EnterpriseStandard;
                lot = StringUtils.isBlank(lot) ? "" : lot;
                whse = StringUtils.isBlank(whse) ? "" : whse;

                Holder<String> pa = new Holder<>("<Parameters><Parameter>" + BItem + "</Parameter>" +
                        "<Parameter>" + EItem + "</Parameter>" +
                        "<Parameter>" + BLot + "</Parameter>" +
                        "<Parameter>" + ELot + "</Parameter>" +
                        "<Parameter>" + str_BFeature18 + "</Parameter>" +
                        "<Parameter>" + str_EFeature18 + "</Parameter>" +
                        "<Parameter>" + str_BFeature16 + "</Parameter>" +
                        "<Parameter>" + str_EFeature16 + "</Parameter>" +
                        "<Parameter>" + str_BFeature53 + "</Parameter>" +
                        "<Parameter>" + str_EFeature53 + "</Parameter>" +
                        "<Parameter>" + str_BFeature19 + "</Parameter>" +
                        "<Parameter>" + str_EFeature19 + "</Parameter>" +
                        "<Parameter>" + str_BFeature43 + "</Parameter>" +
                        "<Parameter>" + str_EFeature43 + "</Parameter>" +
                        "<Parameter>" + str_BFeature5 + "</Parameter>" +
                        "<Parameter>" + str_EFeature5 + "</Parameter>" +
                        "<Parameter>" + Site + "</Parameter>" +
                        "<Parameter>" + CustomerStandard + "</Parameter>" +
                        "<Parameter>" + EnterpriseStandard + "</Parameter>" +
                        "<Parameter>" + str_BQtyOnHand + "</Parameter>" +
                        "<Parameter>" + str_EQtyOnHand + "</Parameter>" +
                        "<Parameter>" + lot + "</Parameter>" +
                        "<Parameter>" + whse + "</Parameter>" +
                        "<Parameter ByRef=\"Y\"></Parameter></Parameters>");
                customNum = getCustomNum(ERPtoken, pa, "HXItemLotFeatureSearchSp_CRM");
                JSONArray jsonArray = XmlUtil.unPackageMAIN(customNum);
                return sendJson(jsonArray, true);
            } catch (Exception e) {
                e.printStackTrace();
                return sendJson("查询异常", false, e.getMessage());
            }
        }
        @RequestMapping("/test2")
        @ResponseBody
        public String getLotFeatureSp (String Item, String Lot){
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
            Holder<String> pa = new Holder<>("<Parameters><Parameter>" + Item + "</Parameter><Parameter>" + Lot + "</Parameter><Parameter ByRef=\"Y\"></Parameter></Parameters>");
            String customNum = getCustomNum(ERPtoken, pa, "HXCLM_GetLotFeatureSp");
            System.out.println(customNum);
            return customNum;
        }
        /**
         * 调用存储过程
         * @param token
         * @return
         */
        public String getCustomNum (String token, Holder < String > pa, String methodName){
            Holder<Object> pa2 = new Holder<>();
            idoWebServiceSoap.callMethod(token, "SP!", methodName, pa, pa2);
            //输出返回结果
//        System.err.println(pa.value);
            return pa.value;
        }


        public JSONArray getItemAndLot () throws Exception {
            String sql = "select customItem130__c,customItem129__c from product";
            JSONObject bySql = queryServer.getByXoqlSimple(sql);
            JSONArray all = queryServer.getAllByXoqlSample(getToken(), bySql, sql);
            return all;
        }
        /**
         * 查询客户标准
         * @return
         */
        public JSONArray getCustomerStandards ()throws Exception {
            JSONArray responseJSON = new JSONArray();
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
            String result = idoWebServiceSoap.loadJson(ERPtoken, "HXCustomerStandards", "StandardCode,Description", "", "Description ASC", "", -1);
            ModuleOutputLogger.handSyncBankInfo.info("ERP查询结果：" + result);
            JSONObject resultJson = JSONObject.parseObject(result);
            JSONArray Items = resultJson.getJSONArray("Items");
            for (int i = 0; i < Items.size(); i++) {
                JSONObject jsonObject = Items.getJSONObject(i);
                JSONArray properties = jsonObject.getJSONArray("Properties");
                JSONObject propertieObject = new JSONObject();
                for (int j = 0; j < properties.size(); j++) {
                    JSONObject jsonObject1 = properties.getJSONObject(j);
                    if (j == 0) {
                        String property_0 = jsonObject1.getString("Property");
                        propertieObject.put("StandardCode", property_0);
                    } else {
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
        public JSONArray getEnterpriseStandards ()throws Exception {
            JSONArray responseJSON = new JSONArray();
            String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
            String result = idoWebServiceSoap.loadJson(ERPtoken, "HXEnterpriseStandards", "StandardCode,Description", "", "Description ASC", "", -1);
            System.out.println("ERP查询结果：" + result);
            ModuleOutputLogger.handSyncBankInfo.info("ERP查询结果：" + result);
            JSONObject resultJson = JSONObject.parseObject(result);
            JSONArray Items = resultJson.getJSONArray("Items");
            for (int i = 0; i < Items.size(); i++) {
                JSONObject jsonObject = Items.getJSONObject(i);
                JSONArray properties = jsonObject.getJSONArray("Properties");
                JSONObject propertieObject = new JSONObject();
                for (int j = 0; j < properties.size(); j++) {
                    JSONObject jsonObject1 = properties.getJSONObject(j);
                    if (j == 0) {
                        String property_0 = jsonObject1.getString("Property");
                        propertieObject.put("StandardCode", property_0);
                    } else {
                        String property_1 = jsonObject1.getString("Property");
                        propertieObject.put("Description", property_1);
                    }
                }
                responseJSON.add(propertieObject);
            }
            return responseJSON;
        }
        /**
         * 创建合同到相关
         * @param param
         * @return
         */
        @RequestMapping("/createDoc")
        @ResponseBody
        public String createDoc (@RequestBody JSONObject param){
            try {
                Long dataId = param.getLong("dataId");
                String language = param.getString("language");
                String templateName = "";
                Integer mblx = param.getInteger("mblx");
                if (mblx.intValue() == 0) {
                    if ("CHN".equals(language)) {//中文合同
                        templateName = "原料销售合同（内销）";
                    } else {//英文合同
                        templateName = "原料销售合同（外销）";
                    }
                    JSONObject contractTemplete = queryServer.getContractTemplete(templateName);
                    if (contractTemplete == null) {
                        return sendJson("未查询到对应模板——" + templateName, false);
                    }
                    String fileName = contractTemplete.getString("fileName");
                    Long templateId = contractTemplete.getLongValue("id");
                    if (templateId == null || templateId == 0) {
                        return sendJson("未查询到对应模板——" + templateName, false);
                    }
                    Map<Boolean, String> contract = queryServer.getContract(dataId, templateId);
                    Boolean key = contract.entrySet().iterator().next().getKey();
                    String value = contract.entrySet().iterator().next().getValue();
                    if (key) {
                        int i = fileName.lastIndexOf(".");
                        String substring = fileName.substring(i + 1, fileName.length());
                        File file = queryServer.base64ToFile(value, "contract_" + df1.format(new Date()) + "." + substring);
                        String s = queryServer.uploadFile(file);
                        if (org.apache.commons.lang3.StringUtils.isBlank(s)) {
                            System.err.println("创建合同失败：" + s);
                        }
                        JSONObject object1 = JSONObject.parseObject(s);
                        long docId = object1.getLongValue("id");
                        //创建文档到合同下
                        String doc = queryServer.createDoc(dataId, docId);
                        JSONObject object2 = JSONObject.parseObject(doc);
                        if (!object2.containsKey("id")) {
                            System.err.println("上传合同失败，id为空：" + object2);
                        }
                    } else {
                        log.error(value);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return sendJson("上传完成", true);
        }

        /**
         * 审批 CRM->OA
         * @param param
         * @return
         */
        @RequestMapping("/sync")
        @ResponseBody
        public String syncFreeSample (@RequestBody JSONObject param){
            System.out.println("同步OA开始。。。");
            Long dataId = param.getLong("dataId");
            String isContract = param.getString("isContract");//合同打印
            try {
                Integer creatorId = param.getInteger("creatorId");
                WorkflowRequestInfo workflowRequestInfo = workFlowUtil.packageXML(param);
                String result = addData(workflowRequestInfo, creatorId);
                //解析报文
                Long value = Long.valueOf(result);
                if (value < 0) {
                    String errorMessage = FlowErrorCode.getErrorMessage(value);
                    throw new Exception(errorMessage);
                }
                Long id = value;
                Long belongId = param.getLong("belongId");
                JSONObject object = new JSONObject();
                object.put("customItem1__c", dataId + "");
                object.put("customItem2__c", id + "");
                object.put("customItem3__c", belongId + "");
                queryServer.createCustomize(object, 1289974802579888L, getToken());
                JSONObject updateObject = new JSONObject();
                updateObject.put("id", dataId);
                updateObject.put("customItem46__c", id + "");
                updateObject.put("customItem44__c", "同步成功");
//            if ("1".equals(isContract)){
//                queryServer.updateContractById(updateObject);
//            }else {
                queryServer.updateCustomizeById(updateObject);
//            }
                System.out.println("同步OA结束。。。");
                return sendJson("同步成功", true);
            } catch (Exception e) {
                e.printStackTrace();
                JSONObject updateObject = new JSONObject();
                updateObject.put("id", dataId);
                updateObject.put("customItem44__c", "同步失败：" + e.getMessage());
                try {
//                if ("1".equals(isContract)){
//                    queryServer.updateContractById(updateObject);
//                }else {
                    queryServer.updateCustomizeById(updateObject);
//                }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                System.out.println("同步OA结束。。。");
                return sendJson("同步失败：" + e.getMessage(), false);
            }
        }


        public String getData (JSONObject dataObject, String fieldName){
            String data = null;
            try {
                data = dataObject.getString(fieldName);
            } catch (Exception e) {
                data = null;
            }
            return data;
        }
        public String getArrayToData (JSONObject dataObject, String fieldName){
            String data = null;
            try {
                data = (String) dataObject.getJSONArray(fieldName).toArray()[0];
            } catch (Exception e) {
                data = null;
            }
            return data;
        }
        public String getSplitData (JSONObject dataObject, String fieldName){
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
        public Float getFloatData (JSONObject dataObject, String fieldName){
            Float data = 0F;
            try {
                data = dataObject.getFloatValue(fieldName);
            } catch (Exception e) {
                data = 0F;
            }
            return data;
        }
        public String geDateData (JSONObject dataObject, String fieldName){
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
         * @param workflowRequestInfo
         * @return
         */
        public String addData (WorkflowRequestInfo workflowRequestInfo, Integer in1) throws Exception {
            WorkflowServicePortType workflowServiceHttpPort = new WorkflowServiceLocator().getWorkflowServiceHttpPort();
            String result = workflowServiceHttpPort.doCreateWorkflowRequest(workflowRequestInfo, in1);
//        String result ="";
            return result;

        }
}
