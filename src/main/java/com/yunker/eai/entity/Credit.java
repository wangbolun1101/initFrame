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
@ExcelTarget("ContactNoTeam")
public class Credit {
    @Excel(name = "账套")
    public String zt;
    @Excel(name = "客户编号")
    public String accountNo;
    @Excel(name = "客户名称")
    public String accountName;
    @Excel(name = "事业部")
    public String syb;
    @Excel(name = "事业部名称")
    public String sybName;
    @Excel(name = "账期条款")
    public String zqtk;
    @Excel(name = "条款描述")
    public String tkDescribe;
    @Excel(name = "客户等级")
    public String accountLevel;
    @Excel(name = "资信额度")
    public String creditLimit;
}