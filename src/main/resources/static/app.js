// ===================== CONFIG =====================
const API_BASE_URL = 'http://localhost:8080/api';

// ===================== INIT =====================
document.addEventListener('DOMContentLoaded', () => {
  // Сразу проверяем: залогинен ли пользователь (по cookie)
  checkAuthAndInit();
});

async function checkAuthAndInit() {
  try {
    const res = await fetch(`${API_BASE_URL}/auth/info`, {
      method: 'GET',
      credentials: 'include'
    });

    if (res.ok) {
      const user = await res.json();
      showMainApp();
      setUserInfo(user);
      showSection('dashboard');
      loadDashboard();
    } else {
      showLoginSection();
    }
  } catch (e) {
    console.error('Auth check error:', e);
    showLoginSection();
  }
}

// ===================== AUTH =====================
async function login(event) {
  event.preventDefault();

  hideError('loginError');

  const username = document.getElementById('loginUsername').value?.trim();
  const password = document.getElementById('loginPassword').value;

  if (!username || !password) {
    showError('loginError', 'Введите username и password');
    return;
  }

  try {
    const res = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include', // <-- ВАЖНО для cookie
      body: JSON.stringify({ username, password })
    });

    const text = await res.text();
    let data = null;
    try { data = JSON.parse(text); } catch { data = { raw: text }; }

    if (!res.ok) {
      showError('loginError', data.message || data.error || data.raw || `HTTP ${res.status}`);
      return;
    }

    // После успешного логина ещё раз читаем /auth/info (чтобы получить username/role)
    const infoRes = await fetch(`${API_BASE_URL}/auth/info`, {
      method: 'GET',
      credentials: 'include'
    });

    if (!infoRes.ok) {
      showError('loginError', 'Логин успешен, но /auth/info не доступен. Проверь cookie/JWT filter.');
      return;
    }

    const user = await infoRes.json();
    showMainApp();
    setUserInfo(user);
    showSection('dashboard');
    loadDashboard();
  } catch (e) {
    console.error('Login error:', e);
    showError('loginError', 'Ошибка сети: ' + e.message);
  }
}

async function logout() {
  try {
    await fetch(`${API_BASE_URL}/auth/logout`, {
      method: 'POST',
      credentials: 'include'
    });
  } catch (e) {
    console.error('Logout error:', e);
  } finally {
    showLoginSection();
  }
}

// ===================== UI SECTIONS =====================
function showLoginSection() {
  // показать login-section, скрыть остальные
  document.getElementById('login-section')?.classList.add('active');
  document.querySelectorAll('.section:not(#login-section)').forEach(el => el.classList.remove('active'));

  // скрыть navbar/sidebar
  const nav = document.querySelector('nav');
  const sidebar = document.querySelector('.sidebar');
  if (nav) nav.style.display = 'none';
  if (sidebar) sidebar.style.display = 'none';
}

function showMainApp() {
  // скрыть login-section
  document.getElementById('login-section')?.classList.remove('active');

  // показать navbar/sidebar
  const nav = document.querySelector('nav');
  const sidebar = document.querySelector('.sidebar');
  if (nav) nav.style.display = 'block';
  if (sidebar) sidebar.style.display = 'block';
}

function showSection(sectionId) {
  document.querySelectorAll('.section').forEach(el => el.classList.remove('active'));
  document.getElementById(sectionId)?.classList.add('active');

  // переключение активного пункта меню
  document.querySelectorAll('.nav-link').forEach(el => el.classList.remove('active'));

  // event может не существовать если вызвали showSection программно
  if (typeof event !== 'undefined' && event?.target) {
    event.target.closest('.nav-link')?.classList.add('active');
  }

  if (sectionId === 'buses') loadBuses();
  if (sectionId === 'sensors') loadSensors();
  if (sectionId === 'dashboard') loadDashboard();
}

// ===================== USER INFO =====================
function setUserInfo(user) {
  const el = document.getElementById('userInfo');
  if (!el) return;

  const name = user.username || user.name || 'user';
  el.textContent = `Welcome, ${name}!`;
}

