package com.yunker.yayun.util;

import java.util.HashMap;
import java.util.Map;

public class ReturnPublic {

    public Map<String, Object> sendJson(Object object, boolean success, String message) {
        Map<String, Object> map = new HashMap();
        map.put("data", object);
        map.put("success", success);
        map.put("message", message);
//        String json = JSON.toJSONString(map);
        return map;
    }
}
