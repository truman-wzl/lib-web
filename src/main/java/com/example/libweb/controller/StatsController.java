package com.example.libweb.controller;

import com.example.libweb.repository.BorrowRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class StatsController {

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @GetMapping("/borrow-trend")
    public ResponseEntity<Map<String, Object>> getBorrowTrend() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<String> dateList = generateLast7DaysExcludingToday();
            List<Object[]> queryResults = borrowRecordRepository.findBorrowTrendLast7Days();

            Map<String, Integer> dateCountMap = new HashMap<>();
            for (Object[] row : queryResults) {
                String dateStr = (String) row[0];
                Number count = (Number) row[1];
                dateCountMap.put(dateStr, count.intValue());
            }

            List<Integer> countList = new ArrayList<>();
            for (String date : dateList) {
                countList.add(dateCountMap.getOrDefault(date, 0));
            }

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
                book.put("bookId", ((Number) row[0]).longValue());
                book.put("bookName", row[1] != null ? row[1].toString() : "");
                book.put("author", row[2] != null ? row[2].toString() : "");
                book.put("publisher", row[3] != null ? row[3].toString() : "");
                book.put("borrowCount", ((Number) row[4]).intValue());

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

    @GetMapping("/top-categories")
    public ResponseEntity<Map<String, Object>> getTopCategories() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Object[]> queryResults = borrowRecordRepository.findTop3PopularCategories();
            List<Map<String, Object>> categoryList = new ArrayList<>();

            for (Object[] row : queryResults) {
                Map<String, Object> category = new HashMap<>();
                category.put("categoryId", ((Number) row[0]).longValue());
                category.put("categoryName", row[1] != null ? row[1].toString() : "");
                category.put("borrowCount", ((Number) row[2]).intValue());

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

    private List<String> generateLast7DaysExcludingToday() {
        List<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();

        for (int i = 1; i <= 7; i++) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DATE, -i);
            dates.add(sdf.format(calendar.getTime()));
        }

        Collections.reverse(dates);
        return dates;
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "StatsController is working!");
        response.put("timestamp", new Date());
        return ResponseEntity.ok(response);
    }
}
