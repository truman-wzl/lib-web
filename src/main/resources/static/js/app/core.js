window.AppState = {
    currentUser: null,
    currentModule: null
};

const ModuleRegistry = {};

let isModuleSystemReady = false;

const pendingModuleRegistrations = [];

function registerModule(moduleName, moduleConfig) {
    ModuleRegistry[moduleName] = moduleConfig;
    console.log(`模块注册成功: ${moduleName}`);
    const event = new CustomEvent('module-registered', {
        detail: { moduleName, moduleConfig }
    });
    window.dispatchEvent(event);
}

function safeRegisterModule(moduleName, moduleConfig) {
    if (typeof window.registerModule === 'function') {
        window.registerModule(moduleName, moduleConfig);
    } else {
        console.log(`模块 ${moduleName} 等待注册，模块系统尚未就绪`);
        pendingModuleRegistrations.push({ moduleName, moduleConfig });

        if (!window.modules) window.modules = {};
        window.modules[moduleName] = moduleConfig;
    }
}
async function loadModule(moduleName) {
    console.log(`开始加载模块: ${moduleName}`);
    console.log('ModuleRegistry:', Object.keys(ModuleRegistry));

    if (AppState.currentModule === moduleName) {
        console.log(`模块 ${moduleName} 已是当前模块，无需重新加载`);
        return;
    }

    console.log(`加载模块: ${moduleName}`);
    AppState.currentModule = moduleName;

    updateMenuActive(moduleName);

    document.querySelector('.welcome-card').style.display = 'none';

    const contentArea = document.getElementById('moduleContent');
    contentArea.style.display = 'block';
    if (ModuleRegistry[AppState.currentModule] &&
        ModuleRegistry[AppState.currentModule].onDestroy) {
        ModuleRegistry[AppState.currentModule].onDestroy();
    }
    if (!ModuleRegistry[moduleName]) {
        try {
            console.log(`模块 ${moduleName} 未注册，开始加载JS文件...`);

            if (window.modules && window.modules[moduleName]) {
                console.log(`从 window.modules 中找到模块 ${moduleName}`);
                ModuleRegistry[moduleName] = window.modules[moduleName];
            } else {
                await loadModuleScript(moduleName);
                const maxWaitTime = 3000;
                const checkInterval = 100;
                let waitedTime = 0;
                let moduleRegistered = false;

                while (waitedTime < maxWaitTime && !moduleRegistered) {
                    // 检查 ModuleRegistry
                    if (ModuleRegistry[moduleName]) {
                        moduleRegistered = true;
                        console.log(`模块 ${moduleName} 已注册到 ModuleRegistry`);
                        break;
                    }
                    if (window.modules && window.modules[moduleName]) {
                        ModuleRegistry[moduleName] = window.modules[moduleName];
                        moduleRegistered = true;
                        console.log(`模块 ${moduleName} 从 window.modules 移动到 ModuleRegistry`);
                        break;
                    }

                    // 等待一段时间
                    await new Promise(resolve => setTimeout(resolve, checkInterval));
                    waitedTime += checkInterval;
                }

                if (!moduleRegistered) {
                    console.error(`模块 ${moduleName} 注册超时 (${maxWaitTime}ms)`);
                    console.log('ModuleRegistry:', ModuleRegistry);
                    console.log('window.modules:', window.modules);

                    const globalVarName = moduleName.replace(/-([a-z])/g, (match, p1) => p1.toUpperCase()) + 'Module';
                    if (window[globalVarName]) {
                        console.log(`从全局变量 window.${globalVarName} 找到模块`);
                        ModuleRegistry[moduleName] = window[globalVarName];
                        moduleRegistered = true;
                    }

                    if (!moduleRegistered) {
                        throw new Error(`模块 ${moduleName} 加载后未正确注册`);
                    }
                }
            }
        } catch (error) {
            console.error(`加载模块脚本失败: ${moduleName}`, error);
            showError(`加载${moduleName}模块失败: ${error.message}`);
            return;
        }
    }
    if (ModuleRegistry[moduleName] && typeof ModuleRegistry[moduleName].render === 'function') {
        console.log(`开始渲染模块: ${moduleName}`);
            contentArea.innerHTML = '';
            const html = ModuleRegistry[moduleName].render();
            console.log(`模块返回HTML长度: ${typeof html === 'string' ? html.length : '非字符串'}`);

            if (html && typeof html === 'string') {
                contentArea.innerHTML = html;
            } else {
                console.warn(`模块 ${moduleName} 的render函数没有返回字符串`);
            }
            if (typeof ModuleRegistry[moduleName].onRender === 'function') {
                console.log(`调用模块的onRender函数: ${moduleName}`);
                ModuleRegistry[moduleName].onRender();
            } else {
                console.log(`模块 ${moduleName} 没有onRender函数`);
            }
    } else {
        console.error(`模块${moduleName}未找到render方法`);
        console.log(`ModuleRegistry[${moduleName}]:`, ModuleRegistry[moduleName]);
        showError(`模块${moduleName}加载失败：缺少render方法`);
    }
}
async function loadModuleScript(moduleName) {
    return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = `js/modules/${moduleName}.js`;
        script.onload = () => {
            console.log(`模块脚本加载成功: ${moduleName}`);
            resolve();
        };
        script.onerror = () => {
            console.error(`模块脚本加载失败: ${moduleName}`);
            reject(new Error(`加载模块${moduleName}失败`));
        };
        document.head.appendChild(script);
    });
}
function updateMenuActive(moduleName) {
    document.querySelectorAll('.menu-item').forEach(item => {
        item.classList.remove('active');
        if (item.getAttribute('data-module') === moduleName) {
            item.classList.add('active');
        }
    });
}
function showError(message) {
    document.getElementById('moduleContent').innerHTML = `
        <div class="alert alert-danger">
            <strong>错误：</strong>${message}
        </div>
    `;
}
isModuleSystemReady = true;

