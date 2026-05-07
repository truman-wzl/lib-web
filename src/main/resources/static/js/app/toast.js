// 提示工具函数
class ToastHelper {

    /**
     * 显示成功提示
     * @param {string} message 提示消息
     */
    static success(message) {
        this.show(message, 'success');
    }

    /**
     * 显示错误提示
     * @param {string} message 提示消息
     */
    static error(message) {
        this.show(message, 'danger');
    }

    /**
     * 显示警告提示
     * @param {string} message 提示消息
     */
    static warning(message) {
        this.show(message, 'warning');
    }

    /**
     * 显示信息提示
     * @param {string} message 提示消息
     */
    static info(message) {
        this.show(message, 'info');
    }

    /**
     * 显示提示框
     * @param {string} message 提示消息
     * @param {string} type 提示类型：success, danger, warning, info
     * @param {number} duration 显示时间（毫秒），默认3000
     */
    static show(message, type = 'success', duration = 3000) {
        // 创建唯一的ID
        const toastId = 'toast-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);

        // 根据类型设置图标和背景色
        const icons = {
            success: 'bi-check-circle',
            danger: 'bi-x-circle',
            warning: 'bi-exclamation-triangle',
            info: 'bi-info-circle'
        };

        const bgColors = {
            success: 'bg-success',
            danger: 'bg-danger',
            warning: 'bg-warning',
            info: 'bg-info'
        };

        const icon = icons[type] || icons.success;
        const bgColor = bgColors[type] || bgColors.success;

        // 创建提示框HTML
        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-white ${bgColor} border-0"
                 role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body d-flex align-items-center">
                        <i class="bi ${icon} fs-4 me-2"></i>
                        <span>${message}</span>
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto"
                            data-bs-dismiss="toast" aria-label="关闭"></button>
                </div>
            </div>
        `;

        // 获取或创建提示容器
        let container = document.getElementById('toastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toastContainer';
            container.className = 'toast-container position-fixed top-0 end-0 p-3';
            container.style.zIndex = '9999';
            document.body.appendChild(container);
        }

        // 添加提示框到容器
        container.insertAdjacentHTML('beforeend', toastHtml);

        // 获取提示框元素
        const toastEl = document.getElementById(toastId);

        // 创建Bootstrap Toast实例
        const toast = new bootstrap.Toast(toastEl, {
            autohide: true,
            delay: duration
        });

        // 显示提示框
        toast.show();

        // 提示框隐藏后移除元素
        toastEl.addEventListener('hidden.bs.toast', function () {
            if (toastEl.parentNode) {
                toastEl.remove();
            }
        });

        return toast;
    }
}

// 将工具类挂载到全局
window.Toast = ToastHelper;