package com.yunker.eai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.yunker.eai.controller.CommonController;
import com.yunker.eai.oaPackage.AnyType2AnyTypeMapEntry;
import com.yunker.eai.oaPackage.GetAllStaffInfoList;
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
import java.io.Serializable;
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

public class HXCRMOAService extends CommonController {

    @Autowired
    private HttpClientUtil httpClientUtil;
    @Autowired
    private QueryServer queryServer;
    @Autowired
    private BulkAPI bulkAPI;


    /**
     * 获取token
     *
     * @return
     * @throws Exception
     */
//    @Scheduled(cron = "0 0 0 */1 * ?")
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


    public void HXCRMoa() throws Exception {

        Map<String, String> oaMap = new HashMap<>();
        AnyType2AnyTypeMapEntry[][] allStaffInfoList = null;
        try {
            //查询OA中OA账号
            HXCRMServicePortType hxcrmServiceHttpPort = new HXCRMServiceLocator().getHXCRMServiceHttpPort();
            allStaffInfoList = hxcrmServiceHttpPort.getAllStaffInfoList();

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        //查询CRM中oa账号
        String sql = "select id, customItem1__c,customItem2__c,customItem3__c,customItem4__c,customItem5__c,customItem6__c ,customItem7__c from customEntity37__c  ";
        String bySql = queryServer.getBySql(sql);
        JSONArray all = queryServer.findAll(getToken(), bySql, sql);
        JSONArray oaArray = new JSONArray();
        JSONArray oaupdateArray = new JSONArray();

        for (int i = 0; i < all.size(); i++) {
            JSONObject jsonObject = all.getJSONObject(i);
            String userid = jsonObject.getString("customItem1__c");
            String id = jsonObject.getString("id");
            oaMap.put(userid, id);
        }

        JSONArray jSONArray = new JSONArray();
        for (AnyType2AnyTypeMapEntry[] anyType2AnyTypeMapEntries : allStaffInfoList) {
            JSONObject jSONObject = new JSONObject();
            for (AnyType2AnyTypeMapEntry item : anyType2AnyTypeMapEntries) {
                String key = (String) item.getKey();
                Object value = item.getValue();
                jSONObject.put(key, value);
            }
            jSONArray.add(jSONObject);
        }


        for (int i = 0; i < jSONArray.size(); i++) {
            JSONObject jsonObject = jSONArray.getJSONObject(i);
            String id = jsonObject.getString("id");
            String workcode = jsonObject.getString("workcode");
            String lastname = jsonObject.getString("lastname");
            String departmentid = jsonObject.getString("departmentid");
            String subcompanyid1 = jsonObject.getString("subcompanyid1");
            String departmentname = jsonObject.getString("departmentname");
            String subcompanyname = jsonObject.getString("subcompanyname");

            String customItem1__c = id;
            String customItem2__c = workcode;
            String customItem3__c = lastname;
            String customItem4__c = subcompanyname;
            String customItem5__c = departmentid;
            String customItem6__c = departmentname;
            String customItem7__c = subcompanyid1;


//            boolean contains = oaMap.containsKey(id);
            String oaId =oaMap.get(id);
            if (oaId == null ) {
                JSONObject object = new JSONObject();
                object.put("customItem1__c", customItem1__c);
                object.put("customItem2__c", customItem2__c);
                object.put("customItem3__c", customItem3__c);
                object.put("customItem4__c", customItem4__c);
                object.put("customItem5__c", customItem5__c);
                object.put("customItem6__c", customItem6__c);
                object.put("customItem7__c", customItem7__c);
                object.put("entityType", "1243278944453008");
                oaArray.add(object);
            } else {
                JSONObject updateObject = new JSONObject();
                updateObject.put("id", oaId);
                updateObject.put("customItem2__c", customItem2__c);
                updateObject.put("customItem3__c", customItem3__c);
                updateObject.put("customItem4__c", customItem4__c);
                updateObject.put("customItem5__c", customItem5__c);
                updateObject.put("customItem6__c", customItem6__c);
                updateObject.put("customItem7__c", customItem7__c);
                oaupdateArray.add(updateObject);

            }
        }


        if (oaArray.size() > 0) {
//            bulkAPI.createDataTaskJob(oaArray, "customEntity37__c", "insert");

        }
        if (oaupdateArray.size() > 0) {
            bulkAPI.createDataTaskJob(oaupdateArray, "customEntity37__c", "update");
//            for (int i = 0; i < 2; i++) {
//                JSONObject jsonObject = oaupdateArray.getJSONObject(i);
//                queryServer.updateCustomizeById(jsonObject);
//            }


        }
    }

}


//    public static void main(String[] args) throws Exception {
//        HXCRMOAService service = new HXCRMOAService();
//        service.HXCRMoa();
//
//    }





