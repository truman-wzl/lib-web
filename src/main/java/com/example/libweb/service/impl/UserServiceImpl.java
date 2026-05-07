package com.example.libweb.service.impl;

import com.example.libweb.entity.Userdata;
import com.example.libweb.repository.UserdataRepository;
import com.example.libweb.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserdataRepository userRepository;

    @Autowired
    private HttpSession session;

    @Override
    @Transactional
    public Userdata register(Userdata user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (user.getUsername().length() < 3 || user.getUsername().length() > 50) {
            throw new RuntimeException("用户名长度必须在3-50个字符之间");
        }
        if (user.getPassword().length() < 6) {
            throw new RuntimeException("密码长度至少6位");
        }

        long userCount = userRepository.count();
        if (userCount == 0) {
            user.setRole("ADMIN"); //第一个注册的用户自动成为管理员
        } else {
            user.setRole("USER");  //其他用户为普通用户
        }
        user.setStatus("ACTIVE");

        if (user.getCreateTime() == null) {
            user.setCreateTime(new Date());
        }

        //保存用户
        return userRepository.save(user);
    }

    @Override
    public Userdata login(String username, String password) {

        Optional<Userdata> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("用户名或密码错误");
        }
        Userdata user = userOpt.get();

        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if ("LOCKED".equals(user.getStatus())) {
            throw new RuntimeException("账户已被锁定，请联系管理员");
        }
        Userdata returnUser = new Userdata();
        returnUser.setUserId(user.getUserId());
        returnUser.setUsername(user.getUsername());
        returnUser.setRealName(user.getRealName());
        returnUser.setRole(user.getRole());
        returnUser.setEmail(user.getEmail());
        returnUser.setPhone(user.getPhone());
        returnUser.setStatus(user.getStatus());
        returnUser.setCreateTime(user.getCreateTime());

        session.setAttribute("loginUser", returnUser);
        return returnUser;
    }

    @Override
    public Userdata getCurrentUserInfo() {
        Userdata user = (Userdata) session.getAttribute("loginUser");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        return user;
    }
    //更新用户信息
    @Override
    @Transactional
    public Userdata updateUserInfo(Userdata updateInfo) {
        Userdata currentUser = getCurrentUserInfo();

        // 从数据库获取完整用户信息
        Userdata dbUser = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 只更新允许修改的字段
        if (updateInfo.getRealName() != null) {
            dbUser.setRealName(updateInfo.getRealName());
        }
        if (updateInfo.getEmail() != null) {

            if (!updateInfo.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new RuntimeException("邮箱格式不正确");
            }
            dbUser.setEmail(updateInfo.getEmail());
        }
        if (updateInfo.getPhone() != null) {
            if (!updateInfo.getPhone().matches("^1[3-9]\\d{9}$")) {
                throw new RuntimeException("手机号码格式不正确");
            }
            dbUser.setPhone(updateInfo.getPhone());
        }
        Userdata sessionUser = (Userdata) session.getAttribute("loginUser");
        if (sessionUser != null) {
            if (updateInfo.getEmail() != null) {
                sessionUser.setEmail(updateInfo.getEmail());
            }
            if (updateInfo.getPhone() != null) {
                sessionUser.setPhone(updateInfo.getPhone());
            }
            session.setAttribute("loginUser", sessionUser);
        }
        return userRepository.save(dbUser);
    }

    //修改密码
    @Override
    @Transactional
    public boolean updatePassword(String oldPassword, String newPassword) {
        Userdata currentUser = getCurrentUserInfo();

        Userdata dbUser = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!oldPassword.equals(dbUser.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        dbUser.setPassword(newPassword);
        userRepository.save(dbUser);

        return true;
    }
    //修改用户名
    @Override
    @Transactional
    public Userdata updateUsername(String newUsername) {
        //获取session用户用于验证
        Userdata sessionUser = getCurrentUserInfo();

        //检查用户名是否与当前相同
        if (newUsername.equals(sessionUser.getUsername())) {
            throw new RuntimeException("新用户名不能与当前用户名相同");
        }

        //检查用户名是否已存在
        Optional<Userdata> existingUser = userRepository.findByUsername(newUsername);
        if (existingUser.isPresent() && !existingUser.get().getUserId().equals(sessionUser.getUserId())) {
            throw new RuntimeException("用户名已存在");
        }

        //从数据库获取实体进行更新
        Userdata dbUser = userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 更新数据库实体
        dbUser.setUsername(newUsername);
        Userdata updatedUser = userRepository.save(dbUser);

        // 更新session
        sessionUser.setUsername(newUsername);
        session.setAttribute("loginUser", sessionUser);

        return updatedUser;
    }
}