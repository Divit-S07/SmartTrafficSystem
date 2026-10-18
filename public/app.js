// ============================================================================
// Vegam City Smart Traffic Control Desk — Enhanced Vanilla JavaScript Controller
// Provides Full CRUD: Create, View, Edit, and Delete for All Entities
// ============================================================================

const API_BASE = window.location.origin;
let currentData = null;

// Preset layout coordinates for demo junctions (J1 to J12)
const JUNCTION_COORDS = {
  J1:  { x: 90,  y: 190 },
  J2:  { x: 220, y: 80 },
  J3:  { x: 380, y: 80 },
  J4:  { x: 530, y: 80 },
  J5:  { x: 240, y: 220 },
  J6:  { x: 400, y: 220 },
  J7:  { x: 90,  y: 320 },
  J8:  { x: 220, y: 340 },
  J9:  { x: 380, y: 340 },
  J10: { x: 530, y: 340 },
  J11: { x: 670, y: 220 },
  J12: { x: 670, y: 110 }
};

document.addEventListener("DOMContentLoaded", () => {
  refreshAll();
  setInterval(refreshAll, 3000); // Live poll every 3 seconds
});

async function refreshAll() {
  await fetchStatus();
  await fetchLogs();
}

// ------------------------------------------------------------------
// Fetch System Status & Render UI Components
// ------------------------------------------------------------------
async function fetchStatus() {
  try {
    const res = await fetch(`${API_BASE}/api/status`);
    if (!res.ok) return;
    const data = await res.json();
    currentData = data;

    renderBadges(data);
    renderVehicles(data.vehicles || []);
    renderJunctionsAndRoads(data.junctions || [], data.roads || []);
    renderRequests(data.pendingRequests || []);
    renderEvents(data.pendingEvents || []);
    renderDropdowns(data.junctions || [], data.vehicles || []);
    renderCanvas(data);
  } catch (err) {
    console.error("Status fetch error:", err);
  }
}

function renderBadges(data) {
  const dbBadge = document.getElementById("statusDb");
  const cloudBadge = document.getElementById("statusCloud");

  if (data.database && data.database.mysqlConnected) {
    dbBadge.innerText = "● MySQL: Connected";
    dbBadge.className = "badge badge-online";
  } else {
    dbBadge.innerText = "● MySQL: Ready";
    dbBadge.className = "badge badge-db";
  }

  if (data.database && data.database.supabaseConfigured) {
    cloudBadge.innerText = "● Supabase: Connected";
    cloudBadge.className = "badge badge-online";
  } else {
    cloudBadge.innerText = "● Supabase: Ready";
    cloudBadge.className = "badge badge-cloud";
  }
}

