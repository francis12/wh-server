package com.wanghuan;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileGenStrTest {
    static final String FILE_PATH = "D://tjyqhdmhcxhfdm2017_ 11.txt";
    static File dstPath = new File("D://beijingSql.txt");
    //北京从16到7462
    //天津从8000 到
    static int startIndex = 16;

    /* INSERT INTO `ucip`.`ip_codeinfo` (`ZJ`, `MBLXBH`, `MBTMZ`, `SJLX`, `MBTMMS`, `SJSXSY`, `GJDBM`, `FJDBM`, `BMJB`,`QLJ`, `SFKJ`, `SJSYGJHXX`, `JLZT`, `KZZD`)VALUES
             ('NewCodeArea000000000000000000017', '110100000000', '110100000000--北京市--市辖区', '50AAD99CBB3A448D99CCEB957A59B42C', '110100000000--北京市--市辖区', NULL, NULL, 'NewCodeArea000000000000000000016', '3', ',000000000000,110000000000,110100000000,', '1', '0', '1', '');*/
    static String formatSql = "INSERT INTO `ucip`.`ip_codeinfo` (`ZJ`, `MBLXBH`, `MBTMZ`, `SJLX`, `MBTMMS`, `SJSXSY`, `GJDBM`, `FJDBM`, `BMJB`,`QLJ`, `SFKJ`, `SJSYGJHXX`, `JLZT`, `KZZD`)VALUES \n" +
            "('ZJ_STR', 'MBLXBH_STR', 'MBTMZ_STR', 'SJLX_STR', 'MBTMMS_STR', NULL, NULL, 'FJDBM_STR', 'BMJB_STR', 'QLJ_STR', '1', '0', '1', '');";

    public static void main(String[] args) {
        //System.out.println("123456789".substring(3,6));
        List<QyCxVo> list = FileGenStrTest.readFileByLines(FILE_PATH);
        FileGenStrTest.processListData(list);
    }

    public static List<QyCxVo> readFileByLines(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        String tempString = null;
        int line = 1;
        List<QyCxVo> list = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            list = new ArrayList<>();
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                //System.out.println("line " + line + ": " + tempString);
                try {
                    QyCxVo vo = genSingleLineStr(tempString.replace("\"", ""));
                    list.add(vo);
                } catch (Exception e) {
                    System.err.println(e.getCause().getMessage() + ",第" + line + "行处理错误，数据为:" + tempString);
                }
                line++;
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return list;
    }

    public static void processListData(List<QyCxVo> list) {
        if (null == list || list.size() == 0) {
            return;
        }
        Map<String, QyCxVo> map = list.stream().collect(Collectors.toMap(QyCxVo::getCode, a -> a));

        if (list.size() != map.size()) {
            System.err.println("转map数据丢失");
        }

        for (int i = 0; i < list.size(); i++) {
            QyCxVo vo = list.get(i);
            String sourceSql = new String(formatSql);

            QyCxVo item = new QyCxVo();
            BeanUtils.copyProperties(vo, item);
            String mbtmzStr = "";
            String qljStr = "";
            mbtmzStr = ("--" + item.getName()) + mbtmzStr;
            qljStr = ("," + item.getCode()) + qljStr;
            //逐级查找父节点
            while (!item.getRoot()) {
                String parentCode = item.getParentCode();
                item = map.get(parentCode);
                mbtmzStr = ("--" + item.getName()) + mbtmzStr;
                qljStr = ("," + item.getCode()) + qljStr;
            }
            String fjdbmStr = "";
            if(!vo.getRoot()) {
                fjdbmStr =  map.get(vo.getParentCode()).getZj();
            }
            sourceSql = sourceSql.replace("ZJ_STR", vo.getZj())
                    .replace("MBLXBH_STR", vo.getCode())
                    .replace("MBTMZ_STR", vo.getCode() + mbtmzStr)
                    .replace("SJLX_STR", "50AAD99CBB3A448D99CCEB957A59B42C")
                    .replace("MBTMMS_STR", vo.getCode() + mbtmzStr)
                    .replace("FJDBM_STR", fjdbmStr)
                    .replace("BMJB_STR", vo.getBmjb() + "")
                    .replace("QLJ_STR", ",000000000000" + qljStr + ",");
            try {
                FileUtils.writeStringToFile(dstPath, sourceSql+ "\r\n", true);
            } catch (IOException e) {
                System.err.println(vo.getCode() + "sql写入错误");
            }
        }
        try {
            FileUtils.writeStringToFile(dstPath, "commit;", true);
        } catch (IOException e) {
            System.err.println( "commit sql写入错误");
        }
        System.out.println(list.size() + "-" + map.size());

    }

    public static String genZjStr() {
        String postStr = "0000000" + (startIndex++);
        String zj = "NewCodeArea00000000000000" +
                postStr.substring(postStr.length() - 7);
        return zj;
    }

    public static QyCxVo genSingleLineStr(String item) throws Exception {
        if (StringUtils.isEmpty(item)) {
            throw new Exception("数据为空");
        }

        String[] dataArr = item.split(",");
        if (dataArr.length != 3) {
            throw new Exception("数据格式不符");
        }
        if (dataArr[0].length() != 12) {
            throw new Exception("第一列数据格式不符");
        }
        String code1 = dataArr[0].substring(0, 3);
        QyCxVo vo = new QyCxVo();
        vo.setZj(genZjStr());
        vo.setCode(dataArr[0]);
        vo.setCode1(dataArr[0].substring(0, 3));
        vo.setCode2(dataArr[0].substring(3, 6));
        vo.setCode3(dataArr[0].substring(6, 9));
        vo.setCode4(dataArr[0].substring(9, 12));
        vo.setName(dataArr[1]);
        vo.setType(dataArr[2]);
        if ("000".equals(vo.getCode2())) {
            vo.setMajorCode(vo.getCode1());
            vo.setParentCode(null);
            vo.setRoot(true);
            vo.setBmjb(2);
        } else {
            if ("000".equals(vo.getCode3())) {
                vo.setMajorCode(vo.getCode1() + vo.getCode2());
                vo.setParentCode((vo.getCode1()+ "000000000000").substring(0,12));
                vo.setBmjb(3);
            } else {
                if ("000".equals(vo.getCode4())) {
                    vo.setMajorCode(vo.getCode1() + vo.getCode2() + vo.getCode3());
                    vo.setParentCode((vo.getCode1() + vo.getCode2()+ "000000000000").substring(0,12));
                    vo.setBmjb(4);
                } else {
                    vo.setMajorCode(vo.getCode());
                    vo.setParentCode((vo.getCode1() + vo.getCode2()+  vo.getCode3() +  "000000000000").substring(0,12));
                    vo.setBmjb(5);
                }
            }
        }
        return vo;
    }
}
