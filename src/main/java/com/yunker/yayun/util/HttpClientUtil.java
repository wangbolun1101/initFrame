package com.yunker.yayun.util;


import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.yunker.yayun.log.ModuleOutputLogger;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by Daniel(wangd@yunker.com.cn) on 2017/11/28.
 */
@Service
public class HttpClientUtil extends HttpPut {

    @Resource
    ConfigReaderUtil configReaderUtil;

    private static final String CHARSET = "utf-8";
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE_TEXT_JSON = "text/json";
    private static final String Authorization = "Authorization";

    //外部文件路径
    private static final String fileName = "D:\\yasen\\task.properties";

    private static SSLConnectionSocketFactory sslConnectionSocketFactory;
    private static RequestConfig config;
    private String accessToken = null;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public HttpClientUtil() {
        initTrustHosts();
        initConfig();
    }

    private void initTrustHosts() {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (NoSuchAlgorithmException e) {
            ModuleOutputLogger.otherProcessError.error(e.getMessage(), e);
        } catch (KeyManagementException e) {
            ModuleOutputLogger.otherProcessError.error(e.getMessage(), e);
        } catch (KeyStoreException e) {
            ModuleOutputLogger.otherProcessError.error(e.getMessage(), e);
        } catch (Exception e) {
            ModuleOutputLogger.otherProcessError.error(e.getMessage(), e);
        }
    }

    private void initConfig() {
        config = RequestConfig.custom().setConnectTimeout(10000000).setSocketTimeout(10000000).build();
    }

    public boolean instanceTargetToken(String username,String password) {
        if (accessToken == null) {
            try {
                //获取外部文件task.properties
                Map<String, String> taskConfigMap = configReaderUtil.loadConfigsByFile("task.properties");

                Map<String, String> targetParamsMap = getTargetParams(taskConfigMap);
                targetParamsMap.replace("username",username);
                targetParamsMap.replace("password",password);
                String writeToken = clientPost(taskConfigMap.get("target.token_url"), targetParamsMap);
                JSONObject json = JSONObject.parseObject(writeToken);

                if (!json.containsKey("access_token")) {
                    ModuleOutputLogger.otherProcessError.error("writeToken from xiaoshouyi is:" + writeToken);
                    return false;
                }
                setAccessToken(json.getString("access_token"));
                ModuleOutputLogger.otherProcess.info("instanceTargetToken successflu, access_token is:" + getAccessToken());
            } catch (Exception e) {
                ModuleOutputLogger.otherProcessError.error("instanceTargetToken failed." + e.getMessage(), e);
                return false;
            }
            return true;
        }
        return true;
    }
    public boolean instanceTargetToken() {
        if (accessToken == null) {
            try {
                //获取外部文件task.properties
                Map<String, String> taskConfigMap = configReaderUtil.loadConfigsByFile("task.properties");

                Map<String, String> targetParamsMap = getTargetParams(taskConfigMap);

                String writeToken = clientPost(taskConfigMap.get("target.token_url"), targetParamsMap);
                JSONObject json = JSONObject.parseObject(writeToken);

                if (!json.containsKey("access_token")) {
                    ModuleOutputLogger.otherProcessError.error("writeToken from xiaoshouyi is:" + writeToken);
                    return false;
                }
                setAccessToken(json.getString("access_token"));
                ModuleOutputLogger.otherProcess.info("instanceTargetToken successflu, access_token is:" + getAccessToken());
            } catch (Exception e) {
                ModuleOutputLogger.otherProcessError.error("instanceTargetToken failed." + e.getMessage(), e);
                return false;
            }
            return true;
        }
        return true;
    }

    private Map<String, String> getTargetParams(Map<String, String> taskConfigMap) {
        Map<String, String> targetParamsMap = new HashMap<String, String>();
        targetParamsMap.put("grant_type", taskConfigMap.get("target.grant_type"));
        targetParamsMap.put("client_id", taskConfigMap.get("target.client_id"));
        targetParamsMap.put("client_secret", taskConfigMap.get("target.client_secret"));
        targetParamsMap.put("redirect_uri", taskConfigMap.get("target.redirect_uri"));
        targetParamsMap.put("username", taskConfigMap.get("target.username"));
        targetParamsMap.put("password", taskConfigMap.get("target.password"));
        return targetParamsMap;
    }

