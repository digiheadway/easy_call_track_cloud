<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo $name; ?> | Private Files</title>
    
    <!-- Favicon -->
    <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>📦</text></svg>">
    
    <meta property="og:type" content="website">
    <meta property="og:title" content="<?php echo $typeInfo['icon'] . ' ' . $name; ?>">
    <meta property="og:description" content="Shared <?php echo $typeInfo['label']; ?> - Open with Private Files on Android">
    <?php if ($previewUrl): ?>
    <meta property="og:image" content="<?php echo $previewUrl; ?>">
    <?php endif; ?>
    <meta name="theme-color" content="#1a1a2e">

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    
    <style>
        :root {
            --bg: #0f0f1a;
            --surface: #1a1a2e;
            --surface-hover: #252540;
            --border: #2d2d4a;
            --primary: #6c5ce7;
            --primary-light: #a29bfe;
            --success: #00b894;
            --warning: #fdcb6e;
            --text: #ffffff;
            --text-secondary: #a0a0b8;
            --text-muted: #6c6c80;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; }
        
        body {
            font-family: 'Inter', -apple-system, sans-serif;
            background: radial-gradient(circle at top right, #1a1a2e, #0f0f1a);
            min-height: 100vh;
            color: var(--text);
            display: flex;
            flex-direction: column;
            overflow-x: hidden;
        }

        .top-bar {
            display: flex;
            align-items: center;
            gap: 1rem;
            padding: 1rem 1.5rem;
            background: var(--surface);
            border-bottom: 1px solid var(--border);
            width: 100%;
            position: sticky;
            top: 0;
            z-index: 100;
        }

        .main-content {
            flex: 1;
            padding: 2rem 1rem;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            width: 100%;
        }
        .top-bar .back-btn {
            width: 36px; height: 36px;
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 10px;
            display: flex; align-items: center; justify-content: center;
            color: var(--text-secondary);
            font-size: 1.1rem;
        }
        .top-bar .title { flex: 1; font-weight: 600; font-size: 1rem; }
        .top-bar .live-badge {
            background: rgba(0, 184, 148, 0.2);
            color: var(--success);
            font-size: 0.7rem;
            font-weight: 600;
            padding: 0.35rem 0.65rem;
            border-radius: 20px;
            display: flex;
            align-items: center;
            gap: 0.35rem;
        }
        .top-bar .live-badge .dot {
            width: 6px; height: 6px;
            background: var(--success);
            border-radius: 50%;
            animation: pulse 1.5s infinite;
        }
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.4; }
        }

        .file-card {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 16px;
            overflow: hidden;
            animation: slideUp 0.4s ease;
            width: 100%;
            max-width: 480px;
        }

        @keyframes slideUp {
            from { opacity: 0; transform: translateY(15px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .file-preview {
            background: linear-gradient(135deg, #1e1e3a 0%, #2a2a4a 100%);
            min-height: 180px;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            position: relative;
            border-bottom: 1px solid var(--border);
            overflow: hidden;
        }

        .file-preview.has-image {
            min-height: 220px;
            background: #000;
        }

        .file-preview .preview-img {
            width: 100%;
            height: 220px;
            object-fit: cover;
            opacity: 0.85;
        }

        .file-preview .icon {
            font-size: 4rem;
            filter: drop-shadow(0 8px 16px rgba(0,0,0,0.3));
        }

        .file-preview .type-badge {
            position: absolute;
            top: 0.75rem;
            left: 0.75rem;
            background: var(--primary);
            padding: 0.35rem 0.65rem;
            border-radius: 6px;
            font-size: 0.6rem;
            font-weight: 700;
            color: white;
            text-transform: uppercase;
            z-index: 10;
        }

        .file-preview .views-badge {
            position: absolute;
            top: 0.75rem;
            right: 0.75rem;
            background: rgba(0,0,0,0.6);
            backdrop-filter: blur(8px);
            padding: 0.35rem 0.65rem;
            border-radius: 20px;
            font-size: 0.65rem;
            display: flex;
            align-items: center;
            gap: 0.3rem;
            color: var(--text-secondary);
            z-index: 10;
        }

        .file-preview .lock-badge {
            position: absolute;
            bottom: 0.75rem;
            right: 0.75rem;
            background: rgba(34,197,94,0.15);
            backdrop-filter: blur(8px);
            padding: 0.35rem 0.65rem;
            border-radius: 20px;
            font-size: 0.6rem;
            display: flex;
            align-items: center;
            gap: 0.3rem;
            color: #4ade80;
            border: 1px solid rgba(34,197,94,0.3);
            font-weight: 600;
            z-index: 10;
        }

        .file-info { padding: 1rem 1.25rem; }

        .file-name {
            font-size: 1.1rem;
            font-weight: 600;
            margin-bottom: 0.4rem;
            word-break: break-word;
            line-height: 1.4;
        }

        .file-meta {
            display: flex;
            flex-wrap: wrap;
            gap: 0.5rem;
            color: var(--text-muted);
            font-size: 0.75rem;
            margin-bottom: 1.25rem;
        }
        .file-meta .chip {
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid rgba(255, 255, 255, 0.1);
            padding: 0.35rem 0.65rem;
            border-radius: 8px;
            display: flex;
            align-items: center;
            gap: 0.35rem;
        }
        .file-meta .chip i { font-style: normal; opacity: 0.8; }

        .file-type-tag {
            display: inline-block;
            background: var(--primary);
            color: white;
            font-size: 0.6rem;
            font-weight: 600;
            padding: 0.25rem 0.5rem;
            border-radius: 4px;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        /* Urgency Banner */
        .urgency-banner {
            background: linear-gradient(90deg, rgba(253, 203, 110, 0.15), rgba(253, 203, 110, 0.05));
            border-top: 1px solid rgba(253, 203, 110, 0.2);
            padding: 0.65rem 1.25rem;
            display: flex;
            align-items: center;
            gap: 0.5rem;
            font-size: 0.75rem;
            color: var(--warning);
        }
        .urgency-banner .timer {
            font-weight: 600;
        }

        .actions {
            padding: 0.75rem 1.25rem 1.25rem;
            display: flex;
            flex-direction: column;
            gap: 0.75rem;
        }

        .btn {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.6rem;
            width: 100%;
            padding: 1rem;
            border-radius: 12px;
            font-weight: 600;
            font-size: 0.95rem;
            text-decoration: none;
            cursor: pointer;
            border: none;
            transition: all 0.2s ease;
            position: relative;
            overflow: hidden;
        }
        
        .btn-primary { 
            background: linear-gradient(135deg, var(--primary), #5b4cdb);
            color: white; 
            box-shadow: 0 4px 15px rgba(108, 92, 231, 0.3);
        }
        .btn-primary:hover, .btn-primary:active { 
            transform: scale(0.98); 
            box-shadow: 0 2px 10px rgba(108, 92, 231, 0.4);
        }

        .btn-primary .arrow {
            transition: transform 0.2s;
        }
        .btn-primary:hover .arrow {
            transform: translateX(3px);
        }

        /* Social Proof */
        .social-proof {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 1.5rem;
            padding: 1rem;
            width: 100%;
            max-width: 480px;
        }

        .proof-item {
            text-align: center;
        }
        .proof-item .number {
            font-size: 1.1rem;
            font-weight: 700;
            color: var(--text);
        }
        .proof-item .label {
            font-size: 0.65rem;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        .android-notice {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            padding: 0.75rem;
            width: 100%;
            max-width: 480px;
        }
        .android-notice .badge {
            display: inline-flex;
            align-items: center;
            gap: 0.4rem;
            background: rgba(0, 184, 148, 0.15);
            color: var(--success);
            font-size: 0.7rem;
            font-weight: 500;
            padding: 0.4rem 0.85rem;
            border-radius: 20px;
        }

        /* Modal */
        .modal-overlay {
            display: none;
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0, 0, 0, 0.85);
            backdrop-filter: blur(8px);
            z-index: 1000;
            align-items: center;
            justify-content: center;
            padding: 1.5rem;
        }
        .modal-overlay.show { display: flex; }

        .modal {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 20px;
            width: 100%;
            max-width: 360px;
            padding: 2rem 1.5rem;
            text-align: center;
            animation: modalSlide 0.3s ease;
            max-height: 90vh;
            overflow-y: auto;
        }

        @keyframes modalSlide {
            from { opacity: 0; transform: scale(0.9) translateY(20px); }
            to { opacity: 1; transform: scale(1) translateY(0); }
        }

        .modal .emoji { font-size: 3rem; margin-bottom: 1rem; }
        .modal h2 { font-size: 1.25rem; margin-bottom: 0.5rem; }
        .modal p { color: var(--text-muted); font-size: 0.9rem; line-height: 1.5; margin-bottom: 1.5rem; }

        .qr-box {
            background: white;
            padding: 1rem;
            border-radius: 16px;
            display: inline-block;
            margin-bottom: 1rem;
        }
        .qr-box img { display: block; width: 150px; height: 150px; }

        .scan-hint { color: var(--text-secondary); font-size: 0.8rem; margin-bottom: 1.5rem; }

        .modal-actions { display: flex; flex-direction: column; gap: 0.6rem; }

        .btn-copy {
            background: var(--surface-hover);
            color: var(--text);
            border: 1px solid var(--border);
        }

        .btn-close {
            background: transparent;
            color: var(--text-muted);
            font-size: 0.85rem;
            padding: 0.75rem;
        }

        /* Footer */
        .page-footer {
            background: var(--surface);
            border-top: 1px solid var(--border);
            padding: 1.25rem 1rem 3rem;
            text-align: center;
        }

        .footer-disclaimer {
            color: var(--text-muted);
            font-size: 0.65rem;
            line-height: 1.6;
            margin-bottom: 0.75rem;
            max-width: 600px;
            margin-left: auto;
            margin-right: auto;
        }

        .footer-links {
            display: flex;
            flex-wrap: wrap;
            justify-content: center;
            gap: 0.4rem 1rem;
            font-size: 0.7rem;
        }

        .footer-links a {
            color: var(--text-muted);
            text-decoration: none;
            transition: color 0.2s;
        }
        .footer-links a:hover { color: var(--primary); }

        /* Responsive */
        @media (min-width: 768px) {
            .main-content { padding: 2rem; }
            .file-card { max-width: 520px; }
            .file-preview { min-height: 220px; }
            .file-preview.has-image { min-height: 280px; }
            .file-preview .preview-img { height: 280px; }
            .file-name { font-size: 1.25rem; }
            .social-proof { max-width: 520px; }
        }

        @media (min-width: 1024px) {
            .file-card { max-width: 560px; }
            .file-preview { min-height: 240px; }
            .file-preview.has-image { min-height: 320px; }
            .file-preview .preview-img { height: 320px; }
            .file-preview .icon { font-size: 5rem; }
            .social-proof { max-width: 560px; }
        }
    </style>
</head>
<body>
    <div class="top-bar">
        <div class="back-btn">←</div>
        <div class="title">Private Files <span style="font-weight:400; font-size:0.85rem; color:var(--text-muted); margin-left:0.5rem">Shared by <?php echo $by; ?></span></div>
        <div class="live-badge">
            <span class="dot"></span>
            LIVE
        </div>
    </div>

    <div class="main-content">

        <div class="file-card">
            <div class="file-preview <?php echo $previewUrl ? 'has-image' : ''; ?>">
                <?php if ($previewUrl): ?>
                    <img src="<?php echo $previewUrl; ?>" alt="Preview" class="preview-img" onerror="this.style.display='none'; this.parentElement.classList.remove('has-image'); this.nextElementSibling.style.display='block';">
                    <span class="icon" style="display:none;"><?php echo $typeInfo['icon']; ?></span>
                <?php else: ?>
                    <span class="icon"><?php echo $typeInfo['icon']; ?></span>
                <?php endif; ?>
                <div class="type-badge"><?php echo strtoupper($typeInfo['label']); ?></div>
                <div class="views-badge">👁 <?php echo $views; ?> views</div>
                <div class="lock-badge">🔒 Protected</div>
            </div>

            <div class="file-info">
                <div class="file-name"><?php echo $name; ?></div>
                <div class="file-meta">
                    <div class="chip"><i>💾</i> <?php echo $size; ?></div>
                    <div class="chip"><i>⏱</i> <?php echo $timeAgo; ?>m ago</div>
                </div>
            </div>

            <div class="urgency-banner">
                <span>⚡</span>
                <span>Access available now • <span class="timer">Opens instantly</span></span>
            </div>

            <div class="actions">
                <button onclick="handleOpen()" class="btn btn-primary">
                    Open <?php echo $typeInfo['label']; ?> Now
                    <span class="arrow">→</span>
                </button>
            </div>
        </div>

        <div class="social-proof">
            <div class="proof-item">
                <div class="number"><?php echo $downloads; ?></div>
                <div class="label">Downloads</div>
            </div>
            <div class="proof-item">
                <div class="number"><?php echo $rating; ?></div>
                <div class="label">Rating</div>
            </div>
            <div class="proof-item">
                <div class="number">100%</div>
                <div class="label">Secure</div>
            </div>
        </div>

        <div class="android-notice">
            <div class="badge">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><path d="M6 18c0 .5.4 1 1 1h1v3.5c0 .8.7 1.5 1.5 1.5s1.5-.7 1.5-1.5V19h2v3.5c0 .8.7 1.5 1.5 1.5s1.5-.7 1.5-1.5V19h1c.6 0 1-.5 1-1V8H6v10z"/></svg>
                Android App Required
            </div>
        </div>
    </div>

    <footer class="page-footer">
        <p class="footer-disclaimer">
            ⚠️ Content info generated from URL parameters. Actual content may differ. We don't host files.
        </p>
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
            <p>This <?php echo strtolower($typeInfo['label']); ?> can only be opened on <strong>Android devices</strong> using the Private Files app.</p>
            
            <div class="qr-box">
                <img src="https://api.qrserver.com/v1/create-qr-code/?size=300x300&margin=8&data=<?php echo urlencode($currentUrl); ?>" alt="QR Code">
            </div>
            <p class="scan-hint">Scan with your Android phone</p>

            <div class="modal-actions">
                <button onclick="copyLink()" class="btn btn-copy">📋 Copy Link</button>
                <button onclick="closeModal()" class="btn btn-close">Close</button>
            </div>
        </div>
    </div>

    <script>
        const intentUrl = '<?php echo addslashes($intentUrl); ?>';
        const shareLink = '<?php echo addslashes($appDeepLink); ?>';
        const token = '<?php echo addslashes($token); ?>';
        const uniqueId = '<?php echo addslashes($uniqueId); ?>';
        const landing = '<?php echo addslashes($landing); ?>';
        const isAndroid = /Android/i.test(navigator.userAgent);

        // Social proof persistence
        const storageKey = `privatefiles_stats_${token}`;
        const initialStats = {
            views: <?php echo $views; ?>,
            opens: <?php echo isset($opens) ? $opens : 'Math.floor(Math.random() * 89) + 10'; ?>,
            time: <?php echo $timeAgo; ?>,
            lastVisit: Date.now()
        };

        function getStats() {
            const stored = localStorage.getItem(storageKey);
            if (!stored) {
                // First visit - use URL params or defaults
                saveStats(initialStats);
                return initialStats;
            }
            
            let stats = JSON.parse(stored);
            const now = Date.now();
            const minutesPassed = Math.floor((now - stats.lastVisit) / 60000);
            
            // Increment views on each visit
            stats.views += 1;
            
            // Increment opens occasionally
            if (Math.random() > 0.6) {
                stats.opens += Math.floor(Math.random() * 3) + 1;
            }
            
            // Time ago increases naturally
            stats.time = Math.min(stats.time + minutesPassed, 999);
            stats.lastVisit = now;
            
            saveStats(stats);
            return stats;
        }

        function saveStats(stats) {
            localStorage.setItem(storageKey, JSON.stringify(stats));
        }

        // Update DOM with persisted stats
        function updateStatsDisplay() {
            const stats = getStats();
            
            const viewsEl = document.querySelector('.views-badge');
            if (viewsEl) viewsEl.innerHTML = '👁 ' + stats.views + ' views';
            
            const timeEl = document.querySelector('.file-meta span:last-child');
            if (timeEl) timeEl.innerHTML = '⏱ Shared ' + (stats.time < 60 ? stats.time + 'm' : Math.floor(stats.time/60) + 'h') + ' ago';
            
            const downloadsEl = document.querySelectorAll('.proof-item .number')[0];
            const opensEl = document.querySelectorAll('.proof-item .number')[1];
        }

        // Run on load
        updateStatsDisplay();

        function handleOpen() {
            // Increment opens on click
            const stats = getStats();
            stats.opens += 1;
            saveStats(stats);

            // Server-side click tracking
            if (uniqueId) {
                fetch(`track_event.php?type=click&uniqueid=${uniqueId}&landing=${landing}`).catch(console.error);
            }
            
            if (isAndroid) {
                window.location.href = intentUrl;
            } else {
                document.getElementById('modal').classList.add('show');
            }
        }

        function closeModal() {
            document.getElementById('modal').classList.remove('show');
        }

        function copyLink() {
            navigator.clipboard.writeText(shareLink).then(() => {
                alert('Link copied! Open it on your Android device.');
            }).catch(() => {
                prompt('Copy this link:', shareLink);
            });
        }

        document.getElementById('modal').addEventListener('click', function(e) {
            if (e.target === this) closeModal();
        });
    </script>
</body>
</html>
