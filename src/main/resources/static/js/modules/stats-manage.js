// js/modules/stats-manage.js
const statsManageModule = {
    name: '数据统计',

    // 模块状态
    state: {
        loading: false,
        trendData: null,
        topBooks: null,
        topCategories: null
    },

    // 渲染方法
    render: function() {
        return `
            <div class="container-fluid py-3">
                <h4 class="mb-4">📈 数据统计</h4>

                <!-- 借阅趋势卡片 -->
                <div class="card mb-4">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">借阅趋势（最近7天）</h5>
                        <button class="btn btn-sm btn-outline-primary" id="refreshTrendBtn">
                            <i class="bi bi-arrow-clockwise"></i> 刷新
                        </button>
                    </div>
                    <div class="card-body">
                        <div class="chart-container" style="position: relative; height:300px;">
                            <canvas id="trendChart"></canvas>
                        </div>
                    </div>
                </div>

                <div class="row">
                    <!-- 热门图书TOP 5 -->
                    <div class="col-md-6">
                        <div class="card h-100">
                            <div class="card-header">
                                <h5 class="mb-0">热门图书TOP 5</h5>
                            </div>
                            <div class="card-body">
                                <div class="table-responsive">
                                    <table class="table table-hover" id="topBooksTable">
                                        <thead>
                                            <tr>
                                                <th>排名</th>
                                                <th>图书名称</th>
                                                <th>借阅次数</th>
                                            </tr>
                                        </thead>
                                        <tbody id="topBooksBody">
                                            <!-- 数据会动态加载到这里 -->
                                            <tr>
                                                <td colspan="3" class="text-center text-muted">
                                                    <div class="spinner-border spinner-border-sm" role="status">
                                                        <span class="visually-hidden">加载中...</span>
                                                    </div>
                                                    加载中...
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 热门类别TOP 3 -->
                    <div class="col-md-6">
                        <div class="card h-100">
                            <div class="card-header">
                                <h5 class="mb-0">热门类别TOP 3</h5>
                            </div>
                            <div class="card-body">
                                <div class="chart-container" style="position: relative; height:250px;">
                                    <canvas id="categoryChart"></canvas>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    },

    // 初始化方法
    onRender: function() {
        console.log('统计模块加载');
        this.initCharts();
        this.loadData();
        this.bindEvents();
    },

    // 初始化图表
    initCharts: function() {
        // 这里初始化Chart实例
    },

    // 加载数据
    loadData: function() {
        // 加载三个卡片的数据
    },

    // 绑定事件
    bindEvents: function() {
        // 绑定刷新按钮等事件
    },

    // 销毁方法
    onDestroy: function() {
        // 清理图表实例
    }
};

// 注册模块
if (typeof safeRegisterModule === 'function') {
    safeRegisterModule('stats-manage', statsManageModule);
} else {
    // 备用注册
    if (!window.modules) window.modules = {};
    window.modules['stats-manage'] = statsManageModule;
    console.log('✅ 统计模块已注册到 window.modules');
}