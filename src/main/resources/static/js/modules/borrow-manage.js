/**
 * 管理员借阅管理模块
 * 修改说明：
 * 1. 修复模块加载逻辑，与core.js模块系统匹配
 * 2. API地址改为相对路径
 */
(function() {
    'use strict';

    console.log('=== borrow-manage.js 加载开始 ===');

    const borrowManageModule = {
        // 模块配置
        config: {
            // 使用相对路径，避免跨域
            apiBase: '/api',
            statusMap: {
                'BORROWED': { text: '借阅中', class: 'primary' },
                'RETURNED': { text: '已归还', class: 'success' },
                'OVERDUE': { text: '已逾期', class: 'danger' },
                'RENEWED': { text: '已续借', class: 'info' }
            }
        },

        // 模块状态
        state: {
            currentPage: 1,
            pageSize: 10,
            filterStatus: 'all',
            records: [],
            totalPages: 1,
            totalItems: 0,
            loading: false,
            stats: {
                total: 0,
                borrowed: 0,
                overdue: 0,
                returned: 0,
                renewed: 0
            }
        },

        // 核心：渲染方法 - 返回HTML字符串
        render: function() {
            console.log(' 渲染管理员借阅管理模块');
            // 正确的：先返回HTML字符串
            return `
                <div class="container-fluid">
                    <!-- 页面标题 -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <div>
                            <h4 class="mb-0">
                                <i class="bi bi-book me-2"></i>借阅管理
                            </h4>
                            <p class="text-muted mb-0">管理所有用户的图书借阅记录</p>
                        </div>
                    </div>

                    <!-- 统计卡片 -->
                    <div class="row g-4 mb-4">
                        <!-- 总借阅记录 -->
                        <div class="col-md-6 col-lg-3">
                            <div class="card border-primary border-2">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between">
                                        <div>
                                            <h6 class="text-muted">总借阅记录</h6>
                                            <h3 id="statTotal" class="mb-0">0</h3>
                                        </div>
                                        <div class="align-self-center">
                                            <i class="bi bi-journal-text text-primary" style="font-size: 2rem;"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- 借阅中 -->
                        <div class="col-md-6 col-lg-3">
                            <div class="card border-warning border-2">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between">
                                        <div>
                                            <h6 class="text-muted">借阅中</h6>
                                            <h3 id="statBorrowed" class="mb-0">0</h3>
                                        </div>
                                        <div class="align-self-center">
                                            <i class="bi bi-book-half text-warning" style="font-size: 2rem;"></i>
                                        </div>
                                    </div>
                                    <div class="mt-2">
                                        <span class="text-muted">正在借阅的图书</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- 已逾期 -->
                        <div class="col-md-6 col-lg-3">
                            <div class="card border-danger border-2">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between">
                                        <div>
                                            <h6 class="text-muted">已逾期</h6>
                                            <h3 id="statOverdue" class="mb-0">0</h3>
                                        </div>
                                        <div class="align-self-center">
                                            <i class="bi bi-exclamation-triangle text-danger" style="font-size: 2rem;"></i>
                                        </div>
                                    </div>
                                    <div class="mt-2">
                                        <span class="text-muted">已过期的借阅</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- 已归还 -->
                        <div class="col-md-6 col-lg-3">
                            <div class="card border-success border-2">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between">
                                        <div>
                                            <h6 class="text-muted">已归还</h6>
                                            <h3 id="statReturned" class="mb-0">0</h3>
                                        </div>
                                        <div class="align-self-center">
                                            <i class="bi bi-check-circle text-success" style="font-size: 2rem;"></i>
                                        </div>
                                    </div>
                                    <div class="mt-2">
                                        <span class="text-muted">已归还的图书</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 筛选和搜索 -->
                    <div class="card shadow-sm mb-4">
                        <div class="card-body">
                            <div class="row g-3">
                                <!-- 状态筛选 -->
                                <div class="col-md-8">
                                    <label class="form-label">
                                        <i class="bi bi-filter-circle me-1"></i>状态筛选
                                    </label>
                                    <select class="form-select" id="statusFilter">
                                        <option value="all">全部状态</option>
                                        <option value="BORROWED">借阅中</option>
                                        <option value="RETURNED">已归还</option>
                                        <option value="OVERDUE">已逾期</option>
                                        <option value="RENEWED">已续借</option>
                                    </select>
                                </div>

                                <!-- 操作按钮 -->
                                <div class="col-md-4 d-flex align-items-end">
                                    <button class="btn btn-outline-secondary me-2" id="resetBtn" title="重置筛选条件">
                                        <i class="bi bi-arrow-clockwise"></i> 重置
                                    </button>
                                    <button class="btn btn-primary" id="refreshBtn" title="刷新数据">
                                        <i class="bi bi-arrow-repeat"></i> 刷新
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 借阅记录表格 -->
                    <div class="card shadow-sm">
                        <div class="card-header d-flex justify-content-between align-items-center">
                            <h5 class="mb-0">
                                <i class="bi bi-list-ul me-2"></i>借阅记录列表
                            </h5>
                            <div class="text-muted small" id="currentPageInfo">
                                <span id="currentPageCount">0</span> 条记录
                            </div>
                        </div>
                        <div class="card-body p-0">
                            <div class="table-responsive">
                                <table class="table table-hover align-middle mb-0">
                                    <thead class="table-light">
                                        <tr>
                                            <th width="60" class="text-center">#</th>
                                            <th width="100">借阅ID</th>
                                            <th width="120">用户信息</th>
                                            <th>图书信息</th>
                                            <th width="150">借出时间</th>
                                            <th width="150">应还时间</th>
                                            <th width="120">状态</th>
                                            <th width="150">逾期天数</th>
                                            <th width="100" class="text-center">操作</th>
                                        </tr>
                                    </thead>
                                    <tbody id="recordsTableBody">
                                        <tr>
                                            <td colspan="9" class="text-center py-5">
                                                <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
                                                <span class="ms-2 text-muted">正在加载借阅记录...</span>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>

                            <!-- 分页 -->
                            <div id="paginationContainer" class="d-flex justify-content-between align-items-center p-3 border-top">
                                <div id="pageInfo" class="text-muted">正在加载分页信息...</div>
                                <nav id="pagination">
                                    <!-- 分页按钮将动态生成 -->
                                </nav>
                            </div>
                        </div>
                    </div>

                    <!-- 详情模态框 -->
                    <div class="modal fade" id="recordDetailModal" tabindex="-1" aria-hidden="true">
                        <div class="modal-dialog modal-lg">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <h5 class="modal-title">借阅记录详情</h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                                </div>
                                <div class="modal-body" id="recordDetailContent">
                                    <!-- 详情内容将通过JS动态填充 -->
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">关闭</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        },

        // 初始化模块 - 在render之后调用
        onRender: function() {
            console.log('🚀 初始化管理员借阅管理模块');

            // 重置状态
            this.state = {
                currentPage: 1,
                pageSize: 10,
                filterStatus: 'all',
                records: [],
                totalPages: 1,
                totalItems: 0,
                loading: false,
                stats: {
                    total: 0,
                    borrowed: 0,
                    overdue: 0,
                    returned: 0,
                    renewed: 0
                }
            };

            // 绑定事件
            this.bindEvents();

            // 加载统计数据
            this.loadBorrowStats();

            // 加载借阅记录
            this.loadBorrowRecords();
        },

        // 模块销毁时清理
        onDestroy: function() {
            console.log('🧹 清理管理员借阅管理模块资源');
            // 清理事件监听器
            const events = ['change', 'click'];
            events.forEach(event => {
                document.removeEventListener(event, this.handleEvent);
            });
        },

        // 绑定事件监听器
        bindEvents: function() {
            const self = this;

            // 状态筛选
            const statusFilter = document.getElementById('statusFilter');
            if (statusFilter) {
                statusFilter.addEventListener('change', function(e) {
                    self.state.filterStatus = e.target.value;
                    self.state.currentPage = 1;
                    self.loadBorrowRecords();
                });
            }

            // 重置按钮
            const resetBtn = document.getElementById('resetBtn');
            if (resetBtn) {
                resetBtn.addEventListener('click', function() {
                    self.state.filterStatus = 'all';
                    self.state.currentPage = 1;

                    const statusFilter = document.getElementById('statusFilter');
                    if (statusFilter) statusFilter.value = 'all';

                    self.loadBorrowRecords();
                });
            }

            // 刷新按钮
            const refreshBtn = document.getElementById('refreshBtn');
            if (refreshBtn) {
                refreshBtn.addEventListener('click', function() {
                    self.loadBorrowStats();
                    self.loadBorrowRecords();
                });
            }
        },

        // 加载借阅记录
        loadBorrowRecords: async function() {
            try {
                if (this.state.loading) return;
                this.state.loading = true;

                this.showLoading();

                // 获取筛选参数
                const page = this.state.currentPage - 1;  // 后端从0开始
                const size = this.state.pageSize;
                const status = this.state.filterStatus;

                // 构建URL
                let url = `${this.config.apiBase}/admin/borrows?page=${page}&size=${size}`;

                // 添加状态筛选参数
                if (status && status !== 'all') {
                    url += `&status=${encodeURIComponent(status)}`;
                }

                console.log('正在请求借阅记录:', url);

                // 调用后端接口
                const response = await fetch(url, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    credentials: 'include'  // 携带Cookie
                });

                // 处理401未登录状态
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
                        记录数: data.list ? data.list.length : 0,
                        总条数: data.total,
                        总页数: data.totalPages,
                        当前页: data.page
                    });

                    // 更新状态
                    this.state.records = data.list || [];
                    this.state.totalItems = data.total || 0;
                    this.state.totalPages = data.totalPages || 1;
                    this.state.currentPage = data.page || 1; // 后端返回0-based，转为1-based

                    // 渲染界面
                    this.renderTable();
                    this.renderPagination();
                    this.updatePageInfo();

                } else {
                    this.showMessage(result.message || "加载失败！", "error");
                }
            } catch (error) {
                console.error("加载借阅记录失败:", error);
                this.showMessage("网络错误: " + error.message, "error");
            } finally {
                this.state.loading = false;
            }
        },

        // 加载统计信息
        loadBorrowStats: async function() {
            try {
                console.log('正在加载统计信息...');

                const response = await fetch(`${this.config.apiBase}/admin/borrows/stats`, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    credentials: 'include'  // 携带Cookie
                });

                if (response.status === 401) {
                    console.log('未授权，跳转到登录页');
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
                    this.state.stats = result.data || {
                        total: 0,
                        borrowed: 0,
                        overdue: 0,
                        returned: 0,
                        renewed: 0
                    };

                    this.renderStats();
                } else {
                    console.warn("加载统计信息失败:", result.message);
                }
            } catch (error) {
                console.error("加载统计信息失败:", error);
            }
        },

        // 渲染统计信息
        renderStats: function() {
            const stats = this.state.stats;

            const statTotal = document.getElementById('statTotal');
            const statBorrowed = document.getElementById('statBorrowed');
            const statOverdue = document.getElementById('statOverdue');
            const statReturned = document.getElementById('statReturned');

            if (statTotal) statTotal.textContent = stats.total || 0;
            if (statBorrowed) statBorrowed.textContent = stats.borrowed || 0;
            if (statOverdue) statOverdue.textContent = stats.overdue || 0;
            if (statReturned) statReturned.textContent = stats.returned || 0;
        },

        // 更新页面信息
        updatePageInfo: function() {
            const currentPageCount = document.getElementById('currentPageCount');
            if (currentPageCount) {
                // 修复为显示更详细的信息：
                const start = (this.state.currentPage - 1) * this.state.pageSize + 1;
                const end = start + this.state.records.length - 1;
                currentPageCount.textContent = `第 ${start} 到 ${end} 条，共 ${this.state.totalItems} 条记录`;
            }
        },

        // 渲染表格
        renderTable: function() {
            const tbody = document.getElementById('recordsTableBody');
            if (!tbody) return;

            if (!this.state.records || this.state.records.length === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="9" class="text-center py-5 text-muted">
                            <i class="bi bi-journal-x" style="font-size: 3rem; opacity: 0.3;"></i>
                            <div class="mt-3">暂无借阅记录</div>
                            <small class="text-muted">当前筛选条件下没有找到借阅记录</small>
                        </td>
                    </tr>
                `;
                return;
            }

            let html = '';
            this.state.records.forEach((record, index) => {
                const recordNum = index + 1;  // 或者使用后端返回的实际位置
                const status = record.status || 'BORROWED';
                const isOverdue = this.isRecordOverdue(record);
                const overdueDays = this.getOverdueDays(record);

                // 格式化用户信息
                const userInfo = record.username ?
                    `<div>
                        <div class="fw-medium">${record.realName || record.username}</div>
                        <div class="small text-muted">@${record.username}</div>
                    </div>` :
                    '<div class="text-muted">未知用户</div>';

                // 格式化图书信息
                const bookInfo = `
                    <div>
                        <div class="fw-medium">${record.bookname || '未知图书'}</div>
                        <div class="small text-muted">
                            <div>作者: ${record.author || '-'}</div>
                            <div>出版社: ${record.publisher || '-'}</div>
                        </div>
                    </div>
                `;

                html += `
                    <tr>
                        <td class="text-center">${recordNum}</td>
                        <td>
                            <span class="badge bg-light text-dark">${record.recordId || '-'}</span>
                        </td>
                        <td>${userInfo}</td>
                        <td>${bookInfo}</td>
                        <td>${this.formatDateTime(record.borrowTime)}</td>
                        <td>
                            ${this.formatDateTime(record.dueTime)}
                            ${isOverdue ? '<span class="badge bg-danger ms-1">逾期</span>' : ''}
                        </td>
                        <td>${this.getStatusBadge(status)}</td>
                        <td>${this.getOverdueDaysHtml(overdueDays, status)}</td>
                        <td class="text-center">
                            <button type="button"
                                    class="btn btn-sm btn-outline-primary view-detail-btn"
                                    data-record-id="${record.recordId}"
                                    title="查看详情">
                                <i class="bi bi-eye"></i> 详情
                            </button>
                        </td>
                    </tr>
                `;
            });

            tbody.innerHTML = html;

            // 绑定详情查看事件
            this.bindDetailViewEvents();
        },

        // 绑定详情查看事件
        bindDetailViewEvents: function() {
            const self = this;

            document.querySelectorAll('.view-detail-btn').forEach(btn => {
                btn.addEventListener('click', function(e) {
                    e.preventDefault();
                    const recordId = this.dataset.recordId;
                    self.showRecordDetail(recordId);
                });
            });
        },

        // 显示记录详情
        showRecordDetail: async function(recordId) {
            try {
                console.log('查看记录详情:', recordId);

                // 从当前记录中查找
                const record = this.state.records.find(r => r.recordId == recordId);

                if (!record) {
                    this.showMessage("未找到该记录", "error");
                    return;
                }

                // 构建详情内容
                const detailContent = this.getRecordDetailHtml(record);
                const detailContentEl = document.getElementById('recordDetailContent');
                if (detailContentEl) {
                    detailContentEl.innerHTML = detailContent;
                }

                // 显示模态框
                const modal = new bootstrap.Modal(document.getElementById('recordDetailModal'));
                modal.show();

            } catch (error) {
                console.error("查看详情失败:", error);
                this.showMessage("查看详情失败: " + error.message, "error");
            }
        },

        // 获取记录详情HTML
        getRecordDetailHtml: function(record) {
            const statusInfo = this.config.statusMap[record.status] || { text: record.status, class: 'secondary' };
            const isOverdue = this.isRecordOverdue(record);
            const overdueDays = this.getOverdueDays(record);

            return `
                <div class="row">
                    <div class="col-md-6">
                        <h6 class="border-bottom pb-2 mb-3">借阅信息</h6>
                        <dl class="row">
                            <dt class="col-sm-4">借阅ID</dt>
                            <dd class="col-sm-8"><code>${record.recordId || '-'}</code></dd>

                            <dt class="col-sm-4">借阅状态</dt>
                            <dd class="col-sm-8">
                                <span class="badge bg-${statusInfo.class}">${statusInfo.text}</span>
                                ${isOverdue ? '<span class="badge bg-danger ms-1">已逾期</span>' : ''}
                            </dd>

                            <dt class="col-sm-4">借出时间</dt>
                            <dd class="col-sm-8">${this.formatDateTime(record.borrowTime)}</dd>

                            <dt class="col-sm-4">应还时间</dt>
                            <dd class="col-sm-8">${this.formatDateTime(record.dueTime)}</dd>

                            <dt class="col-sm-4">归还时间</dt>
                            <dd class="col-sm-8">${record.returnTime ? this.formatDateTime(record.returnTime) : '未归还'}</dd>

                            <dt class="col-sm-4">逾期天数</dt>
                            <dd class="col-sm-8">${overdueDays > 0 ? `<span class="text-danger">${overdueDays} 天</span>` : '未逾期'}</dd>
                        </dl>
                    </div>
                    <div class="col-md-6">
                        <h6 class="border-bottom pb-2 mb-3">用户信息</h6>
                        <dl class="row">
                            <dt class="col-sm-4">用户名</dt>
                            <dd class="col-sm-8">${record.username || '-'}</dd>

                            <dt class="col-sm-4">真实姓名</dt>
                            <dd class="col-sm-8">${record.realName || '-'}</dd>

                            <dt class="col-sm-4">用户ID</dt>
                            <dd class="col-sm-8">${record.userId || '-'}</dd>
                        </dl>

                        <h6 class="border-bottom pb-2 mb-3 mt-4">图书信息</h6>
                        <dl class="row">
                            <dt class="col-sm-4">图书名称</dt>
                            <dd class="col-sm-8">${record.bookname || '-'}</dd>

                            <dt class="col-sm-4">图书ID</dt>
                            <dd class="col-sm-8">${record.bookId || '-'}</dd>

                            <dt class="col-sm-4">作者</dt>
                            <dd class="col-sm-8">${record.author || '-'}</dd>

                            <dt class="col-sm-4">出版社</dt>
                            <dd class="col-sm-8">${record.publisher || '-'}</dd>

                            <dt class="col-sm-4">分类ID</dt>
                            <dd class="col-sm-8">${record.categoryId || '-'}</dd>
                        </dl>
                    </div>
                </div>
            `;
        },

        // 渲染分页
        renderPagination: function() {
            const container = document.getElementById('paginationContainer');
            const pageInfo = document.getElementById('pageInfo');
            const pagination = document.getElementById('pagination');

            if (!container || !pageInfo || !pagination) {
                console.error('分页元素未找到');
                return;
            }

            // 计算分页信息
            let startRecord, endRecord;
            if (this.state.totalItems === 0) {
                startRecord = 0;
                endRecord = 0;
            } else {
                startRecord = (this.state.currentPage - 1) * this.state.pageSize + 1;
                endRecord = startRecord + this.state.records.length - 1;
            }

            // 更新分页信息文本
            if (this.state.totalItems > 0) {
                pageInfo.innerHTML = `显示第 ${startRecord} 到 ${endRecord} 条，共 ${this.state.totalItems} 条记录`;
            } else {
                pageInfo.innerHTML = '暂无记录';
            }

            // 渲染分页按钮
            this.renderPageButtons();
        },

        // 渲染分页按钮
        renderPageButtons: function() {
            const pagination = document.getElementById('pagination');
            if (!pagination) return;

            const self = this;
            let html = '<ul class="pagination pagination-sm mb-0">';

            // 上一页按钮
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

            // 页码按钮
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

            // 下一页按钮
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

            // 绑定分页按钮事件
            this.bindPaginationEvents();
        },

        // 绑定分页事件
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

        // 显示加载状态
        showLoading: function() {
            const tbody = document.getElementById('recordsTableBody');
            if (tbody) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="9" class="text-center py-5">
                            <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
                            <span class="ms-2 text-muted">正在加载借阅记录...</span>
                        </td>
                    </tr>
                `;
            }
        },

        // 辅助方法
        formatDateTime: function(dateString) {
            if (!dateString) return '-';
            try {
                const date = new Date(dateString);
                if (isNaN(date.getTime())) return '-';

                return date.toLocaleDateString('zh-CN') + ' ' +
                       date.toLocaleTimeString('zh-CN', {
                           hour: '2-digit',
                           minute: '2-digit'
                       });
            } catch (error) {
                console.error('日期格式化错误:', error, dateString);
                return '-';
            }
        },

        isRecordOverdue: function(record) {
            if (!record.dueTime || record.status === 'RETURNED') return false;

            try {
                const dueDate = new Date(record.dueTime);
                const now = new Date();
                return now > dueDate;
            } catch (error) {
                console.error('判断逾期错误:', error, record.dueTime);
                return false;
            }
        },

        getOverdueDays: function(record) {
            if (!record.dueTime || record.status === 'RETURNED') return 0;

            try {
                const dueDate = new Date(record.dueTime);
                const now = new Date();
                const diffTime = now - dueDate;
                const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
                return Math.max(0, diffDays);
            } catch (error) {
                console.error('计算逾期天数错误:', error, record.dueTime);
                return 0;
            }
        },

        getOverdueDaysHtml: function(days, status) {
            if (status === 'RETURNED') {
                return '<span class="text-muted">-</span>';
            }

            if (days <= 0) {
                return '<span class="text-success">未逾期</span>';
            }

            return `<span class="text-danger">${days} 天</span>`;
        },

        getStatusBadge: function(status) {
            const statusInfo = this.config.statusMap[status] || {
                text: status,
                class: 'secondary',
                color: 'var(--bs-secondary)'
            };
            return `
                <span class="badge bg-${statusInfo.class}"
                      style="background-color: ${statusInfo.color} !important">
                    ${statusInfo.text}
                </span>
            `;
        },

        showMessage: function(message, type) {
            if (window.showToast) {
                window.showToast(message, type);
            } else {
                const typeMap = {
                    'success': '成功',
                    'error': '错误',
                    'warning': '警告',
                    'info': '提示'
                };
                alert(`${typeMap[type] || '提示'}: ${message}`);
            }
        }
    };
    console.log('🔧 borrow-manage.js 模块定义完成');

    // ✅ 模块注册代码（必须在模块对象外部）
    // 方法1：使用 safeRegisterModule（如果可用）
    if (typeof window.safeRegisterModule === 'function') {
        window.safeRegisterModule('borrow-manage', borrowManageModule);
        console.log('✅ 通过 safeRegisterModule 注册成功');
    }
    // 方法2：使用 registerModule（如果可用）
    else if (typeof window.registerModule === 'function') {
        window.registerModule('borrow-manage', borrowManageModule);
        console.log('✅ 通过 registerModule 注册成功');
    }
    // 方法3：直接注册到 ModuleRegistry
    else if (typeof ModuleRegistry !== 'undefined') {
        ModuleRegistry['borrow-manage'] = borrowManageModule;
        console.log('✅ 直接注册到 ModuleRegistry 成功');
    }
    // 方法4：注册到全局 modules
    else {
        window.modules = window.modules || {};
        window.modules['borrow-manage'] = borrowManageModule;
        console.log('✅ 注册到 window.modules 作为备用');
    }
    console.log('📦 模块注册状态:', {
        '已注册到safeRegisterModule': typeof window.safeRegisterModule === 'function',
        '已注册到registerModule': typeof window.registerModule === 'function',
        '已注册到ModuleRegistry': !!ModuleRegistry && !!ModuleRegistry['borrow-manage'],
        '已注册到window.modules': window.modules && window.modules['borrow-manage']
    });

})();