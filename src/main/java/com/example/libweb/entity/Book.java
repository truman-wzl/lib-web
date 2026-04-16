package com.example.libweb.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "BOOK")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_book")
    @SequenceGenerator(name = "seq_book", sequenceName = "seq_book_id", allocationSize = 1)
    @Column(name = "BOOK_ID")
    private Long bookId;

    @Column(name = "BOOKNAME", nullable = false, length = 120)
    private String bookname;

    @Column(name = "AUTHOR", length = 100)
    private String author;

    @Column(name = "PUBLISHER", length = 100)
    private String publisher;

    // 核心关联：一本书属于一个分类 (对应外键 FK_BOOK_CATEGORY)
    @ManyToOne
    @JoinColumn(name = "CATEGORY_ID", foreignKey = @ForeignKey(name = "FK_BOOK_CATEGORY"))
    private Category category;

    @Column(name = "TOTAL_NUMBER")
    private Integer totalNumber = 0;

    @Column(name = "CAN_BORROW")
    private Integer canBorrow = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATE_TIME")
    private Date createTime = new Date();

    // 构造器
    public Book() {}

    // Getter 和 Setter (务必生成Category的getter/setter)
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getBookname() { return bookname; }
    public void setBookname(String bookname) { this.bookname = bookname; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public Category getCategory() { return category; } // 重要
    public void setCategory(Category category) { this.category = category; } // 重要
    public Integer getTotalNumber() { return totalNumber; }
    public void setTotalNumber(Integer totalNumber) { this.totalNumber = totalNumber; }
    public Integer getCanBorrow() { return canBorrow; }
    public void setCanBorrow(Integer canBorrow) { this.canBorrow = canBorrow; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}