package com.example.libweb.task;

import com.example.libweb.entity.Message;
import com.example.libweb.repository.BorrowRecordRepository;
import com.example.libweb.service.MessageService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
@Transactional
public class OverdueCheckTask {
    private static final Logger logger = LoggerFactory.getLogger(OverdueCheckTask.class);

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private MessageService messageService;

    @PostConstruct
    public void init() {
        logger.info("OverdueCheckTask 初始化完成");
        logger.info("定时任务已配置: 每天凌晨1点执行");
    }
    @Scheduled(fixedRate = 60000)
    public void testCheckOverdue() {
        logger.info("测试任务执行: 当前时间 = {}", LocalDateTime.now());
        checkOverdueRecords();
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduledCheck() {
        logger.info("正式定时任务执行: 当前时间 = {}", LocalDateTime.now());
        checkOverdueRecords();
    }
    public void checkOverdueRecords() {
        logger.info("开始检查逾期记录...");

        try {
            List<Object[]> overdueRecords = borrowRecordRepository.findOverdueRecordsWithUserAndBook();

            logger.info("查询到 {} 条逾期记录", overdueRecords.size());

            if (overdueRecords.isEmpty()) {
                logger.info("没有逾期记录需要处理");
                return;
            }

            int successCount = 0;
            int failCount = 0;
            int skippedCount = 0;

            for (Object[] record : overdueRecords) {
                try {
                    Long recordId = ((Number) record[0]).longValue();
                    Long userId = ((Number) record[1]).longValue();
                    Long bookId = ((Number) record[2]).longValue();
                    String username = (String) record[6];
                    String bookName = (String) record[7];

                    Date borrowTime = (Date) record[3];
                    Date dueTime = (Date) record[4];

                    logger.info("处理逾期记录: 用户={}({}), 图书={}, 借阅ID={}",
                            username, userId, bookName, recordId);

                    Message sentMessage = messageService.sendOverdueMessage(
                            userId,
                            recordId,
                            bookName,
                            borrowTime,
                            dueTime,
                            String.valueOf(bookId)
                    );

                    if (sentMessage != null) {
                        successCount++;
                        Integer remainingCount = sentMessage.getRemindCount();
                        logger.info("已发送逾期提醒: 用户={}, 图书={}, 剩余次数={}",
                                username, bookName, remainingCount);
                    } else {
                        skippedCount++;
                        logger.info("跳过发送: 用户={}, 图书={}, 原因=已有消息或次数用完",
                                username, bookName);
                    }
                } catch (Exception e) {
                    failCount++;
                    logger.error("处理记录失败: 借阅ID={}, 错误: {}",
                            ((Number) record[0]).longValue(), e.getMessage());
                }
            }

            logger.info("逾期检查完成: 成功 {} 条, 跳过 {} 条, 失败 {} 条",
                    successCount, skippedCount, failCount);

        } catch (Exception e) {
            logger.error("逾期检查任务执行失败: {}", e.getMessage());
        }
    }


    //手动触发接口
    public void triggerManually() {
        logger.info("手动触发逾期检查");
        checkOverdueRecords();
    }
}