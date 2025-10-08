package com.example.flight.controller;

import com.example.flight.service.ExcelService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/excel")
public class ExcelController {

  private final ExcelService excelService;

  // ✅ ไม่มีค่าใน properties ก็จะเป็น null (ไม่พัง)
  @Value("${upload.dir:#{null}}")
  private String uploadDir;

  public ExcelController(ExcelService excelService) {
    this.excelService = excelService;
  }

  public record WriteReq(String path, String filename, List<List<String>> rows) {}

  @PostMapping("/write")
  public Object writeFromJson(@RequestBody WriteReq req) {
    // ตำแหน่งโฟลเดอร์: 1) req.path 2) uploadDir 3) temp ของระบบ
    String base = (req.path() != null && !req.path().isBlank())
            ? req.path()
            : (uploadDir != null && !uploadDir.isBlank()
                ? uploadDir
                : System.getProperty("java.io.tmpdir") + "/uploads");

    String fileName = (req.filename() == null || req.filename().isBlank())
            ? "export.xlsx" : req.filename().trim();

    java.nio.file.Path out = java.nio.file.Paths.get(base, fileName).normalize();
    List<List<String>> rows = (req.rows() == null) ? java.util.List.of() : req.rows();

    try {
      java.nio.file.Files.createDirectories(out.getParent());
      String saved = excelService.writeXlsx(out.toString(), rows);
      return java.util.Map.of(
          "message", "Excel saved",
          "path", saved,
          "filename", out.getFileName().toString()
      );
    } catch (Exception e) {
      return java.util.Map.of("error", e.getMessage());
    }
  }
}

