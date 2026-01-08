<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo $name; ?> | Private Files</title>
    <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>📦</text></svg>">
    <meta property="og:title" content="🔒 <?php echo $name; ?>">
    <meta property="og:description" content="Shared <?php echo $typeInfo['label']; ?> - Open with Private Files">
    <?php if ($previewUrl): ?><meta property="og:image" content="<?php echo $previewUrl; ?>"><?php endif; ?>
    <meta name="theme-color" content="#059669">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root{--primary:#10b981;--primary-dark:#059669;--bg:#0d1117;--surface:#161b22;--border:#30363d;--text:#f0f6fc;--text-muted:#8b949e;--warning:#f59e0b}
        *{margin:0;padding:0;box-sizing:border-box}
        body{font-family:'Inter',sans-serif;background: radial-gradient(circle at 0% 0%,#161b22 0%,#0d1117 100%);min-height:100vh;display:flex;flex-direction:column;color:var(--text)}
        .header{background:var(--surface);border-bottom:1px solid var(--border);padding:1rem 1.5rem;display:flex;align-items:center;justify-content:space-between;width:100%;position:sticky;top:0;z-index:100}
        .header .logo{font-weight:700;font-size:1.1rem;color:var(--primary)}
        .header .user-info{font-size:0.8rem;color:var(--text-muted)}
        .main{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:2rem 1.5rem;width:100%}
        .trust-bar{display:flex;gap:1.5rem;margin-bottom:1.5rem;font-size:.7rem;color:var(--text-muted)}
        .trust-bar span{display:flex;align-items:center;gap:.3rem}
        .card{width:100%;max-width:420px;background:var(--surface);border:1px solid var(--border);border-radius:16px;overflow:hidden}
        .card-header{background:linear-gradient(135deg,var(--primary),var(--primary-dark));padding:2rem 1.5rem;text-align:center;position:relative}
        .card-header.has-img{padding:0;min-height:200px}
        .card-header .preview-img{width:100%;height:200px;object-fit:cover;opacity:.9}
        .card-header .overlay{position:absolute;bottom:0;left:0;right:0;background:linear-gradient(transparent,rgba(0,0,0,.8));padding:1.5rem 1rem 1rem;text-align:center}
        .hot-badge{position:absolute;top:.75rem;left:.75rem;background:var(--warning);color:#fff;font-size:.6rem;font-weight:700;padding:.3rem .6rem;border-radius:4px}
        .file-icon{width:72px;height:72px;background:rgba(255,255,255,.2);border-radius:18px;display:flex;align-items:center;justify-content:center;margin:0 auto 1rem;font-size:2.25rem}
        .card-header h1{font-size:1.25rem;font-weight:700;margin-bottom:.25rem}
        .card-header .type{font-size:.85rem;opacity:.85}
        .card-body{padding:1.25rem}
        .stats-row{display:flex;gap:1rem;padding:.75rem 0;border-bottom:1px solid var(--border)}
        .stat{flex:1;text-align:center}
        .stat .num{font-size:1rem;font-weight:700}
        .stat .lbl{font-size:.6rem;color:var(--text-muted);text-transform:uppercase}
        .info-row{display:flex;align-items:center;gap:.75rem;padding:.7rem 0;border-bottom:1px solid var(--border);font-size:.85rem}
        .info-row:last-child{border-bottom:none}
        .info-row .label{color:var(--text-muted);flex:1}
        .info-row .value{font-weight:500;font-size:.8rem}
        .info-row .success{color:var(--primary)}
        .card-footer{padding:1rem 1.25rem;border-top:1px solid var(--border)}
        .btn{display:flex;align-items:center;justify-content:center;gap:.5rem;width:100%;padding:1rem;border-radius:10px;font-weight:600;font-size:.95rem;cursor:pointer;border:none;transition:.2s}
        .btn-primary{background:linear-gradient(135deg,var(--primary),var(--primary-dark));color:#fff;box-shadow:0 4px 15px rgba(16,185,129,.3)}
        .btn-primary:hover{transform:translateY(-1px)}
        .cta-hint{text-align:center;margin-top:.6rem;font-size:.7rem;color:var(--text-muted)}
        .badge-secure{display:inline-flex;align-items:center;gap:.4rem;font-size:.75rem;color:var(--primary);margin-top:1rem}
        .modal-overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,.9);z-index:1000;align-items:center;justify-content:center;padding:1.5rem}
        .modal-overlay.show{display:flex}
        .modal{background:var(--surface);border:1px solid var(--border);border-radius:20px;width:100%;max-width:340px;padding:2rem 1.5rem;text-align:center}
        .modal .emoji{font-size:3rem;margin-bottom:1rem}
        .modal h2{font-size:1.2rem;margin-bottom:.5rem}
        .modal p{color:var(--text-muted);font-size:.85rem;margin-bottom:1.25rem}
        .qr-wrapper{background:#fff;padding:.85rem;border-radius:14px;display:inline-block;margin-bottom:.75rem}
        .qr-wrapper img{display:block;width:140px;height:140px}
        .scan-text{color:var(--text-muted);font-size:.75rem;margin-bottom:1.25rem}
        .modal-btns{display:flex;flex-direction:column;gap:.5rem}
        .btn-ghost{background:transparent;color:var(--text-muted);border:1px solid var(--border)}
        .btn-link{background:transparent;color:var(--text-muted);font-size:.8rem;padding:.6rem}
        .page-footer{background:var(--surface);border-top:1px solid var(--border);padding:1rem 1rem 3rem;text-align:center}
        .footer-disclaimer{color:var(--text-muted);font-size:.65rem;margin-bottom:.6rem}
        .footer-links{display:flex;flex-wrap:wrap;justify-content:center;gap:.4rem 1rem;font-size:.7rem}
        .footer-links a{color:var(--text-muted);text-decoration:none}
        .footer-links a:hover{color:var(--primary)}
        @media(min-width:768px){.card{max-width:480px}.card-header h1{font-size:1.4rem}.card-header.has-img{min-height:240px}.card-header .preview-img{height:240px}}
    </style>
</head>
<body>
    <div class="header">
        <div class="logo">Private Files</div>
        <div class="user-info">Shared by <?php echo $by; ?></div>
    </div>
<div class="main">
    <div class="trust-bar"><span>🔒 Encrypted</span><span>✅ Verified</span><span>📱 Android</span></div>
    <div class="card">
        <div class="card-header <?php echo $previewUrl?'has-img':''; ?>">
            <div class="hot-badge">🔥 POPULAR</div>
            <?php if($previewUrl): ?>
                <img src="<?php echo $previewUrl; ?>" class="preview-img" onerror="this.style.display='none';this.parentElement.classList.remove('has-img');">
                <div class="overlay"><h1><?php echo $name; ?></h1><span class="type">Shared by <?php echo $by; ?></span></div>
            <?php else: ?>
                <div class="file-icon"><?php echo $typeInfo['icon']; ?></div>
                <h1><?php echo $name; ?></h1><span class="type">Shared by <?php echo $by; ?></span>
            <?php endif; ?>
        </div>
        <div class="card-body">
            <div class="stats-row">
                <div class="stat"><div class="num"><?php echo $views; ?></div><div class="lbl">Views</div></div>
                <div class="stat"><div class="num"><?php echo $opens; ?></div><div class="lbl">Opens</div></div>
                <div class="stat"><div class="num"><?php echo $timeAgo; ?>m</div><div class="lbl">Ago</div></div>
            </div>
            <div class="info-row"><span>🔒</span><span class="label">Privacy</span><span class="value success">Protected</span></div>
            <div class="info-row"><span>📦</span><span class="label">Size</span><span class="value"><?php echo $size; ?></span></div>
            <div class="info-row"><span>✅</span><span class="label">Status</span><span class="value success">Ready</span></div>
        </div>
        <div class="card-footer">
            <button onclick="handleOpen()" class="btn btn-primary">Open <?php echo $typeInfo['label']; ?> Now →</button>
            <p class="cta-hint">Free download • Opens instantly</p>
        </div>
    </div>
    <p class="badge-secure">🛡️ End-to-end encrypted</p>
</div>
<footer class="page-footer">
    <p class="footer-disclaimer">⚠️ Content info from URL params. Actual content may differ.</p>
    <div class="footer-links">
        <a href="<?php echo $reportUrl; ?>">Report</a>
        <a href="<?php echo $dmcaUrl; ?>">DMCA</a>
        <a href="<?php echo $reportUrl; ?>">Remove</a>
        <a href="/privacy-policy.html">Privacy</a>
        <a href="/terms-of-usage.html">Terms</a>
    </div>
</footer>
<div class="modal-overlay" id="modal">
    <div class="modal">
        <div class="emoji">📱</div>
        <h2>Android Only</h2>
        <p>This <?php echo strtolower($typeInfo['label']); ?> requires <strong>Private Files on Android</strong>.</p>
        <div class="qr-wrapper"><img src="https://api.qrserver.com/v1/create-qr-code/?size=280x280&margin=8&data=<?php echo urlencode($currentUrl); ?>" alt="QR"></div>
        <p class="scan-text">Scan with your Android phone</p>
        <div class="modal-btns">
            <button onclick="copyLink()" class="btn btn-ghost">📋 Copy Link</button>
            <button onclick="closeModal()" class="btn btn-link">Close</button>
        </div>
    </div>
</div>
<script>
const intentUrl='<?php echo addslashes($intentUrl); ?>',shareLink='<?php echo addslashes($appDeepLink); ?>',token='<?php echo addslashes($token); ?>',uniqueId='<?php echo addslashes($uniqueId); ?>',landing='<?php echo addslashes($landing); ?>',isAndroid=/Android/i.test(navigator.userAgent);
const storageKey=`privatefiles_stats_${token}`;
const initial={views:<?php echo $views; ?>,opens:<?php echo $opens; ?>,time:<?php echo $timeAgo; ?>,lastVisit:Date.now()};

function getStats(){
    const s=localStorage.getItem(storageKey);
    if(!s){saveStats(initial);return initial;}
    let st=JSON.parse(s);
    const mins=Math.floor((Date.now()-st.lastVisit)/60000);
    st.views+=1;
    if(Math.random()>.6)st.opens+=Math.floor(Math.random()*3)+1;
    st.time=Math.min(st.time+mins,999);
    st.lastVisit=Date.now();
    saveStats(st);
    return st;
}
function saveStats(s){localStorage.setItem(storageKey,JSON.stringify(s));}

function updateDisplay(){
    const s=getStats();
    const nums=document.querySelectorAll('.stat .num');
    if(nums[0])nums[0].textContent=s.views;
    if(nums[1])nums[1].textContent=s.opens;
    if(nums[2])nums[2].textContent=(s.time<60?s.time+'m':Math.floor(s.time/60)+'h');
}
updateDisplay();

function handleOpen(){
    const s=getStats();s.opens++;saveStats(s);
    if(uniqueId) fetch(`track_event.php?type=click&uniqueid=${uniqueId}&landing=${landing}`).catch(console.error);
    isAndroid?window.location.href=intentUrl:document.getElementById('modal').classList.add('show');
}
function closeModal(){document.getElementById('modal').classList.remove('show');}
function copyLink(){navigator.clipboard.writeText(shareLink).then(()=>alert('Link copied!')).catch(()=>prompt('Copy:',shareLink));}
document.getElementById('modal').onclick=e=>{if(e.target.id==='modal')closeModal();};
</script>
</body>
</html>
