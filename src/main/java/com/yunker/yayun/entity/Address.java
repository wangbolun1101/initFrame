package com.yunker.yayun.entity;

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
    public String accountNo;
    @Excel(name = "erpid")
    public String erpid;
    @Excel(name = "详细地址[1]")
    public String address1;
    @Excel(name = "详细地址 [2]")
    public String address2;
    @Excel(name = "省/州")
    public String province;
    @Excel(name = "国家/地区")
    public String country;
    @Excel(name = "联系人")
    public String contact;
    @Excel(name = "电话")
    public String phone;
}