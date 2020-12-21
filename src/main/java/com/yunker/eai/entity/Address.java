package com.yunker.eai.entity;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecgframework.poi.excel.annotation.ExcelTarget;


/**
 * 〈发票Excel〉<br>
 * 〈〉
 *
 * @author lucg
 * @create 2019/5/7
 * @since 1.0.0
 */
@Data
@ExcelTarget("Address")
public class Address {
    @Excel(name = "客户编号")
    public String khbh;
    @Excel(name = "事业部")
    public String syb;
    @Excel(name = "信用额度")
    public String xyed;
    @Excel(name = "客户等级")
    public String khdj;
    @Excel(name = "条款")
    public String tk;
    @Excel(name = "事业部名称")
    public String sybmc;
}