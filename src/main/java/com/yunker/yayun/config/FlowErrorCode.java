package com.yunker.yayun.config;

import org.apache.commons.lang.StringUtils;

public enum FlowErrorCode {
    /*
    -1：创建流程失败
    -2：用户没有流程创建权限
    -3：创建流程基本信息失败
    -4：保存表单主表信息失败
    -5：更新紧急程度失败
    -6：流程操作者失败
    -7：流转至下一节点失败
    -8：节点附加操作失败
     */
    ERROR_CODE_1(-1L,"-1:创建流程失败"),
    ERROR_CODE_2(-2L,"-2:用户没有流程创建权限"),
    ERROR_CODE_3(-3L,"-3:创建流程基本信息失败"),
    ERROR_CODE_4(-4L,"-4:保存表单主表信息失败"),
    ERROR_CODE_5(-5L,"-5:更新紧急程度失败"),
    ERROR_CODE_6(-6L,"-6:流程操作者失败"),
    ERROR_CODE_7(-7L,"-7:流转至下一节点失败"),
    ERROR_CODE_8(-8L,"-8:节点附加操作失败");

    FlowErrorCode(Long val, String msg) {
        this.val = val;
        this.msg = msg;
    }
    public static String getErrorMessage(Long val){
        for (FlowErrorCode errorCode:values()){
            if (errorCode.val().equals(val)){
                return errorCode.msg();
            }
        }
        return null;
    }

    public Long val() {
        return val;
    }

    public String msg() {
        return msg;
    }

    private Long val;
    private String msg;
}
