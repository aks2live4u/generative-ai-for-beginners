/* ==========================================================================
   Brain Dump — Home screen (quote, thought list, composer)
   ========================================================================== */

(function () {
  window.Views = window.Views || {};

  Views.home = {
    render(container) {
      let draft = { text: "", mood: null, images: [], audioPath: null, audioDuration: null, audioTranscript: null };
      let attachOpen = false;
      let moodPickerOpen = false;
      let recording = false;
      let recStart = 0;
      let recTimer = null;
      let searchOpen = false;
      let searchQuery = "";
      let quote = Bridge.getQuote(false);
      let busy = false;
      let searchDebounce = null;

      // ---- rendering helpers ------------------------------------------------

      function themeModeIcon() {
        const mode = Store.settings.themeMode;
        if (mode === "light") return Icon("sun");
        if (mode === "dark") return Icon("moon");
        return Icon("contrast");
      }

      function renderThoughtList() {
        const thoughts = Bridge.getThoughts(searchQuery ? { search: searchQuery } : {});
        if (thoughts.length === 0) {
          return (
            '<div class="empty-state">' +
            Icon("notes-edit") +
            "<h2>" + (searchQuery ? "No matching thoughts" : "Start Your Brain Dump!") + "</h2>" +
            "<p>" + (searchQuery ? "Try a different search term." : "Tap the input box below to add your first thought") + "</p>" +
            "</div>"
          );
        }
        return '<div class="thought-list">' + thoughts.map(renderThoughtCard).join("") + "</div>";
      }

      function renderThoughtCard(t) {
        const settings = Store.settings;
        const mood = t.mood ? Catalog.moodByKey(t.mood) : null;
        const imageClass = settings.cropImagesToFill ? "thought-card__image--cover" : "thought-card__image--contain";

        let imagesHtml = "";
        if (t.images && t.images.length) {
          imagesHtml = '<div class="thought-card__images" style="--image-height:' + settings.imageHeight + 'px">' +
            t.images.map((img) => '<img class="thought-card__image ' + imageClass + '" src="' + Store.mediaBaseUrl + img + '">').join("") +
            "</div>";
        }

        let audioHtml = "";
        if (t.audioPath) {
          audioHtml = '<div class="thought-card__audio">' + Icon("music") +
            '<audio controls src="' + Store.mediaBaseUrl + t.audioPath + '"></audio>' +
            (t.audioDuration ? "<span>" + DateUtil.formatDuration(t.audioDuration) + "</span>" : "") +
            "</div>";
        }

        const transcriptHtml = t.audioTranscript
          ? '<div class="thought-card__transcript">“' + UI.escapeHtml(t.audioTranscript) + '”</div>'
          : "";

        return (
          '<div class="thought-card" data-id="' + t.id + '">' +
          '<div class="thought-card__head">' +
          (mood ? '<span class="thought-card__mood">' + mood.emoji + "</span>" : "") +
          '<span class="thought-card__time">' + DateUtil.relativeDateTime(t.createdAt, settings.use24HourTime, settings.dateFormat) + "</span>" +
          "</div>" +
          '<div class="thought-card__text" style="--thought-lines:' + settings.thoughtTextMaxLines + '">' + UI.escapeHtml(t.text) + "</div>" +
          imagesHtml + audioHtml + transcriptHtml +
          "</div>"
        );
      }

      function buildInputBar() {
        const settings = Store.settings;
        const moodInfo = draft.mood ? Catalog.moodByKey(draft.mood) : null;

        let chips = draft.images.map((img, i) =>
          '<div class="attachment-chip"><img src="' + Store.mediaBaseUrl + img + '">' +
          '<button class="attachment-chip__remove" data-action="remove-image" data-index="' + i + '">' + Icon("x") + "</button></div>"
        ).join("");
        if (draft.audioPath) {
          chips += '<div class="attachment-chip">' + Icon("music") +
            '<button class="attachment-chip__remove" data-action="remove-audio">' + Icon("x") + "</button></div>";
        }

        let addButtons = "";
        if (attachOpen && !recording) {
          addButtons =
            '<button class="attachment-add" data-action="attach-camera">' + Icon("camera") + "</button>" +
            '<button class="attachment-add" data-action="attach-gallery">' + Icon("image") + "</button>" +
            '<button class="attachment-add" data-action="attach-mic">' + Icon("mic") + "</button>";
        }

        let attachmentRow = "";
        if (chips || addButtons) {
          attachmentRow = '<div class="attachment-row">' + chips + addButtons + "</div>";
        }

        let moodRow = "";
        if (moodPickerOpen && settings.enableMoodOptions) {
          moodRow = '<div class="mood-picker">' + Catalog.MOODS.map((m) =>
            '<button class="mood-chip ' + (draft.mood === m.key ? "selected" : "") + '" data-mood="' + m.key + '">' + m.emoji + " " + m.label + "</button>"
          ).join("") + "</div>";
        }

        let recordingRow = "";
        if (recording) {
          recordingRow = '<div class="recording-indicator">' +
            '<div class="recording-indicator__dot"></div>' +
            "<span>Recording… <span id=\"rec-time\">0:00</span></span>" +
            '<button class="btn btn--small btn--secondary" data-action="stop-recording" style="width:auto;margin-left:auto">Stop</button>' +
            '<button class="btn btn--small btn--ghost" data-action="cancel-recording" style="width:auto">Cancel</button>' +
            "</div>";
        }

        const canSend = draft.text.trim().length > 0 || draft.images.length > 0 || !!draft.audioPath;

        const moodBtn = settings.enableMoodOptions
          ? '<button class="input-bar__icon input-bar__icon--mood" data-action="toggle-mood">' + (moodInfo ? moodInfo.emoji : Icon("lightbulb")) + "</button>"
          : '<div class="input-bar__icon input-bar__icon--mood">' + Icon("lightbulb") + "</div>";

        return (
          '<div class="input-bar-wrap">' +
          attachmentRow + moodRow + recordingRow +
          '<div class="input-bar ' + (recording ? "hidden" : "") + '">' +
          moodBtn +
          '<textarea class="input-bar__field" id="composer-text" placeholder="What are you thinking?" rows="' + settings.inputMaxLines + '">' + UI.escapeHtml(draft.text) + "</textarea>" +
          '<button class="input-bar__icon input-bar__icon--ai ' + (busy ? "active" : "") + '" data-action="ai-rewrite">' + (busy ? '<div class="spinner"></div>' : Icon("sparkles")) + "</button>" +
          '<button class="input-bar__icon input-bar__icon--attach" data-action="toggle-attach">' + Icon(attachOpen ? "x" : "plus") + "</button>" +
          '<button class="input-bar__send ' + (canSend ? "active" : "") + '" data-action="send">' + Icon("send") + " Dump</button>" +
          "</div>" +
          "</div>"
        );
      }

      function buildHtml() {
        const settings = Store.settings;
        const now = new Date();

        const topbar = '<div class="topbar">' +
          '<button class="icon-btn" data-action="menu">' + Icon("menu") + "</button>" +
          '<div class="topbar-title">Brain Dump</div>' +
          '<button class="icon-btn" data-action="search">' + Icon("search") + "</button>" +
          '<button class="icon-btn" data-action="theme-toggle">' + themeModeIcon() + "</button>" +
          "</div>";

        const searchBar = searchOpen
          ? '<div class="search-bar">' + Icon("search") +
            '<input type="text" id="search-input" placeholder="Search thoughts…" value="' + UI.escapeHtml(searchQuery) + '">' +
            "</div>"
          : "";

        const quoteCard = '<div class="quote-card">' +
          '<div class="quote-card__top"><div>' +
          '<div class="quote-card__greeting">' + DateUtil.greeting(now) + "</div>" +
          '<div class="quote-card__date" id="quote-date">' + DateUtil.formatDate(now, settings.dateFormat) + "</div>" +
          "</div>" +
          '<div class="quote-card__time" id="quote-time">' + DateUtil.formatTime(now, settings.use24HourTime) + "</div>" +
          "</div>" +
          '<div class="quote-card__body">' +
          '<span class="quote-card__mark">“</span>' +
          '<div style="flex:1"><div class="quote-card__text" id="quote-text">' + UI.escapeHtml(quote.text) + "</div>" +
          '<span class="quote-card__author" id="quote-author">— ' + UI.escapeHtml(quote.author) + "</span></div>" +
          '<button class="icon-btn quote-card__refresh" data-action="quote-refresh">' + Icon("refresh") + "</button>" +
          "</div></div>";

        const screen = '<div class="screen">' + searchBar + quoteCard +
          '<div id="thought-list-container">' + renderThoughtList() + "</div>" +
          "</div>";

        return topbar + screen + buildInputBar();
      }

      // ---- actions -------------------------------------------------------------

      function renderAll() {
        container.innerHTML = buildHtml();
        attach();
        if (searchOpen) {
          const input = container.querySelector("#search-input");
          if (input) { input.focus(); input.setSelectionRange(input.value.length, input.value.length); }
        }
      }

      function refreshList() {
        const el = container.querySelector("#thought-list-container");
        if (el) {
          el.innerHTML = renderThoughtList();
          attachThoughtCards();
        }
      }

      function applyQuote(text, author) {
        quote = { text, author };
        const textEl = container.querySelector("#quote-text");
        const authorEl = container.querySelector("#quote-author");
        if (textEl) textEl.textContent = text;
        if (authorEl) authorEl.textContent = "— " + author;
      }

      function refreshQuoteOnline() {
        return Bridge.fetchOnlineQuote().then((res) => {
          if (res.success) applyQuote(res.text, res.author);
          return res;
        });
      }

      function attachThoughtCards() {
        container.querySelectorAll(".thought-card").forEach((card) => {
          card.addEventListener("click", (e) => {
            if (e.target.closest("audio")) return;
            Router.push("editor", { thoughtId: Number(card.dataset.id) });
          });
        });
      }

      function startRecTimer() {
        recTimer = setInterval(() => {
          const el = container.querySelector("#rec-time");
          if (el) el.textContent = DateUtil.formatDuration(Date.now() - recStart);
        }, 500);
      }

      function stopRecTimer() {
        if (recTimer) { clearInterval(recTimer); recTimer = null; }
      }

      function maybeTranscribe() {
        const settings = Store.settings;
        if (!settings.useAudioContext || !Store.aiStatus.hasGeminiKey || !draft.audioPath) return;
        Bridge.transcribeAudio(draft.audioPath).then((res) => {
          if (res.success) {
            draft.audioTranscript = res.transcript;
            renderAll();
          }
        });
      }

      function attach() {
        const q = (sel) => container.querySelector(sel);

        const composer = q("#composer-text");
        if (composer) {
          composer.addEventListener("input", (e) => {
            draft.text = e.target.value;
            const sendBtn = q('[data-action="send"]');
            const canSend = draft.text.trim().length > 0 || draft.images.length > 0 || !!draft.audioPath;
            if (sendBtn) sendBtn.classList.toggle("active", canSend);
          });
          composer.addEventListener("focus", () => {
            if (attachOpen || moodPickerOpen) { attachOpen = false; moodPickerOpen = false; renderAll(); }
          });
        }

        const menuBtn = q('[data-action="menu"]');
        if (menuBtn) menuBtn.addEventListener("click", () => AppShell.openDrawer());

        const searchBtn = q('[data-action="search"]');
        if (searchBtn) searchBtn.addEventListener("click", () => {
          searchOpen = !searchOpen;
          if (!searchOpen) searchQuery = "";
          renderAll();
        });

        const searchInput = q("#search-input");
        if (searchInput) searchInput.addEventListener("input", (e) => {
          const value = e.target.value;
          if (searchDebounce) clearTimeout(searchDebounce);
          searchDebounce = setTimeout(() => { searchQuery = value; refreshList(); }, 200);
        });

        const themeBtn = q('[data-action="theme-toggle"]');
        if (themeBtn) themeBtn.addEventListener("click", () => {
          const modes = ["system", "light", "dark"];
          const next = modes[(modes.indexOf(Store.settings.themeMode) + 1) % modes.length];
          Store.saveSettings({ themeMode: next });
          themeBtn.innerHTML = themeModeIcon();
          UI.toast("Theme mode: " + next.charAt(0).toUpperCase() + next.slice(1));
        });

        const quoteRefresh = q('[data-action="quote-refresh"]');
        if (quoteRefresh) quoteRefresh.addEventListener("click", () => {
          quoteRefresh.innerHTML = '<div class="spinner"></div>';
          refreshQuoteOnline().then((res) => {
            if (!res.success) {
              const local = Bridge.getQuote(true);
              applyQuote(local.text, local.author);
            }
            quoteRefresh.innerHTML = Icon("refresh");
          });
        });

        attachThoughtCards();

        const toggleMood = q('[data-action="toggle-mood"]');
        if (toggleMood) toggleMood.addEventListener("click", () => {
          moodPickerOpen = !moodPickerOpen;
          attachOpen = false;
          renderAll();
        });

        container.querySelectorAll("[data-mood]").forEach((chip) => {
          chip.addEventListener("click", () => {
            const key = chip.dataset.mood;
            draft.mood = draft.mood === key ? null : key;
            moodPickerOpen = false;
            renderAll();
          });
        });

        const toggleAttach = q('[data-action="toggle-attach"]');
        if (toggleAttach) toggleAttach.addEventListener("click", () => {
          attachOpen = !attachOpen;
          moodPickerOpen = false;
          renderAll();
        });

        const cameraBtn = q('[data-action="attach-camera"]');
        if (cameraBtn) cameraBtn.addEventListener("click", () => {
          Bridge.pickImage("camera").then((res) => {
            if (res.success) { draft.images.push(res.name); renderAll(); }
            else if (res.error !== "cancelled") { UI.toast(res.error, true); }
          });
        });

        const galleryBtn = q('[data-action="attach-gallery"]');
        if (galleryBtn) galleryBtn.addEventListener("click", () => {
          Bridge.pickImage("gallery").then((res) => {
            if (res.success) { draft.images.push(res.name); renderAll(); }
            else if (res.error !== "cancelled") { UI.toast(res.error, true); }
          });
        });

        const micBtn = q('[data-action="attach-mic"]');
        if (micBtn) micBtn.addEventListener("click", () => {
          Bridge.startRecording().then((res) => {
            if (res.success) {
              recording = true;
              recStart = Date.now();
              startRecTimer();
              renderAll();
            } else {
              UI.toast(res.error || "Could not start recording", true);
            }
          });
        });

        const stopBtn = q('[data-action="stop-recording"]');
        if (stopBtn) stopBtn.addEventListener("click", () => {
          Bridge.stopRecording().then((res) => {
            recording = false;
            stopRecTimer();
            if (res.success) {
              draft.audioPath = res.name;
              draft.audioDuration = res.durationMs;
              attachOpen = false;
              renderAll();
              maybeTranscribe();
            } else {
              UI.toast(res.error || "Could not save recording", true);
              renderAll();
            }
          });
        });

        const cancelRecBtn = q('[data-action="cancel-recording"]');
        if (cancelRecBtn) cancelRecBtn.addEventListener("click", () => {
          Bridge.cancelRecording();
          recording = false;
          stopRecTimer();
          renderAll();
        });

        container.querySelectorAll('[data-action="remove-image"]').forEach((btn) => {
          btn.addEventListener("click", () => {
            const idx = Number(btn.dataset.index);
            const name = draft.images[idx];
            draft.images.splice(idx, 1);
            Bridge.removeImage(name);
            renderAll();
          });
        });

        const removeAudio = q('[data-action="remove-audio"]');
        if (removeAudio) removeAudio.addEventListener("click", () => {
          Bridge.removeImage(draft.audioPath);
          draft.audioPath = null;
          draft.audioDuration = null;
          draft.audioTranscript = null;
          renderAll();
        });

        const aiBtn = q('[data-action="ai-rewrite"]');
        if (aiBtn) aiBtn.addEventListener("click", () => {
          if (busy) return;
          if (!draft.text.trim()) { UI.toast("Write something first"); return; }
          if (!Store.aiStatus.hasClaudeKey) { UI.toast("Add a Claude API key in AI Assistant Setup", true); return; }
          busy = true;
          renderAll();
          Bridge.aiRewrite({
            text: draft.text,
            instruction: "Improve this journal entry: fix grammar and clarity while keeping the meaning and tone.",
            images: draft.images,
            audioTranscript: draft.audioTranscript || ""
          }).then((res) => {
            busy = false;
            if (res.success) draft.text = res.text;
            else UI.toast(res.error || "AI request failed", true);
            renderAll();
          });
        });

        const sendBtn = q('[data-action="send"]');
        if (sendBtn) sendBtn.addEventListener("click", () => {
          const canSend = draft.text.trim().length > 0 || draft.images.length > 0 || !!draft.audioPath;
          if (!canSend) return;
          const now = Date.now();
          Bridge.addThought({
            text: draft.text.trim(),
            mood: draft.mood,
            images: draft.images,
            audioPath: draft.audioPath,
            audioDuration: draft.audioDuration,
            audioTranscript: draft.audioTranscript,
            createdAt: now,
            updatedAt: now
          });
          draft = { text: "", mood: null, images: [], audioPath: null, audioDuration: null, audioTranscript: null };
          attachOpen = false;
          moodPickerOpen = false;
          renderAll();
        });
      }

      // ---- lifecycle ---------------------------------------------------------

      renderAll();

      // Fetch a fresh quote from the internet in the background and swap it
      // in once it arrives, without disturbing the rest of the screen.
      refreshQuoteOnline();

      const clockTimer = setInterval(() => {
        const now = new Date();
        const dateEl = container.querySelector("#quote-date");
        const timeEl = container.querySelector("#quote-time");
        if (dateEl) dateEl.textContent = DateUtil.formatDate(now, Store.settings.dateFormat);
        if (timeEl) timeEl.textContent = DateUtil.formatTime(now, Store.settings.use24HourTime);
      }, 30000);

      return () => {
        clearInterval(clockTimer);
        stopRecTimer();
        if (recording) Bridge.cancelRecording();
      };
    }
  };
})();
