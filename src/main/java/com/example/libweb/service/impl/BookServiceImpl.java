package com.example.libweb.service.impl;

import com.example.libweb.entity.Book;
import com.example.libweb.entity.Category;
import com.example.libweb.repository.BookRepository;
import com.example.libweb.repository.CategoryRepository;
import com.example.libweb.service.BookService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    @Transactional
    public Book saveBook(Book book) {
        if (book.getBookname() == null || book.getBookname().trim().isEmpty()) {
            throw new RuntimeException("图书名称不能为空");
        }

        if (book.getTotalNumber() == null || book.getTotalNumber() < 0) {
            throw new RuntimeException("图书总数必须大于等于0");
        }

        if (book.getCanBorrow() == null || book.getCanBorrow() < 0) {
            throw new RuntimeException("可借数量必须大于等于0");
        }

        //可借数量不超过总数
        if (book.getCanBorrow() > book.getTotalNumber()) {
            throw new RuntimeException("可借数量不能大于图书总数");
        }

        //验证分类
        if (book.getCategory() == null || book.getCategory().getCategoryId() == null) {
            throw new RuntimeException("图书必须关联到一个分类");
        }

        // 查找分类
        Long categoryId = book.getCategory().getCategoryId();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("指定的分类不存在，ID: " + categoryId));

        // 设置关联的分类
        book.setCategory(category);

        //更新操作的特殊处理
        if (book.getBookId() != null) {
            Optional<Book> existingBookOpt = bookRepository.findById(book.getBookId());
            if (existingBookOpt.isPresent()) {
                Book existingBook = existingBookOpt.get();
                // 计算已借出数量
                int borrowed = existingBook.getTotalNumber() - existingBook.getCanBorrow();
                // 验证新可借数量是否合理
                if (book.getCanBorrow() < 0) {
                    throw new RuntimeException("可借数量不能为负数");
                }

                if (book.getTotalNumber() < borrowed) {
                    throw new RuntimeException("图书总数不能小于已借出数量（当前已借出：" + borrowed + "本）");
                }

                book.setCanBorrow(book.getTotalNumber() - borrowed);
            }
        }

        return bookRepository.save(book);
    }

    @Override
    public Optional<Book> findById(Long bookId) {
        if (bookId == null) {
            return Optional.empty();
        }
        return bookRepository.findById(bookId);
    }

    @Override
    public Page<Book> findAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    @Override
    public Page<Book> searchBooks(String bookname, String author, Long categoryId, Pageable pageable) {
        String booknameParam = StringUtils.hasText(bookname) ? bookname : null;
        String authorParam = StringUtils.hasText(author) ? author : null;
        Long categoryIdParam = (categoryId != null && categoryId > 0) ? categoryId : null;

        int pageSize = pageable.getPageSize();
        int startRow = (int) pageable.getOffset();
        int endRow = startRow + pageSize;
        List<Book> content = bookRepository.searchBooksNative(
                booknameParam, authorParam, categoryIdParam, startRow, endRow
        );
        long total = bookRepository.countSearchBooksNative(
                booknameParam, authorParam, categoryIdParam
        );

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional
    public void deleteBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new RuntimeException("图书不存在，ID: " + bookId);
        }
        bookRepository.deleteById(bookId);
    }

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }
    // BookServiceImpl.java
    @Override
    public Optional<Book> findByBooknameAndAuthorAndPublisher(String bookname, String author, String publisher) {
        return bookRepository.findByBooknameAndAuthorAndPublisher(bookname, author, publisher);
    }

    @Override
    public List<Book> saveAll(List<Book> books) {
        return bookRepository.saveAll(books);
    }
}