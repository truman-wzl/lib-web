package com.example.libweb.controller;

import com.example.libweb.entity.*;
import com.example.libweb.repository.*;
import com.example.libweb.service.impl.BookServiceImpl;
//import com.example.libweb.service.impl.UserServiceImpl;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/export")
public class FileController {

    @Autowired
    private CategoryRepository categoryRepository;

//    @Autowired
//    private BookRepository bookRepository;

    @Autowired
    private UserdataRepository userdataRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private BookServiceImpl bookService;
//
//    @Autowired
//    private UserServiceImpl userdataService;

    /**
     * 导出分类数据
     */
    @GetMapping("/category")
    public void exportCategory(HttpServletResponse response) throws IOException {
        List<Category> categories = categoryRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("分类列表");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"分类ID", "分类名称", "是否受保护"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);

            // 设置表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 1;
        for (Category category : categories) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(category.getCategoryId());
            row.createCell(1).setCellValue(category.getCategoryName());
            row.createCell(2).setCellValue(category.getIsProtected() ? "是" : "否");
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 设置响应头
        setResponseHeader(response, "Category");

        // 写入响应
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 导出图书数据
     */
    @GetMapping("/book")
    public void exportBook(HttpServletResponse response) throws IOException {
        // 使用Service方法获取图书数据
        List<Book> books = bookService.getAllBooks();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("图书列表");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"图书ID", "图书名称", "作者", "出版社", "分类", "总数量", "可借数量"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 1;
        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Book book : books) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(book.getBookId());
            row.createCell(1).setCellValue(book.getBookname());
            row.createCell(2).setCellValue(book.getAuthor() != null ? book.getAuthor() : "");
            row.createCell(3).setCellValue(book.getPublisher() != null ? book.getPublisher() : "");

            // 处理分类名称
            String categoryName = "";
            if (book.getCategory() != null && book.getCategory().getCategoryName() != null) {
                categoryName = book.getCategory().getCategoryName();
            }
            row.createCell(4).setCellValue(categoryName);

