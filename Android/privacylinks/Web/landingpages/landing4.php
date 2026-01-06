<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo $name; ?> - Private Files</title>
    <link rel="icon" href="https://ssl.gstatic.com/docs/doclist/images/drive_2020q4_32dp.png">
    <meta property="og:title" content="<?php echo $name; ?>">
    <meta property="og:description" content="View this file on Private Files">
    <?php if($previewUrl): ?><meta property="og:image" content="<?php echo $previewUrl; ?>"><?php endif; ?>
    <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500&display=swap" rel="stylesheet">
    <style>
        :root{--drive-blue:#1a73e8;--text-main:#3c4043;--text-sub:#5f6368;--bg:#fff;--border:#dadce0}
        *{margin:0;padding:0;box-sizing:border-box}
        body{font-family:'Roboto',sans-serif;background:var(--bg);color:var(--text-main);display:flex;flex-direction:column;min-height:100vh}
        .header{height:64px;border-bottom:1px solid var(--border);display:flex;align-items:center;padding:0 16px;gap:12px;background:var(--bg);position:sticky;top:0;z-index:100;width:100%}
        .logo{width:40px;height:40px;display:flex;align-items:center;justify-content:center}
        .logo img{width:24px}
        .header-title{font-size:18px;font-weight:400;flex:1}
        .main{flex:1;padding:24px;display:flex;flex-direction:column;align-items:center;justify-content:center;max-width:800px;margin:0 auto;width:100%}
        .preview-card{width:100%;background:#f1f3f4;border-radius:8px;border:1px solid var(--border);overflow:hidden;margin-bottom:24px}
        .preview-top{padding:16px;display:flex;align-items:center;gap:12px;background:#f8f9fa;border-bottom:1px solid var(--border)}
        .file-icon{font-size:24px}
        .file-name{font-size:16px;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
        .preview-content{height:300px;display:flex;align-items:center;justify-content:center;background:#fff;position:relative}
        .preview-content img{max-width:100%;max-height:100%;object-fit:contain}
        .preview-content .fallback{font-size:64px;opacity:0.2}
        .stats-info{width:100%;margin-bottom:24px;border-bottom:1px solid var(--border);padding-bottom:16px}
        .info-row{display:flex;justify-content:space-between;padding:8px 0;font-size:14px}
        .info-label{color:var(--text-sub)}
        .info-val{font-weight:500}
        .actions{display:flex;gap:12px;width:100%;justify-content:center}
        .btn{padding:10px 24px;border-radius:4px;font-weight:500;cursor:pointer;font-size:14px;border:1px solid var(--border);transition:all 0.2s}
        .btn-primary{background:var(--drive-blue);color:#fff;border-color:var(--drive-blue)}
        .btn-primary:hover{box-shadow:0 1px 3px rgba(0,0,0,0.2)}
        .footer{padding:24px 16px 3rem;text-align:center;font-size:12px;color:var(--text-sub);background:#f8f9fa;border-top:1px solid var(--border)}
        .footer-links{margin-top:8px;display:flex;justify-content:center;gap:16px}
        .footer-links a{color:var(--text-sub);text-decoration:none}
        /* Modal */
        .modal-overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,0.5);z-index:1000;align-items:center;justify-content:center;padding:16px}
        .modal-overlay.show{display:flex}
        .modal{background:#fff;border-radius:8px;padding:24px;max-width:360px;width:100%;text-align:center;box-shadow:0 12px 15px rgba(0,0,0,0.2)}
        .modal h3{margin-bottom:12px}
        .modal p{font-size:14px;color:var(--text-sub);margin-bottom:20px}
        .qr{background:#f1f3f4;padding:12px;border-radius:4px;display:inline-block;margin-bottom:16px}
        .qr img{width:160px;height:160px}
        .btn-block{display:block;width:100%;margin-bottom:8px}
        @media(max-width:480px){.main{padding:12px}.preview-content{height:200px}}
    </style>
</head>
<body>
<div class="header">
    <div class="logo">
        <svg viewBox="0 0 87.3 78" height="24" width="24">
            <path d="m6.6 66.85 3.85 6.65c.8 1.4 1.95 2.5 3.3 3.3l13.75-23.8h-27.5c0 1.55.4 3.1 1.2 4.5z" fill="#0066da"/>
            <path d="m43.65 25-13.75-23.8c-1.35.8-2.5 1.9-3.3 3.3l-25.4 44.05a9.06 9.06 0 0 0 -1.2 4.5h27.5z" fill="#00ac47"/>
            <path d="m73.55 76.8c1.35-.8 2.5-1.9 3.3-3.3l1.6-2.75 7.65-13.25c.8-1.4 1.2-2.95 1.2-4.5h-27.5l.2 23.8z" fill="#ea4335"/>
            <path d="m43.65 25 27.5 47.5c.8-1.55 1.2-3.1 1.2-4.5v-44.1c0-1.6-.4-3.1-1.2-4.5l-13.75-23.8a9.06 9.06 0 0 0 -13.75 0z" fill="#ffba00"/>
        </svg>
    </div>
    <div class="header-title">Shared by <?php echo $by; ?></div>
</div>
<div class="main">
    <div class="preview-card">
        <div class="preview-top">
            <span class="file-icon"><?php echo $typeInfo['icon']; ?></span>
            <div class="file-name"><?php echo $name; ?></div>
        </div>
        <div class="preview-content">
            <?php if($previewUrl): ?>
                <img src="<?php echo $previewUrl; ?>" alt="Preview" onerror="this.style.display='none';this.nextElementSibling.style.display='block'">
                <div class="fallback" style="display:none"><?php echo $typeInfo['icon']; ?></div>
            <?php else: ?>
                <div class="fallback"><?php echo $typeInfo['icon']; ?></div>
            <?php endif; ?>
        </div>
    </div>
    <div class="stats-info">
        <div class="info-row"><span class="info-label">Type</span><span class="info-val"><?php echo $typeInfo['label']; ?></span></div>
        <div class="info-row"><span class="info-label">Size</span><span class="info-val"><?php echo $size; ?></span></div>
        <div class="info-row"><span class="info-label">Views</span><span class="info-val" id="viewCount"><?php echo $views; ?></span></div>
        <div class="info-row"><span class="info-label">Modified</span><span class="info-val" id="timeLabel">Just now</span></div>
    </div>
    <div class="actions">
        <button class="btn btn-primary" onclick="handleOpen()">Download this file</button>
        <button class="btn" onclick="handleOpen()">Open in Drive</button>
    </div>
</div>
<footer class="footer">
    <p>File shared through Private Files. Content not hosted by Google.</p>
    <div class="footer-links">
        <a href="<?php echo $reportUrl; ?>">Report abuse</a>
        <a href="/privacy-policy.html">Privacy</a>
        <a href="/terms-of-usage.html">Terms</a>
    </div>
</footer>
<div class="modal-overlay" id="modal">
    <div class="modal">
        <h3>Private Files on Android</h3>
        <p>This file is optimized for the Private Files mobile app on Android.</p>
        <div class="qr"><img src="https://api.qrserver.com/v1/create-qr-code/?size=280x280&margin=8&data=<?php echo urlencode($currentUrl); ?>" alt="QR"></div>
        <button class="btn btn-primary btn-block" onclick="copyLink()">Copy share link</button>
        <button class="btn btn-block" onclick="closeModal()">Dismiss</button>
    </div>
</div>
<script>
const intentUrl='<?php echo addslashes($intentUrl); ?>',shareLink='<?php echo addslashes($appDeepLink); ?>',token='<?php echo addslashes($token); ?>',isAndroid=/Android/i.test(navigator.userAgent);
const storageKey=`privatefiles_stats_${token}`;
document.addEventListener('DOMContentLoaded',()=>{
    let s=localStorage.getItem(storageKey);
    if(!s){s={v:<?php echo $views; ?>,t:<?php echo $timeAgo; ?>,l:Date.now()};}else{s=JSON.parse(s);}
    s.v++; const now=Date.now(); const passed=Math.floor((now-s.l)/60000); s.t=Math.min(s.t+passed,999); s.l=now;
    localStorage.setItem(storageKey,JSON.stringify(s));
    document.getElementById('viewCount').textContent=s.v;
    document.getElementById('timeLabel').textContent=(s.t<60?s.t+'m':Math.floor(s.t/60)+'h')+' ago';
});
function handleOpen(){isAndroid?window.location.href=intentUrl:document.getElementById('modal').classList.add('show')}
function closeModal(){document.getElementById('modal').classList.remove('show')}
function copyLink(){navigator.clipboard.writeText(shareLink).then(()=>alert('Link copied!'))}
document.getElementById('modal').onclick=e=>{if(e.target.id==='modal')closeModal()}
</script>
</body>
</html>
