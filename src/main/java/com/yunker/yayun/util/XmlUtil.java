package com.yunker.yayun.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.dom4j.*;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 〈解析拼接报文〉<br>
 * 〈〉
 *
 * @author lucg
 * @create 2019/7/17
 * @since 1.0.0
 */
public class XmlUtil {
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public static String packageXML(String ERPToken, JSONArray fiedlValues) {
        SAXReader reader = new SAXReader();
        try {
            StringBuilder sb=new StringBuilder();
            Document document = reader.read(new File(System.getProperty("user.dir")+"/src/main/resources/jsonConfig/customer.xml"));
            Element root = document.getRootElement();
            Element element = root.element("Body");
            Element SaveDataSet = element.element("SaveDataSet");
            Element strSessionToken = SaveDataSet.element("strSessionToken");
            strSessionToken.setText(ERPToken);
            Element updateDataSet = SaveDataSet.element("updateDataSet");
            Element diffgram = updateDataSet.element("diffgram");
            Element slForecasts = diffgram.element("SLForecasts");
            //取字段
            Element fieldElement = SaveDataSet.element("updateDataSet").element("schema").element("element").element("complexType").element("choice").element("element").element("complexType").element("sequence");
            for(Iterator<Element> it = fieldElement.elementIterator(); it.hasNext();){
                Element next = it.next();
                Attribute name = next.attribute("name");
                String value = name.getValue();
                System.out.println(value);
                sb.append(value+",");
            }
            String[] split = sb.toString().split(",");
            for (int i = 0; i < fiedlValues.size(); i++) {
                int prex=i+1;
                Element IDO = slForecasts.addElement("IDO");
                IDO.addAttribute("diffgr:id","IDO"+prex).addAttribute("msdata:rowOrder",i+"").addAttribute("diffgr:hasChanges","inserted");
                JSONObject jsonObject = fiedlValues.getJSONObject(i);
                for (int j = 0; j < split.length; j++) {
                    Element field = IDO.addElement(split[j]);
                    Object data = jsonObject.get(split[j]);
                    field.setText(data==null?"":data.toString());
                }
            }
            System.out.println(document.asXML());
            String result = document.asXML();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static String packageXML(String filePath,JSONObject param) {
        SAXReader reader = new SAXReader();
        try {
            Long creatorIdData = param.getLong("creatorId");
            Boolean detail = param.getBoolean("detail");//是否有明细
            JSONObject mainData = param.getJSONObject("mainData");
            JSONArray detailData = param.getJSONArray("detailData");

            StringBuilder sb=new StringBuilder();
            Document document = reader.read(new File(filePath));
            Element root = document.getRootElement();
            Element element = root.element("Body");
            Element doCreateWorkflowRequest = element.element("doCreateWorkflowRequest");
            Element in0 = doCreateWorkflowRequest.element("in0");

            Element createTime = in0.element("createTime");
            createTime.setText(dateFormat.format(new Date()));
            Element creatorId = in0.element("creatorId");//todo 创建者id
            Element isnextflow = in0.element("isnextflow");//TODO 流程id
            isnextflow.setText("1");
            creatorId.setText(creatorIdData+"");
            ///////////主数据///////////
            Element workflowMainTableInfo = in0.element("workflowMainTableInfo");
            Element requestRecords = workflowMainTableInfo.element("requestRecords");
            Element workflowRequestTableRecord1 = requestRecords.element("WorkflowRequestTableRecord");
            Element workflowRequestTableFields1 = workflowRequestTableRecord1.element("workflowRequestTableFields");
            Set<Map.Entry<String, Object>> entries = mainData.entrySet();
            entries.iterator().forEachRemaining(map->{
                String key = map.getKey();
                Object value = map.getValue();
                Element WorkflowRequestTableField = workflowRequestTableFields1.addElement("web1:WorkflowRequestTableField");
                Element edit = WorkflowRequestTableField.addElement("web1:edit");
                Element view = WorkflowRequestTableField.addElement("web1:view");
                Element fieldName = WorkflowRequestTableField.addElement("web1:fieldName");
                Element fieldValue = WorkflowRequestTableField.addElement("web1:fieldValue");
                edit.setText("true");
                view.setText("true");
                fieldName.setText(key);
                fieldValue.setText(value.toString());
            });

            if (detail){//存在明细
                ///////////明细///////////
                Element workflowDetailTableInfos = in0.addElement("web1:workflowDetailTableInfos");
                Element workflowDetailTableInfo = workflowDetailTableInfos.addElement("web1:WorkflowDetailTableInfo");
                Element workflowRequestTableRecords = workflowDetailTableInfo.addElement("web1:workflowRequestTableRecords");
                Element workflowRequestTableRecord = workflowRequestTableRecords.addElement("web1:WorkflowRequestTableRecord");
                Element recordOrder = workflowRequestTableRecord.addElement("web1:recordOrder");
                recordOrder.setText("0");
                Element workflowRequestTableFields = workflowRequestTableRecord.addElement("web1:workflowRequestTableFields");
                //循环拼接报文
                for (int i = 0; i < detailData.size(); i++) {
                    JSONObject jsonObject = detailData.getJSONObject(i);
                    Set<Map.Entry<String, Object>> entries1 = jsonObject.entrySet();
                    entries1.iterator().forEachRemaining(map->{
                        String key = map.getKey();
                        Object value = map.getValue();
                        Element WorkflowRequestTableField = workflowRequestTableFields.addElement("WorkflowRequestTableField", "web1");
                        Element edit = WorkflowRequestTableField.addElement("web1:edit");
                        Element view = WorkflowRequestTableField.addElement("web1:view");
                        Element fieldName = WorkflowRequestTableField.addElement("web1:fieldName");
                        Element fieldValue = WorkflowRequestTableField.addElement("web1:fieldValue");
                        edit.setText("true");
                        view.setText("true");
                        fieldName.setText(key);
                        fieldValue.setText(value+"");
                    });
                }
            }
            System.out.println(document.asXML());
            String result = document.asXML();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONArray unPackageXML(String xmlString) throws Exception {

            JSONArray jsonArray=new JSONArray();
            Document document = DocumentHelper.parseText(xmlString);
            Element root = document.getRootElement();
            Element element = root.element("Body");
            Element getAllStaffInfoListResponse = element.element("getAllStaffInfoListResponse");
            Element out = getAllStaffInfoListResponse.element("out");
            List<Element> attributes = out.elements();
            for (Element attribute : attributes) {
                JSONObject object=new JSONObject();
                List<Element> anyType2anyTypeMap = attribute.elements();
                for (Element element1 : anyType2anyTypeMap) {
                    Element key = element1.element("key");
                    Element value = element1.element("value");
                    String data = key.getText();
                    Object data1 = value.getData();
                    object.put(data, data1);
                }
                jsonArray.add(object);
            }
            return jsonArray;
    }
    public static JSONArray unPackageXMLOld(String xmlString) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        JSONArray jsonArray=new JSONArray();
        JSONObject object=new JSONObject();

        Document document = DocumentHelper.parseText(xmlString);
        Element root = document.getRootElement();
        Element element = root.element("Body");
        Element LoadDataSetResponse = element.element("LoadDataSetResponse");
        Element LoadDataSetResult = LoadDataSetResponse.element("LoadDataSetResult");
        Element schema = LoadDataSetResult.element("schema");
        Element element2 = schema.element("element");
        Element complexType = element2.element("complexType");
        Element choice = complexType.element("choice");
        Element element1 = choice.element("element");
        Element complexType1 = element1.element("complexType");
        Element sequence = complexType1.element("sequence");
        List<Element> sequences = sequence.elements();
        for (Element attribute : sequences) {
            String name = attribute.attributeValue("name");
            String type = attribute.attributeValue("type");
            object.put(name, type.replace("xs:", ""));
        }

        Element diffgram = LoadDataSetResult.element("diffgram");
        Element slItemCustPrices = diffgram.element("SLItemCustPrices");
        List<Element> diffgrs = slItemCustPrices.elements();
        for (Element attribute : diffgrs) {
            JSONObject object1=new JSONObject();
            Set<Map.Entry<String, Object>> entries = object.entrySet();
            entries.forEach(entry->{
                String key = entry.getKey();
                String value = entry.getValue() + "";
                if ("string".equals(value)){
                    String keyValue = attribute.elementText(key);
                    object1.put(key, keyValue);
                }else if ("decimal".equals(value)){
                    String keyValue = attribute.elementText(key);
                    object1.put(key, StringUtils.isBlank(keyValue)?0:Double.valueOf(keyValue));
                }else if ("dateTime".equals(value)){
                    String keyValue = attribute.elementText(key);
                    if (StringUtils.isNotBlank(keyValue)) {
                        try {
                            Date date = formatter.parse(keyValue);
                            object1.put(key, date.getTime());
                        } catch (ParseException e) {
                        }
                    }
                }
            });
            jsonArray.add(object1);
        }

        return jsonArray;
    }
    public static JSONArray unPackageApproveXML(String xml) throws Exception {
        JSONArray jsonArray=new JSONArray();
        DateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:dd");
        Document document = DocumentHelper.parseText(xml);
//        SAXReader reader = new SAXReader();
//        Document document = reader.read(new File("C:\\Users\\lucg\\Desktop\\xml.xml"));
        Element root = document.getRootElement();
        Element element = root.element("Body");
        Element getWFStatusByIdListResponse = element.element("getWFStatusByIdListResponse");
        Element out = getWFStatusByIdListResponse.element("out");
        List<Element> outs = out.elements();
        outs.forEach(outElement->{
            List<Element> elements = outElement.elements();
            JSONObject object=new JSONObject();
            AtomicReference<String> date= new AtomicReference<>("");
            AtomicReference<String> time= new AtomicReference<>("");
            elements.forEach(key_value->{
                String key = key_value.element("key").getText();
                String value = key_value.element("value").getText();
                if ("lastoperatedate".equals(key)){
                    date.set(value);
                }else if ("lastoperatetime".equals(key)){
                    time.set(value);
                }else {
                    object.put(key, value);
                }
            });
            try {
                object.put("lastDate", dateFormat.parse((date.get()+" "+time.get())).getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            jsonArray.add(object);
        });

        return jsonArray;
    }
    public static JSONArray unPackageMAIN(String xmlString) throws Exception {
        JSONArray jsonArray=new JSONArray();
        Document document = DocumentHelper.parseText(xmlString);
        Element root = document.getRootElement();
        List<Element> elements = root.elements();
        for (Element element : elements) {
            List<Attribute> attributes = element.attributes();
            if (attributes!=null&&attributes.size()>0){
                String text = element.getText();
                text="<param>"+text+"</param>";
                Document document1 = DocumentHelper.parseText(text);
                Element root1 = document1.getRootElement();
                List<Element> dataElements = root1.elements();
                for (Element dataElement : dataElements) {
                    JSONObject object=new JSONObject();
                    List<Attribute> attributes1 = dataElement.attributes();
                    for (Attribute attribute : attributes1) {
                        QName qName = attribute.getQName();
                        String value = attribute.getValue();
                        object.put(qName.getName(), value);
                    }
                    jsonArray.add(object);
                }
            }
        }
        return jsonArray;
    }

    public static void main(String[] args) {
        try {
            unPackageApproveXML(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
