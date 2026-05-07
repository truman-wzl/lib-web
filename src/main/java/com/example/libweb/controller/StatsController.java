package com.example.libweb.controller;

import com.example.libweb.repository.BorrowRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 数据统计控制器
 * 提供借阅趋势、热门图书、热门类别的统计接口
 */
@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class StatsController {

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    /**
     * 获取最近7天的借阅趋势（不包含今天）
     * GET /api/stats/borrow-trend
     * 返回：{success: true, data: {dates: [], counts: []}}
     */
    @GetMapping("/borrow-trend")
    public ResponseEntity<Map<String, Object>> getBorrowTrend() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 生成过去7天的日期列表（不包含今天）
            List<String> dateList = generateLast7DaysExcludingToday();

            // 2. 查询数据库
            List<Object[]> queryResults = borrowRecordRepository.findBorrowTrendLast7Days();

            // 3. 将查询结果转换为Map便于查找
            Map<String, Integer> dateCountMap = new HashMap<>();
            for (Object[] row : queryResults) {
                String dateStr = (String) row[0];  // 格式：yyyy-MM-dd
                Number count = (Number) row[1];    // 借阅次数
                dateCountMap.put(dateStr, count.intValue());
            }

            // 4. 补全数据，确保7天都有值
            List<Integer> countList = new ArrayList<>();
            for (String date : dateList) {
                countList.add(dateCountMap.getOrDefault(date, 0));
            }

            // 5. 构建响应
            Map<String, Object> data = new HashMap<>();
            data.put("dates", dateList);
            data.put("counts", countList);

            response.put("success", true);
            response.put("data", data);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取借阅趋势失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取热门图书TOP5
     * GET /api/stats/top-books
     * 返回：{success: true, data: [{rank: 1, bookId: 1, bookName: "xxx", author: "xxx", publisher: "xxx", borrowCount: 10}, ...]}
     */
    @GetMapping("/top-books")
    public ResponseEntity<Map<String, Object>> getTopBooks() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Object[]> queryResults = borrowRecordRepository.findTop5PopularBooks();
            List<Map<String, Object>> bookList = new ArrayList<>();

            int rank = 1;
            for (Object[] row : queryResults) {
                Map<String, Object> book = new HashMap<>();
                book.put("rank", rank++);
                book.put("bookId", ((Number) row[0]).longValue());      // BOOK_ID
                book.put("bookName", row[1] != null ? row[1].toString() : "");  // BOOKNAME
                book.put("author", row[2] != null ? row[2].toString() : "");    // AUTHOR
                book.put("publisher", row[3] != null ? row[3].toString() : ""); // PUBLISHER
                book.put("borrowCount", ((Number) row[4]).intValue());  // borrow_count

                bookList.add(book);
            }

            response.put("success", true);
            response.put("data", bookList);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取热门图书失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取热门类别TOP3
     * GET /api/stats/top-categories
     * 返回：{success: true, data: [{categoryId: 1, categoryName: "xxx", borrowCount: 10}, ...]}
     */
    @GetMapping("/top-categories")
    public ResponseEntity<Map<String, Object>> getTopCategories() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Object[]> queryResults = borrowRecordRepository.findTop3PopularCategories();
            List<Map<String, Object>> categoryList = new ArrayList<>();

            for (Object[] row : queryResults) {
                Map<String, Object> category = new HashMap<>();
                category.put("categoryId", ((Number) row[0]).longValue());      // CATEGORY_ID
                category.put("categoryName", row[1] != null ? row[1].toString() : "");  // CATEGORY_NAME
                category.put("borrowCount", ((Number) row[2]).intValue());      // borrow_count

                categoryList.add(category);
            }

            response.put("success", true);
            response.put("data", categoryList);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取热门类别失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 生成过去7天的日期列表（不包含今天）
     * 例如：今天是2024-10-08，则返回["2024-10-01", "2024-10-02", ..., "2024-10-07"]
     */
    private List<String> generateLast7DaysExcludingToday() {
        List<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();

        // 从昨天开始往前推7天
        for (int i = 1; i <= 7; i++) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DATE, -i);
            dates.add(sdf.format(calendar.getTime()));
        }

        // 反转，使最早的日期在前
        Collections.reverse(dates);
        return dates;
    }

    /**
     * 测试接口，用于验证Controller是否正常工作
     * GET /api/stats/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "StatsController is working!");
        response.put("timestamp", new Date());
        return ResponseEntity.ok(response);
    }
}