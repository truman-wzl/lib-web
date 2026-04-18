package com.example.libweb.controller;

import com.example.libweb.entity.Userdata;
import com.example.libweb.repository.UserdataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserdataRepository userdataRepository;

    // 日期格式化
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页获取用户列表
     * 接口：GET /api/admin/users
     * 参数：
     *   page: 页码，从1开始（默认1）
     *   size: 每页数量（默认10）
     *   keyword: 搜索关键词（可选，搜索用户名、邮箱、真实姓名）
     *   sortField: 排序字段（可选，默认userId）
     *   sortOrder: 排序方向（asc/desc，默认desc）
     */
    // 在 AdminController.java 中修改 getUsers 方法
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        try {
            // 验证页码
            if (page < 1) page = 1;
            if (size < 1) size = 10;

            // 计算起始行和结束行
            int startRow = (page - 1) * size;
            int endRow = page * size;

            // 处理搜索关键词
            String searchKeyword = (keyword != null && !keyword.trim().isEmpty())
                    ? keyword.trim() : null;

            // 使用原生SQL分页查询
            List<Userdata> userList = userdataRepository.findByKeywordWithPagination(
                    searchKeyword, startRow, endRow);

            // 获取总记录数
            long totalUsers = (searchKeyword != null)
                    ? userdataRepository.countByKeyword(searchKeyword)
                    : userdataRepository.count();

            // 计算总页数
            int totalPages = (int) Math.ceil((double) totalUsers / size);

            // 转换为前端需要的格式
            List<Map<String, Object>> userMapList = userList.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("userId", user.getUserId());
                userMap.put("username", user.getUsername());
                userMap.put("realName", user.getRealName() != null ? user.getRealName() : "");
                userMap.put("role", user.getRole() != null ? user.getRole() : "USER");
                userMap.put("email", user.getEmail() != null ? user.getEmail() : "");
                userMap.put("phone", user.getPhone() != null ? user.getPhone() : "");
                userMap.put("status", user.getStatus() != null ? user.getStatus() : "ACTIVE");

                // 格式化时间
                if (user.getCreateTime() != null) {
                    userMap.put("createTime", dateFormat.format(user.getCreateTime()));
                } else {
                    userMap.put("createTime", "");
                }

                // 格式化最后登录时间
                if (user.getLastLoginTime() != null) {
                    userMap.put("lastLoginTime", dateFormat.format(user.getLastLoginTime()));
                } else {
                    userMap.put("lastLoginTime", "从未登录");
                }

                return userMap;
            }).collect(Collectors.toList());

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取用户列表成功");

            Map<String, Object> data = new HashMap<>();
            data.put("list", userMapList);  // 这里改为 "list"
            data.put("page", page);  // 这里改为 "page"
            data.put("size", size);  // 这里改为 "size"
            data.put("total", totalUsers);  // 这里改为 "total"
            data.put("totalPages", totalPages);
            data.put("hasNext", page < totalPages);
            data.put("hasPrevious", page > 1);

            if (searchKeyword != null) {
                data.put("keyword", searchKeyword);
            }

            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取用户列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取用户统计信息
     * 接口：GET /api/admin/users/stats
     */
    @GetMapping("/users/stats")
    public ResponseEntity<?> getUserStats() {
        try {
            // 获取用户总数
            long totalUsers = userdataRepository.count();

            // 获取活跃用户数
            long activeUsers = userdataRepository.countByStatus("ACTIVE");

            // 获取管理员数量（角色为ADMIN的用户）
            long adminCount = userdataRepository.countByRole("ADMIN");

            // 获取普通用户数量
            long userCount = totalUsers - adminCount;

            // 获取今天的注册用户数（需要扩展Repository）
            // 这里假设您有按日期统计的方法，如果没有，可以先返回0
            long todayRegistered = 0; // 暂时设为0，需要扩展Repository

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取统计信息成功");

            Map<String, Object> data = new HashMap<>();
            data.put("totalUsers", totalUsers);
            data.put("activeUsers", activeUsers);
            data.put("inactiveUsers", totalUsers - activeUsers);
            data.put("adminCount", adminCount);
            data.put("userCount", userCount);
            data.put("todayRegistered", todayRegistered);

            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 更新用户状态
     * 接口：POST /api/admin/users/{userId}/status
     * 参数：{"status": "ACTIVE" 或 "INACTIVE"}
     */
    @PostMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> requestData) {

        try {
            String status = requestData.get("status");

            // 验证状态值
            if (status == null || (!"ACTIVE".equals(status) && !"INACTIVE".equals(status))) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "状态值无效，必须是 ACTIVE 或 INACTIVE");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 查找用户
            Userdata user = userdataRepository.findById(userId).orElse(null);
            if (user == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "用户不存在，ID: " + userId);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 更新状态
            user.setStatus(status);
            userdataRepository.save(user);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户状态更新成功");

            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getUserId());
            data.put("username", user.getUsername());
            data.put("status", user.getStatus());

            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "更新用户状态失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 删除用户
     * 接口：DELETE /api/admin/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            // 检查用户是否存在
            if (!userdataRepository.existsById(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "用户不存在，ID: " + userId);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 删除用户
            userdataRepository.deleteById(userId);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户删除成功");
            response.put("data", Map.of("deletedUserId", userId));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "删除用户失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}