    public String clientPost(String url, Map<String, String> params) {
        String result = null;
        try {
            Thread.sleep(300);
            HttpPost httppost = new HttpPost(url);
            //建立HttpPost对象
            List<NameValuePair> param = new ArrayList<NameValuePair>();
            //建立一个NameValuePair数组，用于存储欲传送的参数
            Iterator<String> keys = params.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = params.get(key);
                param.add(new BasicNameValuePair(key, value));
            }
            StringEntity stringEntity = new StringEntity("", CHARSET);
            stringEntity.setContentType(CONTENT_TYPE_TEXT_JSON);
            //添加参数
            stringEntity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded"));
            httppost.setEntity(new UrlEncodedFormEntity(param, HTTP.UTF_8));
            //设置编码
            CloseableHttpResponse response = createClient().execute(httppost);
            //发送Post,并返回一个HttpResponse对象
            if (response.getStatusLine().getStatusCode() == 200) {//如果状态码为200,就是正常返回
                ModuleOutputLogger.otherProcess.info("返回状态码为---> [ " + response.getStatusLine().getStatusCode() + " ]");
                result = EntityUtils.toString(response.getEntity());
            }
            response.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    private CloseableHttpClient createClient() {
        return HttpClients.custom().setDefaultRequestConfig(config)
                .setSSLSocketFactory(sslConnectionSocketFactory)
                .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                .build();
    }

    public String post(final String token, final String url, final Map<String, String> params) throws IOException {
        UrlEncodedFormEntity urlEncodedFormEntity = null;
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> pairs = getNameValuePairs(params);
            urlEncodedFormEntity = new UrlEncodedFormEntity(pairs, CHARSET);
        }
        HttpPost httpPost = new HttpPost(url);
        setHeaderAndEntity(token, urlEncodedFormEntity, httpPost);
        if (urlEncodedFormEntity != null) {
            httpPost.setEntity(urlEncodedFormEntity);
        }
        CloseableHttpResponse response = createClient().execute(httpPost);
        return handleResponse(httpPost, response, token, url, params);
    }
    public String postNosys(final String token, final String url, final Map<String, String> params) throws IOException {
        UrlEncodedFormEntity urlEncodedFormEntity = null;
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> pairs = getNameValuePairs(params);
            urlEncodedFormEntity = new UrlEncodedFormEntity(pairs, CHARSET);
        }
        HttpPost httpPost = new HttpPost(url);
        setHeaderAndEntity(token, urlEncodedFormEntity, httpPost);
        if (urlEncodedFormEntity != null) {
            httpPost.setEntity(urlEncodedFormEntity);
        }
        CloseableHttpResponse response = createClient().execute(httpPost);

