package com.example.libweb.repository;

import com.example.libweb.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    /**
     * 检查分类名称是否已存在
     */
    @Query(value = "SELECT COUNT(*) FROM category WHERE category_name = :categoryName",
            nativeQuery = true)
    int countByCategoryName(@Param("categoryName") String categoryName);

    /**
     * 检查分类是否存在的默认方法
     */
    default boolean existsByCategoryName(String categoryName) {
        return countByCategoryName(categoryName) > 0;
    }

    /**
     * 根据名称模糊查询（不区分大小写）
     */
    @Query(value = "SELECT * FROM category WHERE LOWER(category_name) LIKE LOWER('%' || :keyword || '%')",
            nativeQuery = true)
    List<Category> findByCategoryNameContaining(@Param("keyword") String keyword);

    /**
     * 按ID查找分类
     */
    @Query(value = "SELECT * FROM category WHERE category_id = :id AND ROWNUM = 1",
            nativeQuery = true)
    Optional<Category> findById(@Param("id") Long id);

    /**
     * 检查分类是否存在（按ID）
     */
    @Query(value = "SELECT COUNT(*) FROM category WHERE category_id = :id",
            nativeQuery = true)
    int countById(@Param("id") Long id);

    default boolean existsById(Long id) {
        return countById(id) > 0;
    }
    //根据分类名称精确查找
    @Query(value = "SELECT * FROM category WHERE category_name = :categoryName AND ROWNUM = 1",
            nativeQuery = true)
    Optional<Category> findByCategoryName(@Param("categoryName") String categoryName);

}