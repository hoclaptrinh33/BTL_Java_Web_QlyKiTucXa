(() => {
    // 1. Donut Occupancy Chart
    const donutCanvas = document.getElementById("ktxOccupancyDonut");
    if (donutCanvas && window.Chart && window.ktxDashboard) {
        const occupied = Number(window.ktxDashboard.occupied) || 0;
        const vacant = Number(window.ktxDashboard.vacant) || 0;
        const maintenance = Number(window.ktxDashboard.maintenance) || 0;

        if (occupied + vacant + maintenance > 0) {
            new Chart(donutCanvas, {
                type: "doughnut",
                data: {
                    labels: ["Đang sử dụng", "Còn trống", "Bảo trì"],
                    datasets: [{
                        data: [occupied, vacant, maintenance],
                        backgroundColor: ["#6D5EF5", "#22C55E", "#F97316"],
                        borderWidth: 0
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    cutout: "70%",
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    const val = context.raw || 0;
                                    const total = occupied + vacant + maintenance;
                                    const pct = total > 0 ? (val * 100 / total).toFixed(1) : 0;
                                    return ` ${context.label}: ${val} giường (${pct}%)`;
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    // 2. Bar Debt by Month Chart with Range Filter
    const debtCanvas = document.getElementById("ktxDebtBarChart");
    if (debtCanvas && window.Chart) {
        if (window.ktxDebtData && window.ktxDebtData.labels && window.ktxDebtData.labels.length > 0) {
            initDebtChart(debtCanvas, window.ktxDebtData);
        } else {
            fetch("/admin/dashboard/api/debt-by-month")
                .then(res => {
                    if (!res.ok) throw new Error("Network response was not ok");
                    return res.json();
                })
                .then(data => {
                    if (data && data.labels && data.labels.length > 0) {
                        initDebtChart(debtCanvas, data);
                    }
                })
                .catch(err => {
                    console.log("Debt data not available or error:", err);
                });
        }
    }

    function initDebtChart(canvas, debtData) {
        const allLabels = debtData.labels || [];
        const allDataValues = (debtData.data || []).map(v => Number(v) || 0);

        let currentRange = "6"; // Mặc định hiển thị 6 tháng gần nhất cho gọn gàng

        function sliceData(range) {
            let count = allLabels.length;
            if (range === "6") count = 6;
            else if (range === "12") count = 12;

            if (range === "all" || count >= allLabels.length) {
                return { labels: allLabels.slice(), data: allDataValues.slice() };
            }
            return {
                labels: allLabels.slice(-count),
                data: allDataValues.slice(-count)
            };
        }

        const initial = sliceData(currentRange);

        const chart = new Chart(canvas, {
            type: "bar",
            data: {
                labels: initial.labels,
                datasets: [{
                    label: "Công nợ (VNĐ)",
                    data: initial.data,
                    backgroundColor: "#F43F5E",
                    borderRadius: 6,
                    maxBarThickness: 36
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                const val = context.raw || 0;
                                return " Nợ: " + new Intl.NumberFormat("vi-VN").format(val) + " đ";
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                if (value >= 1000000000) {
                                    return (value / 1000000000).toFixed(1) + " tỷ";
                                }
                                if (value >= 1000000) {
                                    return (value / 1000000).toFixed(0) + " tr";
                                }
                                if (value >= 1000) {
                                    return (value / 1000).toFixed(0) + " k";
                                }
                                return value;
                            }
                        },
                        grid: {
                            color: "#F1F5F9"
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        }
                    }
                }
            }
        });

        // Gắn sự kiện chuyển đổi khoảng thời gian
        const rangeBtns = document.querySelectorAll("#debtRangeGroup .debt-range-btn");
        rangeBtns.forEach(btn => {
            btn.addEventListener("click", () => {
                rangeBtns.forEach(b => b.classList.remove("active"));
                btn.classList.add("active");
                const r = btn.getAttribute("data-range");
                const sliced = sliceData(r);
                chart.data.labels = sliced.labels;
                chart.data.datasets[0].data = sliced.data;
                chart.update();
            });
        });
    }

    // 3. Phân trang cho bảng công nợ theo tháng (Sắp xếp mới nhất lên đầu, 5 dòng/trang)
    initDebtTablePagination();

    function initDebtTablePagination() {
        const tbody = document.getElementById("ktxDebtTableBody");
        if (!tbody) return;

        const rows = Array.from(tbody.querySelectorAll(".ktx-debt-row"));
        if (!rows || rows.length === 0) return;

        // Đảo ngược thứ tự để tháng mới nhất (09/2026, 08/2026...) hiển thị lên đầu
        rows.reverse().forEach(row => tbody.appendChild(row));

        const pageSize = 5;
        const totalRows = rows.length;
        const totalPages = Math.ceil(totalRows / pageSize);

        let currentPage = 1;

        const pageSpan = document.getElementById("debtCurrentPage");
        const totalPagesSpan = document.getElementById("debtTotalPages");
        const totalCountSpan = document.getElementById("debtTotalCount");
        const prevBtn = document.getElementById("debtPrevBtn");
        const nextBtn = document.getElementById("debtNextBtn");

        if (totalPagesSpan) totalPagesSpan.textContent = totalPages;
        if (totalCountSpan) totalCountSpan.textContent = totalRows;

        function showPage(p) {
            currentPage = p;
            const start = (p - 1) * pageSize;
            const end = start + pageSize;

            rows.forEach((row, idx) => {
                row.style.display = (idx >= start && idx < end) ? "" : "none";
            });

            if (pageSpan) pageSpan.textContent = currentPage;
            if (prevBtn) prevBtn.disabled = (currentPage <= 1);
            if (nextBtn) nextBtn.disabled = (currentPage >= totalPages);
        }

        if (prevBtn) {
            prevBtn.addEventListener("click", (e) => {
                e.preventDefault();
                if (currentPage > 1) showPage(currentPage - 1);
            });
        }

        if (nextBtn) {
            nextBtn.addEventListener("click", (e) => {
                e.preventDefault();
                if (currentPage < totalPages) showPage(currentPage + 1);
            });
        }

        // Khởi tạo trang 1
        showPage(1);
    }
})();
