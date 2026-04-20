package com.example.libweb.service;

import com.example.libweb.entity.Message;
import com.example.libweb.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

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
    public Message sendMessage(Message message) {
        return messageRepository.save(message);
    }
    // 发送逾期提醒
    public Message sendOverdueMessage(Long userId, Long borrowId, String bookName,
                                      Date borrowTime, Date dueTime, String bookCode) {
        Message message = Message.createOverdueMessage(userId, borrowId, bookName, borrowTime, dueTime, bookCode);
        return messageRepository.save(message);
    }
    // 获取用户未读消息数量
    public long getUnreadCount(Long userId) {
        return messageRepository.countByUserIdAndStatus(userId, "UNREAD");
    }
    // 添加成就消息方法：
    public Message sendAchievementMessage(Long userId, String achievementName, String achievementDescription) {
        Message message = Message.createAchievementMessage(userId, achievementName, achievementDescription);
        return messageRepository.save(message);
    }

    // 可选：如果需要关联借阅记录
    public Message sendAchievementMessage(Long userId, Long borrowId, String achievementName, String achievementDescription) {
        Message message = Message.createAchievementMessage(userId, borrowId, achievementName, achievementDescription);
        return messageRepository.save(message);
    }
    // MessageService.java
    public int getUnreadCountByUserId(long userId) {
        return Math.toIntExact(messageRepository.countByUserIdAndStatus(userId, Message.STATUS_UNREAD));
    }
}