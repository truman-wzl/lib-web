// 全局应用状态
window.AppState = {
    currentUser: null,
    currentModule: null
};

// 模块注册表
const ModuleRegistry = {};

// 模块系统就绪标志
let isModuleSystemReady = false;

// 等待注册的模块队列
const pendingModuleRegistrations = [];

/**
 * 注册模块
 * @param {string} moduleName 模块ID
 * @param {object} moduleConfig 模块配置 {render: 渲染函数, onDestroy: 清理函数}
 */
function registerModule(moduleName, moduleConfig) {
    ModuleRegistry[moduleName] = moduleConfig;
    console.log(`✅ 模块注册成功: ${moduleName}`);

    // 触发模块注册完成事件
    const event = new CustomEvent('module-registered', {
        detail: { moduleName, moduleConfig }
    });
    window.dispatchEvent(event);
}

/**
 * 安全注册模块 - 处理模块系统未就绪的情况
 * 这个函数可以在模块JS中调用，无论模块系统是否就绪
 */
function safeRegisterModule(moduleName, moduleConfig) {
    if (typeof window.registerModule === 'function') {
        // 如果 registerModule 已就绪，直接注册
        window.registerModule(moduleName, moduleConfig);
    } else {
        // 否则，加入等待队列
        console.log(`⏳ 模块 ${moduleName} 等待注册，模块系统尚未就绪`);
        pendingModuleRegistrations.push({ moduleName, moduleConfig });

        // 同时注册到 window.modules 作为备用
        if (!window.modules) window.modules = {};
        window.modules[moduleName] = moduleConfig;
    }
}

/**
 * 加载并渲染模块
 * @param {string} moduleName 模块ID
 */
