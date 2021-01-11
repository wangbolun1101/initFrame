package com.yunker.eai.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yunker.eai.service.HXCRMOAService;
import com.yunker.eai.util.*;
import lombok.extern.slf4j.Slf4j;
import mypackage.IDOWebService;
import mypackage.IDOWebServiceSoap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.xml.ws.Holder;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


@Slf4j
@Controller
@RequestMapping("/query")
@CrossOrigin(origins = "https://login.xiaoshouyi.com", maxAge = 3600)
public class queryTerminal extends CommonController {

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

    //实例化接口
    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    private DateFormat df1 = new SimpleDateFormat("yyyyMMdd");
    private IDOWebService ST = new IDOWebService();
    private IDOWebServiceSoap idoWebServiceSoap = ST.getIDOWebServiceSoap();
    private String userId = "crm";//用户名
    private String pswd = "Crm123456";//密码
    private String config = "LIVE_HXSW";//密码

    @RequestMapping(value = "/queryTerminal", produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public String queryTerminal(@RequestBody String param) {
        String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);

        String where = "";
        //JSONArray
        JSONArray array = JSONArray.parseArray(param);
        for (int i = 0; i < array.size(); i++) {
            JSONObject jsonObject1 = array.getJSONObject(i);
            String name = jsonObject1.getString("name");//item
            String content = jsonObject1.getString("content");//玻尿酸
            String condition = jsonObject1.getString("condition");//1

            if (condition != null && condition.equals("1")) {//等于
                if (where.equals("")) {
                    where = name + "= '" + content + "' ";//where
                } else {
                    where += " and " + name + "= '" + content + "' ";//where
                }
            } else {
                if (where.equals("")) {
                    where = name + " like '%" + content + "%' ";
                } else {
                    where += " and " + name + " like '%" + content + "%' ";//where
                }
            }
        }


        String result = idoWebServiceSoap.loadJson(ERPtoken, "HXTerminalInvens", "siteref,item,description,um ,lot ,qtyonhand,LOC,type,locname,note,Uf_PorductDate,exp_date,Uf_PlanNum,qty_reorder,create_date,day,OwnerBu,OwnerBus", where, "", "", 300000);
        System.out.println(result);
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
//            String lotFeatureSp = getLotFeatureSp(Item, Lot);//待处理
        return propertyArray.toString();


    }



}
