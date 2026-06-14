/* ==========================================================================
   Brain Dump — catalogs, date helpers, app store, small UI helpers
   ========================================================================== */

(function () {
  // ---- Catalogs -----------------------------------------------------------

  const THEMES = [
    { key: "ocean_blue", name: "Ocean Blue" },
    { key: "classic_purple", name: "Classic Purple" },
    { key: "warm_sunset", name: "Warm Sunset" },
    { key: "forest_green", name: "Forest Green" },
    { key: "neon_glow", name: "Neon Glow" },
    { key: "sweet_pink", name: "Sweet Pink" },
    { key: "rose_gold", name: "Rose Gold" },
    { key: "soft_lavender", name: "Soft Lavender" },
    { key: "midnight_blue", name: "Midnight Blue" },
    { key: "galaxy_purple", name: "Galaxy Purple" },
    { key: "deep_teal", name: "Deep Teal" },
    { key: "golden_amber", name: "Golden Amber" },
    { key: "emerald_green", name: "Emerald Green" },
    { key: "bright_turquoise", name: "Bright Turquoise" },
    { key: "cool_slate", name: "Cool Slate" },
    { key: "pure_black", name: "Pure Black" }
  ];

  const FONTS = [
    { key: "default", name: "Default", css: "sans-serif" },
    { key: "sans-serif", name: "Sans Serif", css: "sans-serif" },
    { key: "sans-serif-medium", name: "Sans Serif Medium", css: "sans-serif-medium" },
    { key: "sans-serif-condensed", name: "Condensed", css: "sans-serif-condensed" },
    { key: "sans-serif-condensed-medium", name: "Condensed Medium", css: "sans-serif-condensed-medium" },
    { key: "sans-serif-light", name: "Light", css: "sans-serif-light" },
    { key: "sans-serif-black", name: "Black", css: "sans-serif-black" },
    { key: "sans-serif-thin", name: "Thin", css: "sans-serif-thin" },
    { key: "serif", name: "Serif", css: "serif" },
    { key: "monospace", name: "Monospace", css: "monospace" },
    { key: "casual", name: "Casual", css: "casual" },
    { key: "cursive", name: "Cursive", css: "cursive" }
  ];

  const MOODS = [
    { key: "happy", emoji: "\u{1F60A}", label: "Happy" },
    { key: "calm", emoji: "\u{1F60C}", label: "Calm" },
    { key: "motivated", emoji: "\u{1F4AA}", label: "Motivated" },
    { key: "excited", emoji: "\u{1F929}", label: "Excited" },
    { key: "grateful", emoji: "\u{1F64F}", label: "Grateful" },
    { key: "sad", emoji: "\u{1F622}", label: "Sad" },
    { key: "anxious", emoji: "\u{1F630}", label: "Anxious" },
    { key: "angry", emoji: "\u{1F620}", label: "Angry" },
    { key: "tired", emoji: "\u{1F634}", label: "Tired" },
    { key: "stressed", emoji: "\u{1F623}", label: "Stressed" },
    { key: "loved", emoji: "\u{1F970}", label: "Loved" },
    { key: "confused", emoji: "\u{1F615}", label: "Confused" }
  ];

  const DATE_FORMATS = ["EEEE, MMM d", "MMM d", "yMMMd", "yMd", "d MMM y"];

  const MOOD_BY_KEY = {};
  MOODS.forEach((m) => { MOOD_BY_KEY[m.key] = m; });

  window.Catalog = { THEMES, FONTS, MOODS, DATE_FORMATS, moodByKey: (key) => MOOD_BY_KEY[key] };

  // ---- Date / time helpers --------------------------------------------------

  const WEEKDAYS = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
  const WEEKDAYS_SHORT = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
  const MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
  const MONTHS_SHORT = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

  function formatDate(date, pattern) {
    const d = date.getDate();
    const m = date.getMonth();
    const y = date.getFullYear();
    switch (pattern) {
      case "MMM d":
        return MONTHS_SHORT[m] + " " + d;
      case "yMMMd":
        return MONTHS_SHORT[m] + " " + d + ", " + y;
      case "yMd":
        return (m + 1) + "/" + d + "/" + y;
      case "d MMM y":
        return d + " " + MONTHS_SHORT[m] + " " + y;
      case "EEEE, MMM d":
      default:
        return WEEKDAYS[date.getDay()] + ", " + MONTHS_SHORT[m] + " " + d;
    }
  }

  function formatTime(date, use24h) {
    let h = date.getHours();
    const min = String(date.getMinutes()).padStart(2, "0");
    if (use24h) {
      return String(h).padStart(2, "0") + ":" + min;
    }
    const period = h >= 12 ? "PM" : "AM";
    h = h % 12;
    if (h === 0) h = 12;
    return h + ":" + min + " " + period;
  }

  function greeting(date) {
    const h = date.getHours();
    if (h < 5) return "Good Night";
    if (h < 12) return "Good Morning";
    if (h < 17) return "Good Afternoon";
    if (h < 21) return "Good Evening";
    return "Good Night";
  }

  function startOfDay(date) {
    const d = new Date(date);
    d.setHours(0, 0, 0, 0);
    return d.getTime();
  }

  function relativeTime(ts, use24h, dateFormat) {
    const date = new Date(ts);
    const now = new Date();
    const today = startOfDay(now);
    const day = startOfDay(date);
    const diffDays = Math.round((today - day) / 86400000);
    if (diffDays === 0) return formatTime(date, use24h);
    if (diffDays === 1) return "Yesterday";
    if (diffDays > 1 && diffDays < 7) return WEEKDAYS_SHORT[date.getDay()];
    return formatDate(date, dateFormat || "MMM d");
  }

  /** Like relativeTime, but always includes the time alongside the date/day label. */
  function relativeDateTime(ts, use24h, dateFormat) {
    const date = new Date(ts);
    const now = new Date();
    const today = startOfDay(now);
    const day = startOfDay(date);
    const diffDays = Math.round((today - day) / 86400000);
    let label;
    if (diffDays === 0) label = "Today";
    else if (diffDays === 1) label = "Yesterday";
    else if (diffDays > 1 && diffDays < 7) label = WEEKDAYS_SHORT[date.getDay()];
    else label = formatDate(date, dateFormat || "MMM d");
    return label + " · " + formatTime(date, use24h);
  }

  function formatDuration(ms) {
    const totalSec = Math.round(ms / 1000);
    const min = Math.floor(totalSec / 60);
    const sec = totalSec % 60;
    return min + ":" + String(sec).padStart(2, "0");
  }

  window.DateUtil = { formatDate, formatTime, greeting, relativeTime, relativeDateTime, formatDuration, WEEKDAYS, MONTHS };

  // ---- App store ------------------------------------------------------------

  window.Store = {
    settings: null,
    security: null,
    aiStatus: null,

    init() {
      this.settings = Bridge.getSettings();
      this.security = Bridge.getSecurityStatus();
      this.aiStatus = Bridge.getAiStatus();
      this.mediaBaseUrl = Bridge.getMediaBaseUrl();
      this.applyTheme();
    },

    refreshSecurity() {
      this.security = Bridge.getSecurityStatus();
      return this.security;
    },

    refreshAiStatus() {
      this.aiStatus = Bridge.getAiStatus();
      return this.aiStatus;
    },

    saveSettings(patch) {
      this.settings = Bridge.saveSettings(patch);
      this.applyTheme();
      return this.settings;
    },

    resolvedMode() {
      let mode = this.settings.themeMode;
      if (mode === "system") {
        mode = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
      }
      return mode;
    },

    applyTheme() {
      const root = document.documentElement;
      root.setAttribute("data-mode", this.resolvedMode());
      root.setAttribute("data-color", this.settings.themeColor || "ocean_blue");
      const font = Catalog.FONTS.find((f) => f.key === this.settings.fontFamily) || Catalog.FONTS[0];
      root.style.setProperty("--font-family", font.css);
      root.style.setProperty("--font-scale", (this.settings.fontSizePercent || 100) / 100);
    }
  };

  if (window.matchMedia) {
    window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => {
      if (Store.settings && Store.settings.themeMode === "system") Store.applyTheme();
    });
  }

  // ---- Small UI helpers -------------------------------------------------------

  function escapeHtml(str) {
    if (str == null) return "";
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  let toastTimer = null;

  function toast(message, isError) {
    const root = document.getElementById("toast-root");
    if (!root) return;
    root.innerHTML = '<div class="toast ' + (isError ? "toast--error" : "") + '">' + escapeHtml(message) + "</div>";
    const el = root.querySelector(".toast");
    requestAnimationFrame(() => el.classList.add("show"));
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      el.classList.remove("show");
      setTimeout(() => { root.innerHTML = ""; }, 250);
    }, 2200);
  }

  function showModal(html) {
    const root = document.getElementById("modal-root");
    root.innerHTML = '<div class="modal-backdrop"><div class="modal">' + html + "</div></div>";
    root.querySelector(".modal-backdrop").addEventListener("click", (e) => {
      if (e.target.classList.contains("modal-backdrop")) hideModal();
    });
    return root.querySelector(".modal");
  }

  function hideModal() {
    const root = document.getElementById("modal-root");
    if (root) root.innerHTML = "";
  }

  function modalOpen() {
    const root = document.getElementById("modal-root");
    return !!(root && root.innerHTML.trim());
  }

  function confirmDialog({ title, message, confirmLabel = "Confirm", cancelLabel = "Cancel", danger = false, onConfirm }) {
    const modal = showModal(
      "<h3>" + escapeHtml(title) + "</h3>" +
      "<p>" + escapeHtml(message) + "</p>" +
      '<div class="modal__actions">' +
      '<button class="btn btn--secondary" data-action="cancel">' + escapeHtml(cancelLabel) + "</button>" +
      '<button class="btn ' + (danger ? "btn--danger" : "btn--primary") + '" data-action="confirm">' + escapeHtml(confirmLabel) + "</button>" +
      "</div>"
    );
    modal.querySelector('[data-action="cancel"]').addEventListener("click", hideModal);
    modal.querySelector('[data-action="confirm"]').addEventListener("click", () => {
      hideModal();
      onConfirm && onConfirm();
    });
  }

  window.UI = { escapeHtml, toast, showModal, hideModal, modalOpen, confirmDialog };
})();
