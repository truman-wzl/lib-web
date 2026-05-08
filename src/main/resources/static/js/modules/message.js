//用户消息中心
console.log("!!! message.js 开始执行 !!!");
const MessageModule = {
    name: '系统消息',
    render: function() {
        console.log("渲染消息模块");
        return `
        <div class="container-fluid message-container">
        </div>`;
    },
    onRender: function(container) {
        console.log("消息模块 onRender 被调用");
        this.initialize(container);
    },
    initialize: function(container) {
        console.log("初始化消息模块");

        this.container = container || document.querySelector('.message-container');
        if (!this.container) {
            console.error("找不到消息容器");
            return;
        }

        this.state = {
            messages: [],
            loading: false,
            filter: 'all',
            expandedMessages: {},
            unreadCount: 0
        };

        this.loadMessages = this.loadMessages.bind(this);
        this.refreshMessages = this.refreshMessages.bind(this);
        this.changeFilter = this.changeFilter.bind(this);
        this.toggleMessage = this.toggleMessage.bind(this);
        this.markAsRead = this.markAsRead.bind(this);
        this.markAllAsRead = this.markAllAsRead.bind(this);
        this.deleteMessage = this.deleteMessage.bind(this);
        this.loadMessages();
    },

    async loadMessages() {
        console.log("开始加载消息");
        this.state.loading = true;
        this.renderMessages();

        try {
            console.log("尝试从后端获取消息...");
            const response = await fetch('/api/messages/my-messages', {
                method: 'GET',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                }
            });
            console.log("消息API响应状态:", response.status, response.statusText);
            if (response.ok) {
                const data = await response.json();
                console.log("消息数据获取成功:", data);
                if (data.success !== false) {
                    this.state.messages = data.messages || data.data || [];
                    this.state.unreadCount = data.unreadCount || 0;

                    this.state.expandedMessages = {};
                    this.state.messages.forEach(msg => {
                        this.state.expandedMessages[msg.id] = false;
                    });

                } else {
                    console.warn("后端返回错误:", data.message);
                    this.showError('加载失败', data.message || '未知错误');
                    this.loadMockData();
                }
            } else if (response.status === 401) {
                console.error("用户未登录，状态码 401");
                this.showError('登录过期', '请重新登录');
                setTimeout(() => {
                    window.location.href = 'index.html?module=login';
                }, 2000);
            } else {
                console.warn("消息API返回错误:", response.status, await response.text());
                this.showError('加载失败', `HTTP ${response.status}: ${response.statusText}`);
                this.loadMockData();
            }
        } catch (error) {
            console.error("加载消息失败:", error);
            this.showError('网络错误', '无法连接到服务器，使用模拟数据');
            this.loadMockData();
        } finally {
            this.state.loading = false;
            this.renderMessages();
        }
    },
    loadMockData() {
        console.log("加载模拟数据");

        const mockMessages = [
            {
                id: 1,
                userId: 1,
                borrowId: 1001,
                title: "图书逾期提醒",
                content: "您借阅的《Java编程思想》已逾期3天！\n\n借阅日期：2024-01-10\n⏰ 应还日期：2024-02-10\n🔖 图书编号：BK001\n\n请尽快到图书馆办理还书手续，以免产生更多逾期费用。",
                msgType: "OVERDUE",
                status: "UNREAD",
                createTime: new Date(Date.now() - 3600000).toISOString() // 1小时前
            },
            {
                id: 2,
                userId: 1,
                title: "成就达成",
                content: "恭喜您！达成成就：首次借阅\n感谢您使用图书管理系统，借阅了第一本图书。",
                msgType: "ACHIEVEMENT",
                status: "READ",
                createTime: new Date(Date.now() - 86400000).toISOString()
            }
        ];

        this.state.messages = mockMessages;
        this.state.unreadCount = mockMessages.filter(msg => msg.status === 'UNREAD').length;
        this.state.expandedMessages = {};
        mockMessages.forEach(msg => {
            this.state.expandedMessages[msg.id] = false;
        });
    },
    renderMessages() {
        console.log("渲染消息列表");

        if (!this.container) {
            console.error("容器不存在，无法渲染");
            return;
        }

        const { messages, loading, filter, expandedMessages, unreadCount } = this.state;
        let filteredMessages = messages;
        if (filter === 'UNREAD') {
            filteredMessages = messages.filter(msg => msg.status === 'UNREAD');
        } else if (filter !== 'all') {
            filteredMessages = messages.filter(msg => msg.msgType === filter);
        }
        let html = `
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4 pt-3">
                <h3 class="page-title">
                    <i class="bi bi-envelope me-2"></i>系统消息
                    ${unreadCount > 0 ? `<span class="badge bg-danger ms-2">${unreadCount} 条未读</span>` : ''}
                </h3>
                <div>
                    ${unreadCount > 0 ? `
                    <button id="markAllReadBtn" class="btn btn-success btn-sm me-2">
                        <i class="bi bi-check-all me-1"></i>全部已读
                    </button>
                    ` : ''}
                    <button id="refreshBtn" class="btn btn-outline-primary btn-sm">
                        <i class="bi bi-arrow-clockwise me-1"></i>刷新
                    </button>
                </div>
            </div>

            <div class="row mb-4">
                <div class="col-12">
                    <div class="btn-group btn-group-sm" role="group" id="filterButtons">
                        <button type="button" data-filter="all"
                                class="btn ${filter === 'all' ? 'btn-primary' : 'btn-outline-primary'}">
                            全部消息
                        </button>
                        <button type="button" data-filter="OVERDUE"
                                class="btn ${filter === 'OVERDUE' ? 'btn-danger' : 'btn-outline-danger'}">
                            <i class="bi bi-exclamation-triangle me-1"></i>逾期提醒
                        </button>
                        <button type="button" data-filter="ACHIEVEMENT"
                                class="btn ${filter === 'ACHIEVEMENT' ? 'btn-success' : 'btn-outline-success'}">
                            <i class="bi bi-trophy me-1"></i>成就
                        </button>
                    </div>
                </div>
            </div>`;

        if (loading) {
            html += `
            <div class="text-center py-5">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">加载中...</span>
                </div>
                <p class="text-muted mt-2">正在加载消息...</p>
            </div>`;
        }
        else if (filteredMessages.length === 0) {
            html += `
            <div class="text-center py-5">
                <i class="bi bi-envelope display-1 text-muted mb-3"></i>
                <h5 class="text-muted">暂无消息</h5>
                <p class="text-muted">您还没有收到任何消息</p>
            </div>`;
        }
        else {
            html += `<div class="row"><div class="col-12"><div class="list-group">`;

            filteredMessages.forEach(msg => {
                const badgeClass = this.getMessageBadgeClass(msg.msgType);
                const isExpanded = expandedMessages[msg.id] || false;

                html += `
                <div class="list-group-item list-group-item-action border-0 mb-3 p-0 shadow-sm rounded-3 message-item"
                     data-msg-id="${msg.id}">
                    <div class="card border-0">
                        <div class="card-header bg-transparent border-0 p-3 cursor-pointer message-header"
                             data-msg-id="${msg.id}">
                            <div class="d-flex justify-content-between align-items-center">
                                <div class="d-flex align-items-center">
                                    <span class="badge ${badgeClass} me-3">
                                        <i class="bi ${this.getMessageIcon(msg.msgType)} me-1"></i>${this.getMessageTypeText(msg.msgType)}
                                    </span>
                                    <div>
                                        <h6 class="mb-1 ${msg.status === 'UNREAD' ? 'fw-bold' : ''}">
                                            ${msg.title}
                                        </h6>
                                        <small class="text-muted">
                                            <i class="bi bi-clock me-1"></i>${this.formatTime(msg.createTime)}
                                        </small>
                                    </div>
                                </div>
                                <div class="d-flex align-items-center">
                                    ${msg.status === 'UNREAD' ? `<span class="badge bg-warning me-2">未读</span>` : ''}
                                    <button class="btn btn-sm btn-outline-secondary border-0 expand-btn">
                                        <i class="bi ${isExpanded ? 'bi-chevron-up' : 'bi-chevron-down'}"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                        ${isExpanded ? `
                        <div class="card-body pt-0 message-content">
                            <div class="border-top pt-3">
                                <div class="mb-3" style="white-space: pre-line;">${msg.content}</div>
                                ${msg.borrowId ? `
                                <div class="alert alert-light border mb-3">
                                    <div class="d-flex align-items-center">
                                        <i class="bi bi-link-45deg text-primary me-2"></i>
                                        <small class="text-muted">关联借阅记录: <strong>#${msg.borrowId}</strong></small>
                                    </div>
                                </div>
                                ` : ''}
                                <div class="d-flex justify-content-between align-items-center">
                                    <div>
                                        <small class="text-muted">消息ID: ${msg.id}</small>
                                    </div>
                                    <div>
                                        ${msg.status === 'UNREAD' ? `
                                        <button class="btn btn-success btn-sm me-2 mark-read-btn" data-msg-id="${msg.id}">
                                            <i class="bi bi-check me-1"></i>标记已读
                                        </button>
                                        ` : ''}
                                        <button class="btn btn-outline-danger btn-sm delete-btn" data-msg-id="${msg.id}">
                                            <i class="bi bi-trash me-1"></i>删除
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                        ` : ''}
                    </div>
                </div>`;
            });

            html += `</div></div></div>`;
        }

        html += `</div>`;

        this.container.innerHTML = html;
        this.bindEvents();
    },

    bindEvents() {
        console.log("绑定消息模块事件");

        const refreshBtn = document.getElementById('refreshBtn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', this.refreshMessages);
        }
        const markAllReadBtn = document.getElementById('markAllReadBtn');
        if (markAllReadBtn) {
            markAllReadBtn.addEventListener('click', this.markAllAsRead);
        }

        const filterButtons = document.querySelectorAll('#filterButtons button');
        filterButtons.forEach(btn => {
            btn.addEventListener('click', (e) => {
                const filter = e.target.getAttribute('data-filter');
                this.changeFilter(filter);
            });
        });

        const messageHeaders = document.querySelectorAll('.message-header');
        messageHeaders.forEach(header => {
            header.addEventListener('click', (e) => {
                const msgId = header.getAttribute('data-msg-id');
                this.toggleMessage(parseInt(msgId));
            });
        });
        const markReadBtns = document.querySelectorAll('.mark-read-btn');
        markReadBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const msgId = parseInt(btn.getAttribute('data-msg-id'));
                this.markAsRead(msgId);
            });
        });

        const deleteBtns = document.querySelectorAll('.delete-btn');
        deleteBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const msgId = parseInt(btn.getAttribute('data-msg-id'));
                this.deleteMessage(msgId);
            });
        });
    },

    refreshMessages() {
        console.log("刷新消息");
        this.loadMessages();
    },
    changeFilter(filter) {
        console.log("切换筛选条件:", filter);
        this.state.filter = filter;
        this.renderMessages();
    },
    toggleMessage(messageId) {
        console.log("切换消息展开状态:", messageId);
        this.state.expandedMessages[messageId] = !this.state.expandedMessages[messageId];
        this.renderMessages();
    },
    getUnreadCount() {
        if (!this.state || !this.state.messages) {
            return 0;
        }
        return this.state.messages.filter(msg => msg.status === 'UNREAD').length;
    },
    async markAsRead(messageId) {
        console.log("标记消息为已读:", messageId);
        try {
            const response = await fetch(`/api/messages/${messageId}/read`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${window.AppState.token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    const message = this.state.messages.find(m => m.id === messageId);
                    if (message) {
                        message.status = 'READ';
                        this.state.unreadCount = Math.max(0, this.state.unreadCount - 1);
                    }
                    this.showSuccess('操作成功', '消息已标记为已读');
                    this.renderMessages();
                    this.notifyBadgeUpdate();
                }
            } else {
                const message = this.state.messages.find(m => m.id === messageId);
                if (message) {
                    message.status = 'READ';
                    this.state.unreadCount = Math.max(0, this.state.unreadCount - 1);
                }
                this.showSuccess('操作成功', '消息已标记为已读（测试）');
                this.renderMessages();
            }
        } catch (error) {
            console.error("标记已读失败:", error);
            const message = this.state.messages.find(m => m.id === messageId);
            if (message) {
                message.status = 'READ';
                this.state.unreadCount = Math.max(0, this.state.unreadCount - 1);
            }
            this.showSuccess('操作成功', '消息已标记为已读（模拟）');
            this.renderMessages();
        }
    },
    markAllAsRead: async function() {
        console.log("全部标记为已读");

        if (this.state.unreadCount === 0) {
            this.showInfo('提示', '没有未读消息');
            return;
        }

        if (!confirm('确定要将所有未读消息标记为已读吗？')) {
            return;
        }
        try {
            const response = await fetch('/api/messages/mark-all-read', {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                }
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    this.state.messages.forEach(msg => {
                        if (msg.status === 'UNREAD') {
                            msg.status = 'READ';
                        }
                    });
                    this.state.unreadCount = 0;
                    this.showSuccess('操作成功', data.message || '已标记所有消息为已读');
                    this.renderMessages();
                    this.notifyBadgeUpdate();
                }
            } else {
                // 模拟成功
                this.state.messages.forEach(msg => {
                    if (msg.status === 'UNREAD') {
                        msg.status = 'READ';
                    }
                });
                this.state.unreadCount = 0;

                this.showSuccess('操作成功', '已标记所有消息为已读（测试）');
                this.renderMessages();
            }
        } catch (error) {
            console.error("全部标记已读失败:", error);
            this.state.messages.forEach(msg => {
                if (msg.status === 'UNREAD') {
                    msg.status = 'READ';
                }
            });
            this.state.unreadCount = 0;
            this.showSuccess('操作成功', '已标记所有消息为已读（模拟）');
            this.renderMessages();
        }
    },
    notifyBadgeUpdate: function() {
        console.log('消息状态变化，通知泡泡更新');

        const event = new CustomEvent('message-updated', {
            detail: { unreadCount: this.getUnreadCount() }
        });
        document.dispatchEvent(event);
        if (window.updateMessageBadge) {
            window.updateMessageBadge(this.getUnreadCount());
        }
    },
    // 删除消息
    async deleteMessage(messageId) {
        console.log("删除消息:", messageId);

        const message = this.state.messages.find(m => m.id === messageId);
        if (!message) return;

        if (!confirm(`确定要删除消息"${message.title}"吗？`)) {
            return;
        }

        try {
            const response = await fetch(`/api/messages/${messageId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${window.AppState.token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    const index = this.state.messages.findIndex(m => m.id === messageId);
                    if (index !== -1) {
                        if (this.state.messages[index].status === 'UNREAD') {
                            this.state.unreadCount = Math.max(0, this.state.unreadCount - 1);
                        }
                        this.state.messages.splice(index, 1);
                    }
                    this.showSuccess('删除成功', '消息已删除');
                    this.renderMessages();
                } else {
                    this.showError('删除失败', data.message || '未知错误');
                }
            } else {
                const index = this.state.messages.findIndex(m => m.id === messageId);
                if (index !== -1) {
                    if (this.state.messages[index].status === 'UNREAD') {
                        this.state.unreadCount = Math.max(0, this.state.unreadCount - 1);
                    }
                    this.state.messages.splice(index, 1);
                }
                this.showSuccess('删除成功', '消息已删除（测试）');
                this.renderMessages();
            }
        } catch (error) {
            console.error("删除消息失败:", error);
            const index = this.state.messages.findIndex(m => m.id === messageId);
            if (index !== -1) {
                if (this.state.messages[index].status === 'UNREAD') {
                    this.state.unreadCount = Math.max(0, this.state.unreadCount - 1);
                }
                this.state.messages.splice(index, 1);
            }
            this.showSuccess('删除成功', '消息已删除（模拟）');
            this.renderMessages();
        }
    },
    getMessageBadgeClass(msgType) {
       switch(msgType) {
           case 'OVERDUE': return 'bg-danger';
           case 'ACHIEVEMENT': return 'bg-success';
           default: return 'bg-secondary';
       }
    },

    getMessageIcon(msgType) {
        switch(msgType) {
            case 'OVERDUE': return 'bi-exclamation-triangle';
            case 'ACHIEVEMENT': return 'bi-trophy';  // 奖杯图标表示成就
            default: return 'bi-info-circle';
        }
    },

    getMessageTypeText(msgType) {
        switch(msgType) {
            case 'OVERDUE': return '逾期提醒';
            case 'ACHIEVEMENT': return '成就';
            default: return '系统';
        }
    },
    formatTime(timestamp) {
        if (!timestamp) return '';

        const date = new Date(timestamp);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) {
            return '刚刚';
        } else if (diffMins < 60) {
            return `${diffMins}分钟前`;
        } else if (diffHours < 24) {
            return `${diffHours}小时前`;
        } else if (diffDays < 7) {
            return `${diffDays}天前`;
        } else {
            return date.toLocaleDateString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        }
    },
    showSuccess(title, message) {
        if (window.showAlert) {
            window.showAlert('success', title, message);
        } else {
            alert(`${title}: ${message}`);
        }
    },
    showError(title, message) {
        if (window.showAlert) {
            window.showAlert('error', title, message);
        } else {
            alert(`错误: ${title}: ${message}`);
        }
    },
    showInfo(title, message) {
        if (window.showAlert) {
            window.showAlert('info', title, message);
        } else {
            alert(`${title}: ${message}`);
        }
    },
    onDestroy: function() {
        console.log("消息模块销毁");
        this.container = null;
        this.state = null;
    }

};

console.log("定义消息模块完成");

// 注册模块
console.log("开始注册消息模块...");
if (typeof window !== 'undefined') {
    if (typeof window.safeRegisterModule === 'function') {
        window.safeRegisterModule('message', MessageModule);
        console.log("✅ 通过 window.safeRegisterModule 注册消息模块");
    } else if (typeof window.registerModule === 'function') {
        window.registerModule('message', MessageModule);
        console.log("✅ 通过 window.registerModule 注册消息模块");
    } else {
        // 备用方案
        window.MessageModule = MessageModule;
        console.log("⚠️ MessageModule 已设置为全局变量，但未自动注册");
    }
}

console.log("!!! message.js 执行结束 !!!");