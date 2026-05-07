package com.example.libweb.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "MESSAGE", schema = "ADM_55230931")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_seq")
    @SequenceGenerator(name = "message_seq", sequenceName = "SEQ_MESSAGE_ID", allocationSize = 1, schema = "ADM_55230931")
    @Column(name = "ID")
    private Long id;
    //关联用户ID
    @Column(name = "USER_ID", nullable = false)
    private Long userId;
    // 关联借阅记录ID（可为空）
    @Column(name = "BORROW_ID")
    private Long borrowId;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "CONTENT", nullable = false, length = 1000)
    private String content;

    @Column(name = "MSG_TYPE", nullable = false, length = 20)
    private String msgType;

    public static final String TYPE_OVERDUE = "OVERDUE";         // 逾期提醒
    public static final String TYPE_ACHIEVEMENT = "ACHIEVEMENT"; // 成就消息

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = STATUS_UNREAD;
    public static final String STATUS_UNREAD = "UNREAD";         // 未读
    public static final String STATUS_READ = "READ";             // 已读

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATE_TIME", nullable = false)
    private Date createTime = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "READ_TIME")
    private Date readTime;
    @Column(name = "REMIND_COUNT", nullable = false)
    private Integer remindCount = 1;
    public Message() {}

    public Message(Long userId, String title, String content, String msgType) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.msgType = msgType;
    }

    public Message(Long userId, Long borrowId, String title, String content, String msgType) {
        this.userId = userId;
        this.borrowId = borrowId;
        this.title = title;
        this.content = content;
        this.msgType = msgType;
    }

    public static Message createOverdueMessage(Long userId, Long borrowId, String bookName,
                                               Date borrowTime, Date dueTime, String bookId) {
        String title = " 图书逾期提醒";
        String content = String.format(
                "您借阅的图书《%s》已逾期！\n\n" +
                        "借阅日期：%tF\n" +
                        "应还日期：%tF\n" +
                        " 图书编号：%s\n\n" +
                        "请尽快到图书馆办理还书手续，以免产生更多逾期费用。",
                bookName,
                borrowTime,
                dueTime,
                bookId
        );

        Message message = new Message(userId, borrowId, title, content, TYPE_OVERDUE);
        message.setStatus(STATUS_UNREAD);
        message.setRemindCount(3);  // 逾期消息初始提醒次数为3
        return message;
    }
    public static Message createAchievementMessage(Long userId, String achievementName,
                                                   String achievementDescription) {
        String title = "🏆成就达成";
        String content = String.format(
                "恭喜您！达成成就：%s\n\n" +
                        "成就描述：%s\n\n" +
                        "继续保持，解锁更多成就！",
                achievementName,
                achievementDescription
        );

        Message message = new Message(userId, title, content, TYPE_ACHIEVEMENT);
        message.setStatus(STATUS_UNREAD);
        message.setRemindCount(1);
        return message;
    }

    public static Message createAchievementMessage(Long userId, Long borrowId, String achievementName,
                                                   String achievementDescription) {
        Message message = createAchievementMessage(userId, achievementName, achievementDescription);
        message.setBorrowId(borrowId);
        return message;
    }

    //Getter和Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getBorrowId() { return borrowId; }
    public void setBorrowId(Long borrowId) { this.borrowId = borrowId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMsgType() { return msgType; }
    public void setMsgType(String msgType) { this.msgType = msgType; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        if (STATUS_READ.equals(status)) {
            this.readTime = new Date();
        }
    }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getReadTime() { return readTime; }
    public void setReadTime(Date readTime) { this.readTime = readTime; }

    public Integer getRemindCount() { return remindCount; }
    public void setRemindCount(Integer remindCount) { this.remindCount = remindCount; }

    public boolean isRead() {
        return STATUS_READ.equals(status);
    }

    public void markAsRead() {
        this.status = STATUS_READ;
        this.readTime = new Date();
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", userId=" + userId +
                ", borrowId=" + borrowId +
                ", title='" + title + '\'' +
                ", msgType='" + msgType + '\'' +
                ", status='" + status + '\'' +
                ", remindCount=" + remindCount +
                ", createTime=" + createTime +
                '}';
    }
}