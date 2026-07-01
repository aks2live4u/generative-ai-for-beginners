/* LaunchPilot AI — app logic (state, navigation, rule engines, AI mentor) */
(function () {
  "use strict";

  var KB = window.KB;
  var STORAGE_KEY = "launchpilot_state_v1";

  // -----------------------------------------------------------------------
  // State
  // -----------------------------------------------------------------------
  var defaultState = {
    profile: {
      founderName: "Founder",
      country: "India",
      state: "",
      city: "",
      idea: "",
      industry: "",
      bizmode: "",
      structure: "",
      channel: "",
      budgetId: "",
      audience: "",
      scope: "",
      entity: "",
      businessName: "",
      adhdMode: false
    },
    onboardingComplete: false,
    checklist: {},
    chat: [],
    timelineWeeks: 6
  };

  var state = loadState();

  function loadState() {
    try {
      var raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return JSON.parse(JSON.stringify(defaultState));
      var parsed = JSON.parse(raw);
      // shallow-merge to survive future field additions
      var merged = JSON.parse(JSON.stringify(defaultState));
      merged.profile = Object.assign({}, merged.profile, parsed.profile || {});
      merged.onboardingComplete = !!parsed.onboardingComplete;
      merged.checklist = parsed.checklist || {};
      merged.chat = parsed.chat || [];
      merged.timelineWeeks = parsed.timelineWeeks || 6;
      return merged;
    } catch (e) {
      return JSON.parse(JSON.stringify(defaultState));
    }
  }

  function saveState() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }

  // -----------------------------------------------------------------------
  // DOM helpers
  // -----------------------------------------------------------------------
  function qs(sel, root) { return (root || document).querySelector(sel); }
  function qsa(sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); }
  function el(tag, attrs, html) {
    var e = document.createElement(tag);
    if (attrs) Object.keys(attrs).forEach(function (k) { e.setAttribute(k, attrs[k]); });
    if (html !== undefined) e.innerHTML = html;
    return e;
  }
  function escapeHtml(str) {
    return String(str || "").replace(/[&<>"']/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
    });
  }
  function fmtMoney(n) {
    n = Math.round(n);
    return "₹" + n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  }

  var toastTimer = null;
  function showToast(msg) {
    var t = qs("#lp-toast");
    if (!t) {
      t = el("div", { id: "lp-toast" });
      t.style.cssText = "position:absolute;left:50%;bottom:110px;transform:translateX(-50%);" +
        "background:#1F1B33;color:#fff;padding:10px 16px;border-radius:20px;font-size:12px;" +
        "z-index:999;opacity:0;transition:opacity .2s;pointer-events:none;max-width:80%;text-align:center;";
      qs("#app").appendChild(t);
    }
    t.textContent = msg;
    t.style.opacity = "1";
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { t.style.opacity = "0"; }, 2200);
  }

  // -----------------------------------------------------------------------
  // Navigation
  // -----------------------------------------------------------------------
  var TAB_SCREENS = ["home", "checklist", "research", "tools", "profile"];
  var stack = [];

  function screenEl(id) { return qs("#screen-" + id); }

  function renderStack() {
    qsa(".screen").forEach(function (s) { s.classList.remove("active"); });
    var top = stack[stack.length - 1];
    if (!top) return;
    var s = screenEl(top.id);
    if (s) s.classList.add("active");

    var isTab = TAB_SCREENS.indexOf(top.id) !== -1 && stack.length === 1;
    qs("#bottom-nav").style.display = isTab ? "flex" : "none";
    if (isTab) {
      qsa(".nav-item").forEach(function (n) {
        n.classList.toggle("active", n.getAttribute("data-nav") === top.id);
      });
    }
    runScreenRenderer(top.id, top.params);
  }

  function goToTab(tabId) {
    stack = [{ id: tabId, params: {} }];
    renderStack();
  }

  function pushScreen(id, params) {
    stack.push({ id: id, params: params || {} });
    renderStack();
  }

  function popScreen() {
    if (stack.length > 1) {
      stack.pop();
      renderStack();
      return true;
    }
    return false;
  }

  function handleBack() {
    if (screenEl("onboarding").classList.contains("active")) {
      if (onbStep > 1) {
        setOnbStep(onbStep - 1);
        return true;
      }
      return false;
    }
    if (popScreen()) return true;
    var top = stack[0];
    if (top && top.id !== "home") {
      goToTab("home");
      return true;
    }
    return false;
  }
  window.LaunchPilot = { handleBack: function () { return handleBack(); } };

  qs("#bottom-nav").addEventListener("click", function (e) {
    var item = e.target.closest(".nav-item");
    if (!item) return;
    goToTab(item.getAttribute("data-nav"));
  });

  document.addEventListener("click", function (e) {
    var back = e.target.closest("[data-back]");
    if (back) popScreen();
  });

  // -----------------------------------------------------------------------
  // Onboarding
  // -----------------------------------------------------------------------
  var onbStep = 1;
  var onbAnswers = {};

  function populateSelect(sel, items, valueFn, labelFn, placeholder) {
    sel.innerHTML = "";
    if (placeholder) sel.appendChild(el("option", { value: "" }, placeholder));
    items.forEach(function (item) {
      var opt = el("option", { value: valueFn(item) }, escapeHtml(labelFn(item)));
      sel.appendChild(opt);
    });
  }

  function initOnboardingFields() {
    populateSelect(qs("#f-state"), KB.STATES, function (s) { return s; }, function (s) { return s; }, "Select state (optional)");
    populateSelect(qs("#f-industry"), KB.INDUSTRIES, function (i) { return i.id; }, function (i) { return i.label; }, "Select industry / niche");
    populateSelect(qs("#f-structure"), KB.STRUCTURES, function (s) { return s.id; }, function (s) { return s.label; }, "Select business structure");

    var budgetBox = qs("#f-budget");
    budgetBox.innerHTML = "";
    KB.BUDGET_RANGES.forEach(function (b) {
      var card = el("div", { class: "choice-card", "data-value": b.id }, b.label);
      budgetBox.appendChild(card);
    });

    var dots = qs("#onb-dots");
    dots.innerHTML = "";
    for (var i = 0; i < 7; i++) dots.appendChild(el("span"));
  }

  function fillOnboardingFromProfile(p) {
    onbAnswers = {
      bizmode: p.bizmode, channel: p.channel, budget: p.budgetId,
      audience: p.audience, scope: p.scope, entity: p.entity
    };
    qs("#f-state").value = p.state || "";
    qs("#f-city").value = p.city || "";
    qs("#f-idea").value = p.idea || "";
    qs("#f-industry").value = p.industry || "";
    qs("#f-structure").value = p.structure || "";
    ["bizmode", "channel", "budget", "audience", "scope", "entity"].forEach(function (field) {
      var container = qs('[data-field="' + field + '"]');
      if (!container) return;
      qsa(".choice-card", container).forEach(function (c) {
        c.classList.toggle("selected", c.getAttribute("data-value") === onbAnswers[field]);
      });
    });
  }

  document.addEventListener("click", function (e) {
    var card = e.target.closest(".choice-card");
    if (!card) return;
    var container = card.closest("[data-field]");
    if (!container) return;
    var field = container.getAttribute("data-field");
    qsa(".choice-card", container).forEach(function (c) { c.classList.remove("selected"); });
    card.classList.add("selected");
    onbAnswers[field] = card.getAttribute("data-value");
  });

  function setOnbStep(n) {
    onbStep = n;
    qsa(".onb-step").forEach(function (s) {
      s.hidden = parseInt(s.getAttribute("data-step"), 10) !== n;
    });
    qs("#onb-step-num").textContent = n;
    qsa("#onb-dots span").forEach(function (d, i) {
      d.classList.toggle("filled", i < n);
    });
    qs("#onb-back-btn").hidden = n === 1;
    qs("#onb-continue-btn").textContent = n === 7 ? "Generate My Roadmap 🚀" : "Continue";
  }

  function validateStep(n) {
    switch (n) {
      case 2: return qs("#f-idea").value.trim().length > 0 && qs("#f-industry").value;
      case 3: return !!onbAnswers.bizmode && qs("#f-structure").value;
      case 4: return !!onbAnswers.channel;
      case 5: return !!onbAnswers.budget;
      case 6: return !!onbAnswers.audience && !!onbAnswers.scope;
      case 7: return !!onbAnswers.entity;
      default: return true;
    }
  }

  qs("#onb-back-btn").addEventListener("click", function () {
    if (onbStep > 1) setOnbStep(onbStep - 1);
  });

  qs("#onb-continue-btn").addEventListener("click", function () {
    if (!validateStep(onbStep)) {
      showToast("Please fill this step before continuing.");
      return;
    }
    if (onbStep < 7) {
      setOnbStep(onbStep + 1);
      return;
    }
    finishOnboarding();
  });

  qs("#onb-form").addEventListener("submit", function (e) { e.preventDefault(); });

  function finishOnboarding() {
    var idea = qs("#f-idea").value.trim();
    var p = state.profile;
    p.state = qs("#f-state").value;
    p.city = qs("#f-city").value.trim();
    p.idea = idea;
    p.industry = qs("#f-industry").value;
    p.bizmode = onbAnswers.bizmode;
    p.structure = qs("#f-structure").value;
    p.channel = onbAnswers.channel;
    p.budgetId = onbAnswers.budget;
    p.audience = onbAnswers.audience;
    p.scope = onbAnswers.scope;
    p.entity = onbAnswers.entity;
    p.businessName = deriveBusinessName(idea);

    state.checklist = generateChecklistStatuses(p, state.checklist);
    state.onboardingComplete = true;
    saveState();
    goToTab("home");
  }

  function deriveBusinessName(idea) {
    if (!idea) return "My Business";
    var words = idea.replace(/^i want to (start|build|launch)\s*/i, "").trim();
    words = words.replace(/^(a|an|the)\s+/i, "");
    words = words.charAt(0).toUpperCase() + words.slice(1);
    if (words.length > 40) words = words.slice(0, 40) + "…";
    return words || "My Business";
  }

  function startOnboarding(editMode) {
    initOnboardingFields();
    if (editMode) {
      fillOnboardingFromProfile(state.profile);
    } else {
      onbAnswers = {};
      qs("#onb-form").reset();
      qsa(".choice-card").forEach(function (c) { c.classList.remove("selected"); });
    }
    setOnbStep(1);
    stack = [{ id: "onboarding", params: {} }];
    renderStack();
  }

  // -----------------------------------------------------------------------
  // Checklist rule engine
  // -----------------------------------------------------------------------
  function itemApplies(item, profile) {
    var tags = item.tags || [];
    if (tags.indexOf("always") !== -1) return true;
    return tags.some(function (tag) {
      if (tag === "food") return profile.industry === "food";
      if (tag === "export") return profile.scope === "international";
      if (tag === "offlineStore") return profile.channel === "offline" || profile.channel === "both";
      if (tag === "manufacturing") return profile.bizmode === "manufacturing" || profile.bizmode === "trading";
      if (tag === "ecommerce") return profile.channel === "online" || profile.channel === "both";
      if (tag === "hiring") return profile.entity === "company";
      if (tag.indexOf("structure:") === 0) return tag === "structure:" + profile.structure;
      return false;
    });
  }

  function getApplicableItems(category, profile) {
    return category.items.filter(function (item) { return itemApplies(item, profile); });
  }

  function findItemById(itemId) {
    for (var i = 0; i < KB.CHECKLIST_CATEGORIES.length; i++) {
      var cat = KB.CHECKLIST_CATEGORIES[i];
      for (var j = 0; j < cat.items.length; j++) {
        if (cat.items[j].id === itemId) return { item: cat.items[j], category: cat };
      }
    }
    return null;
  }

  function generateChecklistStatuses(profile, existing) {
    var out = {};
    KB.CHECKLIST_CATEGORIES.forEach(function (cat) {
      getApplicableItems(cat, profile).forEach(function (item) {
        out[item.id] = (existing && existing[item.id]) || "pending";
      });
    });
    return out;
  }

  function getStatus(itemId) { return state.checklist[itemId] || "pending"; }
  function setStatus(itemId, status) { state.checklist[itemId] = status; saveState(); }
  function cycleStatus(current) {
    if (current === "pending") return "progress";
    if (current === "progress") return "done";
    return "pending";
  }

  function categoryStats(cat, profile) {
    var items = getApplicableItems(cat, profile);
    var done = 0, progress = 0;
    items.forEach(function (it) {
      var s = getStatus(it.id);
      if (s === "done") done++;
      else if (s === "progress") progress++;
    });
    return { total: items.length, done: done, progress: progress, pending: items.length - done - progress };
  }

  function overallStats(profile) {
    var total = 0, done = 0;
    KB.CHECKLIST_CATEGORIES.forEach(function (cat) {
      var s = categoryStats(cat, profile);
      total += s.total; done += s.done;
    });
    return { total: total, done: done, pct: total ? Math.round((done / total) * 100) : 0 };
  }

  // -----------------------------------------------------------------------
  // Screen renderers
  // -----------------------------------------------------------------------
  var renderers = {};

  renderers.home = function () {
    var p = state.profile;
    qs("#home-greeting").textContent = "Hello, " + escapeHtml(p.founderName) + " 👋";
    var overall = overallStats(p);

    var applicableCats = KB.CHECKLIST_CATEGORIES.map(function (cat) {
      return { cat: cat, stats: categoryStats(cat, p) };
    }).filter(function (x) { return x.stats.total > 0; });

    var tiles = applicableCats.map(function (x) {
      var pct = x.stats.total ? Math.round((x.stats.done / x.stats.total) * 100) : 0;
      return '<div class="tile" data-open-category="' + x.cat.id + '">' +
        '<span class="tile-icon">' + x.cat.icon + '</span>' +
        '<div class="tile-name">' + escapeHtml(x.cat.name) + '</div>' +
        '<div class="tile-pct">' + x.stats.done + '/' + x.stats.total + ' completed</div>' +
        '<div class="progress-track"><div class="progress-fill" style="width:' + pct + '%"></div></div>' +
        '</div>';
    }).join("");

    var upcoming = [];
    applicableCats.forEach(function (x) {
      getApplicableItems(x.cat, p).forEach(function (item) {
        if (getStatus(item.id) === "pending" && upcoming.length < (p.adhdMode ? 1 : 3)) {
          upcoming.push({ item: item, cat: x.cat });
        }
      });
    });
    var upcomingHtml = upcoming.length ? upcoming.map(function (u) {
      return '<div class="list-row" data-open-task="' + u.item.id + '">' +
        '<div class="row-icon">' + u.cat.icon + '</div>' +
        '<div class="row-body"><div class="row-title">' + escapeHtml(u.item.title) + '</div>' +
        '<div class="row-sub">' + escapeHtml(u.cat.name) + '</div></div></div>';
    }).join("") : '<div class="empty-note">All caught up! Check the Checklist tab for anything new.</div>';

    qs("#home-body").innerHTML =
      '<div class="hero-card"><div class="hero-label">Overall Progress</div>' +
      '<div class="hero-pct">' + overall.pct + '%</div>' +
      '<div class="hero-note">' + (overall.pct >= 100 ? "🎉 All done — you're ready to launch!" : overall.pct >= 50 ? "Great job! You're on track 🚀" : "Let's get moving on your launch checklist.") + '</div>' +
      '<div class="progress-track"><div class="progress-fill" style="width:' + overall.pct + '%"></div></div></div>' +
      '<div class="card"><div class="biz-name-row"><div><div class="card-title" style="margin-bottom:2px">Business</div>' +
      '<div class="name">' + escapeHtml(p.businessName || "My Business") + '</div></div>' +
      '<span class="edit" data-edit-profile>✎ Edit</span></div>' +
      '<div class="tile-grid">' + tiles + '</div></div>' +
      '<div class="section-header upcoming-header"><span>Upcoming Tasks</span><span class="link-btn" data-goto-tab="checklist">View All</span></div>' +
      upcomingHtml +
      '<div class="section-header">Quick Tools</div>' +
      '<div class="tile-grid">' +
      toolTileHtml("🧮", "Cost Estimator", "cost-estimator") +
      toolTileHtml("🤖", "AI Mentor", "ai-mentor") +
      toolTileHtml("🏭", "Sourcing Finder", "manufacturer-finder") +
      toolTileHtml("📅", "Timeline", "timeline") +
      '</div>';
  };

  function toolTileHtml(icon, name, screen) {
    return '<div class="tile" data-open-screen="' + screen + '"><span class="tile-icon">' + icon + '</span>' +
      '<div class="tile-name">' + name + '</div></div>';
  }

  var checklistFilter = "all";
  renderers.checklist = function () {
    var p = state.profile;
    var cats = KB.CHECKLIST_CATEGORIES.map(function (cat) {
      return { cat: cat, stats: categoryStats(cat, p) };
    }).filter(function (x) { return x.stats.total > 0; });

    if (!cats.length) {
      qs("#checklist-body").innerHTML = '<div class="empty-note">Complete onboarding to generate your checklist.</div>';
      return;
    }

    qs("#checklist-body").innerHTML = cats.map(function (x) {
      var s = x.stats;
      var count = checklistFilter === "all" ? s.done + "/" + s.total + " completed" :
        checklistFilter === "pending" ? s.pending + " pending" :
        checklistFilter === "progress" ? s.progress + " in progress" : s.done + " done";
      var pct = s.total ? Math.round((s.done / s.total) * 100) : 0;
      return '<div class="list-row" data-open-category="' + x.cat.id + '">' +
        '<div class="row-icon">' + x.cat.icon + '</div>' +
        '<div class="row-body"><div class="row-title">' + escapeHtml(x.cat.name) + '</div>' +
        '<div class="row-sub">' + count + '</div>' +
        '<div class="progress-track"><div class="progress-fill" style="width:' + pct + '%"></div></div></div>' +
        '<div class="row-pct">' + pct + '%</div></div>';
    }).join("");
  };

  qs("#checklist-tabs").addEventListener("click", function (e) {
    var t = e.target.closest(".tab");
    if (!t) return;
    qsa(".tab", qs("#checklist-tabs")).forEach(function (x) { x.classList.remove("active"); });
    t.classList.add("active");
    checklistFilter = t.getAttribute("data-filter");
    renderers.checklist();
  });

  renderers["category-detail"] = function (params) {
    var cat = KB.CHECKLIST_CATEGORIES.find(function (c) { return c.id === params.categoryId; });
    if (!cat) return;
    qs("#category-detail-title").textContent = cat.icon + " " + cat.name;
    var items = getApplicableItems(cat, state.profile);
    var filtered = items.filter(function (it) {
      var s = getStatus(it.id);
      if (checklistFilter === "all") return true;
      if (checklistFilter === "pending") return s === "pending";
      if (checklistFilter === "progress") return s === "progress";
      return s === "done";
    });
    if (!filtered.length) {
      qs("#category-detail-body").innerHTML = '<div class="empty-note">Nothing here for this filter.</div>';
      return;
    }
    qs("#category-detail-body").innerHTML = filtered.map(function (it) {
      var s = getStatus(it.id);
      var mark = s === "done" ? "✓" : s === "progress" ? "…" : "";
      return '<div class="list-row"><div class="check-row" style="width:100%">' +
        '<div class="check-box ' + s + '" data-toggle-status="' + it.id + '">' + mark + '</div>' +
        '<div class="row-body" data-open-task="' + it.id + '">' +
        '<div class="row-title">' + escapeHtml(it.title) + '</div>' +
        '<div class="row-sub">' + escapeHtml(it.time) + ' · ' + escapeHtml(it.cost) + '</div>' +
        '</div></div></div>';
    }).join("");
  };

  renderers["task-detail"] = function (params) {
    var found = findItemById(params.itemId);
    if (!found) return;
    var item = found.item, cat = found.category;
    var s = getStatus(item.id);
    var statusLabel = s === "done" ? "✓ Completed" : s === "progress" ? "● In Progress" : "○ Pending";

    var docsHtml = item.documents && item.documents.length ?
      '<div class="info-row"><span class="info-label">📄 Documents Required</span>' +
      '<span class="info-value">' + escapeHtml(item.documents.join(", ")) + '</span></div>' : "";
    var whereHtml = item.officialUrl ?
      '<a class="info-row" href="' + item.officialUrl + '" style="text-decoration:none;color:inherit">' +
      '<span class="info-label">📍 Where to Apply</span><span class="info-value link">' + escapeHtml(item.whereToApply) + ' →</span></a>' :
      '<div class="info-row"><span class="info-label">📍 Where to Apply</span><span class="info-value">' + escapeHtml(item.whereToApply || "See in-app tools") + '</span></div>';

    qs("#task-detail-body").innerHTML =
      '<div class="task-badge">' + escapeHtml(cat.name) + '</div>' +
      '<div class="task-status">' + statusLabel + '</div>' +
      '<div class="task-title">' + escapeHtml(item.title) + '</div>' +
      '<div class="task-why-box"><div class="task-why-label">Why is this important?</div>' +
      '<div class="task-why-text">' + escapeHtml(item.why) + '</div></div>' +
      '<div class="info-list">' +
      '<div class="info-row"><span class="info-label">💰 Estimated Cost</span><span class="info-value">' + escapeHtml(item.cost) + '</span></div>' +
      '<div class="info-row"><span class="info-label">⏱ Time Required</span><span class="info-value">' + escapeHtml(item.time) + '</span></div>' +
      whereHtml + docsHtml +
      '</div>' +
      (item.steps && item.steps.length ? '<div class="steps-list"><div class="task-why-label" style="margin-bottom:8px">Step-by-Step Guide</div><ol>' +
        item.steps.map(function (st) { return "<li>" + escapeHtml(st) + "</li>"; }).join("") + '</ol></div>' : "") +
      '<button class="btn-primary" style="width:100%" data-toggle-status="' + item.id + '">' +
      (s === "done" ? "↺ Mark as Pending" : "✓ Mark as Complete") + '</button>';
  };

  document.addEventListener("click", function (e) {
    var toggle = e.target.closest("[data-toggle-status]");
    if (toggle) {
      var id = toggle.getAttribute("data-toggle-status");
      var current = getStatus(id);
      var next = toggle.classList.contains("check-box") ? cycleStatus(current) : (current === "done" ? "pending" : "done");
      setStatus(id, next);
      renderStack();
      return;
    }
    var openCat = e.target.closest("[data-open-category]");
    if (openCat) { pushScreen("category-detail", { categoryId: openCat.getAttribute("data-open-category") }); return; }
    var openTask = e.target.closest("[data-open-task]");
    if (openTask) { pushScreen("task-detail", { itemId: openTask.getAttribute("data-open-task") }); return; }
    var openScreen = e.target.closest("[data-open-screen]");
    if (openScreen) { pushScreen(openScreen.getAttribute("data-open-screen"), {}); return; }
    var gotoTab = e.target.closest("[data-goto-tab]");
    if (gotoTab) { goToTab(gotoTab.getAttribute("data-goto-tab")); return; }
    var editProfile = e.target.closest("[data-edit-profile]");
    if (editProfile) { startOnboarding(true); return; }
  });

  // -----------------------------------------------------------------------
  // Research Hub
  // -----------------------------------------------------------------------
  renderers.research = function () {
    var p = state.profile;
    var industryLabel = (KB.INDUSTRIES.find(function (i) { return i.id === p.industry; }) || {}).label || "your business";
    var searchUrl = "https://www.google.com/search?q=" + encodeURIComponent("how to start a " + industryLabel + " business in India");

    qs("#research-body").innerHTML =
      '<div class="search-row" style="padding:0 0 12px 0"><input class="field-input" placeholder="Search anything…" id="research-search" /></div>' +
      '<div class="section-header">Popular Searches</div>' +
      '<div class="tabs" style="padding:0 0 6px 0">' +
      ["T-shirt manufacturers", "GST registration", "Packaging suppliers", "Startup funding India"].map(function (q) {
        return '<span class="tab" data-research-query="' + escapeHtml(q) + '">' + escapeHtml(q) + '</span>';
      }).join("") + '</div>' +
      '<div class="card"><div class="card-title">📈 Market Insight</div>' +
      '<div style="font-size:12px;color:var(--text-muted);line-height:1.5">Search current market size, trends and demand for <b>' + escapeHtml(industryLabel) + '</b> in India — figures change often, so we point you to live search rather than guess a number.</div>' +
      '<a class="btn-primary" style="display:block;text-align:center;text-decoration:none;margin-top:10px" href="' + searchUrl + '">Search Market Insights →</a></div>' +
      '<div class="section-header">Official Resources</div>' +
      KB.RESEARCH_RESOURCES.map(function (r) {
        return '<a class="list-row" style="text-decoration:none;color:inherit" href="' + r.url + '">' +
          '<div class="row-icon">📘</div><div class="row-body"><div class="row-title">' + escapeHtml(r.title) + '</div>' +
          '<div class="row-sub">' + r.tag + ' · ' + r.url.replace(/^https?:\/\//, "") + '</div></div></a>';
      }).join("");
  };

  document.addEventListener("click", function (e) {
    var rq = e.target.closest("[data-research-query]");
    if (rq) {
      var q = rq.getAttribute("data-research-query");
      window.open("https://www.google.com/search?q=" + encodeURIComponent(q + " India"), "_blank");
    }
  });

  // -----------------------------------------------------------------------
  // Tools grid
  // -----------------------------------------------------------------------
  renderers.tools = function () {
    var tools = [
      { icon: "🤖", name: "AI Business Mentor", desc: "Ask anything, get India-grounded answers", screen: "ai-mentor" },
      { icon: "🏭", name: "Sourcing Finder", desc: "Manufacturers, wholesale markets & suppliers", screen: "manufacturer-finder" },
      { icon: "🧮", name: "Cost Estimator", desc: "Budget breakdown for your business", screen: "cost-estimator" },
      { icon: "📄", name: "Documents & Templates", desc: "Business plan, NDA, invoices & more", screen: "documents" },
      { icon: "📅", name: "Timeline Planner", desc: "Week-by-week launch plan", screen: "timeline" },
      { icon: "🏦", name: "Funding & Support", desc: "Loans, grants & government schemes", screen: "funding" }
    ];
    qs("#tools-body").innerHTML = '<div class="tile-grid">' + tools.map(function (t) {
      return '<div class="tile" data-open-screen="' + t.screen + '" style="min-height:96px">' +
        '<span class="tile-icon">' + t.icon + '</span><div class="tile-name">' + t.name + '</div>' +
        '<div class="tile-pct">' + t.desc + '</div></div>';
    }).join("") + '</div>';
  };

  // -----------------------------------------------------------------------
  // Manufacturer / Wholesale / Supplier Finder
  // -----------------------------------------------------------------------
  var mfState = { tab: "manufacturers", query: "" };

  function matchIndustryKey(query, profile) {
    var q = (query || "").toLowerCase().trim();
    if (!q) return profile.industry || null;
    var keywordMap = {
      clothing: ["shirt", "tshirt", "t-shirt", "clothing", "apparel", "garment", "hoodie", "fashion", "wear"],
      hometextiles: ["bedsheet", "linen", "towel", "blanket", "home textile"],
      leather: ["leather", "footwear", "shoe", "bag"],
      ceramics: ["ceramic", "pottery", "tile", "sanitary"],
      jewelry: ["jewel", "gold", "diamond", "gemstone"],
      furniture: ["furniture", "wood", "decor"],
      electronics: ["electronic", "gadget", "circuit", "hardware"],
      cosmetics: ["cosmetic", "beauty", "skincare", "personal care"],
      sportsgoods: ["sport", "football", "cricket", "gym"],
      automobile: ["auto", "vehicle", "car part"],
      food: ["food", "snack", "spice", "dairy", "beverage"],
      handicrafts: ["handicraft", "handloom", "handmade"]
    };
    for (var key in keywordMap) {
      if (keywordMap[key].some(function (kw) { return q.indexOf(kw) !== -1; })) return key;
    }
    return profile.industry || null;
  }

  function directoryLinksHtml(query) {
    var q = encodeURIComponent(query || "manufacturer India");
    return '<div class="directory-row">' +
      '<a class="directory-btn" href="https://dir.indiamart.com/search.mp?ss=' + q + '">Search IndiaMART</a>' +
      '<a class="directory-btn" href="https://www.tradeindia.com/">TradeIndia</a>' +
      '<a class="directory-btn" href="https://gem.gov.in/">GeM Portal</a>' +
      '<a class="directory-btn" href="https://www.google.com/search?q=' + q + '+manufacturer">Google Search</a>' +
      '</div>';
  }

  renderers["manufacturer-finder"] = function () {
    qs("#mf-search").value = mfState.query;
    var key = matchIndustryKey(mfState.query, state.profile);
    var body = qs("#mf-body");

    if (mfState.tab === "manufacturers") {
      var hubData = key && KB.MANUFACTURING_HUBS[key];
      if (!hubData) {
        body.innerHTML = '<div class="empty-note">Type a product above (e.g. "T-shirt", "bedsheets") to see India\'s top manufacturing hubs.</div>' + directoryLinksHtml(mfState.query);
        return;
      }
      body.innerHTML = '<div class="moq-note">Typical MOQ: ' + escapeHtml(hubData.moq) + '</div>' +
        hubData.hubs.map(function (h) {
          return '<div class="hub-card"><div class="hub-city">📍 ' + escapeHtml(h.city) + '</div>' +
            '<div class="hub-note">' + escapeHtml(h.note) + '</div></div>';
        }).join("") +
        '<div class="card" style="margin-top:6px"><div class="card-title">Find real suppliers</div>' +
        '<div style="font-size:12px;color:var(--text-muted);margin-bottom:6px">We link to verified directories rather than guessing contact details — always verify certifications and get samples before a bulk order.</div>' +
        directoryLinksHtml(mfState.query || key) + '</div>';
    } else if (mfState.tab === "wholesale") {
      var wKey = key && KB.WHOLESALE_MARKETS[key] ? key : "general";
      var markets = KB.WHOLESALE_MARKETS[wKey];
      body.innerHTML = markets.map(function (m) {
        return '<div class="hub-card"><div class="hub-city">🏬 ' + escapeHtml(m) + '</div></div>';
      }).join("") + '<div class="card"><div class="card-title">Find real wholesalers</div>' + directoryLinksHtml(mfState.query) + '</div>';
    } else {
      body.innerHTML = KB.SUPPLIER_CATEGORIES.map(function (s) {
        return '<div class="hub-card"><div class="hub-city">📦 ' + escapeHtml(s.name) + '</div>' +
          '<div class="hub-note">Where to look: ' + escapeHtml(s.where) + '</div></div>';
      }).join("") + directoryLinksHtml(mfState.query);
    }
  };

  qs("#mf-tabs").addEventListener("click", function (e) {
    var t = e.target.closest(".tab");
    if (!t) return;
    qsa(".tab", qs("#mf-tabs")).forEach(function (x) { x.classList.remove("active"); });
    t.classList.add("active");
    mfState.tab = t.getAttribute("data-mf-tab");
    renderers["manufacturer-finder"]();
  });
  qs("#mf-search").addEventListener("input", function () {
    mfState.query = this.value;
    renderers["manufacturer-finder"]();
  });

  // -----------------------------------------------------------------------
  // Cost Estimator
  // -----------------------------------------------------------------------
  function budgetMidpoint(profile) {
    var b = KB.BUDGET_RANGES.find(function (x) { return x.id === profile.budgetId; });
    if (!b) return 200000;
    return b.max >= 10000000 ? b.min * 1.5 : (b.min + b.max) / 2;
  }

  var costState = { amount: null, mode: null };

  renderers["cost-estimator"] = function () {
    var p = state.profile;
    if (costState.amount === null) costState.amount = Math.round(budgetMidpoint(p));
    if (costState.mode === null) costState.mode = p.bizmode || "trading";

    var template = KB.COST_TEMPLATES[costState.mode] || KB.COST_TEMPLATES.trading;
    var rows = template.map(function (t) {
      return { label: t.label, value: Math.round(costState.amount * t.pct) };
    });
    var total = rows.reduce(function (s, r) { return s + r.value; }, 0);

    qs("#cost-body").innerHTML =
      '<div class="card">' +
      '<label class="field-label">Business Model</label>' +
      '<select class="field-select" id="cost-mode-select">' +
      ['manufacturing', 'trading', 'service'].map(function (m) {
        return '<option value="' + m + '"' + (m === costState.mode ? " selected" : "") + '>' + m.charAt(0).toUpperCase() + m.slice(1) + '</option>';
      }).join("") + '</select>' +
      '<label class="field-label">Budget (₹)</label>' +
      '<input class="field-input" type="number" id="cost-amount-input" value="' + costState.amount + '" />' +
      '</div>' +
      '<div class="card"><div class="card-title">Estimated Cost Breakdown</div>' +
      rows.map(function (r) {
        return '<div class="cost-row"><span class="cost-label">' + escapeHtml(r.label) + '</span><span class="cost-value">' + fmtMoney(r.value) + '</span></div>';
      }).join("") +
      '<div class="cost-total-row"><span>Total Estimated Cost</span><span>' + fmtMoney(total) + '</span></div>' +
      '</div>' +
      '<button class="btn-primary" style="width:100%" id="cost-download-btn">Download Report ⭳</button>';

    qs("#cost-mode-select").addEventListener("change", function () {
      costState.mode = this.value; renderers["cost-estimator"]();
    });
    qs("#cost-amount-input").addEventListener("change", function () {
      costState.amount = parseInt(this.value, 10) || 0; renderers["cost-estimator"]();
    });
    qs("#cost-download-btn").addEventListener("click", function () {
      var text = "LAUNCHPILOT AI — COST ESTIMATE REPORT\n" +
        "Business: " + (p.businessName || "My Business") + "\n" +
        "Model: " + costState.mode + " | Budget: " + fmtMoney(costState.amount) + "\n" +
        "----------------------------------------\n" +
        rows.map(function (r) { return r.label + ": " + fmtMoney(r.value); }).join("\n") +
        "\n----------------------------------------\n" +
        "TOTAL: " + fmtMoney(total) +
        "\n\nNote: This is an approximate planning estimate, not a quote. Actual costs vary by vendor, city and negotiation.";
      openDocumentView("Cost Estimate Report", text);
    });
  };

  // -----------------------------------------------------------------------
  // AI Business Mentor
  // -----------------------------------------------------------------------
  var SUGGESTION_CHIPS = [
    "I have ₹50,000 budget",
    "I want passive income",
    "I have ADHD, simplify my plan",
    "What licenses do I need?",
    "Where do I find a manufacturer?",
    "What funding is available?"
  ];

  function renderChat() {
    var body = qs("#mentor-chat");
    if (!state.chat.length) {
      state.chat.push({ role: "bot", text: "Hi " + state.profile.founderName + "! I'm your AI Business Mentor for " + (state.profile.businessName || "your business") + ". I know your India-specific checklist, costs, funding options and manufacturing hubs — ask me anything, or tap a suggestion below." });
    }
    body.innerHTML = state.chat.map(function (m) {
      return '<div class="msg-row ' + m.role + '"><div class="msg-avatar">' + (m.role === "bot" ? "🤖" : "🧑") + '</div>' +
        '<div class="msg-bubble">' + escapeHtml(m.text) + '</div></div>';
    }).join("");
    body.scrollTop = body.scrollHeight;

    qs("#mentor-suggestions").innerHTML = SUGGESTION_CHIPS.map(function (c) {
      return '<button class="chip-btn" data-mentor-suggestion="' + escapeHtml(c) + '">' + escapeHtml(c) + '</button>';
    }).join("");
  }

  renderers["ai-mentor"] = function () { renderChat(); };

  function sendMentorMessage(text) {
    text = text.trim();
    if (!text) return;
    state.chat.push({ role: "user", text: text });
    var reply = generateMentorReply(text, state.profile);
    state.chat.push({ role: "bot", text: reply });
    saveState();
    renderChat();
  }

  qs("#mentor-send-btn").addEventListener("click", function () {
    var input = qs("#mentor-input");
    sendMentorMessage(input.value);
    input.value = "";
  });
  qs("#mentor-input").addEventListener("keydown", function (e) {
    if (e.key === "Enter") { qs("#mentor-send-btn").click(); }
  });
  qs("#mentor-suggestions").addEventListener("click", function (e) {
    var chip = e.target.closest("[data-mentor-suggestion]");
    if (chip) sendMentorMessage(chip.getAttribute("data-mentor-suggestion"));
  });
  qs("#mentor-reset-btn").addEventListener("click", function () {
    state.chat = [];
    saveState();
    renderChat();
  });

  function parseRupeeAmount(text) {
    var m = text.match(/₹?\s*([\d,.]+)\s*(lakh|lakhs|l\b|crore|cr\b|k\b|thousand)?/i);
    if (!m) return null;
    var num = parseFloat(m[1].replace(/,/g, ""));
    if (isNaN(num)) return null;
    var unit = (m[2] || "").toLowerCase();
    if (unit.indexOf("crore") !== -1 || unit === "cr") num *= 10000000;
    else if (unit.indexOf("lakh") !== -1 || unit === "l") num *= 100000;
    else if (unit.indexOf("k") !== -1 || unit.indexOf("thousand") !== -1) num *= 1000;
    return num;
  }

  var BUDGET_IDEAS = {
    under50k: ["Print-on-demand store (no inventory)", "Reselling/dropshipping via Instagram", "Digital products (templates, ebooks, presets)", "Freelance service business", "Affiliate marketing"],
    "50k-2l": ["Small-batch clothing brand via a local tailor/small manufacturer", "Home-based food/bakery business (FSSAI Basic license)", "Handicraft/D2C niche brand", "Reselling with your own private-label branding"],
    "2l-5l": ["Full clothing/accessories brand with a Tiruppur/Jaipur-style manufacturer", "Cosmetics private-label brand", "Small physical retail counter/kiosk"],
    "5l-10l": ["Larger inventory-based D2C brand across 2-3 product lines", "Hybrid online + offline store"],
    "10l-25l": ["Owned small manufacturing setup", "Multi-product brand with a small founding team"],
    above25l: ["Scaled manufacturing operation", "Multi-city retail presence", "Private-label FMCG brand"]
  };

  function budgetTierFromAmount(amount) {
    var b = KB.BUDGET_RANGES.find(function (r) { return amount >= r.min && amount <= r.max; });
    return b ? b.id : (amount > 2500000 ? "above25l" : "under50k");
  }

  function generateMentorReply(text, profile) {
    var lower = text.toLowerCase();
    var out = [];

    var amount = parseRupeeAmount(text);
    if (amount && (lower.indexOf("budget") !== -1 || lower.indexOf("₹") !== -1 || lower.indexOf("rupee") !== -1 || lower.indexOf("have") !== -1)) {
      var tier = budgetTierFromAmount(amount);
      out.push("With " + fmtMoney(amount) + ", here are businesses that typically fit:");
      out.push(BUDGET_IDEAS[tier].map(function (x) { return "• " + x; }).join("\n"));
      out.push("Want me to run the Cost Estimator for any of these? Open Tools → Cost Estimator.");
    }

    if (lower.indexOf("passive income") !== -1) {
      out.push("For passive income from India, consider:\n• Ebooks & digital products\n• Affiliate marketing\n• Print-on-demand\n• Online courses\n• Templates/design assets\n• A simple app or SaaS tool");
    }

    if (lower.indexOf("adhd") !== -1) {
      state.profile.adhdMode = true;
      saveState();
      out.push("Got it — I've simplified your plan: the Home tab will now show just 1 focus task at a time instead of a long list, so it's easier to act on. Take it one small step at a time — that's a completely valid way to build a business.");
    }

    if (/licen[cs]e|registration|legal|register/.test(lower)) {
      var legalCat = KB.CHECKLIST_CATEGORIES.find(function (c) { return c.id === "legal"; });
      var applicable = getApplicableItems(legalCat, profile).slice(0, 6);
      out.push("Based on your profile, here's what you'll likely need legally in India:\n" +
        applicable.map(function (it) { return "• " + it.title + " (" + it.cost + ")"; }).join("\n") +
        "\nFull details (why/cost/documents/where to apply) are in your Checklist → Legal & Compliance.");
    }

    if (lower.indexOf("gst") !== -1) {
      var gst = findItemById("gst-registration").item;
      out.push("GST Registration: " + gst.why + "\nCost: " + gst.cost + " | Time: " + gst.time + "\nApply at: " + gst.officialUrl);
    }

    if (lower.indexOf("trademark") !== -1) {
      var tm = findItemById("trademark").item;
      out.push("Trademark: " + tm.why + "\nCost: " + tm.cost + " | Time: " + tm.time + "\nApply at: " + tm.officialUrl);
    }

    if (/manufactur|supplier|source|sourcing/.test(lower)) {
      var key = matchIndustryKey(text, profile) || matchIndustryKey("", profile);
      var hubData = key && KB.MANUFACTURING_HUBS[key];
      if (hubData) {
        out.push("Top manufacturing hubs for " + hubData.label + " in India:\n" +
          hubData.hubs.map(function (h) { return "• " + h.city + " — " + h.note; }).join("\n") +
          "\nOpen Tools → Sourcing Finder to search real supplier directories.");
      } else {
        out.push("Open Tools → Sourcing Finder and search your product — I'll show you the right manufacturing hub and real supplier directories (IndiaMART, TradeIndia, GeM).");
      }
    }

    if (/fund|loan|invest|grant|scheme/.test(lower)) {
      var top = KB.FUNDING_SCHEMES.slice(0, 4);
      out.push("Funding options worth exploring in India:\n" +
        top.map(function (f) { return "• " + f.name + " — " + f.summary; }).join("\n") +
        "\nSee all of these in Tools → Funding & Support.");
    }

    if (/market|promot|advertis|instagram|social media/.test(lower)) {
      out.push("For an India D2C launch: start with 2 channels max (usually Instagram + WhatsApp), work with 5-10 micro-influencers on a barter/small-fee basis, and run a small ₹300-500/day Meta ad test before scaling spend.");
    }

    if (!out.length) {
      out.push("Here's what I can help with right now for " + (profile.businessName || "your business") + " in " + (profile.city || profile.state || "India") + ":\n" +
        "• Business ideas that fit a budget\n• Licenses/registrations you need\n• Where to find manufacturers/suppliers\n• Funding & government schemes\n• Marketing basics\n\nTry one of the suggestions below, or ask in your own words.");
    }

    return out.join("\n\n");
  }

  // -----------------------------------------------------------------------
  // Documents & Templates
  // -----------------------------------------------------------------------
  var docFilter = "all";
  renderers.documents = function () {
    var items = KB.DOCUMENT_TEMPLATES.filter(function (d) { return docFilter === "all" || d.category === docFilter; });
    qs("#documents-body").innerHTML = items.map(function (d) {
      return '<div class="list-row" data-open-document="' + d.id + '">' +
        '<div class="row-icon">📄</div><div class="row-body"><div class="row-title">' + escapeHtml(d.title) + '</div>' +
        '<div class="row-sub">' + escapeHtml(d.format) + '</div></div></div>';
    }).join("");
  };
  qs("#doc-tabs").addEventListener("click", function (e) {
    var t = e.target.closest(".tab");
    if (!t) return;
    qsa(".tab", qs("#doc-tabs")).forEach(function (x) { x.classList.remove("active"); });
    t.classList.add("active");
    docFilter = t.getAttribute("data-doc-filter");
    renderers.documents();
  });
  document.addEventListener("click", function (e) {
    var d = e.target.closest("[data-open-document]");
    if (!d) return;
    var id = d.getAttribute("data-open-document");
    var meta = KB.DOCUMENT_TEMPLATES.find(function (x) { return x.id === id; });
    openDocumentView(meta.title, generateDocument(id, state.profile));
  });

  function openDocumentView(title, text) {
    qs("#document-view-title").textContent = title;
    qs("#document-view-content").textContent = text;
    pushScreen("document-view", {});
  }

  var LEGAL_DISCLAIMER = "\n\n---\nThis is a general-purpose template for planning only. Have a Chartered Accountant / Company Secretary / lawyer review before you sign or file anything.";

  function generateDocument(id, p) {
    var biz = p.businessName || "[Business Name]";
    var loc = [p.city, p.state].filter(Boolean).join(", ") || "[City, State]";
    switch (id) {
      case "business-plan":
        return "BUSINESS PLAN — " + biz + "\n\n1. Executive Summary\n" + (p.idea || "[Describe your business idea]") +
          "\n\n2. Business Structure: " + (p.structure || "[structure]") +
          "\n3. Location: " + loc + ", India" +
          "\n4. Industry: " + (p.industry || "[industry]") +
          "\n5. Products/Services: [List]\n6. Target Customers: " + (p.audience || "[audience]") +
          "\n7. Sales Channels: " + (p.channel || "[online/offline/both]") +
          "\n8. Manufacturing/Sourcing Plan: [Manufacturer/supplier + location]" +
          "\n9. Marketing Plan: [Channels + budget]\n10. Financial Plan: [See Cost Estimator]\n11. Milestones: [See Timeline Planner]";
      case "pitch-deck":
        return "PITCH DECK OUTLINE — " + biz + "\n\n1. Cover (Name + Tagline)\n2. Problem\n3. Solution\n4. Market Size (India)\n5. Product\n6. Business Model\n7. Traction (if any)\n8. Go-to-Market Plan\n9. Competition\n10. Team\n11. Financials & Ask\n12. Contact";
      case "gst-invoice":
        return "TAX INVOICE\n\nSeller: " + biz + "\nGSTIN: [Your GSTIN]\nAddress: " + loc + ", India\n\nInvoice No: [___]   Date: [___]\n\nBill To: [Customer Name]\nGSTIN (if B2B): [___]\n\nItem | HSN/SAC | Qty | Rate | Taxable Value | CGST | SGST/IGST | Total\n-----|---------|-----|------|---------------|------|-----------|------\n\nTotal Amount: [___]\nAmount in Words: [___]\n\nDeclaration: We declare that this invoice shows the actual price of goods/services and all particulars are true and correct." + LEGAL_DISCLAIMER;
      case "purchase-order":
        return "PURCHASE ORDER\n\nFrom (Buyer): " + biz + ", " + loc + "\nTo (Vendor): [Vendor Name & Address]\n\nPO No: [___]   Date: [___]\n\nItem | Description | Qty | Unit Price | Total\n-----|-------------|-----|-----------|------\n\nDelivery Address: [___]\nExpected Delivery Date: [___]\nPayment Terms: [e.g. 50% advance, 50% on delivery]\n\nAuthorized Signature: [___]" + LEGAL_DISCLAIMER;
      case "nda":
        return "NON-DISCLOSURE AGREEMENT\n\nThis Agreement is made between " + biz + " (\"Disclosing Party\") and [Other Party Name] (\"Receiving Party\").\n\n1. Confidential Information: Any business, technical or financial information shared for the purpose of [purpose].\n2. Obligations: The Receiving Party shall not disclose or use the Confidential Information for any purpose other than [purpose].\n3. Term: This Agreement remains in effect for [X years] from the date of signing.\n4. Governing Law: This Agreement is governed by the laws of India.\n\nSigned:\n" + biz + " ______________     [Other Party] ______________" + LEGAL_DISCLAIMER;
      case "manufacturer-agreement":
        return "MANUFACTURER / VENDOR AGREEMENT\n\nBetween " + biz + " (\"Brand\") and [Manufacturer Name] (\"Manufacturer\").\n\n1. Scope: Manufacture of [product] as per agreed specifications & samples.\n2. Minimum Order Quantity (MOQ): [___]\n3. Pricing: [___] per unit, subject to review every [___] months.\n4. Quality Standards: As per approved sample dated [___].\n5. Lead Time: [___] days from order confirmation.\n6. Payment Terms: [e.g. 50% advance, 50% before dispatch].\n7. Confidentiality: Manufacturer agrees not to share Brand's designs with third parties.\n8. Termination: Either party may terminate with [30] days written notice." + LEGAL_DISCLAIMER;
      case "offer-letter":
        return "OFFER LETTER\n\nDate: [___]\n\nDear [Candidate Name],\n\nWe are pleased to offer you the position of [Role] at " + biz + ", based in " + loc + ".\n\nStart Date: [___]\nCompensation: ₹[___] per [month/annum]\nProbation Period: [___] months\nNotice Period: [___] days\nReporting To: [___]\n\nPlease sign below to confirm your acceptance.\n\nEmployer: ______________     Employee: ______________" + LEGAL_DISCLAIMER;
      case "vendor-agreement":
        return "VENDOR AGREEMENT\n\nBetween " + biz + " and [Vendor Name] for supply of [goods/services].\n\n1. Scope of Supply: [___]\n2. Pricing & Payment Terms: [___]\n3. Delivery Timelines: [___]\n4. Quality/Return Policy: [___]\n5. Term & Termination: [___]" + LEGAL_DISCLAIMER;
      case "return-policy":
        return "RETURN & REFUND POLICY — " + biz + "\n\nWe want you to love your purchase. If you're not satisfied:\n\n1. Returns accepted within [7/15/30] days of delivery.\n2. Product must be unused, in original packaging with tags intact.\n3. Refunds are processed within [5-7] business days after we receive the returned item.\n4. Return shipping: [Free / Customer pays].\n5. Non-returnable items: [list, if any].\n\nFor return requests, contact: [support email/phone]." + LEGAL_DISCLAIMER;
      case "privacy-policy":
        return "PRIVACY POLICY — " + biz + "\n\nWe collect [name, email, phone, address, payment details] to process your orders and improve our service.\n\nWe do not sell your personal data to third parties. We may share data with logistics/payment partners solely to fulfil your order.\n\nYou may request access to or deletion of your data by contacting [support email].\n\nThis policy is governed by applicable Indian data protection law (Information Technology Act, 2000 and the Digital Personal Data Protection Act, 2023)." + LEGAL_DISCLAIMER;
      case "terms":
        return "TERMS & CONDITIONS — " + biz + "\n\n1. By using this website/purchasing our products, you agree to these terms.\n2. All prices are listed in INR (₹) and inclusive of applicable GST unless stated otherwise.\n3. We reserve the right to modify pricing and availability without prior notice.\n4. Orders are subject to acceptance and availability of stock.\n5. Disputes are subject to the jurisdiction of courts in " + (p.city || "[City]") + ", India." + LEGAL_DISCLAIMER;
      default:
        return "Template not found.";
    }
  }

  // -----------------------------------------------------------------------
  // Timeline Planner
  // -----------------------------------------------------------------------
  renderers.timeline = function () {
    var p = state.profile;
    var applicableCats = KB.CHECKLIST_CATEGORIES.filter(function (cat) { return categoryStats(cat, p).total > 0; });
    var weeks = state.timelineWeeks;
    var groupSize = Math.max(1, Math.ceil(applicableCats.length / weeks));
    var groups = [];
    for (var i = 0; i < applicableCats.length; i += groupSize) groups.push(applicableCats.slice(i, i + groupSize));

    var firstActiveAssigned = false;
    var html = groups.map(function (group, idx) {
      var totalItems = 0, doneItems = 0;
      group.forEach(function (cat) {
        var s = categoryStats(cat, p);
        totalItems += s.total; doneItems += s.done;
      });
      var status = "pending";
      if (totalItems > 0 && doneItems === totalItems) status = "done";
      else if (doneItems > 0) status = "active";
      else if (!firstActiveAssigned) { status = "active"; firstActiveAssigned = true; }

      var isLast = idx === groups.length - 1;
      return '<div class="timeline-week">' +
        '<div class="timeline-dot-col"><div class="timeline-dot ' + status + '"></div>' +
        (isLast ? "" : '<div class="timeline-line"></div>') + '</div>' +
        '<div class="timeline-content"><div class="timeline-week-label">Week ' + (idx + 1) + '</div>' +
        '<div class="timeline-week-desc">' + group.map(function (c) { return c.icon + " " + c.name; }).join(" + ") + '</div></div>' +
        '</div>';
    }).join("");

    qs("#timeline-body").innerHTML =
      '<div class="card"><div class="card-title">Launch Timeline (' + weeks + ' Weeks)</div></div>' +
      html +
      '<button class="btn-primary" style="width:100%;margin-top:10px" id="timeline-customize-btn">Customize Timeline (' + weeks + ' weeks) ⚙️</button>';

    qs("#timeline-customize-btn").addEventListener("click", function () {
      var options = [4, 6, 8, 10];
      var idx = options.indexOf(state.timelineWeeks);
      state.timelineWeeks = options[(idx + 1) % options.length];
      saveState();
      renderers.timeline();
    });
  };

  // -----------------------------------------------------------------------
  // Funding & Support
  // -----------------------------------------------------------------------
  var fundingFilter = "all";
  renderers.funding = function () {
    var items = KB.FUNDING_SCHEMES.filter(function (f) { return fundingFilter === "all" || f.type === fundingFilter; });
    qs("#funding-body").innerHTML = items.map(function (f) {
      var applyBtn = f.url ? '<a class="funding-apply-btn" style="text-decoration:none" href="' + f.url + '">Apply / Learn More →</a>' :
        '<span class="funding-apply-btn" style="opacity:.6">Search online →</span>';
      return '<div class="funding-card"><div class="funding-name">' + escapeHtml(f.name) + '</div>' +
        '<div class="funding-summary">' + escapeHtml(f.summary) + '</div>' + applyBtn + '</div>';
    }).join("") + '<div class="empty-note">Figures shown are approximate general-knowledge estimates — always confirm current terms on the official portal.</div>';
  };
  qs("#funding-tabs").addEventListener("click", function (e) {
    var t = e.target.closest(".tab");
    if (!t) return;
    qsa(".tab", qs("#funding-tabs")).forEach(function (x) { x.classList.remove("active"); });
    t.classList.add("active");
    fundingFilter = t.getAttribute("data-funding-filter");
    renderers.funding();
  });

  // -----------------------------------------------------------------------
  // Profile & Settings
  // -----------------------------------------------------------------------
  renderers.profile = function () {
    var p = state.profile;
    qs("#profile-body").innerHTML =
      '<div class="profile-hero"><div class="profile-avatar">🚀</div><div>' +
      '<div class="profile-name">' + escapeHtml(p.founderName) + '</div>' +
      '<div class="profile-email">' + escapeHtml(p.businessName || "My Business") + ' · ' + escapeHtml([p.city, p.state].filter(Boolean).join(", ") || "India") + '</div>' +
      '</div></div>' +
      '<div class="profile-menu">' +
      '<div class="profile-menu-row" data-edit-profile>✎ Edit Business Profile <span>›</span></div>' +
      '<div class="profile-menu-row" data-open-screen="documents">📄 Documents & Templates <span>›</span></div>' +
      '<div class="profile-menu-row" data-open-screen="funding">🏦 Funding & Support <span>›</span></div>' +
      '<div class="profile-menu-row" id="profile-adhd-toggle">🧩 Simplified (ADHD-friendly) Mode <span>' + (p.adhdMode ? "ON" : "OFF") + '</span></div>' +
      '<div class="profile-menu-row" id="profile-about-row">ℹ️ About LaunchPilot AI <span>›</span></div>' +
      '<div class="profile-menu-row danger" id="profile-reset-row">🗑 Reset App Data <span>›</span></div>' +
      '</div>' +
      '<div class="empty-note">LaunchPilot AI works fully offline. Your data is stored only on this device.</div>';

    qs("#profile-adhd-toggle").addEventListener("click", function () {
      state.profile.adhdMode = !state.profile.adhdMode;
      saveState();
      renderers.profile();
    });
    qs("#profile-about-row").addEventListener("click", function () {
      showToast("LaunchPilot AI — Your India Business Launch OS. All data stays on your device.");
    });
    qs("#profile-reset-row").addEventListener("click", function () {
      if (confirm("This will erase all your business data on this device. Continue?")) {
        localStorage.removeItem(STORAGE_KEY);
        state = loadState();
        startOnboarding(false);
      }
    });
  };

  // -----------------------------------------------------------------------
  // Boot
  // -----------------------------------------------------------------------
  function runScreenRenderer(id, params) {
    if (renderers[id]) renderers[id](params || {});
  }

  initOnboardingFields();

  if (state.onboardingComplete) {
    stack = [{ id: "home", params: {} }];
    renderStack();
  } else {
    startOnboarding(false);
  }
})();
