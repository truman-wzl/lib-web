package com.example.libweb.controller;

import com.example.libweb.entity.Userdata;
import com.example.libweb.repository.BorrowRecordRepository;
import com.example.libweb.repository.UserdataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private BorrowRecordRepository borrowRecordRepository;  // 用于执行原生 SQL

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
     * 参数：{"status": "ACTIVE"\"locked"\"cancelled"}
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, String> request) {

        try {
            String newStatus = request.get("status");
            //String reason = request.get("reason");

            // 验证状态值
            if (newStatus == null || newStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "状态值不能为空"
                ));
            }

            newStatus = newStatus.toUpperCase();
            if (!Arrays.asList("ACTIVE", "LOCKED", "CANCELLED").contains(newStatus)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "无效的状态值，只允许: ACTIVE, LOCKED, CANCELLED"
                ));
            }

            // 查找用户
            Optional<Userdata> userOpt = userdataRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "用户不存在"
                ));
            }

            Userdata user = userOpt.get();
            String oldStatus = user.getStatus();

            // 状态变更的验证逻辑
            if ("CANCELLED".equals(oldStatus)) {
                // 已注销的用户不能直接恢复（与您的要求一致）
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "已注销的用户需要重新注册，不能直接恢复状态"
                ));
            }

            if ("CANCELLED".equals(newStatus)) {
                // 注销前检查是否有未归还的图书
                // ✅ 使用 BorrowRecordRepository 检查用户是否有未归还的图书
                try {
                    // 调用 Repository 方法获取未归还图书数量
                    int unreturnedCount = borrowRecordRepository.countBorrowedByUser(userId);
                    // 记录调试信息
                    System.out.println("[注销检查] 用户ID: " + userId + ", 未归还图书数: " + unreturnedCount);
                    if (unreturnedCount > 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "success", false,
                                "message", "用户有" + unreturnedCount + "本图书未归还，请先处理借阅记录"
                        ));
                    }
                } catch (Exception e) {
                    System.out.println("[错误] 查询未归还图书失败: " + e.getMessage());
                    e.printStackTrace();
                    // 如果查询失败，为了安全起见，不允许注销
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "无法检查借阅记录，注销失败: " + e.getMessage()
                    ));
                }
            }

            // 记录操作日志（简化版本）
            System.out.println("用户状态变更: userId=" + userId +
                    ", 从 " + oldStatus + " 变为 " + newStatus);

            // 更新状态
            user.setStatus(newStatus);
            userdataRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "操作成功"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "服务器内部错误: " + e.getMessage()
            ));
        }
    }


}