'use strict';
(() => {
  const $ = id => document.getElementById(id);
  const ui = Object.fromEntries(['dashboard','connectionDot','connectionLabel','miniWindow','mirrorBadge','stage','stageStatus','sizeSlider','sizeValue','audioSwitch','audioMessage','mirrorVolumeControl','volumeSlider','volumeValue','resetButton','toast','stick'].map(id => [id, $(id)]));
  let state = null, online = false, busy = false, pollBusy = false, sizing = false, adjustingVolume = false;
  let sequence = 0, applied = 0, toastTimer;
  const pending = new Map();
  let repeatTimer = null, holdDelay = null, heldButton = null, stickOrigin = null, vector = null;

  function network(ok, message) {
    online = ok;
    ui.connectionLabel.textContent = message;
    ui.connectionDot.parentElement.classList.toggle('online', ok);
    ui.dashboard.inert = !ok;
    if (!ok) stopHold();
  }
  function toast(message) {
    clearTimeout(toastTimer);
    ui.toast.textContent = message; ui.toast.hidden = false;
    toastTimer = setTimeout(() => { ui.toast.hidden = true; }, 3500);
  }
  async function api(path, method = 'GET', body) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 5000);
    const headers = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    try {
      const response = await fetch('/api/' + path, {method, headers, body: body === undefined ? undefined : JSON.stringify(body), signal: controller.signal, cache: 'no-store'});
      const result = await response.json();
      if (!response.ok) throw new Error(result.error || '操作未完成，请重试');
      return result;
    } finally { clearTimeout(timeout); }
  }
  function preview(overlay) {
    ui.stage.style.aspectRatio = overlay.screenWidth + ' / ' + overlay.screenHeight;
    ui.miniWindow.style.left = (overlay.x / overlay.screenWidth * 100) + '%';
    ui.miniWindow.style.top = (overlay.y / overlay.screenHeight * 100) + '%';
    ui.miniWindow.style.width = (overlay.width / overlay.screenWidth * 100) + '%';
    ui.miniWindow.style.height = (overlay.height / overlay.screenHeight * 100) + '%';
  }
  function sliderAppearance(value, overlay) {
    const min = overlay.minWidth, max = overlay.maxWidth;
    ui.sizeSlider.style.setProperty('--fill', ((value - min) / Math.max(1, max - min) * 100) + '%');
    ui.sizeValue.value = Math.round(value / overlay.screenWidth * 100) + '%';
  }
  function volumeAppearance(value) {
    ui.volumeSlider.style.setProperty('--fill', value + '%');
    ui.volumeValue.value = value + '%';
  }
  function apply(data, id) {
    if (id < applied) return;
    applied = id; state = data;
    network(true, '已连接电视');
    ui.dashboard.hidden = false;
    const overlay = data.overlay;
    const live = data.mirroring && overlay.visible;
    ui.miniWindow.classList.toggle('idle', !live);
    ui.mirrorBadge.classList.toggle('live', live);
    ui.mirrorBadge.textContent = live ? '正在投屏' : '等待投屏';
    ui.stageStatus.textContent = live ? '小窗位置 · 实时同步' : '未投屏 · 位置已保存';
    ui.stage.setAttribute('aria-label', `小窗位置预览：横坐标 ${overlay.x}，纵坐标 ${overlay.y}，宽度 ${overlay.width}`);
    ui.sizeSlider.min = overlay.minWidth; ui.sizeSlider.max = overlay.maxWidth;
    if (!sizing && !pending.has('size')) ui.sizeSlider.value = overlay.width;
    const width = Number(ui.sizeSlider.value);
    preview({...overlay, width, height: Math.floor(width * 9 / 16)});
    sliderAppearance(width, overlay);
    ui.audioSwitch.setAttribute('aria-checked', String(data.audio.source === 'mirror'));
    ui.audioMessage.textContent = data.audio.message;
    ui.mirrorVolumeControl.hidden = data.audio.source !== 'mirror';
    if (!adjustingVolume && !pending.has('volume')) ui.volumeSlider.value = data.audio.volume ?? 100;
    volumeAppearance(Number(ui.volumeSlider.value));
  }
  async function poll() {
    if (document.hidden || pollBusy || busy || pending.size) return;
    pollBusy = true;
    const id = ++sequence;
    try { apply(await api('state'), id); }
    catch (_) { network(false, '连接中断 · 正在重连'); }
    finally { pollBusy = false; }
  }
  function enqueue(action, data) {
    if (!online) return;
    if (action === 'reset') { pending.delete('move'); pending.delete('size'); }
    if (action === 'move' && pending.has('move')) {
      const previous = pending.get('move');
      data = {dx: Math.max(-96, Math.min(96, previous.dx + data.dx)), dy: Math.max(-96, Math.min(96, previous.dy + data.dy))};
    }
    pending.set(action, data);
    drain();
  }
  async function drain() {
    if (busy) return;
    busy = true;
    try {
      while (pending.size && online) {
        const [action, data] = pending.entries().next().value;
        pending.delete(action);
        const id = ++sequence;
        try {
          apply(await api(action, 'POST', data), id);
          if (action === 'reset') toast('已恢复右上角小窗');
        } catch (error) {
          pending.clear(); stopHold();
          network(false, '连接中断 · 正在重连'); toast(error.name === 'AbortError' ? '电视响应超时，请稍后重试' : error.message);
        }
      }
    } finally { busy = false; }
  }
  function move(dx, dy) { if (dx || dy) enqueue('move', {dx, dy}); }
  function stopHold() {
    clearInterval(repeatTimer); clearTimeout(holdDelay);
    repeatTimer = null; holdDelay = null; vector = null; stickOrigin = null;
    if (heldButton) heldButton.classList.remove('held');
    heldButton = null; ui.stick.style.transform = '';
    pending.delete('move');
  }
  document.querySelectorAll('.direction').forEach(button => {
    button.addEventListener('pointerdown', event => {
      if (!online || (event.pointerType === 'mouse' && event.button !== 0)) return;
      event.preventDefault(); stopHold();
      button.setPointerCapture(event.pointerId); heldButton = button; button.classList.add('held');
      const dx = Number(button.dataset.dx), dy = Number(button.dataset.dy);
      move(dx, dy);
      holdDelay = setTimeout(() => { repeatTimer = setInterval(() => move(dx, dy), 90); }, 240);
    });
    button.addEventListener('pointerup', stopHold);
    button.addEventListener('pointercancel', stopHold);
    button.addEventListener('lostpointercapture', stopHold);
    button.addEventListener('click', event => { if (event.detail === 0) move(Number(button.dataset.dx), Number(button.dataset.dy)); });
    button.addEventListener('contextmenu', event => event.preventDefault());
  });
  ui.stick.addEventListener('pointerdown', event => {
    if (!online || (event.pointerType === 'mouse' && event.button !== 0)) return;
    event.preventDefault(); stopHold();
    ui.stick.setPointerCapture(event.pointerId);
    stickOrigin = {x: event.clientX, y: event.clientY};
    repeatTimer = setInterval(() => { if (vector) move(vector.dx, vector.dy); }, 90);
  });
  ui.stick.addEventListener('pointermove', event => {
    if (!stickOrigin) return;
    let x = event.clientX - stickOrigin.x, y = event.clientY - stickOrigin.y;
    const distance = Math.hypot(x, y), radius = 26;
    if (distance > radius) { x *= radius / distance; y *= radius / distance; }
    vector = distance < 5 ? null : {dx: Math.round(x), dy: Math.round(y)};
    ui.stick.style.transform = `translate(${x}px, ${y}px)`;
  });
  ['pointerup','pointercancel','lostpointercapture'].forEach(name => ui.stick.addEventListener(name, stopHold));
  ui.stick.addEventListener('contextmenu', event => event.preventDefault());
  window.addEventListener('blur', stopHold);
  document.addEventListener('visibilitychange', () => { stopHold(); if (!document.hidden) poll(); });
  window.addEventListener('pagehide', stopHold);
  document.addEventListener('keydown', event => {
    if (!online || /INPUT|TEXTAREA|BUTTON/.test(document.activeElement.tagName) && document.activeElement !== ui.stick) return;
    const delta = {ArrowLeft: [-24,0], ArrowRight:[24,0], ArrowUp:[0,-24], ArrowDown:[0,24]}[event.key];
    if (delta) { event.preventDefault(); move(...delta); }
  });
  ui.sizeSlider.addEventListener('pointerdown', () => { sizing = true; });
  function finishSizing() { sizing = false; }
  ['pointerup','pointercancel','blur','lostpointercapture'].forEach(name => ui.sizeSlider.addEventListener(name, finishSizing));
  ui.sizeSlider.addEventListener('input', () => {
    if (!state) return;
    const width = Number(ui.sizeSlider.value);
    preview({...state.overlay, width, height: Math.floor(width * 9 / 16)});
    sliderAppearance(width, state.overlay);
    enqueue('size', {width});
  });
  ui.sizeSlider.addEventListener('change', () => { finishSizing(); enqueue('size', {width: Number(ui.sizeSlider.value)}); });
  ui.volumeSlider.addEventListener('pointerdown', () => { adjustingVolume = true; });
  ['pointerup','pointercancel','blur','lostpointercapture'].forEach(name => ui.volumeSlider.addEventListener(name, () => { adjustingVolume = false; }));
  ui.volumeSlider.addEventListener('input', () => {
    const volume = Number(ui.volumeSlider.value);
    volumeAppearance(volume);
    enqueue('volume', {volume});
  });
  ui.volumeSlider.addEventListener('change', () => {
    adjustingVolume = false;
    enqueue('volume', {volume: Number(ui.volumeSlider.value)});
  });
  ui.resetButton.addEventListener('click', () => { stopHold(); enqueue('reset', {}); });
  let suppressSoundClick = false;
  ui.audioSwitch.addEventListener('click', event => {
    if (suppressSoundClick) { suppressSoundClick = false; return; }
    let source;
    const rect = ui.audioSwitch.getBoundingClientRect();
    if (event.detail === 0) source = ui.audioSwitch.getAttribute('aria-checked') === 'true' ? 'tv' : 'mirror';
    else source = event.clientX - rect.left < rect.width / 2 ? 'tv' : 'mirror';
    ui.audioSwitch.setAttribute('aria-checked', String(source === 'mirror'));
    enqueue('audio', {source});
  });
  // A horizontal thumb swipe works as well as tapping either half.
  let soundDown = null;
  ui.audioSwitch.addEventListener('pointerdown', event => { suppressSoundClick = false; soundDown = event.clientX; });
  ui.audioSwitch.addEventListener('pointerup', event => {
    if (soundDown !== null && Math.abs(event.clientX - soundDown) > 25) {
      suppressSoundClick = true;
      const source = event.clientX > soundDown ? 'mirror' : 'tv';
      ui.audioSwitch.setAttribute('aria-checked', String(source === 'mirror'));
      enqueue('audio', {source});
    }
    soundDown = null;
  });
  ui.audioSwitch.addEventListener('pointercancel', () => { soundDown = null; });
  network(false, '正在连接');
  poll();
  setInterval(poll, 1500);
})();
