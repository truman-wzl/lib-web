package com.example.libweb.controller;

import com.example.libweb.entity.Category;  // 需要添加这个
import com.example.libweb.repository.BookRepository;
import com.example.libweb.repository.CategoryRepository;  // 需要添加这个
import com.example.libweb.entity.Book;
import com.example.libweb.service.BookService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;  // 需要添加这个

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import java.io.InputStream;  // 需要添加这个
import java.util.*;  // 需要添加这个
@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "http://localhost:8081")
public class BookController {

    @Autowired
    private BookService bookService;
    // ✅ 添加这两个依赖
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookRepository bookRepository;
    /**
     * 获取图书列表（分页）
     */
    @GetMapping
    public ResponseEntity<?> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Book> books = bookService.findAllBooks(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", books.getContent());
            response.put("totalPages", books.getTotalPages());
            response.put("currentPage", books.getNumber());
            response.put("totalElements", books.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取图书列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 搜索图书
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchBooks(
            @RequestParam(required = false) String bookname,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Book> books = bookService.searchBooks(bookname, author, categoryId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", books.getContent());
            response.put("totalPages", books.getTotalPages());
            response.put("currentPage", books.getNumber());
            response.put("totalElements", books.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "搜索图书失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取单个图书
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Long id) {
        try {
            Optional<Book> book = bookService.findById(id);

            if (book.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", book.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "图书不存在");
                return ResponseEntity.status(404).body(errorResponse);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取图书失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 创建或更新图书
     */
    @PostMapping
    public ResponseEntity<?> saveBook(@RequestBody Book book) {
        try {
            Book savedBook = bookService.saveBook(book);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "保存成功");
            response.put("data", savedBook);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 删除图书
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        try {
            bookService.deleteBook(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    /**
     * 批量导入图书
     * 核心逻辑：
     * 1. 根据书名(bookname)、作者(author)、出版社(publisher)判断图书唯一性。
     * 2. 若三者完全相同，则合并：总数量(totalNumber)和可借数量(canBorrow)分别累加。
     * 3. 若不同，则创建新图书。
     * 4. 分类处理：根据分类名查找，若不存在则使用默认分类（ID=5，“5中转类5”）。
     */
    @PostMapping("/import")
    @Transactional
    public ResponseEntity<Map<String, Object>> importBooks(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 验证文件
            if (file == null || file.isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择一个文件");
                return ResponseEntity.badRequest().body(response);
            }

            // 2. 验证文件类型
            String fileName = file.getOriginalFilename();
            if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
                response.put("success", false);
                response.put("message", "仅支持Excel文件（.xlsx, .xls）");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. 解析和处理Excel
            List<Map<String, Object>> errors = new ArrayList<>();
            List<Book> booksToSave = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;
            int addCount = 0;
            int updateCount = 0; // 新增：记录更新了多少本已存在的书

            try (InputStream inputStream = file.getInputStream()) {
                Workbook workbook = WorkbookFactory.create(inputStream);
                Sheet sheet = workbook.getSheetAt(0);
                int totalRows = sheet.getPhysicalNumberOfRows();

                if (totalRows <= 1) { // 只有表头
                    response.put("success", false);
                    response.put("message", "Excel文件没有数据行");
                    return ResponseEntity.badRequest().body(response);
                }

                // 4. 从第2行开始读取（跳过表头）
                for (int i = 1; i < totalRows; i++) {
                    Row row = sheet.getRow(i);
                    int rowNum = i + 1; // Excel行号（从1开始）

                    // 跳过空行
                    if (row == null || isRowEmpty(row)) {
                        continue;
                    }

                    try {
                        // 读取单元格值
                        String bookName = getCellStringValue(row.getCell(0));
                        String author = getCellStringValue(row.getCell(1));
                        String publisher = getCellStringValue(row.getCell(2));
                        String categoryName = getCellStringValue(row.getCell(3));
                        String totalNumberStr = getCellStringValue(row.getCell(4));
                        String canBorrowStr = getCellStringValue(row.getCell(5));

                        // 5. 验证必填字段
                        if (bookName == null || bookName.trim().isEmpty()) {
                            errors.add(createError(rowNum, "书名不能为空"));
                            failCount++;
                            continue;
                        }
                        if (author == null || author.trim().isEmpty()) {
                            errors.add(createError(rowNum, "作者不能为空"));
                            failCount++;
                            continue;
                        }
                        if (publisher == null || publisher.trim().isEmpty()) {
                            errors.add(createError(rowNum, "出版社不能为空"));
                            failCount++;
                            continue;
                        }

                        // 6. 验证并转换总数量
                        int totalNumber = 0;
                        if (totalNumberStr != null && !totalNumberStr.trim().isEmpty()) {
                            try {
                                totalNumber = Integer.parseInt(totalNumberStr.trim());
                                if (totalNumber < 0) {
                                    errors.add(createError(rowNum, "总数量不能为负数"));
                                    failCount++;
                                    continue;
                                }
                            } catch (NumberFormatException e) {
                                errors.add(createError(rowNum, "总数量必须是整数"));
                                failCount++;
                                continue;
                            }
                        }

                        // 7. 验证并转换可借数量（作为数字处理）
                        int canBorrow = 0; // 默认0
                        if (canBorrowStr != null && !canBorrowStr.trim().isEmpty()) {
                            try {
                                canBorrow = Integer.parseInt(canBorrowStr.trim());
                                if (canBorrow < 0) {
                                    errors.add(createError(rowNum, "可借数量不能为负数"));
                                    failCount++;
                                    continue;
                                }
                            } catch (NumberFormatException e) {
                                errors.add(createError(rowNum, "可借数量必须是整数"));
                                failCount++;
                                continue;
                            }
                        }

                        // 8. 验证可借数量不超过总数量
                        if (canBorrow > totalNumber) {
                            errors.add(createError(rowNum, "可借数量(" + canBorrow + ")不能大于总数量(" + totalNumber + ")"));
                            failCount++;
                            continue;
                        }

                        // 9. 处理分类
                        Category category = null;
                        if (categoryName != null && !categoryName.trim().isEmpty()) {
                            // 尝试查找分类
                            Optional<Category> categoryOpt = categoryRepository.findByCategoryName(categoryName.trim());
                            if (categoryOpt.isPresent()) {
                                category = categoryOpt.get();
                            } else {
                                // 分类不存在，使用默认分类 (ID=5)
                                category = getDefaultCategory();
                                errors.add(createError(rowNum, "分类 '" + categoryName + "' 不存在，已分配到默认分类'5中转类5'"));
                            }
                        } else {
                            // 分类名为空，使用默认分类
                            category = getDefaultCategory();
                        }

                        // 10. 检查图书是否已存在（核心：书名、作者、出版社）
                        // 注意：这里调用的是您在Controller中定义的私有方法，其内部会调用bookService或bookRepository
                        Optional<Book> existingBookOpt = findByBooknameAndAuthorAndPublisher(
                                bookName.trim(), author.trim(), publisher.trim());

                        if (existingBookOpt.isPresent()) {
                            // 10.1 已存在：合并库存
                            Book existingBook = existingBookOpt.get();
                            int newTotal = existingBook.getTotalNumber() + totalNumber;
                            int newCanBorrow = existingBook.getCanBorrow() + canBorrow;

                            // 合并后再次验证可借数 <= 总数
                            if (newCanBorrow > newTotal) {
                                errors.add(createError(rowNum,
                                        String.format("合并后，图书《%s》的可借数量(%d)将大于总数量(%d)，跳过本行更新。",
                                                bookName, newCanBorrow, newTotal)));
                                failCount++;
                                continue;
                            }

                            existingBook.setTotalNumber(newTotal);
                            existingBook.setCanBorrow(newCanBorrow);

                            booksToSave.add(existingBook);
                            updateCount++;
                            successCount++;

                        } else {
                            // 10.2 不存在：创建新书
                            Book newBook = new Book();
                            newBook.setBookname(bookName.trim());
                            newBook.setAuthor(author.trim());
                            newBook.setPublisher(publisher.trim());
                            newBook.setCategory(category);
                            newBook.setTotalNumber(totalNumber);
                            newBook.setCanBorrow(canBorrow);
                            newBook.setCreateTime(new Date());

                            booksToSave.add(newBook);
                            addCount++;
                            successCount++;
                        }

                    } catch (Exception e) {
                        errors.add(createError(rowNum, "处理失败: " + e.getMessage()));
                        failCount++;
                    }
                }
                workbook.close();
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "解析Excel文件失败: " + e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

            // 11. 批量保存
            if (!booksToSave.isEmpty()) {
                try {
                    // 使用Repository的saveAll进行批量保存，效率更高
                    List<Book> savedBooks = bookRepository.saveAll(booksToSave);
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("message", "保存图书数据失败: " + e.getMessage());
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // 12. 构造返回结果
            response.put("success", true);
            response.put("message", String.format("导入完成。共处理%d行，成功%d行（新增%d本，更新%d本），失败%d行。",
                    (successCount + failCount), successCount, addCount, updateCount, failCount));
            response.put("data", Map.of(
                    "total", (successCount + failCount),
                    "success", successCount,
                    "added", addCount,
                    "updated", updateCount,
                    "failed", failCount,
                    "errors", errors
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "系统错误，导入失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取默认分类（ID=5，“5中转类5”）
     */
    private Category getDefaultCategory() {
        // 优先从数据库获取
        return categoryRepository.findById(5L)
                .orElseGet(() -> {
                    // 如果数据库中连默认分类都没有，创建一个临时对象（通常不会发生）
                    Category defaultCategory = new Category();
                    defaultCategory.setCategoryId(5L);
                    defaultCategory.setCategoryName("5中转类5");
                    return defaultCategory;
                });
    }

    // 修改图书已存在检查逻辑：
    private Optional<Book> findByBooknameAndAuthorAndPublisher(String bookname, String author, String publisher) {
        // 如果BookService有这个方法，用bookService的
        // 否则用bookRepository的
        return bookRepository.findByBooknameAndAuthorAndPublisher(bookname, author, publisher);
    }
    // 辅助方法：判断行是否为空
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellStringValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    // 辅助方法：获取单元格字符串值
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 避免科学计数法
                    double num = cell.getNumericCellValue();
                    if (num == (long) num) {
                        return String.valueOf((long) num);
                    } else {
                        return String.valueOf(num);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    // 辅助方法：创建错误信息
    private Map<String, Object> createError(int row, String reason) {
        Map<String, Object> error = new HashMap<>();
        error.put("row", row);
        error.put("reason", reason);
        return error;
    }
}