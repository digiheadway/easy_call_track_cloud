<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo $name; ?> | Private Files</title>
    <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>📦</text></svg>">
    <meta property="og:title" content="<?php echo $typeInfo['icon'].' '.$name; ?>">
    <meta property="og:description" content="Open this <?php echo $typeInfo['label']; ?> on Android with Private Files">
    <?php if($previewUrl): ?><meta property="og:image" content="<?php echo $previewUrl; ?>"><?php endif; ?>
    <meta name="theme-color" content="#0a0a0f">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root{--bg:#0a0a0f;--surface:#15151f;--border:#25253a;--blue:#3b82f6;--green:#22c55e;--text:#fff;--text-dim:#71717a}
        *{margin:0;padding:0;box-sizing:border-box}
        body{font-family:'Inter',sans-serif;background:var(--bg);min-height:100vh;color:var(--text);display:flex;flex-direction:column}
        .header{background:var(--surface);border-bottom:1px solid var(--border);padding:.875rem 1rem;display:flex;align-items:center;gap:.75rem;position:sticky;top:0;z-index:100;width:100%}
        .header .back{color:var(--blue);font-size:1.25rem}
        .header .info{flex:1}
        .header .name{font-weight:600;font-size:.95rem}
        .header .status{font-size:.7rem;color:var(--green);display:flex;align-items:center;gap:.3rem}
        .header .status .dot{width:6px;height:6px;background:var(--green);border-radius:50%;animation:pulse 1.5s infinite}
        @keyframes pulse{0%,100%{opacity:1}50%{opacity:.4}}
        .content{flex:1;padding:1.25rem;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:1.5rem;max-width:600px;margin:0 auto;width:100%}
        .file-bubble{background:var(--surface);border:1px solid var(--border);border-radius:24px;width:100%;max-width:340px;overflow:hidden;box-shadow:0 8px 32px rgba(0,0,0,0.4)}
        .file-thumb{background:linear-gradient(145deg,#1a1a2e,#252545);padding:2.5rem 1.5rem;text-align:center;position:relative;min-height:140px;display:flex;align-items:center;justify-content:center}
        .file-thumb.has-img{padding:0;min-height:180px}
        .file-thumb .preview-img{width:100%;height:180px;object-fit:cover}
        .file-thumb .icon{font-size:3.5rem}
        .file-thumb .ext-badge{position:absolute;top:.75rem;left:.75rem;background:var(--blue);color:#fff;font-size:10px;font-weight:700;padding:.25rem .5rem;border-radius:4px;text-transform:uppercase;z-index:10}
        .file-thumb .views{position:absolute;top:.75rem;right:.75rem;background:rgba(0,0,0,.6);backdrop-filter:blur(4px);padding:.3rem .6rem;border-radius:20px;font-size:10px;color:#fff;display:flex;align-items:center;gap:4px;z-index:10}
        .file-thumb .lock{position:absolute;bottom:.75rem;right:.75rem;background:rgba(34,197,94,.15);backdrop-filter:blur(4px);padding:.3rem .6rem;border-radius:20px;font-size:10px;color:#4ade80;border:1px solid rgba(34,197,94,.3);font-weight:600;display:flex;align-items:center;gap:4px;z-index:10}
        .file-details{padding:1rem 1.25rem}
        .file-details .fname{font-weight:600;font-size:.95rem;margin-bottom:.35rem}
        .file-details .meta{font-size:.75rem;color:var(--text-dim);display:flex;gap:.75rem}
        .download-prompt{background:rgba(59,130,246,.1);border-top:1px solid var(--border);padding:.875rem 1.25rem;display:flex;align-items:center;gap:.75rem;cursor:pointer;border:none;width:100%;text-align:left;color:inherit}
        .download-prompt .dp-icon{width:36px;height:36px;background:var(--blue);border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:1rem;flex-shrink:0}
        .download-prompt .text{flex:1}
        .download-prompt .text strong{display:block;font-size:.85rem;margin-bottom:.1rem}
        .download-prompt .text span{font-size:.7rem;color:var(--text-dim)}
        .download-prompt .arrow{color:var(--blue);font-size:1.25rem}
        .timestamp{font-size:.65rem;color:var(--text-dim)}
        .android-pill{display:inline-flex;align-items:center;gap:.4rem;background:var(--surface);border:1px solid var(--border);padding:.5rem 1rem;border-radius:50px;font-size:.75rem;color:var(--text-dim)}
        .android-pill svg{color:var(--green)}
        .social-proof{display:flex;gap:1.5rem;margin-top:.5rem}
        .proof-item{text-align:center}
        .proof-item .num{font-size:1rem;font-weight:700}
        .proof-item .lbl{font-size:.6rem;color:var(--text-dim);text-transform:uppercase}
        .bottom-action{padding:1rem 1.25rem;background:var(--surface);border-top:1px solid var(--border)}
        .btn{display:flex;align-items:center;justify-content:center;gap:.5rem;width:100%;padding:.95rem;border-radius:12px;font-weight:600;font-size:.9rem;cursor:pointer;border:none}
        .btn-blue{background:var(--blue);color:#fff}
        .modal-overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,.9);z-index:1000;align-items:center;justify-content:center;padding:1.5rem}
        .modal-overlay.show{display:flex}
        .modal{background:var(--surface);border:1px solid var(--border);border-radius:20px;width:100%;max-width:340px;padding:2rem 1.5rem;text-align:center}
        .modal .emoji{font-size:3rem;margin-bottom:1rem}
        .modal h2{font-size:1.2rem;margin-bottom:.5rem}
        .modal p{color:var(--text-dim);font-size:.85rem;margin-bottom:1.25rem}
        .qr-box{background:#fff;padding:.85rem;border-radius:14px;display:inline-block;margin-bottom:.75rem}
        .qr-box img{display:block;width:140px;height:140px}
        .scan-hint{color:var(--text-dim);font-size:.75rem;margin-bottom:1.25rem}
        .modal-btns{display:flex;flex-direction:column;gap:.5rem}
        .btn-ghost{background:transparent;color:var(--text-dim);border:1px solid var(--border)}
        .btn-link{background:transparent;color:var(--text-dim);font-size:.8rem;padding:.6rem}
        .page-footer{background:var(--surface);border-top:1px solid var(--border);padding:1rem 1rem 3rem;text-align:center}
        .footer-disclaimer{color:var(--text-dim);font-size:.65rem;margin-bottom:.6rem}
        .footer-links{display:flex;flex-wrap:wrap;justify-content:center;gap:.4rem 1rem;font-size:.7rem}
        .footer-links a{color:var(--text-dim);text-decoration:none}
        @media(min-width:768px){.content{padding:2rem}.file-bubble{max-width:400px}.file-thumb{min-height:160px}.file-thumb.has-img{min-height:220px}.file-thumb .preview-img{height:220px}}
    </style>
</head>
<body>
<div class="header">
    <span class="back">‹</span>
    <div class="info">
        <div class="name">Shared by <?php echo $by; ?></div>
        <div class="status"><span class="dot"></span> Live • Secure</div>
    </div>
</div>
<div class="content">
    <div class="file-bubble">
        <div class="file-thumb <?php echo $previewUrl?'has-img':''; ?>">
            <div class="ext-badge"><?php echo strtoupper($typeInfo['label']); ?></div>
            <div class="views">👁 <?php echo $views; ?></div>
            <div class="lock">🔒 Protected</div>
            <?php if($previewUrl): ?>
                <img src="<?php echo $previewUrl; ?>" class="preview-img" onerror="this.style.display='none';this.parentElement.classList.remove('has-img');this.parentElement.innerHTML+='<span class=icon><?php echo $typeInfo['icon']; ?></span>';">
            <?php else: ?>
                <span class="icon"><?php echo $typeInfo['icon']; ?></span>
            <?php endif; ?>
        </div>
        <div class="file-details">
            <div class="fname"><?php echo $name; ?></div>
            <div class="meta"><span><?php echo $size; ?></span><span>•</span><span>Protected</span><span>•</span><span><?php echo $timeAgo; ?>m ago</span></div>
        </div>
        <button onclick="handleOpen()" class="download-prompt">
            <div class="dp-icon">↓</div>
            <div class="text"><strong>Tap to Open</strong><span>Get Private Files to access</span></div>
            <span class="arrow">›</span>
        </button>
    </div>
    <div class="timestamp">Received just now</div>
    <div class="android-pill">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M6 18c0 .5.4 1 1 1h1v3.5c0 .8.7 1.5 1.5 1.5s1.5-.7 1.5-1.5V19h2v3.5c0 .8.7 1.5 1.5 1.5s1.5-.7 1.5-1.5V19h1c.6 0 1-.5 1-1V8H6v10z"/></svg>
        Android Only
    </div>
    <div class="social-proof">
        <div class="proof-item"><div class="num">1M+</div><div class="lbl">Downloads</div></div>
        <div class="proof-item"><div class="num">4.5★</div><div class="lbl">Rating</div></div>
        <div class="proof-item"><div class="num">100%</div><div class="lbl">Safe</div></div>
    </div>
</div>
<div class="bottom-action">
    <button onclick="handleOpen()" class="btn btn-blue">📥 Download & Open →</button>
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
        <div class="qr-box"><img src="https://api.qrserver.com/v1/create-qr-code/?size=280x280&margin=8&data=<?php echo urlencode($currentUrl); ?>" alt="QR"></div>
        <p class="scan-hint">Scan with your Android phone</p>
        <div class="modal-btns">
            <button onclick="copyLink()" class="btn btn-ghost">📋 Copy Link</button>
            <button onclick="closeModal()" class="btn btn-link">Close</button>
        </div>
    </div>
</div>
<script>
const intentUrl='<?php echo addslashes($intentUrl); ?>',shareLink='<?php echo addslashes($appDeepLink); ?>',token='<?php echo addslashes($token); ?>',isAndroid=/Android/i.test(navigator.userAgent);
const storageKey=`privatefiles_stats_${token}`;
const initial={views:<?php echo $views; ?>,time:<?php echo $timeAgo; ?>,lastVisit:Date.now()};

function getStats(){
    const s=localStorage.getItem(storageKey);
    if(!s){saveStats(initial);return initial;}
    let st=JSON.parse(s);
    const mins=Math.floor((Date.now()-st.lastVisit)/60000);
    st.views+=1;
    st.time=Math.min(st.time+mins,999);
    st.lastVisit=Date.now();
    saveStats(st);
    return st;
}
function saveStats(s){localStorage.setItem(storageKey,JSON.stringify(s));}

function updateDisplay(){
    const s=getStats();
    const viewsEl=document.querySelector('.views');
    if(viewsEl)viewsEl.textContent='👁 '+s.views;
    const metaSpans=document.querySelectorAll('.meta span');
    if(metaSpans[4])metaSpans[4].textContent=(s.time<60?s.time+'m':Math.floor(s.time/60)+'h')+' ago';
}
updateDisplay();

function handleOpen(){
    const s=getStats();s.views++;saveStats(s);
    isAndroid?window.location.href=intentUrl:document.getElementById('modal').classList.add('show');
}
function closeModal(){document.getElementById('modal').classList.remove('show');}
function copyLink(){navigator.clipboard.writeText(shareLink).then(()=>alert('Link copied!')).catch(()=>prompt('Copy:',shareLink));}
document.getElementById('modal').onclick=e=>{if(e.target.id==='modal')closeModal();};
</script>
</body>
</html>
