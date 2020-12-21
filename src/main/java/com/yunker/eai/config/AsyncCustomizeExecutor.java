package com.yunker.eai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Created by Daniel(wangd@yunker.com.cn) on 2017/11/28.
 * 多线程配置类
 * 公有云线程数不能随意修改，最高为5
 */

@Configuration
@EnableAsync
public class AsyncCustomizeExecutor extends AsyncConfigurerSupport {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200000);
        executor.setThreadNamePrefix("ThreadPool-->");
        executor.initialize();
        return executor;
    }

}