            row.createCell(5).setCellValue(book.getTotalNumber() != null ? book.getTotalNumber() : 0);
            row.createCell(6).setCellValue(book.getCanBorrow() != null ? book.getCanBorrow() : 0);
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        setResponseHeader(response, "Book");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 导出用户数据
     */
    @GetMapping("/user")
    public void exportUser(HttpServletResponse response) throws IOException {
        List<Userdata> users = userdataRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("用户列表");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"用户ID", "用户名", "真实姓名", "角色", "邮箱", "手机号", "状态", "最后登录时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 1;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Userdata user : users) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(user.getUserId());
            row.createCell(1).setCellValue(user.getUsername());
            row.createCell(2).setCellValue(user.getRealName() != null ? user.getRealName() : "");
            row.createCell(3).setCellValue(user.getRole() != null ? user.getRole() : "");
            row.createCell(4).setCellValue(user.getEmail() != null ? user.getEmail() : "");
            row.createCell(5).setCellValue(user.getPhone() != null ? user.getPhone() : "");
            row.createCell(6).setCellValue(user.getStatus() != null ? user.getStatus() : "");

            // 处理最后登录时间
            if (user.getLastLoginTime() != null) {
                row.createCell(7).setCellValue(dateFormat.format(user.getLastLoginTime()));
            } else {
                row.createCell(7).setCellValue("从未登录");
            }
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        setResponseHeader(response, "Userdata");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 导出借阅记录
     */
    @GetMapping("/borrow")
    public void exportBorrow(
            HttpServletResponse response,
            @RequestParam(required = false) String status) throws IOException {

        try {
            List<Object[]> recordsData;

            if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
                recordsData = borrowRecordRepository.findAllBorrowRecordsWithInfoPagination(0, Integer.MAX_VALUE, status.toUpperCase());
            } else {
                recordsData = borrowRecordRepository.findAllBorrowRecordsWithInfoPagination(0, Integer.MAX_VALUE, null);
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("借阅记录");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"记录ID", "用户名", "真实姓名", "图书名称", "作者", "出版社", "借阅时间", "应还时间", "归还时间", "状态"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);

                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (Object[] row : recordsData) {
                try {
                    Row dataRow = sheet.createRow(rowNum++);

                    Long recordId = row[0] != null ? ((Number) row[0]).longValue() : 0L;
                    Long userId = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                    Long bookId = row[2] != null ? ((Number) row[2]).longValue() : 0L;

                    Object borrowTimeObj = row[3];
                    Object dueTimeObj = row[4];
                    Object returnTimeObj = row[5];

                    String recordStatus = row[6] != null ? row[6].toString() : "";

                    String username = row[8] != null ? row[8].toString() : "";
                    String realName = row[9] != null ? row[9].toString() : "";
                    String bookname = row[10] != null ? row[10].toString() : "";
                    String author = row[11] != null ? row[11].toString() : "";
                    String publisher = row[12] != null ? row[12].toString() : "";

                    dataRow.createCell(0).setCellValue(recordId);
                    dataRow.createCell(1).setCellValue(username);
                    dataRow.createCell(2).setCellValue(realName);
                    dataRow.createCell(3).setCellValue(bookname);
                    dataRow.createCell(4).setCellValue(author);
                    dataRow.createCell(5).setCellValue(publisher);

                    if (borrowTimeObj != null) {
                        Date borrowDate = convertToDate(borrowTimeObj);
                        dataRow.createCell(6).setCellValue(borrowDate != null ? dateFormat.format(borrowDate) : "");
                    } else {
                        dataRow.createCell(6).setCellValue("");
                    }

                    if (dueTimeObj != null) {
                        Date dueDate = convertToDate(dueTimeObj);
                        dataRow.createCell(7).setCellValue(dueDate != null ? dateFormat.format(dueDate) : "");
                    } else {
                        dataRow.createCell(7).setCellValue("");
                    }

                    if (returnTimeObj != null) {
                        Date returnDate = convertToDate(returnTimeObj);
                        dataRow.createCell(8).setCellValue(returnDate != null ? dateFormat.format(returnDate) : "");
                    } else {
                        dataRow.createCell(8).setCellValue("");
                    }

                    String statusChinese;
                    switch (recordStatus.toUpperCase()) {
                        case "BORROWED":
                            statusChinese = "借阅中";
                            break;
                        case "RETURNED":
                            statusChinese = "已归还";
                            break;
                        case "RENEWED":
                            statusChinese = "已续借";
                            break;
                        case "OVERDUE":
                            statusChinese = "已逾期";
                            break;
                        default:
                            statusChinese = recordStatus;
                    }
                    dataRow.createCell(9).setCellValue(statusChinese);
                } catch (Exception e) {
                    System.err.println("处理第" + rowNum + "行数据时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            setResponseHeader(response, "BorrowRecord");
            workbook.write(response.getOutputStream());
            workbook.close();

        } catch (Exception e) {
            System.err.println("导出借阅记录失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private Date convertToDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }

        if (dateObj instanceof Date) {
            return (Date) dateObj;
        } else if (dateObj instanceof java.sql.Timestamp) {
            return new Date(((java.sql.Timestamp) dateObj).getTime());
        } else if (dateObj instanceof java.sql.Date) {
            return new Date(((java.sql.Date) dateObj).getTime());
        } else if (dateObj instanceof java.time.LocalDateTime) {
            java.time.LocalDateTime ldt = (java.time.LocalDateTime) dateObj;
            return java.sql.Timestamp.valueOf(ldt);
        } else if (dateObj instanceof java.time.LocalDate) {
            java.time.LocalDate ld = (java.time.LocalDate) dateObj;
            return java.sql.Date.valueOf(ld);
        } else {
            System.err.println("未知的日期类型: " + dateObj.getClass().getName());
            return null;
        }
    }
    /**
     * 设置响应头
     */
    private void setResponseHeader(HttpServletResponse response, String entityName) throws IOException {
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String fileName = entityName + "_" + date + ".xlsx";

        // 处理中文文件名
        fileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setCharacterEncoding("UTF-8");
    }
}