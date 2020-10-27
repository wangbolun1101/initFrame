package com.yunker.yayun.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yunker.yayun.oaPackage.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 〈解析拼接报文〉<br>
 * 〈〉
 *
 * @author lucg
 * @create 2020/7/15
 * @since 1.0.0
 */
@Slf4j
@Component
public class WorkFlowUtil {
    @Autowired
    QueryServer queryServer;
    private long XYbelongId=1220451240657317L;
    private long XYPSbelongId=1220569214959961L;
    private long XWLJGbelongId=1248875077353801L;
    private long ZKSQbelongId=1248888806637902L;

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public WorkflowRequestInfo packageXML(JSONObject param) {
        try {
            String fielName="";
            Long creatorIdData = param.getLong("creatorId");
            Integer flowId=param.getInteger("flowId");//流程名称
            String flowName = param.getString("flowName");//流程名称
            Boolean detail = param.getBoolean("detail");//是否有明细
            String isContract = param.getString("isContract");//合同打印
            String language = param.getString("language");//语言
            Long dataId = param.getLong("dataId");//数据id
            JSONObject mainData = param.getJSONObject("mainData");
            Long belongId = param.getLong("belongId");
            if ("1".equals(isContract)){//合同审批
                    Long xhfj = mainData.getLong("xgfj");
                    if (xhfj != null && xhfj != 0){
                        String s = queryServer.downLoadDoc(xhfj);
                        fielName = queryServer.getDocInfo(xhfj);
                        mainData.put("xgfj", "base64:"+s);
                }
            }

            if (belongId.longValue()==XYbelongId){//协议
                Long xhfj = mainData.getLong("xgfj");
                if (xhfj != null && xhfj != 0){
                    String s = queryServer.downLoadDoc(xhfj);
                    fielName = queryServer.getDocInfo(xhfj);
                    mainData.put("xgfj", "base64:"+s);
                }
            }else if (belongId.longValue()==XYPSbelongId){//协议评审
                Long xgfj = mainData.getLong("xgfj");
                if (xgfj != null && xgfj != 0){
                    String s = queryServer.downLoadDoc(xgfj);
                    fielName = queryServer.getDocInfo(xgfj);
                    mainData.put("xgfj", "base64:"+s);
                }
            }else if (belongId.longValue()==XWLJGbelongId){//新物料价格
                Long xgfj = mainData.getLong("xgfj");
                if (xgfj != null && xgfj != 0){
                    String s = queryServer.downLoadDoc(xgfj);
                    fielName = queryServer.getDocInfo(xgfj);
                    mainData.put("xgfj", "base64:"+s);
                }
            }else if (belongId.longValue()==ZKSQbelongId){//折扣申请
                Long fj = mainData.getLong("fj");
                if (fj != null && fj != 0){
                    String s = queryServer.downLoadDoc(fj);
                    fielName = queryServer.getDocInfo(fj);
                    mainData.put("fj", "base64:"+s);
                }
            }
            JSONArray detailData = param.getJSONArray("detailData");

            ///////////拼接参数////////////
            WorkflowRequestInfo workflowRequestInfo=new WorkflowRequestInfo();
            workflowRequestInfo.setCreateTime(dateFormat.format(new Date()));
            workflowRequestInfo.setCreatorId(creatorIdData+"");
            workflowRequestInfo.setIsnextflow("1");
            workflowRequestInfo.setRemark(null);
            workflowRequestInfo.setRequestLevel("0");
            workflowRequestInfo.setRequestName(flowName);

            ///////////////拼接 workflowBaseInfo//////////////
            WorkflowBaseInfo workflowBaseInfo=new WorkflowBaseInfo();
            workflowBaseInfo.setWorkflowId(flowId+"");
            workflowBaseInfo.setWorkflowName(flowName);
            workflowRequestInfo.setWorkflowBaseInfo(workflowBaseInfo);

            ////////////////////////拼接主数据报文///////////////////////
            /////////拼装 workflowMainTableInfo  start//////////
            WorkflowMainTableInfo workflowMainTableInfo=new WorkflowMainTableInfo();
            /////////拼装 requestRecords start//////////
            /////////拼装 workflowRequestTableRecord  start////////
            WorkflowRequestTableRecord workflowRequestTableRecord=new WorkflowRequestTableRecord();
            workflowRequestTableRecord.setRecordOrder(0);
            ////////////////拼装 workflowRequestTableFields  start////////////////////
            List<WorkflowRequestTableField> workflowRequestTableFields = new ArrayList<>();
            ////////////////循环拼装 WorkflowRequestTableField  start////////////////////

            Set<Map.Entry<String, Object>> entries = mainData.entrySet();
            String fielName1 = fielName;
            entries.iterator().forEachRemaining(map->{
                String key = map.getKey();
                String value = map.getValue()+"";
                WorkflowRequestTableField workflowRequestTableField=new WorkflowRequestTableField();
                workflowRequestTableField.setEdit(true);
                workflowRequestTableField.setView(true);
                workflowRequestTableField.setFieldName(key);
                workflowRequestTableField.setFieldValue(value);
                if ("1".equals(isContract)&&StringUtils.isNotBlank(value)&&"xgfj".equals(key)){//合同审批
                    Integer mblx = mainData.getInteger("mblx");
                    workflowRequestTableField.setFieldType("base64:"+fielName1);
                }
                if (belongId.longValue()==XYbelongId&&StringUtils.isNotBlank(value)&&"xgfj".equals(key)){//协议
                    workflowRequestTableField.setFieldType("base64:"+fielName1);
                }else if (belongId.longValue()==XYPSbelongId&&StringUtils.isNotBlank(value)&&"xgfj".equals(key)){//协议评审
                    workflowRequestTableField.setFieldType("base64:"+fielName1);
                }else if (belongId.longValue()==XWLJGbelongId&&StringUtils.isNotBlank(value)&&"xgfj".equals(key)){//新物料价格
                    workflowRequestTableField.setFieldType("base64:"+fielName1);
                }else if (belongId.longValue()==ZKSQbelongId&&StringUtils.isNotBlank(value)&&"fj".equals(key)){//折扣申请
                    workflowRequestTableField.setFieldType("base64:"+fielName1);
                }
                workflowRequestTableFields.add(workflowRequestTableField);
            });

            ////////////////循环拼装 WorkflowRequestTableField  end////////////////////
            workflowRequestTableRecord.setWorkflowRequestTableFields(workflowRequestTableFields.toArray(new WorkflowRequestTableField[0]));
            ////////////////拼装 workflowRequestTableFields  end////////////////////
            List<WorkflowRequestTableRecord> requestRecords = new ArrayList<>();
            requestRecords.add(workflowRequestTableRecord);
            /////////拼装 workflowRequestTableRecord  end////////
            workflowMainTableInfo.setRequestRecords(requestRecords.toArray(new WorkflowRequestTableRecord[0]));
            /////////拼装 requestRecords end//////////

            workflowRequestInfo.setWorkflowMainTableInfo(workflowMainTableInfo);
            /////////拼装 workflowMainTableInfo  end//////////

            if (detail){//存在明细
                /////////拼装 workflowDetailTableInfos  start//////////
                List<WorkflowDetailTableInfo> workflowDetailTableInfos = new ArrayList<>();
                //循环拼接报文
                for (int i = 0; i < detailData.size(); i++) {
                    /////////拼装 WorkflowDetailTableInfo  start//////////
                    WorkflowDetailTableInfo workflowDetailTableInfo=new WorkflowDetailTableInfo();
                    /////////拼装 workflowRequestTableRecords  start//////////
                    List<WorkflowRequestTableRecord> workflowRequestTableRecords = new ArrayList<>();
                    /////////拼装 WorkflowRequestTableRecord  start//////////
                    WorkflowRequestTableRecord workflowRequestTableRecord_detail = new WorkflowRequestTableRecord();
                    workflowRequestTableRecord_detail.setRecordOrder(0);
                    /////////拼装 workflowRequestTableFields  start//////////
                    List<WorkflowRequestTableField> workflowRequestTableFields_detail = new ArrayList<>();
                    JSONObject jsonObject = detailData.getJSONObject(i);
                    Set<Map.Entry<String, Object>> entries1 = jsonObject.entrySet();
                    entries1.iterator().forEachRemaining(map->{
                        /////////拼装 WorkflowRequestTableField  start//////////
                        WorkflowRequestTableField workflowRequestTableField=new WorkflowRequestTableField();
                        String key = map.getKey();
                        Object value = map.getValue();
                        workflowRequestTableField.setEdit(true);
                        workflowRequestTableField.setView(true);
                        workflowRequestTableField.setFieldName(key);
                        workflowRequestTableField.setFieldValue(value+"");
                        /////////拼装 WorkflowRequestTableField  enf//////////
                        workflowRequestTableFields_detail.add(workflowRequestTableField);
                    });

                    /////////拼装 workflowRequestTableFields  end//////////
                    workflowRequestTableRecord_detail.setWorkflowRequestTableFields(workflowRequestTableFields_detail.toArray(new WorkflowRequestTableField[0]));
                    /////////拼装 WorkflowRequestTableRecord  end//////////
                    workflowRequestTableRecords.add(workflowRequestTableRecord_detail);
                    /////////拼装 workflowRequestTableRecords  end//////////

                    workflowDetailTableInfo.setWorkflowRequestTableRecords(workflowRequestTableRecords.toArray(new WorkflowRequestTableRecord[0]));
                    /////////拼装 WorkflowDetailTableInfo  end//////////
                    workflowDetailTableInfos.add(workflowDetailTableInfo);
                }

                /////////拼装 workflowDetailTableInfos  start//////////
                workflowRequestInfo.setWorkflowDetailTableInfos(workflowDetailTableInfos.toArray(new WorkflowDetailTableInfo[0]));
                /////////拼装 workflowDetailTableInfos  end//////////
            }

            return workflowRequestInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Long unPackageXML(String XML) throws Exception {
        if (StringUtils.isBlank(XML)){
            throw new Exception("webService接口未返回数据");
        }
        Document document = DocumentHelper.parseText(XML);
        Element root = document.getRootElement();
        Element element = root.element("Body");
        Element getAllStaffInfoListResponse = element.element("getAllStaffInfoListResponse");
        Element out = getAllStaffInfoListResponse.element("out");
        String textTrim = out.getTextTrim();
        if (StringUtils.isBlank(textTrim)){
            throw new Exception("webService接口未返回数据");
        }
        Long value = Long.valueOf(textTrim);
        if (value.longValue()<0){
            throw new Exception("审批同步OA失败");
        }else {
            return value;
        }

    }

//    public static void main(String[] args) {
//        packageXML(new JSONObject());
//    }

}
