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

// 导出到全局
window.loadModule = loadModule;
window.registerModule = registerModule;
window.safeRegisterModule = safeRegisterModule;