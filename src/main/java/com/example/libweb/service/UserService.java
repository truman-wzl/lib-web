package com.example.libweb.service;

import com.example.libweb.entity.Userdata;

public interface UserService {
    Userdata register(Userdata user);
    Userdata login(String username, String password);
    Userdata getCurrentUserInfo();
    Userdata updateUserInfo(Userdata user);
    Userdata updateUsername(String newUsername);
    boolean updatePassword(String oldPassword, String newPassword);
}