/**
 * 获取当前登录用户信息
 */
async function fetchCurrentUser() {
    try {
        const response = await fetch('/api/auth/me');
        if (response.status === 401) {
            window.location.href = 'login.html';
            return null;
        }
        const result = await response.json();
        if (result.success) {
            window.AppState.currentUser = result.data;
            return result.data;
        } else {
            throw new Error(result.message || '获取用户信息失败');
        }
    } catch (error) {
        console.error('获取用户信息时出错:', error);
        alert('无法连接服务器，请稍后重试。');
        return null;
    }
}

/**
 * 渲染公共部分（用户信息、退出按钮）
 */
function renderCommonParts() {
    const user = window.AppState.currentUser;
    document.getElementById('welcomeTitle').textContent =
        `欢迎回来，${user.realName || user.username}！`;

    document.getElementById('currentUserInfo').innerHTML = `
        <strong>${user.username}</strong> (${user.role === 'ADMIN' ? '管理员' : '普通用户'})
    `;

    // 移除旧的 confirm 方式，改为使用 Bootstrap Modal
    document.getElementById('logoutLink').addEventListener('click', function(e) {
        e.preventDefault();

        // 显示退出确认模态框
        const logoutModal = new bootstrap.Modal(document.getElementById('logoutConfirmModal'));
        logoutModal.show();
    });

    // 绑定确认退出按钮事件
    document.getElementById('confirmLogoutBtn').addEventListener('click', function() {
        window.location.href = 'login.html';
    });
}