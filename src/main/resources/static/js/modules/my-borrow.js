//我的借阅模块
(function() {
    'use strict';
    console.log('=== my-borrow.js 开始执行 ===');
    const myBorrowModule = {
        state: {
            currentPage: 1,
            pageSize: 10,
            filterStatus: 'all',
            searchKeyword: '',
            records: [],
            totalPages: 1,
            totalItems: 0,
            loading: false
        },
        config: {
            apiBase: '/api',
            statusMap: {
                'BORROWED': { text: '借阅中', class: 'primary' },
                'RETURNED': { text: '已归还', class: 'success' },
                'RENEWED': { text: '已续借', class: 'warning' },
                'OVERDUE': { text: '已逾期', class: 'danger' }
            }
        },
        moduleContent: null,
        render: function() {
            console.log('my-borrow模块渲染函数被调用');
            this.moduleContent = document.getElementById('moduleContent');
            this.moduleContent.innerHTML = this.getTemplate();
            this.initModule();
        },
        getTemplate: function() {
            return `
                <div class="container-fluid py-4">
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <div>
                            <h2 class="mb-1">我的借阅记录</h2>
                            <p class="text-muted mb-0">查看和管理您的图书借阅</p>
                        </div>
                    </div>
                    <div class="card shadow-sm mb-4">
                        <div class="card-body">
                            <div class="row g-3">
                                <div class="col-md-6 col-lg-4">
                                    <label class="form-label">状态筛选</label>
                                    <select class="form-select" id="statusFilter">
                                        <option value="all">全部状态</option>
                                        <option value="BORROWED">借阅中</option>
                                        <option value="RETURNED">已归还</option>
                                        <option value="RENEWED">已续借</option>
                                    </select>
                                </div>
                                <div class="col-md-6 col-lg-5">
                                    <label class="form-label">搜索图书</label>
                                    <div class="input-group">
                                        <input type="text"
                                               class="form-control"
                                               id="searchKeyword"
                                               placeholder="输入书名、作者或图书编号..."
                                               value="${this.state.searchKeyword}">
                                        <button class="btn btn-outline-primary" type="button" id="searchBtn">
                                            <i class="bi bi-search"></i> 搜索
                                        </button>
                                    </div>
                                </div>
                                <div class="col-md-6 col-lg-3 d-flex align-items-end">
                                    <button class="btn btn-outline-secondary me-2" id="resetBtn">
                                        <i class="bi bi-arrow-clockwise"></i> 重置
                                    </button>
                                    <button class="btn btn-primary" id="refreshBtn">
                                        <i class="bi bi-arrow-repeat"></i> 刷新
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="row mb-4" id="statsArea">
                        <div class="col-md-3">
                            <div class="card border-primary">
                                <div class="card-body text-center">
                                    <h3 class="card-title text-primary" id="totalBorrow">0</h3>
                                    <p class="card-text text-muted">总借阅数</p>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="card border-success">
                                <div class="card-body text-center">
                                    <h3 class="card-title text-success" id="currentBorrow">0</h3>
                                    <p class="card-text text-muted">当前借阅</p>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="card border-info">
                                <div class="card-body text-center">
                                    <h3 class="card-title text-info" id="currentPageCount">0</h3>
                                    <p class="card-text text-muted">当前页记录</p>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="card border-secondary">
                                <div class="card-body text-center">
                                    <h3 class="card-title text-secondary" id="overdueCount">0</h3>
                                    <p class="card-text text-muted">逾期数量</p>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="card shadow-sm">
                        <div class="card-header">
                            <h5 class="mb-0">借阅记录列表</h5>
                        </div>
                        <div class="card-body p-0">
                            <div class="table-responsive">
                                <table class="table table-hover align-middle mb-0">
                                    <thead class="table-light">
                                        <tr>
                                            <th width="50">#</th>
                                            <th>图书信息</th>
                                            <th width="150">借阅时间</th>
                                            <th width="150">应还时间</th>
                                            <th width="150">归还时间</th>
                                            <th width="100">剩余天数</th>
                                            <th width="100">状态</th>
                                            <th width="120">操作</th>
                                        </tr>
                                    </thead>
                                    <tbody id="recordsTableBody">
                                        <tr>
                                            <td colspan="8" class="text-center py-5">
                                                <div class="spinner-border spinner-border-sm" role="status"></div>
                                                <span class="ms-2">正在加载借阅记录...</span>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                            <div id="paginationContainer" class="d-flex justify-content-between align-items-center p-3">
                                <div id="pageInfo" class="text-muted">正在加载...</div>
                                <nav id="pagination">
                                </nav>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        },

        initModule: function() {
            console.log('初始化my-borrow模块');
            this.state = {
                currentPage: 1,
                pageSize: 10,
                filterStatus: 'all',
                searchKeyword: '',
                records: [],
                totalPages: 1,
                totalItems: 0,
                loading: false
            };
            this.bindEvents();
            this.loadBorrowRecords();
        },

        bindEvents: function() {
            const self = this;
            const searchBtn = document.getElementById('searchBtn');
            if (searchBtn) {
                searchBtn.addEventListener('click', function() {
                    self.state.searchKeyword = document.getElementById('searchKeyword').value.trim();
                    self.state.currentPage = 1;
                    self.loadBorrowRecords();
                });
            }
            const searchKeywordInput = document.getElementById('searchKeyword');
            if (searchKeywordInput) {
                searchKeywordInput.addEventListener('keypress', function(e) {
                    if (e.key === 'Enter') {
                        self.state.searchKeyword = e.target.value.trim();
                        self.state.currentPage = 1;
                        self.loadBorrowRecords();
                    }
                });
            }
            const statusFilter = document.getElementById('statusFilter');
            if (statusFilter) {
                statusFilter.addEventListener('change', function(e) {
                    self.state.filterStatus = e.target.value;
                    self.state.currentPage = 1;
                    self.loadBorrowRecords();
                });
            }
            const resetBtn = document.getElementById('resetBtn');
            if (resetBtn) {
                resetBtn.addEventListener('click', function() {
                    self.state.searchKeyword = '';
                    self.state.filterStatus = 'all';
                    self.state.currentPage = 1;

                    const searchKeyword = document.getElementById('searchKeyword');
                    const statusFilter = document.getElementById('statusFilter');

                    if (searchKeyword) searchKeyword.value = '';
                    if (statusFilter) statusFilter.value = 'all';

                    self.loadBorrowRecords();
                });
            }
            const refreshBtn = document.getElementById('refreshBtn');
            if (refreshBtn) {
                refreshBtn.addEventListener('click', function() {
                    self.loadBorrowRecords();
                });
            }
        },
        loadBorrowRecords: async function() {
            try {
                const page = this.state.currentPage - 1;
                const size = this.state.pageSize;
                const status = this.state.filterStatus;
                const keyword = this.state.searchKeyword;
                let url = `/api/borrow/records?page=${page}&size=${size}`;
                if (status && status !== 'all') {
                    url += `&status=${encodeURIComponent(status)}`;
                }
                if (keyword) {
                    url += `&keyword=${encodeURIComponent(keyword)}`;
                }

                console.log('正在请求:', url);
                const response = await fetch(url);
                if (response.status === 401) {
                    this.showMessage("请先登录！", "warning");
                    setTimeout(() => {
                        window.location.href = 'login.html';
                    }, 1500);
                    return;
                }

                if (!response.ok) {
                    throw new Error(`HTTP错误: ${response.status}`);
                }

                const result = await response.json();
                if (result.success) {
                    const data = result.data;
                    console.log('后端返回数据详情:', {
                        记录数: data.records ? data.records.length : 0,
                        总条数: data.totalItems,
                        总页数: data.totalPages,
                        当前页: data.currentPage
                    });
                    this.state.records = data.records || [];
                    this.state.totalItems = data.totalItems || 0;
                    this.state.totalPages = data.totalPages || 1;
                    this.state.currentPage = (data.currentPage || 0) + 1;  // 转换为前端页码（从1开始）

                    console.log('更新后的状态:', {
                        记录数: this.state.records.length,
                        总条数: this.state.totalItems,
                        总页数: this.state.totalPages,
                        当前页: this.state.currentPage
                    });
                    this.renderTable();
                    this.renderPagination();
                    this.renderStats();
                } else {
                    this.showMessage(result.message || "加载失败！", "error");
                }
            } catch (error) {
                console.error("加载借阅记录失败:", error);
                this.showMessage("网络错误: " + error.message, "error");
            }
        },
        renderTable: function() {
            const tbody = document.getElementById('recordsTableBody');
            if (!tbody) return;

            if (!this.state.records || this.state.records.length === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="8" class="text-center py-5 text-muted">
                            <i class="bi bi-journal-x" style="font-size: 3rem; opacity: 0.3;"></i>
                            <div class="mt-3">暂无借阅记录</div>
                            <small class="text-muted">快去借阅图书吧！</small>
                        </td>
                    </tr>
                `;
                return;
            }

            let html = '';
            this.state.records.forEach((record, index) => {
                const recordNum = (this.state.currentPage - 1) * this.state.pageSize + index + 1;
                const status = record.status || 'BORROWED';
                const isOverdue = this.isRecordOverdue(record);
                const daysRemaining = this.getDaysRemaining(record);

                html += `
                    <tr>
                        <td class="text-center">${recordNum}</td>
                        <td>
                            <div class="d-flex align-items-start">
                                <div class="flex-grow-1">
                                    <h6 class="mb-1">${record.bookname || '未知图书'}</h6>
                                    <div class="small text-muted">
                                        <div><strong>图书编号：</strong> ${record.bookId || 'N/A'}</div>
                                        <div>作者: ${record.author || '-'}</div>
                                        <div>出版社: ${record.publisher || '-'}</div>
                                    </div>
                                </div>
                            </div>
                        </td>
                        <td>${this.formatDate(record.borrowTime)}</td>
                        <td>
                            ${this.formatDate(record.dueTime)}
                            ${isOverdue ? '<div class="badge bg-danger mt-1">已逾期</div>' : ''}
                        </td>
                        <td>${record.returnTime ? this.formatDate(record.returnTime) : '-'}</td>
                        <td>${this.getDaysRemainingHtml(daysRemaining)}</td>
                        <td>${this.getStatusBadge(status)}</td>
                        <td>
                            <div class="btn-group btn-group-sm" role="group">
                                ${status === 'BORROWED' || status === 'RENEWED' ? `
                                    <button type="button"
                                            class="btn btn-outline-success return-btn"
                                            data-record-id="${record.recordId}"
                                            data-book-id="${record.bookId}"
                                            data-book-name="${record.bookname || '未知图书'}">
                                        <i class="bi bi-arrow-return-left"></i> 还书
                                    </button>
                                ` : `
                                    <button type="button" class="btn btn-outline-secondary" disabled>
                                        <i class="bi bi-arrow-return-left"></i> 还书
                                    </button>
                                `}

                                ${status === 'BORROWED' && !isOverdue ? `
                                    <button type="button"
                                            class="btn btn-outline-warning renew-btn"
                                            data-record-id="${record.recordId}"
                                            data-book-id="${record.bookId}"
                                            data-book-name="${record.bookname || '未知图书'}"
                                            data-due-time="${record.dueTime ? this.formatDateForAttribute(record.dueTime) : ''}">
                                        <i class="bi bi-arrow-clockwise"></i> 续借
                                    </button>
                                ` : `
                                    <button type="button" class="btn btn-outline-secondary" disabled>
                                        <i class="bi bi-arrow-clockwise"></i> 续借
                                    </button>
                                `}
                            </div>
                        </td>
                    </tr>
                `;
            });
            tbody.innerHTML = html;
            this.bindRowEvents();
        },
        bindRowEvents: function() {
            const self = this;
            document.querySelectorAll('.return-btn').forEach(btn => {
                btn.addEventListener('click', function(e) {
                    e.preventDefault();
                    const recordId = this.dataset.recordId;
                    const bookId = this.dataset.bookId;
                    const bookName = this.dataset.bookName;
                    self.showReturnModal(recordId, bookId, bookName);
                });
            });
            document.querySelectorAll('.renew-btn').forEach(btn => {
                btn.addEventListener('click', function(e) {
                    e.preventDefault();
                    const recordId = this.dataset.recordId;
                    const bookId = this.dataset.bookId;
                    const bookName = this.dataset.bookName;
                    const dueTimeStr = this.dataset.dueTime;
                    console.log('续借按钮数据:', {
                        recordId,
                        bookId,
                        bookName,
                        dueTimeStr,
                        dataset: this.dataset
                    });

                    self.showRenewModal(recordId, bookId, bookName, dueTimeStr);
                });
            });
        },
        renderStats: function() {
            const totalBorrow = document.getElementById('totalBorrow');
            const currentBorrow = document.getElementById('currentBorrow');
            const currentPageCount = document.getElementById('currentPageCount');
            const overdueCountEl = document.getElementById('overdueCount');

            if (totalBorrow) totalBorrow.textContent = this.state.totalItems || 0;
            if (currentPageCount) currentPageCount.textContent = this.state.records.length || 0;
            let currentCount = 0;
            let overdueCount = 0;
            this.state.records.forEach(record => {
                if (record.status === 'BORROWED' || record.status === 'RENEWED') {
                    currentCount++;
                    if (this.isRecordOverdue(record)) {
                        overdueCount++;
                    }
                }
            });
            if (currentBorrow) currentBorrow.textContent = currentCount;
            if (overdueCountEl) overdueCountEl.textContent = overdueCount;
            const user = window.AppState?.currentUser || this.getCurrentUser();
            const userNameEl = document.getElementById('currentUserName');
            if (userNameEl && user) {
                userNameEl.textContent = user.userName || '用户';
            }
        },
        renderPagination: function() {
            const container = document.getElementById('paginationContainer');
            const pageInfo = document.getElementById('pageInfo');
            const pagination = document.getElementById('pagination');

            if (!container || !pageInfo || !pagination) {
                console.error('分页元素未找到', {
                    container: !!container,
                    pageInfo: !!pageInfo,
                    pagination: !!pagination
                });
                return;
            }
            const startRecord = Math.min(
                (this.state.currentPage - 1) * this.state.pageSize + 1,
                this.state.totalItems
            );
            const endRecord = Math.min(
                this.state.currentPage * this.state.pageSize,
                this.state.totalItems
            );

            console.log('渲染分页信息:', {
                当前页: this.state.currentPage,
                每页大小: this.state.pageSize,
                总条数: this.state.totalItems,
                开始记录: startRecord,
                结束记录: endRecord
            });
            pageInfo.innerHTML = `显示第 ${startRecord} 到 ${endRecord} 条，共 ${this.state.totalItems} 条记录`;
            this.renderPageButtons();
        },
        renderPageButtons: function() {
            const pagination = document.getElementById('pagination');
            if (!pagination) return;

            const self = this;
            let html = '<ul class="pagination pagination-sm mb-0">';

            if (this.state.currentPage > 1) {
                html += `
                    <li class="page-item">
                        <a class="page-link" href="#" data-page="${this.state.currentPage - 1}">
                            <i class="bi bi-chevron-left"></i>
                        </a>
                    </li>
                `;
            } else {
                html += `
                    <li class="page-item disabled">
                        <span class="page-link">
                            <i class="bi bi-chevron-left"></i>
                        </span>
                    </li>
                `;
            }

            const maxPages = 5;
            let startPage = Math.max(1, this.state.currentPage - Math.floor(maxPages / 2));
            let endPage = Math.min(this.state.totalPages, startPage + maxPages - 1);

            if (endPage - startPage + 1 < maxPages) {
                startPage = Math.max(1, endPage - maxPages + 1);
            }

            for (let i = startPage; i <= endPage; i++) {
                if (i === this.state.currentPage) {
                    html += `
                        <li class="page-item active">
                            <span class="page-link">${i}</span>
                        </li>
                    `;
                } else {
                    html += `
                        <li class="page-item">
                            <a class="page-link" href="#" data-page="${i}">${i}</a>
                        </li>
                    `;
                }
            }

            if (this.state.currentPage < this.state.totalPages) {
                html += `
                    <li class="page-item">
                        <a class="page-link" href="#" data-page="${this.state.currentPage + 1}">
                            <i class="bi bi-chevron-right"></i>
                        </a>
                    </li>
                `;
            } else {
                html += `
                    <li class="page-item disabled">
                        <span class="page-link">
                            <i class="bi bi-chevron-right"></i>
                        </span>
                    </li>
                `;
            }

            html += '</ul>';
            pagination.innerHTML = html;
            this.bindPaginationEvents();
        },
        bindPaginationEvents: function() {
            const self = this;
            const pagination = document.getElementById('pagination');

            if (!pagination) return;

            pagination.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();

                const target = e.target.closest('.page-link');
                if (!target) return;

                const page = target.getAttribute('data-page');
                if (page && !isNaN(page)) {
                    const pageNum = parseInt(page);
                    if (pageNum !== self.state.currentPage) {
                        console.log('点击页码:', pageNum);
                        self.state.currentPage = pageNum;
                        self.loadBorrowRecords();
                    }
                }
            });
        },
        getCurrentUser: function() {
            try {
                const userData = localStorage.getItem('currentUser');
                if (userData) return JSON.parse(userData);

                const urlParams = new URLSearchParams(window.location.search);
                const userId = urlParams.get('userId');
                const userName = urlParams.get('userName');

                if (userId) return { userId: userId, userName: userName || '用户' };
            } catch (error) {
                console.error('获取用户信息失败:', error);
            }
            return null;
        },

        formatDate: function(dateString) {
            if (!dateString) return '-';
            const date = new Date(dateString);
            return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
        },
        parseDateTime: function(dateTimeStr) {
            if (!dateTimeStr) return null;
            if (dateTimeStr.includes('Z') || dateTimeStr.includes('+')) {
                return new Date(dateTimeStr);
            }
            if (dateTimeStr.includes('T')) {
                return new Date(dateTimeStr + '+08:00');  // 添加北京时间时区
            }
            return new Date(dateTimeStr);
        },
        isRecordOverdue: function(record) {
            if (record.status === 'RETURNED') {
                return false;
            }
            if (record.status === 'OVERDUE') {
                return true;
            }
            if (!record.dueTime) return false;

            const dueDate = this.parseDateTime(record.dueTime);
            const now = new Date();

            if (record.status === 'BORROWED' || record.status === 'RENEWED') {
                return now > dueDate;
            }

            return false;
        },

        getDaysRemaining: function(record) {
            if (record.status === 'RETURNED') {
                return 0;
            }
            if (!record.dueTime) return null;

            const dueDate = this.parseDateTime(record.dueTime);
            const now = new Date();
            const diffTime = dueDate - now;
            return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        },

        getDaysRemainingHtml: function(days) {
            if (days === null) return '<span class="text-muted">-</span>';
            if (days < 0) return `<span class="text-danger">已逾期 ${Math.abs(days)} 天</span>`;
            if (days === 0) return '<span class="text-warning">0天</span>';
            if (days <= 3) return `<span class="text-warning">${days} 天</span>`;
            return `<span class="text-success">${days} 天</span>`;
        },

        getStatusBadge: function(status) {
            const statusInfo = this.config.statusMap[status] || { text: status, class: 'secondary' };
            return `<span class="badge bg-${statusInfo.class}">${statusInfo.text}</span>`;
        },

        showMessage: function(message, type) {
            if (window.showToast) {
                window.showToast(message, type);
            } else {
                alert(message);
            }
        },

        showError: function(message) {
            this.showMessage(message, 'error');
        },

        showReturnModal: async function(recordId, bookId, bookName) {
            if (!confirm(`确定要归还《${bookName}》吗？`)) {
                return;
            }

            try {
                this.showMessage('正在处理还书请求...', 'info');

                const response = await fetch('/api/borrow/return', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        recordId: recordId
                    })
                });

                if (!response.ok) {
                    throw new Error(`HTTP错误: ${response.status}`);
                }

                const result = await response.json();

                if (result.success) {
                    this.showMessage(`《${bookName}》归还成功！`, "success");
                    this.loadBorrowRecords();
                } else {
                    this.showMessage(result.message || "还书失败！", "error");
                }
            } catch (error) {
                console.error("还书失败:", error);
                this.showMessage("还书失败: " + error.message, "error");
            }
        },

        showRenewModal: function(recordId, bookId, bookName, dueTimeStr) {
            const self = this;
            console.log('showRenewModal 参数:', {
                recordId,
                bookId,
                bookName,
                dueTimeStr
            });
            const formatDateDisplay = (dateStr) => {
                if (!dateStr || dateStr.trim() === '') {
                    return '未知日期';
                }

                try {
                    let date;
                    if (dateStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
                        const [year, month, day] = dateStr.split('-').map(Number);
                        date = new Date(year, month - 1, day);
                    }
                    else if (dateStr.match(/^\d{4}\/\d{2}\/\d{2}$/)) {
                        const [year, month, day] = dateStr.split('/').map(Number);
                        date = new Date(year, month - 1, day);
                    }
                    else {
                        date = new Date(dateStr);
                    }

                    if (isNaN(date.getTime())) {
                        console.error('无法解析日期:', dateStr);
                        return '无效日期';
                    }

                    return date.getFullYear() + '/' +
                           (date.getMonth() + 1).toString().padStart(2, '0') + '/' +
                           date.getDate().toString().padStart(2, '0');
                } catch (error) {
                    console.error('日期格式化错误:', error, dateStr);
                    return '无效日期';
                }
            };
            const calculateNewDueTime = (dueStr) => {
                if (!dueStr || dueStr.trim() === '') {
                    return '';
                }

                try {
                    let date;
                    if (dueStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
                        const [year, month, day] = dueStr.split('-').map(Number);
                        date = new Date(year, month - 1, day);
                    }
                    else if (dueStr.match(/^\d{4}\/\d{2}\/\d{2}$/)) {
                        const [year, month, day] = dueStr.split('/').map(Number);
                        date = new Date(year, month - 1, day);
                    }
                    else {
                        date = new Date(dueStr);
                    }

                    if (isNaN(date.getTime())) {
                        console.error('无法解析应还日期:', dueStr);
                        return '';
                    }
                    const newDate = new Date(date);
                    newDate.setDate(newDate.getDate() + 30);

                    return newDate.getFullYear() + '/' +
                           (newDate.getMonth() + 1).toString().padStart(2, '0') + '/' +
                           newDate.getDate().toString().padStart(2, '0');
                } catch (error) {
                    console.error('计算新日期错误:', error, dueStr);
                    return '';
                }
            };

            const currentDueTime = formatDateDisplay(dueTimeStr);
            const newDueTime = calculateNewDueTime(dueTimeStr);

            console.log('续借对话框信息:', {
                图书名称: bookName,
                当前应还时间: currentDueTime,
                新应还时间: newDueTime
            });

            if (currentDueTime === '无效日期' || newDueTime === '' || newDueTime === '无效日期') {
                this.showMessage('日期格式错误，无法续借', 'error');
                return;
            }

            if (confirm(`确定要续借《${bookName}》吗？\n续借后应还时间将从 ${currentDueTime} 延长至 ${newDueTime}。`)) {
                this.renewBook(recordId);
            }
        },
        renewBook: function(recordId) {
            const self = this;
            const renewBtn = document.querySelector(`.renew-btn[data-record-id="${recordId}"]`);
            if (renewBtn) {
                renewBtn.disabled = true;
                renewBtn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> 处理中';
            }
            fetch('/api/borrow/renew', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    recordId: recordId
                })
            })
            .then(response => {
                if (response.status === 401) {
                    self.showMessage("请先登录！", "warning");
                    setTimeout(() => {
                        window.location.href = '/login';
                    }, 1500);
                    throw new Error('未登录');
                }
                return response.json();
            })
            .then(data => {
                if (data.success) {
                    self.showMessage("续借成功！新的应还时间为：" + self.formatDate(data.data.newDueTime), "success");
                    setTimeout(() => {
                        self.loadBorrowRecords();
                    }, 1500);
                } else {
                    self.showMessage("续借失败：" + data.message, "error");
                    if (renewBtn) {
                        renewBtn.disabled = false;
                        renewBtn.innerHTML = '<i class="bi bi-arrow-clockwise"></i> 续借';
                    }
                }
            })
            .catch(error => {
                if (error.message !== '未登录') {
                    console.error('Error:', error);
                    self.showMessage("网络错误，请稍后重试", "error");
                    if (renewBtn) {
                        renewBtn.disabled = false;
                        renewBtn.innerHTML = '<i class="bi bi-arrow-clockwise"></i> 续借';
                    }
                }
            });
        },
        formatDate: function(dateStr) {
            if (!dateStr) return '';
            const date = new Date(dateStr);
            return date.getFullYear() + '/' +
                   (date.getMonth() + 1).toString().padStart(2, '0') + '/' +
                   date.getDate().toString().padStart(2, '0');
        },
        formatDateForAttribute: function(dateInput) {
            if (!dateInput) return '';

            try {
                const date = new Date(dateInput);
                if (isNaN(date.getTime())) {
                    return '';
                }
                return date.toISOString().split('T')[0];
            } catch (error) {
                console.error('日期格式化错误:', error, dateInput);
                return '';
            }
        },
        onDestroy: function() {
            console.log('清理my-borrow模块资源');
        }
    };
    if (typeof window !== 'undefined') {
        console.log('开始注册我的借阅模块...');
        if (typeof window.registerModule === 'function') {
            window.registerModule('my-borrow', myBorrowModule);
            console.log('通过 window.registerModule 注册成功');
        }
        else {
            if (!window.modules) {
                window.modules = {};
            }
            window.modules['my-borrow'] = myBorrowModule;
            console.log('通过 window.modules 注册成功');
        }
        if (typeof ModuleRegistry !== 'undefined') {
            ModuleRegistry['my-borrow'] = myBorrowModule;
            console.log('直接注册到 ModuleRegistry 成功');
        }

        console.log('my-borrow 模块注册完成，模块对象:', Object.keys(myBorrowModule));
    }
    console.log('my-borrow.js 执行完成');
    document.addEventListener('DOMContentLoaded', function() {
        console.log('my-borrow.js: DOM加载完成');
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.get('module') === 'my-borrow') {
            console.log('检测到URL参数包含my-borrow，尝试渲染');
            setTimeout(() => {
                if (typeof window.loadModule === 'function') {
                    window.loadModule('my-borrow');
                } else if (ModuleRegistry && ModuleRegistry['my-borrow']) {
                    console.log('从ModuleRegistry直接渲染my-borrow');
                    ModuleRegistry['my-borrow'].render();
                }
            }, 100);
        }
    });
    console.log('=== my-borrow.js 执行结束 ===');
})();