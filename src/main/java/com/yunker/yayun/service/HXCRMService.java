package com.yunker.yayun.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.yunker.yayun.controller.CommonController;
import com.yunker.yayun.oaPackage.AnyType2AnyTypeMapEntry;
import com.yunker.yayun.oaPackage.HXCRMServiceLocator;
import com.yunker.yayun.oaPackage.HXCRMServicePortType;
import com.yunker.yayun.util.*;
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
public class HXCRMService extends CommonController {

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


    public static void main(String[] args) {
        try {
            HXCRMServicePortType hxcrmServiceHttpPort = new HXCRMServiceLocator().getHXCRMServiceHttpPort();
            AnyType2AnyTypeMapEntry[][] allStaffInfoList = hxcrmServiceHttpPort.getAllStaffInfoList();

            System.out.println("***********************");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void HXCRMoa() throws Exception {

        String sql="select customItem1__c,customItem2__c,customItem3__c,customItem4__c,customItem5__c,customItem6__c ,customItem7__c from customEntity29__c  ";
        String bySql = queryServer.getBySql(sql);
        JSONArray all = queryServer.findAll(getToken(), bySql, sql);
        for (int i = 0; i < all.size(); i++) {
            JSONObject jsonObject = all.getJSONObject(i);
            String customItem1__c = jsonObject.getString("customItem1__c");
            String customItem2__c = jsonObject.getString("customItem2__c");
            String customItem3__c = jsonObject.getString("customItem3__c");
            String customItem4__c = jsonObject.getString("customItem4__c");
            String customItem5__c = jsonObject.getString("customItem5__c");
            String customItem6__c = jsonObject.getString("customItem6__c");
            String customItem7__c = jsonObject.getString("customItem7__c");
            if (jsonObject !=null){
                JSONObject object = new JSONObject();
                object.put("customItem1__c", customItem1__c);
                object.put("customItem2__c", customItem1__c);
                object.put("customItem3__c", customItem1__c);
                object.put("customItem4__c", customItem1__c);
                object.put("customItem5__c", customItem1__c);
                object.put("customItem6__c", customItem1__c);
                object.put("customItem7__c", customItem1__c);
                object.put("entityType", "1243278944453008");

                bulkAPI.createDataTaskJob(all, "customEntity37__c", "update");
            }else {
                bulkAPI.createDataTaskJob(all, "customEntity37__c", "insert");
            }

        }
    }



}
