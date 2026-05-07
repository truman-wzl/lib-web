package com.example.libweb.service;

import com.example.libweb.entity.Userdata;

public interface UserService {
    // 用户注册
    Userdata register(Userdata user);

    // 用户登录
    Userdata login(String username, String password);

    // 获取当前登录用户信息
    Userdata getCurrentUserInfo();

    // 更新用户信息
    Userdata updateUserInfo(Userdata user);
    // 修改用户名
    Userdata updateUsername(String newUsername);
    // 修改密码
    boolean updatePassword(String oldPassword, String newPassword);
}