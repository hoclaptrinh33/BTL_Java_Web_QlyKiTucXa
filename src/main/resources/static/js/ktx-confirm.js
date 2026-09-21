document.addEventListener('DOMContentLoaded', function() {
    // Intercept form submissions that have data-confirm (§24.5, 24.12)
    document.addEventListener('submit', function(e) {
        const form = e.target;
        if (form.hasAttribute('data-confirm') && !form.dataset.confirmed) {
            e.preventDefault();
            const message = form.getAttribute('data-confirm');
            const title = form.getAttribute('data-confirm-title') || 'Xác nhận thao tác';
            const type = form.getAttribute('data-confirm-type') || 'warning';
            const confirmText = form.getAttribute('data-confirm-button') || 'Đồng ý';
            const cancelText = form.getAttribute('data-cancel-button') || 'Hủy bỏ';
            const modalEl = document.getElementById('ktxConfirmModal');
            if (modalEl && typeof bootstrap !== 'undefined') {
                const titleEl = document.getElementById('ktxConfirmModalTitle');
                const bodyEl = document.getElementById('ktxConfirmModalBody');
                const okBtn = document.getElementById('ktxConfirmModalOk');
                const cancelBtn = document.getElementById('ktxConfirmModalCancel');
                if (titleEl) titleEl.textContent = title;
                if (bodyEl) bodyEl.textContent = message;
                if (okBtn) {
                    okBtn.textContent = confirmText;
                    let btnClass = 'btn shadow-sm ';
                    if (type === 'danger') btnClass += 'btn-danger';
                    else if (type === 'success') btnClass += 'btn-success';
                    else btnClass += 'btn-primary';
                    okBtn.className = btnClass;
                }
                if (cancelBtn) cancelBtn.textContent = cancelText;
                const bsModal = bootstrap.Modal.getOrCreateInstance(modalEl);
                function onConfirm() {
                    okBtn.removeEventListener('click', onConfirm);
                    form.dataset.confirmed = 'true';
                    bsModal.hide();
                    const submitBtn = form.querySelector('button[type="submit"]');
                    if (submitBtn) submitBtn.disabled = true;
                    form.submit();
                }
                okBtn.addEventListener('click', onConfirm);
                modalEl.addEventListener('hidden.bs.modal', function onHidden() {
                    okBtn.removeEventListener('click', onConfirm);
                    modalEl.removeEventListener('hidden.bs.modal', onHidden);
                });
                bsModal.show();
            } else if (window.confirm(message)) {
                form.dataset.confirmed = 'true';
                form.submit();
            }
        }
    });
});
