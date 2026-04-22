package com.example.libweb.service;

import com.example.libweb.entity.Message;
import com.example.libweb.entity.Userdata;
import com.example.libweb.repository.MessageRepository;
import com.example.libweb.repository.UserdataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MessageService {
    // 添加这行：声明 logger
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private UserdataRepository userdataRepository;  // 需要获取用户邮箱

    @Autowired(required = false)
    private JavaMailSender mailSender;  // 注入邮件发送器

    @Value("${spring.mail.username:noreply@libsystem.com}")
    private String fromEmail;
    // 获取用户消息
    public Map<String, Object> getUserMessages(Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取用户所有消息
            List<Message> messages = messageRepository.findByUserIdOrderByCreateTimeDesc(userId);

            // 统计未读消息
            long unreadCount = messageRepository.countByUserIdAndStatus(userId, "UNREAD");

            result.put("success", true);
            result.put("messages", messages);
            result.put("unreadCount", unreadCount);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取消息失败: " + e.getMessage());
        }

        return result;
    }

    // 标记消息为已读
    @Transactional
    public Map<String, Object> markAsRead(Long messageId, Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            int updated = messageRepository.markAsRead(messageId, userId, new Date());

            if (updated > 0) {
                result.put("success", true);
                result.put("message", "消息已标记为已读");
            } else {
                result.put("success", false);
                result.put("message", "消息不存在或权限不足");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "标记已读失败: " + e.getMessage());
        }

        return result;
    }

    // 标记所有消息为已读
    @Transactional
    public Map<String, Object> markAllAsRead(Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            int updated = messageRepository.markAllAsRead(userId, new Date());

            result.put("success", true);
            result.put("message", String.format("已标记%d条消息为已读", updated));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "标记全部已读失败: " + e.getMessage());
        }

        return result;
    }

    // 删除消息
    @Transactional
    public Map<String, Object> deleteMessage(Long messageId, Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            int deleted = messageRepository.deleteByUserIdAndMessageId(messageId, userId);

            if (deleted > 0) {
                result.put("success", true);
                result.put("message", "消息已删除");
            } else {
                result.put("success", false);
                result.put("message", "消息不存在或权限不足");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除消息失败: " + e.getMessage());
        }

        return result;
    }

    // 发送消息
