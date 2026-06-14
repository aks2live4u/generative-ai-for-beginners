/* ==========================================================================
   Brain Dump — Mood Analytics screen
   ========================================================================== */

(function () {
  window.Views = window.Views || {};

  Views.mood = {
    render(container) {
      function renderMoodList(statsObj) {
        const entries = Object.entries(statsObj).sort((a, b) => b[1] - a[1]);
        if (entries.length === 0) {
          return '<p class="settings-note">No moods logged yet.</p>';
        }
        const max = Math.max(...entries.map(([, c]) => c));
        return '<div class="mood-list">' + entries.map(([key, count]) => {
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

      function renderChart(daily) {
        const dayInfo = daily.map((d) => {
          let dominant = null;
          let dominantCount = 0;
          let total = 0;
          Object.entries(d.moods).forEach(([key, count]) => {
            total += count;
            if (count > dominantCount) { dominant = key; dominantCount = count; }
          });
          return { date: d.date, total, dominant };
        });
        const max = Math.max(1, ...dayInfo.map((d) => d.total));

        const cols = dayInfo.map((d) => {
          const mood = d.dominant ? Catalog.moodByKey(d.dominant) : null;
          const pct = Math.round((d.total / max) * 100);
          const label = DateUtil.WEEKDAYS[new Date(d.date).getDay()].slice(0, 3);
          return '<div class="mood-chart__col">' +
            '<span style="font-size:1.1rem">' + (mood ? mood.emoji : "") + "</span>" +
            '<div class="mood-chart__bar" style="height:' + pct + '%"></div>' +
            '<div class="mood-chart__label">' + label + "</div>" +
            "</div>";
        }).join("");

        return '<div class="card"><h3 style="margin:0 0 4px;font-size:0.95rem">Last 7 Days</h3>' +
          '<div class="mood-chart">' + cols + "</div></div>";
      }

      function buildHtml() {
        const stats = Bridge.getMoodStats();

        const topbar = '<div class="topbar">' +
          '<button class="icon-btn" data-action="back">' + Icon("arrow-left") + "</button>" +
          '<div class="topbar-back-title">Mood Analytics</div>' +
          '<div style="width:40px"></div>' +
          "</div>";

        const chart = renderChart(stats.daily);

        const recentSection = '<div class="settings-section"><div class="settings-section__title">Last 30 Days</div>' +
          '<div class="card">' + renderMoodList(stats.recent) + "</div></div>";

        const allTimeSection = '<div class="settings-section"><div class="settings-section__title">All Time</div>' +
          '<div class="card">' + renderMoodList(stats.allTime) + "</div></div>";

        return topbar + '<div class="screen">' + chart + recentSection + allTimeSection + "</div>";
      }

      function attach() {
        const backBtn = container.querySelector('[data-action="back"]');
        if (backBtn) backBtn.addEventListener("click", () => Router.pop());
      }

      container.innerHTML = buildHtml();
      attach();

      return () => {};
    }
  };
})();
