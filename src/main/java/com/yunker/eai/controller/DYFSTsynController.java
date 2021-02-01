package com.yunker.eai.controller;



import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * crm->oa同步数据
 */
@Controller
@RequestMapping("/syncOAData1")
@CrossOrigin(origins = "https://login.xiaoshouyi.com", maxAge = 3600)

public class DYFSTsynController {
    @RequestMapping("/test")
    @ResponseBody
    public void tset() {
        System.out.println("helloworld");
    }

}









