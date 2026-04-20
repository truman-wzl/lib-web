package com.example.libweb.repository;

import com.example.libweb.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 根据用户ID查找消息
    List<Message> findByUserIdOrderByCreateTimeDesc(Long userId);

    // 根据用户ID和消息类型查找
    List<Message> findByUserIdAndMsgTypeOrderByCreateTimeDesc(Long userId, String msgType);

    // 根据用户ID和状态查找
    List<Message> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, String status);

    // 根据用户ID统计未读消息数量
    long countByUserIdAndStatus(Long userId, String status);

    // 标记消息为已读
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.status = 'READ', m.readTime = :readTime WHERE m.id = :messageId AND m.userId = :userId")
    int markAsRead(@Param("messageId") Long messageId, @Param("userId") Long userId, @Param("readTime") Date readTime);

    // 标记用户所有未读消息为已读
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.status = 'READ', m.readTime = :readTime WHERE m.userId = :userId AND m.status = 'UNREAD'")
    int markAllAsRead(@Param("userId") Long userId, @Param("readTime") Date readTime);

    // 根据借阅记录ID查找消息
    List<Message> findByBorrowId(Long borrowId);

    // 删除用户的消息
    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE m.id = :messageId AND m.userId = :userId")
    int deleteByUserIdAndMessageId(@Param("messageId") Long messageId, @Param("userId") Long userId);
    // MessageRepository.java
    int countByUserIdAndStatus(long userId, String status);
}