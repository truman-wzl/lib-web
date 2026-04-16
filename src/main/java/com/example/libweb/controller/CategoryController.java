package com.example.libweb.controller;

import com.example.libweb.entity.Category;
import com.example.libweb.repository.BookRepository;
import com.example.libweb.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分类管理控制器
 * 专注处理与“图书分类”相关的所有HTTP API。
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:8081") // 允许你的前端（port:8081）调用
public class CategoryController {
    @Autowired
    private BookRepository bookRepository; // 需要你创建这个接口

    @Autowired
    private CategoryRepository categoryRepository;

    // ==================== 1. 查询：获取所有分类 ====================
    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        try {
            List<Category> categories = categoryRepository.findAll();
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

    // ==================== 2. 查询：根据ID获取单个分类 ====================
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

    // ==================== 3. 新增：创建分类 ====================
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        try {
            // 1. 基本验证
            if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
                throw new RuntimeException("分类名称不能为空");
            }
            String categoryName = category.getCategoryName().trim();

            // 2. 唯一性验证
            if (categoryRepository.existsByCategoryName(categoryName)) {
                throw new RuntimeException("分类名称 \"" + categoryName + "\" 已存在");
            }

            // 3. 保存到数据库
            Category savedCategory = categoryRepository.save(category);

            // 4. 返回成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "分类创建成功");
            response.put("data", savedCategory);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // 业务逻辑错误（如验证失败）
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

    // ==================== 4. 更新：修改分类 ====================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        try {
            // 1. 基本验证
            if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
                throw new RuntimeException("分类名称不能为空");
            }
            String newName = category.getCategoryName().trim();

            // 2. 确认要修改的分类存在
            Category existingCategory = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("要修改的分类 (ID: " + id + ") 不存在"));
            // 【新增】3. 保护检查：如果是受保护分类，禁止修改
            if (Boolean.TRUE.equals(existingCategory.getIsProtected())) {
                throw new RuntimeException("受保护的系统分类，禁止修改");
            }
            // 4. 唯一性验证：只有在新名字和旧名字不同，且新名字已被其他分类使用时，才报错
            if (!existingCategory.getCategoryName().equals(newName)
                    && categoryRepository.existsByCategoryName(newName)) {
                throw new RuntimeException("分类名称 \"" + newName + "\" 已被其他分类使用");
            }

            // 5 更新并保存
            existingCategory.setCategoryName(newName);
            Category updatedCategory = categoryRepository.save(existingCategory);

            // 6. 返回成功响应
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

    // ==================== 5. 删除：删除分类 ====================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            // 1. 确认要删除的分类存在
            Category categoryToDelete = categoryRepository.findById(id) // 使用 findById 以便获取对象
                    .orElseThrow(() -> new RuntimeException("要删除的分类 (ID: " + id + ") 不存在"));

            // 🛡️ 【新增】2. 保护检查：如果是受保护分类，禁止删除
            if (Boolean.TRUE.equals(categoryToDelete.getIsProtected())) {
                throw new RuntimeException("受保护的系统分类，禁止删除");
            }

            // 3. 查找“5中转类5”分类
            Category transferCategory = categoryRepository.findByCategoryName("5中转类5")
                    .orElseThrow(() -> new RuntimeException("系统错误：未找到中转分类‘5中转类5’，请联系管理员"));

            // 4. 检查是否试图删除中转分类本身（双重保险）
            if (categoryToDelete.getCategoryId().equals(transferCategory.getCategoryId())) {
                throw new RuntimeException("无法删除系统必需的中转分类");
            }

            // 5. 检查该分类下是否有图书
            long bookCount = bookRepository.countByCategoryId(id); // 需要 BookRepository 有这个方法

            Map<String, Object> response = new HashMap<>();
            if (bookCount > 0) {
                // 6. 有图书：执行转移
                int movedCount = bookRepository.updateBooksCategory(id, transferCategory.getCategoryId()); // 需要此方法
                // 7. 删除原分类
                categoryRepository.delete(categoryToDelete);

                response.put("success", true);
                response.put("message", String.format("删除成功。已将 %d 本图书从「%s」转移至「%s」。", movedCount, categoryToDelete.getCategoryName(), transferCategory.getCategoryName()));
                response.put("movedCount", movedCount);
            } else {
                // 8. 无图书：直接删除
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
}