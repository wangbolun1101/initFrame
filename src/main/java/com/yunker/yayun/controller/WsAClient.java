package com.yunker.yayun.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import mypackage.IDOWebService;
import mypackage.IDOWebServiceSoap;

import javax.xml.ws.Holder;
import java.util.HashMap;
import java.util.Map;

//调用接口需要链接vpn
public class WsAClient {

    public static void main(String[] args) {
        String userId="crm";//用户名
        String pswd="Crm123456";//密码
        String config="LIVE_HXSW";//密码
        Map<String, String> test = new HashMap<>();
        test.put("0", "SalesTeamID");
        test.put("1", "CustNum");
        test.put("2", "Name");
        JSONObject queryResult = new JSONObject();

        //实例化接口
        IDOWebService ST = new IDOWebService();
        IDOWebServiceSoap idoWebServiceSoap = ST.getIDOWebServiceSoap();
        //调用接口,获取Sessiontoken
        String ERPtoken = idoWebServiceSoap.createSessionToken(userId, pswd, config);
        //输出token
        System.err.println(ERPtoken);

        //查询数据（参数：表名，需要查询的字段名，条件，排序，不填，查询数量-1代表查全部）
//        String result = idoWebServiceSoap.loadJson(ERPtoken, "SLItems", "Item, itmUf_Specification", "", "", "", -1);
        String result = idoWebServiceSoap.loadJson(ERPtoken, "SLLots", "Lot", " Lot = '1307040101-1000'", "", "", -1);

        //输出查询结果
        System.out.println(result);
        //重新格式化JSON
        JSONObject resultJson = JSONObject.parseObject(result);
        JSONArray Items = resultJson.getJSONArray("Items");
        for (int i = 0; i < Items.size(); i++) {

            JSONObject tem = Items.getJSONObject(i);
            JSONArray Properties = tem.getJSONArray("Properties");
            for (int j = 0; j < Properties.size(); j++) {
                JSONObject field = Properties.getJSONObject(j);
                String fieldName = test.get(j + "");
                String fieldValue = field.getString("Property");
                if (j == 1) {
                    field.put("Property", fieldValue + "123");
                }
                field.put("Updated", true);
                queryResult.put(fieldName, fieldValue);
            }
            System.err.println(queryResult);
            System.err.println(resultJson);

        }

        //更新/创建 数据，（更新需要先查询。创建就是不传id）
//        String updateResult = idoWebServiceSoap.saveJson(token, resultJson.toJSONString(), "", "", "");
//        System.err.println(updateResult);

        //自定义方法调用（具体参数要问客户那边技术）（这个自定义接口是获取客户最新编号的接口）
//        Holder<String> pa = new Holder<>("<Parameters><Parameter>SF</Parameter><Parameter ByRef=\"Y\"></Parameter></Parameters>");
        Holder<String> pa = new Holder<>("<Parameters><Parameter>" + "hzj0119" + "</Parameter>" +
                "<Parameter>"+"Z001"+"</Parameter>" +
                "<Parameter>"+"E60"+"</Parameter>" +
                "<Parameter>"+"26000.00"+"</Parameter>" +
                "<Parameter ByRef=\"Y\"></Parameter></Parameters>");
        Holder<Object> pa2 = new Holder<>();
        idoWebServiceSoap.callMethod(ERPtoken, "SP!", "hxsp_calc_ecocredit_crm", pa, pa2);
//        //输出返回结果
        System.err.println(pa2.value);

    }
}