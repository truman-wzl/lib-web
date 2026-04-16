package com.example.libweb.repository;

import com.example.libweb.entity.BorrowRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    /**
     * 检查用户是否已借阅某本书且未归还
     */
    @Query(value = "SELECT COUNT(*) FROM BORROW_RECORD WHERE USER_ID = :userId AND BOOK_ID = :bookId AND RETURN_TIME IS NULL",
            nativeQuery = true)
    int countBorrowedByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /**
     * 统计用户当前借阅数量（未归还的）
     */
    @Query(value = "SELECT COUNT(*) FROM BORROW_RECORD WHERE USER_ID = :userId AND RETURN_TIME IS NULL",
            nativeQuery = true)
    int countBorrowedByUser(@Param("userId") Long userId);

    /**
     * 根据用户ID查询借阅记录（无分页，保持现有逻辑不变）
     */
    @Query(value = "SELECT * FROM BORROW_RECORD WHERE USER_ID = :userId ORDER BY BORROW_TIME DESC",
            nativeQuery = true)
    List<BorrowRecord> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询借阅记录（带图书信息，后端分页）
     * 修正：移除不存在的ISBN和COVER_URL字段
     */
    @Query(value = "SELECT br.RECORD_ID, br.USER_ID, br.BOOK_ID, br.BORROW_TIME, " +
            "br.DUE_TIME, br.RETURN_TIME, br.STATUS, br.CREATE_TIME, " +
            "b.BOOKNAME, b.AUTHOR, b.PUBLISHER, b.CATEGORY_ID, b.TOTAL_NUMBER, b.CAN_BORROW " +
            "FROM ( " +
            "  SELECT t.*, ROWNUM as rn FROM ( " +
            "    SELECT * FROM BORROW_RECORD WHERE USER_ID = :userId ORDER BY BORROW_TIME DESC " +
            "  ) t WHERE ROWNUM <= :endRow " +
            ") br LEFT JOIN BOOK b ON br.BOOK_ID = b.BOOK_ID WHERE rn > :startRow",
            nativeQuery = true)
    List<Object[]> findByUserIdWithBookInfoPagination(
            @Param("userId") Long userId,
            @Param("startRow") int startRow,
            @Param("endRow") int endRow);

    /**
     * 根据用户ID统计借阅记录总数（新增，用于分页）
     */
    @Query(value = "SELECT COUNT(*) FROM BORROW_RECORD WHERE USER_ID = :userId",
            nativeQuery = true)
    int countByUserId(@Param("userId") Long userId);

    /**
     * 根据图书ID查询借阅记录
     */
    @Query(value = "SELECT * FROM BORROW_RECORD WHERE BOOK_ID = :bookId ORDER BY BORROW_TIME DESC",
            nativeQuery = true)
    List<BorrowRecord> findByBookId(@Param("bookId") Long bookId);

    /**
     * 查询所有未归还的借阅记录
     */
    @Query(value = "SELECT * FROM BORROW_RECORD WHERE RETURN_TIME IS NULL ORDER BY BORROW_TIME DESC",
            nativeQuery = true)
    List<BorrowRecord> findUnreturnedRecords();

    /**
     * 根据记录ID查询
     */
    @Query(value = "SELECT * FROM BORROW_RECORD WHERE RECORD_ID = :recordId", nativeQuery = true)
    Optional<BorrowRecord> findByRecordId(@Param("recordId") Long recordId);
    /**
     * 根据用户ID查询借阅记录（带图书信息和筛选条件，后端分页）
     * 注意：Oracle ROWNUM 的写法
     */
    @Query(value = "SELECT br.RECORD_ID, br.USER_ID, br.BOOK_ID, br.BORROW_TIME, " +
            "br.DUE_TIME, br.RETURN_TIME, br.STATUS, br.CREATE_TIME, " +
            "b.BOOKNAME, b.AUTHOR, b.PUBLISHER, b.CATEGORY_ID, b.TOTAL_NUMBER, b.CAN_BORROW " +
            "FROM ( " +
            "  SELECT t.*, ROWNUM as rn FROM ( " +
            "    SELECT * FROM BORROW_RECORD WHERE USER_ID = :userId " +
            "    AND (:status IS NULL OR STATUS = :status) " +  // ✅ 状态筛选
            "    AND (:keyword IS NULL OR (EXISTS ( " +
            "      SELECT 1 FROM BOOK b2 WHERE b2.BOOK_ID = BORROW_RECORD.BOOK_ID " +
            "      AND (UPPER(b2.BOOKNAME) LIKE UPPER('%' || :keyword || '%') " +
            "           OR UPPER(b2.AUTHOR) LIKE UPPER('%' || :keyword || '%')) " +
            "    ))) " +  // ✅ 关键词搜索（书名或作者）
            "    ORDER BY BORROW_TIME DESC " +
            "  ) t WHERE ROWNUM <= :endRow " +
            ") br LEFT JOIN BOOK b ON br.BOOK_ID = b.BOOK_ID WHERE rn > :startRow",
            nativeQuery = true)
    List<Object[]> findByUserIdWithBookInfoPaginationWithFilter(
            @Param("userId") Long userId,
            @Param("startRow") int startRow,
            @Param("endRow") int endRow,
            @Param("status") String status,   // 新增：状态参数
            @Param("keyword") String keyword); // 新增：关键词参数

    /**
     * 根据用户ID统计借阅记录总数（带筛选条件）
     */
    @Query(value = "SELECT COUNT(*) FROM BORROW_RECORD br " +
            "LEFT JOIN BOOK b ON br.BOOK_ID = b.BOOK_ID " +
            "WHERE br.USER_ID = :userId " +
            "AND (:status IS NULL OR br.STATUS = :status) " +  // ✅ 状态筛选
            "AND (:keyword IS NULL OR (UPPER(b.BOOKNAME) LIKE UPPER('%' || :keyword || '%') " +
            "     OR UPPER(b.AUTHOR) LIKE UPPER('%' || :keyword || '%')))",  // ✅ 关键词搜索
            nativeQuery = true)
    int countByUserIdWithFilter(@Param("userId") Long userId,
                                @Param("status") String status,
                                @Param("keyword") String keyword);
    @Modifying
    @Query("UPDATE BorrowRecord br SET br.dueTime = :newDueTime, br.status = 'RENEWED' WHERE br.recordId = :recordId AND br.userId = :userId AND br.status = 'BORROWED' AND br.dueTime > CURRENT_DATE")
    int renewBorrowRecord(@Param("recordId") Long recordId, @Param("userId") Long userId, @Param("newDueTime") Date newDueTime);
}