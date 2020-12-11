package com.yunker.yayun.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Service
public class QueryServer extends ReturnPublic {

    @Autowired
    HttpClientUtil httpClientUtil;

    public Map sendMap(Object object, boolean success, String message) {
        Map<String, Object> map = new HashMap();
        map.put("data", object);
        map.put("success", success);
        map.put("message", message);
        return map;
    }

    /**
     * v1查询部分数据
     *
     * @param sql
     */
    public Map queryArray(String token, String sql) {
        JSONArray jsonArray = new JSONArray();
        try {
            Map map = new HashMap<>();
            map.put("q",sql);
            String url = "https://api.xiaoshouyi.com/data/v1/query";
            String result = httpClientUtil.post(token,url,map);
            if (result.contains("error_code")) {
                throw new Exception(JSONObject.parseObject(result).getString("message"));
            }
            jsonArray = JSONObject.parseObject(result).getJSONArray("records");
        } catch (Exception e) {
            jsonArray = null;
            sendMap(jsonArray,false,e.getMessage());
        }
        return sendMap(jsonArray,true,"");
    }
    /**
     * 自定义对象创建公共方法
     * @param paramObject
     * @param belongId
     * @throws Exception
     */
    public Long createCustomize(JSONObject paramObject,Long belongId,String token) throws Exception {
        JSONObject object=new JSONObject();
        object.put("record",paramObject);
        object.put("belongId",belongId);
        String post = httpClientUtil.post(token,"https://api.xiaoshouyi.com/data/v1/objects/customize/create", object.toString());
        JSONObject object1 = JSONObject.parseObject(post);
        if (post.contains("error_code")){
            String message = object1.getString("message");
            String code = object1.getString("error_code");
            throw new Exception("错误码："+code+"  "+message);
        }
        return object1.getLongValue("id");
    }
    /**
     * xoql查询部分数据
     *
     * @param sql
     */
    public Map queryArrayByXoql(String token, String sql) {
        JSONArray jsonArray = new JSONArray();
        try {
            Map map = new HashMap<>();
            map.put("xoql",sql);
            String url = "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql";
            String result = httpClientUtil.post(token,url,map);
            if (JSONObject.parseObject(result).getInteger("code") != 200) {
                throw new Exception(JSONObject.parseObject(result).getString("msg"));
            }
            jsonArray = JSONObject.parseObject(result).getJSONObject("data").getJSONArray("records");
        } catch (Exception e) {
            jsonArray = null;
            sendMap(jsonArray,false,e.getMessage());
        }
        return sendMap(jsonArray,true,"");
    }

    /**
     * v1查询单个数据
     *
     * @param sql
     */
    public Map queryObject(String token, String sql) {
        JSONObject jsonObject = new JSONObject();
        try {
            Map map = new HashMap<>();
            map.put("q",sql);
            String url = "https://api.xiaoshouyi.com/data/v1/query";
            String result = httpClientUtil.post(token,url,map);
            if (result.contains("error_code")) {
                throw new Exception(JSONObject.parseObject(result).getString("message"));
            }
            jsonObject = JSONObject.parseObject(result).getJSONArray("records").getJSONObject(0);
        } catch (Exception e) {
            jsonObject = null;
            sendMap(jsonObject,false,e.getMessage());
        }
        return sendMap(jsonObject,true,"");
    }

