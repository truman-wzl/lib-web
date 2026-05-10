(function() {
    const MODULE_ID = 'book-manage';

    window.registerModule(MODULE_ID, {
        render: render,
        onDestroy: cleanup
    });

    let state = {
        currentPage: 1,
        pageSize: 10,
        searchParams: {}
    };

    function render() {
        const content = `
            <div class="book-manage-module">
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h4 class="mb-0">图书管理</h4>
                        <div>
                            <button class="btn btn-success" id="exportBookBtn">
                                <i class="bi bi-file-excel"></i> 导出Excel
                            </button>
                            <button class="btn btn-sm btn-primary me-2" id="importBookBtn">
                                <i class="bi bi-upload"></i> 批量导入
                            </button>
                            <button class="btn btn-sm btn-success" id="addBookBtn">
                                <span>+ 添加图书</span>
                            </button>
                        </div>
                    </div>
                    <div class="card-body">
                        <!-- 搜索区域 -->
                        <div class="row g-3 mb-4">
                            <div class="col-md-4">
                                <input type="text" class="form-control" id="searchBookname" placeholder="书名">
                            </div>
                            <div class="col-md-3">
                                <input type="text" class="form-control" id="searchAuthor" placeholder="作者">
                            </div>
                            <div class="col-md-3">
                                <select class="form-select" id="searchCategory">
                                    <option value="">全部分类</option>
                                </select>
                            </div>
                            <div class="col-md-2">
                                <button class="btn btn-primary w-100" id="searchBtn">搜索</button>
                            </div>
                        </div>

                        <!-- 图书表格 -->
                        <div class="table-responsive">
                            <table class="table table-hover">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>书名</th>
                                        <th>作者</th>
                                        <th>出版社</th>
                                        <th>分类</th>
                                        <th>总数/可借</th>
                                        <th>操作</th>
                                    </tr>
                                </thead>
                                <tbody id="bookTableBody">
                                    <tr>
                                        <td colspan="6" class="text-center py-4">
                                            <div class="spinner-border spinner-border-sm" role="status"></div>
                                            <span class="ms-2">加载中...</span>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                        <nav>
                            <ul class="pagination justify-content-center" id="pagination">
                            </ul>
                        </nav>
                    </div>
                </div>
                <div class="modal fade" id="bookModal" tabindex="-1">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="modalTitle">添加图书</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <form id="bookForm">
                                    <input type="hidden" id="bookId">
                                    <div class="mb-3">
                                        <label class="form-label">书名 *</label>
                                        <input type="text" class="form-control" id="bookname" required>
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label">作者</label>
                                        <input type="text" class="form-control" id="author">
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label">出版社</label>
                                        <input type="text" class="form-control" id="publisher">
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label">分类</label>
                                        <select class="form-select" id="categoryId" required>
                                            <option value="">请选择分类</option>
                                        </select>
                                    </div>
                                    <div class="row">
                                        <div class="col-6">
                                            <div class="mb-3">
                                                <label class="form-label">总数量 *</label>
                                                <input type="number" class="form-control" id="totalNumber" min="0" value="1" required>
                                            </div>
                                        </div>
                                        <div class="col-6">
                                            <div class="mb-3">
                                                <label class="form-label">可借数量 *</label>
                                                <input type="number" class="form-control" id="canBorrow" min="0" value="1" required>
                                            </div>
                                        </div>
                                    </div>
                                </form>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                                <button type="button" class="btn btn-primary" id="saveBookBtn">保存</button>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal fade" id="importBookModal" tabindex="-1">
                    <div class="modal-dialog modal-lg">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">批量导入图书</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <div class="mb-3">
                                    <label for="bookFile" class="form-label">选择Excel文件</label>
                                    <input class="form-control" type="file" id="bookFile" accept=".xlsx, .xls, .csv">
                                    <div class="form-text">
                                        请确保Excel文件格式正确。列顺序：bookname, author, publisher, category_name, total_number, can_borrow
                                    </div>
                                </div>
                                <div id="importResult" style="display: none;">
                                    <h6>导入结果</h6>
                                    <div id="resultContent" class="alert"></div>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                                <button type="button" class="btn btn-primary" id="uploadBookBtn">开始导入</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.getElementById('moduleContent').innerHTML = content;
        initModule();
    }
    async function initModule() {
        await loadCategories();
        loadBooks();
        bindEvents();
    }
    async function loadCategories() {
        const categorySelect = document.getElementById('categoryId');
        const searchCategorySelect = document.getElementById('searchCategory');
        if (!categorySelect) {
            console.error('找不到分类下拉框元素: #categoryId');
            return;
        }

        try {
            // 强制不缓存
            const timestamp = Date.now();
            const response = await fetch(`/api/categories?_=${timestamp}`);
            const result = await response.json();
            if (result.success && result.data) {
                categorySelect.innerHTML = '<option value="">请选择分类</option>';
                if (searchCategorySelect) {
                    searchCategorySelect.innerHTML = '<option value="">全部分类</option>';
                }
                let loadedCount = 0;
                result.data.forEach(category => {
                    if (category.categoryId !== 5) {
                        const optionHTML = `<option value="${category.categoryId}">${category.categoryName}</option>`;
                        categorySelect.innerHTML += optionHTML;

                        if (searchCategorySelect) {
                            searchCategorySelect.innerHTML += `<option value="${category.categoryId}">${category.categoryName}</option>`;
                        }
                        loadedCount++;
                    } else {
                        console.log(`过滤掉中转分类: ID=${category.categoryId}, 名称=${category.categoryName}`);
                    }
                });
            } else {
                console.error('分类API返回失败:', result.message);
            }
        } catch (error) {
            console.error('加载分类失败:', error);
        }
    }
    async function loadBooks() {
        const tbody = document.getElementById('bookTableBody');
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-4">
                    <div class="spinner-border spinner-border-sm" role="status"></div>
                    <span class="ms-2">加载中...</span>
                </td>
            </tr>
        `;

        try {
            const params = new URLSearchParams({
                page: state.currentPage - 1,
                size: state.pageSize
            });
            if (state.searchParams.bookname) {
                params.append('bookname', state.searchParams.bookname);
            }
            if (state.searchParams.author) {
                params.append('author', state.searchParams.author);
            }
            if (state.searchParams.categoryId) {
                params.append('categoryId', state.searchParams.categoryId);
            }
            const response = await fetch(`/api/books/search?${params}`);
            const result = await response.json();

            if (result.success) {
                renderBooks(result.data);
                renderPagination(result.totalPages, result.currentPage);
            } else {
                throw new Error(result.message);
            }
        } catch (error) {
            console.error('加载图书失败:', error);
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-4 text-danger">
                        加载失败: ${error.message}
                    </td>
                </tr>
            `;
            console.error('详细错误信息:', {
                searchParams: state.searchParams,
                error: error
            });
        }
    }
    function renderBooks(books) {
        const tbody = document.getElementById('bookTableBody');

        if (books.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-4 text-muted">  <!-- colspan改为7 -->
                        暂无图书数据
                    </td>
                </tr>
            `;
            return;
        }

        let html = '';
        books.forEach(book => {
            const publisherDisplay = book.publisher ? book.publisher : '暂无出版社信息';

            html += `
                <tr>
                    <td>${book.bookId}</td>
                    <td>${book.bookname}</td>
                    <td>${book.author || '-'}</td>
                    <td>${publisherDisplay}</td>
                    <td>${book.category ? book.category.categoryName : '-'}</td>
                    <td>
                        <span class="badge bg-secondary">${book.totalNumber}</span> /
                        <span class="badge ${book.canBorrow > 0 ? 'bg-success' : 'bg-danger'}">${book.canBorrow}</span>
                    </td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary me-1 edit-btn" data-id="${book.bookId}">编辑</button>
                        <button class="btn btn-sm btn-outline-danger delete-btn" data-id="${book.bookId}">删除</button>
                    </td>
                </tr>
            `;
        });
        tbody.innerHTML = html;
        bindRowEvents();
    }
    function renderPagination(totalPages, currentPage) {
        const pagination = document.getElementById('pagination');

        if (totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }

        let html = '';
        html += `
            <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${currentPage}">上一页</a>
            </li>
        `;

        for (let i = 0; i < totalPages; i++) {
            html += `
                <li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" data-page="${i + 1}">${i + 1}</a>
                </li>
            `;
        }

        html += `
            <li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${currentPage + 2}">下一页</a>
            </li>
        `;

        pagination.innerHTML = html;
        if (!pagination._hasClickHandler) {
            pagination.addEventListener('click', function(e) {
                const link = e.target.closest('.page-link');
                if (link) {
                    e.preventDefault();
                    e.stopPropagation();
                    const page = parseInt(link.getAttribute('data-page'));
                    if (!isNaN(page)) {
                        state.currentPage = page;
                        loadBooks();
                    }
                }
            });
            pagination._hasClickHandler = true;
        }
    }
    function bindEvents() {
        document.getElementById('searchBtn').addEventListener('click', () => {
            state.searchParams = {
                bookname: document.getElementById('searchBookname').value,
                author: document.getElementById('searchAuthor').value,
                categoryId: document.getElementById('searchCategory').value || null
            };
            state.currentPage = 1;
            loadBooks();
        });

        document.getElementById('addBookBtn').addEventListener('click', () => {
            showBookModal();
        });
        const exportBtn = document.getElementById('exportBookBtn');
        if (exportBtn) {
            exportBtn.addEventListener('click', exportBooksToExcel);
        } else {
            console.error('未找到导出按钮: #exportBookBtn');
        }

        document.getElementById('saveBookBtn').addEventListener('click', saveBook);

        document.getElementById('bookForm').addEventListener('submit', (e) => {
            e.preventDefault();
            saveBook();
        });
        const importBtn = document.getElementById('importBookBtn');
        if (importBtn) {
            importBtn.addEventListener('click', showImportBookModal);
        }
        const bookFileInput = document.getElementById('bookFile');
        if (bookFileInput) {
            bookFileInput.addEventListener('change', function() {
                const uploadBtn = document.getElementById('uploadBookBtn');
                if (uploadBtn) {
                    uploadBtn.disabled = !this.files.length;
                }
            });
        }
        const uploadBtn = document.getElementById('uploadBookBtn');
        if (uploadBtn) {
            uploadBtn.addEventListener('click', uploadBookFile);
        }
    }
    async function exportBooksToExcel() {
        if (!window.ExportManager) {
            alert('导出功能未初始化，请刷新页面重试');
            return;
        }
        const bookname = document.getElementById('searchBookname')?.value || '';
        const author = document.getElementById('searchAuthor')?.value || '';
        const categoryId = document.getElementById('searchCategory')?.value || '';
        const params = new URLSearchParams();

        if (bookname && bookname.trim() !== '') {
            params.append('bookname', bookname.trim());
        }

        if (author && author.trim() !== '') {
            params.append('author', author.trim());
        }

        if (categoryId && categoryId !== '') {
            params.append('categoryId', categoryId);
        }
        let url = '/api/export/book';
        if (params.toString()) {
            url += '?' + params.toString();
        }
        const today = new Date();
        const dateStr = `${today.getFullYear()}${(today.getMonth() + 1).toString().padStart(2, '0')}${today.getDate().toString().padStart(2, '0')}`;
        const filename = `图书列表_${dateStr}.xlsx`;
        window.ExportManager.exportToExcel(url, '图书数据', filename);
    }
    function bindRowEvents() {
        document.querySelectorAll('.edit-btn').forEach(btn => {
            btn.addEventListener('click', async function() {
                const bookId = this.getAttribute('data-id');
                await loadBookForEdit(bookId);
            });
        });
        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const bookId = this.getAttribute('data-id');
                deleteBook(bookId);
            });
        });
    }
    function showBookModal(book = null) {
        const modal = new bootstrap.Modal(document.getElementById('bookModal'));
        const form = document.getElementById('bookForm');

        document.getElementById('modalTitle').textContent = book ? '编辑图书' : '添加图书';
        form.reset();

        const existingNote = document.getElementById('transferNote');
        if (existingNote) {
            existingNote.remove();
        }

        if (book) {
            document.getElementById('bookId').value = book.bookId;
            document.getElementById('bookname').value = book.bookname;
            document.getElementById('author').value = book.author || '';
            document.getElementById('publisher').value = book.publisher || '';
            document.getElementById('totalNumber').value = book.totalNumber;
            document.getElementById('canBorrow').value = book.canBorrow;

            if (book.category && book.category.categoryId === 5) {
                const categoryGroup = document.querySelector('label[for="categoryId"]').parentNode;
                const note = document.createElement('div');
                note.id = 'transferNote';
                note.className = 'alert alert-warning mt-2 p-2 small';
                note.innerHTML = '<i class="bi bi-exclamation-triangle me-1"></i> 此图书当前属于中转分类，请为其选择一个新的分类';
                categoryGroup.appendChild(note);
            } else if (book.category) {
                document.getElementById('categoryId').value = book.category.categoryId;
            }
        }
        loadCategories();
        const modalElement = document.getElementById('bookModal');
        modalElement.addEventListener('shown.bs.modal', function() {
            const categorySelect = document.getElementById('categoryId');
            if (categorySelect) {
                for (let i = 0; i < categorySelect.options.length; i++) {
                    if (categorySelect.options[i].value === '5') {
                        categorySelect.remove(i);
                        break;
                    }
                }
            }
        });

        modal.show();
    }
    async function loadBookForEdit(bookId) {
        try {
            const response = await fetch(`/api/books/${bookId}`);
            const result = await response.json();

            if (result.success) {
                showBookModal(result.data);
            } else {
                throw new Error(result.message);
            }
        } catch (error) {
            console.error('加载图书信息失败:', error);
            alert('加载图书信息失败: ' + error.message);
        }
    }
    async function saveBook() {
        const saveBtn = document.getElementById('saveBookBtn');
        const originalText = saveBtn.innerHTML;
        saveBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> 处理中...';
        saveBtn.disabled = true;
        const selectedCategoryId = parseInt(document.getElementById('categoryId').value);
        if (selectedCategoryId === 5) {
            alert('不能选择中转分类（ID=5），请选择其他分类');
            saveBtn.innerHTML = originalText;
            saveBtn.disabled = false;
            return;
        }

        const bookData = {
            bookname: document.getElementById('bookname').value,
            author: document.getElementById('author').value,
            publisher: document.getElementById('publisher').value,
            category: { categoryId: selectedCategoryId },
            totalNumber: parseInt(document.getElementById('totalNumber').value),
            canBorrow: parseInt(document.getElementById('canBorrow').value)
        };

        const bookId = document.getElementById('bookId').value;
        if (bookId) {
            bookData.bookId = parseInt(bookId);
        }

        try {
            const response = await fetch('/api/books', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(bookData)
            });

            const result = await response.json();

            if (result.success) {
                const modalElement = document.getElementById('bookModal');
                if (modalElement) {
                    const modal = bootstrap.Modal.getInstance(modalElement);
                    if (modal) modal.hide();
                }
                loadBooks();
                if (typeof Toast !== 'undefined') {
                    Toast.success('图书保存成功！');
                } else {
                    alert('保存成功！');
                }
            } else {
                throw new Error(result.message);
            }
        } catch (error) {
            console.error('保存图书失败:', error);
            if (typeof Toast !== 'undefined') {
                Toast.error('保存失败: ' + error.message);
            } else {
                alert('保存失败: ' + error.message);
            }
        } finally {
            saveBtn.innerHTML = originalText;
            saveBtn.disabled = false;
        }
    }
    async function deleteBook(bookId) {
        if (!confirm('确定要删除这本图书吗？此操作不可恢复。')) {
            return;
        }
        try {
            const response = await fetch(`/api/books/${bookId}`, {
                method: 'DELETE'
            });
            const result = await response.json();
            if (result.success) {
                loadBooks();
                alert('删除成功！');
            } else {
                throw new Error(result.message);
            }
        } catch (error) {
            console.error('删除图书失败:', error);
            alert('删除失败: ' + error.message);
        }
    }
    function showImportBookModal() {
        let modalElement = document.getElementById('importBookModal');
        if (!modalElement) {
            const modalHtml = `
                <div class="modal fade" id="importBookModal" tabindex="-1">
                    <div class="modal-dialog modal-lg">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title"><i class="bi bi-upload me-2"></i>批量导入图书</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <div id="categoryListContainer" class="mb-3"></div>
                                <div class="mb-3">
                                    <label for="bookFile" class="form-label">选择Excel文件</label>
                                    <input class="form-control" type="file" id="bookFile" accept=".xlsx, .xls, .csv">
                                    <div class="form-text">
                                        <p class="mb-1">支持格式：.xlsx, .xls, .csv</p>
                                        <p class="mb-0">列顺序：<code>bookname</code>, <code>author</code>, <code>publisher</code>, <code>category_name</code>, <code>total_number</code>, <code>can_borrow</code></p>
                                    </div>
                                </div>
                                <div class="mb-4">
                                    <p class="mb-2"><strong>操作：</strong></p>
                                    <div class="d-flex gap-2">
                                        <button type="button" id="downloadBookTemplate" class="btn btn-outline-primary btn-sm">
                                            <i class="bi bi-download me-1"></i>下载模板
                                        </button>
                                        <button type="button" id="downloadCategoryList" class="btn btn-outline-info btn-sm">
                                            <i class="bi bi-list-check me-1"></i>查看可用分类
                                        </button>
                                    </div>
                                </div>
                                <div id="importResult" style="display: none;">
                                    <hr>
                                    <h6 class="mb-3"><i class="bi bi-clipboard-data me-1"></i>导入结果</h6>
                                    <div id="resultContent" class="alert"></div>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                                <button type="button" class="btn btn-primary" id="uploadBookBtn" disabled>开始导入</button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            document.body.insertAdjacentHTML('beforeend', modalHtml);
            bindImportModalEvents();
        }
        document.getElementById('bookFile').value = '';
        document.getElementById('importResult').style.display = 'none';
        document.getElementById('uploadBookBtn').disabled = true;
        const categoryContainer = document.getElementById('categoryListContainer');
        if (categoryContainer) {
            categoryContainer.innerHTML = '';
        }

        const modal = new bootstrap.Modal(document.getElementById('importBookModal'));
        modal.show();
    }
    async function uploadBookFile() {
        const fileInput = document.getElementById('bookFile');
        const file = fileInput.files[0];
        const uploadBtn = document.getElementById('uploadBookBtn');

        if (!file) {
            alert('请选择要上传的Excel文件');
            return;
        }

        const fileExt = file.name.split('.').pop().toLowerCase();
        if (!['xlsx', 'xls', 'csv'].includes(fileExt)) {
            alert('请选择Excel文件（.xlsx, .xls, .csv）');
            return;
        }

        const originalText = uploadBtn.innerHTML;

        uploadBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> 上传中...';
        uploadBtn.disabled = true;

        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch('/api/books/import', {
                method: 'POST',
                body: formData
            });

            const result = await response.json();

            const resultDiv = document.getElementById('resultContent');
            const resultContainer = document.getElementById('importResult');

            if (result.success) {
                resultDiv.className = 'alert alert-success';
                resultDiv.innerHTML = `
                    <h6><i class="bi bi-check-circle-fill me-2"></i>导入完成！</h6>
                    <p><strong>导入统计：</strong></p>
                    <ul class="mb-2">
                        <li>总记录数：<strong>${result.data.total}</strong> 条</li>
                        <li>成功：<span class="text-success"><strong>${result.data.success}</strong> 条</span></li>
                        <li>失败：<span class="text-danger"><strong>${result.data.failed}</strong> 条</span></li>
                    </ul>
                `;

                if (result.data.errors && result.data.errors.length > 0) {
                    resultDiv.className = 'alert alert-warning';
                    resultDiv.innerHTML = `
                        <h6><i class="bi bi-exclamation-triangle-fill me-2"></i>导入完成，但有失败记录</h6>
                        <p><strong>导入统计：</strong></p>
                        <ul class="mb-2">
                            <li>总记录数：<strong>${result.data.total}</strong> 条</li>
                            <li>成功：<span class="text-success"><strong>${result.data.success}</strong> 条</span></li>
                            <li>失败：<span class="text-danger"><strong>${result.data.failed}</strong> 条</span></li>
                        </ul>
                        <hr>
                        <p><strong>失败记录详情：</strong></p>
                        <div style="max-height: 200px; overflow-y: auto;">
                            <table class="table table-sm table-hover mb-0">
                                <thead>
                                    <tr>
                                        <th width="80">行号</th>
                                        <th>失败原因</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${result.data.errors.map(error => `
                                        <tr>
                                            <td><span class="badge bg-danger">第${error.row}行</span></td>
                                            <td class="small">${error.reason}</td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `;
                }

                resultContainer.style.display = 'block';

                if (result.data.failed === 0) {
                    setTimeout(() => {
                        const modal = bootstrap.Modal.getInstance(document.getElementById('importBookModal'));
                        if (modal) {
                            modal.hide();
                        }
                        loadBooks();
                    }, 3000);
                }
            } else {
                resultDiv.className = 'alert alert-danger';
                resultDiv.innerHTML = `<i class="bi bi-exclamation-triangle-fill me-2"></i>${result.message || '导入失败'}`;
                resultContainer.style.display = 'block';
            }
        } catch (error) {
            console.error('上传失败:', error);
            const resultDiv = document.getElementById('resultContent');
            const resultContainer = document.getElementById('importResult');

            resultDiv.className = 'alert alert-danger';
            resultDiv.innerHTML = `<i class="bi bi-exclamation-triangle-fill me-2"></i>上传失败: ${error.message}`;
            resultContainer.style.display = 'block';
        } finally {
            // 恢复按钮状态
            uploadBtn.innerHTML = originalText;
            uploadBtn.disabled = false;
        }
    }
    function cleanup() {}
    (function ensureTransferCategoryFiltered() {
        const originalLoadCategories = window.loadCategories || function() {};
        window.loadCategories = async function() {
            const result = await originalLoadCategories();
            setTimeout(() => {
                cleanTransferCategoryFromSelects();
            }, 200);

            return result;
        };
        function cleanTransferCategoryFromSelects() {
            const selects = document.querySelectorAll('select');
            selects.forEach(select => {
                for (let i = 0; i < select.options.length; i++) {
                    const option = select.options[i];
                    if (option.text === '5中转类5' || option.value === '5') {
                        select.remove(i);
                        i--;
                    }
                }
            });
        }
        setTimeout(cleanTransferCategoryFromSelects, 1000);
    })();
})();