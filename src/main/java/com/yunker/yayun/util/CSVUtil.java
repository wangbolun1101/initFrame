package com.yunker.yayun.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.csvreader.CsvReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @FileName: CSVUtil
 * @Description: 解析CSV格式文件的工具类
 */
@Slf4j
public class CSVUtil {

    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    public static JSONArray csv(MultipartFile file) {
        //生成CsvReader对象，以，为分隔符，GBK编码方式
        CsvReader r = null;
        try {
            JSONArray jsonArray=new JSONArray();
            r =new CsvReader(file.getInputStream(), '\t', Charset.forName("UTF-8"));
//            String csvFilePath = "C:\\Users\\lucg\\Desktop\\1客户合同价格-HXSW12.csv";
//            r = new CsvReader(csvFilePath, '\t', Charset.forName("UTF-8"));
            //读取表头
            r.readHeaders();
            //逐条读取记录，直至读完
            while (r.readRecord()) {
                JSONObject object=new JSONObject();
                String kh = r.get("客户");
                String mc = r.get("名称");
                String wl = r.get("物料");
                String hb = r.get("货币");
                String sxrq = r.get("生效日期");
                Date date = formatter.parse(sxrq);
                String htjg = r.get("合同价格");
                String ggh = r.get("规格号");
                object.put("CustNum", kh);
                object.put("mc", mc);
                object.put("Item", wl);
                object.put("EffectDate", date.getTime());
                object.put("AdrCurrCode", hb);
                object.put("ContPrice", htjg);
                object.put("Uf_StandNum", ggh);
                jsonArray.add(object);
            }
            r.close();
            return jsonArray;
        } catch (Exception e) {
            log.info("【客户合同价格】批量导入csv异常!", e);
            return new JSONArray();
        } finally {
            r.close();
        }
    }

    public static void main(String[] args) {
//        csv();
    }

}