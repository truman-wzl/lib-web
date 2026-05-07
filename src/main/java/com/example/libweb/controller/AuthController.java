package com.example.libweb.controller;

import com.example.libweb.entity.Userdata;
import com.example.libweb.repository.UserdataRepository;
import com.example.libweb.service.EmailCodeService;
import com.example.libweb.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

//处理与用户认证相关的所有HTTP请求，并提供标准的RESTful API接口
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private EmailCodeService emailCodeService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserdataRepository  userdataRepository;
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Userdata user) {
        try {
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                throw new RuntimeException("用户名不能为空");
            }
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                throw new RuntimeException("密码不能为空");
            }
            if (user.getUsername().length() < 3 || user.getUsername().length() > 50) {
                throw new RuntimeException("用户名长度必须在3-50个字符之间");
            }
            if (user.getPassword().length() < 6) {
                throw new RuntimeException("密码长度至少6位");
            }
            Userdata registeredUser = userService.register(user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "注册成功");
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", registeredUser.getUserId());
            userData.put("username", registeredUser.getUsername());
            userData.put("realName", registeredUser.getRealName());
            userData.put("email", registeredUser.getEmail());
            userData.put("phone", registeredUser.getPhone());
            userData.put("role", registeredUser.getRole());
            userData.put("createTime", registeredUser.getCreateTime());
            response.put("data", userData);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        try {
            String username = loginData.get("username");
            String password = loginData.get("password");
            if (username == null || username.trim().isEmpty()) {
                throw new RuntimeException("请输入用户名");
            }
            if (password == null || password.trim().isEmpty()) {
                throw new RuntimeException("请输入密码");
            }

            // 调用业务层服务，执行核心登录逻辑
            Userdata user = userService.login(username, password);



            Date lastLoginTime = new Date();
            userdataRepository.updateLastLoginTime(user.getUserId(), lastLoginTime);

            //设置到返回对象中
            user.setLastLoginTime(lastLoginTime);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("data", user);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(401).body(errorResponse);
        }
    }
    /**
     * 获取当前已登录用户的详细信息
     * @return 统一格式的响应实体，包含当前用户的数据
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Userdata currentUser = userService.getCurrentUserInfo();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(401).body(errorResponse); // 未登录返回401
        }
    }
    // 修改用户名
    @PostMapping("/update-username")
    public ResponseEntity<?> updateUsername(@RequestBody Map<String, String> data) {
        try {
            String newUsername = data.get("username");

            if (newUsername == null || newUsername.trim().isEmpty()) {
                throw new RuntimeException("用户名不能为空");
            }

            Userdata updatedUser = userService.updateUsername(newUsername);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户名修改成功");

            Map<String, Object> userData = new HashMap<>();
            userData.put("username", updatedUser.getUsername());
            userData.put("userId", updatedUser.getUserId());
            userData.put("email", updatedUser.getEmail());
            userData.put("phone", updatedUser.getPhone());
            userData.put("role", updatedUser.getRole());
            userData.put("createTime", updatedUser.getCreateTime());

            response.put("data", userData);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //修改用户基本信息
    @PostMapping("/update-info")
    public ResponseEntity<?> updateBasicInfo(@RequestBody Map<String, String> data, HttpSession session) {
        try {
            String realName = data.get("realName");
            String email = data.get("email");
            String phone = data.get("phone");

            if ((realName == null || realName.trim().isEmpty()) &&
                    (email == null || email.trim().isEmpty()) &&
                    (phone == null || phone.trim().isEmpty())) {
                throw new RuntimeException("至少提供一个字段进行更新");
            }

            Userdata updateInfo = new Userdata();
            if (realName != null && !realName.trim().isEmpty()) {
                updateInfo.setRealName(realName.trim());
            }
            if (email != null && !email.trim().isEmpty()) {
                updateInfo.setEmail(email.trim());
            }
            if (phone != null && !phone.trim().isEmpty()) {
                updateInfo.setPhone(phone.trim());
            }
            Userdata updatedUser = userService.updateUserInfo(updateInfo);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "信息更新成功");

            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", updatedUser.getUserId());
            userData.put("username", updatedUser.getUsername());
            userData.put("realName", updatedUser.getRealName());
            userData.put("email", updatedUser.getEmail());
            userData.put("phone", updatedUser.getPhone());
            userData.put("role", updatedUser.getRole());
            userData.put("createTime", updatedUser.getCreateTime());

            response.put("data", userData);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    private final Map<String, LocalDateTime> verifiedEmails = new ConcurrentHashMap<>();
    private static final int VERIFIED_EXPIRE_MINUTES = 10;
    @PostMapping("/forgot-password/send-code")
    public ResponseEntity<?> sendResetCode(@RequestBody Map<String, String> data) {
        try {
            String email = data.get("email");
            String username = data.get("username");

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "用户名不能为空"
                ));
            }

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "邮箱不能为空"
                ));
            }

            //验证用户名邮箱是否匹配
            Optional<Userdata> userOpt = userdataRepository.findByUsernameAndEmail(username, email);
            if (!userOpt.isPresent()) {
                return ResponseEntity.ok().body(Map.of(
                        "success", true,
                        "message", "如果用户名和邮箱匹配，验证码将发送到您的邮箱"
                ));
            }

            //发送验证码，同时传入用户名和邮箱
            try {
                String code = emailCodeService.sendCode(email, username);
                System.out.println("验证码发送成功，验证码: " + code + ", 邮箱: " + email + ", 用户: " + username);

                return ResponseEntity.ok().body(Map.of(
                        "success", true,
                        "message", "验证码已发送到邮箱，请查收"
                ));
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "message", "验证码发送失败: " + e.getMessage()
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "服务器内部错误: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/forgot-password/verify-code")
    public ResponseEntity<?> verifyResetCode(@RequestBody Map<String, String> data) {
        try {
            String email = data.get("email");
            String username=data.get("username");
            String code = data.get("code");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "邮箱不能为空"
                ));
            }

            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "验证码不能为空"
                ));
            }

            //验证邮箱是否注册
            Optional<Userdata> userOpt = userdataRepository.findByEmail(email);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "该邮箱未注册"
                ));
            }

            //验证验证码
            boolean isValid = emailCodeService.verifyCode(email,username,code);
            if (!isValid) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "验证码错误或已过期"
                ));
            }

            //验证成功
            String verificationKey = email + ":" + username;
            verifiedEmails.put(verificationKey, LocalDateTime.now().plusMinutes(VERIFIED_EXPIRE_MINUTES));

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "验证码验证成功"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "服务器内部错误"
            ));
        }
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> data) {
        try {
            String email = data.get("email");
            String username=data.get("username");
            String newPassword = data.get("newPassword");
            String confirmPassword = data.get("confirmPassword");

            if (email == null || email.trim().isEmpty()) {
                throw new RuntimeException("邮箱不能为空");
            }
            if (newPassword == null || newPassword.trim().isEmpty()) {
                throw new RuntimeException("新密码不能为空");
            }
            if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
                throw new RuntimeException("确认密码不能为空");
            }

            // 密码长度至少7位，避免与6位验证码混淆
            if (newPassword.length() < 7) {
                throw new RuntimeException("密码长度至少7位");
            }

            if (!newPassword.equals(confirmPassword)) {
                throw new RuntimeException("两次输入的密码不一致");
            }

            // 检查用户名和邮箱的组合是否已验证
            String verificationKey = email + ":" + username;
            LocalDateTime expireTime = verifiedEmails.get(verificationKey);
            if (expireTime == null) {
                throw new RuntimeException("请先完成验证码验证");
            }
            //检查验证是否过期
            if (expireTime.isBefore(LocalDateTime.now())) {
                verifiedEmails.remove(verificationKey);
                throw new RuntimeException("验证已过期，请重新获取验证码");
            }

            //验证用户名和邮箱是否匹配
            if (username == null || username.trim().isEmpty()) {
                throw new RuntimeException("用户名不能为空");
            }

            Optional<Userdata> userOpt = userdataRepository.findByUsernameAndEmail(username, email);
            if (!userOpt.isPresent()) {
                throw new RuntimeException("用户名和邮箱不匹配");
            }
            Userdata user = userOpt.get();

            if (newPassword.equals(user.getPassword())) {
                throw new RuntimeException("新密码不能与旧密码相同");
            }

            user.setPassword(newPassword);
            userdataRepository.save(user);

            verifiedEmails.remove(email);

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "密码重置成功，请使用新密码登录"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "服务器内部错误"
            ));
        }
    }
}