//    public Message sendMessage(Message message) {
//        return messageRepository.save(message);
//    }
    // MessageService.java 中添加
    public Message sendOverdueMessage(Long userId, Long borrowId, String bookName,
                                      Date borrowTime, Date dueTime, String bookId) {
        try {
            // 1. 检查是否已存在该借阅的逾期消息
            List<Message> existingMessages = messageRepository.findOverdueMessageByBorrowId(borrowId);
            Message message = null;

            int currentReminder = 1;  // 当前是第几次提醒
            Integer remainingCount = 0;  // 剩余提醒次数

            if (existingMessages != null && !existingMessages.isEmpty()) {
                // 2. 已存在逾期消息，获取最新的一个
                message = existingMessages.get(0);

                // 3. 检查提醒次数
                remainingCount = message.getRemindCount();
                if (remainingCount == null || remainingCount <= 0) {
                    // 提醒次数已用完，不发送
                    logger.info("借阅记录 {} 的逾期提醒次数已用完，不再发送", borrowId);
                    return null;
                }

                // 4. 计算这是第几次提醒
                currentReminder = 3 - (remainingCount - 1);

                // 5. 减少提醒次数
                int newRemainingCount = remainingCount - 1;
                message.setRemindCount(newRemainingCount);
                remainingCount = newRemainingCount;

                // 6. 更新消息内容和时间
                String newContent = updateReminderContent(bookName, borrowTime, dueTime,
                        bookId, newRemainingCount, currentReminder);
                message.setContent(newContent);
                message.setCreateTime(new Date());
                message.setStatus(Message.STATUS_UNREAD);

            } else {
                // 7. 创建新的逾期消息
                currentReminder = 1;
                remainingCount = 2;  // 第一次提醒后剩余2次

                String content = String.format(
                        "您借阅的图书《%s》已逾期！(第1次提醒)\n\n" +
                                "📅 借阅日期：%tF\n" +
                                "⏰ 应还日期：%tF\n" +
                                "🔖 图书编号：%s\n" +
                                "⏳ 剩余提醒次数：%d次\n\n" +
                                "请尽快到图书馆办理还书手续，以免产生更多逾期费用。",
                        bookName, borrowTime, dueTime, bookId, remainingCount
                );

                message = new Message();
                message.setUserId(userId);
                message.setBorrowId(borrowId);
                message.setTitle("📚 图书逾期提醒");
                message.setContent(content);
                message.setMsgType(Message.TYPE_OVERDUE);
                message.setStatus(Message.STATUS_UNREAD);
                message.setRemindCount(remainingCount);
                message.setCreateTime(new Date());
            }

            // 8. 保存消息
            Message savedMessage = messageRepository.save(message);

            // 9. 发送邮件
            sendOverdueReminderEmail(userId, bookName, dueTime, remainingCount, currentReminder);

            return savedMessage;

        } catch (Exception e) {
            logger.error("发送逾期消息失败: 借阅ID={}, 错误: {}", borrowId, e.getMessage(), e);
            return null;
        }
    }

    public Message sendAchievementMessage(Long userId, String achievementName,
                                          String achievementDescription) {
        Message message = Message.createAchievementMessage(userId, achievementName,
                achievementDescription);
        message.setRemindCount(1);  // 成就消息只提醒1次
        return messageRepository.save(message);
    }
    // 获取用户未读消息数量
    public long getUnreadCount(Long userId) {
        return messageRepository.countByUserIdAndStatus(userId, "UNREAD");
    }
    private String updateReminderContent(String bookName, Date borrowTime,
                                         Date dueTime, String bookId,
                                         int remainingCount, int currentReminder) {
        return String.format(
                "您借阅的图书《%s》已逾期！(第%d次提醒)\n\n" +
                        "📅 借阅日期：%tF\n" +
                        "⏰ 应还日期：%tF\n" +
                        "🔖 图书编号：%s\n" +
                        "⏳ 剩余提醒次数：%d次\n\n" +
                        "请尽快到图书馆办理还书手续，以免产生更多逾期费用。",
                bookName, currentReminder, borrowTime, dueTime, bookId, remainingCount
        );
    }
    /**
     * 发送逾期提醒邮件
     */
    private void sendOverdueReminderEmail(Long userId, String bookName,
                                          Date dueTime, Integer remainingCount,
                                          int currentReminder) {
        try {
            if (mailSender == null) {
                logger.warn("邮件服务未配置，跳过邮件发送");
                return;
            }

            // 1. 获取用户信息
            Userdata user = userdataRepository.findById(userId).orElse(null);
            if (user == null) {
                logger.warn("用户不存在，用户ID: {}", userId);
                return;
            }

            // 2. 检查用户是否有邮箱
            String userEmail = user.getEmail();
            if (userEmail == null || userEmail.trim().isEmpty()) {
                logger.warn("用户 {} 没有邮箱，无法发送逾期提醒邮件", user.getUsername());
                return;
            }

            // 3. 计算逾期天数
            long overdueDays = 0;
            if (dueTime != null) {
                Date now = new Date();
                overdueDays = (now.getTime() - dueTime.getTime()) / (1000 * 60 * 60 * 24);
                if (overdueDays < 0) overdueDays = 0;
            }

            // 4. 构建邮件内容
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(userEmail);
            mailMessage.setSubject(String.format("【图书管理系统】图书逾期提醒（第%d次提醒）", currentReminder));

            String emailContent = String.format(
                    "亲爱的 %s 用户：\n\n" +
                            "您借阅的图书《%s》已逾期%s%s！\n\n" +
                            "📅 应还日期：%tF\n" +
                            "📅 今天日期：%tF\n" +
                            "⏰ 逾期天数：%d 天\n" +
                            "🔔 当前提醒：第%d次提醒\n" +
                            "⏳ 剩余提醒次数：%d 次\n\n" +
                            "请尽快登录系统处理，或前往图书馆办理还书手续。\n\n" +
                            "逾期费用计算：\n" +
                            "• 普通图书：每天 0.5 元\n" +
                            "• 热门图书：每天 1.0 元\n\n" +
                            "如需帮助，请联系图书馆管理员。\n\n" +
                            "此致\n" +
                            "图书管理系统\n" +
                            "%tF %tT\n\n" +
                            "（此为系统自动发送的邮件，请勿直接回复）",
                    user.getUsername(),
                    bookName,
                    overdueDays > 0 ? String.format("，已逾期 %d 天", overdueDays) : "",
                    overdueDays > 0 ? "，可能会产生逾期费用" : "，请尽快归还",
                    dueTime,
                    new Date(),
                    overdueDays,
                    currentReminder,
                    remainingCount != null ? remainingCount : 0,
                    new Date(), new Date()
            );

            mailMessage.setText(emailContent);

            // 5. 发送邮件
            mailSender.send(mailMessage);

            logger.info("✅ 已发送第{}次逾期提醒邮件到: {}, 图书: {}",
                    currentReminder, userEmail, bookName);

        } catch (Exception e) {
            logger.error("❌ 发送逾期提醒邮件失败: {}", e.getMessage());
            // 邮件发送失败不影响系统运行
        }
    }
}