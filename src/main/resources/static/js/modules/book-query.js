// js/modules/book-query.js
(function() {
    'use strict';

    // 普通用户图书查询模块
    const bookQueryModule = {
        // 模块配置
        name: '图书查询',

        // 状态管理
        state: {
            currentPage: 1,
            pageSize: 10,
            searchParams: {
                bookname: '',
                author: '',
                categoryId: null
            }
        },

        // 模块内容容器
        moduleContent: null,

        // 渲染模块界面
        render: function() {
            this.moduleContent = document.getElementById('moduleContent');
            this.moduleContent.innerHTML = this.getTemplate();

            // 初始化模块
            this.initModule();
        },

        // 获取模板
        getTemplate: function() {
            return `
                <div class="container-fluid">
                    <!-- 搜索区域 -->
                    <div class="card mb-4">
                        <div class="card-body">
                            <h5 class="card-title mb-3">🔍 搜索图书</h5>
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

                    <!-- 图书列表 -->
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

                            <!-- 分页 -->
                            <div id="pagination" class="mt-4"></div>
                        </div>
                    </div>

                    <!-- 新增：借阅确认模态框 -->
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
                                        <!-- 图书信息会在这里动态显示 -->
                                    </div>

                                    <div class="alert alert-info mb-0 small">
                                        <i class="bi bi-info-circle me-1"></i>
                                        //借阅期限为30天，请按时归还
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

                    <!-- 新增：借阅结果模态框 -->
                    <div class="modal fade" id="borrowResultModal" tabindex="-1" aria-hidden="true">
                        <div class="modal-dialog modal-dialog-centered">
                            <div class="modal-content">
                                <div class="modal-header" id="resultModalHeader">
                                    <!-- 头部颜色会根据结果动态设置 -->
                                    <h5 class="modal-title" id="resultModalTitle"></h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                </div>
                                <div class="modal-body text-center py-4">
                                    <div class="mb-3" id="resultIcon" style="font-size: 3rem;">
                                        <!-- 图标会动态设置 -->
                                    </div>
                                    <h5 id="resultMessage" class="mb-3"></h5>
                                    <div id="resultDetails" class="text-muted small">
                                        <!-- 详细信息会动态设置 -->
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

        // 初始化模块
        initModule: function() {
            // 加载分类
            this.loadCategories();

            // 加载图书数据
            this.loadBooks();

            // 绑定事件
            this.bindEvents();
        },

        // 加载分类
        async loadCategories() {
            const categorySelect = document.getElementById('category');

            try {
                console.log('开始加载分类数据...');
                const response = await fetch('/api/categories');
                const result = await response.json();

                if (result.success && result.data) {
                    console.log('分类数据加载成功，数量:', result.data.length);

                    // 清空选项（保留"全部分类"）
                    while (categorySelect.options.length > 1) {
                        categorySelect.remove(1);
                    }

                    // 添加分类选项，排除ID=5的中转分类
                    result.data.forEach(category => {
                        if (category.categoryId !== 5) {
                            const option = document.createElement('option');
                            option.value = category.categoryId;
                            option.textContent = category.categoryName;
                            categorySelect.appendChild(option);
                        }
                    });

                    console.log('分类下拉框已更新，排除中转分类(ID=5)');
                } else {
                    console.error('分类API返回失败:', result.message);
                }
            } catch (error) {
                console.error('加载分类失败:', error);
                // 显示错误提示但不阻塞页面
                const errorOption = document.createElement('option');
                errorOption.value = "";
                errorOption.textContent = "加载分类失败";
                errorOption.disabled = true;
                categorySelect.appendChild(errorOption);
            }
        },

        // 绑定事件
        bindEvents: function() {
            const self = this;

            // 搜索表单提交
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

        // 加载图书
        async loadBooks() {
            const tbody = document.getElementById('bookTableBody');

            // 显示加载状态
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-4">
                        <div class="spinner-border spinner-border-sm" role="status"></div>
                        <span class="ms-2">加载图书中...</span>
                    </td>
                </tr>
            `;

            try {
                // 构建查询参数
                const params = new URLSearchParams({
                    page: this.state.currentPage - 1,  // 后端从0开始
                    size: this.state.pageSize
                });

                // 添加搜索参数
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

        // 渲染图书列表
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

                // 安全处理分类名称
                const categoryName = (book.category && book.category.categoryName) ?
                    book.category.categoryName : '-';

                // 转义特殊字符，防止单引号导致JS错误
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

        // 渲染分页
        renderPagination: function(totalPages, currentPage) {
            const container = document.getElementById('pagination');

            if (totalPages <= 1) {
                container.innerHTML = '';
                return;
            }

            console.log('渲染分页，总页数:', totalPages, '当前页(后端索引):', currentPage, '当前页(前端索引):', this.state.currentPage);

            // 后端返回的currentPage是从0开始的，我们需要转换为从1开始
            const currentPageOneBased = currentPage + 1;

            let html = `
                <nav aria-label="图书分页">
                    <ul class="pagination justify-content-center mb-0">
            `;

            // 上一页
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

            // 生成页码按钮
            const maxVisiblePages = 5; // 最多显示5个页码
            let startPage, endPage;

            if (totalPages <= maxVisiblePages) {
                // 总页数较少，显示所有页码
                startPage = 1;
                endPage = totalPages;
            } else {
                // 总页数较多，显示当前页附近的页码
                const halfVisible = Math.floor(maxVisiblePages / 2);
                startPage = Math.max(currentPageOneBased - halfVisible, 1);
                endPage = Math.min(startPage + maxVisiblePages - 1, totalPages);

                // 调整起始页码
                if (endPage - startPage + 1 < maxVisiblePages) {
                    startPage = Math.max(endPage - maxVisiblePages + 1, 1);
                }
            }

            // 添加页码按钮
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

            // 下一页
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

            // 绑定分页按钮事件
            this.bindPaginationEvents();
        },

        // 绑定分页事件
        bindPaginationEvents: function() {
            const container = document.getElementById('pagination');
            const self = this;

            if (!container) return;

            // 使用事件委托处理分页按钮点击
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

        // 切换页码
        changePage: function(page) {
            console.log('切换页码，旧页码:', this.state.currentPage, '新页码:', page);

            if (page < 1) {
                console.warn('页码不能小于1');
                return;
            }

            if (page === this.state.currentPage) {
                console.log('页码未变化，跳过');
                return;
            }

            this.state.currentPage = page;
            console.log('更新当前页码为:', this.state.currentPage);

            // 重新加载图书
            this.loadBooks();

            // 滚动到表格顶部
            const bookTable = document.querySelector('.table-responsive');
            if (bookTable) {
                bookTable.scrollIntoView({ behavior: 'smooth' });
            }
        },

        // 借阅图书
        borrowBook: async function(bookId) {
            console.log('尝试借阅图书，ID:', bookId);

            // 1. 验证用户登录状态
            if (!window.AppState || !window.AppState.currentUser) {
                alert('请先登录后再借阅图书');
                return;
            }

            const userId = window.AppState.currentUser.userId;
            if (!userId) {
                alert('用户信息错误，请重新登录');
                return;
            }

            // 2. 确认借阅
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
                    alert('✅ 借阅成功！请在30天内归还');

                    // 重新加载图书列表，更新可借数量
                    this.loadBooks();

                    // 显示借阅详情
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
                alert(`❌ 借阅失败: ${error.message}`);
            }
        }
    };


    // 立即注册模块函数到全局
    // 注册模块
    if (typeof window !== 'undefined') {
        console.log('注册图书查询模块...');

        // 方法1: 优先使用 window.registerModule（core.js 的系统）
        if (typeof window.registerModule === 'function') {
            window.registerModule('book-query', bookQueryModule);
            console.log('✓ 通过 window.registerModule 注册成功');
        }
        // 方法2: 兼容 window.modules
        else {
            if (!window.modules) {
                window.modules = {};
            }
            window.modules['book-query'] = bookQueryModule;
            console.log('✓ 通过 window.modules 注册成功');
        }

        // 全局借阅函数 - 必须单独暴露
        window.borrowBook = async function(bookId, bookName, author, publisher, category) {
            console.log('[全局函数] 借阅图书，ID:', bookId, '书名:', bookName);

            // 获取当前用户
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
                    // 借阅成功，显示成功模态框
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
                } else {
                    // 借阅失败
                    throw new Error(result.message || '借阅失败');
                }
            } catch (error) {
                console.error('借阅失败:', error);
                window.showBorrowResult('error', `借阅失败: ${error.message}`);
            }
        };

        // 全局借阅确认函数（使用模态框）
        window.showBorrowConfirmWithModal = function(event, bookId, bookName, author, publisher, category) {
            // 阻止默认事件
            if (event) {
                event.preventDefault();
                event.stopPropagation();
            }

            console.log('[全局函数] 显示借阅确认框，图书:', bookName, 'ID:', bookId);

            // 1. 验证用户登录状态
            const currentUser = window.AppState?.currentUser;
            if (!currentUser || !currentUser.userId) {
                window.showBorrowResult('error', '请先登录后再借阅图书');
                return;
            }

            // 2. 显示图书详情
            const bookInfoHtml = `
                <div class="book-info">
                    <p class="mb-1"><strong>书名：</strong><span class="text-primary">《${bookName}》</span></p>
                    <p class="mb-1"><strong>作者：</strong>${author}</p>
                    <p class="mb-1"><strong>出版社：</strong><span class="text-info">${publisher}</span></p>
                    <p class="mb-1"><strong>分类：</strong><span class="badge bg-secondary">${category}</span></p>
                </div>
            `;

            document.getElementById('bookConfirmInfo').innerHTML = bookInfoHtml;

            // 3. 设置确认按钮事件
            const confirmBtn = document.getElementById('confirmBorrowBtn');
            confirmBtn.onclick = function() {
                console.log('用户确认借阅，图书ID:', bookId);
                // 调用原来的借阅函数
                window.borrowBook(bookId, bookName, author, publisher, category);

                // 隐藏确认模态框
                const modal = bootstrap.Modal.getInstance(document.getElementById('borrowConfirmModal'));
                if (modal) {
                    modal.hide();
                }
            };

            // 4. 显示确认模态框
            const confirmModal = new bootstrap.Modal(document.getElementById('borrowConfirmModal'));
            confirmModal.show();
        };

        // 全局借阅结果显示函数
        window.showBorrowResult = function(type, message, details = null) {
            const modal = new bootstrap.Modal(document.getElementById('borrowResultModal'));
            const header = document.getElementById('resultModalHeader');
            const title = document.getElementById('resultModalTitle');
            const icon = document.getElementById('resultIcon');
            const msg = document.getElementById('resultMessage');
            const detailsEl = document.getElementById('resultDetails');

            // 根据类型设置样式
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

            // 显示模态框
            modal.show();

            // 模态框关闭时刷新列表
            const modalEl = document.getElementById('borrowResultModal');
            modalEl.addEventListener('hidden.bs.modal', function() {
                if (type === 'success') {
                    // 借阅成功，刷新图书列表
                    if (window.modules && window.modules['book-query'] &&
                        typeof window.modules['book-query'].loadBooks === 'function') {
                        window.modules['book-query'].loadBooks();
                    }
                }
            });
        };

        console.log('✓ book-query 模块和全局函数已注册完成');
    }
})();