(() => {
    const data = window.ktxDashboard;
    const canvas = document.getElementById("ktxOccupancyDonut");
    if (!data || !canvas || !window.Chart) {
        return;
    }
    const occupied = Number(data.occupied) || 0;
    const vacant = Number(data.vacant) || 0;
    const maintenance = Number(data.maintenance) || 0;
    if (occupied + vacant + maintenance === 0) {
        return;
    }
    new Chart(canvas, {
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
            plugins: { legend: { display: false } }
        }
    });
})();
