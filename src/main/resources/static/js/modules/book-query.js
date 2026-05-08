//图书查询与借阅
(function() {
    'use strict';
    const bookQueryModule = {
        name: '图书查询',
        state: {
            currentPage: 1,
            pageSize: 10,
            searchParams: {
                bookname: '',
                author: '',
                categoryId: null
            }
        },
        moduleContent: null,
        render: function() {
            this.moduleContent = document.getElementById('moduleContent');
            this.moduleContent.innerHTML = this.getTemplate();
            this.initModule();
        },

        getTemplate: function() {
            return `
                <div class="container-fluid">
                    <div class="card mb-4">
                        <div class="card-body">
                            <h5 class="card-title mb-3">搜索图书</h5>
                            <form id="searchForm">
                                <div class="row g-3">
                                    <div class="col-md-4">
                                        <input type="text" class="form-control" id="bookname" placeholder="输入书名">
                                    </div>
                                    <div class="col-md-3">
                                        <input type="text" class="form-control" id="author" placeholder="输入作者">
                                    </div>
                                    <div class="col-md-3">
                                        <select class="form-select" id="category">
                                            <option value="">全部分类</option>
                                        </select>
                                    </div>
                                    <div class="col-md-2">
                                        <button type="submit" class="btn btn-primary w-100">
                                            <i class="bi bi-search me-1"></i>搜索
                                        </button>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </div>
                    <div class="card">
                        <div class="card-body">
                            <div class="table-responsive">
                                <table class="table table-hover table-striped">
                                    <thead class="table-light">
                                        <tr>
                                            <th width="80">ID</th>
                                            <th>书名</th>
                                            <th>作者</th>
                                            <th>出版社</th>
                                            <th>分类</th>
                                            <th width="120">总数/可借</th>
                                            <th width="100">操作</th>
                                        </tr>
                                    </thead>
                                    <tbody id="bookTableBody">
                                        <tr>
                                            <td colspan="7" class="text-center py-4">
                                                <div class="spinner-border spinner-border-sm" role="status"></div>
                                                <span class="ms-2">加载中...</span>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                            <div id="pagination" class="mt-4"></div>
                        </div>
                    </div>
                    <div class="modal fade" id="borrowConfirmModal" tabindex="-1" aria-hidden="true">
                        <div class="modal-dialog modal-dialog-centered">
                            <div class="modal-content">
                                <div class="modal-header bg-primary text-white">
                                    <h5 class="modal-title">
                                        <i class="bi bi-question-circle me-2"></i>确认借阅
                                    </h5>
                                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                                </div>
                                <div class="modal-body">
                                    <div class="text-center mb-3">
                                        <div class="text-primary" style="font-size: 3rem;">
                                            <i class="bi bi-question-circle-fill"></i>
                                        </div>
                                        <h5 class="my-3">确认要借阅这本书吗？</h5>
                                    </div>

                                    <div id="bookConfirmInfo" class="border rounded p-3 mb-3">
                                    </div>
                                    <div class="alert alert-info mb-0 small">
                                        <i class="bi bi-info-circle me-1"></i>
                                        借阅期限为1min，请按时归还
                                    </div>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">取消</button>
                                    <button type="button" class="btn btn-primary" id="confirmBorrowBtn">确认借阅</button>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="modal fade" id="borrowResultModal" tabindex="-1" aria-hidden="true">
                        <div class="modal-dialog modal-dialog-centered">
                            <div class="modal-content">
                                <div class="modal-header" id="resultModalHeader">
                                    <h5 class="modal-title" id="resultModalTitle"></h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                </div>
                                <div class="modal-body text-center py-4">
                                    <div class="mb-3" id="resultIcon" style="font-size: 3rem;">
                                    </div>
                                    <h5 id="resultMessage" class="mb-3"></h5>
                                    <div id="resultDetails" class="text-muted small">
                                    </div>
                                </div>
                                <div class="modal-footer justify-content-center">
                                    <button type="button" class="btn btn-primary" data-bs-dismiss="modal">确定</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        },

        initModule: function() {
            this.loadCategories();
            this.loadBooks();
            this.bindEvents();
        },

        async loadCategories() {
            const categorySelect = document.getElementById('category');

            try {
                const response = await fetch('/api/categories');
                const result = await response.json();

                if (result.success && result.data) {
                    console.log('分类数据加载成功，数量:', result.data.length);

                    while (categorySelect.options.length > 1) {
                        categorySelect.remove(1);
                    }

                    result.data.forEach(category => {
                        if (category.categoryId !== 5) {
                            const option = document.createElement('option');
                            option.value = category.categoryId;
                            option.textContent = category.categoryName;
                            categorySelect.appendChild(option);
                        }
                    });
                } else {
                    console.error('分类API返回失败:', result.message);
                }
            } catch (error) {
                console.error('加载分类失败:', error);
                const errorOption = document.createElement('option');
                errorOption.value = "";
                errorOption.textContent = "加载分类失败";
                errorOption.disabled = true;
                categorySelect.appendChild(errorOption);
            }
        },

        bindEvents: function() {
            const self = this;

            const searchForm = document.getElementById('searchForm');
            if (searchForm) {
                searchForm.addEventListener('submit', function(e) {
                    e.preventDefault();
                    self.state.currentPage = 1;
                    self.state.searchParams = {
                        bookname: document.getElementById('bookname').value.trim(),
                        author: document.getElementById('author').value.trim(),
                        categoryId: document.getElementById('category').value || null
                    };
                    self.loadBooks();
                });
            }
        },

        async loadBooks() {
            const tbody = document.getElementById('bookTableBody');

            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-4">
                        <div class="spinner-border spinner-border-sm" role="status"></div>
                        <span class="ms-2">加载图书中...</span>
                    </td>
                </tr>
            `;

            try {
                const params = new URLSearchParams({
                    page: this.state.currentPage - 1,
                    size: this.state.pageSize
                });
                if (this.state.searchParams.bookname) {
                    params.append('bookname', this.state.searchParams.bookname);
                }
                if (this.state.searchParams.author) {
                    params.append('author', this.state.searchParams.author);
                }
                if (this.state.searchParams.categoryId) {
                    params.append('categoryId', this.state.searchParams.categoryId);
                }

                console.log('请求图书列表，参数:', Object.fromEntries(params));
                const response = await fetch(`/api/books/search?${params}`);
                const result = await response.json();

                if (result.success) {
                    console.log('图书数据加载成功，数量:', result.data.length, '总页数:', result.totalPages);
                    this.renderBooks(result.data);
                    this.renderPagination(result.totalPages, result.currentPage);
                } else {
                    throw new Error(result.message || '加载图书失败');
                }
            } catch (error) {
                console.error('加载图书失败:', error);
                tbody.innerHTML = `
                    <tr>
                        <td colspan="7" class="text-center py-4 text-danger">
                            <i class="bi bi-exclamation-triangle me-2"></i>
                            加载失败: ${error.message}
                        </td>
                    </tr>
                `;
            }
        },

        renderBooks: function(books) {
            const tbody = document.getElementById('bookTableBody');

            if (!books || books.length === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="7" class="text-center py-4 text-muted">
                            <i class="bi bi-book me-2"></i>
                            暂无图书数据
                        </td>
                    </tr>
                `;
                return;
            }

            let html = '';
            books.forEach(book => {
                const publisherDisplay = book.publisher ? book.publisher : '暂无出版社信息';
                const canBorrow = book.canBorrow || 0;
                const isAvailable = canBorrow > 0;

                const categoryName = (book.category && book.category.categoryName) ?
                    book.category.categoryName : '-';

                const bookName = (book.bookname || '未命名').replace(/'/g, "\\'");
                const author = (book.author || '-').replace(/'/g, "\\'");
                const publisher = publisherDisplay.replace(/'/g, "\\'");
                const category = categoryName.replace(/'/g, "\\'");

                html += `
                    <tr>
                        <td>${book.bookId || '-'}</td>
                        <td>
                            <div class="fw-semibold">${book.bookname || '未命名'}</div>
                        </td>
                        <td>${book.author || '-'}</td>
                        <td>${publisherDisplay}</td>
                        <td>
                            <span class="badge bg-info">${categoryName}</span>
                        </td>
                        <td>
                            <span class="badge bg-secondary">${book.totalNumber || 0}</span> /
                            <span class="badge ${isAvailable ? 'bg-success' : 'bg-danger'}">
                                ${canBorrow}
                            </span>
                        </td>
                        <td>
                            <button
                                class="btn btn-sm ${isAvailable ? 'btn-primary' : 'btn-secondary'}"
                                onclick="showBorrowConfirmWithModal(event, ${book.bookId}, '${bookName}', '${author}', '${publisher}', '${category}')"
                                ${!isAvailable ? 'disabled' : ''}
                                title="${isAvailable ? '借阅此书' : '暂无库存'}"
                            >
                                <i class="bi bi-cart-plus"></i>
                                <span class="d-none d-md-inline">借阅</span>
                            </button>
                        </td>
                    </tr>
                `;
            });

            tbody.innerHTML = html;
        },

        renderPagination: function(totalPages, currentPage) {
            const container = document.getElementById('pagination');

            if (totalPages <= 1) {
                container.innerHTML = '';
                return;
            }

            console.log('渲染分页，总页数:', totalPages, '当前页(后端索引):', currentPage, '当前页(前端索引):', this.state.currentPage);
            const currentPageOneBased = currentPage + 1;

            let html = `
                <nav aria-label="图书分页">
                    <ul class="pagination justify-content-center mb-0">
            `;
            if (currentPageOneBased > 1) {
                html += `
                    <li class="page-item">
                        <button class="page-link" data-page="${currentPageOneBased - 1}" aria-label="上一页">
                            <span aria-hidden="true">&laquo;</span>
                        </button>
                    </li>
                `;
            } else {
                html += `
                    <li class="page-item disabled">
                        <span class="page-link" aria-label="上一页">
                            <span aria-hidden="true">&laquo;</span>
                        </span>
                    </li>
                `;
            }

            const maxVisiblePages = 5;
            let startPage, endPage;

            if (totalPages <= maxVisiblePages) {
                startPage = 1;
                endPage = totalPages;
            } else {
                const halfVisible = Math.floor(maxVisiblePages / 2);
                startPage = Math.max(currentPageOneBased - halfVisible, 1);
                endPage = Math.min(startPage + maxVisiblePages - 1, totalPages);
                if (endPage - startPage + 1 < maxVisiblePages) {
                    startPage = Math.max(endPage - maxVisiblePages + 1, 1);
                }
            }
            for (let i = startPage; i <= endPage; i++) {
                if (i === currentPageOneBased) {
                    html += `
                        <li class="page-item active" aria-current="page">
                            <span class="page-link">${i}</span>
                        </li>
                    `;
                } else {
                    html += `
                        <li class="page-item">
                            <button class="page-link" data-page="${i}">${i}</button>
                        </li>
                    `;
                }
            }

            if (currentPageOneBased < totalPages) {
                html += `
                    <li class="page-item">
                        <button class="page-link" data-page="${currentPageOneBased + 1}" aria-label="下一页">
                            <span aria-hidden="true">&raquo;</span>
                        </button>
                    </li>
                `;
            } else {
                html += `
                    <li class="page-item disabled">
                        <span class="page-link" aria-label="下一页">
                            <span aria-hidden="true">&raquo;</span>
                        </span>
                    </li>
                `;
            }

            html += `
                    </ul>
                </nav>
            `;

            container.innerHTML = html;
            this.bindPaginationEvents();
        },
        bindPaginationEvents: function() {
            const container = document.getElementById('pagination');
            const self = this;
            if (!container) return;
            container.addEventListener('click', function(e) {
                const target = e.target;
                const pageButton = target.closest('.page-link');

                if (pageButton && pageButton.hasAttribute('data-page')) {
                    e.preventDefault();

                    const page = parseInt(pageButton.getAttribute('data-page'));
                    console.log('点击分页按钮，目标页码:', page, '当前页码:', self.state.currentPage);

                    if (!isNaN(page) && page !== self.state.currentPage) {
                        self.changePage(page);
                    }
                }
            });
        },

        changePage: function(page) {
            console.log('切换页码，旧页码:', this.state.currentPage, '新页码:', page);

            if (page < 1) {
                console.warn('页码不能小于1');
                return;
            }

            if (page === this.state.currentPage) {
                return;
            }

            this.state.currentPage = page;
            console.log('更新当前页码为:', this.state.currentPage);

            this.loadBooks();

            const bookTable = document.querySelector('.table-responsive');
            if (bookTable) {
                bookTable.scrollIntoView({ behavior: 'smooth' });
            }
        },

        borrowBook: async function(bookId) {
            console.log('尝试借阅图书，ID:', bookId);
            if (!window.AppState || !window.AppState.currentUser) {
                alert('请先登录后再借阅图书');
                return;
            }
            const userId = window.AppState.currentUser.userId;
            if (!userId) {
                alert('用户信息错误，请重新登录');
                return;
            }
            if (!confirm('确定要借阅这本图书吗？')) {
                return;
            }

            try {
                console.log('发送借阅请求，用户ID:', userId, '图书ID:', bookId);

                const response = await fetch('/api/borrow', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({
                        userId: userId,
                        bookId: bookId
                    })
                });

                const result = await response.json();
                console.log('借阅API响应:', result);

                if (result.success) {
                    alert('借阅成功！请在30天内归还');
                    this.loadBooks();
                    if (result.data) {
                        const dueDate = new Date(result.data.dueTime);
                        const dueStr = dueDate.toLocaleDateString('zh-CN', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric'
                        });
                        console.log(`借阅成功！应还日期: ${dueStr}`);
                    }
                } else {
                    throw new Error(result.message || '借阅失败');
                }
            } catch (error) {
                console.error('借阅失败:', error);
                alert(`借阅失败: ${error.message}`);
            }
        }
    };
    if (typeof window !== 'undefined') {
        if (typeof window.registerModule === 'function') {
            window.registerModule('book-query', bookQueryModule);
        }
        else {
            if (!window.modules) {
                window.modules = {};
            }
            window.modules['book-query'] = bookQueryModule;
        }
        window.borrowBook = async function(bookId, bookName, author, publisher, category) {
            console.log('[全局函数] 借阅图书，ID:', bookId, '书名:', bookName);
            const currentUser = window.AppState?.currentUser;
            if (!currentUser || !currentUser.userId) {
                window.showBorrowResult('error', '请先登录后再借阅图书');
                return;
            }

            const userId = currentUser.userId;
            console.log('借阅用户ID:', userId, '图书ID:', bookId);

            try {
                const response = await fetch('/api/borrow', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ userId, bookId })
                });

                const result = await response.json();
                console.log('借阅结果:', result);

                if (result.success) {
                    const dueDate = new Date(result.data.dueTime);
                    const dueStr = dueDate.toLocaleDateString('zh-CN', {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                        weekday: 'long'
                    });

                    const successDetails = `
                        <div class="text-start">
                            <p class="mb-1"><strong>借阅图书：</strong>《${bookName}》</p>
                            <p class="mb-1"><strong>借阅时间：</strong>${new Date().toLocaleDateString('zh-CN')}</p>
                            <p class="mb-1"><strong>应还时间：</strong>${dueStr}</p>
                            <p class="mb-0"><strong>借阅编号：</strong>${result.data.recordId}</p>
                        </div>
                    `;

                    window.showBorrowResult('success', '借阅成功！请在30天内归还', successDetails);
                    setTimeout(() => {
                        location.reload();
                    }, 1000);
                } else {
                    throw new Error(result.message || '借阅失败');
                }
            } catch (error) {
                console.error('借阅失败:', error);
                window.showBorrowResult('error', `借阅失败: ${error.message}`);
            }
        };
        window.showBorrowConfirmWithModal = function(event, bookId, bookName, author, publisher, category) {
            if (event) {
                event.preventDefault();
                event.stopPropagation();
            }
            console.log('[全局函数] 显示借阅确认框，图书:', bookName, 'ID:', bookId);
            const currentUser = window.AppState?.currentUser;
            if (!currentUser || !currentUser.userId) {
                window.showBorrowResult('error', '请先登录后再借阅图书');
                return;
            }
            const bookInfoHtml = `
                <div class="book-info">
                    <p class="mb-1"><strong>书名：</strong><span class="text-primary">《${bookName}》</span></p>
                    <p class="mb-1"><strong>作者：</strong>${author}</p>
                    <p class="mb-1"><strong>出版社：</strong><span class="text-info">${publisher}</span></p>
                    <p class="mb-1"><strong>分类：</strong><span class="badge bg-secondary">${category}</span></p>
                </div>
            `;
            document.getElementById('bookConfirmInfo').innerHTML = bookInfoHtml;
            const confirmBtn = document.getElementById('confirmBorrowBtn');
            confirmBtn.onclick = function() {
                console.log('用户确认借阅，图书ID:', bookId);
                window.borrowBook(bookId, bookName, author, publisher, category);
                const modal = bootstrap.Modal.getInstance(document.getElementById('borrowConfirmModal'));
                if (modal) {
                    modal.hide();
                }
            };
            const confirmModal = new bootstrap.Modal(document.getElementById('borrowConfirmModal'));
            confirmModal.show();
        };

        window.showBorrowResult = function(type, message, details = null) {
            const modal = new bootstrap.Modal(document.getElementById('borrowResultModal'));
            const header = document.getElementById('resultModalHeader');
            const title = document.getElementById('resultModalTitle');
            const icon = document.getElementById('resultIcon');
            const msg = document.getElementById('resultMessage');
            const detailsEl = document.getElementById('resultDetails');

            if (type === 'success') {
                header.className = 'modal-header bg-success text-white';
                title.textContent = '借阅成功';
                icon.innerHTML = '<i class="bi bi-check-circle-fill text-success"></i>';
            } else if (type === 'error') {
                header.className = 'modal-header bg-danger text-white';
                title.textContent = '借阅失败';
                icon.innerHTML = '<i class="bi bi-x-circle-fill text-danger"></i>';
            } else if (type === 'warning') {
                header.className = 'modal-header bg-warning text-white';
                title.textContent = '提示';
                icon.innerHTML = '<i class="bi bi-exclamation-triangle-fill text-warning"></i>';
            } else {
                header.className = 'modal-header bg-info text-white';
                title.textContent = '提示';
                icon.innerHTML = '<i class="bi bi-info-circle-fill text-info"></i>';
            }

            msg.textContent = message;

            if (details) {
                detailsEl.innerHTML = details;
                detailsEl.style.display = 'block';
            } else {
                detailsEl.style.display = 'none';
            }

            modal.show();
            const modalEl = document.getElementById('borrowResultModal');
            modalEl.addEventListener('hidden.bs.modal', function() {
                if (type === 'success') {
                    if (window.modules && window.modules['book-query'] &&
                        typeof window.modules['book-query'].loadBooks === 'function') {
                        window.modules['book-query'].loadBooks();
                    }
                }
            });
        };
    }
})();