/**
 * 用户管理模块
 */
(function() {
    'use strict';

    console.log('📦 用户管理模块加载中...');

    // 用户管理对象
    const UserManage = {
        // 分页参数
        currentPage: 1,
        pageSize: 10,
        searchKeyword: '',

        // 模块的render函数（被core.js调用）
        render: function() {
            console.log('🚀 开始渲染用户管理模块');
            this.init();
        },

        // 模块销毁函数
        onDestroy: function() {
            console.log('🧹 清理用户管理模块');
            // 清理事件监听器等资源
        },

        // 初始化模块
        init: function() {
            console.log('🔄 初始化用户管理模块');

            // 1. 渲染UI
            console.log('🎨 渲染用户管理界面...');
            const uiRendered = this.renderUI();

            if (!uiRendered) {
                console.error('❌ UI渲染失败');
                this.showErrorMessage('用户管理界面初始化失败');
                return;
            }

            console.log('✅ UI渲染成功');

            // 2. 加载数据
            console.log('📥 加载用户数据...');
            this.loadUserList();

            // 3. 绑定事件
            console.log('🔗 绑定事件监听器...');
            this.bindEvents();

            console.log('🎉 用户管理模块初始化完成');
        },

        // 渲染用户管理界面
        renderUI: function() {
            console.log('🔍 查找模块容器...');
            const moduleContent = document.getElementById('moduleContent');

            if (!moduleContent) {
                console.error('❌ 错误：找不到模块内容容器 (moduleContent)');
                console.error('当前页面结构：');
                console.error(document.body.innerHTML);
                return false;
            }

            console.log('✅ 找到模块容器，开始构建界面');

            // 构建用户管理界面HTML
            moduleContent.innerHTML = `
                <div class="container-fluid">
                    <!-- 标题和操作按钮 -->
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h4><i class="fas fa-users me-2"></i>用户管理</h4>
                        <div>
                            <button class="btn btn-sm btn-primary" id="refreshBtn">
                                <i class="fas fa-sync-alt me-1"></i>刷新
                            </button>
                        </div>
                    </div>

                    <!-- 搜索区域 -->
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
                                    <small class="text-muted">共 <span id="totalUsers">0</span> 个用户</small>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 用户表格 -->
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
                                        <th width="150">最后登录</th>  <!-- 新增列 -->
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

                        <!-- 分页 -->
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

        // 加载用户列表
        loadUserList: function(page = 1, keyword = '') {
            console.log(`📊 加载用户列表，页码: ${page}, 关键词: "${keyword}"`);

            this.currentPage = page;
            this.searchKeyword = keyword;

            // 检查表格元素是否存在
            const tableBody = document.getElementById('userTableBody');
            if (!tableBody) {
                console.error('❌ 错误：找不到表格内容区域 (userTableBody)');
                console.log('重新渲染UI并重试...');
                setTimeout(() => {
                    this.renderUI();
                    this.loadUserList(page, keyword);
                }, 100);
                return;
            }

            // 显示加载状态
            tableBody.innerHTML = `
                <tr>
                    <td colspan="10" class="text-center py-4">
                        <div class="spinner-border spinner-border-sm" role="status"></div>
                        <span class="ms-2">正在加载用户数据...</span>
                    </td>
                </tr>
            `;

            // 构建API URL
            let url = `/api/admin/users?page=${page}&size=${this.pageSize}`;
            if (keyword && keyword.trim()) {
                url += `&keyword=${encodeURIComponent(keyword.trim())}`;
            }

            // 发送请求
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
                    } else {
                        throw new Error(data?.message || '获取用户列表失败');
                    }
                })
                .catch(error => {
                    console.error('加载用户列表失败:', error);
                    this.showError('加载用户列表失败: ' + error.message);
                });
        },

        // 渲染用户表格
        renderUserTable: function(data) {
            const tableBody = document.getElementById('userTableBody');
            if (!tableBody) return;

            const users = data.data?.list || [];
            const total = data.data?.total || 0;
            const currentPage = data.data?.page || 1;
            const totalPages = data.data?.totalPages || 1;

            // 更新总数显示
            const totalCountEl = document.getElementById('totalCount');
            if (totalCountEl) totalCountEl.textContent = total;

            // 更新分页信息
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
                    // 支持三种状态的代码：
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

                    // 格式化注册时间
                    const createTime = user.createTime ? new Date(user.createTime).toLocaleString() : '未知';
                    // 修改后：
                    let lastLoginTime = '从未登录';
                    if (user.lastLoginTime && user.lastLoginTime !== 'null' && user.lastLoginTime !== '') {
                        try {
                            const date = new Date(user.lastLoginTime);
                            if (!isNaN(date.getTime())) {
                                // 如果日期有效，则显示注册时间
                                lastLoginTime = date.toLocaleString('zh-CN');
                            } else {
                                // 如果无效，显示注册时间
                                lastLoginTime = createTime || '从未登录';
                            }
                        } catch (e) {
                            // 解析失败，显示注册时间
                            lastLoginTime = createTime || '从未登录';
                        }
                    } else {
                        // 如果没有最后登录时间，显示注册时间
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
                            <td><small>${lastLoginTime}</small></td>  <!-- 新增列 -->
                            <td class="text-center">
                                <div class="btn-group btn-group-sm" role="group">
                                    <!-- 编辑按钮（对所有状态显示） -->
                                    <button type="button" class="btn btn-outline-primary edit-user"
                                            data-id="${user.userId}"
                                            title="编辑">
                                        <i class="fas fa-edit"></i>
                                    </button>

                                    <!-- 根据状态显示不同按钮 -->
                                    ${user.status === 'ACTIVE'
                                        ? `
                                            <!-- 正常状态：显示锁定和注销按钮 -->
                                            <button type="button" class="btn btn-outline-warning lock-user"
                                                    data-id="${user.userId}"
                                                    title="锁定">
                                                <i class="fas fa-lock"></i>
                                            </button>
                                            <button type="button" class="btn btn-outline-danger cancel-user"
                                                    data-id="${user.userId}"
                                                    title="注销">
                                                <i class="fas fa-user-times"></i>
                                            </button>
                                        `
                                        : user.status === 'LOCKED'
                                        ? `
                                            <!-- 锁定状态：显示解锁和注销按钮 -->
                                            <button type="button" class="btn btn-outline-success unlock-user"
                                                    data-id="${user.userId}"
                                                    title="解锁">
                                                <i class="fas fa-unlock"></i>
                                            </button>
                                            <button type="button" class="btn btn-outline-danger cancel-user"
                                                    data-id="${user.userId}"
                                                    title="注销">
                                                <i class="fas fa-user-times"></i>
                                            </button>
                                        `
                                        : user.status === 'CANCELLED'
                                        ? `
                                            <!-- 已注销状态：只显示恢复按钮 -->
                                            <button type="button" class="btn btn-outline-success restore-user"
                                                    data-id="${user.userId}"
                                                    title="恢复">
                                                <i class="fas fa-undo"></i>
                                            </button>
                                        `
                                        : ''}
                                </div>
                            </td>
                        </tr>
                    `;
                });
                tableBody.innerHTML = rows;
            }

            // 渲染分页
            this.renderPagination(currentPage, totalPages);
        },

        // 渲染分页
        renderPagination: function(currentPage, totalPages) {
            const paginationEl = document.getElementById('pagination');
            if (!paginationEl || totalPages <= 1) {
                if (paginationEl) paginationEl.innerHTML = '';
                return;
            }

            let paginationHtml = '';

            // 上一页按钮
            if (currentPage > 1) {
                paginationHtml += `
                    <li class="page-item">
                        <a class="page-link" href="#" onclick="userManager.loadUserList(${currentPage - 1}, '${this.searchKeyword}'); return false;">
                            上一页
                        </a>
                    </li>
                `;
            } else {
                paginationHtml += '<li class="page-item disabled"><a class="page-link" href="#">上一页</a></li>';
            }

            // 页码按钮
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
                            <a class="page-link" href="#" onclick="userManager.loadUserList(${i}, '${this.searchKeyword}'); return false;">
                                ${i}
                            </a>
                        </li>
                    `;
                }
            }

            // 下一页按钮
            if (currentPage < totalPages) {
                paginationHtml += `
                    <li class="page-item">
                        <a class="page-link" href="#" onclick="userManager.loadUserList(${currentPage + 1}, '${this.searchKeyword}'); return false;">
                            下一页
                        </a>
                    </li>
                `;
            } else {
                paginationHtml += '<li class="page-item disabled"><a class="page-link" href="#">下一页</a></li>';
            }

            paginationEl.innerHTML = paginationHtml;
        },

        // 切换用户状态
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
                } else {
                    alert(`${action}失败: ${data?.message || '未知错误'}`);
                }
            })
            .catch(error => {
                console.error(`${action}失败:`, error);
                alert(`${action}失败: ${error.message}`);
            });
        },

        // 删除用户
        deleteUser: function(userId, username) {
            if (!confirm(`确定要删除用户 "${username}" 吗？此操作不可撤销！`)) {
                return;
            }

            fetch(`/api/admin/users/${userId}`, {
                method: 'DELETE'
            })
            .then(response => response.json())
            .then(data => {
                if (data && data.success) {
                    alert('删除成功');
                    this.loadUserList(this.currentPage, this.searchKeyword);
                } else {
                    alert(`删除失败: ${data?.message || '未知错误'}`);
                }
            })
            .catch(error => {
                console.error('删除失败:', error);
                alert('删除失败: ' + error.message);
            });
        },

        // 绑定事件
        bindEvents: function() {
            // 搜索按钮
            const searchBtn = document.getElementById('searchBtn');
            if (searchBtn) {
                searchBtn.addEventListener('click', () => {
                    const keyword = document.getElementById('searchInput')?.value || '';
                    this.loadUserList(1, keyword);
                });
            }

            // 搜索框回车
            const searchInput = document.getElementById('searchInput');
            if (searchInput) {
                searchInput.addEventListener('keypress', (e) => {
                    if (e.key === 'Enter') {
                        const keyword = searchInput.value || '';
                        this.loadUserList(1, keyword);
                    }
                });
            }

            // 刷新按钮
            const refreshBtn = document.getElementById('refreshBtn');
            if (refreshBtn) {
                refreshBtn.addEventListener('click', () => {
                    if (searchInput) searchInput.value = '';
                    this.loadUserList(1);
                });
            }
        },

        // 显示错误信息
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

        // 显示错误信息（完整）
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

    // ==== 模块注册 ====
    // 注册到全局模块系统
    if (typeof window.registerModule === 'function') {
        window.registerModule('user-manage', {
            render: function() {
                UserManage.render();
            },
            onDestroy: function() {
                UserManage.onDestroy();
            }
        });
        console.log('✅ 用户管理模块注册成功 (通过 registerModule)');
    } else if (typeof safeRegisterModule === 'function') {
        safeRegisterModule('user-manage', {
            render: function() {
                UserManage.render();
            },
            onDestroy: function() {
                UserManage.onDestroy();
            }
        });
        console.log('✅ 用户管理模块注册成功 (通过 safeRegisterModule)');
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
        console.log('✅ 用户管理模块注册成功 (通过 window.modules)');
    }

    // 全局访问
    window.userManager = UserManage;

    console.log('🎯 用户管理模块加载完成');
})();