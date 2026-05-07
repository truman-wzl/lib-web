// 分类管理模块
(function() {
    const MODULE_ID = 'category-manage';

    // 注册模块
    window.registerModule(MODULE_ID, {
        render: render,
        onDestroy: cleanup
    });

    // 模块状态
    let state = {
        currentPage: 1,
        pageSize: 10,
        searchKeyword: ''
    };

    /**
     * 渲染模块
     */
    function render() {
        const content = `
            <div class="category-manage-module">
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h4 class="mb-0">📂 分类管理</h4>
                        <div>
                            <button class="btn btn-success" id="exportCategoryBtn">
                                <i class="bi bi-file-excel"></i> 导出Excel
                            </button>
                            <button class="btn btn-sm btn-primary me-2" id="importCategoryBtn">
                                <i class="bi bi-upload"></i> 批量导入
                            </button>
                            <button class="btn btn-sm btn-success" id="addCategoryBtn">
                                <i class="bi bi-plus-circle"></i> 添加分类
                            </button>
                        </div>
                    </div>
                    <div class="card-body">
                        <!-- 搜索区域 -->
                        <div class="row g-3 mb-4">
                            <div class="col-md-8">
                                <input type="text" class="form-control" id="searchKeyword"
                                       placeholder="输入分类名称搜索" value="${state.searchKeyword}">
                            </div>
                            <div class="col-md-2">
                                <button class="btn btn-primary w-100" id="searchBtn">搜索</button>
                            </div>
                            <div class="col-md-2">
                                <button class="btn btn-outline-secondary w-100" id="resetBtn">重置</button>
                            </div>
                        </div>

                        <!-- 分类表格 -->
                        <div class="table-responsive">
                            <table class="table table-hover">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>分类名称</th>
                                        <th>保护状态</th>
                                        <th>创建时间</th>
                                        <th>操作</th>
                                    </tr>
                                </thead>
                                <tbody id="categoryTableBody">
                                    <!-- 分类数据将通过JS动态加载 -->
                                    <tr>
                                        <td colspan="4" class="text-center py-4">
                                            <div class="spinner-border spinner-border-sm" role="status"></div>
                                            <span class="ms-2">加载中...</span>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        <!-- 分页 -->
                        <nav>
                            <ul class="pagination justify-content-center" id="pagination">
                                <!-- 分页将通过JS动态生成 -->
                            </ul>
                        </nav>
                    </div>
                </div>

                <!-- 添加/编辑分类模态框 -->
                <div class="modal fade" id="categoryModal" tabindex="-1">
                    <div class="modal-dialog modal-dialog-centered">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="modalTitle">添加分类</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <form id="categoryForm">
                                    <input type="hidden" id="categoryId">
                                    <div class="mb-3">
                                        <label class="form-label">分类名称 *</label>
                                        <input type="text" class="form-control" id="categoryName"
                                               placeholder="例如：计算机科学、文学小说、经济管理" required>
                                        <div class="form-text">分类名称需简洁明确，便于图书归类</div>
                                    </div>
                                </form>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                                <button type="button" class="btn btn-primary" id="saveCategoryBtn">保存</button>
                            </div>
                        </div>
                    </div>
                </div>
                <!-- 分类批量导入模态框 -->
                <div class="modal fade" id="importCategoryModal" tabindex="-1">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">批量导入分类</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <div class="mb-3">
                                    <label for="categoryFile" class="form-label">选择Excel文件</label>
                                    <input class="form-control" type="file" id="categoryFile" accept=".xlsx, .xls, .csv">
                                    <div class="form-text">
                                        请确保Excel文件格式正确。第一行应为表头：category_name, is_protected
                                    </div>
                                </div>

                                <div id="importCategoryProgress" style="display: none;">
                                    <div class="progress mb-3">
                                        <div class="progress-bar progress-bar-striped" role="progressbar"
                                             id="categoryProgressBar" style="width: 0%">0%</div>
                                    </div>
                                    <p id="importCategoryMessage" class="text-center small"></p>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                                <button type="button" class="btn btn-primary" id="uploadCategoryBtn">开始导入</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.getElementById('moduleContent').innerHTML = content;

        // 初始化模块
        initModule();
    }

    /**
     * 初始化模块
     */
    async function initModule() {
        // 加载分类数据
        loadCategories();

        // 绑定事件
        bindEvents();
    }

    /**
     * 加载分类数据
     */
    async function loadCategories() {
        const tbody = document.getElementById('categoryTableBody');
        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="text-center py-4">
                    <div class="spinner-border spinner-border-sm" role="status"></div>
                    <span class="ms-2">加载中...</span>
                </td>
            </tr>
        `;

        try {
            // 构建搜索参数
            let url = `/api/categories`;
            if (state.searchKeyword) {
                url += `?keyword=${encodeURIComponent(state.searchKeyword)}`;
            }

            const response = await fetch(url);
            const result = await response.json();

            if (result.success) {
                renderCategories(result.data);
            } else {
                throw new Error(result.message);
            }
        } catch (error) {
            console.error('加载分类失败:', error);
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center py-4 text-danger">
                        加载失败: ${error.message}
                    </td>
                </tr>
            `;
        }
    }

    /**
     * 渲染分类表格
     */
    function renderCategories(categories) {
        const tbody = document.getElementById('categoryTableBody');

        if (!categories || categories.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center py-4 text-muted">
                        ${state.searchKeyword ? '未找到匹配的分类' : '暂无分类数据，请点击"添加分类"按钮创建'}
                    </td>
                </tr>
            `;
            return;
        }

        let html = '';
        categories.forEach(category => {
            const createTime = category.createTime ?
                new Date(category.createTime).toLocaleString() : '-';

            // 判断是否为受保护分类
            const isProtected = category.isProtected === true || category.categoryId === 5;
            const protectedBadge = isProtected ?
                '<span class="badge bg-warning"><i class="bi bi-shield-lock me-1"></i>受保护</span>' :
                '<span class="badge bg-secondary"><i class="bi bi-unlock me-1"></i>普通</span>';

            // 根据是否受保护决定操作按钮
            let actionButtons = '';
            if (isProtected) {
                actionButtons = '<span class="text-muted small">受保护分类</span>';
            } else {
                actionButtons = `
                    <button class="btn btn-sm btn-outline-primary me-1 edit-btn"
                            data-id="${category.categoryId}" data-name="${category.categoryName}">
                        编辑
                    </button>
                    <button class="btn btn-sm btn-outline-danger delete-btn"
                            data-id="${category.categoryId}">
                        删除
                    </button>
                `;
            }

            html += `
                <tr>
                    <td>${category.categoryId}</td>
                    <td><strong>${category.categoryName}</strong></td>
                    <td>${protectedBadge}</td>
                    <td>${createTime}</td>
                    <td>${actionButtons}</td>
                </tr>
            `;
        });

        tbody.innerHTML = html;

        // 绑定行内按钮事件
        bindRowEvents();
    }
    /**
     * 绑定事件
     */
    function bindEvents() {
        // 搜索按钮
        document.getElementById('searchBtn').addEventListener('click', () => {
            state.searchKeyword = document.getElementById('searchKeyword').value;
            loadCategories();
        });

        // 重置按钮
        document.getElementById('resetBtn').addEventListener('click', () => {
            state.searchKeyword = '';
            document.getElementById('searchKeyword').value = '';
            loadCategories();
        });
        // 🔴 在这里添加导出按钮绑定
        const exportBtn = document.getElementById('exportCategoryBtn');
        if (exportBtn) {
            exportBtn.addEventListener('click', exportCategoriesToExcel);
        }
        // 添加分类按钮
        document.getElementById('addCategoryBtn').addEventListener('click', () => {
            showCategoryModal();
        });

        // 保存分类按钮
        document.getElementById('saveCategoryBtn').addEventListener('click', async () => {
            await saveCategory();
        });

        // 表单提交
        document.getElementById('categoryForm').addEventListener('submit', (e) => {
            e.preventDefault();
            saveCategory();
        });
        // 批量导入按钮事件
        document.getElementById('importCategoryBtn').addEventListener('click', () => {
            showImportCategoryModal();
        });

        // 上传文件事件
        document.getElementById('uploadCategoryBtn').addEventListener('click', () => {
            uploadCategoryFile();
        });
        // 搜索框回车事件
        document.getElementById('searchKeyword').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                state.searchKeyword = document.getElementById('searchKeyword').value;
                loadCategories();
            }
        });
    }

    /**
     * 绑定行内按钮事件
     */
    function bindRowEvents() {
        // 编辑按钮
        document.querySelectorAll('.edit-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const categoryId = this.getAttribute('data-id');
                const categoryName = this.getAttribute('data-name');
                showCategoryModal({ categoryId, categoryName });
            });
        });

        // 删除按钮
        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const categoryId = this.getAttribute('data-id');
                deleteCategory(categoryId);
            });
        });
    }

    /**
     * 显示添加/编辑分类模态框
     */
    function showCategoryModal(category = null) {
        const modal = new bootstrap.Modal(document.getElementById('categoryModal'));
        const form = document.getElementById('categoryForm');

        document.getElementById('modalTitle').textContent = category ? '编辑分类' : '添加分类';

        // 清空表单
        form.reset();

        if (category) {
            document.getElementById('categoryId').value = category.categoryId;
            document.getElementById('categoryName').value = category.categoryName;
        } else {
            document.getElementById('categoryId').value = '';
        }

        modal.show();
    }

    /**
     * 保存分类
     */
    async function saveCategory() {
        const saveBtn = document.getElementById('saveCategoryBtn');
        const categoryId = document.getElementById('categoryId').value;
        const categoryName = document.getElementById('categoryName').value.trim();

        if (!categoryName) {
            Toast.error('请输入分类名称');
            return;
        }

        // 保存原始按钮文本
        const originalText = saveBtn.innerHTML;

        // 显示加载状态
        saveBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> 处理中...';
        saveBtn.disabled = true;

        const url = categoryId ? `/api/categories/${categoryId}` : '/api/categories';
        const method = categoryId ? 'PUT' : 'POST';

        try {
            const response = await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ categoryName: categoryName })
            });

            const result = await response.json();

            if (result.success) {
                // 显示成功提示
                const action = categoryId ? '修改' : '添加';
                Toast.success(`分类${action}成功！`);

                // 关闭模态框
                const modalElement = document.getElementById('categoryModal');
                if (modalElement) {
                    const modal = bootstrap.Modal.getInstance(modalElement);
                    if (modal) {
                        // 延迟关闭，让用户看到提示
                        setTimeout(() => modal.hide(), 500);
                    }
                }

                // 重新加载列表
                setTimeout(() => loadCategories(), 300);
            } else {
                Toast.error(result.message || '操作失败');
            }
        } catch (error) {
            console.error('保存分类失败:', error);
            Toast.error('网络错误，请稍后重试');
        } finally {
            // 恢复按钮状态
            saveBtn.innerHTML = originalText;
            saveBtn.disabled = false;
        }
    }
    /**
     * 删除分类
     */
    async function deleteCategory(categoryId) {
        if (!confirm('确定要删除这个分类吗？如果该分类下有图书，删除操作将失败。')) {
            return;
        }

        try {
            const response = await fetch(`/api/categories/${categoryId}`, {
                method: 'DELETE'
            });

            const result = await response.json();

            if (result.success) {
                Toast.success('分类删除成功！');
                loadCategories();
            } else {
                Toast.error(result.message || '删除失败');
            }
        } catch (error) {
            console.error('删除分类失败:', error);
            Toast.error('删除失败: ' + error.message);
        }
    }
    /**
     * 显示批量导入分类模态框
     */
    function showImportCategoryModal() {
        const modal = new bootstrap.Modal(document.getElementById('importCategoryModal'));

        // 重置表单
        document.getElementById('categoryFile').value = '';
        document.getElementById('importCategoryProgress').style.display = 'none';

        modal.show();
    }

    /**
     * 下载分类模板
     */
    function downloadCategoryTemplate() {
        // 创建模板数据
        const templateData = [
            ['category_name', 'is_protected'],
            ['计算机科学', 'false'],
            ['文学', 'false'],
            ['历史', 'false']
        ];

        // 这里可以调用后端接口下载模板
        // 或者生成一个CSV文件供下载
        const csvContent = templateData.map(row => row.join(',')).join('\n');
        const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = '分类导入模板.csv';
        link.click();

        Toast.success('模板下载开始');
    }

    /**
     * 上传分类文件
     */
    async function uploadCategoryFile() {
        const fileInput = document.getElementById('categoryFile');
        const file = fileInput.files[0];

        if (!file) {
            Toast.error('请选择要上传的文件');
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        const uploadBtn = document.getElementById('uploadCategoryBtn');
        const originalText = uploadBtn.innerHTML;

        uploadBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> 上传中...';
        uploadBtn.disabled = true;

        try {
            const response = await fetch('/api/categories/import', {
                method: 'POST',
                body: formData
            });

            const result = await response.json();

            if (result.success) {
                // 修改这里：result.data.success -> 成功条数，result.data.failed -> 失败条数
                Toast.success(`导入成功！成功${result.data.success}条，失败${result.data.failed}条`);

                // 关闭模态框
                const modal = bootstrap.Modal.getInstance(document.getElementById('importCategoryModal'));
                modal.hide();

                // 刷新列表
                loadCategories();
            } else {
                Toast.error(result.message || '导入失败');
            }
        } catch (error) {
            console.error('导入失败:', error);
            Toast.error('导入失败: ' + error.message);
        } finally {
            uploadBtn.innerHTML = originalText;
            uploadBtn.disabled = false;
        }
    }

    // 🔴 在这里添加导出函数（在cleanup函数之前）
    async function exportCategoriesToExcel() {
        console.log('📤 点击了分类导出按钮');

        if (!window.ExportManager) {
            alert('导出功能未初始化，请刷新页面重试');
            return;
        }

        // 获取当前搜索条件
        const keyword = document.getElementById('searchKeyword')?.value || '';
        console.log('🔍 搜索参数:', { keyword });

        // 构建参数
        const params = new URLSearchParams();
        if (keyword && keyword.trim() !== '') {
            params.append('keyword', keyword.trim());
        }

        // 构建完整的URL
        let url = '/api/export/category';
        if (params.toString()) {
            url += '?' + params.toString();
        }

        console.log('🌐 分类导出URL:', url);

        // 生成文件名
        const today = new Date();
        const dateStr = `${today.getFullYear()}${(today.getMonth() + 1).toString().padStart(2, '0')}${today.getDate().toString().padStart(2, '0')}`;
        const filename = `分类列表_${dateStr}.xlsx`;

        // 调用通用导出管理器
        window.ExportManager.exportToExcel(url, '分类数据', filename);
    }

    /**
     * 清理模块资源
     */
    function cleanup() {
        console.log('清理分类管理模块资源');
    }
    /**
     * 显示成功消息（使用Bootstrap Toast）
     */
    function showSuccessToast(message) {
        // 移除可能存在的旧toast
        const oldToast = document.getElementById('successToast');
        if (oldToast) {
            oldToast.remove();
        }

        // 创建toast HTML
        const toastHtml = `
            <div id="successToast" class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 9999">
                <div class="toast align-items-center text-bg-success border-0" role="alert" aria-live="assertive" aria-atomic="true">
                    <div class="d-flex">
                        <div class="toast-body">
                            <i class="bi bi-check-circle me-2"></i>${message}
                        </div>
                        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                    </div>
                </div>
            </div>
        `;

        // 添加到页面
        document.body.insertAdjacentHTML('beforeend', toastHtml);

        // 显示toast
        const toastEl = document.getElementById('successToast');
        const toast = new bootstrap.Toast(toastEl, {
            autohide: true,
            delay: 3000
        });
        toast.show();

        // 3秒后自动移除
        setTimeout(() => {
            if (toastEl && toastEl.parentNode) {
                toastEl.remove();
            }
        }, 3500);
    }
    /**
     * 显示操作通知
     */
    function showSuccessNotification(message, type = 'success') {
        // 直接使用你已有的 showSuccessToast 函数
        showSuccessToast(message);
    }

    /**
     * 显示按钮加载状态
     */
    function showButtonState(button, isLoading = true) {
        if (!button) return;

        if (isLoading) {
            button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> 处理中...';
            button.disabled = true;
        } else {
            button.innerHTML = '保存';
            button.disabled = false;
        }
    }

    /**
     * 高亮表格行
     */
    function highlightTableRow(rowId, action = 'add') {
        // 暂时不实现，先保证基本功能
        console.log('高亮行:', rowId, action);
    }
})();