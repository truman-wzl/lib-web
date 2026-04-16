package com.example.libweb.controller;

import com.example.libweb.entity.Book;
import com.example.libweb.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "http://localhost:8081")
public class BookController {

    @Autowired
    private BookService bookService;

    /**
     * 获取图书列表（分页）
     */
    @GetMapping
    public ResponseEntity<?> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Book> books = bookService.findAllBooks(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", books.getContent());
            response.put("totalPages", books.getTotalPages());
            response.put("currentPage", books.getNumber());
            response.put("totalElements", books.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取图书列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 搜索图书
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchBooks(
            @RequestParam(required = false) String bookname,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Book> books = bookService.searchBooks(bookname, author, categoryId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", books.getContent());
            response.put("totalPages", books.getTotalPages());
            response.put("currentPage", books.getNumber());
            response.put("totalElements", books.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "搜索图书失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取单个图书
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Long id) {
        try {
            Optional<Book> book = bookService.findById(id);

            if (book.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", book.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "图书不存在");
                return ResponseEntity.status(404).body(errorResponse);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取图书失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 创建或更新图书
     */
    @PostMapping
    public ResponseEntity<?> saveBook(@RequestBody Book book) {
        try {
            Book savedBook = bookService.saveBook(book);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "保存成功");
            response.put("data", savedBook);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 删除图书
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        try {
            bookService.deleteBook(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}