    /**
     * 获取符合条件的数组
     * @param token
     * @param getResult
     * @param sql
     * @return
     */
    public JSONArray findAll(String token, String getResult, String sql) throws Exception{
        JSONArray resultArray = new JSONArray();
        //获取符合条件的数组
        resultArray = JSONObject.parseObject(getResult).getJSONArray("records");

        //获取到符合条件的数量
        int totalSize = JSONObject.parseObject(getResult).getInteger("totalSize");

        //获取到符合条件的count数量
        int count = JSONObject.parseObject(getResult).getInteger("count");

        while (true) {
            Thread.sleep(800);
            String addsql = sql + " order by id limit " + count + "," + totalSize;
            Map map = new HashMap();
            map.put("q", addsql);
            String url = "https://api.xiaoshouyi.com/data/v1/query";
            String result = httpClientUtil.post(token, url,map);
            if (result == null || result.contains("error_code")) {
                throw new Exception("没有信息！");
            }
            //获取符合条件的数组
            JSONArray jsonArray = JSONObject.parseObject(result).getJSONArray("records");
            resultArray.addAll(jsonArray);
            //如果已经保存的数量 加上最新要查询的数量 大于总数量的话就不再循环了
            if ((count + jsonArray.size()) < totalSize) {
                count += jsonArray.size();
            } else {
                break;
            }
        }
        return resultArray;
    }
    /**
     * 自定义对象创建公共方法
     * @param paramObject
     * @param belongId
     * @throws Exception
     */
    public Long createCustomize(JSONObject paramObject,Long belongId) throws Exception {
        JSONObject object=new JSONObject();
        object.put("record",paramObject);
        object.put("belongId",belongId);
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/create", object.toString());
        JSONObject object1 = JSONObject.parseObject(post);
        if (post.contains("error_code")){
            String message = object1.getString("message");
            String code = object1.getString("error_code");
            throw new Exception("错误码："+code+"  "+message);
        }
        return object1.getLongValue("id");
    }

    /**
     * 更新自定义业务对象公共方法
     * @param paramobject
     * @throws Exception
     */
    public void updateCustomizeById(JSONObject paramobject) throws Exception {
        String post1 = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", paramobject.toString());
        JSONObject object1 = JSONObject.parseObject(post1);
        if (post1.contains("error_code")){
            String message = object1.getString("message");
            String code = object1.getString("error_code");
            throw new Exception("错误码："+code+"  "+message);
        }
    }
    /**
     * 更新自定义业务对象公共方法
     * @param paramobject
     * @throws Exception
     */
    public String updateCustomizeByIdNoThrowException(JSONObject paramobject) throws Exception {
        String post1 = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", paramobject.toString());
        JSONObject object1 = JSONObject.parseObject(post1);
        return post1;
    }
    /**
     * 更新自定义业务对象公共方法
     * @throws Exception
     */
    public void updateCustomizeByIdExitnull(String json) throws Exception {
        String post1 = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/update", json);
        JSONObject object1 = JSONObject.parseObject(post1);
        if (post1.contains("error_code")){
            String message = object1.getString("message");
            String code = object1.getString("error_code");
            throw new Exception("错误码："+code+"  "+message);
        }
    }
    /**
     * 查询公共方法
     */
    public void deleteCustomizeById(Long dataId) throws Exception {
        Map map=new HashMap();
        map.put("id",dataId+"");
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/delete", map);
        JSONObject object1 = JSONObject.parseObject(post);
        if (post.contains("error_code")){
            String message = object1.getString("message");
            String code = object1.getString("error_code");
            throw new Exception("错误码："+code+"  "+message);
        }
    }
    /**
     * 查询公共方法
     */
    public JSONObject getByXoqlSimple(String sql) throws Exception {
        Map map = new HashMap();
        map.put("xoql", sql+" order by id");
        map.put("useSimpleCode","true");
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
        JSONObject object = JSONObject.parseObject(post);
        if (!"200".equals(object.getString("code"))) {
            String msg = object.getString("msg");
            String code = object.getString("code");
            throw new Exception("错误码：" + code + "  " + msg);
        }
        return object;
    }
    /**
     * xoql查询所有数据
     *
     * @param sql
     */
    public JSONArray getAllByXoqlSample(String token,JSONObject object, String sql) throws Exception {
        JSONObject data = object.getJSONObject("data");
        JSONArray records = data.getJSONArray("records");
        if (data.getInteger("count")<3000){
            return records;
        }
        int count=3000;
        while (true){
            Thread.sleep(200);
            String newSql=sql+" order by id limit 3000 OFFSET "+count;
            Map map = new HashMap<>();
            map.put("xoql",newSql);
            map.put("useSimpleCode","true");
            String url = "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql";
            String result = httpClientUtil.post(token,url,map);
            if (JSONObject.parseObject(result).getInteger("code") != 200) {
                throw new Exception(JSONObject.parseObject(result).getString("msg"));
            }
            JSONObject data1 = JSONObject.parseObject(result).getJSONObject("data");
            JSONArray records1 = data1.getJSONArray("records");
            records.addAll(records1);
            if (data1.getInteger("count")<3000){
                break;
            }else {
                count+=3000;
            }
        }
        return records;
    }
    /**
     * 查询公共方法
     */
    public JSONObject getByXoql(String sql, int dataCheckType) throws Exception {
        Map map=new HashMap();
        map.put("xoql",sql);
        map.put("dataCheckType",dataCheckType+"");
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/query/xoql", map);
        JSONObject object = JSONObject.parseObject(post);
        if (!"200".equals(object.getString("code"))){
            String msg = object.getString("msg");
            String code = object.getString("code");
            throw new Exception("错误码："+code+"  "+msg);
        }
        return object;
    }
    /**
     * 查询公共方法
     */
    public String getBySql(String sql) throws Exception {
        Map map=new HashMap();
        map.put("q",sql+" order by id");
        Thread.sleep(200);
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/query", map);
        JSONObject object1 = JSONObject.parseObject(post);
        if (post.contains("error_code")){
            String message = object1.getString("message");
            String code = object1.getString("error_code");
            throw new Exception("错误码："+code+"  "+message);
        }
        return post;
    }