// ------------------------------------------------------------------
// Render Emergency Vehicles (with Edit and Delete)
// ------------------------------------------------------------------
function renderVehicles(vehicles) {
  const tbody = document.getElementById("vehicleList");
  document.getElementById("vehicleCount").innerText = `${vehicles.length} Registered`;
  if (!vehicles || vehicles.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No emergency vehicles registered. Use "🚑 Vehicle" tab to register one.</td></tr>`;
    return;
  }

  tbody.innerHTML = vehicles.map(v => {
    let routeDisplay = "None";
    let advanceAction = "";

    if (v.route && v.route.path && v.route.path.length > 0) {
      if (v.route.status === "UNREACHABLE") {
        routeDisplay = `<span style="color:var(--accent-rose)">UNREACHABLE</span>`;
      } else {
        routeDisplay = `<span class="route-text">${v.route.path.join(" → ")} (${v.route.time.toFixed(1)}m)</span>`;
        if (v.route.path.length >= 2 && v.status === "EN_ROUTE") {
          const nextHop = v.route.path[1];
          advanceAction = `<button class="btn btn-sm btn-primary" onclick="advanceVehicle('${v.id}','${nextHop}')">Step to ${nextHop}</button>`;
        }
      }
    }

    const typeIcons = { AMBULANCE: "🚑", FIRE_ENGINE: "🚒", RESCUE: "🚨" };
    return `
      <tr>
        <td><strong>${typeIcons[v.type] || "🚗"} ${v.id}</strong></td>
        <td>${v.type}</td>
        <td><span class="pill">${v.position}</span></td>
        <td>${v.destination ? `<span class="pill pill-warning">${v.destination}</span>` : "-"}</td>
        <td><span class="status-tag status-${v.status}">${v.status}</span></td>
        <td>${routeDisplay}</td>
        <td style="display:flex; gap:4px; flex-wrap:wrap;">
          ${advanceAction}
          <button class="btn btn-sm btn-outline" onclick="editVehicle('${v.id}','${v.type}','${v.position}','${v.status}')" title="Edit Vehicle">✏️</button>
          <button class="btn btn-sm btn-danger" onclick="deleteVehicle('${v.id}')" title="Delete Vehicle">🗑️</button>
        </td>
      </tr>
    `;
  }).join("");
}

// ------------------------------------------------------------------
// Render Junctions and Roads (with Edit and Delete)
// ------------------------------------------------------------------
function renderJunctionsAndRoads(junctions, roads) {
  document.getElementById("junctionCount").innerText = `${junctions.length} Junctions`;
  document.getElementById("roadCount").innerText = `${roads.length} Roads`;

  // Junctions badges
  const juncContainer = document.getElementById("junctionList");
  if (!junctions || junctions.length === 0) {
    juncContainer.innerHTML = `<span class="empty-state" style="padding:4px 8px;">No junctions added yet</span>`;
  } else {
    juncContainer.innerHTML = junctions.map(j => `
      <span class="pill" style="display:inline-flex; align-items:center; gap:6px; background:#1e293b; color:#fff;">
        <strong>${j}</strong>
        <span onclick="deleteJunction('${j}')" style="cursor:pointer; color:#f43f5e; font-weight:bold;" title="Delete Junction ${j}">✖</span>
      </span>
    `).join("");
  }

  // Roads Table
  const roadsTbody = document.getElementById("roadsList");
  if (!roads || roads.length === 0) {
    roadsTbody.innerHTML = `<tr><td colspan="5" class="empty-state">No roads in the network. Add roads using the "🛣️ Road/Node" tab.</td></tr>`;
    return;
  }

  roadsTbody.innerHTML = roads.map(r => {
    let statusBadge = `<span class="status-tag" style="background:#065f46; color:#34d399;">Clear</span>`;
    if (r.isClosed) {
      statusBadge = `<span class="status-tag status-UNREACHABLE">Closed</span>`;
    } else if (r.currentTravelTime > r.baseTravelTime) {
      statusBadge = `<span class="status-tag" style="background:#78350f; color:#fcd34d;">Congested</span>`;
    }

    return `
      <tr>
        <td><strong>${r.from}</strong></td>
        <td><strong>${r.to}</strong></td>
        <td>${r.currentTravelTime.toFixed(1)}m ${r.currentTravelTime !== r.baseTravelTime ? `(base ${r.baseTravelTime.toFixed(1)}m)` : ""}</td>
        <td>${statusBadge}</td>
        <td style="display:flex; gap:4px;">
          <button class="btn btn-sm btn-outline" onclick="editRoad('${r.from}','${r.to}',${r.currentTravelTime},${r.isClosed})" title="Edit Road Travel Time or Open/Close">✏️ Edit</button>
          <button class="btn btn-sm btn-danger" onclick="deleteRoad('${r.from}','${r.to}')" title="Delete Road">🗑️</button>
        </td>
      </tr>
    `;
  }).join("");
}

// ------------------------------------------------------------------
// Render Priority Queue Requests (with Edit and Delete)
// ------------------------------------------------------------------
function renderRequests(reqs) {
  const container = document.getElementById("requestsList");
  document.getElementById("requestCount").innerText = `${reqs.length} Pending`;
  if (!reqs || reqs.length === 0) {
    container.innerHTML = `<div class="empty-state">No pending emergency requests in priority queue</div>`;
    return;
  }

  container.innerHTML = reqs.map(r => `
    <div class="queue-item">
      <div class="queue-item-left">
        <span class="severity-badge sev-${r.severity}">${r.severity}</span>
        <strong>${r.id}</strong>: <span>${r.vehicleId} → <strong>${r.destination}</strong></span>
        <span style="color:var(--text-dim)">(#${r.arrivalSequence})</span>
      </div>
      <div style="display:flex; gap:4px;">
        <button class="btn btn-sm btn-outline" onclick="editRequest('${r.id}','${r.destination}','${r.severity}')" title="Edit Destination or Severity">✏️</button>
        <button class="btn btn-sm btn-outline" onclick="promptEscalate('${r.id}')" title="Escalate Priority">⬆️</button>
        <button class="btn btn-sm btn-danger" onclick="deleteRequest('${r.id}')" title="Cancel Request">🗑️</button>
      </div>
    </div>
  `).join("");
}

// ------------------------------------------------------------------
// Render FIFO Traffic Events (with Delete)
// ------------------------------------------------------------------
function renderEvents(events) {
  const container = document.getElementById("eventsList");
  document.getElementById("eventCount").innerText = `${events.length} Pending`;
  if (!events || events.length === 0) {
    container.innerHTML = `<div class="empty-state">No pending traffic events in FIFO queue</div>`;
    return;
  }

  container.innerHTML = events.map(e => `
    <div class="queue-item">
      <div class="queue-item-left">
        <span>🚧</span>
        <span>#${e.sequence} ${e.description}</span>
      </div>
      <div style="display:flex; align-items:center; gap:6px;">
        <span class="pill">FIFO</span>
        <button class="btn btn-sm btn-danger" onclick="deleteEvent(${e.sequence})" title="Delete event from queue">🗑️</button>
      </div>
    </div>
  `).join("");
}

function renderDropdowns(junctions, vehicles) {
  const reqVeh = document.getElementById("reqVehicle");
  const reqDest = document.getElementById("reqDest");
  const evtFrom = document.getElementById("evtFrom");
  const evtTo = document.getElementById("evtTo");
  const vehStart = document.getElementById("vehStart");

  const curVeh = reqVeh.value;
  const curDest = reqDest.value;
  const curFrom = evtFrom.value;
  const curTo = evtTo.value;
  const curStart = vehStart.value;

  if (vehicles && vehicles.length > 0) {
    reqVeh.innerHTML = vehicles.map(v => `<option value="${v.id}">${v.id} (${v.type} @ ${v.position})</option>`).join("");
    if (curVeh) reqVeh.value = curVeh;
  } else {
    reqVeh.innerHTML = `<option value="">No vehicles available</option>`;
  }

  if (junctions && junctions.length > 0) {
    const jOpts = junctions.map(j => `<option value="${j}">${j}</option>`).join("");
    reqDest.innerHTML = jOpts;
    evtFrom.innerHTML = jOpts;
    evtTo.innerHTML = jOpts;
    vehStart.innerHTML = jOpts;

    if (curDest) reqDest.value = curDest;
    if (curFrom) evtFrom.value = curFrom;
    if (curTo) evtTo.value = curTo;
    if (curStart) vehStart.value = curStart;
  } else {
    const none = `<option value="">No junctions added</option>`;
    reqDest.innerHTML = none;
    evtFrom.innerHTML = none;
    evtTo.innerHTML = none;
    vehStart.innerHTML = none;
  }
}

// ------------------------------------------------------------------
// Canvas Graph Visualizer
// ------------------------------------------------------------------
function renderCanvas(data) {
  const canvas = document.getElementById("networkCanvas");
  if (!canvas) return;
  const ctx = canvas.getContext("2d");
  const width = canvas.width;
  const height = canvas.height;

  ctx.clearRect(0, 0, width, height);

  // Background Grid Pattern
  ctx.strokeStyle = "#162033";
  ctx.lineWidth = 1;
  for (let x = 0; x < width; x += 40) {
    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, height); ctx.stroke();
  }
  for (let y = 0; y < height; y += 40) {
    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(width, y); ctx.stroke();
  }

  const junctions = data.junctions || [];
  const roads = data.roads || [];
  const vehicles = data.vehicles || [];

  if (junctions.length === 0) {
    ctx.font = "14px sans-serif";
    ctx.fillStyle = "#64748b";
    ctx.textAlign = "center";
    ctx.fillText("Network is empty — Add Junctions & Roads manually, or click 'Load Sample Dataset' above.", width / 2, height / 2);
    return;
  }

  // Calculate coordinates for any user-added junctions
  junctions.forEach((j, index) => {
    if (!JUNCTION_COORDS[j]) {
      const col = (index % 5) + 1;
      const row = Math.floor(index / 5) + 1;
      JUNCTION_COORDS[j] = { x: 120 * col, y: 70 * row };
    }
  });

  // Active routes highlight
  const activeRouteSegments = new Set();
  vehicles.forEach(v => {
    if (v.status === "EN_ROUTE" && v.route && v.route.path) {
      for (let i = 0; i < v.route.path.length - 1; i++) {
        activeRouteSegments.add(`${v.route.path[i]}->${v.route.path[i+1]}`);
      }
    }
  });

  // 1. Draw Roads
  roads.forEach(r => {
    const p1 = JUNCTION_COORDS[r.from];
    const p2 = JUNCTION_COORDS[r.to];
    if (!p1 || !p2) return;

    const isRoute = activeRouteSegments.has(`${r.from}->${r.to}`);

    ctx.save();
    if (r.isClosed) {
      ctx.strokeStyle = "#f43f5e";
      ctx.lineWidth = 2.5;
      ctx.setLineDash([5, 5]);
    } else if (r.currentTravelTime > r.baseTravelTime) {
      ctx.strokeStyle = "#f59e0b";
      ctx.lineWidth = 3;
    } else if (isRoute) {
      ctx.strokeStyle = "#06b6d4";
      ctx.lineWidth = 4;
      ctx.shadowColor = "#06b6d4";
      ctx.shadowBlur = 8;
    } else {
      ctx.strokeStyle = "#334155";
      ctx.lineWidth = 2;
    }

    ctx.beginPath();
    ctx.moveTo(p1.x, p1.y);
    ctx.lineTo(p2.x, p2.y);
    ctx.stroke();
    ctx.restore();

    // Road time label
    const midX = (p1.x + p2.x) / 2;
    const midY = (p1.y + p2.y) / 2;
    ctx.font = "10px monospace";
    ctx.fillStyle = r.isClosed ? "#f43f5e" : (r.currentTravelTime > r.baseTravelTime ? "#f59e0b" : "#64748b");
    const label = r.isClosed ? "CLOSED" : `${r.currentTravelTime.toFixed(1)}m`;
    ctx.fillText(label, midX - 10, midY - 6);
  });

  // 2. Draw Junction Nodes
  junctions.forEach(j => {
    const pt = JUNCTION_COORDS[j];
    if (!pt) return;

    ctx.beginPath();
    ctx.arc(pt.x, pt.y, 14, 0, Math.PI * 2);
    ctx.fillStyle = "#1e293b";
    ctx.fill();
    ctx.strokeStyle = "#38bdf8";
    ctx.lineWidth = 2;
    ctx.stroke();

    ctx.beginPath();
    ctx.arc(pt.x, pt.y, 4, 0, Math.PI * 2);
    ctx.fillStyle = "#38bdf8";
    ctx.fill();

    ctx.font = "bold 11px sans-serif";
    ctx.fillStyle = "#ffffff";
    ctx.textAlign = "center";
    ctx.fillText(j, pt.x, pt.y + 26);
  });

  // 3. Draw Vehicle Markers
  vehicles.forEach((v, idx) => {
    const pt = JUNCTION_COORDS[v.position];
    if (!pt) return;

    const offsetAngle = (idx * (Math.PI / 2));
    const vx = pt.x + Math.cos(offsetAngle) * 22;
    const vy = pt.y + Math.sin(offsetAngle) * 22;

    const colors = { AMBULANCE: "#10b981", FIRE_ENGINE: "#ef4444", RESCUE: "#f59e0b" };
    ctx.beginPath();
    ctx.arc(vx, vy, 11, 0, Math.PI * 2);
    ctx.fillStyle = colors[v.type] || "#3b82f6";
    ctx.fill();
    ctx.strokeStyle = "#ffffff";
    ctx.lineWidth = 1.5;
    ctx.stroke();

    ctx.font = "9px sans-serif";
    ctx.fillStyle = "#ffffff";
    ctx.textAlign = "center";
    ctx.fillText(v.id.substring(0, 3), vx, vy + 3);
  });
}

// ------------------------------------------------------------------
// Full CRUD Action Handlers (Create, Edit, Delete, Clear)
// ------------------------------------------------------------------

async function clearAllData() {
  if (!confirm("Are you sure you want to clear all data? This will reset the network to an empty state.")) return;
  await fetch(`${API_BASE}/api/clear`, { method: "POST" });
  await refreshAll();
}

async function loadSampleData() {
  await fetch(`${API_BASE}/api/sample-data`, { method: "POST" });
  await refreshAll();
}

async function runScriptedDemo() {
  const res = await fetch(`${API_BASE}/api/demo`, { method: "POST" });
  const data = await res.json();
  alert("Scripted Demonstration Completed! Check the Activity Log Feed below for detailed outputs.");
  await refreshAll();
}

// --- Junction Actions ---
async function handleJunctionSubmit(e) {
  e.preventDefault();
  const id = document.getElementById("juncId").value.trim().toUpperCase();
  if (!id) return;
  const res = await fetch(`${API_BASE}/api/junction`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ id })
  });
  if (res.ok) {
    document.getElementById("juncId").value = "";
    await refreshAll();
  }
}

async function deleteJunction(id) {
  if (!confirm(`Delete junction ${id} and all connected roads?`)) return;
  await fetch(`${API_BASE}/api/junction/delete`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ id })
  });
  await refreshAll();
}

// --- Road Actions ---
async function handleRoadSubmit(e) {
  e.preventDefault();
  const from = document.getElementById("roadFrom").value.trim().toUpperCase();
  const to = document.getElementById("roadTo").value.trim().toUpperCase();
  const time = document.getElementById("roadTime").value;
  const twoWay = document.getElementById("roadTwoWay").value === "true";

  const res = await fetch(`${API_BASE}/api/road`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ from, to, time, twoWay })
  });
  if (res.ok) {
    alert("Road added to network!");
    document.getElementById("roadFrom").value = "";
    document.getElementById("roadTo").value = "";
    await refreshAll();
  }
}

async function editRoad(from, to, curTime, isClosed) {
  const newTimeStr = prompt(`Edit base travel time (minutes) for ${from} -> ${to}:`, curTime);
  if (newTimeStr === null) return;
  const newTime = parseFloat(newTimeStr);
  if (isNaN(newTime) || newTime <= 0) {
    alert("Invalid travel time.");
    return;
  }
  const closedToggle = confirm(`Is road ${from} -> ${to} currently CLOSED?\nClick OK for CLOSED, Cancel for OPEN.`);
  await fetch(`${API_BASE}/api/road/update`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ from, to, time: newTime, closed: closedToggle })
  });
  await refreshAll();
}

async function deleteRoad(from, to) {
  if (!confirm(`Delete road ${from} -> ${to}?`)) return;
  await fetch(`${API_BASE}/api/road/delete`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ from, to })
  });
  await refreshAll();
}

// --- Vehicle Actions ---
async function handleVehicleRegister(e) {
  e.preventDefault();
  const id = document.getElementById("vehId").value.trim().toUpperCase();
  const type = document.getElementById("vehType").value;
  const start = document.getElementById("vehStart").value;

  if (!id || !start) {
    alert("Please enter a vehicle ID and choose a starting junction (add a junction first if none exist).");
    return;
  }

  const res = await fetch(`${API_BASE}/api/vehicle`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ id, type, start })
  });
  if (res.ok) {
    alert(`Vehicle ${id} registered at ${start}!`);
    document.getElementById("vehId").value = "";
    await refreshAll();
  }
}

async function editVehicle(id, curType, curPos, curStatus) {
  const newPos = prompt(`Update current junction for vehicle ${id}:`, curPos);
  if (!newPos) return;
  const newType = prompt(`Update vehicle type (AMBULANCE / FIRE_ENGINE / RESCUE):`, curType);
  if (!newType) return;
  const newStatus = prompt(`Update status (IDLE / EN_ROUTE / ARRIVED / UNREACHABLE):`, curStatus);
  if (!newStatus) return;

  await fetch(`${API_BASE}/api/vehicle/update`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ id, type: newType.trim().toUpperCase(), junction: newPos.trim().toUpperCase(), status: newStatus.trim().toUpperCase() })
  });
  await refreshAll();
}

async function deleteVehicle(id) {
  if (!confirm(`Delete emergency vehicle ${id}?`)) return;
  await fetch(`${API_BASE}/api/vehicle/delete`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ id })
  });
  await refreshAll();
}

async function advanceVehicle(vehicleId, to) {
  await fetch(`${API_BASE}/api/vehicle/advance`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ vehicleId, to })
  });
  await refreshAll();
}

// --- Request Actions ---
async function handleRequestSubmit(e) {
  e.preventDefault();
  const vehicleId = document.getElementById("reqVehicle").value;
  const destination = document.getElementById("reqDest").value;
  const severity = document.getElementById("reqSeverity").value;

  if (!vehicleId || !destination) {
    alert("Please ensure vehicles and junctions exist before submitting requests.");
    return;
  }

  const res = await fetch(`${API_BASE}/api/request`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ vehicleId, destination, severity })
  });
  if (res.ok) {
    alert("Emergency routing request queued!");
    await refreshAll();
  }
}

async function editRequest(id, curDest, curSev) {
  const newDest = prompt(`Edit destination junction for ${id}:`, curDest);
  if (!newDest) return;
  const newSev = prompt(`Edit severity for ${id} (CRITICAL / HIGH / NORMAL):`, curSev);
  if (!newSev) return;

  await fetch(`${API_BASE}/api/request/update`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ id, destination: newDest.trim().toUpperCase(), severity: newSev.trim().toUpperCase() })
  });
  await refreshAll();
}

async function promptEscalate(requestId) {
  const newSev = prompt(`Escalate Severity for ${requestId} to:\nCRITICAL / HIGH / NORMAL`, "CRITICAL");
  if (!newSev) return;
  await fetch(`${API_BASE}/api/request/escalate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ requestId, severity: newSev.trim().toUpperCase() })
  });
  await refreshAll();
}

