package com.example.libweb.repository;

import com.example.libweb.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
     * 根据用户ID查询借阅记录（无分页）
     */
    @Query(value = "SELECT * FROM BORROW_RECORD WHERE USER_ID = :userId ORDER BY BORROW_TIME DESC",
            nativeQuery = true)
    List<BorrowRecord> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询借阅记录（带图书信息和分页）
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
     * 根据用户ID统计借阅记录总数（分页）
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
     * 根据用户ID查询借阅记录（带图书信息和筛选、分页）
     */
    @Query(value = """
    SELECT br.RECORD_ID, br.USER_ID, br.BOOK_ID, br.BORROW_TIME, 
    br.DUE_TIME, br.RETURN_TIME, br.STATUS, br.CREATE_TIME, 
    b.BOOKNAME, b.AUTHOR, b.PUBLISHER, b.CATEGORY_ID, b.TOTAL_NUMBER, b.CAN_BORROW 
    FROM ( 
      SELECT t.*, ROWNUM as rn FROM ( 
        SELECT * FROM BORROW_RECORD WHERE USER_ID = :userId 
        AND (:status IS NULL OR STATUS = :status) 
        AND (:keyword IS NULL OR (EXISTS ( 
          SELECT 1 FROM BOOK b2 WHERE b2.BOOK_ID = BORROW_RECORD.BOOK_ID 
          AND (UPPER(b2.BOOKNAME) LIKE UPPER('%' || :keyword || '%') 
               OR UPPER(b2.AUTHOR) LIKE UPPER('%' || :keyword || '%')
               OR TO_CHAR(b2.BOOK_ID) LIKE '%' || :keyword || '%')) 
        )) 
        ORDER BY BORROW_TIME DESC 
      ) t WHERE ROWNUM <= :endRow 
    ) br LEFT JOIN BOOK b ON br.BOOK_ID = b.BOOK_ID WHERE rn > :startRow
    """, nativeQuery = true)
    List<Object[]> findByUserIdWithBookInfoPaginationWithFilter(
            @Param("userId") Long userId,
            @Param("startRow") int startRow,
            @Param("endRow") int endRow,
            @Param("status") String status,
            @Param("keyword") String keyword);

    /**
     * 根据用户ID统计借阅记录总数（带筛选）
     */
    @Query(value = "SELECT COUNT(*) FROM BORROW_RECORD br " +
            "LEFT JOIN BOOK b ON br.BOOK_ID = b.BOOK_ID " +
            "WHERE br.USER_ID = :userId " +
            "AND (:status IS NULL OR br.STATUS = :status) " +
            "AND (:keyword IS NULL OR (UPPER(b.BOOKNAME) LIKE UPPER('%' || :keyword || '%') " +
            "     OR UPPER(b.AUTHOR) LIKE UPPER('%' || :keyword || '%') " +
            "     OR TO_CHAR(b.BOOK_ID) LIKE '%' || :keyword || '%'))",
            nativeQuery = true)
    int countByUserIdWithFilter(@Param("userId") Long userId,
                                @Param("status") String status,
                                @Param("keyword") String keyword);
    @Modifying
    @Query("UPDATE BorrowRecord br SET br.dueTime = :newDueTime, br.status = 'RENEWED' WHERE br.recordId = :recordId AND br.userId = :userId AND br.status = 'BORROWED' AND br.dueTime > CURRENT_DATE")
    int renewBorrowRecord(@Param("recordId") Long recordId, @Param("userId") Long userId, @Param("newDueTime") LocalDateTime newDueTime);

