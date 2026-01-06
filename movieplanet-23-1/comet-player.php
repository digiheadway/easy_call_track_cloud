<?php
// ==== NO REFERRER POLICY ====
header('Referrer-Policy: no-referrer');

// ==== DETECT TITLE ====
$title = isset($_GET['title']) && !empty($_GET['title']) ? htmlspecialchars($_GET['title']) : 'Mov567868';

// ==== DETECT DEVICE ====
$userAgent = strtolower($_SERVER['HTTP_USER_AGENT']);
$isMobile = preg_match('/mobile|android|iphone|ipad|tablet|opera mini|mobi/', $userAgent);
$isComet = strpos($userAgent, 'comet') !== false;

// ==== REDIRECT NON-PC ====
if ($isMobile) {
    header('Location: https://be6.in/mov5667y8_adst_17dec');
    exit;
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title><?php echo $title; ?></title>
  <style>
    body {
      margin: 0;
      background: linear-gradient(145deg, #0d0d0d, #1a1a1a);
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      color: #fff;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100vh;
    }

    header img {
      width: 80px;
      margin-bottom: 20px;
    }

    .player-container {
      position: relative;
      width: 90%;
      max-width: 800px;
      aspect-ratio: 16 / 9;
      border-radius: 16px;
      overflow: hidden;
      box-shadow: 0 8px 25px rgba(0, 0, 0, 0.6);
      background: #000;
    }

    .video-title {
      position: absolute;
      top: 10px;
      left: 15px;
      background: rgba(0, 0, 0, 0.6);
      padding: 6px 14px;
      border-radius: 6px;
      font-size: 1rem;
      font-weight: 500;
      color: #fff;
    }

    video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .play-button {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 90px;
      height: 90px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.1);
      backdrop-filter: blur(8px);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s ease;
      border: 2px solid rgba(255, 255, 255, 0.3);
    }

    .play-button:hover {
      background: rgba(255, 255, 255, 0.3);
      transform: translate(-50%, -50%) scale(1.1);
    }

    .play-button::before {
      content: '';
      display: block;
      width: 0;
      height: 0;
      border-left: 24px solid #fff;
      border-top: 14px solid transparent;
      border-bottom: 14px solid transparent;
      margin-left: 6px;
    }

    .modal {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.85);
      display: flex;
      align-items: center;
      justify-content: center;
      visibility: hidden;
      opacity: 0;
      transition: all 0.4s ease;
      z-index: 10;
    }

    .modal.active {
      visibility: visible;
      opacity: 1;
    }

    .modal-content {
      background: #1f1f1f;
      padding: 30px;
      border-radius: 14px;
      text-align: center;
      width: 90%;
      max-width: 400px;
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.6);
      animation: fadeUp 0.4s ease;
    }

    .modal-content img {
      width: 70px;
      margin-bottom: 20px;
    }

    .modal-content h2 {
      font-size: 1.6rem;
      margin-bottom: 12px;
    }

    .modal-content p {
      font-size: 0.95rem;
      margin-bottom: 20px;
      color: #aaa;
    }

    .modal-content button {
      background: #0078ff;
      color: white;
      border: none;
      padding: 12px 20px;
      border-radius: 8px;
      font-size: 1rem;
      cursor: pointer;
      transition: background 0.3s ease;
    }

    .modal-content button:hover {
      background: #005ec2;
    }

    @keyframes fadeUp {
      from {
        transform: translateY(30px);
        opacity: 0;
      }
      to {
        transform: translateY(0);
        opacity: 1;
      }
    }
  </style>
</head>
<body>
  <header>
    <img src="https://cdn.prod.website-files.com/5f15081919fdf673994ab5fd/6807f205d9da98a89a158c03_comet-browser-icon.svg" alt="Comet Logo">
  </header>

  <div class="player-container">
    <div class="video-title"><?php echo $title; ?></div>
    <video id="video" poster="https://t" controls>
      <source src="https://cdn.pixabay.com/video/2022/03/03/108282-689345292_large.mp4" type="video/mp4">
    </video>
    <div class="play-button" id="playBtn"></div>
  </div>

  <div class="modal" id="modal">
    <div class="modal-content" id="modalContent">
      <img src="https://cdn.prod.website-files.com/5f15081919fdf673994ab5fd/6807f205d9da98a89a158c03_comet-browser-icon.svg" alt="Comet Browser">
      <h2>Play in Comet Browser</h2>
      <p>This video can only be played in Comet Browser for best experience.</p>
      <button onclick="window.open('https://pplx.ai/myuptownco72776', '_blank')">Download Now</button>
    </div>
  </div>

  <script>
    const modal = document.getElementById('modal');
    const playBtn = document.getElementById('playBtn');
    const video = document.getElementById('video');
    const isComet = navigator.userAgent.toLowerCase().includes('comet');

    if (isComet) {
      const modalContent = document.getElementById('modalContent');
      modalContent.innerHTML = `
        <img src="https://cdn.prod.website-files.com/5f15081919fdf673994ab5fd/6807f205d9da98a89a158c03_comet-browser-icon.svg" alt="Comet Browser">
        <h2>Welcome to Comet Browser</h2>
        <p>Signup now and chat with Perplexity AI directly inside your browser!</p>
        <button onclick=\"window.open('https://pplx.ai/myuptownco72776', '_blank')\">Signup & Chat</button>
      `;
      modal.classList.add('active');
    }

    playBtn.addEventListener('click', () => {
      if (!isComet) {
        video.pause();
        modal.classList.add('active');
      }
    });

    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        modal.classList.remove('active');
      }
    });
  </script>
</body>
</html>
