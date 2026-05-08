//用户管理模块
(function() {
    'use strict';
    const UserManage = {
        currentPage: 1,
        pageSize: 10,
        searchKeyword: '',
        getAuthToken: function() {
            const token = localStorage.getItem('authToken') ||
                          localStorage.getItem('token') ||
                          sessionStorage.getItem('authToken') ||
                          sessionStorage.getItem('token') ||
                          '';

            console.log('获取认证token:', token ? '找到token' : '未找到token');
            return token;
        },
        loadUserStats: function() {
            const token = this.getAuthToken();
            const headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            };
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            } else {
                console.warn('未找到认证token，可能以未登录状态访问统计接口');
            }
            fetch('/api/admin/users/stats', {
                method: 'GET',
                headers: headers,
                credentials: 'include'
            })
            .then(response => {
                console.log('统计接口响应状态:', response.status, response.statusText);
                if (response.status === 401) {
                    console.warn('未授权访问统计接口 (401)');
                    this.updateUserStats({totalUsers: '未授权'});
                    return null;
                }
                if (response.status === 403) {
                    console.warn('权限不足 (403)');
                    this.updateUserStats({totalUsers: '无权限'});
                    return null;
                }

                if (!response.ok) {
                    throw new Error(`HTTP错误 ${response.status}: ${response.statusText}`);
                }

                return response.json();
            })
            .then(data => {
                if (!data) {
                    return;
                }

                console.log('用户统计接口返回数据:', data);

                if (data && data.success && data.data) {
                    this.updateUserStats(data.data);
                } else {
                    console.error('获取用户统计失败:', data?.message || '未知错误');
                    this.updateUserStats({totalUsers: '加载失败'});
                }
            })
            .catch(error => {
                console.error('加载用户统计信息失败:', error);
                setTimeout(() => {
                    this.fallbackToUserListStats();
                }, 1000);
            });
        },
        updateUserStats: function(stats) {
            console.log('更新用户统计显示:', stats);

            const totalUsersEl = document.getElementById('totalUsers');
            if (!totalUsersEl) {
                console.warn('未找到totalUsers元素');
                return;
            }

            if (stats.totalUsers !== undefined && stats.totalUsers !== null) {
                totalUsersEl.textContent = stats.totalUsers;
                totalUsersEl.className = 'text-primary fw-bold';
                console.log(`用户统计更新成功: ${stats.totalUsers} 个用户`);
            } else {
                console.warn('统计信息无效:', stats);
                totalUsersEl.textContent = 'N/A';
                totalUsersEl.className = 'text-muted';
            }
        },
        fallbackToUserListStats: function() {
            const totalUsersEl = document.getElementById('totalUsers');
            if (totalUsersEl && totalUsersEl.textContent === '0') {
                totalUsersEl.textContent = '...';
                totalUsersEl.className = 'text-warning';
            }
        },
        showLoginPrompt: function() {
            if (confirm('您的登录已过期，需要重新登录。\n\n点击确定前往登录页面')) {
                if (window.location.pathname.includes('index.html')) {
                    window.location.href = 'login.html?redirect=' + encodeURIComponent(window.location.href);
                } else {
                    window.location.href = '../login.html';
                }
            }
        },
        render: function() {
            this.init();
        },
        onDestroy: function() {
            console.log('清理用户管理模块');
        },
        init: function() {
            const uiRendered = this.renderUI();
            if (!uiRendered) {
                console.error('UI渲染失败');
                this.showErrorMessage('用户管理界面初始化失败');
                return;
            }
            this.loadUserStats();
            this.loadUserList();
            this.bindEvents();
        },
        renderUI: function() {
            const moduleContent = document.getElementById('moduleContent');

            if (!moduleContent) {
                console.error('错误：找不到模块内容容器 (moduleContent)');
                console.error('当前页面结构：');
                console.error(document.body.innerHTML);
                return false;
            }
            moduleContent.innerHTML = `
                <div class="container-fluid">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h4><i class="fas fa-users me-2"></i>用户管理</h4>
                        <div>
                            <button class="btn btn-success" id="exportUserBtn">
                                <i class="bi bi-file-excel"></i> 导出Excel
                            </button>
                            <button class="btn btn-sm btn-primary" id="refreshBtn">
                                <i class="fas fa-sync-alt me-1"></i>刷新
                            </button>
                        </div>
                    </div>
                    <div class="card mb-3">
                        <div class="card-body py-2">
                            <div class="row align-items-center">
                                <div class="col-md-8">
                                    <div class="input-group input-group-sm">
                                        <input type="text" class="form-control"
                                               placeholder="搜索用户名、真实姓名、邮箱或电话"
                                               id="searchInput" value="${this.searchKeyword}">
                                        <button class="btn btn-outline-primary" type="button" id="searchBtn">
                                            <i class="fas fa-search me-1"></i>搜索
                                        </button>
                                    </div>
                                </div>
                                <div class="col-md-4 text-end">
                                    <small class="text-muted">共 <span id="totalUsers" class="text-primary fw-bold">0</span> 个用户</small>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="card">
                        <div class="table-responsive">
                            <table class="table table-hover table-sm mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th width="60">ID</th>
                                        <th>用户名</th>
                                        <th>真实姓名</th>
                                        <th>邮箱</th>
                                        <th>电话</th>
                                        <th width="100">角色</th>
                                        <th width="80">状态</th>
                                        <th width="150">注册时间</th>
                                        <th width="150">最后登录</th>
                                        <th width="120" class="text-center">操作</th>
                                    </tr>
                                </thead>
                                <tbody id="userTableBody">
                                    <tr>
                                        <td colspan="10" class="text-center py-5 text-muted">
                                            <div class="spinner-border spinner-border-sm" role="status"></div>
                                            <span class="ms-2">正在加载用户数据...</span>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                        <div class="card-footer py-2">
                            <div class="d-flex justify-content-between align-items-center">
                                <div class="text-muted small">
                                    显示 <span id="pageInfoStart">0</span> 到 <span id="pageInfoEnd">0</span> 条，共 <span id="totalCount">0</span> 条记录
                                </div>
                                <nav aria-label="用户分页">
                                    <ul class="pagination pagination-sm mb-0" id="pagination">
                                        <!-- 分页按钮会动态生成 -->
                                    </ul>
                                </nav>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            return true;
        },

        loadUserList: function(page = 1, keyword = '') {
            console.log(`加载用户列表，页码: ${page}, 关键词: "${keyword}"`);

            this.currentPage = page;
            this.searchKeyword = keyword;
            const tableBody = document.getElementById('userTableBody');
            if (!tableBody) {
                console.error('错误：找不到表格内容区域 (userTableBody)');
                console.log('重新渲染UI并重试...');
                setTimeout(() => {
                    this.renderUI();
                    this.loadUserList(page, keyword);
                }, 100);
                return;
            }
            tableBody.innerHTML = `
                <tr>
                    <td colspan="10" class="text-center py-4">
                        <div class="spinner-border spinner-border-sm" role="status"></div>
                        <span class="ms-2">正在加载用户数据...</span>
                    </td>
                </tr>
            `;
            let url = `/api/admin/users?page=${page}&size=${this.pageSize}`;
            if (keyword && keyword.trim()) {
                url += `&keyword=${encodeURIComponent(keyword.trim())}`;
            }
            fetch(url)
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                    }
                    return response.json();
                })
                .then(data => {
                    if (data && data.success) {
                        this.renderUserTable(data);
                        const total = data.data?.total || 0;
                        const totalUsersEl = document.getElementById('totalUsers');
                        if (totalUsersEl && (totalUsersEl.textContent === '0' ||
                            totalUsersEl.textContent === '...' ||
                            totalUsersEl.textContent === '加载失败' ||
                            totalUsersEl.textContent === 'N/A')) {
                            console.log('使用用户列表数据更新统计显示:', total);
                            totalUsersEl.textContent = total;
                            totalUsersEl.className = 'text-info fw-bold';
                        }

                    } else {
                        throw new Error(data?.message || '获取用户列表失败');
                    }
                })
                .catch(error => {
                    console.error('加载用户列表失败:', error);
                    this.showError('加载用户列表失败: ' + error.message);
                });
        },
        renderUserTable: function(data) {
            const tableBody = document.getElementById('userTableBody');
            if (!tableBody) return;
            const users = data.data?.list || [];
            const total = data.data?.total || 0;
            const currentPage = data.data?.page || 1;
            const totalPages = data.data?.totalPages || 1;

            const totalCountEl = document.getElementById('totalCount');
            if (totalCountEl) totalCountEl.textContent = total;

            const pageInfoStartEl = document.getElementById('pageInfoStart');
            const pageInfoEndEl = document.getElementById('pageInfoEnd');
            if (pageInfoStartEl && pageInfoEndEl) {
                const start = (currentPage - 1) * this.pageSize + 1;
                const end = Math.min(currentPage * this.pageSize, total);
                pageInfoStartEl.textContent = start;
                pageInfoEndEl.textContent = end;
            }

            if (users.length === 0) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="10" class="text-center py-5 text-muted">
                            <i class="fas fa-inbox fa-2x mb-2"></i>
                            <p class="mb-0">暂无用户数据</p>
                            <small>${this.searchKeyword ? '请尝试其他搜索条件' : '暂时没有用户数据'}</small>
                        </td>
                    </tr>
                `;
            } else {
                let rows = '';
                users.forEach(user => {
                    let statusBadge = '';
                    switch(user.status) {
                        case 'ACTIVE':
                            statusBadge = '<span class="badge bg-success">正常</span>';
                            break;
                        case 'LOCKED':
                            statusBadge = '<span class="badge bg-warning text-dark">锁定</span>';
                            break;
                        case 'CANCELLED':
                            statusBadge = '<span class="badge bg-secondary">已注销</span>';
                            break;
                        default:
                            statusBadge = '<span class="badge bg-light text-dark">未知</span>';
                    }

                    const roleBadge = user.role === 'ADMIN'
                        ? '<span class="badge bg-primary">管理员</span>'
                        : '<span class="badge bg-secondary">普通用户</span>';
                    const createTime = user.createTime ? new Date(user.createTime).toLocaleString() : '未知';
                    let lastLoginTime = '从未登录';
                    if (user.lastLoginTime && user.lastLoginTime !== 'null' && user.lastLoginTime !== '') {
                        try {
                            const date = new Date(user.lastLoginTime);
                            if (!isNaN(date.getTime())) {
                                lastLoginTime = date.toLocaleString('zh-CN');
                            } else {
                                lastLoginTime = createTime || '从未登录';
                            }
                        } catch (e) {
                            lastLoginTime = createTime || '从未登录';
                        }
                    } else {
                        lastLoginTime = createTime || '从未登录';
                    }

                    rows += `
                        <tr>
                            <td>${user.userId}</td>
                            <td>${user.username || '-'}</td>
                            <td>${user.realName || '-'}</td>
                            <td>${user.email || '-'}</td>
                            <td>${user.phone || '-'}</td>
                            <td>${roleBadge}</td>
                            <td>${statusBadge}</td>
                            <td><small>${createTime}</small></td>
                            <td><small>${lastLoginTime}</small></td>
                            <td class="text-center">
                                <div class="btn-group btn-group-sm" role="group">
                                    ${user.status === 'CANCELLED'
                                        ? '<span class="badge bg-secondary">已注销</span>'
                                        : `
                                            <button type="button" class="btn btn-sm ${user.status === 'LOCKED' ? 'btn-outline-success' : 'btn-outline-warning'} toggle-lock-btn"
                                                    data-id="${user.userId}"
                                                    data-current-status="${user.status}"
                                                    title="${user.status === 'LOCKED' ? '点击解锁' : '点击锁定'}">
                                                <i class="fas ${user.status === 'LOCKED' ? 'fa-unlock' : 'fa-lock'}"></i>
                                            </button>

                                            <button type="button" class="btn btn-sm btn-outline-danger cancel-user-btn"
                                                    data-id="${user.userId}"
                                                    title="注销用户">
                                                <i class="fas fa-user-times"></i>
                                            </button>
                                        `
                                    }
                                </div>
                            </td>
                        </tr>
                    `;
                });
                tableBody.innerHTML = rows;
            }

            this.renderPagination(currentPage, totalPages);
        },

        renderPagination: function(currentPage, totalPages) {
            const paginationEl = document.getElementById('pagination');
            if (!paginationEl || totalPages <= 1) {
                if (paginationEl) paginationEl.innerHTML = '';
                return;
            }

            let paginationHtml = '';

            if (currentPage > 1) {
                paginationHtml += `
                    <li class="page-item">
                        <a class="page-link" href="#" data-page="${currentPage - 1}">
                            上一页
                        </a>
                    </li>
                `;
            } else {
                paginationHtml += '<li class="page-item disabled"><a class="page-link" href="#">上一页</a></li>';
            }

            const maxVisiblePages = 5;
            let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2));
            let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

            if (endPage - startPage + 1 < maxVisiblePages) {
                startPage = Math.max(1, endPage - maxVisiblePages + 1);
            }

            for (let i = startPage; i <= endPage; i++) {
                if (i === currentPage) {
                    paginationHtml += `<li class="page-item active"><a class="page-link" href="#">${i}</a></li>`;
                } else {
                    paginationHtml += `
                        <li class="page-item">
                            <a class="page-link" href="#" data-page="${i}">
                                ${i}
                            </a>
                        </li>
                    `;
                }
            }

            if (currentPage < totalPages) {
                paginationHtml += `
                    <li class="page-item">
                        <a class="page-link" href="#" data-page="${currentPage + 1}">
                            下一页
                        </a>
                    </li>
                `;
            } else {
                paginationHtml += '<li class="page-item disabled"><a class="page-link" href="#">下一页</a></li>';
            }

            paginationEl.innerHTML = paginationHtml;
        },
        toggleUserLock: function(userId, currentStatus, username) {
            const newStatus = currentStatus === 'LOCKED' ? 'ACTIVE' : 'LOCKED';
            const action = newStatus === 'ACTIVE' ? '解锁' : '锁定';

            if (!confirm(`确定要${action}用户 "${username}" 吗？`)) {
                return;
            }

            fetch(`/api/admin/users/${userId}/status`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({status: newStatus})
            })
            .then(response => response.json())
            .then(data => {
                if (data && data.success) {
                    alert(`${action}成功`);
                    this.loadUserList(this.currentPage, this.searchKeyword);
                    setTimeout(() => {
                        this.loadUserStats();
                    }, 500);

                } else {
                    alert(`${action}失败: ${data?.message || '未知错误'}`);
                }
            })
            .catch(error => {
                console.error(`${action}失败:`, error);
                alert(`${action}失败: ${error.message}`);
            });
        },
        cancelUser: function(userId, username) {
            if (!confirm(`确定要注销用户 "${username}" 吗？\n\n注意：\n1. 注销后用户将无法登录\n2. 此操作不可恢复\n3. 用户需要重新注册才能再次使用`)) {
                return;
            }
            fetch(`/api/admin/users/${userId}/status`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    status: 'CANCELLED',
                })
            })
            .then(response => response.json())
            .then(data => {
                if (data && data.success) {
                    alert('用户已成功注销！');
                    this.loadUserList(this.currentPage, this.searchKeyword);

                    setTimeout(() => {
                        this.loadUserStats();
                    }, 500);

                } else {
                    alert(`注销失败: ${data?.message || '未知错误'}`);
                }
            })
            .catch(error => {
                console.error('注销失败:', error);
                alert('注销失败: ' + error.message);
            });
        },
        toggleUserStatus: function(userId, newStatus, username) {
            const action = newStatus === 'ACTIVE' ? '启用' : '停用';
            if (!confirm(`确定要${action}用户 "${username}" 吗？`)) {
                return;
            }

            fetch(`/api/admin/users/${userId}/status`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({status: newStatus})
            })
            .then(response => response.json())
            .then(data => {
                if (data && data.success) {
                    alert(`${action}成功`);
                    this.loadUserList(this.currentPage, this.searchKeyword);
                    setTimeout(() => {
                        this.loadUserStats();
                    }, 500);

                } else {
                    alert(`${action}失败: ${data?.message || '未知错误'}`);
                }
            })
            .catch(error => {
                console.error(`${action}失败:`, error);
                alert(`${action}失败: ${error.message}`);
            });
        },
        bindEvents: function() {
            const searchBtn = document.getElementById('searchBtn');
            if (searchBtn) {
                searchBtn.addEventListener('click', () => {
                    const keyword = document.getElementById('searchInput')?.value || '';
                    console.log('搜索用户，重新加载统计...');
                    this.loadUserStats();
                    this.loadUserList(1, keyword);
                });
            }

            // 搜索框回车
            const searchInput = document.getElementById('searchInput');
            if (searchInput) {
                searchInput.addEventListener('keypress', (e) => {
                    if (e.key === 'Enter') {
                        const keyword = searchInput.value || '';
                        console.log('搜索用户（回车），重新加载统计...');
                        this.loadUserStats();
                        this.loadUserList(1, keyword);
                    }
                });
            }
            const refreshBtn = document.getElementById('refreshBtn');
            if (refreshBtn) {
                refreshBtn.addEventListener('click', () => {
                    if (searchInput) {
                        searchInput.value = '';
                        this.searchKeyword = '';
                    }
                    console.log('刷新用户数据和统计...');
                    this.loadUserStats();
                    this.loadUserList(1);
                });
            }
            const exportBtn = document.getElementById('exportUserBtn');
            if (exportBtn) {
                exportBtn.addEventListener('click', () => {
                    this.exportUsersToExcel();
                });
            }
            document.addEventListener('click', (e) => {
                if (e.target.closest('.toggle-lock-btn')) {
                    e.preventDefault();
                    const button = e.target.closest('.toggle-lock-btn');
                    const userId = button.getAttribute('data-id');
                    const currentStatus = button.getAttribute('data-current-status');
                    const username = button.closest('tr').querySelector('td:nth-child(2)').textContent.trim();
                    this.toggleUserLock(userId, currentStatus, username);
                }
                if (e.target.closest('.cancel-user-btn')) {
                    e.preventDefault();
                    const button = e.target.closest('.cancel-user-btn');
                    const userId = button.getAttribute('data-id');
                    const username = button.closest('tr').querySelector('td:nth-child(2)').textContent.trim();
                    this.cancelUser(userId, username);
                }
                //处理分页按钮
                if (e.target.closest('.page-link')) {
                    e.preventDefault();
                    e.stopPropagation();
                    const moduleContent = document.getElementById('moduleContent');
                    const clickedElement = e.target.closest('#moduleContent');
                    if (clickedElement && AppState.currentModule === 'user-manage') {
                        const pageLink = e.target.closest('.page-link');
                        const page = pageLink.getAttribute('data-page');
                        if (page) {
                            this.loadUserList(parseInt(page), this.searchKeyword);
                        }
                    }
                }
            });
        },
        exportUsersToExcel: function() {
            console.log('点击了用户导出按钮');

            if (!window.ExportManager) {
                alert('导出功能未初始化，请刷新页面重试');
                return;
            }
            const keyword = document.getElementById('searchInput')?.value || '';
            console.log('搜索参数:', { keyword });
            const params = new URLSearchParams();
            if (keyword && keyword.trim() !== '') {
                params.append('keyword', keyword.trim());
            }
            let url = '/api/export/user';
            if (params.toString()) {
                url += '?' + params.toString();
            }
            console.log('用户导出URL:', url);
            const today = new Date();
            const dateStr = `${today.getFullYear()}${(today.getMonth() + 1).toString().padStart(2, '0')}${today.getDate().toString().padStart(2, '0')}`;
            const filename = `用户列表_${dateStr}.xlsx`;
            window.ExportManager.exportToExcel(url, '用户数据', filename);
        },
        showError: function(message) {
            const tableBody = document.getElementById('userTableBody');
            if (tableBody) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="10" class="text-center py-4 text-danger">
                            <i class="fas fa-exclamation-triangle me-2"></i>
                            ${message}
                            <button class="btn btn-sm btn-outline-primary ms-3" onclick="userManager.loadUserList(1)">
                                重试
                            </button>
                        </td>
                    </tr>
                `;
            }
        },
        showErrorMessage: function(message) {
            const moduleContent = document.getElementById('moduleContent');
            if (moduleContent) {
                moduleContent.innerHTML = `
                    <div class="container-fluid mt-3">
                        <div class="alert alert-danger">
                            <h5><i class="fas fa-exclamation-triangle me-2"></i>用户管理模块错误</h5>
                            <p>${message}</p>
                            <hr>
                            <p class="mb-1"><small>可能原因：</small></p>
                            <ul class="mb-2 small">
                                <li>网络连接问题</li>
                                <li>服务器未响应</li>
                                <li>接口权限问题</li>
                            </ul>
                            <button class="btn btn-primary" onclick="location.reload()">
                                <i class="fas fa-redo me-1"></i>刷新页面
                            </button>
                        </div>
                    </div>
                `;
            }
        }
    };
    if (typeof window.registerModule === 'function') {
        window.registerModule('user-manage', {
            render: function() {
                UserManage.render();
            },
            onDestroy: function() {
                UserManage.onDestroy();
            }
        });
    } else if (typeof safeRegisterModule === 'function') {
        safeRegisterModule('user-manage', {
            render: function() {
                UserManage.render();
            },
            onDestroy: function() {
                UserManage.onDestroy();
            }
        });
    } else {
        window.modules = window.modules || {};
        window.modules['user-manage'] = {
            render: function() {
                UserManage.render();
            },
            onDestroy: function() {
                UserManage.onDestroy();
            }
        };
    }
    window.userManager = UserManage;
})();