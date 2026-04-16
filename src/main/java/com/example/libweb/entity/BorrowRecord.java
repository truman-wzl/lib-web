package com.example.libweb.entity;

import jakarta.persistence.*;
import java.util.Date;

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
    @Temporal(TemporalType.DATE)
    private Date borrowTime;

    @Column(name = "DUE_TIME", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dueTime;

    @Column(name = "RETURN_TIME")
    @Temporal(TemporalType.DATE)
    private Date returnTime;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "CREATE_TIME", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date createTime;

    // 构造器
    public BorrowRecord() {
        this.borrowTime = new Date();
        this.createTime = new Date();
        this.status = "BORROWED";
    }

    public BorrowRecord(Long userId, Long bookId, Date dueTime) {
        this();
        this.userId = userId;
        this.bookId = bookId;
        this.dueTime = dueTime;
    }

    // Getter 和 Setter
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public Date getBorrowTime() { return borrowTime; }
    public void setBorrowTime(Date borrowTime) { this.borrowTime = borrowTime; }

    public Date getDueTime() { return dueTime; }
    public void setDueTime(Date dueTime) { this.dueTime = dueTime; }

    public Date getReturnTime() { return returnTime; }
    public void setReturnTime(Date returnTime) { this.returnTime = returnTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

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