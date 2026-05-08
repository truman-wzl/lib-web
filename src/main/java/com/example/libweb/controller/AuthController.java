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
            validateRegisterInput(user);
            Userdata registeredUser = userService.register(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "注册成功");
            response.put("data", buildUserResponse(registeredUser));

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

            Userdata user = userService.login(username, password);
            Date lastLoginTime = new Date();// 更新最后登录时间
            userdataRepository.updateLastLoginTime(user.getUserId(), lastLoginTime);
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
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

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
            response.put("data", buildUserResponse(updatedUser));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/update-info")
    public ResponseEntity<?> updateBasicInfo(@RequestBody Map<String, String> data) {
        try {
            String realName = data.get("realName");
            String email = data.get("email");
            String phone = data.get("phone");
            // 至少提供一个字段
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
            response.put("data", buildUserResponse(updatedUser));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    // 存储已验证的邮箱，用于忘记密码流程
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
            // 防止枚举攻击：即使用户不存在也返回成功
            Optional<Userdata> userOpt = userdataRepository.findByUsernameAndEmail(username, email);
            if (!userOpt.isPresent()) {
                return ResponseEntity.ok().body(Map.of(
                        "success", true,
                        "message", "如果用户名和邮箱匹配，验证码将发送到您的邮箱"
                ));
            }

            try {
                emailCodeService.sendCode(email, username);

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

            Optional<Userdata> userOpt = userdataRepository.findByEmail(email);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "该邮箱未注册"
                ));
            }

            boolean isValid = emailCodeService.verifyCode(email,username,code);
            if (!isValid) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "验证码错误或已过期"
                ));
            }

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

            if (newPassword.length() < 7) {
                throw new RuntimeException("密码长度至少7位");
            }

            if (!newPassword.equals(confirmPassword)) {
                throw new RuntimeException("两次输入的密码不一致");
            }

            String verificationKey = email + ":" + username;
            LocalDateTime expireTime = verifiedEmails.get(verificationKey);
            if (expireTime == null) {
                throw new RuntimeException("请先完成验证码验证");
            }

            if (expireTime.isBefore(LocalDateTime.now())) {
                verifiedEmails.remove(verificationKey);
                throw new RuntimeException("验证已过期，请重新获取验证码");
            }

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

    private void validateRegisterInput(Userdata user) {
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
    }

    private Map<String, Object> buildUserResponse(Userdata user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUserId());
        userData.put("username", user.getUsername());
        userData.put("realName", user.getRealName());
        userData.put("email", user.getEmail());
        userData.put("phone", user.getPhone());
        userData.put("role", user.getRole());
        userData.put("createTime", user.getCreateTime());
        return userData;
    }
}