        return handleResponseNosys(httpPost, response, token, url, params);
    }

    public String post(final String token, final String url, final String json) throws IOException {
        StringEntity stringEntity = getStringEntityByJson(json);
        HttpPost httpPost = new HttpPost(url);
        setHeaderAndEntity(token, stringEntity, httpPost);
        CloseableHttpResponse response = createClient().execute(httpPost);
        return handleResponse(httpPost, response, token, url, json);
    }
    public String postNosys(final String token, final String url, final String json) throws IOException {
        StringEntity stringEntity = getStringEntityByJson(json);
        HttpPost httpPost = new HttpPost(url);
        setHeaderAndEntity(token, stringEntity, httpPost);
        CloseableHttpResponse response = createClient().execute(httpPost);
        return handleResponseNosys(httpPost, response, token, url, json);
    }
    public String postExistNull(final String token, final String url, final String json) throws IOException {
        StringEntity stringEntity = getStringEntityByJsonExistNull(json);
        HttpPost httpPost = new HttpPost(url);
        setHeaderAndEntity(token, stringEntity, httpPost);
        CloseableHttpResponse response = createClient().execute(httpPost);
        return handleResponse(httpPost, response, token, url, json);
    }

    private StringEntity getStringEntityByJson(String json) {
        StringEntity stringEntity = null;
        if (StringUtils.isNotBlank(json)) {//非空白
            stringEntity = new StringEntity(json, CHARSET);
            stringEntity.setContentType(APPLICATION_JSON);
            stringEntity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, APPLICATION_JSON));
        }
        return stringEntity;
    }
    private StringEntity getStringEntityByJsonExistNull(String json) {
        StringEntity stringEntity = null;
        stringEntity = new StringEntity(json, CHARSET);
        stringEntity.setContentType(APPLICATION_JSON);
        stringEntity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, APPLICATION_JSON));
        return stringEntity;
    }

    private <T extends Object> String handleResponse(T httpMethod, CloseableHttpResponse response, final String token, final String url, final String json) throws IOException {
        String result = null;
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity, CHARSET);
                JSONObject resultJson = JSONObject.parseObject(result);
                Long errorCode = resultJson.getLong("error_code");
                if (errorCode != null) {
                    if (errorCode == 20000002L) {
                        ModuleOutputLogger.otherProcess.info("token失效,重新获取token.......  " + result);
                        accessToken = null;
                        instanceTargetToken();
                        result = post(getAccessToken(), url, json);
                    }
                    //调用接口过于频繁,重新调用。
                    if (errorCode == 110006L) {
                        result = post(getAccessToken(), url, json);
                    }
                } else if (result.contains("无效的access token") || result.contains("访问频率超出限制")) {
                    if (result.contains("无效的access token")) {
                        ModuleOutputLogger.otherProcess.info("token失效,重新获取token.......  " + result);
                        accessToken = null;
                        instanceTargetToken();
                        result = post(getAccessToken(), url, json);
                    }
                    //调用接口过于频繁,重新调用。
                    if (result.contains("访问频率超出限制")) {
                        result = post(getAccessToken(), url, json);
                    }
                }
                EntityUtils.consume(entity);
            } else {
                ModuleOutputLogger.otherProcess.info("[info text] empty entity!");
            }
        } else {
            if (httpMethod instanceof HttpGet) {
                ((HttpGet) httpMethod).abort();
            }
            if (httpMethod instanceof HttpPost) {
                ((HttpPost) httpMethod).abort();
            }
            ModuleOutputLogger.otherProcess.info("[info text] wrong response , status code:" + statusCode + " param is：" + json);
        }
        response.close();
        return result;
    }
    private <T extends Object> String handleResponseNosys(T httpMethod, CloseableHttpResponse response, final String token, final String url, final String json) throws IOException {
        String result = null;
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity, CHARSET);
                /*JSONObject resultJson = JSONObject.parseObject(result);
                Long errorCode = resultJson.getLong("error_code");
                if (errorCode != null) {
                    if (errorCode == 20000002L) {
                        ModuleOutputLogger.otherProcess.info("token失效,重新获取token.......  " + result);
                        accessToken = null;
                        instanceTargetToken();
                        result = post(getAccessToken(), url, json);
                    }
                    //调用接口过于频繁,重新调用。
                    if (errorCode == 110006L) {
                        result = post(getAccessToken(), url, json);
                    }
                } else if (result.contains("无效的access token") || result.contains("访问频率超出限制")) {
                    if (result.contains("无效的access token")) {
                        ModuleOutputLogger.otherProcess.info("token失效,重新获取token.......  " + result);
                        accessToken = null;
                        instanceTargetToken();
                        result = post(getAccessToken(), url, json);
                    }
                    //调用接口过于频繁,重新调用。
                    if (result.contains("访问频率超出限制")) {
                        result = post(getAccessToken(), url, json);
                    }
                }*/
                EntityUtils.consume(entity);
            } else {
                ModuleOutputLogger.otherProcess.info("[info text] empty entity!");
            }
        } else {
            if (httpMethod instanceof HttpGet) {
                ((HttpGet) httpMethod).abort();
            }
            if (httpMethod instanceof HttpPost) {
                ((HttpPost) httpMethod).abort();
            }
            ModuleOutputLogger.otherProcess.info("[info text] wrong response , status code:" + statusCode + " param is：" + json);
        }
        response.close();
        return result;
    }

    private <T extends Object> String handleResponse(T httpMethod, CloseableHttpResponse response, final String token, final String url, final Map<String, String> params) throws IOException {
        String result = null;
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity, CHARSET);
                JSONObject resultJson = JSONObject.parseObject(result);
                Long errorCode = resultJson.getLong("error_code");
                if (errorCode != null) {
                    if (errorCode == 20000002L) {
                        ModuleOutputLogger.otherProcess.info("token失效,重新获取token......." + result + "  param is：" + params + "  url：" + url);
                        accessToken = null;
                        instanceTargetToken();
                        result = post(getAccessToken(), url, params);
                    }
                    //调用接口过于频繁,重新调用。
                    if (errorCode == 110006L) {
                        result = post(getAccessToken(), url, params);
                    }
                } else if (result.contains("无效的access token") || result.contains("访问频率超出限制")) {
                    if (result.contains("无效的access token")) {
                        ModuleOutputLogger.otherProcess.info("token失效,重新获取token......." + result + "  param is：" + params + "  url：" + url);
                        accessToken = null;
                        instanceTargetToken();
                        result = post(getAccessToken(), url, params);
                    }
                    //调用接口过于频繁,重新调用。
                    if (result.contains("访问频率超出限制")) {
                        result = post(getAccessToken(), url, params);
                    }
                }
                EntityUtils.consume(entity);
            } else {
                ModuleOutputLogger.otherProcess.info("[info text] empty entity!");
            }
        } else {
            if (httpMethod instanceof HttpGet) {
                ((HttpGet) httpMethod).abort();
            }
            if (httpMethod instanceof HttpPost) {
                ((HttpPost) httpMethod).abort();
            }
            ModuleOutputLogger.otherProcess.info("[info text] wrong response , status code:" + statusCode);
        }
        response.close();
        return result;
    }
    private <T extends Object> String handleResponseNosys(T httpMethod, CloseableHttpResponse response, final String token, final String url, final Map<String, String> params) throws IOException {
        String result = null;
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity, CHARSET);
                /*JSONObject resultJson = JSONObject.parseObject(result);
                Long errorCode = resultJson.getLong("error_code");
                if (errorCode != null) {
                    if (errorCode == 20000002L) {
                        ModuleOutputLogger.otherProcess.info("token失效,重新获取token......." + result + "  param is：" + params + "  url：" + url);
                        accessToken = null;
                        instanceTargetToken();
                        result = post(getAccessToken(), url, params);
                    }
                    //调用接口过于频繁,重新调用。
                    if (errorCode == 110006L) {
                        result = post(getAccessToken(), url, params);
                    }
                } else if (result.contains("无效的access token") || result.contains("访问频率超出限制")) {
                    if (result.contains("无效的access token")) {
                        ModuleOutputLogger.otherProcess.info("token失效,重新获取token......." + result + "  param is：" + params + "  url：" + url);
                        accessToken = null;
                        instanceTargetToken();
                        result = post(getAccessToken(), url, params);
                    }
                    //调用接口过于频繁,重新调用。
                    if (result.contains("访问频率超出限制")) {
                        result = post(getAccessToken(), url, params);
                    }
                }*/
                EntityUtils.consume(entity);
            } else {
                ModuleOutputLogger.otherProcess.info("[info text] empty entity!");
            }
        } else {
            if (httpMethod instanceof HttpGet) {
                ((HttpGet) httpMethod).abort();
            }
            if (httpMethod instanceof HttpPost) {
                ((HttpPost) httpMethod).abort();
            }
            ModuleOutputLogger.otherProcess.info("[info text] wrong response , status code:" + statusCode);
        }
        response.close();
        return result;
    }

    private <T extends HttpEntity> void setHeaderAndEntity(String token, T entity, Object httpMethod) {
        if (StringUtils.isNotBlank(token)) {
            if (httpMethod instanceof HttpGet) {
                ((HttpGet) httpMethod).addHeader(Authorization, token);
            }
            if (httpMethod instanceof HttpPost) {
                ((HttpPost) httpMethod).addHeader(Authorization, "Bearer " + token);
            }
        }
        if (entity != null) {
            if (httpMethod instanceof HttpPost) {
                ((HttpPost) httpMethod).setEntity(entity);
            }
        }
    }

    private List<NameValuePair> getNameValuePairs(Map<String, String> params) {
        List<NameValuePair> pairs = new ArrayList<NameValuePair>(params.size());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                pairs.add(new BasicNameValuePair(entry.getKey(), value));
            }
        }
        return pairs;
    }

    public String get(final String token, final String url, final Map<String, String> params) throws IOException {
        CloseableHttpResponse response = getHttpResponse(token, url, params);
        return handleResponse(null, response, token, url, params);
    }
    public InputStream getInputStream(final String token, final String url, final Map<String, String> params) throws IOException {
        CloseableHttpResponse response = getHttpResponse(token, url, params);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            return response.getEntity().getContent();
        }else {
            return null;
        }
    }
    public String getNosys(final String token, final String url, final Map<String, String> params) throws IOException {
        CloseableHttpResponse response = getHttpResponse(token, url, params);
        return handleResponseNosys(null, response, token, url, params);
    }

    public CloseableHttpResponse getHttpResponse(final String token, final String url, final Map<String, String> params) throws IOException {
        StringBuilder sb = new StringBuilder(url);
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> pairs = getNameValuePairs(params);
            sb.append("?").append(EntityUtils.toString(new UrlEncodedFormEntity(pairs, CHARSET)));
        }
        HttpGet httpGet = new HttpGet(sb.toString());
        setHeaderAndEntity(token, null, httpGet);
        return createClient().execute(httpGet);
    }

    @Override
    public String getMethod() {
        return "PATCH";
    }

    public String patch(String token, String url, String jsonParam) {
        String result = null;
        HttpClient httpClient = new DefaultHttpClient();
        HttpPatch httpPatch = new HttpPatch(url);
        httpPatch.setHeader("Content-type", "application/json");
        httpPatch.setHeader("Charset", HTTP.UTF_8);
        httpPatch.setHeader("Accept", "application/json");
        httpPatch.setHeader("Accept-Charset", HTTP.UTF_8);
        httpPatch.setHeader("Authorization", token);
        try {
            if (jsonParam != null) {
                StringEntity entity = new StringEntity(jsonParam, HTTP.UTF_8);
                httpPatch.setEntity(entity);
            }
            HttpResponse response = httpClient.execute(httpPatch);
            result = EntityUtils.toString(response.getEntity());
            JSONObject resultJson = JSONObject.parseObject(result);
            Long errorCode = resultJson.getLong("error_code");
            if (errorCode != null) {
                if (errorCode == 20000002L) {
                    ModuleOutputLogger.otherProcess.info("token失效,重新获取token.......  " + result);
                    accessToken = null;
                    instanceTargetToken();
                    result = patch(getAccessToken(), url, jsonParam);
                }
                //调用接口过于频繁,重新调用。
                if (errorCode == 110006L) {
                    result = patch(getAccessToken(), url, jsonParam);
                }
            } else if (result.contains("无效的access token") || result.contains("访问频率超出限制")) {
                if (result.contains("无效的access token")) {
                    ModuleOutputLogger.otherProcess.info("token失效,重新获取token.......  " + result);
                    accessToken = null;
                    instanceTargetToken();
                    result = patch(getAccessToken(), url, jsonParam);
                }
                //调用接口过于频繁,重新调用。
                if (result.contains("访问频率超出限制")) {
                    result = patch(getAccessToken(), url, jsonParam);
                }
            }
        } catch (ParseException | JSONException | IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public String Sqlv2(final String token, final String params) throws IOException {
        String sql = params.replaceAll(" ", "%20");
        String url = "https://api.xiaoshouyi.com/rest/data/v2/query?q=" + sql;
        CloseableHttpResponse response = getHttpResponse(token, url, null);
        JSONObject result = JSONObject.parseObject(handleResponse(null, response, token, url, ""));
        //System.out.println("Sqlv2" + result);
        if (result == null || !result.getString("code").equals("200") || !result.getString("msg").equals("OK")) {
            return "查询出错" + result;
        } else {
            return result.get("result").toString();
        }
    }
    public String xoql(final String token, final String sql) throws IOException {
        Map map=new HashMap();
        map.put("xoql",sql);
        String url = "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql";
        String post = post(token, url, map);
        return post;
    }
}