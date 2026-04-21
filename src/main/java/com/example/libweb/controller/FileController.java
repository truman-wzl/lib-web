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
    public void exportBorrow(HttpServletResponse response) throws IOException {
        List<BorrowRecord> records = borrowRecordRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("借阅记录");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"记录ID", "用户ID", "图书ID", "借阅时间", "应还时间", "归还时间", "状态"};
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

        // 填充数据
        int rowNum = 1;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (BorrowRecord record : records) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(record.getRecordId());
            row.createCell(1).setCellValue(record.getUserId());
            row.createCell(2).setCellValue(record.getBookId());

            // 借阅时间
            if (record.getBorrowTime() != null) {
                row.createCell(3).setCellValue(dateFormat.format(record.getBorrowTime()));
            } else {
                row.createCell(3).setCellValue("");
            }

            // 应还时间
            if (record.getDueTime() != null) {
                row.createCell(4).setCellValue(dateFormat.format(record.getDueTime()));
            } else {
                row.createCell(4).setCellValue("");
            }

            // 归还时间
            if (record.getReturnTime() != null) {
                row.createCell(5).setCellValue(dateFormat.format(record.getReturnTime()));
            } else {
                row.createCell(5).setCellValue("");
            }

            // 状态映射
            String status = record.getStatus();
            String statusChinese;
            switch (status) {
                case "borrowed":
                    statusChinese = "借阅中";
                    break;
                case "returned":
                    statusChinese = "已归还";
                    break;
                case "renewed":
                    statusChinese = "已续借";
                    break;
                case "overdue":
                    statusChinese = "已逾期";
                    break;
                default:
                    statusChinese = status;
            }
            row.createCell(6).setCellValue(statusChinese);
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        setResponseHeader(response, "BorrowRecord");
        workbook.write(response.getOutputStream());
        workbook.close();
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