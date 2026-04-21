const $ = (id) => document.getElementById(id);

let timer = null;
let chart = null;

const chartData = {
    labels: [],
    bw: [],
    pps: []
};

function nowClock() {
    const d = new Date();
    $("clock").textContent = d.toLocaleTimeString();
}

function buildQuery() {
    const q = new URLSearchParams();
    const ip = $("ip").value.trim();
    const protocol = $("protocol").value.trim();
    const direction = $("direction").value.trim();
    const limit = $("limit").value.trim();

    if (ip) q.set("ip", ip);
    if (protocol) q.set("protocol", protocol);
    if (direction) q.set("direction", direction);
    if (limit) q.set("limit", limit);

    return q.toString() ? `?${q.toString()}` : "";
}

function buildStatsQuery() {
    const q = new URLSearchParams();
    const ip = $("ip").value.trim();
    const protocol = $("protocol").value.trim();
    const periodSec = $("periodSec").value.trim();

    if (ip) q.set("ip", ip);
    if (protocol) q.set("protocol", protocol);
    if (periodSec) q.set("periodSec", periodSec);

    return q.toString() ? `?${q.toString()}` : "";
}

function setStatus(ok, msg) {
    $("status").textContent = ok ? `Status: OK` : `Status: ERROR`;
    $("status").style.borderColor = ok ? "#1f3a66" : "#7f1d1d";
    $("status").style.color = ok ? "#9ab0d0" : "#fecaca";
    if (msg) console.log(msg);
}

function fmt(n, digits = 2) {
    if (n === null || n === undefined) return "0";
    return Number(n).toFixed(digits);
}

function tsToTime(ts) {
    return new Date(ts).toLocaleTimeString();
}

function filterSearchRow(p, search) {
    if (!search) return true;
    const s = search.toLowerCase();
    return (
        p.srcIp.toLowerCase().includes(s) ||
        p.dstIp.toLowerCase().includes(s) ||
        (p.protocol || "").toLowerCase().includes(s)
    );
}

async function refreshAll() {
    try {
        const search = $("search").value.trim();
        const packetsRes = await fetch(`/api/packets${buildQuery()}`);
        const packets = await packetsRes.json();

        const statsRes = await fetch(`/api/stats${buildStatsQuery()}`);
        const stats = await statsRes.json();

        setStatus(true);

        // Meta
        $("meta").textContent = `Packets: ${packets.length}`;

        // KPIs
        $("kpiPackets").textContent = stats.packetCount ?? packets.length;
        $("kpiPps").textContent = `${fmt(stats.loadPps ?? 0, 2)} pps`;
        $("kpiBw").textContent = fmt(stats.bandwidthKbps ?? 0, 2);
        $("kpiLat").textContent = fmt(stats.averageLatencyMs ?? 0, 1);
        $("kpiBer").textContent = fmt(stats.BER ?? 0, 6);

        // Table
        const tbody = $("tbody");
        tbody.innerHTML = "";
        const view = packets.filter(p => filterSearchRow(p, search));

        for (const p of view) {
            const tr = document.createElement("tr");
            tr.innerHTML = `
        <td>${tsToTime(p.timestamp)}</td>
        <td>${p.srcIp}</td>
        <td>${p.dstIp}</td>
        <td class="proto ${p.protocol}">${p.protocol}</td>
        <td>${p.length}</td>
      `;
            tbody.appendChild(tr);
        }

        // Chart update
        pushChartPoint(stats);
    } catch (e) {
        setStatus(false, e);
    }
}

function initChart() {
    const ctx = $("chart").getContext("2d");
    chart = new Chart(ctx, {
        type: "line",
        data: {
            labels: chartData.labels,
            datasets: [
                {
                    label: "Bandwidth (kbps)",
                    data: chartData.bw,
                    borderColor: "#4f8cff",
                    backgroundColor: "rgba(79,140,255,.15)",
                    tension: 0.25,
                    fill: true,
                    yAxisID: "y"
                },
                {
                    label: "Load (pps)",
                    data: chartData.pps,
                    borderColor: "#22c55e",
                    backgroundColor: "rgba(34,197,94,.10)",
                    tension: 0.25,
                    fill: true,
                    yAxisID: "y1"
                }
            ]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { labels: { color: "#9ab0d0" } }
            },
            scales: {
                x: { ticks: { color: "#9ab0d0" }, grid: { color: "rgba(32,48,79,.4)" } },
                y: { position: "left", ticks: { color: "#9ab0d0" }, grid: { color: "rgba(32,48,79,.4)" } },
                y1: { position: "right", ticks: { color: "#9ab0d0" }, grid: { drawOnChartArea: false } }
            }
        }
    });
}

function pushChartPoint(stats) {
    const t = new Date().toLocaleTimeString();
    const bw = Number(stats.bandwidthKbps ?? 0);
    const pps = Number(stats.loadPps ?? 0);

    chartData.labels.push(t);
    chartData.bw.push(bw);
    chartData.pps.push(pps);

    // garde 30 points max
    if (chartData.labels.length > 30) {
        chartData.labels.shift();
        chartData.bw.shift();
        chartData.pps.shift();
    }

    $("chartInfo").textContent = `Points: ${chartData.labels.length}`;
    if (chart) chart.update();
}

function setupAutoRefresh() {
    if (timer) clearInterval(timer);
    const ms = Number($("refreshMs").value);
    if (ms > 0) {
        timer = setInterval(refreshAll, ms);
    }
}

async function clearServer() {
    await fetch("/api/clear", { method: "POST" });
    await refreshAll();
}

function exportJson() {
    // export les paquets actuels (avec filtres)
    fetch(`/api/packets${buildQuery()}`)
        .then(r => r.json())
        .then(data => {
            const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = "packets.json";
            a.click();
            URL.revokeObjectURL(url);
        });
}

// --- Events ---
window.addEventListener("DOMContentLoaded", async () => {
    initChart();

    $("btnRefresh").addEventListener("click", refreshAll);
    $("btnClear").addEventListener("click", clearServer);
    $("btnExport").addEventListener("click", exportJson);

    $("refreshMs").addEventListener("change", setupAutoRefresh);

    // refresh automatique quand l'utilisateur change un filtre
    ["ip","protocol","direction","periodSec","limit"].forEach(id => {
        $(id).addEventListener("change", refreshAll);
        $(id).addEventListener("keyup", (e) => {
            if (id === "ip" || id === "search") refreshAll();
        });
    });
    $("search").addEventListener("keyup", refreshAll);

    setInterval(nowClock, 500);
    setupAutoRefresh();
    await refreshAll();
});