async function loadModule(moduleName) {
    console.log(`🚀 开始加载模块: ${moduleName}`);
    console.log('📊 ModuleRegistry:', Object.keys(ModuleRegistry));

    if (AppState.currentModule === moduleName) {
        console.log(`✅ 模块 ${moduleName} 已是当前模块，无需重新加载`);
        return;
    }

    console.log(`🔍 加载模块: ${moduleName}`);
    AppState.currentModule = moduleName;

    // 更新菜单激活状态
    updateMenuActive(moduleName);

    // 隐藏欢迎卡片
    document.querySelector('.welcome-card').style.display = 'none';

    // 显示模块内容区
    const contentArea = document.getElementById('moduleContent');
    contentArea.style.display = 'block';

    // 清理上一个模块
    if (ModuleRegistry[AppState.currentModule] &&
        ModuleRegistry[AppState.currentModule].onDestroy) {
        ModuleRegistry[AppState.currentModule].onDestroy();
    }

    // 动态加载模块JS（如果尚未注册）
    if (!ModuleRegistry[moduleName]) {
        try {
            console.log(`📦 模块 ${moduleName} 未注册，开始加载JS文件...`);

            // 先检查 window.modules
            if (window.modules && window.modules[moduleName]) {
                console.log(`🔍 从 window.modules 中找到模块 ${moduleName}`);
                ModuleRegistry[moduleName] = window.modules[moduleName];
            } else {
                // 加载脚本文件
                await loadModuleScript(moduleName);

                // 等待模块注册，最长等待3秒
                const maxWaitTime = 3000; // 3秒
                const checkInterval = 100; // 每100ms检查一次
                let waitedTime = 0;
                let moduleRegistered = false;

                while (waitedTime < maxWaitTime && !moduleRegistered) {
                    // 检查 ModuleRegistry
                    if (ModuleRegistry[moduleName]) {
                        moduleRegistered = true;
                        console.log(`✅ 模块 ${moduleName} 已注册到 ModuleRegistry`);
                        break;
                    }

                    // 检查 window.modules
                    if (window.modules && window.modules[moduleName]) {
                        ModuleRegistry[moduleName] = window.modules[moduleName];
                        moduleRegistered = true;
                        console.log(`✅ 模块 ${moduleName} 从 window.modules 移动到 ModuleRegistry`);
                        break;
                    }

                    // 等待一段时间
                    await new Promise(resolve => setTimeout(resolve, checkInterval));
                    waitedTime += checkInterval;
                }

                if (!moduleRegistered) {
                    console.error(`⏰ 模块 ${moduleName} 注册超时 (${maxWaitTime}ms)`);
                    console.log('ModuleRegistry:', ModuleRegistry);
                    console.log('window.modules:', window.modules);

                    // 最后尝试从 window 全局变量查找
                    const globalVarName = moduleName.replace(/-([a-z])/g, (match, p1) => p1.toUpperCase()) + 'Module';
                    if (window[globalVarName]) {
                        console.log(`🔍 从全局变量 window.${globalVarName} 找到模块`);
                        ModuleRegistry[moduleName] = window[globalVarName];
                        moduleRegistered = true;
                    }

                    if (!moduleRegistered) {
                        throw new Error(`模块 ${moduleName} 加载后未正确注册`);
                    }
                }
            }
        } catch (error) {
            console.error(`❌ 加载模块脚本失败: ${moduleName}`, error);
            showError(`加载${moduleName}模块失败: ${error.message}`);
            return;
        }
    }

    // 执行模块渲染
    if (ModuleRegistry[moduleName] && typeof ModuleRegistry[moduleName].render === 'function') {
        console.log(`🎨 开始渲染模块: ${moduleName}`);
            contentArea.innerHTML = ''; // 清空内容

            // ✅ 获取模块返回的HTML
            const html = ModuleRegistry[moduleName].render();
            console.log(`📄 模块返回HTML长度: ${typeof html === 'string' ? html.length : '非字符串'}`);

            if (html && typeof html === 'string') {
                contentArea.innerHTML = html;  // ✅ 设置HTML到页面
            } else {
                console.warn(`⚠️ 模块 ${moduleName} 的render函数没有返回字符串`);
            }

            // ✅ 调用模块的初始化函数
            if (typeof ModuleRegistry[moduleName].onRender === 'function') {
                console.log(`🔧 调用模块的onRender函数: ${moduleName}`);
                ModuleRegistry[moduleName].onRender();
            } else {
                console.log(`ℹ️ 模块 ${moduleName} 没有onRender函数`);
            }
    } else {
        console.error(`❌ 模块${moduleName}未找到render方法`);
        console.log(`ModuleRegistry[${moduleName}]:`, ModuleRegistry[moduleName]);
        showError(`模块${moduleName}加载失败：缺少render方法`);
    }
}

/**
 * 动态加载模块JS文件
 */
async function loadModuleScript(moduleName) {
    return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = `js/modules/${moduleName}.js`;
        script.onload = () => {
            console.log(`✅ 模块脚本加载成功: ${moduleName}`);
            resolve();
        };
        script.onerror = () => {
            console.error(`❌ 模块脚本加载失败: ${moduleName}`);
            reject(new Error(`加载模块${moduleName}失败`));
        };
        document.head.appendChild(script);
    });
}

/**
 * 更新菜单激活状态
 */
function updateMenuActive(moduleName) {
    document.querySelectorAll('.menu-item').forEach(item => {
        item.classList.remove('active');
        if (item.getAttribute('data-module') === moduleName) {
            item.classList.add('active');
        }
    });
}

/**
 * 显示错误信息
 */
function showError(message) {
    document.getElementById('moduleContent').innerHTML = `
        <div class="alert alert-danger">
            <strong>错误：</strong>${message}
        </div>
    `;
}

// 标记模块系统就绪
isModuleSystemReady = true;

// 处理等待注册的模块
if (pendingModuleRegistrations.length > 0) {
    console.log(`🔄 处理 ${pendingModuleRegistrations.length} 个等待注册的模块`);
    pendingModuleRegistrations.forEach(({ moduleName, moduleConfig }) => {
        registerModule(moduleName, moduleConfig);
    });
    pendingModuleRegistrations.length = 0; // 清空队列
}

