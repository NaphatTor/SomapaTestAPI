package com.example.flight.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class ExcelService {

    public Map<Integer, List<String>> readExcel(String filePath) throws IOException {
        Map<Integer, List<String>> data = new LinkedHashMap<>();
        DataFormatter fmt = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            int rows = sheet.getPhysicalNumberOfRows();
            for (int i = 0; i < rows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                List<String> rowData = new ArrayList<>();
                int columns = row.getLastCellNum();

                for (int j = 0; j < columns; j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowData.add(fmt.formatCellValue(cell));
                }
                data.put(i, rowData);
            }
        }
        return data;
    }

    public String writeXlsx(String outPath, List<List<String>> rows) throws IOException {
        Path p = Paths.get(outPath);
        if (p.getParent() != null) Files.createDirectories(p.getParent());

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Sheet1");

            for (int r = 0; r < rows.size(); r++) {
                Row row = sh.createRow(r);
                List<String> vals = rows.get(r);
                for (int c = 0; c < vals.size(); c++) {
                    row.createCell(c).setCellValue(vals.get(c) == null ? "" : vals.get(c));
                }
            }

            try (OutputStream out = Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                wb.write(out);
            }
        }
        return p.toAbsolutePath().toString();
    }

    public String writeXlsx(String outPath, Map<Integer, List<String>> rowsByIndex) throws IOException {
        List<Integer> keys = new ArrayList<>(rowsByIndex.keySet());
        Collections.sort(keys);
        List<List<String>> rows = new ArrayList<>();
        for (Integer k : keys) rows.add(rowsByIndex.getOrDefault(k, Collections.emptyList()));
        return writeXlsx(outPath, rows);
    }
}
