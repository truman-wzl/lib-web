package com.example.libweb.repository;

import com.example.libweb.entity.Userdata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserdataRepository extends JpaRepository<Userdata, Long> {

    // 使用原生查询，避免 FETCH FIRST 语法
    @Query(value = "SELECT * FROM userdata WHERE username = :username AND ROWNUM = 1", nativeQuery = true)
    Optional<Userdata> findByUsername(@Param("username") String username);

    // 使用原生查询检查用户名是否存在
    @Query(value = "SELECT COUNT(*) FROM userdata WHERE username = :username", nativeQuery = true)
    int countByUsername(@Param("username") String username);

    // 提供一个方便的 exists 方法
    default boolean existsByUsername(String username) {
        return countByUsername(username) > 0;
    }
    // 通过邮箱查找用户（新增）
    @Query(value = "SELECT * FROM userdata WHERE email = :email AND ROWNUM = 1", nativeQuery = true)
    Optional<Userdata> findByEmail(@Param("email") String email);
    // ==== 新增的用户管理相关查询方法 ====

    // 根据状态统计用户数量
    @Query(value = "SELECT COUNT(*) FROM userdata WHERE status = :status", nativeQuery = true)
    long countByStatus(@Param("status") String status);

    // 根据角色统计用户数量
    @Query(value = "SELECT COUNT(*) FROM userdata WHERE role = :role", nativeQuery = true)
    long countByRole(@Param("role") String role);

    // 关键词搜索（用户名、真实姓名、邮箱）
    /**
     * Oracle 11g 兼容的分页查询
     * 使用ROWNUM进行分页
     */
    @Query(value = "SELECT * FROM (" +
            "SELECT t.*, ROWNUM rn FROM (" +
            "  SELECT * FROM userdata WHERE " +
            "  (:keyword IS NULL OR " +
            "  LOWER(username) LIKE '%' || LOWER(:keyword) || '%' OR " +
            "  LOWER(real_name) LIKE '%' || LOWER(:keyword) || '%' OR " +
            "  LOWER(email) LIKE '%' || LOWER(:keyword) || '%') " +
            "  ORDER BY user_id DESC" +
            ") t WHERE ROWNUM <= :endRow" +
            ") WHERE rn > :startRow",
            nativeQuery = true)
    List<Userdata> findByKeywordWithPagination(
            @Param("keyword") String keyword,
            @Param("startRow") int startRow,
            @Param("endRow") int endRow);
    /**
     * 获取搜索结果的记录数
     */
    @Query(value = "SELECT COUNT(*) FROM userdata WHERE " +
            "(:keyword IS NULL OR " +
            "LOWER(username) LIKE '%' || LOWER(:keyword) || '%' OR " +
            "LOWER(real_name) LIKE '%' || LOWER(:keyword) || '%' OR " +
            "LOWER(email) LIKE '%' || LOWER(:keyword) || '%')",
            nativeQuery = true)
    long countByKeyword(@Param("keyword") String keyword);
    // ==== 新增：只更新最后登录时间 ====
    @Modifying
    @Transactional
    @Query(value = "UPDATE userdata SET last_login_time = :lastLoginTime WHERE user_id = :userId", nativeQuery = true)
    void updateLastLoginTime(@Param("userId") Long userId, @Param("lastLoginTime") Date lastLoginTime);
}