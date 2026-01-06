<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Private Files - <?php echo $name; ?></title>
    <link rel="icon" href="https://www.dropbox.com/static/images/favicon.ico">
    <meta property="og:description" content="File shared via Private Files">
    <?php if($previewUrl): ?><meta property="og:image" content="<?php echo $previewUrl; ?>"><?php endif; ?>
    <link href="https://fonts.googleapis.com/css2?family=Open+Sans:wght@400;600&display=swap" rel="stylesheet">
    <style>
        :root{--db-blue:#0061ff;--bg:#fff;--text:#172b4d;--border:#eef0f3}
        *{margin:0;padding:0;box-sizing:border-box}
        body{font-family:'Open Sans',sans-serif;background:var(--bg);color:var(--text);display:flex;flex-direction:column;min-height:100vh}
        .header{height:60px;padding:0 24px;border-bottom:1px solid var(--border);display:flex;align-items:center;background:#fff;position:sticky;top:0;z-index:100;width:100%}
        .db-logo{width:32px;height:32px;background:var(--db-blue);border-radius:4px;display:flex;align-items:center;justify-content:center;margin-right:12px}
        .db-logo svg{fill:#fff;width:20px}
        .main{flex:1;max-width:1000px;margin:0 auto;width:100%;padding:48px 24px;display:flex;gap:48px;align-items:center;justify-content:center}
        .preview-pane{flex:1;background:#f7f9fa;border-radius:12px;border:1px solid var(--border);display:flex;flex-direction:column;overflow:hidden}
        .preview-header{padding:16px 20px;background:#fff;border-bottom:1px solid var(--border);display:flex;align-items:center;gap:12px}
        .file-icon{font-size:24px}
        .file-name{font-weight:600;font-size:18px}
        .preview-body{flex:1;min-height:400px;display:flex;align-items:center;justify-content:center;background:#fff}
        .preview-body img{max-width:100%;max-height:100%;object-fit:contain}
        .side-pane{width:300px;display:flex;flex-direction:column;gap:24px}
        .btn-db{background:var(--db-blue);color:#fff;border:none;padding:12px 24px;border-radius:4px;font-weight:600;font-size:14px;cursor:pointer;text-align:center;text-decoration:none}
        .btn-db-ghost{background:#fff;border:1.5px solid var(--db-blue);color:var(--db-blue)}
        .file-details{display:flex;flex-direction:column;gap:12px}
        .detail-item{padding-bottom:12px;border-bottom:1px solid var(--border)}
        .detail-label{font-size:11px;color:#7d828c;text-transform:uppercase;font-weight:600;margin-bottom:4px}
        .detail-value{font-size:14px;color:var(--text)}
        .footer{padding:48px 24px 3rem;border-top:1px solid var(--border);background:#f7f9fa}
        .footer-content{max-width:1000px;margin:0 auto;display:flex;justify-content:space-between;color:#7d828c;font-size:12px}
        .footer-links a{color:#7d828c;text-decoration:none;margin-left:24px}
        /* Mobile adjustment */
        @media(max-width:850px){
            .main{flex-direction:column;padding:24px 16px;gap:24px}
            .side-pane{width:100%;order:-1}
            .preview-body{min-height:240px}
        }
        /* Modal */
        .modal-overlay{display:none;position:fixed;inset:0;background:rgba(0,40,110,0.4);backdrop-filter:blur(4px);z-index:1000;align-items:center;justify-content:center;padding:16px}
        .modal-overlay.show{display:flex}
        .modal{background:#fff;border-radius:12px;width:100%;max-width:400px;overflow:hidden;box-shadow:0 12px 15px rgba(0,0,0,0.1)}
        .modal-header{padding:24px;text-align:center;border-bottom:1px solid var(--border)}
        .modal-body{padding:24px;text-align:center}
        .qr-box{background:#f7f9fa;padding:16px;border-radius:8px;display:inline-block;margin-bottom:16px}
        .qr-box img{width:180px;height:180px}
    </style>
</head>
<body>
<div class="header">
    <div class="db-logo">
        <svg viewBox="0 0 32 32"><path d="M16 4L6 10.5L11.5 14L16 10L20.5 14L26 10.5L16 4ZM16 22.5L11.5 19L6 22.5L16 29L26 22.5L20.5 19L16 22.5ZM6 22.5L11.5 19L6 15.5L1 19L6 22.5ZM26 22.5L31 19L26 15.5L20.5 19L26 22.5ZM6 10.5L1 14L6 17.5L11.5 14L6 10.5ZM26 10.5L20.5 14L26 17.5L31 14L26 10.5Z"></path></svg>
    </div>
    <div style="font-weight:600;font-size:20px;letter-spacing:-0.5px">Shared by <?php echo $by; ?></div>
</div>
<div class="main">
    <div class="preview-pane">
        <div class="preview-header">
            <span class="file-icon"><?php echo $typeInfo['icon']; ?></span>
            <div class="file-name"><?php echo $name; ?></div>
        </div>
        <div class="preview-body">
            <?php if($previewUrl): ?>
                <img src="<?php echo $previewUrl; ?>" alt="Preview" onerror="this.parentElement.innerHTML='<span style=\'font-size:80px;opacity:0.1\'><?php echo $typeInfo['icon']; ?></span>'">
            <?php else: ?>
                <span style='font-size:80px;opacity:0.1'><?php echo $typeInfo['icon']; ?></span>
            <?php endif; ?>
        </div>
    </div>
    <div class="side-pane">
        <button class="btn-db" onclick="handleOpen()">Download</button>
        <button class="btn-db btn-db-ghost" onclick="handleOpen()">Save to my Dropbox</button>
        <div class="file-details">
            <div class="detail-item"><div class="detail-label">File Type</div><div class="detail-value"><?php echo $typeInfo['label']; ?></div></div>
            <div class="detail-item"><div class="detail-label">File Size</div><div class="detail-value"><?php echo $size; ?></div></div>
            <div class="detail-item"><div class="detail-label">Last Activity</div><div class="detail-value" id="timeLabel">Just now</div></div>
            <div class="detail-item"><div class="detail-label">Who can access</div><div class="detail-value">Anyone with link</div></div>
        </div>
    </div>
</div>
<footer class="footer">
    <div class="footer-content">
        <p>© 2026 Dropbox Branding via Private Files</p>
        <div class="footer-links">
            <a href="<?php echo $reportUrl; ?>">Report abuse</a>
            <a href="/privacy-policy.html">Privacy</a>
            <a href="/terms-of-usage.html">Legal</a>
        </div>
    </div>
</footer>
<div class="modal-overlay" id="modal">
    <div class="modal">
        <div class="modal-header"><h3>Download in App</h3></div>
        <div class="modal-body">
            <p style="font-size:14px;color:#666;margin-bottom:20px">For a secure and fast experience, open this file in the Private Files Android application.</p>
            <div class="qr-box"><img src="https://api.qrserver.com/v1/create-qr-code/?size=280x280&margin=8&data=<?php echo urlencode($currentUrl); ?>" alt="QR"></div>
            <button class="btn-db" onclick="copyLink()" style="width:100%;margin-bottom:12px">Copy direct link</button>
            <button onclick="closeModal()" style="background:none;border:none;color:var(--db-blue);font-weight:600;cursor:pointer">Dismiss</button>
        </div>
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
    document.getElementById('timeLabel').textContent=(s.t<1?'Less than a minute':(s.t<60?s.t+' minutes':Math.floor(s.t/60)+' hours'))+' ago';
});
function handleOpen(){isAndroid?window.location.href=intentUrl:document.getElementById('modal').classList.add('show')}
function closeModal(){document.getElementById('modal').classList.remove('show')}
function copyLink(){navigator.clipboard.writeText(shareLink).then(()=>alert('Link copied!'))}
document.getElementById('modal').onclick=e=>{if(e.target.id==='modal')closeModal()}
</script>
</body>
</html>
