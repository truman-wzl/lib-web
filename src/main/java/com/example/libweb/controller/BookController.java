package com.example.libweb.controller;

import com.example.libweb.entity.Category;
import com.example.libweb.repository.BookRepository;
import com.example.libweb.repository.CategoryRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "http://localhost:8081")
public class BookController {

    @Autowired
    private BookService bookService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private BookRepository bookRepository;

    @GetMapping
    public ResponseEntity<?> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Book> books = bookService.findAllBooks(pageable);

            return ResponseEntity.ok(buildPageResponse(books));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(buildErrorResponse("获取图书列表失败: " + e.getMessage()));
        }
    }

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

            return ResponseEntity.ok(buildPageResponse(books));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(buildErrorResponse("搜索图书失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Long id) {
        try {
            Optional<Book> book = bookService.findById(id);

            if (book.isPresent()) {
                return ResponseEntity.ok(buildSuccessResponse(book.get()));
            } else {
                return ResponseEntity.status(404).body(buildErrorResponse("图书不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(buildErrorResponse("获取图书失败: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> saveBook(@RequestBody Book book) {
        try {
            Book savedBook = bookService.saveBook(book);
            return ResponseEntity.ok(buildSuccessResponse("保存成功", savedBook));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        try {
            bookService.deleteBook(id);
            return ResponseEntity.ok(buildSuccessResponse("删除成功", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/import")
    @Transactional
    public ResponseEntity<Map<String, Object>> importBooks(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (file == null || file.isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择一个文件");
                return ResponseEntity.badRequest().body(response);
            }
            String fileName = file.getOriginalFilename();
            if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
                response.put("success", false);
                response.put("message", "仅支持Excel文件（.xlsx, .xls）");
                return ResponseEntity.badRequest().body(response);
            }

            List<Map<String, Object>> errors = new ArrayList<>();
            List<Book> booksToSave = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;
            int addCount = 0;
            int updateCount = 0;

            try (InputStream inputStream = file.getInputStream()) {
                Workbook workbook = WorkbookFactory.create(inputStream);
                Sheet sheet = workbook.getSheetAt(0);
                int totalRows = sheet.getPhysicalNumberOfRows();

                if (totalRows <= 1) {
                    response.put("success", false);
                    response.put("message", "Excel文件没有数据行");
                    return ResponseEntity.badRequest().body(response);
                }

                for (int i = 1; i < totalRows; i++) {
                    Row row = sheet.getRow(i);
                    int rowNum = i + 1;

                    if (row == null || isRowEmpty(row)) {
                        continue;
                    }
                    try {
                        String bookName = getCellStringValue(row.getCell(0));
                        String author = getCellStringValue(row.getCell(1));
                        String publisher = getCellStringValue(row.getCell(2));
                        String categoryName = getCellStringValue(row.getCell(3));
                        String totalNumberStr = getCellStringValue(row.getCell(4));
                        String canBorrowStr = getCellStringValue(row.getCell(5));

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

                        int canBorrow = 0;
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

                        if (canBorrow > totalNumber) {
                            errors.add(createError(rowNum, "可借数量(" + canBorrow + ")不能大于总数量(" + totalNumber + ")"));
                            failCount++;
                            continue;
                        }
                        // 查找或创建默认分类
                        Category category = null;
                        if (categoryName != null && !categoryName.trim().isEmpty()) {
                            Optional<Category> categoryOpt = categoryRepository.findByCategoryName(categoryName.trim());
                            if (categoryOpt.isPresent()) {
                                category = categoryOpt.get();
                            } else {
                                category = getDefaultCategory();
                                errors.add(createError(rowNum, "分类 '" + categoryName + "' 不存在，已分配到默认分类'5中转类5'"));
                            }
                        } else {
                            category = getDefaultCategory();
                        }
                        // 检查图书是否已存在（书名+作者+出版社相同视为同一本）
                        Optional<Book> existingBookOpt = findByBooknameAndAuthorAndPublisher(
                                bookName.trim(), author.trim(), publisher.trim());

                        if (existingBookOpt.isPresent()) {
                            Book existingBook = existingBookOpt.get();
                            int newTotal = existingBook.getTotalNumber() + totalNumber;
                            int newCanBorrow = existingBook.getCanBorrow() + canBorrow;

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

            if (!booksToSave.isEmpty()) {
                try {
                    bookRepository.saveAll(booksToSave);
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("message", "保存图书数据失败: " + e.getMessage());
                    return ResponseEntity.badRequest().body(response);
                }
            }

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
            return ResponseEntity.status(500).body(response);
        }
    }

    private Map<String, Object> buildPageResponse(Page<?> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", page.getContent());
        response.put("totalPages", page.getTotalPages());
        response.put("currentPage", page.getNumber());
        response.put("totalElements", page.getTotalElements());
        return response;
    }

    private Map<String, Object> buildSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> buildSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    private Category getDefaultCategory() {
        return categoryRepository.findById(5L)
                .orElseGet(() -> {
                    Category defaultCategory = new Category();
                    defaultCategory.setCategoryId(5L);
                    defaultCategory.setCategoryName("5中转类5");
                    return defaultCategory;
                });
    }

    private Optional<Book> findByBooknameAndAuthorAndPublisher(String bookname, String author, String publisher) {
        return bookRepository.findByBooknameAndAuthorAndPublisher(bookname, author, publisher);
    }

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

    private Map<String, Object> createError(int row, String reason) {
        Map<String, Object> error = new HashMap<>();
        error.put("row", row);
        error.put("reason", reason);
        return error;
    }
}
