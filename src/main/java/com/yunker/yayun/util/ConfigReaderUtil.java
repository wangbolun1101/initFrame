package com.yunker.yayun.util;

import com.yunker.yayun.log.ModuleOutputLogger;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by Daniel(wangd@yunker.com.cn) on 2017/11/28.
 * 读取配置文件类
 */

@Service
public class ConfigReaderUtil {

    private static File configurationFilePath = new File("../conf");
    private static String absoluteFilePath = configurationFilePath.getAbsolutePath() + "\\";

    static {
        if (!configurationFilePath.exists()) {
            //创建配置文件 conf 文件目录
            configurationFilePath.mkdirs();
        }
    }

    public Map<String, String> loadConfigsByFile(String configFileName) {
        Map<String, String> configMap = new HashMap<String, String>();
        try {
            Configuration configuration = new PropertiesConfiguration(configFileName);
            // TODO 遍历方式优化
            Iterator<String> keys = configuration.getKeys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = configuration.getString(key);
                configMap.put(key, value);
            }
        } catch (ConfigurationException e) {
            ModuleOutputLogger.otherProcessError.error("Exception occur when loadConfigs, configFileName is:" + configFileName, e);
        }
        return configMap;
    }

    public Map<String, String> loadCNConfigsByFile(String configFileName) {

        Map<String, String> configMap = new HashMap<>();
        Properties prop = new Properties();
        try {
            prop.load(new InputStreamReader(new FileInputStream(absoluteFilePath + configFileName), "UTF-8"));
            Enumeration enum1 = prop.propertyNames();//得到配置文件的名字
            while (enum1.hasMoreElements()) {
                String strKey = (String) enum1.nextElement();
                String strValue = prop.getProperty(strKey);
                configMap.put(strKey, strValue);
            }
        } catch (IOException e) {
            ModuleOutputLogger.otherProcessError.error(e.getMessage(), e);
        }
        return configMap;

    }

}
