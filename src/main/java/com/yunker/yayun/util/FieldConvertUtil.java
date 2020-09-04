package com.yunker.yayun.util;

import com.alibaba.fastjson.JSONObject;
import com.yunker.yayun.log.ModuleOutputLogger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Daniel(wangd@yunker.com.cn) on 2017/11/28.
 * 字段转换类
 */
@Service
public class FieldConvertUtil {

    @Resource
    ConfigReaderUtil configReaderUtil;

    public JSONObject convert(JSONObject inputJson, String taskName) {
        JSONObject outputJson = new JSONObject();
        try {
            Map<String, String> siebleConfigMap = configReaderUtil.loadConfigsByFile("sourceField.properties");
            Map<String, String> xiaoshouyiConfigMap = configReaderUtil.loadConfigsByFile("targetField.properties");
            Iterator<String> keys = siebleConfigMap.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next(); // KEY5_1
                if (key.split("_")[0].equals(taskName)) {//截取前缀
                    String inputField = siebleConfigMap.get(key);//获取所有value的值(sieble字段名)
                    String outputField = xiaoshouyiConfigMap.get(key);//获取所有value的值(crm字段名)
                    try {
                        String value = inputJson.getString(inputField);//从sieble端获取字段值
                        outputJson.put(outputField, value);//储存crm字段名和字段值
                    } catch (Exception ex) {
                        ModuleOutputLogger.otherProcessError.error(ex.getMessage(), ex);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return outputJson;
    }

}
