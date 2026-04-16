package com.example.libweb.repository;

import com.example.libweb.entity.Userdata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
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
}