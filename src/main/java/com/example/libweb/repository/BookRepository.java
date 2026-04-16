package com.example.libweb.repository;

import com.example.libweb.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * 统计某个分类下的图书数量
     */
    @Query(value = "SELECT COUNT(*) FROM book WHERE category_id = :categoryId", nativeQuery = true)
    long countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 将图书从一个分类转移到另一个分类
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE book SET category_id = :targetCategoryId WHERE category_id = :sourceCategoryId",
            nativeQuery = true)
    int updateBooksCategory(@Param("sourceCategoryId") Long sourceCategoryId,
                            @Param("targetCategoryId") Long targetCategoryId);

    /**
     * 检查图书是否存在
     */
    @Query(value = "SELECT COUNT(*) FROM book WHERE book_id = :bookId", nativeQuery = true)
    int countByBookId(@Param("bookId") Long bookId);

    default boolean existsById(Long bookId) {
        return countByBookId(bookId) > 0;
    }
    // ========== 新增：修复编译错误的方法 ==========

    /**
     * 搜索图书（分页，Oracle 11g兼容）
     */
    @Query(value = "SELECT * FROM (SELECT b.*, ROWNUM rnum FROM (" +
            "SELECT * FROM book WHERE " +
            "(:bookname IS NULL OR LOWER(bookname) LIKE '%' || LOWER(:bookname) || '%') AND " +
            "(:author IS NULL OR LOWER(author) LIKE '%' || LOWER(:author) || '%') AND " +
            "(:categoryId IS NULL OR category_id = :categoryId) " +
            "ORDER BY book_id" +
            ") b WHERE ROWNUM <= :endRow) WHERE rnum > :startRow",
            nativeQuery = true)
    List<Book> searchBooksNative(
            @Param("bookname") String bookname,
            @Param("author") String author,
            @Param("categoryId") Long categoryId,
            @Param("startRow") int startRow,
            @Param("endRow") int endRow
    );

    /**
     * 统计搜索结果数量
     */
    @Query(value = "SELECT COUNT(*) FROM book WHERE " +
            "(:bookname IS NULL OR LOWER(bookname) LIKE '%' || LOWER(:bookname) || '%') AND " +
            "(:author IS NULL OR LOWER(author) LIKE '%' || LOWER(:author) || '%') AND " +
            "(:categoryId IS NULL OR category_id = :categoryId)",
            nativeQuery = true)
    long countSearchBooksNative(
            @Param("bookname") String bookname,
            @Param("author") String author,
            @Param("categoryId") Long categoryId
    );
    // 在BookRepository中添加这个方法
    @Modifying
    @Query("UPDATE Book b SET b.canBorrow = b.canBorrow - 1 WHERE b.bookId = :bookId AND b.canBorrow > 0")
    int decrementCanBorrow(@Param("bookId") Long bookId);

}