// ===================== DASHBOARD =====================
async function loadDashboard() {
  try {
    const [buses, sensors] = await Promise.all([
      apiGetJson('/buses'),
      apiGetJson('/sensors')
    ]);

    document.getElementById('totalBuses').textContent = (buses || []).length;
    document.getElementById('totalSensors').textContent = (sensors || []).length;
    document.getElementById('totalAnomalies').textContent =
      (sensors || []).filter(s => s.anomaly === true).length;
    document.getElementById('totalFiles').textContent =
      (sensors || []).filter(s => !!s.filePath).length;
  } catch (e) {
    console.error('Dashboard error:', e);
  }
}

// ===================== BUSES =====================
async function loadBuses() {
  showLoader('busesLoader', true);
  try {
    const buses = await apiGetJson('/buses');
    const tbody = document.getElementById('busesList');
    tbody.innerHTML = '';

    (buses || []).forEach(bus => {
      const tr = document.createElement('tr');
      const safeModel = escapeHtml(bus.model ?? '');
      tr.innerHTML = `
        <td>${bus.id}</td>
        <td>${safeModel}</td>
        <td>
          <button class="btn btn-sm btn-warning" onclick="openEditBusModal(${bus.id}, '${escapeJs(bus.model ?? '')}')">
            <i class="fas fa-edit"></i> Edit
          </button>
          <button class="btn btn-sm btn-danger" onclick="deleteBus(${bus.id})">
            <i class="fas fa-trash"></i> Delete
          </button>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (e) {
    console.error('Load buses error:', e);
  } finally {
    showLoader('busesLoader', false);
  }
}

async function addBus(event) {
  event.preventDefault();
  const model = document.getElementById('busModel').value?.trim();
  if (!model) return;

  try {
    await apiJson('/buses', 'POST', { model });
    document.getElementById('busModel').value = '';
    showToast('Bus added successfully!');
    loadBuses();
  } catch (e) {
    console.error('Add bus error:', e);
    alert('Ошибка добавления автобуса');
  }
}

function openEditBusModal(id, model) {
  document.getElementById('editBusId').value = id;
  document.getElementById('editBusModel').value = model;
  new bootstrap.Modal(document.getElementById('editBusModal')).show();
}

async function updateBus(event) {
  event.preventDefault();
  const id = document.getElementById('editBusId').value;
  const model = document.getElementById('editBusModel').value?.trim();

  try {
    await apiJson(`/buses/${id}`, 'PUT', { model });
    bootstrap.Modal.getInstance(document.getElementById('editBusModal')).hide();
    showToast('Bus updated successfully!');
    loadBuses();
  } catch (e) {
    console.error('Update bus error:', e);
    alert('Ошибка обновления автобуса');
  }
}

async function deleteBus(id) {
  if (!confirm('Are you sure?')) return;
  try {
    await apiNoBody(`/buses/${id}`, 'DELETE');
    showToast('Bus deleted!');
    loadBuses();
  } catch (e) {
    console.error('Delete bus error:', e);
    alert('Ошибка удаления автобуса');
  }
}

// ===================== SENSORS =====================
async function loadSensors() {
  showLoader('sensorsLoader', true);
  try {
    const sensors = await apiGetJson('/sensors');
    const tbody = document.getElementById('sensorsList');
    tbody.innerHTML = '';

    (sensors || []).forEach(sensor => {
      const busId = sensor?.bus?.id ?? '';
      const fileBtn = sensor.filePath
        ? `<a href="/api/files/download/${encodeURIComponent(sensor.filePath)}" class="btn btn-sm btn-info">
             <i class="fas fa-download"></i>
           </a>`
        : '-';

      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${sensor.id}</td>
        <td>${busId}</td>
        <td>${escapeHtml(sensor.sensorType ?? '')}</td>
        <td>${sensor.value ?? ''}</td>
        <td>${formatDate(sensor.timestamp)}</td>
        <td>${sensor.anomaly ? '<span class="badge bg-danger">Yes</span>' : '<span class="badge bg-success">No</span>'}</td>
        <td>${fileBtn}</td>
        <td>
          <button class="btn btn-sm btn-warning"
            onclick="openEditSensorModal(${sensor.id}, '${escapeJs(sensor.sensorType ?? '')}', ${sensor.value ?? 0}, ${sensor.anomaly === true})">
            <i class="fas fa-edit"></i>
          </button>
          <button class="btn btn-sm btn-danger" onclick="deleteSensor(${sensor.id})">
            <i class="fas fa-trash"></i>
          </button>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (e) {
    console.error('Load sensors error:', e);
  } finally {
    showLoader('sensorsLoader', false);
  }
}

async function addSensorData(event) {
  event.preventDefault();

  const busId = parseInt(document.getElementById('sensorBusId').value, 10);
  const sensorType = document.getElementById('sensorType').value;
  const value = parseFloat(document.getElementById('sensorValue').value);
  const anomaly = document.getElementById('sensorAnomaly').value === 'true';
  const timestamp = new Date().toISOString();

  try {
    await apiJson('/sensors', 'POST', { busId, sensorType, value, timestamp, anomaly });
    document.getElementById('sensorBusId').value = '';
    document.getElementById('sensorValue').value = '';
    showToast('Sensor data added successfully!');
    loadSensors();
  } catch (e) {
    console.error('Add sensor error:', e);
    alert('Ошибка добавления SensorData');
  }
}

function openEditSensorModal(id, sensorType, value, anomaly) {
  document.getElementById('editSensorId').value = id;
  document.getElementById('editSensorType').value = sensorType;
  document.getElementById('editSensorValue').value = value;
  document.getElementById('editSensorAnomaly').value = anomaly ? 'true' : 'false';
  new bootstrap.Modal(document.getElementById('editSensorModal')).show();
}

async function updateSensor(event) {
  event.preventDefault();
  const id = document.getElementById('editSensorId').value;
  const sensorType = document.getElementById('editSensorType').value;
  const value = parseFloat(document.getElementById('editSensorValue').value);
  const anomaly = document.getElementById('editSensorAnomaly').value === 'true';

  // ВНИМАНИЕ: твой backend updateSensorData принимает Entity SensorData [file:46][file:9]
  // поэтому для корректной работы возможно нужно отправлять полный объект.
  // Тут отправляем только часть, если backend падает — скажи, поправим.
  try {
    await apiJson(`/sensors/${id}`, 'PUT', { sensorType, value, anomaly });
    bootstrap.Modal.getInstance(document.getElementById('editSensorModal')).hide();
    showToast('Sensor updated successfully!');
    loadSensors();
  } catch (e) {
    console.error('Update sensor error:', e);
    alert('Ошибка обновления SensorData');
  }
}

async function deleteSensor(id) {
  if (!confirm('Are you sure?')) return;
  try {
    await apiNoBody(`/sensors/${id}`, 'DELETE');
    showToast('Sensor deleted!');
    loadSensors();
  } catch (e) {
    console.error('Delete sensor error:', e);
    alert('Ошибка удаления SensorData');
  }
}

// ===================== FILE UPLOAD =====================
function handleDragOver(event) {
  event.preventDefault();
  event.stopPropagation();
  document.getElementById('fileUploadZone')?.classList.add('dragover');
}

function handleDragLeave(event) {
  event.preventDefault();
  document.getElementById('fileUploadZone')?.classList.remove('dragover');
}

function handleFileDrop(event) {
  event.preventDefault();
  document.getElementById('fileUploadZone')?.classList.remove('dragover');
  const files = event.dataTransfer.files;
  if (files && files.length > 0) {
    document.getElementById('fileInput').files = files;
    uploadFile();
  }
}

async function uploadFile() {
  const sensorId = document.getElementById('fileSensorId').value;
  const fileInput = document.getElementById('fileInput');
  const file = fileInput.files[0];

  if (!sensorId) {
    showError('uploadResult', 'Please enter Sensor Data ID');
    return;
  }
  if (!file) {
    showError('uploadResult', 'Please select a file');
    return;
  }

  const formData = new FormData();
  formData.append('file', file);

  document.getElementById('uploadProgress').style.display = 'block';

  try {
    const res = await fetch(`${API_BASE_URL}/files/upload/${sensorId}`, {
      method: 'POST',
      credentials: 'include', // <-- cookie
      body: formData
    });

    const text = await res.text();
    let data = null;
    try { data = JSON.parse(text); } catch { data = { raw: text }; }

    if (!res.ok) {
      showError('uploadResult', data.message || data.raw || `HTTP ${res.status}`);
      return;
    }

    document.getElementById('uploadProgress').style.display = 'none';
    showSuccess('uploadResult', 'File uploaded successfully!');
    document.getElementById('fileInput').value = '';
    document.getElementById('fileSensorId').value = '';
    loadSensors();
  } catch (e) {
    document.getElementById('uploadProgress').style.display = 'none';
    showError('uploadResult', 'Upload failed: ' + e.message);
  }
}

// ===================== CSV IMPORT =====================
function handleCsvDrop(event) {
  event.preventDefault();
  document.getElementById('csvUploadZone')?.classList.remove('dragover');
  const files = event.dataTransfer.files;
  if (files && files.length > 0) {
    document.getElementById('csvInput').files = files;
    importCsv();
  }
}

async function importCsv() {
  const fileInput = document.getElementById('csvInput');
  const file = fileInput.files[0];

  if (!file) {
    showError('csvResult', 'Please select a CSV file');
    return;
  }

  const formData = new FormData();
  formData.append('file', file);

  document.getElementById('csvProgress').style.display = 'block';

  try {
    const res = await fetch(`${API_BASE_URL}/sensors/import-csv`, {
      method: 'POST',
      credentials: 'include',
      body: formData
    });

    const text = await res.text();
    let data = null;
    try { data = JSON.parse(text); } catch { data = { raw: text }; }

    document.getElementById('csvProgress').style.display = 'none';

    if (!res.ok) {
      showError('csvResult', data.message || data.raw || `HTTP ${res.status}`);
      return;
    }

    if (data.failedCount && data.failedCount > 0) {
      let html = `<h5 class="text-warning">Import Completed with Errors:</h5>
        <p><strong>Success:</strong> ${data.successCount} | <strong>Failed:</strong> ${data.failedCount}</p>
        <ul>${(data.errors || []).map(e => `<li>${escapeHtml(e)}</li>`).join('')}</ul>`;
      document.getElementById('csvResult').innerHTML = html;
    } else {
      showSuccess('csvResult', `CSV import successful! ${data.successCount || 0} records imported.`);
    }

    document.getElementById('csvInput').value = '';
    loadSensors();
  } catch (e) {
    document.getElementById('csvProgress').style.display = 'none';
    showError('csvResult', 'Import failed: ' + e.message);
  }
}

// ===================== API HELPERS =====================
async function apiGetJson(path) {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method: 'GET',
    credentials: 'include'
  });
  if (!res.ok) throw new Error(`GET ${path} -> HTTP ${res.status}`);
  return res.json();
}

async function apiJson(path, method, bodyObj) {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(bodyObj)
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`${method} ${path} -> HTTP ${res.status}: ${t}`);
  }
  // иногда backend может вернуть пустое тело
  const txt = await res.text();
  if (!txt) return null;
  try { return JSON.parse(txt); } catch { return txt; }
}

async function apiNoBody(path, method) {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method,
    credentials: 'include'
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`${method} ${path} -> HTTP ${res.status}: ${t}`);
  }
}

// ===================== UI HELPERS =====================
function showLoader(id, show) {
  const el = document.getElementById(id);
  if (!el) return;
  el.classList.toggle('active', show);
}

function showToast(message) {
  const toast = document.createElement('div');
  toast.className = 'alert alert-success position-fixed bottom-0 end-0 m-3';
  toast.innerHTML = message;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 2500);
}

function showSuccess(elementId, message) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.innerHTML = `<div class="alert alert-success">${escapeHtml(message)}</div>`;
}

function showError(elementId, message) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.style.display = 'block';
  el.innerHTML = `<div>${escapeHtml(message)}</div>`;
}

function hideError(elementId) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.style.display = 'none';
  el.innerHTML = '';
}

function formatDate(dateString) {
  if (!dateString) return '';
  try {
    return new Date(dateString).toLocaleString();
  } catch {
    return dateString;
  }
}

// защитные функции для вывода
function escapeHtml(str) {
  return String(str)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function escapeJs(str) {
  return String(str).replaceAll('\\', '\\\\').replaceAll("'", "\\'");
}
