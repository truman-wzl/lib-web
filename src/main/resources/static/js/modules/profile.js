class ProfileManager {
    constructor(container) {
        this.container = container;
    }

    init() {
        this.render();
        this.loadUserInfo();
    }

    render() {
        this.container.innerHTML = `
            <div class="container mt-4">
                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0"><i class="fas fa-user me-2"></i>个人中心</h5>
                    </div>
                    <div class="card-body">
                        <div id="user-info-area" class="text-center">
                            <div class="spinner-border text-primary" role="status">
                                <span class="visually-hidden">加载中...</span>
                            </div>
                            <p class="mt-2">加载中...</p>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    async loadUserInfo() {
        try {
            const response = await fetch('/api/auth/me', {
                credentials: 'include'
            });
            const result = await response.json();

            if (result.success) {
                this.renderUserInfo(result.data);
            } else {
                document.getElementById('user-info-area').innerHTML = `
                    <div class="alert alert-danger">加载失败: ${result.message}</div>
                `;
            }
        } catch (error) {
            console.error('加载失败:', error);
            document.getElementById('user-info-area').innerHTML = `
                <div class="alert alert-danger">网络错误，请刷新重试</div>
            `;
        }
    }

    renderUserInfo(user) {
        const container = document.getElementById('user-info-area');
        if (!container) return;
        container.innerHTML = `
            <div class="row">
                <div class="col-md-8 offset-md-2">
                    <table class="table table-bordered">
                        <tbody>
                            <tr>
                                <th width="30%">用户名</th>
                                <td>
                                    <div class="d-flex justify-content-between align-items-center">
                                        <span id="username-display">${this.escape(user.username)}</span>
                                        <button class="btn btn-sm btn-outline-primary" id="change-username-btn">修改</button>
                                    </div>
                                    <form id="username-form" class="mt-2" style="display: none;">
                                        <div class="input-group">
                                            <input type="text" class="form-control" id="new-username"
                                                   value="${this.escape(user.username)}" placeholder="输入新用户名">
                                            <button class="btn btn-primary" type="button" id="save-username-btn">保存</button>
                                            <button class="btn btn-secondary" type="button" id="cancel-username-btn">取消</button>
                                        </div>
                                    </form>
                                </td>
                            </tr>
                            <tr>
                                <th>真实姓名</th>
                                <td>
                                    <div class="d-flex justify-content-between align-items-center">
                                        <span id="realname-display">${this.escape(user.realName || '未设置')}</span>
                                        <button class="btn btn-sm btn-outline-primary" id="change-realname-btn">修改</button>
                                    </div>
                                    <form id="realname-form" class="mt-2" style="display: none;">
                                        <div class="input-group">
                                            <input type="text" class="form-control" id="new-realname"
                                                   value="${this.escape(user.realName || '')}" placeholder="输入真实姓名">
                                            <button class="btn btn-primary" type="button" id="save-realname-btn">保存</button>
                                            <button class="btn btn-secondary" type="button" id="cancel-realname-btn">取消</button>
                                        </div>
                                    </form>
                                </td>
                            </tr>

                            <tr>
                                <th>邮箱</th>
                                <td>
                                    <div class="d-flex justify-content-between align-items-center">
                                        <span id="email-display">${this.escape(user.email || '未设置')}</span>
                                        <button class="btn btn-sm btn-outline-primary" id="change-email-btn">修改</button>
                                    </div>
                                    <form id="email-form" class="mt-2" style="display: none;">
                                        <div class="input-group">
                                            <input type="email" class="form-control" id="new-email"
                                                   value="${this.escape(user.email || '')}" placeholder="输入邮箱">
                                            <button class="btn btn-primary" type="button" id="save-email-btn">保存</button>
                                            <button class="btn btn-secondary" type="button" id="cancel-email-btn">取消</button>
                                        </div>
                                    </form>
                                </td>
                            </tr>

                            <tr>
                                <th>联系电话</th>
                                <td>
                                    <div class="d-flex justify-content-between align-items-center">
                                        <span id="phone-display">${this.escape(user.phone || '未设置')}</span>
                                        <button class="btn btn-sm btn-outline-primary" id="change-phone-btn">修改</button>
                                    </div>
                                    <form id="phone-form" class="mt-2" style="display: none;">
                                        <div class="input-group">
                                            <input type="tel" class="form-control" id="new-phone"
                                                   value="${this.escape(user.phone || '')}" placeholder="输入联系电话">
                                            <button class="btn btn-primary" type="button" id="save-phone-btn">保存</button>
                                            <button class="btn btn-secondary" type="button" id="cancel-phone-btn">取消</button>
                                        </div>
                                    </form>
                                </td>
                            </tr>
                            <tr>
                                <th>用户角色</th>
                                <td><span class="badge ${this.getRoleClass(user.role)}">${this.getRoleText(user.role)}</span></td>
                            </tr>
                            <tr>
                                <th>账户状态</th>
                                <td><span class="badge ${this.getStatusClass(user.status)}">${this.getStatusText(user.status)}</span></td>
                            </tr>
                            <tr>
                                <th>注册时间</th>
                                <td>${this.formatDate(user.createTime)}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        `;

        this.bindEvents();
    }

    bindEvents() {
        document.getElementById('change-username-btn')?.addEventListener('click', () => {
            document.getElementById('username-form').style.display = 'block';
        });

        document.getElementById('cancel-username-btn')?.addEventListener('click', () => {
            document.getElementById('username-form').style.display = 'none';
        });
        document.getElementById('save-username-btn')?.addEventListener('click', async () => {
            const newUsername = document.getElementById('new-username').value.trim();
            if (!newUsername) {
                alert('请输入新用户名');
                return;
            }

            try {
                const response = await fetch('/api/auth/update-username', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ username: newUsername })
                });

                const result = await response.json();
                if (result.success) {
                    document.getElementById('username-display').textContent = newUsername;
                    document.getElementById('username-form').style.display = 'none';
                    alert('用户名修改成功');
                } else {
                    alert('修改失败: ' + result.message);
                }
            } catch (error) {
                alert('网络错误，请重试');
            }
        });
        this.bindFieldEvent('realname', '真实姓名', async (value) => {
            const response = await fetch('/api/auth/update-info', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ realName: value })
            });
            return response.json();
        });
        this.bindFieldEvent('email', '邮箱', async (value) => {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(value)) {
                throw new Error('邮箱格式不正确');
            }

            const response = await fetch('/api/auth/update-info', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ email: value })
            });
            return response.json();
        });
        this.bindFieldEvent('phone', '电话', async (value) => {
            const phoneRegex = /^1[3-9]\d{9}$/;
            if (!phoneRegex.test(value)) {
                throw new Error('请输入有效的手机号');
            }

            const response = await fetch('/api/auth/update-info', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ phone: value })
            });
            return response.json();
        });
    }
    bindFieldEvent(fieldName, fieldLabel, apiCall) {
        const changeBtn = document.getElementById(`change-${fieldName}-btn`);
        const cancelBtn = document.getElementById(`cancel-${fieldName}-btn`);
        const saveBtn = document.getElementById(`save-${fieldName}-btn`);
        const form = document.getElementById(`${fieldName}-form`);
        const input = document.getElementById(`new-${fieldName}`);
        const display = document.getElementById(`${fieldName}-display`);

        if (!changeBtn) return;

        changeBtn.addEventListener('click', () => {
            form.style.display = 'block';
        });

        cancelBtn?.addEventListener('click', () => {
            form.style.display = 'none';
        });

        saveBtn?.addEventListener('click', async () => {
            const newValue = input.value.trim();
            const oldValue = display.textContent;

            if (newValue === oldValue) {
                alert(`新${fieldLabel}不能与原${fieldLabel}相同`);
                return;
            }

            if (!newValue) {
                alert(`请输入${fieldLabel}`);
                return;
            }

            try {
                saveBtn.disabled = true;
                saveBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> 保存中...';

                const result = await apiCall(newValue);

                if (result.success) {
                    display.textContent = newValue || '未设置';
                    form.style.display = 'none';
                    alert(`${fieldLabel}修改成功`);
                } else {
                    alert(`修改失败: ${result.message}`);
                }
            } catch (error) {
                alert(error.message || '网络错误，请重试');
            } finally {
                saveBtn.disabled = false;
                saveBtn.textContent = '保存';
            }
        });
    }
    escape(html) {
        if (!html) return '';
        const div = document.createElement('div');
        div.textContent = html;
        return div.innerHTML;
    }

    formatDate(dateStr) {
        if (!dateStr) return '--';
        try {
            return new Date(dateStr).toLocaleString('zh-CN');
        } catch (e) {
            return dateStr;
        }
    }

    getRoleClass(role) {
        const map = { 'ADMIN': 'bg-danger', 'USER': 'bg-primary' };
        return map[role] || 'bg-secondary';
    }

    getRoleText(role) {
        const map = { 'ADMIN': '管理员', 'USER': '普通用户' };
        return map[role] || '未知';
    }

    getStatusClass(status) {
        const map = { 'ACTIVE': 'bg-success', 'LOCKED': 'bg-danger' };
        return map[status] || 'bg-secondary';
    }

    getStatusText(status) {
        const map = { 'ACTIVE': '正常', 'LOCKED': '已锁定' };
        return map[status] || '未知';
    }
    onDestroy() {
        console.log('profile模块清理完成');
    }
}
const profileModule = {
    render: function() {
        console.log('开始渲染 profile 模块');
        const profileManager = new ProfileManager(document.getElementById('moduleContent'));
        profileManager.init();
        this.profileManager = profileManager;
    },

    onDestroy: function() {
        console.log('清理 profile 模块');
        if (this.profileManager && this.profileManager.onDestroy) {
            this.profileManager.onDestroy();
        }
    }
};

if (typeof safeRegisterModule === 'function') {
    console.log('通过 safeRegisterModule 注册 profile 模块');
    safeRegisterModule('profile', profileModule);
} else if (typeof registerModule === 'function') {
    console.log('通过 registerModule 注册 profile 模块');
    registerModule('profile', profileModule);
} else {
    console.log('模块系统未就绪，将 profile 模块保存到 window.modules');
    if (!window.modules) window.modules = {};
    window.modules.profile = profileModule;
}