if (pendingModuleRegistrations.length > 0) {
    console.log(`处理 ${pendingModuleRegistrations.length} 个等待注册的模块`);
    pendingModuleRegistrations.forEach(({ moduleName, moduleConfig }) => {
        registerModule(moduleName, moduleConfig);
    });
    pendingModuleRegistrations.length = 0;
}
window.dispatchEvent(new CustomEvent('module-system-ready', {
    detail: {
        ModuleRegistry,
        registerModule,
        loadModule,
        safeRegisterModule
    }
}));

(function() {
    'use strict';
    if (window.MessageBadgeManager) {
        return;
    }
    window.MessageBadgeManager = {
        updateBadge: function(count) {
            console.log('updateBadge被调用，参数:', count, '类型:', typeof count);
            const unreadCount = parseInt(count) || 0;
            console.log(`更新泡泡显示，未读消息数: ${unreadCount}`);
            let badge = document.getElementById('message-notification-badge') ||
                        document.querySelector('.message-badge');

            if (!badge) {
                console.warn('泡泡元素不存在，尝试重新创建...');
                const messageLink = document.querySelector('a[data-module="message"]');
                if (messageLink) {
                    badge = document.createElement('span');
                    badge.className = 'message-badge';
                    badge.id = 'message-notification-badge';
                    badge.style.cssText = `
                        position: absolute;
                        top: 0;
                        right: 0;
                        background: #ff4757;
                        color: white;
                        font-size: 10px;
                        font-weight: bold;
                        padding: 1px 5px;
                        border-radius: 8px;
                        min-width: 16px;
                        height: 16px;
                        text-align: center;
                        line-height: 14px;
                        display: ${unreadCount > 0 ? 'block' : 'none'};
                        z-index: 1000;
                        border: 1px solid white;
                        box-shadow: 0 1px 3px rgba(0,0,0,0.3);
                    `;
                    badge.textContent = unreadCount;
                    messageLink.style.position = 'relative';
                    messageLink.appendChild(badge);
                } else {
                    console.error('无法创建泡泡：找不到消息菜单项');
                    return;
                }
            }
            if (unreadCount > 0) {
                badge.textContent = unreadCount > 99 ? '99+' : unreadCount;
                badge.style.display = 'block';
                badge.style.opacity = '1';
                badge.style.visibility = 'visible';
                badge.style.transform = 'scale(1)';

                console.log(`显示泡泡，数量: ${badge.textContent}`);
            } else {
                badge.style.display = 'none';
            }
            if (unreadCount > 0) {
                badge.style.animation = 'none';
                setTimeout(() => {
                    badge.style.animation = 'badgePulse 0.6s ease-in-out';
                }, 10);
            }
        },
        startPeriodicCheck: function() {
            setInterval(() => {
                this.checkUnreadCount();
            }, 30000);
        },
        checkUnreadCount: async function() {
            try {
                const response = await fetch('/api/messages/unread-count', {
                    method: 'GET',
                    credentials: 'include',
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json'
                    }
                });
                console.log('API响应状态:', response.status);
                if (response.ok) {
                    const data = await response.json();
                    console.log('API返回数据:', data);
                    let unreadCount = 0;
                    if (data.unreadCount !== undefined && data.unreadCount !== null) {
                        unreadCount = parseInt(data.unreadCount) || 0;
                        console.log(`使用unreadCount字段: ${unreadCount}`);
                    }
                    else if (data.count !== undefined && data.count !== null) {
                        unreadCount = parseInt(data.count) || 0;
                        console.log(`使用count字段: ${unreadCount}`);
                    }
                    else if (data.data !== undefined && data.data !== null) {
                        unreadCount = parseInt(data.data) || 0;
                        console.log(`使用data字段: ${unreadCount}`);
                    }
                    else {
                        console.warn('API返回格式不明确，尝试查找数值字段...');
                        console.log('数据所有字段:', Object.keys(data));
                        for (let key in data) {
                            if (typeof data[key] === 'number') {
                                unreadCount = data[key];
                                console.log(`使用字段 ${key}: ${unreadCount}`);
                                break;
                            }
                        }
                    }
                    console.log(`最终未读数量: ${unreadCount}`);
                    this.updateBadge(unreadCount);

                } else {
                    console.warn(`API请求失败: ${response.status}`);
                    this.updateBadge(0);
                }
            } catch (error) {
                console.error('获取未读消息失败:', error);
                this.updateBadge(0);
            }
        }
    };
})();

