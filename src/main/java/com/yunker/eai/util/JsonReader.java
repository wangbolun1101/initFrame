package com.yunker.eai.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;

public class JsonReader {

    public static JSONObject excute(String fileName) throws IOException {

        StringBuffer buffer = new StringBuffer();
        //Todo 改为代码程序相关路径
        // BufferedReader bf= new BufferedReader(new FileReader("D:\\jiuweiCRM-K3\\"+fileName));
        /*ClassPathResource classPathResource = new ClassPathResource("/jsonConfig/"+fileName);
        InputStream inputStream = classPathResource.getInputStream();
        BufferedReader bf= new BufferedReader(new InputStreamReader(inputStream));*/
//      BufferedReader bf= new BufferedReader(new FileReader(System.getProperty("user.dir")+"/jsonConfig/"+fileName));
        BufferedReader bf= new BufferedReader(new FileReader("D:\\xiaoshouyi\\jsonConfig\\"+fileName));
        String s = null;
        while((s = bf.readLine())!=null){//使用readLine方法，一次读一行
            buffer.append(s.trim());
        }
        String result = buffer.toString();
        JSONObject repeat = new JSONObject(new LinkedHashMap<>());
        repeat = JSON.parseObject(result);
        return repeat;
    }
    public static String excutetest(String fileName) throws IOException {

        StringBuffer buffer = new StringBuffer();
        //Todo 改为代码程序相关路径
        // BufferedReader bf= new BufferedReader(new FileReader("D:\\jiuweiCRM-K3\\"+fileName));
        /*ClassPathResource classPathResource = new ClassPathResource("/jsonConfig/"+fileName);
        InputStream inputStream = classPathResource.getInputStream();
        BufferedReader bf= new BufferedReader(new InputStreamReader(inputStream));*/
//      BufferedReader bf= new BufferedReader(new FileReader(System.getProperty("user.dir")+"/jsonConfig/"+fileName));
        BufferedReader bf= new BufferedReader(new FileReader("C:\\Users\\lucg\\Desktop\\"+fileName));
        String s = null;
        while((s = bf.readLine())!=null){//使用readLine方法，一次读一行
            buffer.append(s.trim());
        }
        String result = buffer.toString();
        return result;
    }
}
