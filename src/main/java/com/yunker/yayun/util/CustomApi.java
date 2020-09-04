package com.yunker.yayun.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomApi {
    @Autowired
    private HttpClientUtil httpClientUtil;
    /**
     * 获取token
     *
     * @return
     * @throws Exception
     */
    public String getToken() throws Exception {
        if (!httpClientUtil.instanceTargetToken()) {
            throw new Exception("验证Token出错！");
        }
        String token = httpClientUtil.getAccessToken().toString();
        if (token.contains("error_code")) {
            throw new Exception("验证Token出错！");
        }
        return token;
    }
    public void executeEntity(JSONObject sendJSON,String url) throws Exception {
        String s = JSONObject.toJSONString(sendJSON, SerializerFeature.WriteMapNullValue);
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/script-api/customopenapi/"+url, s);
        System.out.println("调用Xobject接口："+post);
    }
}