//    @Query(value = "SELECT * FROM BORROW_RECORD WHERE STATUS IN ('BORROWED', 'RENEWED') AND DUE_TIME < SYSDATE", nativeQuery = true)
//    List<BorrowRecord> findRecordsToMarkOverdue();

    @Modifying
    @Query(value = "UPDATE BORROW_RECORD SET STATUS = 'OVERDUE' WHERE RECORD_ID = :recordId", nativeQuery = true)
    int markAsOverdue(@Param("recordId") Long recordId);

    /**
     * 根据用户ID查询需要标记为逾期的记录
     */
    @Query(value = "SELECT * FROM BORROW_RECORD WHERE USER_ID = :userId AND STATUS IN ('BORROWED', 'RENEWED') AND DUE_TIME <= SYSDATE",
            nativeQuery = true)
    List<BorrowRecord> findRecordsToMarkOverdueByUserId(@Param("userId") Long userId);


    /**
     * 查询逾期记录（包含用户和图书信息）
     * 返回：借阅记录/用户邮箱/图书信息
     */
    @Query(value = """
SELECT 
    br.RECORD_ID,
    br.USER_ID,
    br.BOOK_ID,
    br.BORROW_TIME,
    br.DUE_TIME,
    br.STATUS,
    u.USERNAME,
    b.BOOKNAME
FROM BORROW_RECORD br
JOIN USERDATA u ON br.USER_ID = u.USER_ID
JOIN BOOK b ON br.BOOK_ID = b.BOOK_ID
WHERE br.STATUS IN ('BORROWED', 'RENEWED')
  AND br.RETURN_TIME IS NULL
  AND br.DUE_TIME < SYSDATE
  AND NOT EXISTS (
    SELECT 1 FROM MESSAGE m 
    WHERE m.BORROW_ID = br.RECORD_ID 
    AND m.MSG_TYPE = 'OVERDUE'
    AND TRUNC(m.CREATE_TIME) = TRUNC(SYSDATE)
  )
""", nativeQuery = true)
    List<Object[]> findOverdueRecordsWithUserAndBook();






    /**
     * 管理员：查询所有借阅记录（分页+状态筛选）
     * 关联USERDATA和BOOK表，获取完整信息
     */
    @Query(value = "SELECT br.RECORD_ID, br.USER_ID, br.BOOK_ID, br.BORROW_TIME, " +
            "br.DUE_TIME, br.RETURN_TIME, br.STATUS, br.CREATE_TIME, " +
            "u.USERNAME, u.REAL_NAME, b.BOOKNAME, b.AUTHOR, b.PUBLISHER, b.CATEGORY_ID " +
            "FROM ( " +
            "  SELECT t.*, ROWNUM as rn FROM ( " +
            "    SELECT br2.* FROM BORROW_RECORD br2 " +
            "    WHERE (:status IS NULL OR br2.STATUS = :status) " +
            "    ORDER BY br2.BORROW_TIME DESC " +
            "  ) t WHERE ROWNUM <= :endRow " +
            ") br " +
            "LEFT JOIN USERDATA u ON br.USER_ID = u.USER_ID " +
            "LEFT JOIN BOOK b ON br.BOOK_ID = b.BOOK_ID " +
            "WHERE rn > :startRow", nativeQuery = true)
    List<Object[]> findAllBorrowRecordsWithInfoPagination(
            @Param("startRow") int startRow,
            @Param("endRow") int endRow,
            @Param("status") String status);

    /**
     * 管理员：统计所有借阅记录总数（带状态筛选）
     */
    @Query(value = "SELECT COUNT(*) FROM BORROW_RECORD " +
            "WHERE (:status IS NULL OR STATUS = :status)", nativeQuery = true)
    int countAllBorrowRecordsWithFilter(@Param("status") String status);

    /**
     * 管理员：统计各状态的数量
     * 返回状态和对应的数量
     */
    @Query(value = "SELECT STATUS, COUNT(*) as count FROM BORROW_RECORD " +
            "GROUP BY STATUS", nativeQuery = true)
    List<Object[]> countByStatusGroup();
    /**
     * 借阅趋势 - 统计最近7天的借阅次数（不包含今天）
     */
    @Query(value = """
    SELECT 
        TO_CHAR(TRUNC(br.BORROW_TIME), 'yyyy-MM-dd') as borrow_date,
        COUNT(*) as borrow_count
    FROM BORROW_RECORD br
    WHERE br.BORROW_TIME >= TRUNC(SYSDATE) - 7
      AND br.BORROW_TIME < TRUNC(SYSDATE)
    GROUP BY TRUNC(br.BORROW_TIME)
    ORDER BY TRUNC(br.BORROW_TIME)
    """, nativeQuery = true)
    List<Object[]> findBorrowTrendLast7Days();

    /**
     * 热门图书TOP5（排除默认分类ID=5）
     * 返回：图书ID, 图书名称, 作者, 出版社, 借阅次数
     */
    @Query(value = """
    SELECT * FROM (
        SELECT b.BOOK_ID, b.BOOKNAME, b.AUTHOR, b.PUBLISHER, COUNT(br.RECORD_ID) as borrow_count
        FROM BORROW_RECORD br
        JOIN BOOK b ON br.BOOK_ID = b.BOOK_ID
        WHERE b.CATEGORY_ID != 5
        GROUP BY b.BOOK_ID, b.BOOKNAME, b.AUTHOR, b.PUBLISHER
        ORDER BY COUNT(br.RECORD_ID) DESC, b.BOOKNAME
    ) WHERE ROWNUM <= 5
    """, nativeQuery = true)
    List<Object[]> findTop5PopularBooks();

    /**
     * 热门类别TOP3（排除默认分类ID=5）
     * 返回：分类ID, 分类名称, 借阅次数
     */
    @Query(value = """
    SELECT * FROM (
        SELECT c.CATEGORY_ID, c.CATEGORY_NAME, COUNT(br.RECORD_ID) as borrow_count
        FROM BORROW_RECORD br
        JOIN BOOK b ON br.BOOK_ID = b.BOOK_ID
        JOIN CATEGORY c ON b.CATEGORY_ID = c.CATEGORY_ID
        WHERE c.CATEGORY_ID != 5
        GROUP BY c.CATEGORY_ID, c.CATEGORY_NAME
        ORDER BY COUNT(br.RECORD_ID) DESC, c.CATEGORY_NAME
    ) WHERE ROWNUM <= 3
    """, nativeQuery = true)
    List<Object[]> findTop3PopularCategories();


}