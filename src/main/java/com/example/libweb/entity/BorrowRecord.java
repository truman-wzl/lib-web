package com.example.libweb.entity;

import jakarta.persistence.*;
import java.util.Date;
import java.time.LocalDateTime;

@Entity
@Table(name = "BORROW_RECORD")
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "borrow_seq")
    @SequenceGenerator(name = "borrow_seq", sequenceName = "SEQ_RECORD_ID", allocationSize = 1)
    @Column(name = "RECORD_ID")
    private Long recordId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "BOOK_ID", nullable = false)
    private Long bookId;

    @Column(name = "BORROW_TIME", nullable = false)
    private LocalDateTime borrowTime;

    @Column(name = "DUE_TIME", nullable = false)
    private LocalDateTime dueTime;

    @Column(name = "RETURN_TIME")
    private LocalDateTime returnTime;



    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "CREATE_TIME", nullable = false)
    private LocalDateTime createTime;

    public BorrowRecord() {
        this.borrowTime = LocalDateTime.now();
        this.createTime = LocalDateTime.now();
        this.status = "BORROWED";
    }

    public BorrowRecord(Long userId, Long bookId,LocalDateTime dueTime) {
        this();
        this.userId = userId;
        this.bookId = bookId;
        this.dueTime = dueTime;
    }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public LocalDateTime getBorrowTime() { return borrowTime; }
    public void setBorrowTime(LocalDateTime borrowTime) { this.borrowTime = borrowTime; }

    public LocalDateTime getDueTime() { return dueTime; }
    public void setDueTime(LocalDateTime dueTime) { this.dueTime = dueTime; }

    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "BorrowRecord{" +
                "recordId=" + recordId +
                ", userId=" + userId +
                ", bookId=" + bookId +
                ", borrowTime=" + borrowTime +
                ", dueTime=" + dueTime +
                ", returnTime=" + returnTime +
                ", status='" + status + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}