// 触发模块系统就绪事件
window.dispatchEvent(new CustomEvent('module-system-ready', {
    detail: {
        ModuleRegistry,
        registerModule,
        loadModule,
        safeRegisterModule
    }
}));

console.log('✅ 模块系统已就绪');
// ========================================
// 消息泡泡管理器 - 动态样式和功能
// ========================================

(function() {
    'use strict';

    // 检查是否已有消息泡泡管理器
    if (window.MessageBadgeManager) {
        return;
    }

    // 消息泡泡管理器
    // 在core.js中简化MessageBadgeManager
    window.MessageBadgeManager = {
        // 只需保留核心功能
        updateBadge: function(count) {
            console.log('🔄 updateBadge被调用，参数:', count, '类型:', typeof count);

            // 确保count是数字
            const unreadCount = parseInt(count) || 0;
            console.log(`📊 更新泡泡显示，未读消息数: ${unreadCount}`);

            // 查找泡泡元素
            let badge = document.getElementById('message-notification-badge') ||
                        document.querySelector('.message-badge');

            if (!badge) {
                console.warn('⚠️ 泡泡元素不存在，尝试重新创建...');

                // 尝试重新创建泡泡
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
                    console.log('✅ 重新创建泡泡');
                } else {
                    console.error('❌ 无法创建泡泡：找不到消息菜单项');
                    return;
                }
            }

            // 更新泡泡
            if (unreadCount > 0) {
                badge.textContent = unreadCount > 99 ? '99+' : unreadCount;
                badge.style.display = 'block';
                badge.style.opacity = '1';
                badge.style.visibility = 'visible';
                badge.style.transform = 'scale(1)';

                console.log(`✅ 显示泡泡，数量: ${badge.textContent}`);
            } else {
                badge.style.display = 'none';
                console.log('📭 隐藏泡泡，无未读消息');
            }

            // 添加动画效果
            if (unreadCount > 0) {
                badge.style.animation = 'none';
                setTimeout(() => {
                    badge.style.animation = 'badgePulse 0.6s ease-in-out';
                }, 10);
            }
        },
        // 定期检查函数
        startPeriodicCheck: function() {
            // 每30秒检查一次
            setInterval(() => {
                this.checkUnreadCount();
            }, 30000);
        },

        // 修改checkUnreadCount方法
        checkUnreadCount: async function() {
            console.log('🔍 开始检查未读消息...');

            try {
                const response = await fetch('/api/messages/unread-count', {
                    method: 'GET',
                    credentials: 'include',
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json'
                    }
                });

                console.log('📡 API响应状态:', response.status);

                if (response.ok) {
                    const data = await response.json();
                    console.log('📊 API返回数据:', data);

                    // 🔥 关键修复：使用正确的字段名
                    let unreadCount = 0;

                    // 从您的截图看，API返回的是 {success: true, unreadCount: 1}
                    if (data.unreadCount !== undefined && data.unreadCount !== null) {
                        unreadCount = parseInt(data.unreadCount) || 0;
                        console.log(`✅ 使用unreadCount字段: ${unreadCount}`);
                    }
                    // 如果API返回的是 {success: true, count: 1}
                    else if (data.count !== undefined && data.count !== null) {
                        unreadCount = parseInt(data.count) || 0;
                        console.log(`✅ 使用count字段: ${unreadCount}`);
                    }
                    // 如果API返回的是 {success: true, data: 1}
                    else if (data.data !== undefined && data.data !== null) {
                        unreadCount = parseInt(data.data) || 0;
                        console.log(`✅ 使用data字段: ${unreadCount}`);
                    }
                    // 如果API返回的是其他格式
                    else {
                        console.warn('⚠️ API返回格式不明确，尝试查找数值字段...');
                        console.log('数据所有字段:', Object.keys(data));

                        // 遍历所有字段，寻找数字类型的值
                        for (let key in data) {
                            if (typeof data[key] === 'number') {
                                unreadCount = data[key];
                                console.log(`🔍 使用字段 ${key}: ${unreadCount}`);
                                break;
                            }
                        }
                    }

                    console.log(`🎯 最终未读数量: ${unreadCount}`);
                    this.updateBadge(unreadCount);

                } else {
                    console.warn(`⚠️ API请求失败: ${response.status}`);
                    this.updateBadge(0);
                }
            } catch (error) {
                console.error('❌ 获取未读消息失败:', error);
                this.updateBadge(0);
            }
        }
    };
})();


