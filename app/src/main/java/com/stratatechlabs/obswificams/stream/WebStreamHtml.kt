package com.stratatechlabs.obswificams.stream

/**
 * Serves responsive, modern dark-mode HTML5 web interfaces for browser viewing, OBS Studio,
 * and comprehensive remote camera studio controls with real-time audio and cinematic filters.
 * Styled with a sophisticated Light Slate theme (#cbd5e1 / #94a3b8 / #334155 / #1e293b).
 */
object WebStreamHtml {

    fun getIndexHtml(hostAddress: String, port: Int): String {
        val streamUrl = "http://$hostAddress:$port/video.mjpeg"
        val audioUrl = "http://$hostAddress:$port/audio.wav"
        val obsUrl = "http://$hostAddress:$port/obs"
        val snapshotUrl = "http://$hostAddress:$port/snapshot.jpg"

        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>ObsWifiCams — Live Stream Studio</title>
  <style>
    :root {
      --bg: #0f172a;
      --card-bg: #1e293b;
      --card-border: #334155;
      --text: #f8fafc;
      --text-muted: #94a3b8;
      --accent: #cbd5e1;
      --slate-light: #cbd5e1;
      --slate-mid: #94a3b8;
      --slate-dark: #475569;
      --live: #cbd5e1;
      --btn-grad: linear-gradient(180deg, #334155 0%, #1e293b 100%);
      --btn-hover: linear-gradient(180deg, #475569 0%, #334155 100%);
      --btn-active: linear-gradient(180deg, #cbd5e1 0%, #94a3b8 100%);
    }

    * { box-sizing: border-box; margin: 0; padding: 0; }

    body {
      background-color: var(--bg);
      color: var(--text);
      font-family: Roboto, -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif;
      line-height: 1.4;
      padding: 12px;
    }

    .container {
      max-width: 1420px;
      margin: 0 auto;
    }

    /* Light Slate Studio Header */
    header {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 16px;
      margin-bottom: 14px;
      background: #1e293b;
      border: 1px solid var(--card-border);
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.5);
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .brand h1 {
      font-size: 1.3rem;
      font-weight: 700;
      letter-spacing: -0.5px;
      color: #f8fafc;
    }

    /* Light Slate Live Badge */
    .badge-live {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: #334155;
      color: #cbd5e1;
      border: 1px solid #475569;
      border-radius: 4px;
      padding: 2px 8px;
      font-size: 0.72rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .dot {
      width: 7px;
      height: 7px;
      background: #cbd5e1;
      border-radius: 50%;
      box-shadow: 0 0 6px #cbd5e1;
      animation: pulse 1.4s infinite;
    }

    @keyframes pulse {
      0% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.4; transform: scale(1.2); }
      100% { opacity: 1; transform: scale(1); }
    }

    .header-stats {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .stat-pill {
      background: #0f172a;
      border: 1px solid #334155;
      border-radius: 6px;
      padding: 3px 10px;
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .stat-pill strong {
      color: #cbd5e1;
    }

    .tea-btn {
      background: #334155 !important;
      border: 1px solid #475569 !important;
      color: #cbd5e1 !important;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: all 0.15s ease;
    }

    .tea-btn:hover {
      background: #cbd5e1 !important;
      color: #0f172a !important;
      transform: translateY(-1px);
    }

    .tea-toast {
      position: fixed;
      bottom: 24px;
      right: 24px;
      background: #1e293b;
      color: #f8fafc;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      padding: 12px 20px;
      font-size: 0.9rem;
      display: flex;
      align-items: center;
      gap: 10px;
      box-shadow: 0 10px 30px rgba(0,0,0,0.8);
      z-index: 9999;
      opacity: 0;
      transform: translateY(10px);
      transition: all 0.3s ease;
      pointer-events: none;
    }

    .tea-toast.show {
      opacity: 1;
      transform: translateY(0);
    }

    .studio-layout {
      display: grid;
      grid-template-columns: 1fr;
      gap: 16px;
    }

    @media (min-width: 1024px) {
      .studio-layout {
        grid-template-columns: 1.65fr 1.05fr;
      }
    }

    /* Light Slate Player Frame */
    .video-panel {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .yt-player-frame {
      background: #020617;
      border: 1px solid #334155;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.7);
      display: flex;
      flex-direction: column;
      position: relative;
    }

    .yt-player-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 14px;
      background: #1e293b;
      border-bottom: 1px solid #334155;
      font-size: 0.8rem;
    }

    .yt-live-badge {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #cbd5e1;
      font-weight: 700;
      text-transform: uppercase;
      font-size: 0.75rem;
      letter-spacing: 0.5px;
    }

    .yt-live-dot {
      width: 8px;
      height: 8px;
      background-color: #cbd5e1;
      border-radius: 50%;
      box-shadow: 0 0 6px #cbd5e1;
      animation: yt-pulse 1.4s infinite;
    }

    @keyframes yt-pulse {
      0% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.4; transform: scale(1.2); }
      100% { opacity: 1; transform: scale(1); }
    }

    .yt-aspect-selector {
      display: flex;
      align-items: center;
      gap: 4px;
      background: #0f172a;
      border: 1px solid #334155;
      border-radius: 6px;
      padding: 2px;
    }

    .yt-aspect-btn {
      background: transparent;
      border: none;
      color: #94a3b8;
      padding: 3px 8px;
      font-size: 0.72rem;
      font-weight: 600;
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .yt-aspect-btn.active, .yt-aspect-btn:hover {
      background: #cbd5e1;
      color: #0f172a;
    }

    .video-viewport {
      position: relative;
      background: #020617;
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: aspect-ratio 0.3s ease, max-height 0.3s ease;
      min-height: 240px;
    }

    .video-viewport.aspect-16-9 { aspect-ratio: 16 / 9; max-height: none; }
    .video-viewport.aspect-9-16 { aspect-ratio: 9 / 16; max-height: 70vh; margin: 0 auto; }
    .video-viewport.aspect-auto { aspect-ratio: auto; max-height: 75vh; }

    .video-viewport img {
      width: 100%;
      height: 100%;
      object-fit: contain;
      transition: transform 0.2s ease, filter 0.2s ease;
    }

    .video-viewport.mirrored img { transform: scaleX(-1); }

    /* Light Slate Live Line */
    .yt-live-bar {
      height: 3px;
      background: #cbd5e1;
      width: 100%;
      box-shadow: 0 0 6px rgba(203, 213, 225, 0.8);
      position: relative;
      z-index: 10;
    }

    /* Player Controls Bar */
    .yt-controls-bar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      background: #1e293b;
      border-top: 1px solid #334155;
      padding: 8px 12px;
    }

    .yt-controls-left, .yt-controls-right {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    /* Light Slate Buttons */
    .yt-btn, .btn-action, .chip, .tab-btn {
      background: #334155;
      border: 1px solid #475569;
      color: #f8fafc;
      border-radius: 6px;
      font-size: 0.78rem;
      font-weight: 500;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 5px;
      transition: all 0.15s ease;
    }

    .yt-btn {
      padding: 6px 10px;
    }

    .yt-btn:hover, .btn-action:hover, .chip:hover, .tab-btn:hover {
      background: #475569;
      border-color: #cbd5e1;
      color: #ffffff;
    }

    .yt-btn.active, .chip.active, .tab-btn.active {
      background: #cbd5e1;
      color: #0f172a;
      border-color: #cbd5e1;
      font-weight: 700;
      box-shadow: 0 0 10px rgba(203, 213, 225, 0.3);
    }

    /* Volume & Range Sliders */
    .yt-volume-wrap {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .yt-volume-slider, input[type=range] {
      accent-color: #cbd5e1;
      cursor: pointer;
    }

    input[type=range] {
      height: 5px;
      background: #0f172a;
      border-radius: 3px;
    }

    /* Live Audio VU Meter (Slate Gradient) */
    .vu-meter-container {
      display: flex;
      align-items: center;
      gap: 6px;
      flex: 1;
      min-width: 100px;
      background: #0f172a;
      border: 1px solid #334155;
      border-radius: 6px;
      padding: 3px 6px;
    }

    .vu-meter-label {
      font-size: 0.7rem;
      font-weight: 700;
      color: var(--text-muted);
      text-transform: uppercase;
    }

    .vu-meter-bar {
      flex: 1;
      height: 6px;
      background: #020617;
      border-radius: 3px;
      overflow: hidden;
      position: relative;
    }

    .vu-meter-fill {
      height: 100%;
      width: 0%;
      background: linear-gradient(to right, #475569, #94a3b8, #cbd5e1);
      transition: width 0.08s linear;
    }

    /* Controls Chassis Panel */
    .controls-panel {
      background: #1e293b;
      border: 1px solid var(--card-border);
      border-radius: 12px;
      padding: 14px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.5);
    }

    /* Studio Tab Bar */
    .tab-bar {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      background: #0f172a;
      border: 1px solid #334155;
      border-radius: 8px;
      padding: 4px;
    }

    .tab-btn {
      flex: 1;
      min-width: 80px;
      padding: 6px 8px;
      font-size: 0.78rem;
      text-align: center;
      justify-content: center;
    }

    .tab-content {
      display: none;
      border: 1px solid #334155;
      border-radius: 8px;
      padding: 12px;
      background: #182232;
    }

    .tab-content.active { display: block; }

    .tab-content h3 {
      font-size: 0.85rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #cbd5e1;
      margin-bottom: 12px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .control-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 10px;
    }

    .control-row:last-child { margin-bottom: 0; }

    .control-label {
      font-size: 0.8rem;
      font-weight: 600;
      color: var(--text-muted);
      min-width: 65px;
    }

    .chip-group {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      flex: 1;
    }

    .chip {
      padding: 5px 10px;
      font-size: 0.78rem;
    }

    .chip-record {
      background: #334155;
      border-color: #64748b;
      color: #f8fafc;
    }

    .chip-record.active {
      background: #cbd5e1;
      color: #0f172a;
      font-weight: 700;
      animation: pulse 1.4s infinite;
    }

    .slider-val {
      font-size: 0.8rem;
      font-family: monospace;
      font-weight: 700;
      color: #cbd5e1;
      min-width: 45px;
      text-align: right;
    }

    .btn-action {
      padding: 8px 12px;
      font-size: 0.85rem;
    }

    .btn-capture {
      background: #cbd5e1;
      color: #0f172a;
      font-weight: 700;
      border-color: #cbd5e1;
    }

    .btn-capture:hover {
      background: #e2e8f0;
      color: #0f172a;
    }

    /* Stream URL Copy Boxes */
    .url-box {
      display: flex;
      flex-direction: column;
      gap: 4px;
      margin-top: 8px;
    }

    .url-box label {
      font-size: 0.72rem;
      color: var(--text-muted);
      text-transform: uppercase;
      font-weight: 700;
    }

    .url-input-wrap {
      display: flex;
      gap: 4px;
    }

    .url-input-wrap input {
      flex: 1;
      background: #0f172a;
      border: 1px solid #334155;
      color: var(--text);
      padding: 6px 8px;
      border-radius: 6px;
      font-family: monospace;
      font-size: 0.75rem;
    }

    .url-input-wrap button {
      padding: 6px 12px;
      background: #334155;
      border: 1px solid #475569;
      border-radius: 6px;
      color: var(--text);
      font-size: 0.75rem;
      font-weight: 700;
      cursor: pointer;
    }

    .url-input-wrap button:hover {
      background: #475569;
      border-color: #cbd5e1;
      color: #ffffff;
    }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <div class="brand">
        <h1>ObsWifiCams Studio</h1>
        <div class="badge-live"><span class="dot"></span>Live Stream</div>
      </div>
      <div class="header-stats">
        <div class="stat-pill">Quality: <strong id="resBadge">720p</strong></div>
        <div class="stat-pill">FPS: <strong id="fpsVal">--</strong></div>
        <div class="stat-pill">Mic: <strong id="audioStat">Real-time</strong></div>
        <div class="stat-pill">Facing: <strong id="facingBadge">Back</strong></div>
        <div class="stat-pill">Viewers: <strong id="clientsVal">1</strong></div>
        <button id="teaRewardRemoteBtn" class="stat-pill tea-btn" onclick="sendRewardTea()" title="Reward Tea">
          <svg class="tea-icon" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M6,2c0.55,0 1,0.45 1,1v1c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1V3C5,2.45 5.45,2 6,2zM10,1c0.55,0 1,0.45 1,1v2c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1V2C9,1.45 9.45,1 10,1zM14,2c0.55,0 1,0.45 1,1v1c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1V3C13,2.45 13.45,2 14,2z" />
            <path d="M20,6H4v8c0,2.21 1.79,4 4,4h6c2.21,0 4,-1.79 4,-4v-2h2c1.1,0 2,-0.9 2,-2V8C22,6.9 21.1,6 20,6zM20,10h-2V8h2V10zM2,20h18c0.55,0 1,0.45 1,1s-0.45,1 -1,1H2c-0.55,0 -1,-0.45 -1,-1S1.45,20 2,20z" />
          </svg>
          <strong>Reward Tea</strong>
        </button>
      </div>
    </header>

    <div class="studio-layout">
      <!-- Left Column: Video Player & Controls Bar -->
      <div class="video-panel">
        <div class="yt-player-frame">
          <div class="yt-player-header">
            <div class="yt-live-badge">
              <span class="yt-live-dot"></span>
              <span>LIVE CAMERA STREAM</span>
            </div>
            <div class="yt-aspect-selector">
              <button class="yt-aspect-btn active" data-aspect="auto" onclick="setAspectMode('auto')" title="Auto Orientation">📐 Auto</button>
              <button class="yt-aspect-btn" data-aspect="16-9" onclick="setAspectMode('16-9')" title="16:9 Landscape">📺 16:9</button>
              <button class="yt-aspect-btn" data-aspect="9-16" onclick="setAspectMode('9-16')" title="9:16 Portrait">📱 9:16</button>
            </div>
          </div>

          <div class="video-viewport aspect-auto" id="viewport">
            <img id="streamImg" class="filter-none" src="/video.mjpeg" alt="Live Camera Stream" />
          </div>

          <!-- Light Slate Live Progress Line -->
          <div class="yt-live-bar"></div>

          <div class="yt-controls-bar">
            <div class="yt-controls-left">
              <button class="yt-btn" id="audioToggleBtn" onclick="toggleAudioPlayback()" title="Mute / Unmute Real-Time Audio">
                <span id="audioIcon">🔈</span> <span id="audioBtnText">Unmute</span>
              </button>
              <div class="yt-volume-wrap">
                <input type="range" class="yt-volume-slider" id="volumeSlider" min="0" max="1" step="0.05" value="1" oninput="setAudioVolume(this.value)" title="Volume">
              </div>
              <div class="vu-meter-container">
                <span class="vu-meter-label">VU</span>
                <div class="vu-meter-bar">
                  <div class="vu-meter-fill" id="vuFill"></div>
                </div>
                <span class="slider-val" id="vuText" style="min-width: 28px; font-size: 0.7rem;">0%</span>
              </div>
            </div>

            <div class="yt-controls-right">
              <button class="yt-btn tea-btn" onclick="sendRewardTea()" title="Reward Tea to Streamer">☕ Tea</button>
              <button class="yt-btn" onclick="saveSnapshotToPc()" title="Save frame as JPEG to PC">📷 Snap</button>
              <button class="yt-btn" onclick="toggleMirror()" title="Flip horizontally">⇄ Mirror</button>
              <button class="yt-btn" onclick="rotateNext()" title="Rotate Stream 90°">⟳ Rot <span id="rotBadge" style="font-size:0.7rem; color:#cbd5e1;">0°</span></button>
              <button class="yt-btn" onclick="toggleFullscreen()" title="Fullscreen">⛶</button>
              <button class="yt-btn" onclick="reconnectStream()" title="Reload Stream">🔄</button>
            </div>
          </div>
        </div>

        <audio id="audioPlayer" src="/audio.wav" preload="none" style="display:none;"></audio>
      </div>

      <!-- Right Column: Studio Controls Panel -->
      <div class="controls-panel">
        <!-- Tab Bar -->
        <div class="tab-bar">
          <button class="tab-btn" data-tab="allTab" onclick="selectTab('allTab', this)" title="View all tools on a single page">🌐 All Tools</button>
          <button class="tab-btn active" data-tab="captureTab" onclick="selectTab('captureTab', this)">📸 Capture</button>
          <button class="tab-btn" data-tab="filterTab" onclick="selectTab('filterTab', this)">🎨 Filters</button>
          <button class="tab-btn" data-tab="qualityTab" onclick="selectTab('qualityTab', this)">⚡ Quality</button>
          <button class="tab-btn" data-tab="cameraTab" onclick="selectTab('cameraTab', this)">🎥 Optics</button>
          <button class="tab-btn" data-tab="proTab" onclick="selectTab('proTab', this)">🎛 Pro & OBS</button>
        </div>

        <!-- Tab 1: 📸 Capture & Mode -->
        <div class="tab-content active" id="captureTab">
          <h3><span>Camera Mode & Capture</span> <span id="recTimer" style="color:var(--live); display:none; font-weight:700;">REC 00:00</span></h3>
          <div class="control-row">
            <span class="control-label">Mode:</span>
            <div class="chip-group" id="modeChips">
              <div class="chip active" data-mode="photo" onclick="setMode('photo')">Photo</div>
              <div class="chip" data-mode="video" onclick="setMode('video')">Video</div>
              <div class="chip" data-mode="pro" onclick="setMode('pro')">Pro Manual</div>
            </div>
          </div>
          <div class="control-row">
            <span class="control-label">Save To:</span>
            <div class="chip-group" id="saveDestChips">
              <div class="chip active" data-dest="phone" onclick="setSaveDestination('phone')" title="Record to Mobile gallery">📱 Mobile</div>
              <div class="chip" data-dest="both" onclick="setSaveDestination('both')" title="Save to Mobile gallery and auto-download to PC">📱💻 Mobile + PC</div>
            </div>
          </div>
          <div style="display:grid; grid-template-columns: 1fr 1fr; gap:8px; margin-top:8px;">
            <button class="btn-action btn-capture" id="captureBtn" onclick="takeSnapshot()">📸 Take Photo</button>
            <button class="btn-action chip-record" id="recordBtn" onclick="toggleRecording()">⏺ Record Video</button>
          </div>
          <div id="phoneVideoRow" style="margin-top:8px; display:none;">
            <a id="downloadPhoneVideoBtn" href="/latest-video.mp4" download class="btn-action" style="display:block; text-align:center; text-decoration:none; background:#1e293b; color:#cbd5e1; border:1px solid #cbd5e1; padding:6px 10px; font-size:0.8rem; border-radius:6px;">
              📥 Download Recorded MP4
            </a>
          </div>
        </div>

        <!-- Tab 2: 🎨 Filters & Color Grading -->
        <div class="tab-content" id="filterTab">
          <h3>Cinematic Color Grading</h3>
          <div class="chip-group" id="filterChips">
            <div class="chip active" data-filter="none" onclick="setFilter('none')">Normal</div>
            <div class="chip" data-filter="imax" onclick="setFilter('imax')">🎬 IMAX 70mm</div>
            <div class="chip" data-filter="bluray" onclick="setFilter('bluray')">💿 Blu-Ray</div>
            <div class="chip" data-filter="tealorange" onclick="setFilter('tealorange')">🎬 Teal & Orange</div>
            <div class="chip" data-filter="vivid" onclick="setFilter('vivid')">🌈 Vivid</div>
            <div class="chip" data-filter="vintage" onclick="setFilter('vintage')">🎞 Vintage</div>
            <div class="chip" data-filter="noir" onclick="setFilter('noir')">🕶 Noir B&W</div>
            <div class="chip" data-filter="cyberpunk" onclick="setFilter('cyberpunk')">⚡ Cyberpunk</div>
            <div class="chip" data-filter="invert" onclick="setFilter('invert')">🔄 Invert</div>
          </div>
        </div>

        <!-- Tab 3: ⚡ Quality & Performance -->
        <div class="tab-content" id="qualityTab">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
            <h3 style="margin:0;">Resolution</h3>
            <button class="chip active" id="autoAdjustBtn" onclick="toggleAutoAdjust()" style="margin:0; font-size:11px; padding:3px 10px; cursor:pointer;" title="Automatically adjusts compression for lag-free streaming">
              ⚡ Auto-Smooth: ON
            </button>
          </div>
          <div class="chip-group" id="qualityChips" style="margin-bottom:12px;">
            <div class="chip" data-q="auto" onclick="setQuality('auto')">Auto Resolution</div>
            <div class="chip" data-q="4k" onclick="setQuality('4k')">4K UHD</div>
            <div class="chip" data-q="2k" onclick="setQuality('2k')">2K QHD</div>
            <div class="chip" data-q="1080p" onclick="setQuality('1080p')">1080p FHD</div>
            <div class="chip active" data-q="720p" onclick="setQuality('720p')">720p HD</div>
            <div class="chip" data-q="480p" onclick="setQuality('480p')">480p SD</div>
          </div>

          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
            <h3 style="margin:0;">Frame Rate (FPS)</h3>
            <span class="stat-pill" style="font-size:11px;" id="fpsTargetBadge">Auto FPS</span>
          </div>
          <div class="chip-group" id="fpsChips">
            <div class="chip active" data-fps="auto" onclick="setFps('auto')">Auto FPS</div>
            <div class="chip" data-fps="120" onclick="setFps('120')">120 FPS</div>
            <div class="chip" data-fps="60" onclick="setFps('60')">60 FPS</div>
            <div class="chip" data-fps="30" onclick="setFps('30')">30 FPS</div>
            <div class="chip" data-fps="24" onclick="setFps('24')">24 FPS</div>
            <div class="chip" data-fps="15" onclick="setFps('15')">15 FPS</div>
          </div>
        </div>

        <!-- Tab 4: 🎥 Camera, Optics & Lighting -->
        <div class="tab-content" id="cameraTab">
          <h3>Camera, Lenses & Frame</h3>
          <div class="control-row">
            <span class="control-label">Facing:</span>
            <div class="chip-group">
              <button class="btn-action" style="padding:4px 10px; font-size:0.8rem;" onclick="flipCamera()">🔄 Flip Camera</button>
              <select id="lensSelect" onchange="selectLens(this.value)" style="background:#334155; color:#fff; border:1px solid #475569; border-radius:6px; padding:4px 8px; font-size:0.8rem;">
                <option value="">Default Lens</option>
              </select>
            </div>
          </div>
          <div class="control-row">
            <span class="control-label">Aspect:</span>
            <div class="chip-group" id="aspectChips">
              <div class="chip" data-aspect="16:9" onclick="setAspect('16:9')">16:9 Wide</div>
              <div class="chip active" data-aspect="4:3" onclick="setAspect('4:3')">4:3 Standard</div>
              <div class="chip" data-aspect="1:1" onclick="setAspect('1:1')">1:1 Square</div>
            </div>
          </div>
          <div class="control-row">
            <span class="control-label">Rotate:</span>
            <div class="chip-group" id="rotationChips">
              <div class="chip active" data-rot="0" onclick="setRotation(0)">0° Normal</div>
              <div class="chip" data-rot="90" onclick="setRotation(90)">90° CW</div>
              <div class="chip" data-rot="180" onclick="setRotation(180)">180°</div>
              <div class="chip" data-rot="270" onclick="setRotation(270)">270° CCW</div>
            </div>
          </div>

          <h3 style="margin-top:12px;">Zoom & Exposure</h3>
          <div class="control-row">
            <span class="control-label">Zoom:</span>
            <input type="range" id="zoomSlider" min="1.0" max="10.0" step="0.1" value="1.0" oninput="setZoom(this.value)">
            <span class="slider-val" id="zoomVal">1.0×</span>
          </div>
          <div class="chip-group" style="margin-bottom:8px;">
            <div class="chip" onclick="setZoom(0.6)">0.6× Wide</div>
            <div class="chip" onclick="setZoom(1.0)">1.0× Standard</div>
            <div class="chip" onclick="setZoom(2.0)">2.0× Tele</div>
            <div class="chip" onclick="setZoom(5.0)">5.0× Zoom</div>
          </div>
          <div class="control-row">
            <span class="control-label">Exposure:</span>
            <input type="range" id="evSlider" min="-4" max="4" step="1" value="0" oninput="setExposure(this.value)">
            <span class="slider-val" id="evVal">0 EV</span>
          </div>

          <h3 style="margin-top:12px;">Lighting & Flash</h3>
          <div class="control-row">
            <span class="control-label">Torch:</span>
            <button class="btn-action" id="torchBtn" style="padding:4px 12px; font-size:0.8rem;" onclick="toggleTorch()">🔦 Toggle Torch</button>
            <input type="range" id="torchStrengthSlider" min="1" max="10" value="5" oninput="setTorchStrength(this.value)" style="display:none;" title="Torch Level">
          </div>
          <div class="control-row">
            <span class="control-label">Flash:</span>
            <div class="chip-group" id="flashChips">
              <div class="chip active" data-flash="off" onclick="setFlash('off')">Off</div>
              <div class="chip" data-flash="auto" onclick="setFlash('auto')">Auto</div>
              <div class="chip" data-flash="on" onclick="setFlash('on')">On</div>
              <div class="chip" data-flash="torch" onclick="setFlash('torch')">Torch</div>
            </div>
          </div>
        </div>

        <!-- Tab 5: 🎛 Pro Manual & OBS URLs -->
        <div class="tab-content" id="proTab">
          <h3>Pro Manual Controls</h3>
          <div class="control-row">
            <span class="control-label">ISO:</span>
            <select id="isoSelect" onchange="setProIso(this.value)" style="background:#334155; color:#fff; border:1px solid #475569; border-radius:6px; padding:4px 8px; font-size:0.8rem;">
              <option value="auto">Auto ISO</option>
              <option value="100">ISO 100</option>
              <option value="200">ISO 200</option>
              <option value="400">ISO 400</option>
              <option value="800">ISO 800</option>
              <option value="1600">ISO 1600</option>
              <option value="3200">ISO 3200</option>
              <option value="6400">ISO 6400</option>
            </select>
            <span class="control-label" style="min-width:30px; margin-left:8px;">WB:</span>
            <select id="wbSelect" onchange="setProAwb(this.value)" style="background:#334155; color:#fff; border:1px solid #475569; border-radius:6px; padding:4px 8px; font-size:0.8rem;">
              <option value="auto">Auto AWB</option>
              <option value="2">Incandescent</option>
              <option value="3">Fluorescent</option>
              <option value="4">Warm Fluorescent</option>
              <option value="5">Daylight</option>
              <option value="6">Cloudy</option>
              <option value="7">Twilight</option>
              <option value="8">Shade</option>
            </select>
          </div>
          <div class="control-row">
            <span class="control-label">Focus:</span>
            <input type="range" id="focusSlider" min="0" max="10" step="0.2" value="0" oninput="setProFocus(this.value)">
            <button class="btn-action" style="padding:2px 8px; font-size:0.75rem;" onclick="setProFocus('auto')">AF</button>
          </div>

          <h3 style="margin-top:14px;">OBS Studio & Streaming URLs</h3>
          <div class="url-box">
            <label>OBS Video Stream (`/video.mjpeg`)</label>
            <div class="url-input-wrap">
              <input type="text" readonly value="$streamUrl" id="obsVideoInput">
              <button onclick="copyText('obsVideoInput', this)">Copy</button>
            </div>
          </div>
          <div class="url-box">
            <label>OBS Audio Stream (`/audio.wav`)</label>
            <div class="url-input-wrap">
              <input type="text" readonly value="$audioUrl" id="obsAudioInput">
              <button onclick="copyText('obsAudioInput', this)">Copy</button>
            </div>
          </div>
          <div class="url-box">
            <label>OBS Clean Browser Source (`/obs`)</label>
            <div class="url-input-wrap">
              <input type="text" readonly value="$obsUrl" id="obsCleanInput">
              <button onclick="copyText('obsCleanInput', this)">Copy</button>
            </div>
          </div>
          <div class="url-box">
            <label>Live JPEG Snapshot (`/snapshot.jpg`)</label>
            <div class="url-input-wrap">
              <input type="text" readonly value="$snapshotUrl" id="obsSnapshotInput">
              <button onclick="copyText('obsSnapshotInput', this)">Copy</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div id="toast" class="tea-toast"></div>

  <script>
    function selectTab(tabId, btn) {
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      if (btn) btn.classList.add('active');
      else {
        const activeBtn = document.querySelector('[data-tab="' + tabId + '"]');
        if (activeBtn) activeBtn.classList.add('active');
      }

      if (tabId === 'allTab') {
        document.querySelectorAll('.tab-content').forEach(el => el.classList.add('active'));
      } else {
        document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
        const el = document.getElementById(tabId);
        if (el) el.classList.add('active');
      }
      try { localStorage.setItem('obs_active_tab', tabId); } catch(e) {}
    }

    // Restore active tab on load
    document.addEventListener('DOMContentLoaded', () => {
      const savedTab = localStorage.getItem('obs_active_tab') || 'captureTab';
      selectTab(savedTab);
    });

    const streamImg = document.getElementById('streamImg');
    const viewport = document.getElementById('viewport');
    let aspectMode = 'auto';

    function setAspectMode(mode) {
      aspectMode = mode;
      document.querySelectorAll('.yt-aspect-btn').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-aspect') === mode);
      });
      viewport.classList.remove('aspect-auto', 'aspect-16-9', 'aspect-9-16');
      if (aspectMode === '16-9') {
        viewport.classList.add('aspect-16-9');
        viewport.style.aspectRatio = '16 / 9';
        viewport.style.maxHeight = 'none';
      } else if (aspectMode === '9-16') {
        viewport.classList.add('aspect-9-16');
        viewport.style.aspectRatio = '9 / 16';
        viewport.style.maxHeight = '70vh';
      } else {
        viewport.classList.add('aspect-auto');
        autoDetectStreamAspect();
      }
    }

    function autoDetectStreamAspect() {
      if (aspectMode !== 'auto' || !streamImg || !viewport) return;
      const nw = streamImg.naturalWidth;
      const nh = streamImg.naturalHeight;
      if (nw > 0 && nh > 0) {
        viewport.style.aspectRatio = nw + ' / ' + nh;
        viewport.style.maxHeight = (nh > nw) ? '70vh' : 'none';
      } else {
        viewport.style.aspectRatio = '16 / 9';
        viewport.style.maxHeight = 'none';
      }
    }

    streamImg.addEventListener('load', autoDetectStreamAspect);

    function toggleFullscreen() {
      if (!document.fullscreenElement) {
        if (viewport.requestFullscreen) viewport.requestFullscreen();
        else if (viewport.webkitRequestFullscreen) viewport.webkitRequestFullscreen();
      } else {
        if (document.exitFullscreen) document.exitFullscreen();
      }
    }

    function copyText(id, btn) {
      const el = document.getElementById(id);
      el.select();
      navigator.clipboard.writeText(el.value).then(() => {
        const prev = btn.innerText;
        btn.innerText = 'Copied!';
        setTimeout(() => btn.innerText = prev, 1500);
      });
    }

    // Remote action APIs
    function postApi(endpoint, params = {}) {
      const qs = new URLSearchParams(params).toString();
      const url = endpoint + (qs ? '?' + qs : '');
      return fetch(url, { method: 'POST' }).then(r => r.json()).catch(() => ({}));
    }

    // Save destination: 'phone' (Mobile) or 'both' (Mobile PC)
    let saveDestination = localStorage.getItem('obs_save_dest') || 'phone';
    if (saveDestination === 'pc') saveDestination = 'phone';

    function setSaveDestination(dest) {
      if (dest !== 'both') dest = 'phone';
      saveDestination = dest;
      try { localStorage.setItem('obs_save_dest', dest); } catch(e) {}
      document.querySelectorAll('#saveDestChips .chip').forEach(c => {
        c.classList.toggle('active', c.getAttribute('data-dest') === dest);
      });
      updateShutterButtonLabels();
    }

    function updateShutterButtonLabels() {
      const capBtn = document.getElementById('captureBtn');
      const recBtn = document.getElementById('recordBtn');
      if (isPcRecording || isPhoneRecording || isStartingRecording) return;
      if (capBtn) {
        if (saveDestination === 'both') capBtn.innerText = '📸 Take Photo (Mobile PC)';
        else capBtn.innerText = '📸 Take Photo';
      }
      if (recBtn) {
        if (saveDestination === 'both') recBtn.innerText = '⏺ Record Video (Mobile PC)';
        else recBtn.innerText = '⏺ Record Video';
      }
    }

    async function takeSnapshot() {
      const dest = saveDestination;
      const btn = document.getElementById('captureBtn');
      const prev = btn ? btn.innerText : '';
      if (btn) btn.innerText = '⏳ Capturing...';

      if (dest === 'phone' || dest === 'both') {
        postApi('/api/photo');
        if (dest === 'phone') {
          showToast('📸 Photo captured on Mobile');
        }
      }

      if (dest === 'both') {
        await saveSnapshotToPc();
      }

      if (btn) btn.innerText = prev;
    }

    async function saveSnapshotToPc() {
      const now = new Date();
      const pad = n => String(n).padStart(2, '0');
      const filename = 'ObsWifiCams_Photo_' + now.getFullYear() + pad(now.getMonth() + 1) + pad(now.getDate()) + '_' + pad(now.getHours()) + pad(now.getMinutes()) + pad(now.getSeconds()) + '.jpg';

      let saved = false;
      try {
        const res = await fetch('/snapshot.jpg?t=' + Date.now());
        if (res.ok) {
          const blob = await res.blob();
          if (blob && blob.size > 1000) {
            downloadBlob(blob, filename);
            showToast('📸 Photo saved to PC: ' + filename);
            saved = true;
          }
        }
      } catch(e) {
        console.warn('Snapshot fetch error, falling back to canvas:', e);
      }

      if (!saved) {
        try {
          const canvas = document.createElement('canvas');
          canvas.width = streamImg.naturalWidth || 1280;
          canvas.height = streamImg.naturalHeight || 720;
          const ctx = canvas.getContext('2d');
          if (activeFilter && activeFilter !== 'none') {
            ctx.filter = getComputedStyle(streamImg).filter;
          }
          ctx.drawImage(streamImg, 0, 0, canvas.width, canvas.height);
          canvas.toBlob(blob => {
            if (blob) {
              downloadBlob(blob, filename);
              showToast('📸 Photo saved to PC: ' + filename);
            }
          }, 'image/jpeg', 0.95);
        } catch(e) {
          console.error('Canvas snapshot error:', e);
          showToast('❌ Failed to capture photo to PC');
        }
      }
    }

    function downloadBlob(blob, filename) {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(() => URL.revokeObjectURL(url), 15000);
    }

    // In-browser PC Video Recording Engine (Canvas + MediaRecorder)
    let isPcRecording = false;
    let isPhoneRecording = false;
    let phoneRecordingSeconds = 0;
    let pcMediaRecorder = null;
    let pcRecordedChunks = [];
    let pcRecordStartTime = 0;
    let pcDrawInterval = null;
    let pcTimerInterval = null;
    let pcRecCanvas = null;
    let pcRecCtx = null;
    let pcMjpegAbort = null;
    let pcCurrentFrame = null;

    async function startMjpegFramePump() {
      if (pcMjpegAbort) {
        try { pcMjpegAbort.abort(); } catch(e) {}
      }
      pcMjpegAbort = new AbortController();
      try {
        const res = await fetch('/video.mjpeg', { signal: pcMjpegAbort.signal });
        if (!res.body) return;
        const reader = res.body.getReader();
        let buffer = new Uint8Array(0);

        while (isPcRecording) {
          const { done, value } = await reader.read();
          if (done) break;

          const next = new Uint8Array(buffer.length + value.length);
          next.set(buffer);
          next.set(value, buffer.length);
          buffer = next;

          let start = -1;
          for (let i = 0; i < buffer.length - 1; i++) {
            if (buffer[i] === 0xFF && buffer[i+1] === 0xD8) { start = i; break; }
          }
          if (start >= 0) {
            let end = -1;
            for (let i = start + 2; i < buffer.length - 1; i++) {
              if (buffer[i] === 0xFF && buffer[i+1] === 0xD9) { end = i + 2; break; }
            }
            if (end > start) {
              const jpegData = buffer.subarray(start, end);
              buffer = buffer.subarray(end);
              const blob = new Blob([jpegData], { type: 'image/jpeg' });
              createImageBitmap(blob).then(bmp => {
                if (pcCurrentFrame) try { pcCurrentFrame.close(); } catch(e) {}
                pcCurrentFrame = bmp;
              }).catch(() => {});
            }
          }
          if (buffer.length > 2 * 1024 * 1024) buffer = new Uint8Array(0);
        }
      } catch(e) {
        console.warn('MJPEG pump stopped:', e);
      }
    }

    function startPcRecording() {
      try {
        pcRecCanvas = document.createElement('canvas');
        pcRecCanvas.width = streamImg.naturalWidth || 1280;
        pcRecCanvas.height = streamImg.naturalHeight || 720;
        pcRecCtx = pcRecCanvas.getContext('2d');

        const stream = pcRecCanvas.captureStream(30);
        if (audioStreamTrack && audioStreamTrack.readyState === 'live') {
          stream.addTrack(audioStreamTrack);
        }

        let options = { mimeType: 'video/webm;codecs=vp9,opus' };
        if (!MediaRecorder.isTypeSupported(options.mimeType)) {
          options = { mimeType: 'video/webm;codecs=vp8,opus' };
        }
        if (!MediaRecorder.isTypeSupported(options.mimeType)) {
          options = { mimeType: 'video/webm' };
        }

        pcMediaRecorder = new MediaRecorder(stream, options);
        pcRecordedChunks = [];

        pcMediaRecorder.ondataavailable = e => {
          if (e.data && e.data.size > 0) pcRecordedChunks.push(e.data);
        };

        pcMediaRecorder.onstop = () => {
          const blob = new Blob(pcRecordedChunks, { type: pcMediaRecorder.mimeType || 'video/webm' });
          const now = new Date();
          const pad = n => String(n).padStart(2, '0');
          const ext = (pcMediaRecorder.mimeType && pcMediaRecorder.mimeType.includes('mp4')) ? 'mp4' : 'webm';
          const filename = 'ObsWifiCams_StreamRec_' + now.getFullYear() + pad(now.getMonth() + 1) + pad(now.getDate()) + '_' + pad(now.getHours()) + pad(now.getMinutes()) + pad(now.getSeconds()) + '.' + ext;
          downloadBlob(blob, filename);
          showToast('💾 PC Video saved: ' + filename);
        };

        isPcRecording = true;
        pcRecordStartTime = Date.now();
        pcMediaRecorder.start(1000);

        startMjpegFramePump();

        pcDrawInterval = setInterval(() => {
          if (!pcRecCtx) return;
          if (pcCurrentFrame) {
            if (pcRecCanvas.width !== pcCurrentFrame.width || pcRecCanvas.height !== pcCurrentFrame.height) {
              pcRecCanvas.width = pcCurrentFrame.width;
              pcRecCanvas.height = pcCurrentFrame.height;
            }
            pcRecCtx.drawImage(pcCurrentFrame, 0, 0);
          } else if (streamImg.complete && streamImg.naturalWidth > 0) {
            if (pcRecCanvas.width !== streamImg.naturalWidth || pcRecCanvas.height !== streamImg.naturalHeight) {
              pcRecCanvas.width = streamImg.naturalWidth;
              pcRecCanvas.height = streamImg.naturalHeight;
            }
            if (activeFilter && activeFilter !== 'none') {
              pcRecCtx.filter = getComputedStyle(streamImg).filter;
            }
            pcRecCtx.drawImage(streamImg, 0, 0);
          }
        }, 1000 / 30);

        showToast('⚪ PC Studio Recording started');
      } catch(e) {
        console.error('Failed to start PC recording:', e);
        isPcRecording = false;
        showToast('❌ Failed to start PC video recording');
      }
    }

    function stopPcRecording() {
      if (!isPcRecording) return;
      isPcRecording = false;
      if (pcMjpegAbort) { try { pcMjpegAbort.abort(); } catch(e) {} }
      if (pcDrawInterval) clearInterval(pcDrawInterval);
      if (pcTimerInterval) clearInterval(pcTimerInterval);
      if (pcMediaRecorder && pcMediaRecorder.state !== 'inactive') {
        pcMediaRecorder.stop();
      }
      if (pcCurrentFrame) { try { pcCurrentFrame.close(); } catch(e) {} pcCurrentFrame = null; }
    }

    let isStartingRecording = false;
    let startRecordReqTime = 0;
    let lastRecordDest = 'phone';

    function downloadUrl(url, filename) {
      const a = document.createElement('a');
      a.href = url + (url.includes('?') ? '&' : '?') + 't=' + Date.now();
      a.download = filename || 'ObsWifiCams_Video.mp4';
      document.body.appendChild(a);
      a.click();
      setTimeout(() => {
        try { document.body.removeChild(a); } catch(e) {}
      }, 1200);
    }

    function toggleRecording() {
      const isAnyRec = isPhoneRecording || isStartingRecording || isPcRecording;
      const dest = saveDestination;

      if (isAnyRec) {
        isStartingRecording = false;
        if (isPcRecording) stopPcRecording();
        if (isPhoneRecording) {
          postApi('/api/record').then(() => {
            setTimeout(updateStatus, 300);
            setTimeout(updateStatus, 800);
          });
        }
        updateRecordingUI();
      } else {
        initAudioGraph();
        if (audioCtx && audioCtx.state === 'suspended') {
          try { audioCtx.resume(); } catch(e) {}
        }
        lastRecordDest = dest;
        if (dest === 'both') {
          startPcRecording();
        }
        isStartingRecording = true;
        startRecordReqTime = Date.now();
        postApi('/api/record').then(() => {
          setTimeout(updateStatus, 400);
          setTimeout(updateStatus, 1200);
        });
        updateRecordingUI();
      }
    }

    function updateRecordingUI() {
      const recBtn = document.getElementById('recordBtn');
      const recTimer = document.getElementById('recTimer');
      const isAnyRec = isPhoneRecording || isStartingRecording || isPcRecording;

      if (isAnyRec) {
        if (recBtn) {
          recBtn.innerText = '⏹ Stop Recording';
          recBtn.classList.add('active');
        }
        if (recTimer) {
          recTimer.style.display = 'inline';
          let sec = phoneRecordingSeconds;
          let label = 'REC';
          if (isStartingRecording && !isPhoneRecording) {
            label = 'REC (Starting...)';
            sec = 0;
          }
          const m = Math.floor(sec / 60);
          const s = sec % 60;
          recTimer.innerText = label + ' ' + String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
        }
      } else {
        if (recBtn) {
          recBtn.classList.remove('active');
          updateShutterButtonLabels();
        }
        if (recTimer) {
          recTimer.style.display = 'none';
        }
      }
    }

    function setMode(mode) {
      postApi('/api/mode', { mode: mode }).then(() => {
        document.querySelectorAll('#modeChips .chip').forEach(c => {
          c.classList.toggle('active', c.getAttribute('data-mode') === mode);
        });
        updateStatus();
      });
    }

    let activeFilter = 'none';

    function setFilter(filterName) {
      activeFilter = filterName;
      streamImg.className = 'filter-' + filterName;

      document.querySelectorAll('#filterChips .chip').forEach(c => {
        c.classList.toggle('active', c.getAttribute('data-filter') === filterName);
      });

      postApi('/api/filter', { name: filterName });
    }

    function setQuality(q) {
      postApi('/api/quality', { quality: q }).then(() => {
        document.querySelectorAll('#qualityChips .chip').forEach(c => {
          c.classList.toggle('active', c.getAttribute('data-q') === q);
        });
        setTimeout(reconnectStream, 400);
      });
    }

    function setFps(f) {
      postApi('/api/fps', { fps: f }).then(() => {
        document.querySelectorAll('#fpsChips .chip').forEach(c => {
          c.classList.toggle('active', c.getAttribute('data-fps') === f);
        });
        const badge = document.getElementById('fpsTargetBadge');
        if (badge) badge.innerText = (f === 'auto') ? 'Auto FPS' : f + ' FPS';
      });
    }

    let autoAdjustEnabled = true;

    function toggleAutoAdjust() {
      autoAdjustEnabled = !autoAdjustEnabled;
      postApi('/api/auto-adjust', { enabled: autoAdjustEnabled }).then(() => updateStatus());
    }

    function flipCamera() {
      postApi('/api/switch-camera').then(() => setTimeout(updateStatus, 400));
    }

    function selectLens(id) {
      postApi('/api/select-camera', { id: id }).then(() => setTimeout(updateStatus, 400));
    }

    function setAspect(a) {
      postApi('/api/aspect', { aspect: a }).then(() => {
        document.querySelectorAll('#aspectChips .chip').forEach(c => {
          c.classList.toggle('active', c.getAttribute('data-aspect') === a);
        });
        updateStatus();
      });
    }

    function setZoom(z) {
      document.getElementById('zoomVal').innerText = parseFloat(z).toFixed(1) + '×';
      postApi('/api/zoom', { ratio: z });
    }

    function setExposure(ev) {
      document.getElementById('evVal').innerText = (ev > 0 ? '+' : '') + ev + ' EV';
      postApi('/api/exposure', { index: ev });
    }

    function toggleTorch() {
      postApi('/api/torch').then(() => updateStatus());
    }

    function setTorchStrength(lvl) {
      postApi('/api/torch-strength', { level: lvl });
    }

    function setFlash(f) {
      postApi('/api/flash', { option: f }).then(() => {
        document.querySelectorAll('#flashChips .chip').forEach(c => {
          c.classList.toggle('active', c.getAttribute('data-flash') === f);
        });
        updateStatus();
      });
    }

    let currentRotation = 0;

    function setRotation(deg) {
      deg = parseInt(deg) % 360;
      currentRotation = deg;
      postApi('/api/rotation', { degrees: deg }).then(() => {
        document.querySelectorAll('#rotationChips .chip').forEach(c => {
          c.classList.toggle('active', parseInt(c.getAttribute('data-rot')) === deg);
        });
        const rb = document.getElementById('rotBadge');
        if (rb) rb.innerText = deg + '°';
        setTimeout(reconnectStream, 300);
      });
    }

    function rotateNext() {
      const next = (currentRotation + 90) % 360;
      setRotation(next);
    }

    function setProIso(iso) {
      postApi('/api/pro-manual', { iso: iso });
    }

    function setProAwb(awb) {
      postApi('/api/pro-manual', { awb: awb });
    }

    function setProFocus(f) {
      postApi('/api/pro-manual', { focusDiopters: f });
    }

    // Telemetry polling
    function updateStatus() {
      fetch('/status')
        .then(r => r.json())
        .then(d => {
          if (d.fps !== undefined) document.getElementById('fpsVal').innerText = d.fps;
          if (d.resolution) document.getElementById('resBadge').innerText = d.resolution;
          if (d.facing) document.getElementById('facingBadge').innerText = d.facing;
          if (d.clients !== undefined) document.getElementById('clientsVal').innerText = d.clients;

          if (d.mode) {
            document.querySelectorAll('#modeChips .chip').forEach(c => {
              c.classList.toggle('active', c.getAttribute('data-mode') === d.mode);
            });
          }

          if (d.filter && d.filter !== activeFilter) {
            activeFilter = d.filter;
            streamImg.className = 'filter-' + d.filter;
            document.querySelectorAll('#filterChips .chip').forEach(c => {
              c.classList.toggle('active', c.getAttribute('data-filter') === d.filter);
            });
          }

          if (d.aspect) {
            document.querySelectorAll('#aspectChips .chip').forEach(c => {
              c.classList.toggle('active', c.getAttribute('data-aspect') === d.aspect);
            });
          }

          if (d.flash) {
            document.querySelectorAll('#flashChips .chip').forEach(c => {
              c.classList.toggle('active', c.getAttribute('data-flash') === d.flash);
            });
          }

          if (d.audioSupported) {
            document.getElementById('audioStat').innerText = 'Real-time';
            const lvl = d.audioLevel || 0;
            document.getElementById('vuFill').style.width = lvl + '%';
            document.getElementById('vuText').innerText = lvl + '%';
          } else {
            document.getElementById('audioStat').innerText = 'No Mic Perm';
          }

          if (d.quality) {
            document.querySelectorAll('#qualityChips .chip').forEach(c => {
              c.classList.toggle('active', c.getAttribute('data-q') === d.quality);
            });
          }

          if (d.targetFps) {
            document.querySelectorAll('#fpsChips .chip').forEach(c => {
              c.classList.toggle('active', c.getAttribute('data-fps') === d.targetFps);
            });
            const badge = document.getElementById('fpsTargetBadge');
            if (badge) badge.innerText = (d.targetFps === 'auto') ? 'Auto FPS' : d.targetFps + ' FPS';
          }

          if (d.rotation !== undefined) {
            currentRotation = d.rotation;
            document.querySelectorAll('#rotationChips .chip').forEach(c => {
              c.classList.toggle('active', parseInt(c.getAttribute('data-rot')) === d.rotation);
            });
            const rb = document.getElementById('rotBadge');
            if (rb) rb.innerText = d.rotation + '°';
          }

          const isoSel = document.getElementById('isoSelect');
          if (isoSel && !isoSel.matches(':focus')) {
            if (d.iso === null || d.iso === undefined || d.iso <= 0) {
              isoSel.value = 'auto';
            } else {
              let found = false;
              for (let i = 0; i < isoSel.options.length; i++) {
                if (parseInt(isoSel.options[i].value) === d.iso) {
                  isoSel.selectedIndex = i;
                  found = true;
                  break;
                }
              }
              if (!found) {
                const opt = document.createElement('option');
                opt.value = d.iso;
                opt.text = 'ISO ' + d.iso;
                isoSel.add(opt);
                isoSel.value = d.iso;
              }
            }
          }

          const wbSel = document.getElementById('wbSelect');
          if (wbSel && !wbSel.matches(':focus')) {
            if (d.awb === null || d.awb === undefined || d.awb === 1 || d.awb <= 0) {
              wbSel.value = 'auto';
            } else {
              wbSel.value = String(d.awb);
            }
          }

          if (d.autoAdjust !== undefined) {
            autoAdjustEnabled = d.autoAdjust;
            const btn = document.getElementById('autoAdjustBtn');
            if (btn) {
              btn.classList.toggle('active', autoAdjustEnabled);
              btn.innerText = autoAdjustEnabled ? '⚡ Auto-Smooth: ON' : '⚡ Auto-Smooth: OFF';
            }
          }

          const torchBtn = document.getElementById('torchBtn');
          if (d.isTorchOn) {
            torchBtn.style.background = '#cbd5e1';
            torchBtn.style.color = '#0f172a';
          } else {
            torchBtn.style.background = '';
            torchBtn.style.color = '';
          }

          if (d.torchStrengthMax > 1) {
            const ts = document.getElementById('torchStrengthSlider');
            ts.style.display = 'block';
            ts.max = d.torchStrengthMax;
            ts.value = d.torchStrength;
          }

          if (d.minZoom && d.maxZoom) {
            const zs = document.getElementById('zoomSlider');
            zs.min = d.minZoom;
            zs.max = d.maxZoom;
            if (!zs.matches(':active')) {
              zs.value = d.zoomRatio;
              document.getElementById('zoomVal').innerText = d.zoomRatio.toFixed(1) + '×';
            }
          }

          if (d.evMin !== undefined && d.evMax !== undefined) {
            const es = document.getElementById('evSlider');
            es.min = d.evMin;
            es.max = d.evMax;
            if (!es.matches(':active')) {
              es.value = d.evIndex;
              document.getElementById('evVal').innerText = (d.evIndex > 0 ? '+' : '') + d.evIndex + ' EV';
            }
          }

          const wasPhoneRecording = isPhoneRecording;
          if (isStartingRecording) {
            if (d.isRecording) {
              isStartingRecording = false;
              isPhoneRecording = true;
            } else if (Date.now() - startRecordReqTime > 7000) {
              isStartingRecording = false;
              isPhoneRecording = false;
            }
          } else {
            isPhoneRecording = !!d.isRecording;
          }
          phoneRecordingSeconds = d.recordingSeconds || 0;
          updateRecordingUI();

          if (wasPhoneRecording && !isPhoneRecording) {
            const pRow = document.getElementById('phoneVideoRow');
            if (pRow) pRow.style.display = 'block';

            if (saveDestination === 'both') {
              showToast('💾 Video finalized! Downloading MP4 to PC...');
              setTimeout(() => {
                const now = new Date();
                const pad = n => String(n).padStart(2, '0');
                const filename = 'ObsWifiCams_Video_' + now.getFullYear() + pad(now.getMonth() + 1) + pad(now.getDate()) + '_' + pad(now.getHours()) + pad(now.getMinutes()) + pad(now.getSeconds()) + '.mp4';
                downloadUrl('/latest-video.mp4', filename);
              }, 500);
            } else {
              showToast('📱 Video saved to Mobile gallery!');
            }
          }

          if (d.lenses && d.lenses.length > 0) {
            const ls = document.getElementById('lensSelect');
            if (ls.options.length <= 1) {
              ls.innerHTML = '<option value="">Default Lens</option>';
              d.lenses.forEach(lens => {
                const opt = document.createElement('option');
                opt.value = lens.id;
                opt.text = (lens.isFront ? 'Front: ' : 'Rear: ') + lens.name;
                ls.appendChild(opt);
              });
            }
          }
        })
        .catch(() => {});
    }

    function sendRewardTea() {
      postApi('/api/tea').then(() => showToast('☕ Tea reward sent to streamer!'));
    }

    function showToast(msg) {
      const t = document.getElementById('toast');
      if (!t) return;
      t.innerHTML = msg;
      t.classList.add('show');
      setTimeout(() => t.classList.remove('show'), 3000);
    }

    function toggleMirror() {
      viewport.classList.toggle('mirrored');
    }

    function reconnectStream() {
      streamImg.src = '/video.mjpeg?t=' + Date.now();
      showToast('🔄 Stream reloaded');
    }

    // Live Web Audio Engine (WAV stream decoding)
    let audioCtx = null;
    let audioStreamTrack = null;
    let isAudioPlaying = false;
    let audioGainNode = null;

    function toggleAudioPlayback() {
      if (isAudioPlaying) {
        stopAudioPlayback();
      } else {
        startAudioPlayback();
      }
    }

    function initAudioGraph() {
      if (!audioCtx) {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (AudioContext) {
          audioCtx = new AudioContext();
          audioGainNode = audioCtx.createGain();
          audioGainNode.connect(audioCtx.destination);
        }
      }
    }

    function setAudioVolume(val) {
      initAudioGraph();
      if (audioGainNode) {
        audioGainNode.gain.value = parseFloat(val);
      }
      const el = document.getElementById('audioPlayer');
      if (el) el.volume = parseFloat(val);
    }

    async function startAudioPlayback() {
      initAudioGraph();
      if (audioCtx && audioCtx.state === 'suspended') {
        try { await audioCtx.resume(); } catch(e) {}
      }

      const btn = document.getElementById('audioToggleBtn');
      const icon = document.getElementById('audioIcon');
      const text = document.getElementById('audioBtnText');

      try {
        const audioEl = document.getElementById('audioPlayer');
        audioEl.src = '/audio.wav?t=' + Date.now();
        await audioEl.play();
        isAudioPlaying = true;
        if (btn) btn.classList.add('active');
        if (icon) icon.innerText = '🔊';
        if (text) text.innerText = 'Live Audio';
        showToast('🔊 Live audio enabled');
      } catch(e) {
        console.warn('Audio play error:', e);
        showToast('⚠️ Click player to allow audio playback');
      }
    }

    function stopAudioPlayback() {
      const audioEl = document.getElementById('audioPlayer');
      if (audioEl) {
        audioEl.pause();
        audioEl.src = '';
      }
      isAudioPlaying = false;
      const btn = document.getElementById('audioToggleBtn');
      const icon = document.getElementById('audioIcon');
      const text = document.getElementById('audioBtnText');
      if (btn) btn.classList.remove('active');
      if (icon) icon.innerText = '🔈';
      if (text) text.innerText = 'Unmute';
    }

    // Start telemetry loop
    setInterval(updateStatus, 1000);
    updateStatus();
    updateShutterButtonLabels();
  </script>
</body>
</html>
        """.trimIndent()
    }

    fun getObsHtml(streamUrl: String, audioUrl: String): String = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>ObsWifiCams OBS Stream</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body { width: 100%; height: 100%; background: transparent; overflow: hidden; }
    img { width: 100%; height: 100%; object-fit: contain; }
  </style>
</head>
<body>
  <img src="$streamUrl" alt="Live Stream" />
  <audio src="$audioUrl" autoplay style="display:none;"></audio>
</body>
</html>
""".trimIndent()
}
