package com.example.libweb.service;

import com.example.libweb.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface BookService {
    Book saveBook(Book book);

    Optional<Book> findById(Long bookId);

    Page<Book> findAllBooks(Pageable pageable);

    Page<Book> searchBooks(String bookname, String author, Long categoryId, Pageable pageable);

    void deleteBook(Long bookId);

    // 获取所有图书（不分页，用于导出）
    List<Book> getAllBooks();
    Optional<Book> findByBooknameAndAuthorAndPublisher(String bookname, String author, String publisher);
    List<Book> saveAll(List<Book> books);
}