class ExportManager {
    static exportToExcel(url, moduleName, filename) {
        if (!confirm(`确定要导出${moduleName}吗？`)) {
            return;
        }
        try {
            ExportManager.showLoading(`正在导出${moduleName}，请稍候...`);
            //隐藏的a标签触发下载
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            a.style.display = 'none';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            setTimeout(() => {
                ExportManager.hideLoading();
                ExportManager.showMessage(`${moduleName}导出成功！`, 'success');
            }, 1500);

        } catch (error) {
            console.error('导出失败:', error);
            ExportManager.hideLoading();
            ExportManager.showMessage('导出失败: ' + error.message, 'error');
        }
    }
    static showLoading(message) {
        const loadingDiv = document.createElement('div');
        loadingDiv.id = 'export-loading';
        loadingDiv.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 9999;
        `;

        loadingDiv.innerHTML = `
            <div style="
                background: white;
                padding: 30px;
                border-radius: 8px;
                text-align: center;
                box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
            ">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
                <p class="mt-3" style="margin-top: 20px;">${message}</p>
            </div>
        `;

        document.body.appendChild(loadingDiv);
    }
    static hideLoading() {
        const loading = document.getElementById('export-loading');
        if (loading) {
            loading.remove();
        }
    }
    static showMessage(message, type) {
        if (window.showToast) {
            window.showToast(message, type);
        } else if (window.showMessage) {
            window.showMessage(message, type);
        } else {
            alert(message);
        }
    }
}
window.ExportManager = ExportManager;
window.loadModule = loadModule;
window.registerModule = registerModule;
window.safeRegisterModule = safeRegisterModule;
window.MessageBadgeManager.init();
window.addEventListener('menu-rendered', function(event) {
    console.log('收到菜单渲染完成事件，准备初始化泡泡', event.detail);

    if (!window.AppState.currentUser) {
        return;
    }
    setTimeout(() => {
        const existingBadge = document.querySelector('.message-badge');
        if (existingBadge) {
            return;
        }
        if (window.MessageBadgeManager) {
            if (window.MessageBadgeManager.checkInterval) {
                clearInterval(window.MessageBadgeManager.checkInterval);
                window.MessageBadgeManager.checkInterval = null;
            }
            window.MessageBadgeManager.setupBadge();
            setTimeout(() => {
                if (window.MessageBadgeManager.checkUnreadCount) {
                    window.MessageBadgeManager.checkUnreadCount();
                }
            }, 1000);
        }
    }, 300);
});
document.addEventListener('pageChange', function(event) {
    if (event.detail && event.detail.module === 'message') {
        setTimeout(() => {
            if (window.MessageBadgeManager && window.MessageBadgeManager.checkUnreadCount) {
                window.MessageBadgeManager.checkUnreadCount();
            }
        }, 1000);
    }
});
document.addEventListener('message-read', function() {
    setTimeout(() => {
        if (window.MessageBadgeManager && window.MessageBadgeManager.checkUnreadCount) {
            window.MessageBadgeManager.checkUnreadCount();
        }
    }, 500);
});