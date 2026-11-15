package com.stratatechlabs.obswificams.stream

/**
 * Serves responsive, modern dark-mode HTML5 web interfaces for browser viewing, OBS Studio,
 * and comprehensive remote camera studio controls with real-time audio and cinematic filters.
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
      --bg: #090d16;
      --card-bg: #131b2e;
      --card-border: #1e293b;
      --text: #f8fafc;
      --text-muted: #94a3b8;
      --accent: #38bdf8;
      --accent-hover: #0284c7;
      --live: #ef4444;
      --success: #22c55e;
      --warning: #f59e0b;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: var(--bg);
      color: var(--text);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      line-height: 1.4;
      padding: 12px;
    }
    .container {
      max-width: 1400px;
      margin: 0 auto;
    }
    header {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding-bottom: 12px;
      margin-bottom: 12px;
      border-bottom: 1px solid var(--card-border);
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
    }
    .badge-live {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: rgba(239, 68, 68, 0.15);
      color: var(--live);
      border: 1px solid var(--live);
      border-radius: 999px;
      padding: 2px 10px;
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
    }
    .dot {
      width: 8px;
      height: 8px;
      background: var(--live);
      border-radius: 50%;
      animation: pulse 1.5s infinite;
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
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 6px;
      padding: 3px 8px;
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    .stat-pill strong {
      color: var(--accent);
    }
    .tea-btn {
      background: rgba(245, 158, 11, 0.15) !important;
      border: 1px solid rgba(245, 158, 11, 0.4) !important;
      color: #fbbf24 !important;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: all 0.2s ease;
    }
    .tea-btn:hover {
      background: #f59e0b !important;
      color: #090d16 !important;
      transform: translateY(-1px);
    }
    .tea-btn strong {
      color: inherit !important;
    }
    .tea-toast {
      position: fixed;
      bottom: 24px;
      right: 24px;
      background: #1e293b;
      color: #f8fafc;
      border: 1px solid #f59e0b;
      border-radius: 8px;
      padding: 12px 20px;
      font-size: 0.9rem;
      display: flex;
      align-items: center;
      gap: 10px;
      box-shadow: 0 10px 25px rgba(0,0,0,0.5);
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
        grid-template-columns: 1.7fr 1.1fr;
      }
    }

    /* Video player */
    .video-panel {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .video-viewport {
      position: relative;
      background: #000;
      border-radius: 12px;
      overflow: hidden;
      aspect-ratio: 16 / 9;
      border: 1px solid var(--card-border);
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .video-viewport img {
      width: 100%;
      height: 100%;
      object-fit: contain;
      transition: transform 0.2s ease, filter 0.2s ease;
    }
    .video-viewport.mirrored img {
      transform: scaleX(-1);
    }

    /* Live Filters - Stream is hardware-graded by camera server; no double-filtering */
    .filter-none,
    .filter-bluray,
    .filter-tealorange,
    .filter-vivid,
    .filter-vintage,
    .filter-noir,
    .filter-cyberpunk,
    .filter-invert { filter: none !important; }

    /* Audio Bar */
    .audio-toolbar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 10px;
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 10px;
      padding: 8px 12px;
    }
    .audio-btn {
      background: #1e293b;
      border: 1px solid var(--card-border);
      color: var(--text);
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 0.8rem;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: all 0.2s;
    }
    .audio-btn:hover { background: #334155; }
    .audio-btn.active {
      background: var(--success);
      color: #090d16;
      font-weight: 700;
      border-color: var(--success);
    }
    .vu-meter-container {
      display: flex;
      align-items: center;
      gap: 6px;
      flex: 1;
      min-width: 120px;
    }
    .vu-meter-label {
      font-size: 0.72rem;
      color: var(--text-muted);
      text-transform: uppercase;
    }
    .vu-meter-bar {
      flex: 1;
      height: 8px;
      background: #090d16;
      border-radius: 4px;
      overflow: hidden;
      border: 1px solid var(--card-border);
      position: relative;
    }
    .vu-meter-fill {
      height: 100%;
      width: 0%;
      background: linear-gradient(to right, #22c55e, #eab308 75%, #ef4444 95%);
      transition: width 0.08s linear;
    }

    /* Controls Panel */
    .controls-panel {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 12px;
      padding: 14px;
      display: flex;
      flex-direction: column;
      gap: 14px;
      max-height: 88vh;
      overflow-y: auto;
    }
    .panel-section {
      border: 1px solid var(--card-border);
      border-radius: 8px;
      padding: 12px;
      background: rgba(15, 23, 42, 0.4);
    }
    .panel-section h3 {
      font-size: 0.85rem;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--accent);
      margin-bottom: 10px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .control-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }
    .control-row:last-child { margin-bottom: 0; }
    .control-label {
      font-size: 0.8rem;
      color: var(--text-muted);
      min-width: 70px;
    }
    .chip-group {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      flex: 1;
    }
    .chip {
      background: #1e293b;
      color: var(--text);
      border: 1px solid var(--card-border);
      border-radius: 6px;
      padding: 5px 10px;
      font-size: 0.78rem;
      cursor: pointer;
      transition: all 0.15s;
    }
    .chip:hover { background: #334155; }
    .chip.active {
      background: var(--accent);
      color: #090d16;
      border-color: var(--accent);
      font-weight: 600;
    }
    .chip-record {
      background: rgba(239, 68, 68, 0.2);
      border-color: var(--live);
      color: var(--live);
    }
    .chip-record.active {
      background: var(--live);
      color: #fff;
      font-weight: 700;
      animation: pulse 1.5s infinite;
    }

    input[type=range] {
      flex: 1;
      accent-color: var(--accent);
      cursor: pointer;
    }
    .slider-val {
      font-size: 0.8rem;
      font-family: monospace;
      color: var(--accent);
      min-width: 45px;
      text-align: right;
    }

    .btn-action {
      background: #1e293b;
      color: var(--text);
      border: 1px solid var(--card-border);
      border-radius: 6px;
      padding: 8px 12px;
      font-size: 0.85rem;
      font-weight: 500;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      transition: all 0.2s;
    }
    .btn-action:hover { background: #334155; border-color: var(--accent); }
    .btn-capture {
      background: var(--accent);
      color: #090d16;
      font-weight: 700;
      border-color: var(--accent);
    }
    .btn-capture:hover { background: var(--accent-hover); }

    /* Stream URL Copy boxes */
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
      font-weight: 600;
    }
    .url-input-wrap {
      display: flex;
      gap: 4px;
    }
    .url-input-wrap input {
      flex: 1;
      background: #090d16;
      border: 1px solid var(--card-border);
      color: var(--text);
      padding: 5px 8px;
      border-radius: 6px;
      font-family: monospace;
      font-size: 0.75rem;
    }
    .url-input-wrap button {
      padding: 5px 10px;
      background: #1e293b;
      border: 1px solid var(--card-border);
      border-radius: 6px;
      color: var(--text);
      font-size: 0.75rem;
      cursor: pointer;
    }
    .url-input-wrap button:hover { background: #334155; }
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
      <!-- Left Column: Video Viewport + Audio Controls -->
      <div class="video-panel">
        <div class="video-viewport" id="viewport">
          <img id="streamImg" class="filter-none" src="/video.mjpeg" alt="Live Camera Video Stream" />
        </div>

        <!-- Audio & Stream Control Bar -->
        <div class="audio-toolbar">
          <button class="audio-btn" id="audioToggleBtn" onclick="toggleAudioPlayback()">
            <span id="audioIcon">🔈</span> <span id="audioBtnText">Unmute Real-Time Audio</span>
          </button>

          <input type="range" id="volumeSlider" min="0" max="1" step="0.05" value="1" oninput="setAudioVolume(this.value)" style="max-width: 90px;" title="Audio Volume">

          <div class="vu-meter-container">
            <span class="vu-meter-label">Mic VU</span>
            <div class="vu-meter-bar">
              <div class="vu-meter-fill" id="vuFill"></div>
            </div>
            <span class="slider-val" id="vuText" style="min-width: 32px;">0%</span>
          </div>

          <button class="audio-btn tea-btn" onclick="sendRewardTea()" title="Reward Tea to Streamer">
            <svg class="tea-icon" viewBox="0 0 24 24" width="14" height="14" fill="currentColor" style="vertical-align:middle; margin-right:4px;">
              <path d="M6,2c0.55,0 1,0.45 1,1v1c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1V3C5,2.45 5.45,2 6,2zM10,1c0.55,0 1,0.45 1,1v2c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1V2C9,1.45 9.45,1 10,1zM14,2c0.55,0 1,0.45 1,1v1c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1V3C13,2.45 13.45,2 14,2z" />
              <path d="M20,6H4v8c0,2.21 1.79,4 4,4h6c2.21,0 4,-1.79 4,-4v-2h2c1.1,0 2,-0.9 2,-2V8C22,6.9 21.1,6 20,6zM20,10h-2V8h2V10zM2,20h18c0.55,0 1,0.45 1,1s-0.45,1 -1,1H2c-0.55,0 -1,-0.45 -1,-1S1.45,20 2,20z" />
            </svg>Reward Tea
          </button>

          <button class="audio-btn" onclick="toggleMirror()" title="Flip horizontally">
            <span>⇄</span> Mirror
          </button>
          <button class="audio-btn" onclick="rotateNext()" title="Rotate Stream 90°">
            <span>⟳</span> Rotate <span id="rotBadge" style="font-size:0.75rem; color:var(--accent);">0°</span>
          </button>
          <button class="audio-btn" onclick="toggleFullscreen()" title="Fullscreen">
            <span>⛶</span> Fullscreen
          </button>
          <button class="audio-btn" onclick="reconnectStream()" title="Reload video and audio">
            <span>⟳</span> Reload
          </button>
        </div>

        <!-- Hidden audio element as fallback -->
        <audio id="audioPlayer" src="/audio.wav" preload="none" style="display:none;"></audio>
      </div>

      <!-- Right Column: All Tools & Controls -->
      <div class="controls-panel">
        <!-- Section 1: Camera Mode & Shutter -->
        <div class="panel-section">
          <h3><span>Camera Mode & Shutter</span> <span id="recTimer" style="color:var(--live); display:none; font-weight:700;">REC 00:00</span></h3>
          <div class="control-row" style="margin-bottom:10px;">
            <span class="control-label">Mode:</span>
            <div class="chip-group" id="modeChips">
              <div class="chip active" data-mode="photo" onclick="setMode('photo')">Photo</div>
              <div class="chip" data-mode="video" onclick="setMode('video')">Video</div>
              <div class="chip" data-mode="pro" onclick="setMode('pro')">Pro Manual</div>
            </div>
          </div>
          <div style="display:grid; grid-template-columns: 1fr 1fr; gap:8px;">
            <button class="btn-action btn-capture" onclick="takeSnapshot()">📸 Take Photo</button>
            <button class="btn-action chip-record" id="recordBtn" onclick="toggleRecording()">⏺ Record Video</button>
          </div>
        </div>

        <!-- Section 2: Filters & Color Grading (Blu-Ray & More) -->
        <div class="panel-section">
          <h3>Filters & Color Grading</h3>
          <div class="chip-group" id="filterChips">
            <div class="chip active" data-filter="none" onclick="setFilter('none')">Normal</div>
            <div class="chip" data-filter="bluray" onclick="setFilter('bluray')">💿 Blu-Ray</div>
            <div class="chip" data-filter="tealorange" onclick="setFilter('tealorange')">🎬 Teal & Orange</div>
            <div class="chip" data-filter="vivid" onclick="setFilter('vivid')">🌈 Vivid</div>
            <div class="chip" data-filter="vintage" onclick="setFilter('vintage')">🎞 Vintage</div>
            <div class="chip" data-filter="noir" onclick="setFilter('noir')">🕶 Noir B&W</div>
            <div class="chip" data-filter="cyberpunk" onclick="setFilter('cyberpunk')">⚡ Cyberpunk</div>
            <div class="chip" data-filter="invert" onclick="setFilter('invert')">🔄 Invert</div>
          </div>
        </div>

        <!-- Section 3: Stream Quality Selection (Auto, 4K, 2K, 1080p, 720p, 480p) + Auto-Adjust -->
        <div class="panel-section">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
            <h3 style="margin:0;">Stream Resolution</h3>
            <button class="chip active" id="autoAdjustBtn" onclick="toggleAutoAdjust()" style="margin:0; font-size:11px; padding:3px 10px; cursor:pointer;" title="Automatically adjusts compression and bitrate for lag-free Wi-Fi stream">
              ⚡ Auto-Smooth: ON
            </button>
          </div>
          <div class="chip-group" id="qualityChips">
            <div class="chip" data-q="auto" onclick="setQuality('auto')">Auto Resolution</div>
            <div class="chip" data-q="4k" onclick="setQuality('4k')">4K UHD</div>
            <div class="chip" data-q="2k" onclick="setQuality('2k')">2K QHD</div>
            <div class="chip" data-q="1080p" onclick="setQuality('1080p')">1080p FHD</div>
            <div class="chip active" data-q="720p" onclick="setQuality('720p')">720p HD</div>
            <div class="chip" data-q="480p" onclick="setQuality('480p')">480p SD</div>
          </div>
        </div>

        <!-- Section 3b: Frame Rate Selection (Auto, 120, 60, 30, 24, 15 FPS) -->
        <div class="panel-section">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
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

        <!-- Section 4: Camera, Lens, Aspect Ratio & Orientation -->
        <div class="panel-section">
          <h3>Camera, Lenses & Frame</h3>
          <div class="control-row">
            <span class="control-label">Facing:</span>
            <div class="chip-group">
              <button class="btn-action" style="padding:4px 10px; font-size:0.8rem;" onclick="flipCamera()">🔄 Flip Camera</button>
              <select id="lensSelect" onchange="selectLens(this.value)" style="background:#1e293b; color:#fff; border:1px solid var(--card-border); border-radius:6px; padding:4px 8px; font-size:0.8rem;">
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
        </div>

        <!-- Section 5: Zoom & Exposure (EV) -->
        <div class="panel-section">
          <h3>Zoom & Exposure</h3>
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
        </div>

        <!-- Section 6: Lighting (Torch & Flash) -->
        <div class="panel-section">
          <h3>Lighting & Flash</h3>
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

        <!-- Section 7: Pro Manual Controls -->
        <div class="panel-section">
          <h3>Pro Manual Settings</h3>
          <div class="control-row">
            <span class="control-label">ISO:</span>
            <select id="isoSelect" onchange="setProIso(this.value)" style="background:#1e293b; color:#fff; border:1px solid var(--card-border); border-radius:6px; padding:4px 8px; font-size:0.8rem;">
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
            <select id="wbSelect" onchange="setProAwb(this.value)" style="background:#1e293b; color:#fff; border:1px solid var(--card-border); border-radius:6px; padding:4px 8px; font-size:0.8rem;">
              <option value="auto">Auto AWB</option>
              <option value="2">Incandescent (Bulb)</option>
              <option value="3">Fluorescent</option>
              <option value="4">Warm Fluorescent</option>
              <option value="5">Daylight (Sun)</option>
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
        </div>

        <!-- Section 8: Streaming URLs (OBS & VLC) -->
        <div class="panel-section">
          <h3>OBS Studio & Streaming URLs</h3>
          <div class="url-box">
            <label>OBS Video Source (Media Source / VLC)</label>
            <div class="url-input-wrap">
              <input type="text" readonly value="$streamUrl" id="obsVideoInput">
              <button onclick="copyText('obsVideoInput', this)">Copy</button>
            </div>
          </div>
          <div class="url-box">
            <label>OBS Audio Source (Media Source / VLC)</label>
            <div class="url-input-wrap">
              <input type="text" readonly value="$audioUrl" id="obsAudioInput">
              <button onclick="copyText('obsAudioInput', this)">Copy</button>
            </div>
          </div>
          <div class="url-box">
            <label>OBS Clean Browser Source (Video + Audio combined)</label>
            <div class="url-input-wrap">
              <input type="text" readonly value="$obsUrl" id="obsCleanInput">
              <button onclick="copyText('obsCleanInput', this)">Copy</button>
            </div>
          </div>
          <div class="url-box">
            <label>Live JPEG Snapshot (HTTP Image / Static overlay)</label>
            <div class="url-input-wrap">
              <input type="text" readonly value="$snapshotUrl" id="obsSnapshotInput">
              <button onclick="copyText('obsSnapshotInput', this)">Copy</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <script>
    const streamImg = document.getElementById('streamImg');
    const viewport = document.getElementById('viewport');
    const audioPlayer = document.getElementById('audioPlayer');

    // -------------------------------------------------------------
    // Real-Time Ultra-Low Latency Web Audio Engine (<50ms sync)
    // -------------------------------------------------------------
    let audioCtx = null;
    let audioGain = null;
    let audioReader = null;
    let nextAudioPlayTime = 0;
    let isRealtimeAudioActive = false;

    function updateAudioBtnUI(active) {
      const btn = document.getElementById('audioToggleBtn');
      const icon = document.getElementById('audioIcon');
      const text = document.getElementById('audioBtnText');
      if (active) {
        btn.classList.add('active');
        icon.innerText = '🔊';
        text.innerText = 'Mute Real-Time Audio';
      } else {
        btn.classList.remove('active');
        icon.innerText = '🔈';
        text.innerText = 'Unmute Real-Time Audio';
      }
    }

    async function startRealtimeAudio() {
      try {
        if (!audioCtx) {
          audioCtx = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 44100 });
          audioGain = audioCtx.createGain();
          audioGain.connect(audioCtx.destination);
          setAudioVolume(document.getElementById('volumeSlider').value);
        }
        if (audioCtx.state === 'suspended') {
          await audioCtx.resume();
        }

        isRealtimeAudioActive = true;
        updateAudioBtnUI(true);

        const response = await fetch('/audio.wav?t=' + Date.now());
        if (!response.body) {
          // Fallback to standard audio tag if fetch streaming unsupported
          audioPlayer.src = '/audio.wav?t=' + Date.now();
          audioPlayer.play().catch(() => {});
          return;
        }

        const reader = response.body.getReader();
        audioReader = reader;

        let headerSkipped = false;
        let leftoverBytes = new Uint8Array(0);

        while (isRealtimeAudioActive) {
          const { done, value } = await reader.read();
          if (done) break;
          if (!value || value.length === 0) continue;

          let chunk = value;
          if (!headerSkipped) {
            if (chunk.length < 44) continue;
            chunk = chunk.subarray(44); // Skip 44-byte WAV header
            headerSkipped = true;
          }

          let full;
          if (leftoverBytes.length > 0) {
            full = new Uint8Array(leftoverBytes.length + chunk.length);
            full.set(leftoverBytes);
            full.set(chunk, leftoverBytes.length);
            leftoverBytes = new Uint8Array(0);
          } else {
            full = chunk;
          }

          const usableLen = full.length - (full.length % 2);
          if (full.length % 2 !== 0) {
            leftoverBytes = full.subarray(usableLen);
          }

          const sampleCount = usableLen / 2;
          if (sampleCount === 0) continue;

          // Convert 16-bit PCM little endian to Float32
          const int16View = new Int16Array(full.buffer, full.byteOffset, sampleCount);
          const floatBuffer = new Float32Array(sampleCount);
          for (let i = 0; i < sampleCount; i++) {
            floatBuffer[i] = int16View[i] / 32768.0;
          }

          const audioBuf = audioCtx.createBuffer(1, sampleCount, 44100);
          audioBuf.copyToChannel(floatBuffer, 0);

          const source = audioCtx.createBufferSource();
          source.buffer = audioBuf;
          source.connect(audioGain);

          const currentTime = audioCtx.currentTime;
          // Maintain low-latency window: snap to real-time if lag exceeds 70ms
          if (nextAudioPlayTime < currentTime || nextAudioPlayTime > currentTime + 0.07) {
            nextAudioPlayTime = currentTime + 0.015; // 15ms safety jitter buffer
          }

          source.start(nextAudioPlayTime);
          nextAudioPlayTime += audioBuf.duration;
        }
      } catch (err) {
        console.warn('Real-time audio stream error:', err);
      } finally {
        stopRealtimeAudio();
      }
    }

    function stopRealtimeAudio() {
      isRealtimeAudioActive = false;
      if (audioReader) {
        try { audioReader.cancel(); } catch(e) {}
        audioReader = null;
      }
      try { audioPlayer.pause(); audioPlayer.src = ''; } catch(e) {}
      updateAudioBtnUI(false);
    }

    function toggleAudioPlayback() {
      if (isRealtimeAudioActive) {
        stopRealtimeAudio();
      } else {
        startRealtimeAudio();
      }
    }

    function setAudioVolume(val) {
      const v = parseFloat(val);
      if (audioGain) audioGain.gain.value = v;
      if (audioPlayer) audioPlayer.volume = v;
    }

    function reconnectStream() {
      streamImg.src = '/video.mjpeg?t=' + Date.now();
      if (isRealtimeAudioActive) {
        stopRealtimeAudio();
        setTimeout(startRealtimeAudio, 250);
      }
    }

    streamImg.onerror = () => setTimeout(reconnectStream, 1500);

    function toggleMirror() {
      viewport.classList.toggle('mirrored');
    }

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

    function takeSnapshot() {
      // Trigger capture on device camera
      postApi('/api/capture').then(() => {
        // Also download live frame locally for user convenience
        const a = document.createElement('a');
        a.href = '/snapshot.jpg?t=' + Date.now();
        a.download = 'ObsWifiCams_Photo_' + Date.now() + '.jpg';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      });
    }

    function toggleRecording() {
      postApi('/api/record').then(() => updateStatus());
    }

    function setMode(mode) {
      postApi('/api/mode', { mode: mode }).then(() => {
        document.querySelectorAll('#modeChips .chip').forEach(c => {
          c.classList.toggle('active', c.getAttribute('data-mode') === mode);
        });
        updateStatus();
      });
    }

    // Filter controls (Blu-Ray, Teal & Orange, Vivid, Vintage, Noir, Cyberpunk, Invert)
    let activeFilter = 'none';

    function setFilter(filterName) {
      activeFilter = filterName;
      // Instant browser CSS filter
      streamImg.className = 'filter-' + filterName;

      // Update active chip
      document.querySelectorAll('#filterChips .chip').forEach(c => {
        c.classList.toggle('active', c.getAttribute('data-filter') === filterName);
      });

      // Synchronize with phone encoder for MJPEG stream output
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

          // Mode chip synchronization
          if (d.mode) {
            document.querySelectorAll('#modeChips .chip').forEach(c => {
              c.classList.toggle('active', c.getAttribute('data-mode') === d.mode);
            });
          }

          // Filter synchronization
          if (d.filter && d.filter !== activeFilter) {
            activeFilter = d.filter;
            streamImg.className = 'filter-' + d.filter;
            document.querySelectorAll('#filterChips .chip').forEach(c => {
              c.classList.toggle('active', c.getAttribute('data-filter') === d.filter);
            });
          }

          // Aspect synchronization
          if (d.aspect) {
            document.querySelectorAll('#aspectChips .chip').forEach(c => {
              c.classList.toggle('active', c.getAttribute('data-aspect') === d.aspect);
            });
          }

          // Flash synchronization
          if (d.flash) {
            document.querySelectorAll('#flashChips .chip').forEach(c => {
              c.classList.toggle('active', c.getAttribute('data-flash') === d.flash);
            });
          }

          // Audio telemetry & VU meter
          if (d.audioSupported) {
            document.getElementById('audioStat').innerText = 'Real-time';
            const lvl = d.audioLevel || 0;
            document.getElementById('vuFill').style.width = lvl + '%';
            document.getElementById('vuText').innerText = lvl + '%';
          } else {
            document.getElementById('audioStat').innerText = 'No Mic Perm';
          }

          // Quality active chip
          if (d.quality) {
            document.querySelectorAll('#qualityChips .chip').forEach(c => {
              c.classList.toggle('active', c.getAttribute('data-q') === d.quality);
            });
          }

          // Frame rate (FPS) active chip
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

          // Torch & Flash state
          const torchBtn = document.getElementById('torchBtn');
          if (d.isTorchOn) {
            torchBtn.style.background = 'var(--accent)';
            torchBtn.style.color = '#090d16';
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

          // Zoom limits
          if (d.minZoom && d.maxZoom) {
            const zs = document.getElementById('zoomSlider');
            zs.min = d.minZoom;
            zs.max = d.maxZoom;
            if (!zs.matches(':active')) {
              zs.value = d.zoomRatio;
              document.getElementById('zoomVal').innerText = d.zoomRatio.toFixed(1) + '×';
            }
          }

          // EV limits
          if (d.evMin !== undefined && d.evMax !== undefined) {
            const es = document.getElementById('evSlider');
            es.min = d.evMin;
            es.max = d.evMax;
            if (!es.matches(':active')) {
              es.value = d.evIndex;
              document.getElementById('evVal').innerText = (d.evIndex > 0 ? '+' : '') + d.evIndex + ' EV';
            }
          }

          // Recording state
          const recBtn = document.getElementById('recordBtn');
          const recTimer = document.getElementById('recTimer');
          if (d.isRecording) {
            recBtn.innerText = '⏹ Stop Recording';
            recBtn.classList.add('active');
            recTimer.style.display = 'inline';
            const m = Math.floor(d.recordingSeconds / 60);
            const s = d.recordingSeconds % 60;
            recTimer.innerText = 'REC ' + String(m).padStart(2,'0') + ':' + String(s).padStart(2,'0');
          } else {
            recBtn.innerText = '⏺ Record Video';
            recBtn.classList.remove('active');
            recTimer.style.display = 'none';
          }

          // Lens list population
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
      fetch('/api/tea', { method: 'POST' })
        .then(r => r.json())
        .then(d => {
          showTeaToast('🍵 Thank you for the tea! Reward sent to camera!');
        })
        .catch(() => {
          showTeaToast('🍵 Thank you for the tea! Reward sent to camera!');
        });
    }

    function showTeaToast(msg) {
      let t = document.getElementById('teaToast');
      if (!t) {
        t = document.createElement('div');
        t.id = 'teaToast';
        t.className = 'tea-toast';
        document.body.appendChild(t);
      }
      t.innerHTML = msg;
      t.classList.add('show');
      setTimeout(() => t.classList.remove('show'), 3500);
    }

    setInterval(updateStatus, 800);
    updateStatus();
  </script>
</body>
</html>
""".trimIndent()
    }

    fun getObsHtml(streamUrl: String, audioUrl: String): String {
        return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>ObsWifiCams OBS Stream</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body {
      width: 100%;
      height: 100%;
      overflow: hidden;
      background-color: #000;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
    .filter-none,
    .filter-bluray,
    .filter-tealorange,
    .filter-vivid,
    .filter-vintage,
    .filter-noir,
    .filter-cyberpunk,
    .filter-invert { filter: none !important; }
  </style>
</head>
<body>
  <img id="stream" src="$streamUrl" alt="Live Camera Video" />
  <audio id="audio" src="$audioUrl" autoplay></audio>
  <script>
    const img = document.getElementById('stream');
    const audio = document.getElementById('audio');
    
    // Check if filter param exists in URL, e.g. ?filter=bluray
    const params = new URLSearchParams(window.location.search);
    const filter = params.get('filter');
    if (filter) {
      fetch('/api/filter?name=' + encodeURIComponent(filter), { method: 'POST' }).catch(()=>{});
    }

    img.onerror = () => {
      setTimeout(() => { img.src = '$streamUrl?t=' + Date.now(); }, 1500);
    };
    audio.onerror = () => {
      setTimeout(() => { audio.src = '$audioUrl?t=' + Date.now(); audio.play().catch(()=>{}); }, 1500);
    };
  </script>
</body>
</html>
""".trimIndent()
    }
}