async function deleteRequest(id) {
  if (!confirm(`Cancel routing request ${id}?`)) return;
  await fetch(`${API_BASE}/api/request/delete`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ id })
  });
  await refreshAll();
}

// --- Event Actions ---
async function handleEventSubmit(e) {
  e.preventDefault();
  const type = document.getElementById("evtType").value;
  const from = document.getElementById("evtFrom").value;
  const to = document.getElementById("evtTo").value;
  const factor = document.getElementById("evtFactor").value;

  const res = await fetch(`${API_BASE}/api/event`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ type, from, to, factor })
  });
  if (res.ok) {
    alert("Traffic event queued!");
    await refreshAll();
  }
}

async function deleteEvent(sequence) {
  if (!confirm(`Delete traffic event #${sequence} from FIFO queue?`)) return;
  await fetch(`${API_BASE}/api/event/delete`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ sequence })
  });
  await refreshAll();
}

// --- Queue Processing ---
async function processNextEvent() {
  await fetch(`${API_BASE}/api/event/process-next`, { method: "POST" });
  await refreshAll();
}

async function processAllEvents() {
  await fetch(`${API_BASE}/api/event/process-all`, { method: "POST" });
  await refreshAll();
}

async function processNextRequest() {
  await fetch(`${API_BASE}/api/request/process-next`, { method: "POST" });
  await refreshAll();
}

