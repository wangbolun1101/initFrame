package com.yunker.yayun.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class BulkAPI {

    @Autowired
    HttpClientUtil httpClientUtil;
    private DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
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

    public void createDataTaskJob(JSONArray paramArray, String project, String type) throws Exception {
        String results = httpClientUtil.get(getToken(), "https://api.xiaoshouyi.com/rest/bulk/v2/job", null);
        JSONArray jobs = JSONObject.parseObject(results).getJSONObject("result").getJSONArray("jobs");
        String jobId = "";
        for (int i = 0; i < jobs.size(); i++) {
            JSONObject jsonObject1 = jobs.getJSONObject(i);
            String object = jsonObject1.getString("object");
            String operation = jsonObject1.getString("operation");
            String state = jsonObject1.getString("state");
            String createdAt = jsonObject1.getString("createdAt");
            String createDate = createdAt.split(" ")[0];
            String newDate = df1.format(new Date());

            if (project.equals(object) && type.equals(operation) && "open".equals(state) && createDate.equals(newDate)) {
                jobId = jsonObject1.getString("jobId");
                break;
            }
        }
        if (StringUtils.isBlank(jobId)) {

            JSONObject jobObject = new JSONObject();
            JSONObject data = new JSONObject();
            jobObject.put("operation", type);
            jobObject.put("object", project);
            data.put("data", jobObject);
            //创建异步作业
            String result = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/bulk/v2/job", data.toJSONString());
            JSONObject object = JSONObject.parseObject(result);
            if (object.getInteger("code") == 200) {
                jobId = object.getJSONObject("result").getString("id");
            } else {
                return;
            }
        }
        int j = 0;
        JSONArray jsonArray = new JSONArray();
        Boolean flag = false;
        for (int i = 0; i < paramArray.size(); i++) {
            if (j == 0) {
                jsonArray = new JSONArray();
            }
            j++;
            JSONObject jsonObject1 = paramArray.getJSONObject(i);
            jsonArray.add(jsonObject1);
            if (j == 5000) {
                JSONObject dateBulk = new JSONObject();
                JSONObject data = new JSONObject();
                data.put("jobId", jobId);
                data.put("datas", jsonArray);
                dateBulk.put("data", data);
                String task = createTask(dateBulk);
                if (StringUtils.isBlank(task)) {
                    flag = true;
                    break;
                } else {
                    JSONObject object = JSONObject.parseObject(task);
                    String code = object.getString("code");
                    if (!"200".equals(code)) {
                        flag = true;
                        break;
                    }
                }
                j = 0;
            }
        }
        if (j < 5000) {
            JSONObject dateBulk = new JSONObject();
            JSONObject data = new JSONObject();
            data.put("jobId", jobId);
            data.put("datas", jsonArray);
            dateBulk.put("data", data);
            String task = createTask(dateBulk);
            if (StringUtils.isBlank(task)) {
                flag = true;
            } else {
                JSONObject object = JSONObject.parseObject(task);
                String code = object.getString("code");
                if (!"200".equals(code)) {
                    flag = true;
                }
            }
        }
        if (flag) {
            //异步任务创建失败，删除原有异步作业，重新创建
            JSONObject status = new JSONObject();
            JSONObject data = new JSONObject();
            status.put("status", 3);
            data.put("data", status);
            String patch = httpClientUtil.patch(getToken(), "https://api.xiaoshouyi.com/rest/bulk/v2/job/" + jobId, JSONObject.toJSONString(data,SerializerFeature.WriteMapNullValue));
            JSONObject object = JSONObject.parseObject(patch);
            if (!"200".equals(object.getString("code"))) {
                return;
            }
            createDataTaskJob(paramArray, project, type);
        }
    }
    public void queryDataTaskJob(JSONArray paramArray, String project, String type) throws Exception {
        String results = httpClientUtil.get(getToken(), "https://api.xiaoshouyi.com/rest/bulk/v2/job", null);
        JSONArray jobs = JSONObject.parseObject(results).getJSONObject("result").getJSONArray("jobs");
        String jobId = "";
        for (int i = 0; i < jobs.size(); i++) {
            JSONObject jsonObject1 = jobs.getJSONObject(i);
            String object = jsonObject1.getString("object");
            String operation = jsonObject1.getString("operation");
            String state = jsonObject1.getString("state");
            String createdAt = jsonObject1.getString("createdAt");
            String createDate = createdAt.split(" ")[0];
            String newDate = df1.format(new Date());

            if (project.equals(object) && type.equals(operation) && "open".equals(state) && createDate.equals(newDate)) {
                jobId = jsonObject1.getString("jobId");
                break;
            }
        }
        if (StringUtils.isBlank(jobId)) {

            JSONObject jobObject = new JSONObject();
            JSONObject data = new JSONObject();
            jobObject.put("operation", type);
            jobObject.put("object", project);
            data.put("data", jobObject);
            //创建异步作业
            String result = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/bulk/v2/job", data.toJSONString());
            JSONObject object = JSONObject.parseObject(result);
            if (object.getInteger("code") == 200) {
                jobId = object.getJSONObject("result").getString("id");
            } else {
                return;
            }
        }
        Boolean flag = false;
        JSONObject dateBulk = new JSONObject();
        JSONObject data = new JSONObject();
        data.put("jobId", jobId);
        data.put("datas", paramArray);
        dateBulk.put("data", data);
        String task = createTask(dateBulk);
        if (StringUtils.isBlank(task)) {
            flag = true;
        } else {
            JSONObject object = JSONObject.parseObject(task);
            String code = object.getString("code");
            if (!"200".equals(code)) {
                flag = true;
            }
        }
        if (flag) {
            //异步任务创建失败，删除原有异步作业，重新创建
            JSONObject status = new JSONObject();
            JSONObject data1 = new JSONObject();
            status.put("status", 3);
            data1.put("data", status);
            String patch = httpClientUtil.patch(getToken(), "https://api.xiaoshouyi.com/rest/bulk/v2/job/" + jobId, data1.toJSONString());
            JSONObject object = JSONObject.parseObject(patch);
            if (!"200".equals(object.getString("code"))) {
                return;
            }
            queryDataTaskJob(paramArray, project, type);
        }
    }

    /**
     * 创建异步作业
     */
    public String createTask(JSONObject dateBulk) throws Exception {
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/bulk/v2/batch", JSONObject.toJSONString(dateBulk,SerializerFeature.WriteMapNullValue));
        System.out.println("创建异步任务：" + post);
        return post;
    }

}