/**
 * 通用Excel导出管理器
 */
class ExportManager {
    /**
     * 导出数据到Excel
     * @param {string} url 导出接口地址
     * @param {string} moduleName 模块名称（用于提示）
     * @param {string} filename 导出的文件名
     */
    static exportToExcel(url, moduleName, filename) {
        if (!confirm(`确定要导出${moduleName}吗？`)) {
            return;
        }

        try {
            // 显示加载提示
            ExportManager.showLoading(`正在导出${moduleName}，请稍候...`);

            // 创建隐藏的a标签触发下载
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            a.style.display = 'none';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);

            // 延迟隐藏加载提示
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

    /**
     * 显示加载提示
     */
    static showLoading(message) {
        // 创建加载提示
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

    /**
     * 隐藏加载提示
     */
    static hideLoading() {
        const loading = document.getElementById('export-loading');
        if (loading) {
            loading.remove();
        }
    }

    /**
     * 显示消息提示
     */
    static showMessage(message, type) {
        // 使用你现有的消息提示系统
        if (window.showToast) {
            window.showToast(message, type);
        } else if (window.showMessage) {
            window.showMessage(message, type);
        } else {
            alert(message);
        }
    }
}

// 注册到全局
window.ExportManager = ExportManager;
// 导出到全局
window.loadModule = loadModule;
window.registerModule = registerModule;
window.safeRegisterModule = safeRegisterModule;

// 自动初始化
window.MessageBadgeManager.init();

// 监听菜单渲染完成事件
window.addEventListener('menu-rendered', function(event) {
    console.log('📁 收到菜单渲染完成事件，准备初始化泡泡', event.detail);

    // 确保用户已登录
    if (!window.AppState.currentUser) {
        console.log('👤 用户未登录，跳过泡泡初始化');
        return;
    }

    // 延迟初始化，确保DOM完全加载
    setTimeout(() => {
        console.log('🎯 检查泡泡管理器状态...');

        // 检查泡泡是否存在
        const existingBadge = document.querySelector('.message-badge');
        if (existingBadge) {
            console.log('✅ 泡泡已存在，跳过重新初始化');
            return;
        }

        console.log('🎯 初始化消息泡泡管理器');

        // 重新初始化
        if (window.MessageBadgeManager) {
            // 如果已有定时器，先清理
            if (window.MessageBadgeManager.checkInterval) {
                clearInterval(window.MessageBadgeManager.checkInterval);
                window.MessageBadgeManager.checkInterval = null;
            }

            // 重新设置泡泡
            window.MessageBadgeManager.setupBadge();

            // 立即检查一次未读消息
            setTimeout(() => {
                if (window.MessageBadgeManager.checkUnreadCount) {
                    window.MessageBadgeManager.checkUnreadCount();
                }
            }, 1000);
        }
    }, 300);
});

// 监听页面切换事件
document.addEventListener('pageChange', function(event) {
    if (event.detail && event.detail.module === 'message') {
        console.log('📨 切换到消息页面，刷新泡泡');
        setTimeout(() => {
            if (window.MessageBadgeManager && window.MessageBadgeManager.checkUnreadCount) {
                window.MessageBadgeManager.checkUnreadCount();
            }
        }, 1000);
    }
});

// 监听消息状态变化事件
document.addEventListener('message-read', function() {
    console.log('📩 收到消息已读事件，更新泡泡');
    setTimeout(() => {
        if (window.MessageBadgeManager && window.MessageBadgeManager.checkUnreadCount) {
            window.MessageBadgeManager.checkUnreadCount();
        }
    }, 500);
});