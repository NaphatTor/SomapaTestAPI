package com.example.flight.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.flight.domain.Passenger;
import com.example.flight.domain.UploadResponse;

@Service
public class FileService {
  private final Path uploadDir;
  
  private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z]{1,20}$");
  private static final Pattern NATIONALITY_PATTERN = Pattern.compile("^[A-Za-z]{3}$");


  public FileService(@Value("${upload.dir:#{null}}") String uploadDirProp) throws IOException {
    Path base = (uploadDirProp == null || uploadDirProp.isBlank())
        ? Paths.get(System.getProperty("java.io.tmpdir"), "uploads")
        : Paths.get(uploadDirProp);
    this.uploadDir = base.toAbsolutePath().normalize();
    Files.createDirectories(this.uploadDir);
  	}

    /** ========== UPLOAD (≤ 1MB) ========== */
    public UploadResponse fileUpload(MultipartFile multipartfile, String name) throws IOException {
        if (multipartfile == null || multipartfile.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (multipartfile.getSize() > 1024 * 1024) { // 1MB
            throw new IllegalArgumentException("File size must be less than 1 MB");
        }

        String originalFileName = multipartfile.getOriginalFilename();
        String extension = "";
        int dot = originalFileName.lastIndexOf('.');
        if (dot >= 0) extension = originalFileName.substring(dot);
        if (!extension.equalsIgnoreCase(".xlsx")) {
            throw new IllegalArgumentException("HTTP 400 Bad Request");
        }
        	
        UploadResponse res = validateExcel(multipartfile.getInputStream());
        return res;
    }
    

    private UploadResponse validateExcel(InputStream in) throws IOException {
    	UploadResponse upload = new UploadResponse();
    	List<String> errors = new ArrayList<>();
    	List<Passenger> rows = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter(Locale.US);
            
            int first = sheet.getFirstRowNum();
            int last  = sheet.getLastRowNum();
            
            for (int r = first + 1; r <= last; r++) {
            	Passenger passenger = new Passenger();
            	String error = "";
                Row row = sheet.getRow(r);
                if (row == null) continue;
                
                String firstName  = fmt.formatCellValue(row.getCell(0));
                String lastName   = fmt.formatCellValue(row.getCell(1));
                String gender     = fmt.formatCellValue(row.getCell(2));
                String dobRaw     = fmt.formatCellValue(row.getCell(3));
                String nationality= fmt.formatCellValue(row.getCell(4));
                
                System.out.println(r+" "+firstName+" "+lastName+" "+gender+" "+dobRaw+" "+nationality);
                
                passenger.setNo(r);
                passenger.setFirstName(firstName);
                passenger.setLastName(lastName);
                passenger.setGender(gender);
                passenger.setDateOfBirth(dobRaw);
                passenger.setNationality(nationality);
                
                rows.add(passenger);
                
                if (!NAME_PATTERN.matcher(firstName).matches()) {
                	error += "First name, ";
                }
                if (!NAME_PATTERN.matcher(lastName).matches()) {
                	error += "Last name, ";
                }
                if(!(gender.equals("Male") || gender.equals("Female") || gender.equals("Unknown"))) {
                	error += "Gender, ";
                }
                if(!NATIONALITY_PATTERN.matcher(nationality).matches()) {
                	error += "Nationality, ";
                }
                if(!error.equals("")) {
                	error = "Row " + r + " Invalid " + error.substring(0, error.length() - 2);  
                	errors.add(error);
                }
                System.out.println(error);
            }
        }catch(Exception e) {
        	e.printStackTrace();
        }
        upload.setErrors(errors);
        upload.setRows(rows);
        return upload;
	}

	/** ========== DOWNLOAD ========== */
    public Resource fileDownload(String filename) throws IOException {
    	if (filename == null || filename.trim().isEmpty()) return null;
        String safeName = filename.replace("..", "");
        Path target = uploadDir.resolve(safeName).normalize();

        if (!target.startsWith(uploadDir) || Files.notExists(target) || Files.isDirectory(target)) {
            return null;
        }
        try {
            return new UrlResource(target.toUri());
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
