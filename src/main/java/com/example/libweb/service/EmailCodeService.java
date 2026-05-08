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
    private final Map<String, EmailCode> codeStore = new ConcurrentHashMap<>();
    private String generateKey(String email, String username) {
        return email + ":" + username;
    }
    private static final int CODE_EXPIRE_MINUTES = 10;
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final int MAX_TRY_COUNT = 5;
    @Autowired(required = false)
    private JavaMailSender mailSender;
    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    //发送验证码到指定邮箱
    public String sendCode(String email,String username) {
        try {
            if (email == null || email.trim().isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new RuntimeException("邮箱格式不正确");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new RuntimeException("用户名不能为空");
            }
            String key = generateKey(email, username);
            EmailCode existingCode = codeStore.get(key);
            if (existingCode != null) {
                long secondsSinceLastSend = ChronoUnit.SECONDS.between(
                        existingCode.getLastSendTime(), LocalDateTime.now());
                if (secondsSinceLastSend < SEND_INTERVAL_SECONDS) {
                    throw new RuntimeException("请等待" + (SEND_INTERVAL_SECONDS - secondsSinceLastSend) + "秒后再试");
                }
            }
            String code = String.format("%06d", new Random().nextInt(999999));
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expireTime = now.plusMinutes(CODE_EXPIRE_MINUTES);
            codeStore.put(key, new EmailCode(code, expireTime, now,username,email));
            try {
                sendEmail(email, code,username);
                logger.info("邮件发送成功，邮箱: {}", email);
            } catch (Exception e) {
                codeStore.remove(key);
                throw new RuntimeException("邮件发送失败: " + e.getMessage());
            }

            return code;
        } catch (RuntimeException e) {
            logger.error("发送验证码时发生运行时异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("发送验证码时发生错误", e);
            throw new RuntimeException("发送验证码时发生错误: " + e.getMessage());
        }
    }
    public boolean verifyCode(String email, String username, String code) {
        if (email == null || code == null || code.trim().length() != 6) {
            return false;
        }
        String key = (username == null || username.trim().isEmpty()) ? email : generateKey(email, username);

        EmailCode emailCode = codeStore.get(key);
        if (emailCode == null) {
            logger.warn("验证码不存在，邮箱: {}，用户名: {}", email, username);
            return false;
        }
        if (emailCode.getTryCount() >= MAX_TRY_COUNT) {
            codeStore.remove(key);
            logger.warn("邮箱 {} 尝试次数过多", email);
            return false;
        }
        if (emailCode.getExpireTime().isBefore(LocalDateTime.now())) {
            codeStore.remove(key);
            logger.warn("邮箱 {} 的验证码已过期", email);
            return false;
        }
        boolean isValid = emailCode.getCode().equals(code);
        if (isValid) {
            codeStore.remove(key);
            logger.info("邮箱 {} 验证码验证成功", email);
        } else {
            emailCode.incrementTryCount();
            logger.warn("邮箱 {} 验证码验证失败，尝试次数: {}", email, emailCode.getTryCount());
        }
        return isValid;
    }
    public void removeCode(String email) {
        removeCode(email, null);
    }

    public void removeCode(String email, String username) {
        String key = (username == null || username.trim().isEmpty()) ? email : generateKey(email, username);
        codeStore.remove(key);
        logger.info("移除验证码，邮箱: {}，用户名: {}", email, username);
    }
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
    private static class EmailCode {
        private final String code;
        private final LocalDateTime expireTime;
        private final LocalDateTime lastSendTime;
        private final String username;
        private final String email;
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