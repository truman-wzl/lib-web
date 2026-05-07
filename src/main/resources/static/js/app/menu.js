// 菜单配置
const menuConfig = {
    USER: [
        { id: 'book-query', name: ' 图书查询', group: '用户功能' },
        { id: 'my-borrow', name: ' 我的借阅', group: '用户功能' },
        { id: 'profile', name: ' 个人中心', group: '用户功能' },
        { id: 'message', name: '系统消息', group: '用户功能' },
        { id: 'stats-manage', name: ' 数据统计', group: '公共功能' }
    ],
    ADMIN: [
        { id: 'category-manage', name: ' 分类管理', group: '管理功能' },
        { id: 'book-manage', name: ' 图书管理', group: '管理功能' },
        { id: 'user-manage', name: ' 用户管理', group: '管理功能' },
        { id: 'borrow-manage', name: ' 借阅管理', group: '管理功能' },
        { id: 'stats-manage', name: '数据统计', group: '公共功能' }
    ]
};
/**
 * 渲染左侧菜单
 */
function renderSidebarMenu() {
    const sidebar = document.getElementById('sidebarMenu');
    const user = window.AppState.currentUser;
    const items = user.role === 'ADMIN' ? menuConfig.ADMIN : menuConfig.USER;

    let html = '';
    let lastGroup = '';

    items.forEach(item => {
        if (item.group !== lastGroup) {
            html += `<div class="menu-group">${item.group}</div>`;
            lastGroup = item.group;
        }
        html += `
            <a class="menu-item" href="#" data-module="${item.id}">
                ${item.name}
            </a>
        `;
    });

    sidebar.innerHTML = html;
    sidebar.querySelectorAll('.menu-item').forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();
            const moduleId = this.getAttribute('data-module');

            const url = new URL(window.location);
            url.searchParams.set('module', moduleId);
            window.history.pushState({}, '', url);

            window.loadModule(moduleId);
        });
    });

    setTimeout(() => {
        addMessageBadgeToMenu();
        if (window.MessageBadgeManager && window.MessageBadgeManager.checkUnreadCount) {
            setTimeout(() => {
                window.MessageBadgeManager.checkUnreadCount();
                window.MessageBadgeManager.startPeriodicCheck();
            }, 1000);
        }
    }, 100);
}
function addMessageBadgeToMenu() {
    const messageLink = document.querySelector('a[data-module="message"]');
    if (!messageLink) {
        console.warn('未找到消息菜单项，无法添加泡泡');
        return false;
    }
    const existingBadge = messageLink.querySelector('.message-badge');
    if (existingBadge) {
        existingBadge.remove();
    }
    const badge = document.createElement('span');
    badge.className = 'message-badge';
    badge.id = 'message-notification-badge';
    badge.style.cssText = `
        position: absolute;
        top: 2px;
        right: 2px;
        background: #ff4757;
        color: white;
        font-size: 10px;
        font-weight: bold;
        padding: 1px 4px;
        border-radius: 8px;
        min-width: 16px;
        height: 16px;
        text-align: center;
        line-height: 14px;
        display: none;
        z-index: 1000;
        border: 1px solid white;
    `;
    badge.textContent = '0';

    messageLink.style.position = 'relative';
    messageLink.appendChild(badge);

    return badge;
}
/**
 * 检查模块ID是否有效
 */
function isValidModule(moduleId) {
    const user = window.AppState.currentUser;
    const config = user.role === 'ADMIN' ? menuConfig.ADMIN : menuConfig.USER;
    return config.some(item => item.id === moduleId);
}