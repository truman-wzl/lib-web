package com.example.libweb.controller;

import com.example.libweb.entity.Userdata;
import com.example.libweb.repository.BorrowRecordRepository;
import com.example.libweb.repository.UserdataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private UserdataRepository userdataRepository;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        try {
            if (page < 1) page = 1;
            if (size < 1) size = 10;

            int startRow = (page - 1) * size;
            int endRow = page * size;

            String searchKeyword = (keyword != null && !keyword.trim().isEmpty())
                    ? keyword.trim() : null;

            List<Userdata> userList = userdataRepository.findByKeywordWithPagination(
                    searchKeyword, startRow, endRow);

            long totalUsers = (searchKeyword != null)
                    ? userdataRepository.countByKeyword(searchKeyword)
                    : userdataRepository.count();

            int totalPages = (int) Math.ceil((double) totalUsers / size);

            List<Map<String, Object>> userMapList = userList.stream()
                    .map(this::buildUserResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取用户列表成功");

            Map<String, Object> data = new HashMap<>();
            data.put("list", userMapList);
            data.put("page", page);
            data.put("size", size);
            data.put("total", totalUsers);
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

    @GetMapping("/users/stats")
    public ResponseEntity<?> getUserStats() {
        try {
            long totalUsers = userdataRepository.count();
            long activeUsers = userdataRepository.countByStatus("ACTIVE");
            long adminCount = userdataRepository.countByRole("ADMIN");
            long userCount = totalUsers - adminCount;
            long todayRegistered = 0;

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

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, String> request) {

        try {
            String newStatus = request.get("status");

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

            Optional<Userdata> userOpt = userdataRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "用户不存在"
                ));
            }

            Userdata user = userOpt.get();
            String oldStatus = user.getStatus();

            if ("CANCELLED".equals(oldStatus)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "已注销的用户需要重新注册，不能直接恢复状态"
                ));
            }

            if ("CANCELLED".equals(newStatus)) {
                try {
                    // 检查用户是否有未归还图书
                    int unreturnedCount = borrowRecordRepository.countBorrowedByUser(userId);
                    if (unreturnedCount > 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "success", false,
                                "message", "用户有" + unreturnedCount + "本图书未归还，请先处理借阅记录"
                        ));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "无法检查借阅记录，注销失败: " + e.getMessage()
                    ));
                }
            }

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

    @GetMapping("/borrows")
    public ResponseEntity<?> getBorrowRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        try {
            if (page < 1) page = 1;
            if (size < 1) size = 10;

            int startRow = (page - 1) * size;// Oracle ROWNUM分页，startRow从0开始
            int endRow = page * size;

            List<Object[]> recordsData = borrowRecordRepository.findAllBorrowRecordsWithInfoPagination(
                    startRow, endRow, status);
            int total = borrowRecordRepository.countAllBorrowRecordsWithFilter(status);
            int totalPages = (int) Math.ceil((double) total / size);

            List<Map<String, Object>> recordList = recordsData.stream()
                    .map(this::buildBorrowRecordResponse)
                    .collect(Collectors.toList());

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

    private Map<String, Object> buildUserResponse(Userdata user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("username", user.getUsername());
        userMap.put("realName", user.getRealName() != null ? user.getRealName() : "");
        userMap.put("role", user.getRole() != null ? user.getRole() : "USER");
        userMap.put("email", user.getEmail() != null ? user.getEmail() : "");
        userMap.put("phone", user.getPhone() != null ? user.getPhone() : "");
        userMap.put("status", user.getStatus() != null ? user.getStatus() : "ACTIVE");

        if (user.getCreateTime() != null) {
            userMap.put("createTime", dateFormat.format(user.getCreateTime()));
        } else {
            userMap.put("createTime", "");
        }

        if (user.getLastLoginTime() != null) {
            userMap.put("lastLoginTime", dateFormat.format(user.getLastLoginTime()));
        } else {
            userMap.put("lastLoginTime", "从未登录");
        }

        return userMap;
    }

    private Map<String, Object> buildBorrowRecordResponse(Object[] row) {
        Map<String, Object> record = new HashMap<>();
        record.put("recordId", row[0]);
        record.put("userId", row[1]);
        record.put("bookId", row[2]);

        record.put("borrowTime", row[3] != null ? dateFormat.format((Date) row[3]) : "");
        record.put("dueTime", row[4] != null ? dateFormat.format((Date) row[4]) : "");
        record.put("returnTime", row[5] != null ? dateFormat.format((Date) row[5]) : "");
        record.put("status", row[6]);
        record.put("createTime", row[7] != null ? dateFormat.format((Date) row[7]) : "");

        record.put("username", row[8] != null ? row[8] : "");
        record.put("realName", row[9] != null ? row[9] : "");
        record.put("bookname", row[10] != null ? row[10] : "");
        record.put("author", row[11] != null ? row[11] : "");
        record.put("publisher", row[12] != null ? row[12] : "");
        record.put("categoryId", row[13] != null ? row[13] : "");

        return record;
    }
}
