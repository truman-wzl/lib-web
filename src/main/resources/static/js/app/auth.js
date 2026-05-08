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
function renderCommonParts() {
    const user = window.AppState.currentUser;
    document.getElementById('welcomeTitle').textContent =
        `欢迎回来，${user.realName || user.username}！`;
    document.getElementById('currentUserInfo').innerHTML = `
        <strong>${user.username}</strong> (${user.role === 'ADMIN' ? '管理员' : '普通用户'})
    `;
    document.getElementById('logoutLink').addEventListener('click', function(e) {
        e.preventDefault();
        const logoutModal = new bootstrap.Modal(document.getElementById('logoutConfirmModal'));
        logoutModal.show();
    });
    document.getElementById('confirmLogoutBtn').addEventListener('click', function() {
        window.location.href = 'login.html';
    });
}