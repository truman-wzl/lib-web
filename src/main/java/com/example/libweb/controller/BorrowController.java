package com.example.libweb.controller;

import com.example.libweb.dto.BorrowRequest;
//实体类
import com.example.libweb.entity.Book;
import com.example.libweb.entity.BorrowRecord;
import com.example.libweb.entity.Userdata;
//仓库类
import com.example.libweb.repository.BookRepository;
import com.example.libweb.repository.BorrowRecordRepository;
import com.example.libweb.repository.UserdataRepository;
//服务实现接口
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
    /**
     * 借阅图书
     * POST /api/borrow
     */
    @PostMapping("/borrow")
    @Transactional
    public ResponseEntity<Map<String, Object>> borrowBook(@RequestBody BorrowRequest borrowRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (!borrowRequest.isValid()) {
                response.put("success", false);
                response.put("message", "用户ID和图书ID必须为正整数");
                return ResponseEntity.badRequest().body(response);
            }
            Long userId = borrowRequest.getUserId();
            Long bookId = borrowRequest.getBookId();
            Optional<Userdata> userOpt = userdataRepository.findById(userId);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.badRequest().body(response);
            }

            Userdata user = userOpt.get();

            //验证图书是否存在
            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (!bookOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "图书不存在");
                return ResponseEntity.badRequest().body(response);
            }

            Book book = bookOpt.get();
            Integer canBorrow = book.getCanBorrow();
            if (canBorrow == null || canBorrow == 0) {
                response.put("success", false);
                response.put("message", "该图书暂不可借（库存为零或者你已借阅该图书！）");
                return ResponseEntity.badRequest().body(response);
            }

            //检查用户是否已借阅此书且未归还
            int borrowCount = borrowRecordRepository.countBorrowedByUserAndBook(userId, bookId);
            if (borrowCount > 0) {
                response.put("success", false);
                response.put("message", "您已借阅此书且未归还");
                return ResponseEntity.badRequest().body(response);
            }
            int userBorrowCount = borrowRecordRepository.countBorrowedByUser(userId);
            if (userBorrowCount >= 5) {
                response.put("success", false);
                response.put("message", "您已达到借阅上限（最多5本）");
                return ResponseEntity.badRequest().body(response);
            }

            int updatedRows = bookRepository.decrementCanBorrow(bookId);

            if (updatedRows == 0) {
                response.put("success", false);
                response.put("message", "库存不足，已被其他同学借走");
                return ResponseEntity.badRequest().body(response);
            }
            BorrowRecord record = new BorrowRecord();
            record.setUserId(userId);
            record.setBookId(bookId);
            record.setBorrowTime(LocalDateTime.now());
            Calendar calendar = Calendar.getInstance();
            //calendar.add(Calendar.DAY_OF_MONTH, 30);
            calendar.add(Calendar.MINUTE, 1);
            record.setDueTime(LocalDateTime.now().plusMinutes(1));
            // record.setDueTime(LocalDateTime.now().plusDays(30));

            // 状态为"BORROWED"
            record.setStatus("BORROWED");
            record.setCreateTime(LocalDateTime.now());

            borrowRecordRepository.save(record);

            //更新图书库存
