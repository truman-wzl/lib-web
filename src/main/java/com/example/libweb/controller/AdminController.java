package com.example.libweb.controller;

import com.example.libweb.entity.Userdata;
import com.example.libweb.repository.BorrowRecordRepository;
import com.example.libweb.repository.UserdataRepository;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
//import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private BorrowRecordRepository borrowRecordRepository;  //原生的SQL

    @Autowired
    private UserdataRepository userdataRepository;

    // 日期格式化
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页获取用户列表
     */
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


            long todayRegistered = 0; // 暂时设为0

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

                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "已注销的用户需要重新注册，不能直接恢复状态"
                ));
            }

            if ("CANCELLED".equals(newStatus)) {
                try {
                    int unreturnedCount = borrowRecordRepository.countBorrowedByUser(userId);
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
    /**
     * 分页获取借阅记录（管理员）
     * 接口：GET /api/admin/borrows
   状态筛选（BORROWED, RETURNED, OVERDUE, RENEWED）
     */
    @GetMapping("/borrows")
    public ResponseEntity<?> getBorrowRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        try {
            // 验证页码
            if (page < 1) page = 1;
            if (size < 1) size = 10;

            // 计算起始行和结束行（Oracle ROWNUM分页）
            int startRow = (page - 1) * size;
            int endRow = page * size;
            List<Object[]> recordsData = borrowRecordRepository.findAllBorrowRecordsWithInfoPagination(
                    startRow, endRow, status);
            int total = borrowRecordRepository.countAllBorrowRecordsWithFilter(status);
            int totalPages = (int) Math.ceil((double) total / size);
            List<Map<String, Object>> recordList = recordsData.stream().map(row -> {
                Map<String, Object> record = new HashMap<>();
                record.put("recordId", row[0]);
                record.put("userId", row[1]);
                record.put("bookId", row[2]);
                if (row[3] != null) {
                    record.put("borrowTime", dateFormat.format((Date) row[3]));
                } else {
                    record.put("borrowTime", "");
                }

                if (row[4] != null) {
                    record.put("dueTime", dateFormat.format((Date) row[4]));
                } else {
                    record.put("dueTime", "");
                }

                if (row[5] != null) {
                    record.put("returnTime", dateFormat.format((Date) row[5]));
                } else {
                    record.put("returnTime", "");
                }

                record.put("status", row[6]);

                if (row[7] != null) {
                    record.put("createTime", dateFormat.format((Date) row[7]));
                } else {
                    record.put("createTime", "");
                }

                record.put("username", row[8] != null ? row[8] : "");
                record.put("realName", row[9] != null ? row[9] : "");
                record.put("bookname", row[10] != null ? row[10] : "");
                record.put("author", row[11] != null ? row[11] : "");
                record.put("publisher", row[12] != null ? row[12] : "");
                record.put("categoryId", row[13] != null ? row[13] : "");

                return record;
            }).collect(Collectors.toList());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取借阅记录成功");

            Map<String, Object> data = new HashMap<>();
            data.put("list", recordList);
            data.put("page", page);
            data.put("size", size);
            data.put("total", total);
            data.put("totalPages", totalPages);
            data.put("hasNext", page < totalPages);
            data.put("hasPrevious", page > 1);

            if (status != null) {
                data.put("status", status);
            }

            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取借阅记录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取借阅统计信息
     * 接口：GET /api/admin/borrows/stats
     */
    @GetMapping("/borrows/stats")
    public ResponseEntity<?> getBorrowStats() {
        try {
            List<Object[]> statusCounts = borrowRecordRepository.countByStatusGroup();
            int totalBorrowRecords = borrowRecordRepository.countAllBorrowRecordsWithFilter(null);
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", totalBorrowRecords);

            stats.put("borrowed", 0);
            stats.put("overdue", 0);
            stats.put("returned", 0);
            stats.put("renewed", 0);
            for (Object[] row : statusCounts) {
                String status = (String) row[0];
                Number count = (Number) row[1];

                if (status != null && count != null) {
                    int countInt = count.intValue();

                    switch (status.toUpperCase()) {
                        case "BORROWED":
                            stats.put("borrowed", countInt);
                            break;
                        case "OVERDUE":
                            stats.put("overdue", countInt);
                            break;
                        case "RETURNED":
                            stats.put("returned", countInt);
                            break;
                        case "RENEWED":
                            stats.put("renewed", countInt);
                            break;
                    }
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取借阅统计成功");
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取借阅统计失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

}