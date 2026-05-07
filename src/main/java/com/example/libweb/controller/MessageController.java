package com.example.libweb.controller;

import com.example.libweb.entity.Message;
import com.example.libweb.entity.Userdata;
import com.example.libweb.service.MessageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 获取当前用户的消息列表
     * GET /api/messages/my-messages
     */
    @GetMapping("/my-messages")
    public Map<String, Object> getMyMessages(HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "用户未登录，请先登录");
            return error;
        }
        return messageService.getUserMessages(userId);
    }

    /**
     * 标记消息为已读
     * POST /api/messages/{id}/read
     */
    @PostMapping("/{id}/read")
    public Map<String, Object> markAsRead(@PathVariable Long id, HttpSession session) {
        Long userId = getCurrentUserId(session);

        if (userId == null) {
            return createErrorResponse("用户未登录");
        }

        return messageService.markAsRead(id, userId);
    }

    /**
     * 全部标记为已读
     * POST /api/messages/mark-all-read
     */
    @PostMapping("/mark-all-read")
    public Map<String, Object> markAllAsRead(HttpSession session) {
        Long userId = getCurrentUserId(session);

        if (userId == null) {
            return createErrorResponse("用户未登录");
        }

        return messageService.markAllAsRead(userId);
    }

    /**
     * 删除消息
     * DELETE /api/messages/{id}
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteMessage(@PathVariable Long id, HttpSession session) {
        Long userId = getCurrentUserId(session);

        if (userId == null) {
            return createErrorResponse("用户未登录");
        }

        return messageService.deleteMessage(id, userId);
    }

    /**
     * 获取未读消息数量
     * GET /api/messages/unread-count
     */
    @GetMapping("/unread-count")
    public Map<String, Object> getUnreadCount(HttpSession session) {
        Long userId = getCurrentUserId(session);

        if (userId == null) {
            return createErrorResponse("用户未登录");
        }

        Map<String, Object> result = new HashMap<>();
        long unreadCount = messageService.getUnreadCount(userId);

        result.put("success", true);
        result.put("unreadCount", unreadCount);

        return result;
    }

//    @PostMapping("/send-test")
//    public Map<String, Object> sendTestMessage(HttpSession session) {
//        Long userId = getCurrentUserId(session);
//
//        if (userId == null) {
//            return createErrorResponse("用户未登录");
//        }
//        Message message = new Message();
//        message.setUserId(userId);
//        message.setTitle("测试消息");
//        message.setContent("这是一条测试消息，用于测试消息系统功能。\n\n发送时间：" + new Date());
//        message.setMsgType(Message.TYPE_ACHIEVEMENT);
//        message.setStatus(Message.STATUS_UNREAD);
//        message.setCreateTime(new Date());
//
//        Message savedMessage = messageService.sendMessage(message);
//
//        Map<String, Object> result = new HashMap<>();
//        result.put("success", true);
//        result.put("message", "测试消息发送成功");
//        result.put("data", savedMessage);
//
//        return result;
//    }

    // MessageController.java
    private Long getCurrentUserId(HttpSession session) {
        Userdata currentUser = (Userdata) session.getAttribute("loginUser");
        if (currentUser != null) {
            return currentUser.getUserId();
        }
        if (currentUser != null) {
            try {
                java.lang.reflect.Method method = currentUser.getClass().getMethod("getUserId");
                Object result = method.invoke(currentUser);
                if (result instanceof Long) {
                    return (Long) result;
                } else if (result instanceof Integer) {
                    return ((Integer) result).longValue();
                }
            } catch (Exception e) {
                try {
                    java.lang.reflect.Method method = currentUser.getClass().getMethod("getId");
                    Object result = method.invoke(currentUser);
                    if (result instanceof Long) {
                        return (Long) result;
                    } else if (result instanceof Integer) {
                        return ((Integer) result).longValue();
                    }
                } catch (Exception ex) {
                    // 什么都不做
                }
            }
        }
        return null;
    }

    // 创建错误响应
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
    @GetMapping("/my-messages-test")
    public Map<String, Object> getMyMessagesTest() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> messages = new ArrayList<>();

            Calendar calendar = Calendar.getInstance();
            Map<String, Object> msg1 = new HashMap<>();
            msg1.put("id", 1L);
            msg1.put("userId", 1L);
            msg1.put("borrowId", 1001L);
            msg1.put("title", " 图书逾期提醒");
            msg1.put("content", "您借阅的《Java编程思想》已逾期3天！\n\n 借阅日期：2024-01-10\n 应还日期：2024-02-10\n 图书编号：BK001\n\n请尽快到图书馆办理还书手续，以免产生更多逾期费用。");
            msg1.put("msgType", "OVERDUE");
            msg1.put("status", "UNREAD");

            calendar.setTime(new Date());
            calendar.add(Calendar.HOUR, -1);
            msg1.put("createTime", calendar.getTime());
            messages.add(msg1);

            Map<String, Object> msg2 = new HashMap<>();
            msg2.put("id", 2L);
            msg2.put("userId", 1L);
            msg2.put("borrowId", 1002L);
            msg2.put("title", " 借阅成功");
            msg2.put("content", "您已成功借阅《Spring Boot实战》\n\n 借阅日期：2024-01-20\n 应还日期：2024-02-20\n 图书编号：BK002\n\n请妥善保管图书，按时归还。");
            msg2.put("msgType", "BORROW_SUCCESS");
            msg2.put("status", "READ");

            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            msg2.put("createTime", calendar.getTime());
            messages.add(msg2);

            Map<String, Object> msg3 = new HashMap<>();
            msg3.put("id", 3L);
            msg3.put("userId", 1L);
            msg3.put("borrowId", 1003L);
            msg3.put("title", "归还成功");
            msg3.put("content", "您已成功归还《MySQL必知必会》\n\n 归还日期：2024-01-15\n 图书编号：BK003\n\n感谢您的使用，欢迎再次借阅！");
            msg3.put("msgType", "RETURN_SUCCESS");
            msg3.put("status", "READ");

            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, -2);
            msg3.put("createTime", calendar.getTime());
            messages.add(msg3);

            result.put("success", true);
            result.put("messages", messages);
            result.put("unreadCount", 1L);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取消息失败: " + e.getMessage());
        }

        return result;
    }
}