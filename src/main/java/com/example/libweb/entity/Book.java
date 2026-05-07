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

    @Column(name = "AUTHOR", nullable=false,length = 100)
    private String author;

    @Column(name = "PUBLISHER", nullable = false,length = 100)
    private String publisher;
    //一本书属于一个分类 (对应外键 FK_BOOK_CATEGORY)
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

    //Getter和Setter
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getBookname() { return bookname; }
    public void setBookname(String bookname) { this.bookname = bookname; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Integer getTotalNumber() { return totalNumber; }
    public void setTotalNumber(Integer totalNumber) { this.totalNumber = totalNumber; }
    public Integer getCanBorrow() { return canBorrow; }
    public void setCanBorrow(Integer canBorrow) { this.canBorrow = canBorrow; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
//Excel导入静态方法
//    public static Book fromExcelRow(String[] rowData, Category category) {
//        if (rowData == null || rowData.length < 6) {
//            throw new IllegalArgumentException("Excel行数据不完整，至少需要6列数据");
//        }
//        Book book = new Book();
//        try {
//            // 第0列书名
//            if (rowData[0] != null && !rowData[0].trim().isEmpty()) {
//                book.setBookname(rowData[0].trim());
//            } else {
//                throw new IllegalArgumentException("书名不能为空");
//            }
//
//            // 第1列作者
//            if (rowData[1] != null && !rowData[1].trim().isEmpty()) {
//                book.setAuthor(rowData[1].trim());
//            } else {
//                throw new IllegalArgumentException("作者不能为空");
//            }
//
//            // 第2列出版社
//            if (rowData[2] != null && !rowData[2].trim().isEmpty()) {
//                String publisher = rowData[2].trim();
//                if (publisher.length() > 100) {
//                    throw new IllegalArgumentException("出版社名称不能超过100个字符");
//                }
//                book.setPublisher(publisher);
//            } else {
//                throw new IllegalArgumentException("出版社不能为空");
//            }
//
//            // 第3列
//            if (category != null) {
//                book.setCategory(category);
//            } else {
//
//                throw new IllegalArgumentException("关联分类不能为空");
//            }
//
//            // 第4列总数
//            if (rowData[4] != null && !rowData[4].trim().isEmpty()) {
//                try {
//                    int totalNum = Integer.parseInt(rowData[4].trim());
//                    if (totalNum >= 0) {
//                        book.setTotalNumber(totalNum);
//                    } else {
//                        book.setTotalNumber(0);
//                    }
//                } catch (NumberFormatException e) {
//                    throw new IllegalArgumentException("总数量必须是整数");
//                }
//            } else {
//                book.setTotalNumber(0);
//            }
//
//            // 第5列可借数量
//            if (rowData[5] != null && !rowData[5].trim().isEmpty()) {
//                try {
//                    int canBorrowNum = Integer.parseInt(rowData[5].trim());
//                    if (canBorrowNum >= 0) {
//                        book.setCanBorrow(canBorrowNum);
//                    } else {
//                        book.setCanBorrow(0);
//                    }
//                } catch (NumberFormatException e) {
//                    throw new IllegalArgumentException("可借数量必须是整数");
//                }
//            } else {
//                book.setCanBorrow(0);
//            }
//            book.setCreateTime(new Date());
//        } catch (Exception e) {
//            throw new RuntimeException("创建图书对象失败: " + e.getMessage(), e);
//        }
//        return book;
//    }
}