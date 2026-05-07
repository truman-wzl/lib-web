package com.example.libweb.service;

import com.example.libweb.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface BookService {
    // 添加或更新图书
    Book saveBook(Book book);

    // 根据ID查找图书
    Optional<Book> findById(Long bookId);

    // 分页查询所有图书
    Page<Book> findAllBooks(Pageable pageable);

    // 多条件搜索图书
    Page<Book> searchBooks(String bookname, String author, Long categoryId, Pageable pageable);

    // 删除图书
    void deleteBook(Long bookId);

    // 获取所有图书（不分页，用于导出等）
    List<Book> getAllBooks();
    //批量导入
    Optional<Book> findByBooknameAndAuthorAndPublisher(String bookname, String author, String publisher);
    List<Book> saveAll(List<Book> books);
}