/* ==========================================================================
   Brain Dump — app shell: router, drawer, lock-screen orchestration
   ========================================================================== */

(function () {
  let drawerOpen = false;

  // ---- Router ---------------------------------------------------------------

  const Router = {
    stack: [],
    viewRoot: null,
    currentCleanup: null,

    push(screen, params) {
      this.stack.push({ screen, params: params || {} });
      this.render();
    },

    pop() {
      if (this.stack.length > 1) {
        this.stack.pop();
        this.render();
        return true;
      }
      return false;
    },

    replace(screen, params) {
      this.stack[this.stack.length - 1] = { screen, params: params || {} };
      this.render();
    },

    reset(screen, params) {
      this.stack = [{ screen, params: params || {} }];
      this.render();
    },

    render() {
      if (this.currentCleanup) {
        try { this.currentCleanup(); } catch (e) { /* ignore */ }
        this.currentCleanup = null;
      }
      const top = this.stack[this.stack.length - 1];
      const view = window.Views[top.screen];
      this.viewRoot.innerHTML = "";
      const cleanup = view.render(this.viewRoot, top.params || {});
      if (typeof cleanup === "function") this.currentCleanup = cleanup;
    }
  };

  window.Router = Router;

  // ---- Drawer ---------------------------------------------------------------

  function statCard(emoji, value, label) {
    return (
      '<div class="stat-card"><div class="stat-card__icon">' + emoji + "</div>" +
      '<div class="stat-card__value">' + value + "</div>" +
      '<div class="stat-card__label">' + label + "</div></div>"
    );
  }

  function buildDrawerHtml() {
    const stats = Bridge.getStats();
    const moodStats = Bridge.getMoodStats();
    const info = Bridge.getAppInfo();

    const recentEntries = Object.entries(moodStats.recent || {}).sort((a, b) => b[1] - a[1]).slice(0, 5);
    let moodsHtml;
    if (recentEntries.length === 0) {
      moodsHtml = '<p class="settings-note">Log a mood with your thoughts to see trends here.</p>';
    } else {
      const max = Math.max(...recentEntries.map(([, c]) => c));
      moodsHtml = '<div class="mood-list">' + recentEntries.map(([key, count]) => {
        const mood = Catalog.moodByKey(key);
        const pct = max > 0 ? Math.round((count / max) * 100) : 0;
        return '<div class="mood-row">' +
          '<span class="mood-row__emoji">' + (mood ? mood.emoji : "❓") + "</span>" +
          '<span class="mood-row__name">' + UI.escapeHtml(mood ? mood.label : key) + "</span>" +
          '<div class="mood-row__bar"><div class="mood-row__bar-fill" style="width:' + pct + '%"></div></div>' +
          '<span class="mood-row__count">' + count + "</span>" +
          "</div>";
      }).join("") + "</div>";
    }

    return (
      '<div class="drawer__header">' +
      '<div class="drawer__logo">' + Icon("lightbulb") + "</div>" +
      '<div class="drawer__title">Brain Dump</div>' +
      "</div>" +
      "<h3>Your Stats</h3>" +
      '<div class="stats-grid">' +
      statCard("\u{1F4DD}", stats.total, "Total") +
      statCard("\u{1F4C5}", stats.today, "Today") +
      statCard("\u{1F5D3}️", stats.week, "This Week") +
      statCard("\u{1F525}", stats.streak, "Streak") +
      "</div>" +
      "<h3>Recent Moods</h3>" +
      moodsHtml +
      '<div class="drawer__nav">' +
      '<button class="drawer__nav-item" data-nav="chat">' + Icon("chat") + "<span>Chat with AI</span>" + Icon("chevron-right", 'class="chevron"') + "</button>" +
      '<button class="drawer__nav-item" data-nav="mood">' + Icon("trending-up") + "<span>Mood Analytics</span>" + Icon("chevron-right", 'class="chevron"') + "</button>" +
      '<button class="drawer__nav-item" data-nav="settings">' + Icon("settings") + "<span>Settings</span>" + Icon("chevron-right", 'class="chevron"') + "</button>" +
      "</div>" +
      '<div class="drawer__spacer"></div>' +
      '<div class="drawer__footer">Brain Dump v' + UI.escapeHtml(info.versionName) + "</div>"
    );
  }

  function renderDrawer() {
    const drawer = document.getElementById("drawer");
    drawer.innerHTML = buildDrawerHtml();
    drawer.querySelectorAll("[data-nav]").forEach((btn) => {
      btn.addEventListener("click", () => {
        closeDrawer();
        Router.push(btn.dataset.nav);
      });
    });
  }

  function openDrawer() {
    renderDrawer();
    document.getElementById("drawer-backdrop").classList.add("open");
    document.getElementById("drawer").classList.add("open");
    drawerOpen = true;
  }

  function closeDrawer() {
    document.getElementById("drawer-backdrop").classList.remove("open");
    document.getElementById("drawer").classList.remove("open");
    drawerOpen = false;
  }

  window.AppShell = { openDrawer, closeDrawer };

  // ---- Lock screen -------------------------------------------------------------

  function showLock() {
    const root = document.getElementById("lock-root");
    Views.lock.render(root, {
      onUnlock: () => { root.innerHTML = ""; }
    });
  }

  window.__relock = function () {
    Store.refreshSecurity();
    if (Store.security && Store.security.hasPin) showLock();
  };

  // ---- Back button -------------------------------------------------------------

  window.__handleBackButton = function () {
    const lockRoot = document.getElementById("lock-root");
    if (lockRoot && lockRoot.innerHTML.trim()) return false;
    if (UI.modalOpen()) { UI.hideModal(); return true; }
    if (drawerOpen) { closeDrawer(); return true; }
    if (Router.pop()) return true;
    return false;
  };

  // ---- Bootstrap -------------------------------------------------------------

  document.addEventListener("DOMContentLoaded", () => {
    Store.init();

    document.getElementById("drawer-backdrop").addEventListener("click", closeDrawer);

    Router.viewRoot = document.getElementById("view-root");
    Router.reset("home");

    if (Store.security && Store.security.hasPin) showLock();
  });
})();
