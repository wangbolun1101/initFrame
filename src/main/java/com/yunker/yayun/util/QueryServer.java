package com.yunker.yayun.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
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
            String addsql = sql + " limit " + count + "," + totalSize;
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
        map.put("xoql", sql);
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
        map.put("q",sql+" order by Id");
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

    public Map<Boolean, String> getContract(String templateName, Long dataId){
        Map<Boolean,String>map=new HashMap<>();
        try {
            Long templateId = getTemplateId(templateName);
            if (templateId==null||templateId==0){
                map.put(false,"未查询到对应模板——"+templateName);
                return map;
            }
            //获取打印合同
            String contactInfo = getContactInfo(dataId, templateId);
            JSONObject object = JSONObject.parseObject(contactInfo);
            String content=object.getString("data");
            String newName = new String("合同".getBytes("utf-8"),"utf-8");
            File file = base64ToFile(content, newName, ".pdf");
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
    private Long getTemplateId(String templateName) throws Exception {
        System.out.println("templateName:"+templateName);
        String s = httpClientUtil.get(getToken(), "https://api.xiaoshouyi.com/rest/template/v2.0/" + URLEncoder.encode(templateName, "UTF-8"), null);
        System.out.println("查询合同模板："+s);
        JSONObject object = JSONObject.parseObject(s);
        if (!"200".equals(object.getString("code"))){
            return 0L;
        }
        JSONArray batchData = object.getJSONArray("batchData");
        if (batchData.size()==0){
            return 0L;
        }
        Long longValue = batchData.getJSONObject(0).getJSONObject("data").getLongValue("id");
        return longValue;
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
        String s = httpClientUtil.getNosys(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/document/file/info?id=" + docId, null);
        String imageString = getImageString(s);
        return imageString;
    }
    public static String getImageString(String fileData) throws IOException {
        byte[] data = fileData.getBytes("UTF-8");
        BASE64Encoder encoder = new BASE64Encoder();
        return data != null ? encoder.encode(data) : "";
    }
    public String getDocInfo(Long docId) throws Exception {
        String s = httpClientUtil.getNosys(getToken(), "https://api.xiaoshouyi.com/data/v1/objects/document/info?id=" + docId, null);
        JSONObject object = JSONObject.parseObject(s);
        String fileName = object.getString("fileName");
        return fileName;
    }
}
