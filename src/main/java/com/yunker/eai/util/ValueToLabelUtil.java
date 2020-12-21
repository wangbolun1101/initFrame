package com.yunker.eai.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yunker.eai.log.ModuleOutputLogger;
import org.springframework.stereotype.Component;

/**
 * Created by Daniel(wangd@yunker.com.cn) on 2018/1/12.
 * CRM键值转换类
 */
@Component
public class ValueToLabelUtil {

    public String replace(String result, String fieldName, String value, String choice) {
        try {
            if ("".equals(value)) {
                if ("selectitem".equals(choice)) {
                    return null;
                }
            }
            JSONObject jsonObject = JSONObject.parseObject(result);
            JSONArray jsonArray = jsonObject.getJSONArray("fields");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                if (jsonObject1.getString("propertyname").equals(fieldName)) {
                    JSONArray jsonArray1 = jsonObject1.getJSONArray(choice);
                    if ("selectitem".equals(choice)) {
                        for (int j = 0; j < jsonArray1.size(); j++) {
                            JSONObject jsonObject2 = jsonArray1.getJSONObject(j);
                            if (jsonObject2.getString("value").equals(value)) {
                                return jsonObject2.getString("label");
                            }
                        }
                    }
                }
            }
        } catch (NullPointerException e) {
            ModuleOutputLogger.otherProcess.info(fieldName + "---此字段无参数");
        }
        return null;
    }

}
