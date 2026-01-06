<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Link Generator | Private Files</title>
    <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🔗</text></svg>">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root{--bg:#0a0a0f;--surface:#14141f;--surface-2:#1a1a28;--border:#2a2a3a;--primary:#6366f1;--primary-hover:#5558e3;--success:#22c55e;--danger:#ef4444;--warning:#f59e0b;--text:#fff;--text-muted:#888899}
        *{margin:0;padding:0;box-sizing:border-box}
        body{font-family:'Inter',sans-serif;background:var(--bg);color:var(--text);min-height:100vh;padding-bottom:100px}
        
        .header{background:var(--surface);border-bottom:1px solid var(--border);padding:1rem 1.5rem;display:flex;align-items:center;gap:1rem;position:sticky;top:0;z-index:100}
        .header h1{font-size:1.25rem;flex:1;display:flex;align-items:center;gap:0.5rem}
        .header-btn{background:var(--surface-2);border:1px solid var(--border);color:var(--text-muted);padding:0.5rem 0.85rem;border-radius:8px;font-size:0.75rem;cursor:pointer;display:flex;align-items:center;gap:0.35rem}
        .header-btn:hover{color:var(--text);border-color:var(--primary)}
        .history-count{background:var(--primary);color:#fff;font-size:0.6rem;padding:0.1rem 0.35rem;border-radius:8px}
        
        .main{padding:1rem;max-width:700px;margin:0 auto}
        
        .card{background:var(--surface);border:1px solid var(--border);border-radius:12px;margin-bottom:1rem;overflow:hidden}
        .card-header{display:flex;align-items:center;justify-content:space-between;padding:0.85rem 1rem;cursor:pointer;user-select:none}
        .card-header:hover{background:var(--surface-2)}
        .card-title{font-size:0.9rem;font-weight:600;display:flex;align-items:center;gap:0.5rem;margin:0}
        .toggle-icon{color:var(--text-muted);font-size:0.7rem;transition:transform 0.2s}
        .card.open .toggle-icon{transform:rotate(180deg)}
        .card-body{display:none;padding:0 1rem 1rem;border-top:1px solid var(--border)}
        .card.open .card-body{display:block}
        
        .form-grid{display:grid;grid-template-columns:1fr 1fr;gap:0.75rem;padding-top:1rem}
        .form-grid.single{grid-template-columns:1fr}
        @media(max-width:500px){.form-grid{grid-template-columns:1fr}}
        .form-group{display:flex;flex-direction:column;gap:0.3rem}
        label{font-size:0.7rem;color:var(--text-muted);font-weight:500}
        input,select{background:var(--surface-2);border:1px solid var(--border);border-radius:8px;padding:0.65rem 0.85rem;color:var(--text);font-size:0.85rem;font-family:inherit}
        input:focus,select:focus{outline:none;border-color:var(--primary)}
        input::placeholder{color:#555}
        
        .landing-list{padding-top:0.75rem;display:grid;grid-template-columns:1fr 1fr;gap:0.5rem}
        @media(max-width:500px){.landing-list{grid-template-columns:1fr}}
        .landing-item{display:flex;align-items:center;gap:0.75rem;padding:0.6rem 0.75rem;background:var(--surface-2);border:1px solid var(--border);border-radius:8px;cursor:pointer;transition:all 0.15s}
        .landing-item:hover{border-color:var(--text-muted)}
        .landing-item.active{border-color:var(--primary);background:rgba(99,102,241,0.1)}
        .landing-item .num{background:var(--primary);color:#fff;width:22px;height:22px;border-radius:5px;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:0.75rem}
        .landing-item .name{flex:1;font-size:0.85rem;font-weight:500}
        .landing-item .preview-btn{background:transparent;border:1px solid var(--border);color:var(--text-muted);padding:0.3rem 0.6rem;border-radius:5px;font-size:0.65rem;cursor:pointer}
        .landing-item .preview-btn:hover{color:var(--text);border-color:var(--text)}
        
        .sticky-actions{position:fixed;bottom:0;left:0;right:0;background:var(--surface);border-top:1px solid var(--border);padding:0.75rem 1rem;display:flex;gap:0.75rem;justify-content:center;z-index:100;box-shadow:0 -5px 15px rgba(0,0,0,0.3)}
        .btn{display:inline-flex;align-items:center;justify-content:center;gap:0.4rem;padding:0.75rem 1.5rem;border-radius:10px;font-weight:600;font-size:0.85rem;cursor:pointer;border:none;transition:all 0.2s}
        .btn-primary{background:var(--primary);color:#fff}
        .btn-full{width:100%}
        .btn-outline{background:transparent;color:var(--text);border:1px solid var(--border)}
        .btn-sm{padding:0.5rem 1rem;font-size:0.75rem}
        
        .card.success-card{border-color:var(--success)}
        .output-box{background:var(--surface-2);border-radius:8px;padding:0.75rem;font-family:'Monaco',monospace;font-size:0.75rem;word-break:break-all;color:var(--success);position:relative}
        .copy-btn{position:absolute;top:0.4rem;right:0.4rem;background:var(--surface);border:1px solid var(--border);color:var(--text-muted);padding:0.25rem 0.5rem;border-radius:5px;font-size:0.6rem;cursor:pointer}
        .output-actions{display:flex;gap:0.5rem;margin-top:0.75rem}
        .variant-row{display:flex;align-items:center;gap:0.5rem;padding:0.4rem 0.6rem;background:var(--surface-2);border-radius:6px;margin-bottom:0.35rem;font-size:0.7rem}
        .variant-row .num{background:var(--primary);color:#fff;width:18px;height:18px;border-radius:4px;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:0.6rem}
        .variant-row .url{flex:1;font-family:'Monaco',monospace;color:var(--text-muted);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
        .variant-row button{background:transparent;border:1px solid var(--border);color:var(--text-muted);padding:0.2rem 0.4rem;border-radius:4px}

        .overlay{position:fixed;inset:0;background:rgba(0,0,0,0.6);z-index:150;display:none}
        .overlay.show{display:block}
        .history-panel{position:fixed;top:0;right:-380px;width:380px;max-width:100%;height:100vh;background:var(--surface);border-left:1px solid var(--border);z-index:200;transition:right 0.25s;display:flex;flex-direction:column}
        .history-panel.open{right:0}
        .history-header{padding:1rem;border-bottom:1px solid var(--border);display:flex;align-items:center;justify-content:space-between}
        .history-actions{padding:0.6rem 1rem;border-bottom:1px solid var(--border)}
        .history-list{flex:1;overflow-y:auto;padding:0.75rem}
        .h-item{background:var(--surface-2);border:1px solid var(--border);border-radius:8px;padding:0.75rem;margin-bottom:0.6rem}
        .h-top{display:flex;align-items:center;gap:0.6rem;margin-bottom:0.4rem}
        .h-info{flex:1;min-width:0}
        .h-name{font-weight:600;font-size:0.8rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
        .h-meta{font-size:0.6rem;color:var(--text-muted)}
        .h-url{font-family:'Monaco',monospace;font-size:0.55rem;color:var(--text-muted);background:var(--surface);padding:0.35rem;border-radius:4px;margin-bottom:0.5rem;word-break:break-all;max-height:40px;overflow:hidden}
        .h-btns{display:flex;gap:0.35rem}
        .h-btns button{flex:1;background:transparent;border:1px solid var(--border);color:var(--text-muted);padding:0.35rem;border-radius:4px;font-size:0.6rem;cursor:pointer;transition:all 0.15s}
        .h-btns button:hover{background:var(--surface);color:var(--text);border-color:var(--text-muted)}
        .h-btns button.del-btn:hover{background:var(--danger);color:#fff;border-color:var(--danger)}
        
        .close-btn{background:var(--surface-2);border:1px solid var(--border);color:var(--text-muted);width:32px;height:32px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:1.25rem;cursor:pointer;transition:all 0.2s}
        .close-btn:hover{background:var(--danger);color:#fff;border-color:var(--danger);transform:rotate(90deg)}
    </style>
</head>
<body>
<div class="header">
    <h1>🔗 Link Generator</h1>
    <button class="header-btn" onclick="toggleHistory()">📜 <span class="history-count" id="historyCount">0</span></button>
</div>

<div class="main">
    <div class="card open success-card" id="outputCard" style="display:none">
        <div class="card-header" onclick="toggleCard('outputCard')">
            <div class="card-title" style="color:var(--success)">✅ Generated Link</div>
            <span class="toggle-icon">▼</span>
        </div>
        <div class="card-body">
            <div class="output-box">
                <button class="copy-btn" onclick="copyMain()">📋 Copy</button>
                <span id="mainLink"></span>
            </div>
            <div class="output-actions">
                <button class="btn btn-outline btn-sm" onclick="openPreview()">👁 Preview</button>
            </div>
            <div style="margin-top:1rem; font-size:0.65rem; color:var(--text-muted)">All variants:</div>
            <div id="allLinks" style="margin-top:0.5rem"></div>
        </div>
    </div>

    <!-- Content Details -->
    <div class="card open" id="contentCard">
        <div class="card-header" onclick="toggleCard('contentCard')">
            <div class="card-title">📝 Content Details</div>
            <span class="toggle-icon">▼</span>
        </div>
        <div class="card-body">
            <div class="form-grid single">
                <div class="form-group"><label>Token / ID</label><input type="text" id="token" placeholder="abc123" oninput="onInput()"></div>
                <div class="form-group"><label>Display Name</label><input type="text" id="name" placeholder="My File.mp4" oninput="onInput()"></div>
                <div class="form-group">
                    <label>Content Type</label>
                    <select id="type" onchange="onInput()">
                        <option value="file">📄 File</option><option value="folder">📁 Folder</option><option value="video">🎬 Video</option>
                        <option value="image">🖼️ Image</option><option value="audio">🎵 Audio</option><option value="document">📑 Document</option>
                        <option value="pdf">📕 PDF</option><option value="zip">📦 Archive</option><option value="apk">📲 APK</option>
                    </select>
                </div>
                <div class="form-group"><label>File Size</label><input type="text" id="size" placeholder="e.g. 1.2 GB" oninput="onInput()"></div>
                <div class="form-group"><label>Shared by</label><input type="text" id="by" placeholder="Your Name" oninput="onInput()"></div>
                <div class="form-group"><label>Preview Image</label><input type="url" id="preview" placeholder="https://..." oninput="onInput()"></div>
            </div>
        </div>
    </div>

    <!-- Landing Page Selection -->
    <div class="card open" id="landingCard">
        <div class="card-header" onclick="toggleCard('landingCard')">
            <div class="card-title">🎨 Landing Page</div>
            <span class="toggle-icon">▼</span>
        </div>
        <div class="card-body">
            <div class="landing-list">
                <div class="landing-item active" data-landing="1" onclick="selectLanding(1)"><div class="num">1</div><div class="name">Indigo</div><button class="preview-btn" onclick="event.stopPropagation();previewLanding(1)">👁</button></div>
                <div class="landing-item" data-landing="2" onclick="selectLanding(2)"><div class="num">2</div><div class="name">Dark Card</div><button class="preview-btn" onclick="event.stopPropagation();previewLanding(2)">👁</button></div>
                <div class="landing-item" data-landing="3" onclick="selectLanding(3)"><div class="num">3</div><div class="name">Chat</div><button class="preview-btn" onclick="event.stopPropagation();previewLanding(3)">👁</button></div>
                <div class="landing-item" data-landing="4" onclick="selectLanding(4)"><div class="num">4</div><div class="name">Drive</div><button class="preview-btn" onclick="event.stopPropagation();previewLanding(4)">👁</button></div>
                <div class="landing-item" data-landing="5" onclick="selectLanding(5)"><div class="num">5</div><div class="name">MEGA</div><button class="preview-btn" onclick="event.stopPropagation();previewLanding(5)">👁</button></div>
                <div class="landing-item" data-landing="6" onclick="selectLanding(6)"><div class="num">6</div><div class="name">Dropbox</div><button class="preview-btn" onclick="event.stopPropagation();previewLanding(6)">👁</button></div>
            </div>
        </div>
    </div>

    <!-- Advanced -->
    <div class="card" id="advancedCard">
        <div class="card-header" onclick="toggleCard('advancedCard')">
            <div class="card-title">⚙️ Stats & Proof</div>
            <span class="toggle-icon">▼</span>
        </div>
        <div class="card-body">
            <div class="form-grid single">
                <div class="form-group"><label>Downloads</label><input type="text" id="downloads" value="1M+" oninput="onInput()"></div>
                <div class="form-group"><label>Rating</label><input type="text" id="rating" value="4.5★" oninput="onInput()"></div>
                <div class="form-group"><label>Views</label><input type="number" id="views" placeholder="Random" oninput="onInput()"></div>
                <div class="form-group"><label>Time Ago (min)</label><input type="number" id="time" placeholder="Random" oninput="onInput()"></div>
                <div class="form-group"><label>Custom Badge</label><input type="text" id="badge" placeholder="HOT, NEW, etc" oninput="onInput()"></div>
            </div>
        </div>
    </div>
    <div style="margin-top:0.5rem">
        <button class="btn btn-outline btn-full" onclick="resetForm()">🔄 Reset Form</button>
        <button class="btn btn-outline btn-full" onclick="useSample()" style="margin-top:0.5rem; border-color:var(--primary); color:var(--primary)">✨ Use Sample Data</button>
    </div>
</div>

<div class="sticky-actions">
    <button class="btn btn-primary btn-full" onclick="generateLinks()">🚀 Generate Link Now</button>
</div>

<div class="overlay" id="overlay" onclick="toggleHistory()"></div>
<div class="history-panel" id="historyPanel">
    <div class="history-header"><h2>📜 History</h2><button class="close-btn" onclick="toggleHistory()">×</button></div>
    <div class="history-actions"><button class="btn btn-outline btn-sm" style="width:100%" onclick="clearHistory()">🗑️ Clear All</button></div>
    <div class="history-list" id="historyList"></div>
</div>

<script>
let selectedLanding = 1;
let isGenerated = false;
const baseUrl = window.location.origin;
const FORM_KEY = 'privatefiles_form_v2';
const HISTORY_KEY = 'privatefiles_history';

document.addEventListener('DOMContentLoaded', () => { loadForm(); renderHistory(); updateHistoryCount(); });

function toggleCard(id) { document.getElementById(id).classList.toggle('open'); }
function selectLanding(n) { selectedLanding = n; document.querySelectorAll('.landing-item').forEach(c => c.classList.toggle('active', c.dataset.landing == n)); onInput(); }
function onInput() { 
    handleTokenInput();
    saveForm(); 
    if (isGenerated) generateLinks(); 
}

function handleTokenInput() {
    const el = document.getElementById('token');
    let val = el.value.trim();
    if (val.startsWith('http')) {
        try {
            const url = new URL(val);
            const pathParts = url.pathname.split('/').filter(p => p);
            if (pathParts.length > 0) {
                el.value = pathParts[pathParts.length - 1];
            }
        } catch (e) {}
    }
}

function useSample() {
    document.getElementById('token').value = 'mh-s1-full';
    document.getElementById('name').value = 'Money_heist_season1_full.zip';
    document.getElementById('type').value = 'zip';
    document.getElementById('size').value = '1.8 GB';
    document.getElementById('by').value = 'Professor';
    document.getElementById('preview').value = 'https://image.tmdb.org/t/p/w500/reEMJA1uzscCbkpeRJeTT2bjqUp.jpg';
    document.getElementById('downloads').value = '2.5M+';
    document.getElementById('rating').value = '4.9★';
    document.getElementById('views').value = '8420';
    document.getElementById('time').value = '12';
    document.getElementById('badge').value = 'TRENDING';
    selectLanding(5); // MEGA style for zip sample
    generateLinks();
}

function saveForm() {
    const s = {
        token: document.getElementById('token').value, name: document.getElementById('name').value, size: document.getElementById('size').value,
        by: document.getElementById('by').value,
        type: document.getElementById('type').value, preview: document.getElementById('preview').value, downloads: document.getElementById('downloads').value,
        rating: document.getElementById('rating').value, views: document.getElementById('views').value, time: document.getElementById('time').value,
        badge: document.getElementById('badge').value, landing: selectedLanding
    };
    localStorage.setItem(FORM_KEY, JSON.stringify(s));
}

function loadForm() {
    const d = localStorage.getItem(FORM_KEY); if (!d) return;
    const s = JSON.parse(d);
    for (let k in s) { const el = document.getElementById(k); if(el) el.value = s[k]; }
    if (s.landing) selectLanding(s.landing);
}

function getIcon(t) { return {file:'📄',folder:'📁',video:'🎬',image:'🖼️',audio:'🎵',document:'📑',pdf:'📕',zip:'📦',apk:'📲'}[t] || '📄'; }

function buildUrl(l) {
    const p = {
        token: document.getElementById('token').value.trim() || 'demo',
        name: document.getElementById('name').value.trim(),
        size: document.getElementById('size').value.trim(),
        by: document.getElementById('by').value.trim(),
        type: document.getElementById('type').value,
        preview: document.getElementById('preview').value.trim(),
        downloads: document.getElementById('downloads').value.trim(),
        rating: document.getElementById('rating').value.trim(),
        views: document.getElementById('views').value.trim(),
        time: document.getElementById('time').value.trim(),
        badge: document.getElementById('badge').value.trim(),
        landing: l
    };
    let url = `${baseUrl}/${p.token}?`;
    const params = [];
    if (p.name) params.push(`name=${encodeURIComponent(p.name)}`);
    if (p.size) params.push(`size=${encodeURIComponent(p.size)}`);
    if (p.by) params.push(`by=${encodeURIComponent(p.by)}`);
    params.push(`type=${p.type}`);
    if (p.preview) params.push(`preview=${encodeURIComponent(p.preview)}`);
    if (p.downloads && p.downloads !== '1M+') params.push(`downloads=${encodeURIComponent(p.downloads)}`);
    if (p.rating && p.rating !== '4.5★') params.push(`rating=${encodeURIComponent(p.rating)}`);
    if (p.views) params.push(`views=${p.views}`);
    if (p.time) params.push(`time=${p.time}`);
    if (p.badge) params.push(`badge=${encodeURIComponent(p.badge)}`);
    params.push(`landing=${l}`);
    return url + params.join('&');
}

function generateLinks() {
    isGenerated = true;
    const card = document.getElementById('outputCard');
    card.style.display = 'block';
    card.classList.add('open');
    document.getElementById('mainLink').textContent = buildUrl(selectedLanding);
    let html = '';
    for (let i = 1; i <= 6; i++) {
        const u = buildUrl(i);
        html += `<div class="variant-row"><div class="num">${i}</div><div class="url">${u}</div><button onclick="copyText('${u.replace(/'/g,"\\'")}')">📋</button><button onclick="window.open('${u}','_blank')">👁</button></div>`;
    }
    document.getElementById('allLinks').innerHTML = html;
    addHistory({
        token: document.getElementById('token').value.trim() || 'demo',
        name: document.getElementById('name').value.trim() || 'Shared Content',
        type: document.getElementById('type').value,
        landing: selectedLanding,
        url: buildUrl(selectedLanding),
        time: Date.now()
    });
}

function previewLanding(l) { window.open(buildUrl(l), '_blank'); }
function openPreview() { window.open(buildUrl(selectedLanding), '_blank'); }
function copyMain() { copyText(document.getElementById('mainLink').textContent); }

function copyText(t) {
    navigator.clipboard.writeText(t).then(() => {
        const ev = event.target; const old = ev.textContent; ev.textContent = '✓';
        setTimeout(() => ev.textContent = old, 1000);
    }).catch(() => prompt('Copy:', t));
}

function resetForm() {
    ['token','name','size','by','preview','views','time','badge'].forEach(id => document.getElementById(id).value = '');
    document.getElementById('type').value = 'file';
    document.getElementById('downloads').value = '1M+';
    document.getElementById('rating').value = '4.5★';
    document.getElementById('outputCard').style.display = 'none';
    isGenerated = false; selectLanding(1); localStorage.removeItem(FORM_KEY);
}

function addHistory(item) {
    let h = getHistory().filter(x => x.token !== item.token);
    h.unshift(item); localStorage.setItem(HISTORY_KEY, JSON.stringify(h.slice(0, 50)));
    renderHistory(); updateHistoryCount();
}
function delHistory(token) { localStorage.setItem(HISTORY_KEY, JSON.stringify(getHistory().filter(x => x.token !== token))); renderHistory(); updateHistoryCount(); }
function clearHistory() { if (confirm('Clear all?')) { localStorage.removeItem(HISTORY_KEY); renderHistory(); updateHistoryCount(); } }
function getHistory() { return JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]'); }
function updateHistoryCount() { document.getElementById('historyCount').textContent = getHistory().length; }
function toggleHistory() { document.getElementById('historyPanel').classList.toggle('open'); document.getElementById('overlay').classList.toggle('show'); }

function editHistory(token) {
    const item = getHistory().find(x => x.token === token); if (!item) return;
    const url = new URL(item.url);
    const params = ['name','type','size','by','preview','downloads','rating','views','time','badge'];
    params.forEach(p => { const val = url.searchParams.get(p); if(val) document.getElementById(p).value = val; });
    document.getElementById('token').value = item.token;
    selectLanding(parseInt(url.searchParams.get('landing')) || 1);
    saveForm(); toggleHistory(); generateLinks(); window.scrollTo({top:0,behavior:'smooth'});
}

function renderHistory() {
    const h = getHistory(); const el = document.getElementById('historyList');
    if (!h.length) { el.innerHTML = '<div class="empty-msg">No history yet</div>'; return; }
    el.innerHTML = h.map(i => `
        <div class="h-item">
            <div class="h-top">
                <span class="h-icon">${getIcon(i.type)}</span>
                <div class="h-info"><div class="h-name">${i.name}</div><div class="h-meta">${i.type} • ${timeAgo(i.time)}</div></div>
                <div class="h-landing">${i.landing}</div>
            </div>
            <div class="h-url">${i.url}</div>
            <div class="h-btns">
                <button onclick="copyText('${i.url.replace(/'/g,"\\'")}')" title="Copy">📋</button>
                <button onclick="window.open('${i.url}','_blank')" title="Preview">👁</button>
                <button onclick="editHistory('${i.token}')" title="Edit">✏️</button>
                <button class="del-btn" onclick="delHistory('${i.token}')" title="Delete">×</button>
            </div>
        </div>
    `).join('');
}
function timeAgo(ts) { const m = Math.floor((Date.now() - ts) / 60000); if (m < 1) return 'Now'; if (m < 60) return m + 'm'; const h = Math.floor(m / 60); return h < 24 ? h + 'h' : Math.floor(h / 24) + 'd'; }
</script>
</body>
</html>
