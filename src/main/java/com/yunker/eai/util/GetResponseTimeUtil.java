package com.yunker.eai.util;

import org.springframework.stereotype.Component;

/**
 * Created by Daniel(wangd@yunker.com.cn) on 2018/1/26.
 * 获取接口响应时间类
 */
@Component
public class GetResponseTimeUtil {

    public static long getResponseTime(long startTime) {
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        return responseTime;
    }

}