async function processAllRequests() {
  await fetch(`${API_BASE}/api/request/process-all`, { method: "POST" });
  await refreshAll();
}

// --- Cloud & SQL ---
async function handleCloudConfigSubmit(e) {
  e.preventDefault();
  const url = document.getElementById("sbUrl").value.trim();
  const key = document.getElementById("sbKey").value.trim();
  const bucket = document.getElementById("sbBucket").value.trim();

  await fetch(`${API_BASE}/api/supabase/config`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ url, key, bucket })
  });
  alert("Supabase settings updated!");
  await refreshAll();
}

async function backupToSupabase() {
  const res = await fetch(`${API_BASE}/api/supabase/backup`, { method: "POST" });
  const data = await res.json();
  if (data.success) {
    if (data.cloud) {
      alert(`Cloud Backup Successful!\nSnapshot saved to Supabase Storage Bucket:\n${data.filename}`);
    } else {
      alert(`Local Snapshot Saved!\n${data.message}`);
    }
  } else {
    alert(`Backup Notice: ${data.error || data.response || "Check config"}`);
  }
  await refreshAll();
}

function exportSqlDump() {
  window.open(`${API_BASE}/api/export-sql`, "_blank");
}

async function fetchLogs() {
  try {
    const res = await fetch(`${API_BASE}/api/logs`);
    if (!res.ok) return;
    const data = await res.json();
    const feed = document.getElementById("logFeed");
    if (data.logs && data.logs.length > 0) {
      feed.innerHTML = data.logs.slice(-30).map(l => `<div>${escapeHtml(l)}</div>`).join("");
      feed.scrollTop = feed.scrollHeight;
    }
  } catch (err) {}
}

function toggleFactorInput() {
  const type = document.getElementById("evtType").value;
  document.getElementById("factorGroup").style.display = (type === "CONGESTION") ? "flex" : "none";
}

function switchTab(tabId) {
  document.querySelectorAll(".tab-btn").forEach(btn => btn.classList.remove("active"));
  document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));
  document.getElementById(tabId).classList.add("active");
  event.target.classList.add("active");
}

function escapeHtml(str) {
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
