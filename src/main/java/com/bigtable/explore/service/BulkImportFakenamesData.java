package com.bigtable.explore.service;

import com.bigtable.explore.util.DataFormatUtil;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sam Ma
 * @date 2020-4-15
 * 批量从csv文件中将数据导入hbase数据库
 */
@Service
public class BulkImportFakenamesData {

    private static final Logger logger = LoggerFactory.getLogger(BulkImportFakenamesData.class);

    /**
     * 将csv文件数据批量导入到hbase数据表中
     * @param csvFilePath
     */
    public void bulkImportCsvData(String csvFilePath) throws Exception {
        /**
         * hbase(main):002:0> create 'fakenames', 'personal', 'contactinfo', 'creditcard'
         * 0 row(s) in 2.4060 seconds
         * => Hbase::Table - fakenames
         */
        HTable fakenamesTable = null; /* warning: new HTable(configure, "fakenames"); */
// warning: fakenamesTable.setAutoFlush(false, true);

        int currentRow = 1;
        File dataFile = new File(csvFilePath);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(dataFile));
        // 解析csv文件内容，从文件中的数据列转换成为Put对象
        String line = bufferedReader.readLine();
        while (line != null) {
            final String[] values = line.split(",");
            // 对csv行数据记性转换（map类型），其key为列的名称 byte[]为数据值对象，便于在Put对象中进行设置
            Map<String, byte[]> dataMap = new ByteValueHashMap<String>() {{
                putAsBytes("personNumber", values[0]);
                putAsBytes("gender", values[1]);
                putAsBytes("givenName", values[2]);
                putAsBytes("mi", values[3]);
                putAsBytes("surname", values[4]);
                putAsBytes("street", values[5]);
                putAsBytes("city", values[6]);
                putAsBytes("state", values[7]);
                putAsBytes("postalCode", values[8]);
                putAsBytes("country", values[9]);
                putAsBytes("email", values[10]);
                putAsBytes("telephone", values[11]);
                putAsBytes("maidenName", values[12]);
                putAsBytes("birthdate", DataFormatUtil.reformatBirthdate(values[13]));
                putAsBytes("cardInfo", DataFormatUtil.makeCreditCardInfo(values));
                putAsBytes("nationalId", values[18]);
                putAsBytes("ups", values[19]);
            }};

            // hbase中fakenames数据表的rowKey使用业务键进行拼接
            String rowKey = String.format("%s-%s-%s-%s",
                    Bytes.toString(dataMap.get("surname")),
                    Bytes.toString(dataMap.get("givenName")),
                    Bytes.toString(dataMap.get("mi")),
                    Bytes.toString(dataMap.get("personNumber")))
                    .toLowerCase();
            saveExtractRecordToDb(fakenamesTable, rowKey, dataMap, values);

            if (currentRow % 100 == 0) {
                logger.info("now at line [{}] rowKey [{}]", currentRow, rowKey);
            }
            currentRow++;
            line = bufferedReader.readLine();
        }

        fakenamesTable.close();
    }

    /**
     * 将从csv文件中解析得到的数据保存到hbase数据表中
     * @param rowKey
     * @param dataMap
     */
    private void saveExtractRecordToDb(HTable fakenamesTable, String rowKey, Map<String, byte[]> dataMap,
                                       String[] values) throws Exception {
        Put put = new Put(Bytes.toBytes(rowKey));
        /*put.add(FakenamesConst.PERSONAL_FAMILY, FakenamesConst.PERSONNUMBER_QUALIFIER, dataMap.get("personNumber"));
        put.add(FakenamesConst.PERSONAL_FAMILY, FakenamesConst.GENDER_QUALIFIER, dataMap.get("gender"));
        put.add(FakenamesConst.PERSONAL_FAMILY, FakenamesConst.GIVENNAME_QUALIFIER, dataMap.get("givenName"));
        put.add(FakenamesConst.PERSONAL_FAMILY, FakenamesConst.MI_QUALIFIER, dataMap.get("mi"));
        put.add(FakenamesConst.PERSONAL_FAMILY, FakenamesConst.SURNAME_QUALIFIER, dataMap.get("surname"));
        put.add(FakenamesConst.PERSONAL_FAMILY, FakenamesConst.MAIDENNAME_QUALIFIER, dataMap.get("maidenName"));
        put.add(FakenamesConst.PERSONAL_FAMILY, FakenamesConst.BIRTHDATE_QUALIFIER, dataMap.get("birthdate"));
        put.add(FakenamesConst.PERSONAL_FAMILY, FakenamesConst.NATIONALID_QUALIFIER, dataMap.get("nationalId"));
        put.add(FakenamesConst.PERSONAL_FAMILY, FakenamesConst.UPS_QUALIFIER, dataMap.get("ups"));
        put.add(FakenamesConst.CONTACTINFO_FAMILY, FakenamesConst.STREET_QUALIFIER, dataMap.get("street"));
        put.add(FakenamesConst.CONTACTINFO_FAMILY, FakenamesConst.CITY_QUALIFIER, dataMap.get("city"));
        put.add(FakenamesConst.CONTACTINFO_FAMILY, FakenamesConst.STATE_QUALIFIER, dataMap.get("state"));
        put.add(FakenamesConst.CONTACTINFO_FAMILY, FakenamesConst.POSTALCODE_QUALIFIER, dataMap.get("postalCode"));
        put.add(FakenamesConst.CONTACTINFO_FAMILY, FakenamesConst.COUNTRY_QUALIFIER, dataMap.get("country"));
        put.add(FakenamesConst.CONTACTINFO_FAMILY, FakenamesConst.EMAIL_QUALIFIER, dataMap.get("email"));
        put.add(FakenamesConst.CONTACTINFO_FAMILY, FakenamesConst.TELEPHONE_QUALIFIER, dataMap.get("telephone"));
        put.add(FakenamesConst.CREDITCARD_FAMILY, Bytes.toBytes(DataFormatUtil.makeCreditCardQualifier(values)), dataMap.get("cardInfo"));*/
        fakenamesTable.put(put);
    }

    /**
     * 自定义HashMap类型其key为为泛型，value类型为byte[]
     * @param <K>
     */
    private static class ByteValueHashMap<K> extends HashMap<K, byte[]> {
        void putAsBytes(K key, String value) {
            put(key, Bytes.toBytes(value));
        }
    }

}
