package com.yunker.yayun.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.csvreader.CsvReader;
import com.yunker.yayun.entity.Address;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.entity.result.ExcelImportResult;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
    public static JSONArray csvSYBZQ() {
        //生成CsvReader对象，以，为分隔符，GBK编码方式
        CsvReader r = null;
        try {
            File file = new File("C:\\Users\\lucg\\Desktop\\分事业部信用额度-原料.xls");
            FileInputStream input = new FileInputStream(file);
            MultipartFile multipartFile = new MockMultipartFile("file", file.getName(), "text/plain", IOUtils.toByteArray(input));
            JSONArray successJsonArray = importExcel(multipartFile, Address.class);
            return successJsonArray;
        } catch (Exception e) {
            log.info("【客户合同价格】批量导入csv异常!", e);
            return new JSONArray();
        }
    }

    public static void main(String[] args) {
//        csv();
    }
    /**
     * 解析Excel
     *
     * @param file
     * @return
     */
    public static <T> JSONArray importExcel(MultipartFile file, Class<?> pojoClass) {
        ImportParams importParams = new ImportParams();
        importParams.setHeadRows(1);
        importParams.setNeedVerfiy(true);
        String filename = file.getOriginalFilename();
        try {
            ExcelImportResult<T> result = ExcelImportUtil.importExcelVerify(file.getInputStream(), pojoClass, importParams);
            //成功导入
            List<T> list = result.getList();
            JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(list));
            return jsonArray;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}