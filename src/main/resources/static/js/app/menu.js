// 菜单配置
const menuConfig = {
    USER: [
        { id: 'book-query', name: '🔍 图书查询', group: '用户功能' },
        { id: 'my-borrow', name: '📖 我的借阅', group: '用户功能' },
        { id: 'profile', name: '👤 个人中心', group: '用户功能' }
    ],
    ADMIN: [
        { id: 'category-manage', name: '📂 分类管理', group: '管理功能' }, // 新增
        { id: 'book-manage', name: '📚 图书管理', group: '管理功能' },
        { id: 'user-manage', name: '👥 用户管理', group: '管理功能' },
        { id: 'borrow-manage', name: '📊 借阅管理', group: '管理功能' },
        { id: 'stats', name: '📈 数据统计', group: '管理功能' }
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

    // 绑定菜单点击事件
    sidebar.querySelectorAll('.menu-item').forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();
            const moduleId = this.getAttribute('data-module');

            // 更新URL，支持浏览器前进后退
            const url = new URL(window.location);
            url.searchParams.set('module', moduleId);
            window.history.pushState({}, '', url);

            // 加载模块
            window.loadModule(moduleId);
        });
    });
}

/**
 * 检查模块ID是否有效
 */
function isValidModule(moduleId) {
    const user = window.AppState.currentUser;
    const config = user.role === 'ADMIN' ? menuConfig.ADMIN : menuConfig.USER;
    return config.some(item => item.id === moduleId);
}