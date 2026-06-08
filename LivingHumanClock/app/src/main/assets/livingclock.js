(function () {
  'use strict';

  var plane = document.getElementById('plane');
  var SVG_NS = 'http://www.w3.org/2000/svg';

  // ---------------------------------------------------------------
  // Polar helpers — angle 0 = 12 o'clock, increases clockwise.
  // Radius is given as a percentage of the dial's half-width.
  // Returns left/top as CSS percentage strings anchored to the plane.
  // ---------------------------------------------------------------
  function polar(angleDeg, radiusPercent) {
    var rad = (angleDeg - 0) * Math.PI / 180;
    var x = 50 + radiusPercent / 2 * Math.sin(rad);
    var y = 50 - radiusPercent / 2 * Math.cos(rad);
    return { left: x + '%', top: y + '%' };
  }

  function place(el, angleDeg, radiusPercent) {
    var p = polar(angleDeg, radiusPercent);
    el.style.left = p.left;
    el.style.top = p.top;
  }

  // ---------------------------------------------------------------
  // Numbers 1–12, brushed gold, always on the outer rim
  // ---------------------------------------------------------------
  var numbersLayer = document.getElementById('numbers');
  for (var n = 1; n <= 12; n++) {
    var el = document.createElement('div');
    el.className = 'number';
    var span = document.createElement('span');
    span.textContent = String(n);
    el.appendChild(span);
    var p = polar(n * 30, 100);
    el.style.left = p.left;
    el.style.top = p.top;
    el.style.transform = 'translate(-50%,-50%)';
    numbersLayer.appendChild(el);
  }

  // ---------------------------------------------------------------
  // Ring guide tracks (visual circles for hour / minute / second paths)
  // ---------------------------------------------------------------
  function sizeTrack(id, radiusPercent) {
    var t = document.getElementById(id);
    var d = radiusPercent * 2;
    t.style.width = d + '%';
    t.style.height = d + '%';
    t.style.marginLeft = (-radiusPercent) + '%';
    t.style.marginTop = (-radiusPercent) + '%';
  }
  sizeTrack('trackHour', 65);
  sizeTrack('trackMinute', 85);
  sizeTrack('trackSecond', 95);

  // ---------------------------------------------------------------
  // Park landscaping — bushes, flowers, grass tufts and golden lamps
  // scattered evenly around the perimeter, just outside the number ring
  // ---------------------------------------------------------------
  var landscape = document.getElementById('landscape');
  var lampCount = 12;
  for (var i = 0; i < lampCount; i++) {
    var lamp = document.createElement('div');
    lamp.className = 'lamp';
    var lp = polar(i * 30 + 15, 99);
    lamp.style.left = lp.left;
    lamp.style.top = lp.top;
    lamp.style.transform = 'translate(-50%,-50%)';
    lamp.style.animationDelay = (i * 0.35) + 's';
    landscape.appendChild(lamp);
  }
  var greenCount = 30;
  for (var g = 0; g < greenCount; g++) {
    var kind = g % 3;
    var item = document.createElement('div');
    var radiusP = 91 + (g % 4);
    var ang = g * (360 / greenCount) + (g % 2 ? 4 : -4);
    var pp = polar(ang, radiusP);
    item.style.left = pp.left;
    item.style.top = pp.top;
    item.style.transform = 'translate(-50%,-50%) scale(' + (0.8 + (g % 5) * 0.08) + ')';
    if (kind === 0) item.className = 'bush';
    else if (kind === 1) item.className = 'flower';
    else item.className = 'grass';
    landscape.appendChild(item);
  }

  // ---------------------------------------------------------------
  // Stylised human figures (built as small SVGs, animated via CSS)
  // Each figure faces outward/tangentially and is anchored at the feet.
  // ---------------------------------------------------------------
  function svgEl(tag, attrs) {
    var el = document.createElementNS(SVG_NS, tag);
    for (var k in attrs) el.setAttribute(k, attrs[k]);
    return el;
  }

  // Builds a simple jointed figure: head, torso, two arms, two legs.
  // colors: {skin, hair, coat, accent, accent2}
  function buildFigure(kind, colors) {
    var svg = svgEl('svg', { viewBox: '0 0 40 84', xmlns: SVG_NS });

    var grp = svgEl('g', {});
    svg.appendChild(grp);

    // Legs (drawn first so torso overlaps hips)
    var legL = svgEl('g', { class: 'limb leg-l' });
    var legR = svgEl('g', { class: 'limb leg-r' });
    legL.setAttribute('style', 'transform-origin:14px 50px');
    legR.setAttribute('style', 'transform-origin:26px 50px');
    legL.appendChild(svgEl('rect', { x: 11, y: 50, width: 6, height: 28, rx: 3, fill: colors.coat2 || colors.coat }));
    legR.appendChild(svgEl('rect', { x: 23, y: 50, width: 6, height: 28, rx: 3, fill: colors.coat2 || colors.coat }));
    // shoes / boots — small accent
    legL.appendChild(svgEl('ellipse', { cx: 14, cy: 80, rx: 5, ry: 3, fill: colors.accent2 || '#2b2b2b' }));
    legR.appendChild(svgEl('ellipse', { cx: 26, cy: 80, rx: 5, ry: 3, fill: colors.accent2 || '#2b2b2b' }));
    grp.appendChild(legL);
    grp.appendChild(legR);

    // Torso / coat
    var torso = svgEl('g', { class: 'limb torso' });
    torso.setAttribute('style', 'transform-origin:20px 52px');
    if (kind === 'gentleman') {
      torso.appendChild(svgEl('path', {
        d: 'M11,30 Q20,24 29,30 L32,58 Q20,64 8,58 Z',
        fill: colors.coat
      }));
      torso.appendChild(svgEl('path', { d: 'M17,30 L20,40 L23,30 Z', fill: colors.shirt || '#e9e2d2' }));
      torso.appendChild(svgEl('rect', { x: 18.5, y: 30, width: 3, height: 10, fill: colors.accent }));
    } else if (kind === 'explorer') {
      torso.appendChild(svgEl('rect', { x: 11, y: 30, width: 18, height: 26, rx: 7, fill: colors.coat }));
      // backpack
      torso.appendChild(svgEl('rect', { x: 24, y: 33, width: 9, height: 16, rx: 3, fill: colors.accent }));
      torso.appendChild(svgEl('rect', { x: 26, y: 36, width: 5, height: 3, rx: 1, fill: colors.accent2 || '#caa86a' }));
    } else { // runner
      torso.appendChild(svgEl('path', {
        d: 'M13,30 Q20,26 27,30 L29,55 Q20,60 11,55 Z',
        fill: colors.coat
      }));
      torso.appendChild(svgEl('path', { d: 'M13,44 L27,44 L26,55 Q20,58 14,55 Z', fill: colors.accent }));
    }
    grp.appendChild(torso);

    // Arms (children of torso group so they swing with the body)
    var armL = svgEl('g', { class: 'limb arm-l' });
    var armR = svgEl('g', { class: 'limb arm-r' });
    armL.setAttribute('style', 'transform-origin:13px 33px');
    armR.setAttribute('style', 'transform-origin:27px 33px');
    armL.appendChild(svgEl('rect', { x: 10.5, y: 33, width: 5, height: 22, rx: 2.5, fill: colors.coat }));
    armR.appendChild(svgEl('rect', { x: 24.5, y: 33, width: 5, height: 22, rx: 2.5, fill: colors.coat }));
    armL.appendChild(svgEl('circle', { cx: 13, cy: 56, r: 3, fill: colors.skin }));
    armR.appendChild(svgEl('circle', { cx: 27, cy: 56, r: 3, fill: colors.skin }));
    torso.appendChild(armL);
    torso.appendChild(armR);

    // Head
    var head = svgEl('g', { class: 'limb head' });
    head.appendChild(svgEl('circle', { cx: 20, cy: 20, r: 7.5, fill: colors.skin }));
    if (kind === 'gentleman') {
      head.appendChild(svgEl('path', { d: 'M12.5,18 a7.5,7.5 0 0 1 15,0 q-7.5,-4 -15,0 Z', fill: colors.hair }));
      // top hat
      head.appendChild(svgEl('rect', { x: 14.5, y: 8, width: 11, height: 3, rx: 1, fill: '#15110a' }));
      head.appendChild(svgEl('rect', { x: 16.5, y: 2, width: 7, height: 8, rx: 1, fill: '#15110a' }));
      head.appendChild(svgEl('rect', { x: 16.5, y: 8.5, width: 7, height: 1.6, fill: colors.accent }));
    } else if (kind === 'explorer') {
      head.appendChild(svgEl('path', { d: 'M12,17 a8,8 0 0 1 16,0 l-1,-1 q-7,-3 -14,0 Z', fill: colors.hair }));
      head.appendChild(svgEl('path', { d: 'M11,16 Q20,10 29,16 L29,15 Q20,12 11,15 Z', fill: colors.accent2 || '#caa86a' }));
    } else {
      head.appendChild(svgEl('path', { d: 'M13,17 a7.5,7.5 0 0 1 14,-1 q-7,-3 -14,1 Z', fill: colors.hair }));
      head.appendChild(svgEl('rect', { x: 12.5, y: 17, width: 15, height: 2.4, rx: 1.2, fill: colors.accent }));
    }
    grp.appendChild(head);

    return svg;
  }

  function makeHuman(kind, radius, colors, extraClass) {
    var anchor = document.createElement('div');
    anchor.className = 'figure-anchor';
    var shadow = document.createElement('div');
    shadow.className = 'shadow';
    var fig = document.createElement('div');
    fig.className = 'fig ' + extraClass;
    fig.appendChild(buildFigure(kind, colors));
    anchor.appendChild(shadow);
    anchor.appendChild(fig);
    plane.appendChild(anchor);
    return { anchor: anchor, radius: radius, fig: fig };
  }

  var hourHuman = makeHuman('gentleman', 65, {
    skin: '#e0c2a0', hair: '#3a3a3a', coat: '#21303a', coat2: '#1a262e',
    shirt: '#efe7d8', accent: '#d4af6a'
  }, 'gentleman');

  var minuteHuman = makeHuman('explorer', 85, {
    skin: '#e7c6a3', hair: '#5b3a23', coat: '#5b7a63', coat2: '#3f5a48',
    accent: '#caa86a', accent2: '#8a6a3c'
  }, 'explorer');

  var secondHuman = makeHuman('runner', 95, {
    skin: '#e3c4a0', hair: '#2c2420', coat: '#23282e', coat2: '#1a1e23',
    accent: '#e8703a', accent2: '#e8703a'
  }, 'runner');

  // expose for the driver section
  window.__livingClock = {
    plane: plane,
    polar: polar,
    place: place,
    hourHuman: hourHuman,
    minuteHuman: minuteHuman,
    secondHuman: secondHuman
  };
})();

