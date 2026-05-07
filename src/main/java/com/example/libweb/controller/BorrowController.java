package com.example.libweb.controller;

import com.example.libweb.dto.BorrowRequest;
import com.example.libweb.entity.Book;
import com.example.libweb.entity.BorrowRecord;
import com.example.libweb.entity.Userdata;
import com.example.libweb.repository.BookRepository;
import com.example.libweb.repository.BorrowRecordRepository;
import com.example.libweb.repository.UserdataRepository;
import com.example.libweb.service.impl.UserServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BorrowController {

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private UserdataRepository userdataRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserServiceImpl userService;

    @PostMapping("/borrow")
    @Transactional
    public ResponseEntity<Map<String, Object>> borrowBook(@RequestBody BorrowRequest borrowRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (!borrowRequest.isValid()) {
                return ResponseEntity.badRequest().body(buildErrorResponse("用户ID和图书ID必须为正整数"));
            }

            Long userId = borrowRequest.getUserId();
            Long bookId = borrowRequest.getBookId();

            Optional<Userdata> userOpt = userdataRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(buildErrorResponse("用户不存在"));
            }

            Userdata user = userOpt.get();

            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (!bookOpt.isPresent()) {
                return ResponseEntity.badRequest().body(buildErrorResponse("图书不存在"));
            }

            Book book = bookOpt.get();
            Integer canBorrow = book.getCanBorrow();
            if (canBorrow == null || canBorrow == 0) {
                return ResponseEntity.badRequest().body(buildErrorResponse("该图书暂不可借（库存为零或者你已借阅该图书！）"));
            }

            int borrowCount = borrowRecordRepository.countBorrowedByUserAndBook(userId, bookId);
            if (borrowCount > 0) {
                return ResponseEntity.badRequest().body(buildErrorResponse("您已借阅此书且未归还"));
            }

            int userBorrowCount = borrowRecordRepository.countBorrowedByUser(userId);
            if (userBorrowCount >= 5) {
                return ResponseEntity.badRequest().body(buildErrorResponse("您已达到借阅上限（最多5本）"));
            }

            int updatedRows = bookRepository.decrementCanBorrow(bookId);
            if (updatedRows == 0) {
                return ResponseEntity.badRequest().body(buildErrorResponse("库存不足，已被其他同学借走"));
            }

            BorrowRecord record = new BorrowRecord();
            record.setUserId(userId);
            record.setBookId(bookId);
            record.setBorrowTime(LocalDateTime.now());
            record.setDueTime(LocalDateTime.now().plusMinutes(1));// 测试环境设置为1分钟，生产环境应改为30天
            record.setStatus("BORROWED");
            record.setCreateTime(LocalDateTime.now());

            borrowRecordRepository.save(record);

            response.put("success", true);
            response.put("message", "借阅成功");
            response.put("data", Map.of(
                    "recordId", record.getRecordId(),
                    "borrowTime", record.getBorrowTime(),
                    "dueTime", record.getDueTime(),
                    "bookName", book.getBookname(),
                    "userName", user.getUsername()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "借阅失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/borrow/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserBorrowRecords(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Userdata> userOpt = userdataRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(buildErrorResponse("用户不存在"));
            }

            Userdata user = userOpt.get();
            List<BorrowRecord> records = borrowRecordRepository.findByUserId(userId);

            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", Map.of(
                    "user", user,
                    "records", records,
                    "total", records.size()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/borrow/records")
    public ResponseEntity<Map<String, Object>> getCurrentUserBorrowRecordsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> response = new HashMap<>();

        try {
            Userdata currentUser = userService.getCurrentUserInfo();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "用户未登录");
                return ResponseEntity.status(401).body(response);
            }

            Long userId = currentUser.getUserId();
            // 查询前先更新该用户的逾期记录
            try {
                List<BorrowRecord> overdueRecords = borrowRecordRepository.findRecordsToMarkOverdueByUserId(userId);

                int updatedCount = 0;
                for (BorrowRecord record : overdueRecords) {
                    int result = borrowRecordRepository.markAsOverdue(record.getRecordId());
                    if (result > 0) {
                        updatedCount++;
                    }
                }
            } catch (Exception e) {
                System.err.println("逾期状态更新失败: " + e.getMessage());
            }

            if (page < 0) {
                page = 0;
            }
            if (size <= 0) {
                size = 10;
            } else if (size > 100) {
                size = 100;
            }

            int startRow = page * size;
            int endRow = (page + 1) * size;

            if (status != null && status.trim().isEmpty()) {
                status = null;
            } else if (status != null) {
                status = status.toUpperCase();
            }

            if (keyword != null && keyword.trim().isEmpty()) {
                keyword = null;
            }

            List<Object[]> results = null;
            int totalItems = 0;

            if ((status == null || "ALL".equalsIgnoreCase(status)) && keyword == null) {
                results = borrowRecordRepository.findByUserIdWithBookInfoPagination(userId, startRow, endRow);
                totalItems = borrowRecordRepository.countByUserId(userId);
            } else {
                if (status != null && "ALL".equalsIgnoreCase(status)) {
                    status = null;
                }

                results = borrowRecordRepository.findByUserIdWithBookInfoPaginationWithFilter(
                        userId, startRow, endRow, status, keyword);
                totalItems = borrowRecordRepository.countByUserIdWithFilter(userId, status, keyword);
            }

            List<Map<String, Object>> records = new ArrayList<>();
            if (results != null) {
                for (Object[] row : results) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("recordId", row[0]);
                    record.put("userId", row[1]);
                    record.put("bookId", row[2]);
                    record.put("borrowTime", row[3]);
                    record.put("dueTime", row[4]);
                    record.put("returnTime", row[5]);
                    record.put("status", row[6]);
                    record.put("createTime", row[7]);
                    record.put("bookname", row[8]);
                    record.put("author", row[9]);
                    record.put("publisher", row[10]);
                    record.put("categoryId", row[11]);
                    record.put("totalNumber", row[12]);
                    record.put("canBorrow", row[13]);
                    records.add(record);
                }
            }

            int totalPages = (int) Math.ceil((double) totalItems / size);
            if (totalPages == 0) {
                totalPages = 1;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("records", records);
            data.put("currentPage", page);
            data.put("pageSize", size);
            data.put("totalPages", totalPages);
            data.put("totalItems", totalItems);

            response.put("success", true);
            response.put("data", data);
            response.put("message", "查询成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/borrow/book/{bookId}")
    public ResponseEntity<Map<String, Object>> getBookBorrowRecords(@PathVariable Long bookId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (!bookOpt.isPresent()) {
                return ResponseEntity.badRequest().body(buildErrorResponse("图书不存在"));
            }

            Book book = bookOpt.get();
            List<BorrowRecord> records = borrowRecordRepository.findByBookId(bookId);

            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", Map.of(
                    "book", book,
                    "records", records,
                    "total", records.size()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/borrow/return")
    @Transactional
    public ResponseEntity<Map<String, Object>> returnBook(@RequestBody Map<String, Object> returnRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long recordId = null;
            try {
                Object recordIdObj = returnRequest.get("recordId");
                if (recordIdObj instanceof Integer) {
                    recordId = ((Integer) recordIdObj).longValue();
                } else if (recordIdObj instanceof Long) {
                    recordId = (Long) recordIdObj ;
                } else if (recordIdObj instanceof String) {
                    recordId = Long.parseLong((String) recordIdObj);
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(buildErrorResponse("参数格式错误: recordId必须是数字"));
            }

            if (recordId == null) {
                return ResponseEntity.badRequest().body(buildErrorResponse("借阅记录ID不能为空"));
            }

            Optional<BorrowRecord> recordOpt = borrowRecordRepository.findByRecordId(recordId);
            if (!recordOpt.isPresent()) {
                return ResponseEntity.badRequest().body(buildErrorResponse("借阅记录不存在"));
            }

            BorrowRecord record = recordOpt.get();

            if (record.getReturnTime() != null) {
                return ResponseEntity.badRequest().body(buildErrorResponse("该图书已归还"));
            }

            record.setReturnTime(LocalDateTime.now());
            record.setStatus("RETURNED");
            borrowRecordRepository.save(record);

            Long bookId = record.getBookId();
            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (bookOpt.isPresent()) {
                Book book = bookOpt.get();
                int newCanBorrow = book.getCanBorrow() + 1;
                book.setCanBorrow(newCanBorrow);
                bookRepository.save(book);
                bookRepository.flush();
            }

            response.put("success", true);
            response.put("message", "归还成功");
            response.put("data", Map.of(
                    "recordId", record.getRecordId(),
                    "returnTime", record.getReturnTime()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "归还失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/borrow/renew")
    @Transactional
    public ResponseEntity<Map<String, Object>> renewBook(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Userdata currentUser = userService.getCurrentUserInfo();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(buildErrorResponse("用户未登录，请重新登录"));
            }

            Long currentUserId = currentUser.getUserId();

            if (request.get("recordId") == null) {
                return ResponseEntity.badRequest().body(buildErrorResponse("参数错误：recordId不能为空"));
            }

            Long recordId = null;
            try {
                recordId = Long.valueOf(request.get("recordId").toString());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(buildErrorResponse("参数格式错误：recordId必须是数字"));
            }

            Optional<BorrowRecord> recordOpt = borrowRecordRepository.findByRecordId(recordId);
            if (!recordOpt.isPresent()) {
                return ResponseEntity.badRequest().body(buildErrorResponse("借阅记录不存在"));
            }

            BorrowRecord record = recordOpt.get();

            if (!record.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(403).body(buildErrorResponse("只能续借自己的借阅记录"));
            }

            LocalDateTime now = LocalDateTime.now();
            if (!"BORROWED".equals(record.getStatus())) {
                return ResponseEntity.badRequest().body(buildErrorResponse("只有借阅中的图书才能续借"));
            }

            if (record.getDueTime().isBefore(now)) {
                return ResponseEntity.badRequest().body(buildErrorResponse("已逾期的图书不能续借"));
            }
            // 计算新的应还时间（当前应还时间+30天）
            LocalDateTime newDueTime = record.getDueTime().plusDays(30);

            int updatedRows = borrowRecordRepository.renewBorrowRecord(
                    recordId, currentUserId, newDueTime
            );

            if (updatedRows == 0) {
                return ResponseEntity.badRequest().body(buildErrorResponse("续借失败，可能不满足续借条件或记录已被修改"));
            }

            response.put("success", true);
            response.put("message", "续借成功");
            response.put("data", Map.of(
                    "recordId", recordId,
                    "newDueTime", newDueTime,
                    "status", "RENEWED"
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "续借失败: " + (e.getMessage() != null ? e.getMessage() : "服务器内部错误"));
            return ResponseEntity.status(500).body(response);
        }
    }

    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