    /**
     * 查询belongId
     * @return
     */
    public String getBelongIds() throws Exception {
        String s = httpClientUtil.get(getToken(), "https://api.xiaoshouyi.com/data/v1/picks/dimension/belongs", null);
        return s;
    }
    public String getFieldsByBelongId(Long belongId) throws Exception {
        String s = httpClientUtil.get(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/describe?belongId="+belongId, null);
        return s;
    }

    public Map<String,Long> getAllUsers() throws Exception {
        Map<String,Long> map=new HashMap<>();
        String sql="select id,name from user";
        String bySql = getBySql(sql);
        JSONArray all = findAll(getToken(), bySql, sql);
        for (int i = 0; i < all.size(); i++) {
            JSONObject jsonObject = all.getJSONObject(i);
            long id = jsonObject.getLongValue("id");
            String name = jsonObject.getString("name");
            map.put(name,id);
        }
        return map;
    }
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

    /**
     * 更新订单
     * @param param
     * @throws Exception
     */
    public String updateOrder(JSONObject param) throws Exception {
        String url = "https://api.xiaoshouyi.com/data/v1/objects/order/update";
        String result = httpClientUtil.post(getToken(),url,param.toString());
        return result;

    }


    public JSONObject getContractTemplete(String templateName){
        try {
            JSONObject template = getTemplateId(templateName);
//            String fileName = template.getString("fileName");
//            Long templateId = template.getLongValue("id");
//            if (templateId==null||templateId==0){
//                map.put(false,"未查询到对应模板——"+templateName);
//                return map;
//            }
            return template;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public Map<Boolean, String> getContract( Long dataId,Long templateId){
        Map<Boolean,String>map=new HashMap<>();
        try {
            //获取打印合同
            String contactInfo = getContactInfo(dataId, templateId);
            JSONObject object = JSONObject.parseObject(contactInfo);
            String content=object.getString("data");
            map.put(true,content);
        } catch (Exception e) {
            e.printStackTrace();
            map.put(false,e.getMessage());
        }
        return map;
    }


    public static File base64ToFile(String base64,String prefix,String suffix) {
        if(base64==null||"".equals(base64)) {
            return null;
        }
        byte[] buff= org.apache.tomcat.util.codec.binary.Base64.decodeBase64(base64);
        File file=null;
        FileOutputStream fout=null;
        try {
//            file = File.createTempFile(prefix, suffix);
            file = new File(prefix+suffix);
            file.createNewFile();
            System.out.println(file);
            fout=new FileOutputStream(file);
            fout.write(buff);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(fout!=null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }
    public static File base64ToFile(String base64,String fileName) throws IOException {
        if(base64==null||"".equals(base64)) {
            return null;
        }
//        BASE64Decoder decoder = new BASE64Decoder();
//        byte[] buff= decoder.decodeBuffer(base64);
        byte[] buff = Base64.decodeBase64(base64);
        File file=null;
        FileOutputStream fout=null;
        try {
//            file = File.createTempFile(prefix, suffix);
            file = new File(fileName);
            file.createNewFile();
            System.out.println(file);
            fout=new FileOutputStream(file);
            fout.write(buff);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(fout!=null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }
    /**
     *  将PDF转换成base64编码
     *  1.使用BufferedInputStream和FileInputStream从File指定的文件中读取内容；
     *  2.然后建立写入到ByteArrayOutputStream底层输出流对象的缓冲输出流BufferedOutputStream
     *  3.底层输出流转换成字节数组，然后由BASE64Encoder的对象对流进行编码
     * */
    static String getPDFBinary(File file) {
        BASE64Encoder encoder = new BASE64Encoder();
        FileInputStream fin = null;
        BufferedInputStream bin = null;
        ByteArrayOutputStream baos = null;
        BufferedOutputStream bout = null;
        try {
            // 建立读取文件的文件输出流
            fin = new FileInputStream(file);
            // 在文件输出流上安装节点流（更大效率读取）
            bin = new BufferedInputStream(fin);
            // 创建一个新的 byte 数组输出流，它具有指定大小的缓冲区容量
            baos = new ByteArrayOutputStream();
            // 创建一个新的缓冲输出流，以将数据写入指定的底层输出流
            bout = new BufferedOutputStream(baos);
            byte[] buffer = new byte[1024];
            int len = bin.read(buffer);
            while (len != -1) {
                bout.write(buffer, 0, len);
                len = bin.read(buffer);
            }
            // 刷新此输出流并强制写出所有缓冲的输出字节，必须这行代码，否则有可能有问题
            bout.flush();
            byte[] bytes = baos.toByteArray();
            // sun公司的API
            return encoder.encodeBuffer(bytes).trim();
            // apache公司的API
            // return Base64.encodeBase64String(bytes);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fin.close();
                bin.close();
                // 关闭 ByteArrayOutputStream 无效。此类中的方法在关闭此流后仍可被调用，而不会产生任何 IOException
                // IOException
                // baos.close();
                bout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public String uploadFile(File file) throws Exception {

        HttpClient httpclient = new DefaultHttpClient();

        try {  //上传文件路径
            HttpPost httppost = new HttpPost("https://api.xiaoshouyi.com/data/v1/objects/document/file/create");


            FileBody bin = new FileBody(file);

            MultipartEntity reqEntity = new MultipartEntity();  //建立多文件实体

            reqEntity.addPart("file", bin);//upload为请求后台的File upload;属性

            httppost.setEntity(reqEntity);  //设置实体
            httppost.setHeader("Authorization",getToken());
            httppost.setHeader("Charset", HTTP.UTF_8);
            httppost.setHeader("Accept-Charset", HTTP.UTF_8);

            HttpResponse response = httpclient.execute(httppost);

            int statusCode = response.getStatusLine().getStatusCode();

            if(statusCode == HttpStatus.SC_OK){

                HttpEntity resEntity = response.getEntity();
                String s = EntityUtils.toString(resEntity);
                System.out.println(s);//httpclient自带的工具类读取返回数据

                EntityUtils.consume(resEntity);
                return s;
            }else {
                return null;
            }

        } finally {
            try {
                httpclient.getConnectionManager().shutdown();
            } catch (Exception ignore) {

            }
        }
    }
    public String createDoc(long dataId, long docId) throws Exception {
        JSONObject record=new JSONObject();
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("id", docId);
        jsonObject.put("belongId", 1332135646396821L);
        jsonObject.put("dataId", dataId);
        jsonObject.put("directoryId", -1);
        record.put("record", jsonObject);
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/document/create", record.toJSONString());
        return post;
    }

    private JSONObject getTemplateId(String templateName) throws Exception {
        System.out.println("templateName:"+templateName);
        String s = httpClientUtil.get(getToken(), "https://api.xiaoshouyi.com/rest/template/v2.0/" + URLEncoder.encode(templateName, "UTF-8"), null);
        System.out.println("查询合同模板："+s);
        JSONObject object = JSONObject.parseObject(s);
        if (!"200".equals(object.getString("code"))){
            return null;
        }
        JSONArray batchData = object.getJSONArray("batchData");
        if (batchData.size()==0){
            return null;
        }
        JSONObject data = batchData.getJSONObject(0).getJSONObject("data");
        return data;
    }
    private String getContactInfo(long dataId, Long templateId) throws Exception {
        String s = httpClientUtil.get(getToken(), "https://api.xiaoshouyi.com/rest/template/v2.0/customEntity62__c/"+dataId+"/"+templateId, null);
        return s;
    }

    public void updateContractById(JSONObject paramobject) throws Exception {
        String post1 = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/contract/update", paramobject.toString());
        JSONObject object1 = JSONObject.parseObject(post1);
        if (post1.contains("error_code")){
            String message = object1.getString("message");
            String code = object1.getString("error_code");
            throw new Exception("错误码："+code+"  "+message);
        }
    }

    public Long createOrder(JSONObject orderJSON) throws Exception {
        String token = getToken();
        JSONObject object = new JSONObject();
        object.put("record", orderJSON);
        String result = httpClientUtil.post(token, "https://api.xiaoshouyi.com/data/v1/objects/order/create", object.toString());
        Long id = JSONObject.parseObject(result).getLong("id");
        return id;
    }

    public Long createOrderTeatil(JSONObject orderDetailJSON) throws Exception {
//        JSONObject record=new JSONObject();
//        record.put("record", orderDetailJSON);
        JSONObject response = new JSONObject();
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/orderProduct/create", JSONObject.toJSONString(orderDetailJSON,SerializerFeature.WriteMapNullValue));
        response = JSONObject.parseObject(post);
        if (response.containsKey("error_code")) {
            System.err.println(response.getString("message"));
            throw new Exception(response.getString("message"));
//            return null;
        }
        Long id = response.getLong("id");
        return id;
    }

    public String downLoadDoc(Long docId) throws Exception {
        InputStream inputStream = httpClientUtil.getInputStream(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/document/file/info?id=" + docId, null);
        String imageString = getImageString(inputStream);//        String imageString = getImageString(s);
        return imageString;
    }
    public String downLoadAllDoc(Long belongId,String dataId) throws Exception {
        Map map=new HashMap();
        map.put("belongId", belongId+"");
        map.put("objectIds", dataId);
        String s = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/file/v2.0/document/actions/batchDownloadByObjects", map);
//        String imageString = getImageString(s);
        return s;
    }
    public String downDocInputStream(Long docId) throws Exception {
        InputStream inputStream = httpClientUtil.getInputStream(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/document/file/info?id=" + docId, null);
        String imageString = getImageString(inputStream);
        return imageString;
    }
    public static String getImageString(InputStream fileData) throws IOException {
        byte[] data = IOUtils.toByteArray(fileData);
        BASE64Encoder encoder = new BASE64Encoder();
        return fileData != null ? encoder.encodeBuffer(data) : "";
    }
    public String getDocInfo(Long docId) throws Exception {
        String s = httpClientUtil.getNosys(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/document/info?id=" + docId, null);
        JSONObject object = JSONObject.parseObject(s);
        String fileName = object.getString("fileName");
        return fileName;
    }

    public String updateAccount(JSONObject object) throws Exception {
        String post1 = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/account/update", object.toString());
        return post1;
    }

    public String getEnterPriseInfo(String accountName, String token) throws IOException {
        String creditCode=null;
        try {
            Thread.sleep(400);
            String s = httpClientUtil.get(token, "https://api.xiaoshouyi.com/data/v1/objects/enterprise/info?name=" + accountName, null);
            JSONObject object = JSONObject.parseObject(s);
            JSONObject result = object.getJSONObject("result");
            creditCode=result.getString("creditCode");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return creditCode;
    }
    public void sendMessage(JSONObject requestJSON) throws Exception {
        Thread.sleep(400);
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/notice/v2.0/newNotice", requestJSON.toJSONString());
        System.out.println("发送通知消息:"+post);
    }

    public JSONObject getCustomInfo(Long dataId) throws Exception {
        Thread.sleep(400);
        String s = httpClientUtil.get(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/customize/info?id=" + dataId, null);
        JSONObject object = JSONObject.parseObject(s);
        return object;
    }
    public Long getFlowId(Long dataID, Long belongID) throws Exception {
        int pageNo = 1;
        int pageSize = 25;
        String url = "https://api.xiaoshouyi.com/data/v1/objects/approval/approvals?pageNo=" + pageNo + "&pageSize=" + pageSize + "&userId=743527959707865";
        Long id = null;
        String s = httpClientUtil.get(getToken(), url, null);
        JSONObject object = JSONObject.parseObject(s);
        Integer totalSize = object.getInteger("totalSize");
        JSONArray records = object.getJSONArray("records");
        if (totalSize > pageSize) {
            while (pageNo * pageSize < totalSize) {
                pageNo++;
                //获取符合条件的数组
                String result = httpClientUtil.get(getToken(), url, null);
                JSONArray jsonArray = JSONObject.parseObject(result).getJSONArray("records");
                records.addAll(jsonArray);
            }
        }
        for (int i = 0; i < records.size(); i++) {
            JSONObject jsonObject = records.getJSONObject(i);
            Long belongId = jsonObject.getLong("belongId");
            Long dataId = jsonObject.getLong("dataId");
            Long folwId = jsonObject.getLong("id");
            if (belongId.longValue() != belongID.longValue()) {
                continue;
            }
            if (dataId.longValue() != dataID.longValue()) {
                continue;
            } else {
                id = folwId;
            }

        }
        return id;
    }
    public void notAgreeApprove(Long approvalId) throws Exception {
        String url = "https://api.xiaoshouyi.com/data/v1/objects/approval/refuse";
        JSONObject object = new JSONObject();
        object.put("approvalId", approvalId);
        object.put("comments", "OA审批拒绝后，自动审批拒绝");
        String post = httpClientUtil.post(getToken(), url, object.toJSONString());
        System.out.println("OA审批拒绝后，自动审批拒绝 ========> " + post);
    }
    public void updatePriceBookEntry(Long priceBookEntryId,JSONObject param) throws Exception {
        String url = "https://api.xiaoshouyi.com/rest/data/v2.0/xobjects/priceBookEntry/"+priceBookEntryId;
        JSONObject object = new JSONObject();
        object.put("data", param);
        String post = httpClientUtil.patch(getToken(), url, object.toJSONString());
        System.out.println("更新价格表明细 ========> " + post);
    }

    public void createPriceBookEntry(JSONObject priceDetailJson) throws Exception {
        JSONObject object = new JSONObject();
        object.put("data", priceDetailJson);
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/xobjects/priceBookEntry", object.toJSONString());
        System.out.println("创建价格表明细 ========> " + post);
    }
    public Long createPriceBook(JSONObject priceJson) throws Exception {
        Long priceBookId = null;
        JSONObject object = new JSONObject();
        object.put("data", priceJson);
        String post = httpClientUtil.post(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/xobjects/priceBook", object.toJSONString());
        System.out.println("创建价格表 ========> " + post);
        try {
            JSONObject resultJson = JSONObject.parseObject(post);
            JSONObject data = resultJson.getJSONObject("data");
            priceBookId = data.getLong("id");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return priceBookId;
    }

    public void takeEffectOrder(Long orderId) throws Exception {
        String post = httpClientUtil.patch(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/xobjects/order/actions/activation?recordId="+orderId, null);
        System.out.println("生效订单 ========> " + post);
    }

    public void deactivationOrder(Long id) throws Exception {
        String patch = httpClientUtil.patch(getToken(), "https://api.xiaoshouyi.com/rest/data/v2.0/xobjects/order/actions/deactivation?recordId=" + id, null);
        System.out.println("取消生效订单："+patch);
    }
}
