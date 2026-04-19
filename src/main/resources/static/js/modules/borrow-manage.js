/**
 * 借阅管理模块
 * 管理员查看所有用户的借阅记录
 */
(function() {
    'use strict';

    console.log('📚 借阅管理模块加载中...');

    // 借阅管理对象
    const BorrowManage = {
        // 状态常量
        STATUS: {
            ALL: 'ALL',           // 全部
            BORROWED: 'BORROWED', // 在借中
            OVERDUE: 'OVERDUE',   // 已逾期
            RETURNED: 'RETURNED', // 已归还
            CANCELLED: 'CANCELLED' // 已取消
        },

        // 模块状态
        state: {
            currentPage: 1,
            pageSize: 10,
            filterStatus: 'ALL',
            borrowRecords: [],
            totalCount: 0,
            totalPages: 1,
            loading: false
        },

        // 初始化模块
        init: function() {
            console.log('🔄 初始化借阅管理模块');

            // 1. 渲染UI
            this.renderUI();

            // 2. 绑定事件
            this.bindEvents();

            // 3. 加载数据
            this.loadBorrowRecords();
        },

        // 渲染UI
        renderUI: function() {
            const app = document.getElementById('app');
            if (!app) {
                console.error('❌ 找不到app容器');
                return;
            }

            app.innerHTML = `
                <div class="container-fluid py-3">
                    <!-- 页面标题 -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <div>
                            <h2 class="mb-1">
                                <i class="bi bi-book me-2"></i>借阅管理
                            </h2>
                            <p class="text-muted mb-0">管理员 - 查看所有借阅记录</p>
                        </div>
                        <button class="btn btn-outline-secondary" id="refreshBtn">
                            <i class="bi bi-arrow-clockwise me-1"></i>刷新
                        </button>
                    </div>

                    <!-- 筛选区域 -->
                    <div class="card mb-4 filter-card">
                        <div class="card-body py-3">
                            <div class="row g-3 align-items-center">
                                <div class="col-md-4 col-lg-3">
                                    <label class="form-label fw-semibold mb-1">状态筛选</label>
                                    <select class="form-select" id="statusFilter">
                                        <option value="ALL">全部状态</option>
                                        <option value="BORROWED">在借中</option>
                                        <option value="OVERDUE">已逾期</option>
                                        <option value="RETURNED">已归还</option>
                                        <option value="CANCELLED">已取消</option>
                                    </select>
                                </div>
                                <div class="col-md-8 col-lg-9 d-flex align-items-end">
                                    <button class="btn btn-primary" id="applyFilter">
                                        <i class="bi bi-funnel me-1"></i>筛选
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 统计卡片 -->
                    <div class="row mb-4" id="statsContainer">
                        <!-- 统计卡片会动态加载 -->
                        <div class="col-md-3 mb-3">
                            <div class="card stat-card border-primary">
                                <div class="card-body text-center p-3">
                                    <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
                                    <span class="ms-2">加载中...</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 借阅记录表格 -->
                    <div class="card">
                        <div class="card-body p-0">
                            <div class="table-responsive">
                                <table class="table table-hover align-middle mb-0">
                                    <thead class="table-light">
                                        <tr>
                                            <th width="60">ID</th>
                                            <th width="120">用户名</th>
                                            <th width="120">真实姓名</th>
                                            <th width="200">图书信息</th>
                                            <th width="120">ISBN</th>
                                            <th width="150">借出时间</th>
                                            <th width="150">应还时间</th>
                                            <th width="100">状态</th>
                                            <th width="120">逾期天数</th>
                                            <th width="100" class="text-center">操作</th>
                                        </tr>
                                    </thead>
                                    <tbody id="borrowTableBody">
                                        <tr>
                                            <td colspan="10" class="text-center py-5 text-muted">
                                                <div class="spinner-border spinner-border-sm" role="status"></div>
                                                <span class="ms-2">正在加载借阅记录...</span>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>

                            <!-- 分页区域 -->
                            <div class="d-flex justify-content-between align-items-center p-3 border-top">
                                <div class="text-muted small" id="pageInfo">
                                    <span id="recordCount">0</span> 条记录
                                </div>
                                <nav id="pagination">
                                    <ul class="pagination pagination-sm mb-0">
                                        <li class="page-item disabled">
                                            <a class="page-link" href="#">上一页</a>
                                        </li>
                                        <li class="page-item active">
                                            <a class="page-link" href="#">1</a>
                                        </li>
                                        <li class="page-item disabled">
                                            <a class="page-link" href="#">下一页</a>
                                        </li>
                                    </ul>
                                </nav>
                            </div>
                        </div>
                    </div>

                    <!-- 详情模态框 -->
                    <div class="modal fade" id="detailModal" tabindex="-1">
                        <div class="modal-dialog modal-lg">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <h5 class="modal-title">借阅详情</h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                </div>
                                <div class="modal-body" id="modalBody">
                                    加载中...
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">关闭</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;

            // 设置筛选框初始值
            const statusFilter = document.getElementById('statusFilter');
            if (statusFilter) {
                statusFilter.value = this.state.filterStatus;
            }
        },

        // 获取认证token
        getAuthToken: function() {
            return localStorage.getItem('authToken') || sessionStorage.getItem('authToken') || '';
        },

        // 加载借阅记录
        loadBorrowRecords: function() {
            console.log(`📥 加载借阅记录，页码: ${this.state.currentPage}, 状态: ${this.state.filterStatus}`);

            this.state.loading = true;

            // 显示加载状态
            const tableBody = document.getElementById('borrowTableBody');
            if (tableBody) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="10" class="text-center py-5">
                            <div class="spinner-border spinner-border-sm" role="status"></div>
                            <span class="ms-2">正在加载借阅记录...</span>
                        </td>
                    </tr>
                `;
            }

            // 构建请求URL
            const token = this.getAuthToken();
            let url = `${API_BASE}/admin/borrows?page=${this.state.currentPage}&size=${this.state.pageSize}`;

            if (this.state.filterStatus !== 'ALL') {
                url += `&status=${this.state.filterStatus}`;
            }

            // 发送请求
            fetch(url, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            })
            .then(response => {
                if (!response.ok) {
                    if (response.status === 401) {
                        this.showLoginPrompt();
                        return null;
                    }
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                this.state.loading = false;

                if (!data) return;

                console.log('📄 借阅记录数据:', data);

                if (data && data.success) {
                    this.state.borrowRecords = data.data.list || [];
                    this.state.totalCount = data.data.total || 0;
                    this.state.totalPages = data.data.totalPages || 1;

                    this.renderTable();
                    this.renderPagination();

                    // 加载统计信息
                    this.loadStats();

                } else {
                    this.showError('加载借阅记录失败: ' + (data.message || '未知错误'));
                }
            })
            .catch(error => {
                this.state.loading = false;
                console.error('加载借阅记录失败:', error);
                this.showError('加载借阅记录失败: ' + error.message);
            });
        },

        // 加载统计信息
        loadStats: function() {
            console.log('📈 加载借阅统计信息...');

            const token = this.getAuthToken();

            fetch(`${API_BASE}/admin/borrows/stats`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            })
            .then(response => {
                if (!response.ok) {
                    console.warn('获取统计信息失败，使用列表数据统计');
                    this.calculateStatsFromList();
                    return null;
                }
                return response.json();
            })
            .then(data => {
                if (data && data.success) {
                    this.renderStats(data.data);
                } else {
                    this.calculateStatsFromList();
                }
            })
            .catch(error => {
                console.error('加载统计信息失败:', error);
                this.calculateStatsFromList();
            });
        },

        // 从列表数据计算统计
        calculateStatsFromList: function() {
            const records = this.state.borrowRecords;
            const total = records.length;

            const stats = {
                total: total,
                borrowed: records.filter(r => r.status === 'BORROWED').length,
                overdue: records.filter(r => r.status === 'OVERDUE').length,
                returned: records.filter(r => r.status === 'RETURNED').length
            };

            this.renderStats(stats);
        },

        // 渲染统计卡片
        renderStats: function(stats) {
            const statsContainer = document.getElementById('statsContainer');
            if (!statsContainer) return;

            statsContainer.innerHTML = `
                <div class="col-md-3 mb-3">
                    <div class="card stat-card border-primary">
                        <div class="card-body text-center p-3">
                            <h3 class="card-title text-primary mb-1">${stats.total || 0}</h3>
                            <p class="card-text text-muted small mb-0">总借阅数</p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 mb-3">
                    <div class="card stat-card border-info">
                        <div class="card-body text-center p-3">
                            <h3 class="card-title text-info mb-1">${stats.borrowed || 0}</h3>
                            <p class="card-text text-muted small mb-0">在借中</p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 mb-3">
                    <div class="card stat-card border-warning">
                        <div class="card-body text-center p-3">
                            <h3 class="card-title text-warning mb-1">${stats.overdue || 0}</h3>
                            <p class="card-text text-muted small mb-0">已逾期</p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 mb-3">
                    <div class="card stat-card border-success">
                        <div class="card-body text-center p-3">
                            <h3 class="card-title text-success mb-1">${stats.returned || 0}</h3>
                            <p class="card-text text-muted small mb-0">已归还</p>
                        </div>
                    </div>
                </div>
            `;
        },

        // 渲染表格
        renderTable: function() {
            const tableBody = document.getElementById('borrowTableBody');
            if (!tableBody) return;

            const records = this.state.borrowRecords;
            const total = this.state.totalCount;

            // 更新记录数显示
            const recordCountEl = document.getElementById('recordCount');
            if (recordCountEl) {
                recordCountEl.textContent = total;
            }

            if (records.length === 0) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="10" class="text-center py-5 text-muted">
                            <i class="bi bi-inbox fs-1 mb-3"></i>
                            <p class="mb-0">暂无借阅记录</p>
                            <small>${this.state.filterStatus !== 'ALL' ? '请尝试其他筛选条件' : '暂时没有借阅记录'}</small>
                        </td>
                    </tr>
                `;
                return;
            }

            let rows = '';
            records.forEach(record => {
                // 计算逾期天数
                const overdueDays = this.calculateOverdueDays(record);

                // 状态徽章
                const statusBadge = this.getStatusBadge(record.status, overdueDays);

                // 格式化时间
                const borrowTime = this.formatDateTime(record.borrowTime);
                const dueTime = this.formatDateTime(record.dueTime);
                const returnTime = this.formatDateTime(record.returnTime);

                rows += `
                    <tr>
                        <td>${record.borrowId || record.id || '-'}</td>
                        <td>${record.username || '-'}</td>
                        <td>${record.realName || '-'}</td>
                        <td>
                            <div class="fw-semibold">${record.bookTitle || '未知图书'}</div>
                            <small class="text-muted">${record.author || '未知作者'}</small>
                        </td>
                        <td><code>${record.isbn || '-'}</code></td>
                        <td><small>${borrowTime}</small></td>
                        <td><small>${dueTime}</small></td>
                        <td>${statusBadge}</td>
                        <td>
                            ${overdueDays > 0 ? `<span class="badge bg-danger">${overdueDays}天</span>` : '<span class="text-muted">-</span>'}
                        </td>
                        <td class="text-center">
                            <button class="btn btn-sm btn-outline-primary btn-action"
                                    title="查看详情"
                                    onclick="borrowManager.showDetail(${record.borrowId || record.id})">
                                <i class="bi bi-eye"></i>
                            </button>
                        </td>
                    </tr>
                `;
            });

            tableBody.innerHTML = rows;
        },

        // 计算逾期天数
        calculateOverdueDays: function(record) {
            if (record.status !== 'OVERDUE' && record.status !== 'BORROWED') {
                return 0;
            }

            const dueDate = new Date(record.dueTime);
            const now = new Date();

            if (now > dueDate) {
                const diffTime = Math.abs(now - dueDate);
                const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
                return diffDays;
            }

            return 0;
        },

        // 获取状态徽章
        getStatusBadge: function(status, overdueDays) {
            switch(status) {
                case 'BORROWED':
                    return overdueDays > 0
                        ? '<span class="badge bg-danger borrow-status-badge">已逾期</span>'
                        : '<span class="badge bg-primary borrow-status-badge">在借中</span>';
                case 'OVERDUE':
                    return '<span class="badge bg-danger borrow-status-badge">已逾期</span>';
                case 'RETURNED':
                    return '<span class="badge bg-success borrow-status-badge">已归还</span>';
                case 'CANCELLED':
                    return '<span class="badge bg-secondary borrow-status-badge">已取消</span>';
                default:
                    return '<span class="badge bg-light text-dark borrow-status-badge">未知</span>';
            }
        },

        // 格式化时间
        formatDateTime: function(dateTime) {
            if (!dateTime) return '-';

            try {
                const date = new Date(dateTime);
                if (isNaN(date.getTime())) return '-';

                return date.toLocaleString('zh-CN', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    hour12: false
                });
            } catch (e) {
                return '-';
            }
        },

        // 渲染分页
        renderPagination: function() {
            const pagination = document.getElementById('pagination');
            if (!pagination || this.state.totalPages <= 1) {
                if (pagination) {
                    pagination.innerHTML = '';
                }
                return;
            }

            const currentPage = this.state.currentPage;
            const totalPages = this.state.totalPages;
            const maxVisible = 5; // 最多显示5个页码

            let paginationHtml = '<ul class="pagination pagination-sm mb-0">';

            // 上一页
            if (currentPage > 1) {
                paginationHtml += `
                    <li class="page-item">
                        <a class="page-link" href="#" onclick="borrowManager.goToPage(${currentPage - 1}); return false;">
                            <i class="bi bi-chevron-left"></i>
                        </a>
                    </li>
                `;
            } else {
                paginationHtml += `
                    <li class="page-item disabled">
                        <span class="page-link">
                            <i class="bi bi-chevron-left"></i>
                        </span>
                    </li>
                `;
            }

            // 页码
            let startPage = Math.max(1, currentPage - Math.floor(maxVisible / 2));
            let endPage = Math.min(totalPages, startPage + maxVisible - 1);

            if (endPage - startPage + 1 < maxVisible) {
                startPage = Math.max(1, endPage - maxVisible + 1);
            }

            for (let i = startPage; i <= endPage; i++) {
                if (i === currentPage) {
                    paginationHtml += `<li class="page-item active"><span class="page-link">${i}</span></li>`;
                } else {
                    paginationHtml += `
                        <li class="page-item">
                            <a class="page-link" href="#" onclick="borrowManager.goToPage(${i}); return false;">
                                ${i}
                            </a>
                        </li>
                    `;
                }
            }

            // 下一页
            if (currentPage < totalPages) {
                paginationHtml += `
                    <li class="page-item">
                        <a class="page-link" href="#" onclick="borrowManager.goToPage(${currentPage + 1}); return false;">
                            <i class="bi bi-chevron-right"></i>
                        </a>
                    </li>
                `;
            } else {
                paginationHtml += `
                    <li class="page-item disabled">
                        <span class="page-link">
                            <i class="bi bi-chevron-right"></i>
                        </span>
                    </li>
                `;
            }

            paginationHtml += '</ul>';
            pagination.innerHTML = paginationHtml;
        },

        // 跳转页面
        goToPage: function(page) {
            if (page < 1 || page > this.state.totalPages || page === this.state.currentPage) {
                return;
            }

            this.state.currentPage = page;
            this.loadBorrowRecords();

            // 滚动到表格顶部
            const table = document.querySelector('.table-responsive');
            if (table) {
                table.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        },

        // 显示详情
        showDetail: function(borrowId) {
            console.log('查看借阅详情:', borrowId);

            const token = this.getAuthToken();

            fetch(`${API_BASE}/admin/borrows/${borrowId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            })
            .then(response => response.json())
            .then(data => {
                if (data && data.success) {
                    this.showDetailModal(data.data);
                } else {
                    this.showError('获取详情失败: ' + (data.message || '未知错误'));
                }
            })
            .catch(error => {
                console.error('获取详情失败:', error);
                this.showError('获取详情失败: ' + error.message);
            });
        },

        // 显示详情模态框
        showDetailModal: function(record) {
            const modalBody = document.getElementById('modalBody');
            if (!modalBody) return;

            // 计算逾期天数
            const overdueDays = this.calculateOverdueDays(record);

            // 格式化时间
            const borrowTime = this.formatDateTime(record.borrowTime);
            const dueTime = this.formatDateTime(record.dueTime);
            const returnTime = this.formatDateTime(record.returnTime);
            const createTime = this.formatDateTime(record.createTime);

            modalBody.innerHTML = `
                <div class="row">
                    <!-- 基本信息 -->
                    <div class="col-md-6">
                        <h6 class="fw-semibold mb-3">借阅信息</h6>
                        <table class="table table-sm">
                            <tbody>
                                <tr>
                                    <td class="fw-semibold" style="width: 120px;">借阅ID</td>
                                    <td>${record.borrowId || record.id || '-'}</td>
                                </tr>
                                <tr>
                                    <td class="fw-semibold">状态</td>
                                    <td>${this.getStatusBadge(record.status, overdueDays)}</td>
                                </tr>
                                <tr>
                                    <td class="fw-semibold">借出时间</td>
                                    <td>${borrowTime}</td>
                                </tr>
                                <tr>
                                    <td class="fw-semibold">应还时间</td>
                                    <td>${dueTime}</td>
                                </tr>
                                <tr>
                                    <td class="fw-semibold">实际归还</td>
                                    <td>${returnTime}</td>
                                </tr>
                                ${overdueDays > 0 ? `
                                <tr>
                                    <td class="fw-semibold text-danger">逾期天数</td>
                                    <td class="text-danger fw-semibold">${overdueDays} 天</td>
                                </tr>
                                ` : ''}
                            </tbody>
                        </table>
                    </div>

                    <!-- 用户信息 -->
                    <div class="col-md-6">
                        <h6 class="fw-semibold mb-3">用户信息</h6>
                        <table class="table table-sm">
                            <tbody>
                                <tr>
                                    <td class="fw-semibold" style="width: 120px;">用户名</td>
                                    <td>${record.username || '-'}</td>
                                </tr>
                                <tr>
                                    <td class="fw-semibold">真实姓名</td>
                                    <td>${record.realName || '-'}</td>
                                </tr>
                                <tr>
                                    <td class="fw-semibold">邮箱</td>
                                    <td>${record.email || '-'}</td>
                                </tr>
                                <tr>
                                    <td class="fw-semibold">电话</td>
                                    <td>${record.phone || '-'}</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <hr>

                <!-- 图书信息 -->
                <div class="row">
                    <div class="col-12">
                        <h6 class="fw-semibold mb-3">图书信息</h6>
                        <div class="card">
                            <div class="card-body">
                                <h6 class="card-title">${record.bookTitle || '未知图书'}</h6>
                                <div class="row">
                                    <div class="col-md-4">
                                        <small class="text-muted">ISBN</small><br>
                                        <strong>${record.isbn || '-'}</strong>
                                    </div>
                                    <div class="col-md-4">
                                        <small class="text-muted">作者</small><br>
                                        <strong>${record.author || '-'}</strong>
                                    </div>
                                    <div class="col-md-4">
                                        <small class="text-muted">出版社</small><br>
                                        <strong>${record.publisher || '-'}</strong>
                                    </div>
                                </div>
                                ${record.description ? `
                                <hr>
                                <small class="text-muted">简介</small>
                                <p class="mb-0">${record.description}</p>
                                ` : ''}
                            </div>
                        </div>
                    </div>
                </div>
            `;

            // 显示模态框
            const modal = new bootstrap.Modal(document.getElementById('detailModal'));
            modal.show();
        },

        // 应用筛选
        applyFilter: function() {
            const statusFilter = document.getElementById('statusFilter');
            if (statusFilter) {
                this.state.filterStatus = statusFilter.value;
                this.state.currentPage = 1; // 重置到第一页
                this.loadBorrowRecords();
            }
        },

        // 绑定事件
        bindEvents: function() {
            // 筛选按钮
            const applyFilterBtn = document.getElementById('applyFilter');
            if (applyFilterBtn) {
                applyFilterBtn.addEventListener('click', () => {
                    this.applyFilter();
                });
            }

            // 刷新按钮
            const refreshBtn = document.getElementById('refreshBtn');
            if (refreshBtn) {
                refreshBtn.addEventListener('click', () => {
                    this.state.currentPage = 1;
                    this.loadBorrowRecords();
                });
            }

            // 状态筛选框变化
            const statusFilter = document.getElementById('statusFilter');
            if (statusFilter) {
                // 可选：添加回车键筛选
                statusFilter.addEventListener('keypress', (e) => {
                    if (e.key === 'Enter') {
                        this.applyFilter();
                    }
                });
            }
        },

        // 显示错误
        showError: function(message) {
            const tableBody = document.getElementById('borrowTableBody');
            if (tableBody) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="10" class="text-center py-5 text-danger">
                            <i class="bi bi-exclamation-triangle me-2"></i>
                            ${message}
                            <button class="btn btn-sm btn-outline-primary ms-3" onclick="borrowManager.loadBorrowRecords()">
                                <i class="bi bi-arrow-clockwise me-1"></i>重试
                            </button>
                        </td>
                    </tr>
                `;
            }

            // 也更新统计区域
            const statsContainer = document.getElementById('statsContainer');
            if (statsContainer) {
                statsContainer.innerHTML = `
                    <div class="col-md-12">
                        <div class="alert alert-warning mb-0">
                            <i class="bi bi-exclamation-triangle me-2"></i>
                            统计信息加载失败
                        </div>
                    </div>
                `;
            }
        },

        // 显示登录提示
        showLoginPrompt: function() {
            if (confirm('登录已过期，需要重新登录。点击确定前往登录页面')) {
                window.location.href = 'login.html';
            }
        }
    };

    // 全局访问
    window.borrowManager = BorrowManage;

    // 导出初始化函数
    window.initBorrowManage = function() {
        BorrowManage.init();
    };

    console.log('✅ 借阅管理模块加载完成');
})();