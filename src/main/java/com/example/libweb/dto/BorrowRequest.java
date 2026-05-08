package com.example.libweb.dto;

public class BorrowRequest {
    private Long userId;
    private Long bookId;
    public BorrowRequest() {}

    public BorrowRequest(Long userId, Long bookId) {
        this.userId = userId;
        this.bookId = bookId;
    }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public boolean isValid() {
        return userId != null && userId > 0 && bookId != null && bookId > 0;
    }
}