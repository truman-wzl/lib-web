package com.example.libweb.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailCodeService {

    private static final Logger logger = LoggerFactory.getLogger(EmailCodeService.class);

    // 存储验证码：key=邮箱, value={code, expireTime, tryCount, lastSendTime}
    private final Map<String, EmailCode> codeStore = new ConcurrentHashMap<>();
    // 生成key的方法
    private String generateKey(String email, String username) {
        return email + ":" + username;  // 例如：123@qq.com:zhangsan
    }
    // 验证码有效时间（分钟）
    private static final int CODE_EXPIRE_MINUTES = 10;
    // 发送间隔（秒）
    private static final int SEND_INTERVAL_SECONDS = 60;
    // 最大尝试次数
    private static final int MAX_TRY_COUNT = 5;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    /**
     * 发送验证码到指定邮箱
     * @param email 邮箱地址
     *  username 用户名
     * @return 验证码（仅用于测试，生产环境不要返回）
     */
    public String sendCode(String email,String username) {
        try {
            // 检查邮箱格式
            if (email == null || email.trim().isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new RuntimeException("邮箱格式不正确");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new RuntimeException("用户名不能为空");
            }
            // 使用组合key
            String key = generateKey(email, username);
            // 检查发送频率
            EmailCode existingCode = codeStore.get(key);
            if (existingCode != null) {
                long secondsSinceLastSend = ChronoUnit.SECONDS.between(
                        existingCode.getLastSendTime(), LocalDateTime.now());
                if (secondsSinceLastSend < SEND_INTERVAL_SECONDS) {
                    throw new RuntimeException("请等待" + (SEND_INTERVAL_SECONDS - secondsSinceLastSend) + "秒后再试");
                }
            }

            // 生成6位数字验证码
            String code = String.format("%06d", new Random().nextInt(999999));
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expireTime = now.plusMinutes(CODE_EXPIRE_MINUTES);

            // 存储验证码，包括发送时间和过期时间
            codeStore.put(key, new EmailCode(code, expireTime, now,username,email));

            System.out.println("生成的验证码: " + code + " 已存储，邮箱: " + email + ", 过期时间: " + expireTime);

            // 发送邮件
            try {
                sendEmail(email, code,username);
                logger.info("邮件发送成功，邮箱: {}", email);
            } catch (Exception e) {
                // 如果邮件发送失败，从存储中移除验证码
                codeStore.remove(email);
                throw new RuntimeException("邮件发送失败: " + e.getMessage());
            }

            return code;
        } catch (RuntimeException e) {
            logger.error("发送验证码时发生运行时异常: {}", e.getMessage());
            throw e; // 重新抛出RuntimeException
        } catch (Exception e) {
            logger.error("发送验证码时发生错误", e);
            throw new RuntimeException("发送验证码时发生错误: " + e.getMessage());
        }
    }

    /**
     * 验证验证码
     * @param email 邮箱
     * @param code 用户输入的验证码
     * @return 是否验证成功
     */
    public boolean verifyCode(String email, String username, String code) {
        if (email == null || code == null || code.trim().length() != 6) {
            return false;
        }
        // 使用组合 key
        String key = (username == null || username.trim().isEmpty()) ? email : generateKey(email, username);

        EmailCode emailCode = codeStore.get(key);
        if (emailCode == null) {
            logger.warn("验证码不存在，邮箱: {}，用户名: {}", email, username);
            return false;
        }

        // 检查尝试次数
        if (emailCode.getTryCount() >= MAX_TRY_COUNT) {
            codeStore.remove(email);
            logger.warn("邮箱 {} 尝试次数过多", email);
            return false;
        }

        // 检查是否过期
        if (emailCode.getExpireTime().isBefore(LocalDateTime.now())) {
            codeStore.remove(email);
            logger.warn("邮箱 {} 的验证码已过期", email);
            return false;
        }

        // 验证验证码
        boolean isValid = emailCode.getCode().equals(code);
        if (isValid) {
            codeStore.remove(email); // 验证成功后移除验证码
            logger.info("邮箱 {} 验证码验证成功", email);
        } else {
            emailCode.incrementTryCount(); // 增加尝试次数
            logger.warn("邮箱 {} 验证码验证失败，尝试次数: {}", email, emailCode.getTryCount());
        }
        return isValid;
    }

    /**
     * 移除验证码（用于重置密码成功后清除）
     * @param email 邮箱
     */
    // 可以重载或修改
    public void removeCode(String email) {
        removeCode(email, null);
    }

    public void removeCode(String email, String username) {
        String key = (username == null || username.trim().isEmpty()) ? email : generateKey(email, username);
        codeStore.remove(key);
        logger.info("移除验证码，邮箱: {}，用户名: {}", email, username);
    }

    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 生成100000-999999之间的数字
        return String.valueOf(code);
    }

    /**
     * 发送邮件
     */
    private void sendEmail(String toEmail, String code,String username) {
        if (mailSender == null) {
            logger.warn("邮件服务未配置，无法发送验证码到: {}，验证码为: {}", toEmail, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("图书管理系统 - 密码重置验证码");
            message.setText(String.format(
                    "您的验证码是：%s\n验证码有效时间：%d分钟\n请勿将验证码泄露给他人。\n\n如果不是您本人操作，请忽略此邮件。",
                    code, CODE_EXPIRE_MINUTES
            ));

            mailSender.send(message);
            logger.info("验证码邮件已发送到: {}", toEmail);
        } catch (Exception e) {
            logger.error("发送验证码邮件失败，邮箱: {}", toEmail, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 内部类：存储验证码信息
     */
    private static class EmailCode {
        private final String code;
        private final LocalDateTime expireTime;
        private final LocalDateTime lastSendTime;
        private final String username;  // 新增字段
        private final String email;     // 新增字段
        private int tryCount = 0;

        public EmailCode(String code, LocalDateTime expireTime, LocalDateTime lastSendTime,String username, String email) {
            this.code = code;
            this.expireTime = expireTime;
            this.lastSendTime = lastSendTime;
            this.username = username;
            this.email = email;
        }

        public String getCode() { return code; }
        public LocalDateTime getExpireTime() { return expireTime; }
        public LocalDateTime getLastSendTime() { return lastSendTime; }
        public int getTryCount() { return tryCount; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public void incrementTryCount() { this.tryCount++; }
    }
}