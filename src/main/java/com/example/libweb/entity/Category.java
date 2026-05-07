package com.example.libweb.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "CATEGORY")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_category")
    @SequenceGenerator(name = "seq_category", sequenceName = "seq_category_id", allocationSize = 1)
    @Column(name = "CATEGORY_ID")
    private Long categoryId;

    @Column(name = "CATEGORY_NAME", nullable = false, length = 50)
    private String categoryName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATE_TIME")
    private Date createTime = new Date();
    //映射数据库中的IS_PROTECTED字段
    @Column(name = "IS_PROTECTED")
    private Boolean isProtected = false;  // 默认false
    public Category() {}

    public Category(String categoryName) {
        this.categoryName = categoryName;
    }

    //Getter和Setter
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Boolean getIsProtected() {return isProtected;}
    public void setIsProtected(Boolean isProtected) {this.isProtected = isProtected;}
}