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

// POI
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

// Spring 文件上传
import org.springframework.web.multipart.MultipartFile;

// Java IO
import java.io.InputStream;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

// 其他Java类
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
/**
 * 分类管理控制器
 * 专注处理与“图书分类”相关的所有HTTP API。
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:8081")
public class CategoryController {
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    //获取所有分类
    @GetMapping
    public ResponseEntity<?> getAllCategories(
            @RequestParam(value = "keyword", required = false) String keyword) {
        try {
            List<Category> categories;

            if (keyword != null && !keyword.trim().isEmpty()) {
                // 如果有搜索关键字，执行模糊搜索
                categories = categoryRepository.findByCategoryNameContaining(keyword.trim());
            } else {
                // 没有关键字，返回所有分类
                categories = categoryRepository.findAll();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取分类列表失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //根据ID获取单个分类
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(id);
            if (categoryOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", categoryOpt.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "未找到 ID 为" + id + "的分类");
                return ResponseEntity.status(404).body(errorResponse);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取分类详情失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //创建分类
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        try {
            //基本验证
            if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
                throw new RuntimeException("分类名称不能为空");
            }
            String categoryName = category.getCategoryName().trim();

            // 唯一
            if (categoryRepository.existsByCategoryName(categoryName)) {
                throw new RuntimeException("分类名称 \"" + categoryName + "\" 已存在");
            }

            // 保存到数据库
            Category savedCategory = categoryRepository.save(category);

            //成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "分类创建成功");
            response.put("data", savedCategory);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // 业务逻辑错误
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            // 其他未知错误
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "创建分类时发生系统错误");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    //修改分类
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        try {
            //基本验证
            if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
                throw new RuntimeException("分类名称不能为空");
            }
            String newName = category.getCategoryName().trim();

            //确认要修改的分类存在
            Category existingCategory = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("要修改的分类 (ID: " + id + ") 不存在"));
            //保护检查：如果是受保护分类禁止修改
            if (Boolean.TRUE.equals(existingCategory.getIsProtected())) {
                throw new RuntimeException("受保护的系统分类，禁止修改");
            }
            //只有在新名字和旧名字不同，且新名字已被其他分类使用时，才报错
            if (!existingCategory.getCategoryName().equals(newName)
                    && categoryRepository.existsByCategoryName(newName)) {
                throw new RuntimeException("分类名称 \"" + newName + "\" 已被其他分类使用");
            }

            //更新并保存
            existingCategory.setCategoryName(newName);
            Category updatedCategory = categoryRepository.save(existingCategory);

            //返回成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "分类更新成功");
            response.put("data", updatedCategory);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "更新分类时发生系统错误");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    //删除分类
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            //确认要删除的分类存在
            Category categoryToDelete = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("要删除的分类 (ID: " + id + ") 不存在"));

            //保护检查
            if (Boolean.TRUE.equals(categoryToDelete.getIsProtected())) {
                throw new RuntimeException("受保护的系统分类，禁止删除");
            }

            //查找“5中转类5”分类
            Category transferCategory = categoryRepository.findByCategoryName("5中转类5")
                    .orElseThrow(() -> new RuntimeException("系统错误：未找到中转分类‘5中转类5’，请联系管理员"));

            //是否试图删除中转分类本身
            if (categoryToDelete.getCategoryId().equals(transferCategory.getCategoryId())) {
                throw new RuntimeException("无法删除系统必需的中转分类");
            }

            //检查该分类下是否有图书
            long bookCount = bookRepository.countByCategoryId(id);

            Map<String, Object> response = new HashMap<>();
            if (bookCount > 0) {

                int movedCount = bookRepository.updateBooksCategory(id, transferCategory.getCategoryId());

                categoryRepository.delete(categoryToDelete);

                response.put("success", true);
                response.put("message", String.format("删除成功。已将 %d 本图书从「%s」转移至「%s」。", movedCount, categoryToDelete.getCategoryName(), transferCategory.getCategoryName()));
                response.put("movedCount", movedCount);
            } else {
                categoryRepository.delete(categoryToDelete);
                response.put("success", true);
                response.put("message", "删除成功。");
            }

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "删除分类时发生系统错误");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    //批量导入新分类
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

            // 解析Excel
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

                    if (totalRows <= 1) {  // 只有表头或没有数据
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
                    for (int i = 1; i < totalRows; i++) {  // 从第2行开始
                        Row row = sheet.getRow(i);
                        int rowNum = i + 1;  // Excel行从1开始

                        // 跳过空行
                        if (row == null) {
                            continue;
                        }

                        // 获取单元格值
                        String categoryName = getCellStringValue(row.getCell(0));
                        String isProtectedStr = getCellStringValue(row.getCell(1));

                        // 验证数据
                        List<String> validationErrors = validateCategoryRow(categoryName, isProtectedStr, rowNum);

                        if (!validationErrors.isEmpty()) {

                            for (String error : validationErrors) {
                                errors.add(createError(rowNum, error));
                            }
                            continue;
                        }

                        //转换为boolean
                        boolean isProtected = false;
                        if (isProtectedStr != null && !isProtectedStr.trim().isEmpty()) {
                            isProtectedStr = isProtectedStr.trim().toLowerCase();
                            isProtected = isProtectedStr.equals("true") || isProtectedStr.equals("1") || isProtectedStr.equals("是");
                        }

                        // 检查分类是否已存在
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
    /**
     * 获取单元格的字符串值
     */
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
            //对于公式单元格，尝试获取计算结果
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

    /**
     * 验证单行数据
     */
    private List<String> validateCategoryRow(String categoryName, String isProtectedStr, int rowNum) {
        List<String> errors = new ArrayList<>();

        // 验证分类名称
        if (categoryName == null || categoryName.trim().isEmpty()) {
            errors.add("分类名称不能为空");
        } else if (categoryName.length() > 50) {
            errors.add("分类名称不能超过50个字符");
        }

        // 验证是否受保护字段
        if (isProtectedStr == null || isProtectedStr.trim().isEmpty()) {
            errors.add("is_protected字段不能为空");
        } else {
            isProtectedStr = isProtectedStr.trim().toLowerCase();
            // 允许的值：true, false, 1, 0, 是, 否
            if (!isProtectedStr.equals("true") && !isProtectedStr.equals("false") &&
                    !isProtectedStr.equals("1") && !isProtectedStr.equals("0") &&
                    !isProtectedStr.equals("是") && !isProtectedStr.equals("否")) {
                errors.add("is_protected字段值无效，必须是true/false、1/0、是/否");
            }
        }

        return errors;
    }

    /**
     * 创建错误信息
     */
    private Map<String, Object> createError(int row, String reason) {
        Map<String, Object> error = new HashMap<>();
        error.put("row", row);
        error.put("reason", reason);
        return error;
    }
}