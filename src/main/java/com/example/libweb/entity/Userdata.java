package com.example.libweb.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "userdata")
public class Userdata {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_userdata")
    @SequenceGenerator(name = "seq_userdata", sequenceName = "seq_userdata_id", allocationSize = 1)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 14)
    private String password;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(name = "role", length = 20)
    private String role = "USER";

    @Column(name = "email", length = 64)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "status", length = 10)
    private String status = "ACTIVE";

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time")
    private Date createTime = new Date();

    @Column(name = "LAST_LOGIN_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLoginTime;  // 最后登录时间，默认为null

    public Userdata() {}

    public Userdata(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getLastLoginTime() {return lastLoginTime;}
    public void setLastLoginTime(Date lastLoginTime) {this.lastLoginTime = lastLoginTime;}
}