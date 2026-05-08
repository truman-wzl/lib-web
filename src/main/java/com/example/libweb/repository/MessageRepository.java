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

    List<Message> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<Message> findByUserIdAndMsgTypeOrderByCreateTimeDesc(Long userId, String msgType);

    List<Message> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, String status);

    long countByUserIdAndStatus(Long userId, String status);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.status = 'READ', m.readTime = :readTime WHERE m.id = :messageId AND m.userId = :userId")
    int markAsRead(@Param("messageId") Long messageId, @Param("userId") Long userId, @Param("readTime") Date readTime);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.status = 'READ', m.readTime = :readTime WHERE m.userId = :userId AND m.status = 'UNREAD'")
    int markAllAsRead(@Param("userId") Long userId, @Param("readTime") Date readTime);

    @Query("SELECT m FROM Message m WHERE m.borrowId = :borrowId AND m.msgType = 'OVERDUE' ORDER BY m.createTime DESC")
    List<Message> findOverdueMessageByBorrowId(@Param("borrowId") Long borrowId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE m.id = :messageId AND m.userId = :userId")
    int deleteByUserIdAndMessageId(@Param("messageId") Long messageId, @Param("userId") Long userId);

    int countByUserIdAndStatus(long userId, String status);
    List<Message> findByBorrowIdAndMsgTypeAndCreateTimeBetween(
            @Param("borrowId") Long borrowId,
            @Param("msgType") String msgType,
            @Param("start") Date start,
            @Param("end") Date end
    );
}
