<?php
// LinkBox - Redirection Page
// Host this file at: https://api.pokipro.com/linkbox/index.php or map /linkbox/ to this file

$token = isset($_GET['token']) ? trim($_GET['token']) : '';
$hasToken = !empty($token);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Open in LinkBox</title>
    <meta name="robots" content="noindex, nofollow">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #0b0c10;
            --card-bg: #1f2833;
            --primary: #45a29e;
            --primary-hover: #66fcf1;
            --text: #ffffff;
            --text-secondary: #c5c6c7;
            --error: #ef4444;
            --surface: #1f2833;
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Outfit', -apple-system, BlinkMacSystemFont, sans-serif;
            background-color: var(--bg);
            color: var(--text);
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 20px;
            -webkit-font-smoothing: antialiased;
        }

        .container {
            width: 100%;
            max-width: 400px;
            text-align: center;
        }

        .card {
            background-color: var(--card-bg); /* Solid dark grey */
            border-radius: 16px;
            padding: 48px 32px;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
            border: 1px solid rgba(255, 255, 255, 0.08);
        }

        .icon-wrapper {
            width: 72px;
            height: 72px;
            background-color: rgba(69, 162, 158, 0.15); /* Solid transparent primary */
            border-radius: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 24px;
            font-size: 32px;
            color: var(--primary);
        }

        h1 {
            font-size: 24px;
            font-weight: 600;
            margin-bottom: 12px;
            color: var(--text); /* Solid white */
            letter-spacing: -0.5px;
        }

        p {
            color: var(--text-secondary);
            font-size: 15px;
            line-height: 1.6;
            margin-bottom: 32px;
            font-weight: 400;
        }

        .btn {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 100%;
            padding: 14px;
            background-color: var(--primary);
            color: #0b0c10; /* Dark text on bright button for contrast */
            border: none;
            border-radius: 12px;
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s ease;
            text-decoration: none;
        }

        .btn:hover {
            background-color: var(--primary-hover);
            transform: translateY(-1px);
        }

        .btn:active {
            transform: translateY(0);
        }

        .error-message {
            display: none;
            margin-top: 24px;
            padding: 12px;
            background-color: rgba(239, 68, 68, 0.1);
            border: 1px solid rgba(239, 68, 68, 0.2);
            border-radius: 8px;
            color: #fca5a5;
            font-size: 13px;
            font-weight: 500;
            animation: fadeIn 0.3s ease;
            line-height: 1.4;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(5px); }
            to { opacity: 1; transform: translateY(0); }
        }

        /* SVG Icon styling */
        svg {
            width: 32px;
            height: 32px;
            stroke-width: 2;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="card">
            <?php if ($hasToken): ?>
                <div class="icon-wrapper">
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>
                        <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
                    </svg>
                </div>
                <h1>LinkBox</h1>
                <p>Click below to open this content in the LinkBox app.</p>
                
                <a href="#" onclick="openApp()" class="btn">Open App</a>

                <div id="not-installed" class="error-message">
                    App not installed. Please install LinkBox first.
                </div>
            <?php else: ?>
                <div class="icon-wrapper">
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                </div>
                <h1>Invalid Link</h1>
                <p>This link appears to be invalid or missing a token.</p>
            <?php endif; ?>
        </div>
    </div>

    <script>
        function openApp() {
            var token = "<?php echo htmlspecialchars($token); ?>";
            var deepLink = "linkbox://open?token=" + encodeURIComponent(token);
            var webFallback = "https://play.google.com/store/apps/details?id=com.pokipro.linkbox"; // Replace with actual Play Store link if available
            
            // Try to open the app
            window.location.href = deepLink;

            // Check if app opened (naive approach using timeout)
            var start = Date.now();
            setTimeout(function() {
                var end = Date.now();
                // If the user is still here after 1.5 seconds, assume app didn't open
                if (end - start < 2000) {
                     document.getElementById('not-installed').style.display = 'block';
                }
            }, 1500);
        }
    </script>
</body>
</html>