//            int newCanBorrow = canBorrow - 1;
//            if (newCanBorrow < 0) {
//                newCanBorrow = 0;
//            }
//            book.setCanBorrow(newCanBorrow);
//            bookRepository.save(book);
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
    /**
     * 查询用户的借阅记录（无分页）
     * GET /api/borrow/user/{userId}
     */
    @GetMapping("/borrow/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserBorrowRecords(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 验证用户是否存在
            Optional<Userdata> userOpt = userdataRepository.findById(userId);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.badRequest().body(response);
            }

            // 查询借阅记录
            Userdata user = userOpt.get();
            java.util.List<BorrowRecord> records = borrowRecordRepository.findByUserId(userId);

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
    /**
     * 查询当前登录用户的借阅记录（带分页和筛选）
     * GET /api/borrow/records
     */
    @GetMapping("/borrow/records")
    public ResponseEntity<Map<String, Object>> getCurrentUserBorrowRecordsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> response = new HashMap<>();

        try {
            //从Session获取当前用户
            Userdata currentUser = userService.getCurrentUserInfo();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "用户未登录");
                return ResponseEntity.status(401).body(response);
            }

            Long userId = currentUser.getUserId();
            System.out.println("从Session获取当前用户ID: " + userId);
            //查询前先更新该用户的逾期记录
            try {

                List<BorrowRecord> overdueRecords = borrowRecordRepository.findRecordsToMarkOverdueByUserId(userId);


                int updatedCount = 0;
                for (BorrowRecord record : overdueRecords) {
                    int result = borrowRecordRepository.markAsOverdue(record.getRecordId());
                    if (result > 0) {
                        updatedCount++;
                        System.out.println(" 自动更新逾期状态: 记录ID=" + record.getRecordId()
                                + ", 用户ID=" + userId);
                    }
                }

                if (updatedCount > 0) {
                    System.out.println(" 用户ID=" + userId + " 的逾期记录已更新, 共" + updatedCount + "条");
                }
            } catch (Exception e) {
                System.err.println("逾期状态更新失败: " + e.getMessage());
            }
            System.out.println("筛选参数 - status: " + status + ", keyword: " + keyword);

            //验证分页参数
            if (page < 0) {
                page = 0;
            }
            if (size <= 0) {
                size = 10;
            } else if (size > 100) {
                size = 100; // 限制最大每页100条
            }

            //计算分页参数（Oracle ROWNUM从1开始）
            int startRow = page * size;
            int endRow = (page + 1) * size;

            //处理筛选参数
            //将空字符串转为null，以便SQL中的IS NULL判断
            if (status != null && status.trim().isEmpty()) {
                status = null;
            } else if (status != null) {
                status = status.toUpperCase();
            }

            if (keyword != null && keyword.trim().isEmpty()) {
                keyword = null;
            }

            System.out.println("处理后的筛选参数 - status: " + status + ", keyword: " + keyword);


            List<Object[]> results = null;
            int totalItems = 0;

            if ((status == null || "ALL".equalsIgnoreCase(status)) && keyword == null) {
                //如果没有筛选条件，使用原来的查询
                results = borrowRecordRepository.findByUserIdWithBookInfoPagination(userId, startRow, endRow);
                totalItems = borrowRecordRepository.countByUserId(userId);
            } else {
                // 如果状态是"ALL"，则视为null
                if (status != null && "ALL".equalsIgnoreCase(status)) {
                    status = null;
                }

                results = borrowRecordRepository.findByUserIdWithBookInfoPaginationWithFilter(
                        userId, startRow, endRow, status, keyword);
                totalItems = borrowRecordRepository.countByUserIdWithFilter(userId, status, keyword);
            }

            System.out.println("查询到记录数: " + (results != null ? results.size() : 0));
            System.out.println("总记录数: " + totalItems);

            // 6. 转换结果
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
    /**
     * 查询图书的借阅记录
     * GET /api/borrow/book/{bookId}
     */
    @GetMapping("/borrow/book/{bookId}")
    public ResponseEntity<Map<String, Object>> getBookBorrowRecords(@PathVariable Long bookId) {
        Map<String, Object> response = new HashMap<>();

        try {
            //验证图书是否存在
            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (!bookOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "图书不存在");
                return ResponseEntity.badRequest().body(response);
            }

            //查询借阅记录
            Book book = bookOpt.get();
            java.util.List<BorrowRecord> records = borrowRecordRepository.findByBookId(bookId);

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

    /**
     * 归还图书
     * POST /api/borrow/return
     */
    @PostMapping("/borrow/return")
    @Transactional
    public ResponseEntity<Map<String, Object>> returnBook(@RequestBody Map<String, Object> returnRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            //获取请求参数
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
                response.put("success", false);
                response.put("message", "参数格式错误: recordId必须是数字");
                return ResponseEntity.badRequest().body(response);
            }

            if (recordId == null) {
                response.put("success", false);
                response.put("message", "借阅记录ID不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 查询借阅记录
            Optional<BorrowRecord> recordOpt = borrowRecordRepository.findByRecordId(recordId);
            if (!recordOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "借阅记录不存在");
                return ResponseEntity.badRequest().body(response);
            }

            BorrowRecord record = recordOpt.get();

            // 检查是否已归还
            if (record.getReturnTime() != null) {
                response.put("success", false);
                response.put("message", "该图书已归还");
                return ResponseEntity.badRequest().body(response);
            }

            // 更新借阅记录
            record.setReturnTime(LocalDateTime.now());
            record.setStatus("RETURNED");
            borrowRecordRepository.save(record);

            // 更新图书库存
            Long bookId = record.getBookId();
            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (bookOpt.isPresent()) {
                Book book = bookOpt.get();
                int newCanBorrow = book.getCanBorrow() + 1;
                book.setCanBorrow(newCanBorrow);
                bookRepository.save(book);
                bookRepository.flush();  //强制立即同步到数据库
            }

            // 返回成功响应
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

    /**
     * 续借图书
     * POST /api/borrow/renew
     */
    @PostMapping("/borrow/renew")
    @Transactional
    public ResponseEntity<Map<String, Object>> renewBook(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            //获取当前登录用户
            Userdata currentUser = userService.getCurrentUserInfo();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "用户未登录，请重新登录");
                return ResponseEntity.status(401).body(response);
            }

            Long currentUserId = currentUser.getUserId();

            //获取请求参数
            if (request.get("recordId") == null) {
                response.put("success", false);
                response.put("message", "参数错误：recordId不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            Long recordId = null;
            try {
                recordId = Long.valueOf(request.get("recordId").toString());
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "参数格式错误：recordId必须是数字");
                return ResponseEntity.badRequest().body(response);
            }

            //查询借阅记录
            Optional<BorrowRecord> recordOpt = borrowRecordRepository.findByRecordId(recordId);
            if (!recordOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "借阅记录不存在");
                return ResponseEntity.badRequest().body(response);
            }

            BorrowRecord record = recordOpt.get();

            // 验证权限：只能续借自己的记录
            if (!record.getUserId().equals(currentUserId)) {
                response.put("success", false);
                response.put("message", "只能续借自己的借阅记录");
                return ResponseEntity.status(403).body(response);
            }

            //验证状态和日期
            LocalDateTime now = LocalDateTime.now();
            if (!"BORROWED".equals(record.getStatus())) {
                response.put("success", false);
                response.put("message", "只有借阅中的图书才能续借");
                return ResponseEntity.badRequest().body(response);
            }

            if (record.getDueTime().isBefore(now)) {
                response.put("success", false);
                response.put("message", "已逾期的图书不能续借");
                return ResponseEntity.badRequest().body(response);
            }

            //计算新的应还时间（当前应还时间+30天）
            Calendar calendar = Calendar.getInstance();
            LocalDateTime newDueTime = record.getDueTime().plusDays(30);
            calendar.add(Calendar.DAY_OF_MONTH, 30);
            //Date newDueTime = calendar.getTime();

            //原子更新
            int updatedRows = borrowRecordRepository.renewBorrowRecord(
                    recordId, currentUserId, newDueTime
            );

            if (updatedRows == 0) {
                response.put("success", false);
                response.put("message", "续借失败，可能不满足续借条件或记录已被修改");
                return ResponseEntity.badRequest().body(response);
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

}