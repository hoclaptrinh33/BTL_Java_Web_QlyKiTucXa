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
                    responsive: false,
                    cutout: "72%",
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

    // 2. Bar Debt by Month Chart
    const debtCanvas = document.getElementById("ktxDebtBarChart");
    if (debtCanvas && window.Chart) {
        if (window.ktxDebtData && window.ktxDebtData.labels && window.ktxDebtData.labels.length > 0) {
            renderDebtChart(debtCanvas, window.ktxDebtData);
        } else {
            fetch("/admin/dashboard/api/debt-by-month")
                .then(res => {
                    if (!res.ok) throw new Error("Network response was not ok");
                    return res.json();
                })
                .then(data => {
                    if (data && data.labels && data.labels.length > 0) {
                        renderDebtChart(debtCanvas, data);
                    }
                })
                .catch(err => {
                    console.log("Debt data not available or error:", err);
                });
        }
    }

    function renderDebtChart(canvas, debtData) {
        const labels = debtData.labels || [];
        const dataValues = (debtData.data || []).map(v => Number(v) || 0);

        new Chart(canvas, {
            type: "bar",
            data: {
                labels: labels,
                datasets: [{
                    label: "Công nợ (VNĐ)",
                    data: dataValues,
                    backgroundColor: "#F43F5E",
                    borderRadius: 6,
                    maxBarThickness: 40
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
                                if (value >= 1000000) {
                                    return (value / 1000000).toFixed(1) + " tr";
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
    }
})();
