// 位置：src/main/java/com/example/libweb/dto/BorrowRequest.java
package com.example.libweb.dto;

public class BorrowRequest {
    private Long userId;
    private Long bookId;

    // 构造器
    public BorrowRequest() {}

    public BorrowRequest(Long userId, Long bookId) {
        this.userId = userId;
        this.bookId = bookId;
    }

    // Getter和Setter
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    // 可以添加数据验证
    public boolean isValid() {
        return userId != null && userId > 0 && bookId != null && bookId > 0;
    }
}