package com.example.libweb.controller;

import com.example.libweb.entity.Category;
import com.example.libweb.repository.BookRepository;
import com.example.libweb.repository.CategoryRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:8081")
public class CategoryController {
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<?> getAllCategories(
            @RequestParam(value = "keyword", required = false) String keyword) {
        try {
            List<Category> categories;

            if (keyword != null && !keyword.trim().isEmpty()) {
                categories = categoryRepository.findByCategoryNameContaining(keyword.trim());
            } else {
                categories = categoryRepository.findAll();
            }

            return ResponseEntity.ok(buildSuccessResponse(categories));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(buildErrorResponse("获取分类列表失败：" + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(id);
            if (categoryOpt.isPresent()) {
                return ResponseEntity.ok(buildSuccessResponse(categoryOpt.get()));
            } else {
                return ResponseEntity.status(404).body(buildErrorResponse("未找到 ID 为" + id + "的分类"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(buildErrorResponse("获取分类详情失败：" + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        try {
            if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
                throw new RuntimeException("分类名称不能为空");
            }
            String categoryName = category.getCategoryName().trim();

            if (categoryRepository.existsByCategoryName(categoryName)) {
                throw new RuntimeException("分类名称 \"" + categoryName + "\" 已存在");
            }

            Category savedCategory = categoryRepository.save(category);

            return ResponseEntity.ok(buildSuccessResponse("分类创建成功", savedCategory));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildErrorResponse("创建分类时发生系统错误"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        try {
            if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
                throw new RuntimeException("分类名称不能为空");
            }
            String newName = category.getCategoryName().trim();

            Category existingCategory = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("要修改的分类 (ID: " + id + ") 不存在"));

            if (Boolean.TRUE.equals(existingCategory.getIsProtected())) {
                throw new RuntimeException("受保护的系统分类，禁止修改");
            }

            if (!existingCategory.getCategoryName().equals(newName)
                    && categoryRepository.existsByCategoryName(newName)) {
                throw new RuntimeException("分类名称 \"" + newName + "\" 已被其他分类使用");
            }

            existingCategory.setCategoryName(newName);
            Category updatedCategory = categoryRepository.save(existingCategory);

            return ResponseEntity.ok(buildSuccessResponse("分类更新成功", updatedCategory));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildErrorResponse("更新分类时发生系统错误"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            Category categoryToDelete = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("要删除的分类 (ID: " + id + ") 不存在"));

            if (Boolean.TRUE.equals(categoryToDelete.getIsProtected())) {
                throw new RuntimeException("受保护的系统分类，禁止删除");
            }
            // 查找"5中转类5"分类，用于转移图书
            Category transferCategory = categoryRepository.findByCategoryName("5中转类5")
                    .orElseThrow(() -> new RuntimeException("系统错误：未找到中转分类'5中转类5'，请联系管理员"));

            if (categoryToDelete.getCategoryId().equals(transferCategory.getCategoryId())) {
                throw new RuntimeException("无法删除系统必需的中转分类");
            }

            long bookCount = bookRepository.countByCategoryId(id);

            Map<String, Object> response = new HashMap<>();
            if (bookCount > 0) {
                int movedCount = bookRepository.updateBooksCategory(id, transferCategory.getCategoryId());
                categoryRepository.delete(categoryToDelete);

                response.put("success", true);
                response.put("message", String.format("删除成功。已将 %d 本图书从「%s」转移至「%s」。",
                        movedCount, categoryToDelete.getCategoryName(), transferCategory.getCategoryName()));
                response.put("movedCount", movedCount);
            } else {
                categoryRepository.delete(categoryToDelete);
                response.put("success", true);
                response.put("message", "删除成功。");
            }

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildErrorResponse("删除分类时发生系统错误"));
        }
    }

    @PostMapping("/import")
    public ResponseEntity<?> importCategories(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "文件不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            String fileName = file.getOriginalFilename();
            if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
                response.put("success", false);
                response.put("message", "仅支持Excel文件（.xlsx, .xls）");
                return ResponseEntity.badRequest().body(response);
            }

            List<Map<String, Object>> dataRows = new ArrayList<>();
            List<Map<String, Object>> errors = new ArrayList<>();
            int totalRows = 0;
            int successCount = 0;

            try (InputStream inputStream = file.getInputStream()) {
                Workbook workbook = null;

                if (fileName.endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(inputStream);
                } else if (fileName.endsWith(".xls")) {
                    workbook = new HSSFWorkbook(inputStream);
                }

                if (workbook == null) {
                    response.put("success", false);
                    response.put("message", "无法读取Excel文件");
                    return ResponseEntity.badRequest().body(response);
                }

                try {
                    Sheet sheet = workbook.getSheetAt(0);
                    totalRows = sheet.getPhysicalNumberOfRows();

                    if (totalRows <= 1) {
                        response.put("success", false);
                        response.put("message", "Excel文件没有数据行");
                        return ResponseEntity.badRequest().body(response);
                    }

                    Row headerRow = sheet.getRow(0);
                    if (headerRow == null ||
                            !"category_name".equalsIgnoreCase(getCellStringValue(headerRow.getCell(0))) ||
                            !"is_protected".equalsIgnoreCase(getCellStringValue(headerRow.getCell(1)))) {
                        response.put("success", false);
                        response.put("message", "Excel表头格式不正确，请使用正确的模板格式（第一行：category_name, is_protected）");
                        return ResponseEntity.badRequest().body(response);
                    }

                    for (int i = 1; i < totalRows; i++) {
                        Row row = sheet.getRow(i);
                        int rowNum = i + 1;

                        if (row == null) {
                            continue;
                        }

                        String categoryName = getCellStringValue(row.getCell(0));
                        String isProtectedStr = getCellStringValue(row.getCell(1));
                        List<String> validationErrors = validateCategoryRow(categoryName, isProtectedStr, rowNum);

                        if (!validationErrors.isEmpty()) {
                            for (String error : validationErrors) {
                                errors.add(createError(rowNum, error));
                            }
                            continue;
                        }
                        // 转换为boolean值
                        boolean isProtected = false;
                        if (isProtectedStr != null && !isProtectedStr.trim().isEmpty()) {
                            isProtectedStr = isProtectedStr.trim().toLowerCase();
                            isProtected = isProtectedStr.equals("true") || isProtectedStr.equals("1") || isProtectedStr.equals("是");
                        }

                        java.util.Optional<Category> existingCategoryOpt = categoryRepository.findByCategoryName(categoryName);
                        if (existingCategoryOpt.isPresent()) {
                            errors.add(createError(rowNum, "分类已存在，跳过：" + categoryName));
                            continue;
                        }

                        Category category = new Category();
                        category.setCategoryName(categoryName);
                        category.setIsProtected(isProtected);
                        category.setCreateTime(new Date());

                        try {
                            categoryRepository.save(category);
                            successCount++;
                            Map<String, Object> rowData = new HashMap<>();
                            rowData.put("row", rowNum);
                            rowData.put("categoryName", categoryName);
                            rowData.put("isProtected", isProtected);
                            dataRows.add(rowData);

                        } catch (Exception e) {
                            errors.add(createError(rowNum, "保存到数据库失败：" + e.getMessage()));
                        }
                    }
                } finally {
                    if (workbook != null) {
                        workbook.close();
                    }
                }

            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "解析Excel文件失败：" + e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

            int dataCount = totalRows - 1;
            int failedCount = errors.size();

            response.put("success", true);
            response.put("message", String.format("导入完成。共处理%d条数据，成功%d条，失败%d条",
                    dataCount, successCount, failedCount));
            response.put("data", Map.of(
                    "total", dataCount,
                    "success", successCount,
                    "failed", failedCount,
                    "errors", errors,
                    "importedData", dataRows
            ));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "导入失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            double numValue = cell.getNumericCellValue();
            if (numValue == 1.0) {
                return "true";
            } else if (numValue == 0.0) {
                return "false";
            }
            return String.valueOf((int) numValue);
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (cell.getCellType() == CellType.FORMULA) {
            try {
                CellType resultType = cell.getCachedFormulaResultType();
                if (resultType == CellType.STRING) {
                    return cell.getStringCellValue().trim();
                } else if (resultType == CellType.NUMERIC) {
                    return String.valueOf(cell.getNumericCellValue());
                } else if (resultType == CellType.BOOLEAN) {
                    return String.valueOf(cell.getBooleanCellValue());
                } else {
                    return cell.getCellFormula();
                }
            } catch (Exception e) {
                return cell.getCellFormula();
            }
        } else if (cell.getCellType() == CellType.BLANK) {
            return "";
        } else {
            return "";
        }
    }

    private List<String> validateCategoryRow(String categoryName, String isProtectedStr, int rowNum) {
        List<String> errors = new ArrayList<>();

        if (categoryName == null || categoryName.trim().isEmpty()) {
            errors.add("分类名称不能为空");
        } else if (categoryName.length() > 50) {
            errors.add("分类名称不能超过50个字符");
        }

        if (isProtectedStr == null || isProtectedStr.trim().isEmpty()) {
            errors.add("is_protected字段不能为空");
        } else {
            isProtectedStr = isProtectedStr.trim().toLowerCase();
            if (!isProtectedStr.equals("true") && !isProtectedStr.equals("false") &&
                    !isProtectedStr.equals("1") && !isProtectedStr.equals("0") &&
                    !isProtectedStr.equals("是") && !isProtectedStr.equals("否")) {
                errors.add("is_protected字段值无效，必须是true/false、1/0、是/否");
            }
        }

        return errors;
    }

    private Map<String, Object> createError(int row, String reason) {
        Map<String, Object> error = new HashMap<>();
        error.put("row", row);
        error.put("reason", reason);
        return error;
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
}