// =====================================================================
// DRIVER — real-time positioning, ambience, settings & hourly event
// =====================================================================
(function () {
  'use strict';

  var C = window.__livingClock;
  var hourHuman = C.hourHuman, minuteHuman = C.minuteHuman, secondHuman = C.secondHuman;

  var digital = document.getElementById('digitalTime');
  var dateLine = document.getElementById('dateLine');
  var fountain = document.getElementById('fountain');
  var ambientLayer = document.getElementById('ambient');
  var banner = document.getElementById('banner');
  var burst = document.getElementById('burst');

  var gearBtn = document.getElementById('gearBtn');
  var panel = document.getElementById('panel');
  var closeBtn = document.getElementById('closeBtn');
  var optReduce = document.getElementById('optReduce');
  var optCelebration = document.getElementById('optCelebration');
  var optFountain = document.getElementById('optFountain');
  var optAmbient = document.getElementById('optAmbient');

  // ---------------------------------------------------------------
  // Accessibility / preference settings — persisted across sessions
  // ---------------------------------------------------------------
  var SETTINGS_KEY = 'livingClockSettings';
  var settings = { reduce: false, noCelebration: false, noFountain: false, ambient: true };
  try {
    var stored = JSON.parse(localStorage.getItem(SETTINGS_KEY) || '{}');
    for (var k in stored) settings[k] = stored[k];
  } catch (e) { /* localStorage unavailable — fall back to defaults */ }

  function saveSettings() {
    try { localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings)); } catch (e) {}
  }
  function applySettings() {
    document.body.classList.toggle('reduced', !!settings.reduce);
    fountain.style.visibility = settings.noFountain ? 'hidden' : 'visible';
    ambientLayer.style.display = settings.ambient ? '' : 'none';
    optReduce.checked = !!settings.reduce;
    optCelebration.checked = !!settings.noCelebration;
    optFountain.checked = !!settings.noFountain;
    optAmbient.checked = settings.ambient !== false;
  }
  applySettings();

  gearBtn.addEventListener('click', function () { panel.classList.add('on'); });
  closeBtn.addEventListener('click', function () { panel.classList.remove('on'); });
  panel.addEventListener('click', function (e) { if (e.target === panel) panel.classList.remove('on'); });
  [
    ['reduce', optReduce],
    ['noCelebration', optCelebration],
    ['noFountain', optFountain],
    ['ambient', optAmbient]
  ].forEach(function (pair) {
    pair[1].addEventListener('change', function () {
      settings[pair[0]] = pair[1].checked;
      saveSettings();
      applySettings();
    });
  });

  // ---------------------------------------------------------------
  // Ambient effects — fireflies (steady pool) & falling leaves (spawned)
  // ---------------------------------------------------------------
  for (var i = 0; i < 9; i++) {
    var f = document.createElement('div');
    f.className = 'firefly';
    var fp = C.polar(Math.random() * 360, 28 + Math.random() * 58);
    f.style.left = fp.left;
    f.style.top = fp.top;
    f.style.animationDelay = (Math.random() * 9).toFixed(2) + 's, ' + (Math.random() * 2.6).toFixed(2) + 's';
    ambientLayer.appendChild(f);
  }
  function spawnLeaf() {
    if (!settings.ambient || settings.reduce) return;
    var leaf = document.createElement('div');
    leaf.className = 'leaf';
    leaf.style.left = (8 + Math.random() * 84) + '%';
    leaf.style.top = '-6%';
    var dur = 9 + Math.random() * 7;
    leaf.style.animationDuration = dur.toFixed(1) + 's';
    leaf.style.opacity = (0.45 + Math.random() * 0.4).toFixed(2);
    ambientLayer.appendChild(leaf);
    setTimeout(function () { leaf.remove(); }, dur * 1000 + 300);
  }
  setInterval(function () { if (settings.ambient && !settings.reduce) spawnLeaf(); }, 4200);

  // ---------------------------------------------------------------
  // Hourly celebration — all three humans gather at 12, then disperse
  // ---------------------------------------------------------------
  var celebTimeline = null;
  var celebrationArmed = true;

  function easeInOut(x) { return x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2; }

  function celebrationBlend() {
    if (!celebTimeline) return 0;
    var t = performance.now() - celebTimeline.start;
    var rise = 2200, hold = 3200, fall = 2400;
    if (t < rise) return easeInOut(t / rise);
    if (t < rise + hold) return 1;
    if (t < rise + hold + fall) return 1 - easeInOut((t - rise - hold) / fall);
    return 0;
  }

  // Interpolates an angle toward 12 o'clock (0deg) along its shortest arc.
  function towardTwelve(angle, blend) {
    if (blend <= 0) return angle;
    var target = angle > 180 ? 360 : 0;
    return (angle + (target - angle) * blend) % 360;
  }

  function spawnFireworks() {
    if (settings.reduce) return;
    for (var s = 0; s < 16; s++) {
      (function (idx) {
        var spark = document.createElement('div');
        spark.className = 'spark';
        var ang = Math.random() * Math.PI * 2;
        var dist = 36 + Math.random() * 68;
        spark.style.setProperty('--dx', (Math.cos(ang) * dist).toFixed(1) + 'px');
        spark.style.setProperty('--dy', (Math.sin(ang) * dist).toFixed(1) + 'px');
        spark.style.background = idx % 3 === 0 ? '#f1d9a8' : (idx % 3 === 1 ? '#d4af6a' : '#fff7e0');
        spark.style.animationDelay = (idx * 45) + 'ms';
        burst.appendChild(spark);
        requestAnimationFrame(function () { spark.classList.add('go'); });
        setTimeout(function () { spark.remove(); }, 1700);
      })(s);
    }
  }

  var audioCtx = null;
  function unlockAudio() {
    try {
      audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)();
      if (audioCtx.state === 'suspended') audioCtx.resume();
    } catch (e) { /* Web Audio unavailable */ }
  }
  document.body.addEventListener('touchstart', unlockAudio, { once: true, passive: true });
  document.body.addEventListener('click', unlockAudio, { once: true });

  function playBell() {
    if (settings.reduce) return;
    unlockAudio();
    if (!audioCtx) return;
    try {
      var now = audioCtx.currentTime;
      [880, 1318.5, 1760].forEach(function (freq, idx) {
        var osc = audioCtx.createOscillator();
        var gain = audioCtx.createGain();
        osc.type = 'sine';
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.0001, now);
        gain.gain.exponentialRampToValueAtTime(0.2 / (idx + 1), now + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + 2.6);
        osc.connect(gain).connect(audioCtx.destination);
        osc.start(now);
        osc.stop(now + 2.7);
      });
    } catch (e) { /* synthesis failed — silently skip the chime */ }
  }

  function showBanner(now) {
    var h = now.getHours() % 12;
    if (h === 0) h = 12;
    banner.textContent = h + " o'clock — together at twelve";
    banner.classList.add('show');
    setTimeout(function () { banner.classList.remove('show'); }, 6600);
  }

  function runCelebration(now) {
    celebTimeline = { start: performance.now() };
    showBanner(now);
    spawnFireworks();
    playBell();
    setTimeout(function () { celebTimeline = null; }, 7900);
  }

  function checkCelebration(now) {
    if (now.getMinutes() !== 0) { celebrationArmed = true; return; }
    if (celebrationArmed) {
      celebrationArmed = false;
      if (!settings.noCelebration) runCelebration(now);
    }
  }

  // ---------------------------------------------------------------
  // Real-time positioning
  //   • Hour & Minute humans: re-targeted once per second, then glide
  //     to that target with a linear CSS transition — continuous,
  //     interpolated motion without per-frame DOM writes.
  //   • Second human: re-positioned every animation frame from the
  //     live clock (incl. milliseconds) — perfectly continuous, never
  //     snaps, completes one revolution every 60 seconds.
  // ---------------------------------------------------------------
  // A subtle lean toward the direction of travel — makes the orbit feel alive
  // without rotating the whole body (which would fight the walk/run cycles).
  function lean(figEl, angleDeg, maxLean) {
    if (settings.reduce) { figEl.style.transform = ''; return; }
    var rad = angleDeg * Math.PI / 180;
    var tilt = Math.sin(rad) * maxLean;
    figEl.style.transform = 'rotate(' + tilt.toFixed(1) + 'deg)';
  }

  var lastShownSecond = -1;
  function updateDigital(now) {
    var s = now.getSeconds();
    if (s === lastShownSecond) return;
    lastShownSecond = s;
    var hh = String(now.getHours()).padStart(2, '0');
    var mm = String(now.getMinutes()).padStart(2, '0');
    var ss = String(s).padStart(2, '0');
    digital.textContent = hh + ':' + mm + ':' + ss;
  }

  var lastShownDay = -1;
  function updateDateLine(now) {
    var d = now.getDate();
    if (d === lastShownDay) return;
    lastShownDay = d;
    var label = now.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' });
    dateLine.textContent = label.toUpperCase();
  }

  function updateSlowHumans() {
    var now = new Date();
    var hours = now.getHours() % 12;
    var minutes = now.getMinutes();
    var seconds = now.getSeconds();

    var hourAngle = (hours + minutes / 60 + seconds / 3600) * 30;   // 360°/12h
    var minuteAngle = (minutes + seconds / 60) * 6;                  // 360°/60m

    var blend = celebrationBlend();
    var hA = towardTwelve(hourAngle, blend);
    var mA = towardTwelve(minuteAngle, blend);

    C.place(hourHuman.anchor, hA, hourHuman.radius);
    C.place(minuteHuman.anchor, mA, minuteHuman.radius);
    lean(minuteHuman.fig, mA, 5);

    updateDateLine(now);
    checkCelebration(now);
  }

  function tickSeconds() {
    var now = new Date();
    var ms = now.getMilliseconds();
    var secAngle = (now.getSeconds() + ms / 1000) * 6;               // 360°/60s

    var blend = celebrationBlend();
    var sA = towardTwelve(secAngle, blend);

    C.place(secondHuman.anchor, sA, secondHuman.radius);
    lean(secondHuman.fig, sA, 7);

    updateDigital(now);
    requestAnimationFrame(tickSeconds);
  }

  // Initial placement so the diorama doesn't pop in at 12 on first paint
  (function initialPlacement() {
    var now = new Date();
    var hours = now.getHours() % 12;
    var minutes = now.getMinutes();
    var seconds = now.getSeconds();
    C.place(hourHuman.anchor, (hours + minutes / 60 + seconds / 3600) * 30, hourHuman.radius);
    C.place(minuteHuman.anchor, (minutes + seconds / 60) * 6, minuteHuman.radius);
    C.place(secondHuman.anchor, (seconds + now.getMilliseconds() / 1000) * 6, secondHuman.radius);
    updateDigital(now);
    updateDateLine(now);
  })();

  // Add the smooth glide only after the first paint, so the very first
  // placement above doesn't animate in from the 12 o'clock position.
  requestAnimationFrame(function () {
    hourHuman.anchor.classList.add('interp');
    minuteHuman.anchor.classList.add('interp');
  });

  setInterval(updateSlowHumans, 1000);
  requestAnimationFrame(tickSeconds);
})();
