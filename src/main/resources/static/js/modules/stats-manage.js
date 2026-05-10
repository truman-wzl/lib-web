const statsManageModule = {
    name: '数据统计',
    state: {
        loading: false,
        trendData: null,
        topBooks: null,
        topCategories: null,
        trendChart: null,
        categoryChart: null
    },
    render: function() {
        return `
            <div class="container-fluid py-3">
                <h4 class="mb-4">数据统计</h4>
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
                    <div class="col-md-6">
                        <div class="card h-100">
                            <div class="card-header">
                                <h5 class="mb-0">热门图书TOP 5</h5>
                            </div>
                            <div class="card-body">
                                <div class="table-responsive">
                                    <table class="table table-hover table-sm" id="topBooksTable">
                                        <thead>
                                            <tr>
                                                <th width="15%">排名</th>
                                                <th width="50%">图书名称</th>
                                                <th width="20%">作者</th>
                                                <th width="15%">借阅次数</th>
                                            </tr>
                                        </thead>
                                        <tbody id="topBooksBody">
                                            <tr>
                                                <td colspan="4" class="text-center text-muted py-4">
                                                    <div class="spinner-border spinner-border-sm" role="status">
                                                        <span class="visually-hidden">加载中...</span>
                                                    </div>
                                                    <span class="ms-2">加载中...</span>
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card h-100">
                            <div class="card-header">
                                <h5 class="mb-0">热门类别TOP 3</h5>
                            </div>
                            <div class="card-body">
                                <div class="chart-container" style="position: relative; height:250px;">
                                    <canvas id="categoryChart"></canvas>
                                </div>
                                <div class="mt-3" id="categoryDetail">
                                    <table class="table table-sm table-borderless mb-0">
                                        <tbody id="categoryDetailBody">
                                            <tr>
                                                <td colspan="2" class="text-center text-muted">
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
                </div>
            </div>
        `;
    },
    onRender: function() {
        this.bindEvents();
        this.waitForDOMReady();
    },
    waitForDOMReady: function() {
        const checkDOM = () => {
            const trendChart = document.getElementById('trendChart');
            const categoryChart = document.getElementById('categoryChart');
            if (trendChart && categoryChart) {
                this.initCharts();
                this.loadData();
            } else {
                setTimeout(checkDOM, 50);
            }
        };
        checkDOM();
    },
    initCharts: function() {
        if (this.state.trendChart) {
            this.state.trendChart.destroy();
            this.state.trendChart = null;
        }
        if (this.state.categoryChart) {
            this.state.categoryChart.destroy();
            this.state.categoryChart = null;
        }
    },
    loadData: function() {
        this.state.loading = true;
        this.loadBorrowTrend();
        this.loadTopBooks();
        this.loadTopCategories();
    },
    loadBorrowTrend: function() {
        const canvas = document.getElementById('trendChart');
        if (!canvas) {
            console.error('严重错误：trendChart元素不存在！');
            return;
        }

        const chartContainer = canvas.parentElement;
        if (!chartContainer) {
            console.error('找不到trendChart的父元素');
            return;
        }
        this.showLoadingOverlay(canvas, '加载借阅趋势数据...');

        fetch('/api/stats/borrow-trend')
            .then(response => {
                if (!response.ok) throw new Error(`HTTP错误 ${response.status}`);
                return response.json();
            })
            .then(data => {
                this.removeLoadingOverlay(canvas);
                if (data.success) {
                    this.state.trendData = data.data;
                    this.renderTrendChart();
                } else {
                    throw new Error(data.message || '获取数据失败');
                }
            })
            .catch(error => {
                console.error('加载借阅趋势失败:', error);
                this.removeLoadingOverlay(canvas);
                this.showErrorOverlay(canvas, `加载失败: ${error.message}`);
            });
    },
    showLoadingOverlay: function(canvas, message) {
        const container = canvas.parentElement;
        const overlay = document.createElement('div');
        overlay.className = 'chart-loading-overlay';
        overlay.style.cssText = `
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(255, 255, 255, 0.9);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 10;
        `;

        const spinner = document.createElement('div');
        spinner.className = 'spinner-border text-primary';
        spinner.setAttribute('role', 'status');

        const text = document.createElement('p');
        text.className = 'mt-2 mb-0';
        text.textContent = message || '加载中...';

        overlay.appendChild(spinner);
        overlay.appendChild(text);
        canvas.chartOverlay = overlay;
        container.style.position = 'relative';
        container.insertBefore(overlay, canvas);
    },

    removeLoadingOverlay: function(canvas) {
        if (canvas.chartOverlay && canvas.chartOverlay.parentNode) {
            canvas.chartOverlay.parentNode.removeChild(canvas.chartOverlay);
            canvas.chartOverlay = null;
        }
    },

    showErrorOverlay: function(canvas, errorMessage) {
        const container = canvas.parentElement;

        const errorDiv = document.createElement('div');
        errorDiv.className = 'alert alert-danger alert-dismissible fade show';
        errorDiv.style.cssText = `
            position: absolute;
            top: 10px;
            left: 10px;
            right: 10px;
            z-index: 20;
        `;
        errorDiv.innerHTML = `
            <strong>错误：</strong>${errorMessage}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;

        container.appendChild(errorDiv);
        setTimeout(() => {
            if (errorDiv.parentNode) {
                errorDiv.parentNode.removeChild(errorDiv);
            }
        }, 5000);
    },
    renderTrendChart: function() {
        if (!this.state.trendData) {
            return;
        }
        const ctx = document.getElementById('trendChart');
        if (!ctx) {
            console.error('找不到trendChart元素');
            return;
        }

        const ctx2d = ctx.getContext('2d');
        if (!ctx2d) {
            console.error('无法获取canvas上下文');
            return;
        }
        if (this.state.trendChart) {
            this.state.trendChart.destroy();
        }

        const dates = this.state.trendData.dates;
        const counts = this.state.trendData.counts;
        const displayDates = dates.map(date => {
            const year = date.substring(0, 4);
            const currentYear = new Date().getFullYear().toString();
            return year === currentYear ? date.substring(5) : date;
        });

        try {
            this.state.trendChart = new Chart(ctx2d, {
                type: 'line',
                data: {
                    labels: displayDates,
                    datasets: [{
                        label: '借阅次数',
                        data: counts,
                        borderColor: 'rgba(59, 130, 246, 1)',
                        backgroundColor: 'rgba(59, 130, 246, 0.1)',
                        borderWidth: 2,
                        tension: 0.3,
                        pointBackgroundColor: 'rgba(59, 130, 246, 1)',
                        pointBorderColor: '#fff',
                        pointBorderWidth: 2,
                        pointRadius: 5,
                        pointHoverRadius: 7
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: true,
                            position: 'top',
                            labels: {
                                font: {
                                    size: 12
                                }
                            }
                        },
                        tooltip: {
                            mode: 'index',
                            intersect: false,
                            callbacks: {
                                label: function(context) {
                                    const label = context.dataset.label || '';
                                    const value = context.parsed.y;
                                    const dateIndex = context.dataIndex;
                                    const fullDate = dates[dateIndex];
                                    return `${label}: ${value}次 (${fullDate})`;
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            grid: {
                                display: true,
                                color: 'rgba(0, 0, 0, 0.05)'
                            }
                        },
                        y: {
                            beginAtZero: true,
                            ticks: {
                                stepSize: 1,
                                precision: 0
                            },
                            grid: {
                                display: true,
                                color: 'rgba(0, 0, 0, 0.05)'
                            }
                        }
                    },
                    interaction: {
                        intersect: false,
                        mode: 'index'
                    }
                }
            });
        } catch (error) {
            console.error('渲染借阅趋势图表失败:', error);
            const container = ctx.parentElement;
            container.innerHTML = `
                <div class="alert alert-warning">
                    <strong>图表渲染失败：</strong>${error.message}
                </div>
            `;
        }
    },
    loadTopBooks: function() {
        const tableBody = document.getElementById('topBooksBody');
        if (!tableBody) {
            console.error('找不到topBooksBody元素');
            return;
        }

        tableBody.innerHTML = `
            <tr>
                <td colspan="4" class="text-center text-muted py-4">
                    <div class="spinner-border spinner-border-sm" role="status">
                        <span class="visually-hidden">加载中...</span>
                    </div>
                    <span class="ms-2">加载中...</span>
                </td>
            </tr>
        `;

        fetch('/api/stats/top-books')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP错误 ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                if (data.success) {
                    this.state.topBooks = data.data;
                    this.renderTopBooks();
                } else {
                    throw new Error(data.message || '获取数据失败');
                }
            })
            .catch(error => {
                console.error('加载热门图书失败:', error);
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="4" class="text-center text-danger py-3">
                            <i class="bi bi-exclamation-triangle"></i>
                            <span class="ms-2">加载失败: ${error.message}</span>
                        </td>
                    </tr>
                `;
            });
    },

    renderTopBooks: function() {
        if (!this.state.topBooks) {
            return;
        }
        const tableBody = document.getElementById('topBooksBody');
        if (!tableBody) {
            console.error('找不到topBooksBody元素');
            return;
        }

        if (this.state.topBooks.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center text-muted py-4">
                        <i class="bi bi-info-circle"></i>
                        <span class="ms-2">暂无借阅数据</span>
                    </td>
                </tr>
            `;
            return;
        }

        let html = '';
        const rankColors = ['#FFD700', '#C0C0C0', '#CD7F32', '#3498db', '#2ecc71'];
        this.state.topBooks.forEach((book, index) => {
            const rankColor = rankColors[index] || '#6c757d';
            html += `
                <tr>
                    <td>
                        <span class="badge" style="background-color: ${rankColor}; color: white; min-width: 30px;">
                            ${book.rank}
                        </span>
                    </td>
                    <td>
                        <div class="fw-semibold">${this.escapeHtml(book.bookName)}</div>
                        <small class="text-muted">${this.escapeHtml(book.publisher || '')}</small>
                    </td>
                    <td>${this.escapeHtml(book.author || '未知作者')}</td>
                    <td>
                        <span class="badge bg-info rounded-pill">${book.borrowCount}次</span>
                    </td>
                </tr>
            `;
        });

        tableBody.innerHTML = html;
    },

    loadTopCategories: function() {
        const canvas = document.getElementById('categoryChart');
        if (!canvas) {
            console.error('categoryChart元素不存在！');
            return;
        }
        const detailBody = document.getElementById('categoryDetailBody');
        this.showLoadingOverlay(canvas, '加载类别数据...');
        if (detailBody) {
            detailBody.innerHTML = `
                <tr>
                    <td colspan="2" class="text-center text-muted">
                        <div class="spinner-border spinner-border-sm" role="status"></div>
                        加载中...
                    </td>
                </tr>
            `;
        }

        fetch('/api/stats/top-categories')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP错误 ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                this.removeLoadingOverlay(canvas);

                if (data.success) {
                    this.state.topCategories = data.data;
                    this.renderCategoryChart();
                } else {
                    throw new Error(data.message || '获取数据失败');
                }
            })
            .catch(error => {
                console.error('加载热门类别失败:', error);
                this.removeLoadingOverlay(canvas);
                this.showErrorOverlay(canvas, `加载类别数据失败: ${error.message}`);

                if (detailBody) {
                    detailBody.innerHTML = `
                        <tr>
                            <td colspan="2" class="text-center text-danger">
                                <i class="bi bi-exclamation-triangle"></i>
                                加载失败: ${error.message}
                            </td>
                        </tr>
                    `;
                }
            });
    },

    renderCategoryChart: function() {
        if (!this.state.topCategories) {
            return;
        }
        const ctx = document.getElementById('categoryChart');
        if (!ctx) {
            console.error('找不到categoryChart元素');
            return;
        }

        const ctx2d = ctx.getContext('2d');
        if (!ctx2d) {
            console.error('无法获取canvas上下文');
            return;
        }
        if (this.state.categoryChart) {
            this.state.categoryChart.destroy();
        }

        const labels = this.state.topCategories.map(cat => cat.categoryName);
        const data = this.state.topCategories.map(cat => cat.borrowCount);

        const colors = [
            'rgba(255, 99, 132, 0.8)',
            'rgba(54, 162, 235, 0.8)',
            'rgba(255, 206, 86, 0.8)',
            'rgba(75, 192, 192, 0.8)',
            'rgba(153, 102, 255, 0.8)'
        ];

        const borderColors = [
            'rgb(255, 99, 132)',
            'rgb(54, 162, 235)',
            'rgb(255, 206, 86)',
            'rgb(75, 192, 192)',
            'rgb(153, 102, 255)'
        ];

        try {
            this.state.categoryChart = new Chart(ctx2d, {
                type: 'doughnut',
                data: {
                    labels: labels,
                    datasets: [{
                        data: data,
                        backgroundColor: colors.slice(0, data.length),
                        borderColor: borderColors.slice(0, data.length),
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'right',
                            labels: {
                                padding: 20,
                                font: {
                                    size: 12
                                },
                                usePointStyle: true
                            }
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    const label = context.label || '';
                                    const value = context.raw || 0;
                                    const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                    const percentage = Math.round((value / total) * 100);
                                    return `${label}: ${value}次 (${percentage}%)`;
                                }
                            }
                        }
                    },
                    cutout: '50%'
                }
            });
            this.renderCategoryDetails();
        } catch (error) {
            console.error('渲染类别图表失败:', error);
            const container = ctx.parentElement;
            container.innerHTML = `
                <div class="alert alert-warning">
                    <strong>图表渲染失败：</strong>${error.message}
                </div>
            `;
        }
    },
    renderCategoryDetails: function() {
        if (!this.state.topCategories || this.state.topCategories.length === 0) {
            return;
        }
        const detailBody = document.getElementById('categoryDetailBody');
        if (!detailBody) {
            console.error('找不到categoryDetailBody元素');
            return;
        }

        let html = '';
        this.state.topCategories.forEach((category, index) => {
            const colors = ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF'];
            const color = colors[index] || '#6c757d';
            const total = this.state.topCategories.reduce((sum, cat) => sum + cat.borrowCount, 0);
            const percentage = total > 0 ? Math.round((category.borrowCount / total) * 100) : 0;

            html += `
                <tr>
                    <td style="width: 30px;">
                        <span class="badge" style="background-color: ${color}; width: 12px; height: 12px; border-radius: 50%; display: inline-block;"></span>
                    </td>
                    <td>
                        <div class="d-flex justify-content-between align-items-center">
                            <span>${this.escapeHtml(category.categoryName)}</span>
                            <span class="badge bg-secondary">${category.borrowCount}次 (${percentage}%)</span>
                        </div>
                        <div class="progress mt-1" style="height: 4px;">
                            <div class="progress-bar" style="width: ${percentage}%; background-color: ${color};"></div>
                        </div>
                    </td>
                </tr>
            `;
        });

        detailBody.innerHTML = html;
    },
    bindEvents: function() {
        const refreshBtn = document.getElementById('refreshTrendBtn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => {
                this.loadData();
                const originalText = refreshBtn.innerHTML;
                refreshBtn.innerHTML = '<i class="bi bi-arrow-clockwise"></i> 刷新中...';
                refreshBtn.disabled = true;
                setTimeout(() => {
                    refreshBtn.innerHTML = originalText;
                    refreshBtn.disabled = false;
                    this.showToast('数据刷新成功！', 'success');
                }, 1000);
            });
        }
    },

    showToast: function(message, type = 'success') {
        let toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            toastContainer.style.zIndex = '1055';
            document.body.appendChild(toastContainer);
        }

        const toastId = 'toast-' + Date.now();
        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;

        toastContainer.insertAdjacentHTML('beforeend', toastHtml);

        const toastEl = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
        toast.show();
        toastEl.addEventListener('hidden.bs.toast', () => {
            toastEl.remove();
        });
    },

    escapeHtml: function(text) {
        if (text === null || text === undefined) {
            return '';
        }
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },
    onDestroy: function() {
        if (this.state.trendChart) {
            this.state.trendChart.destroy();
            this.state.trendChart = null;
        }
        if (this.state.categoryChart) {
            this.state.categoryChart.destroy();
            this.state.categoryChart = null;
        }
        const refreshBtn = document.getElementById('refreshTrendBtn');
        if (refreshBtn) {
            refreshBtn.replaceWith(refreshBtn.cloneNode(true));
        }
    }
};
(function() {
    if (typeof safeRegisterModule === 'function') {
        safeRegisterModule('stats-manage', statsManageModule);
    } else if (typeof registerModule === 'function') {
        registerModule('stats-manage', statsManageModule);
    } else {
        window.modules = window.modules || {};
        window.modules['stats-manage'] = statsManageModule;
        if (window.isModuleSystemReady === true) {
            if (typeof window.registerModule === 'function') {
                window.registerModule('stats-manage', statsManageModule);
            }
        }
    }
})();