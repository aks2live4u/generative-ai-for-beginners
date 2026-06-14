/* ==========================================================================
   Brain Dump — Editor screen (view / edit an existing thought)
   ========================================================================== */

(function () {
  window.Views = window.Views || {};

  const AI_PRESETS = [
    { label: "Fix Grammar", icon: "check", instruction: "Fix any grammar and spelling mistakes. Keep the meaning, tone and language unchanged." },
    { label: "Make Longer", icon: "plus", instruction: "Expand this journal entry with more detail and reflection, while keeping the same voice." },
    { label: "Make Shorter", icon: "crop", instruction: "Make this journal entry more concise without losing the key points." },
    { label: "Cheer Me Up", icon: "smile", instruction: "Rewrite this in a more positive, encouraging tone while staying authentic to the original." },
    { label: "Summarize", icon: "file-text", instruction: "Summarize this journal entry in a few short sentences." }
  ];

  Views.editor = {
    render(container, { thoughtId }) {
      const original = Bridge.getThought(thoughtId);
      let thought = Object.assign({}, original);
      let moodPickerOpen = false;
      let busy = false;
      let recording = false;
      let recStart = 0;
      let recTimer = null;
      let saveDebounce = null;

      // ---- persistence ---------------------------------------------------------

      function doSave() {
        if (saveDebounce) { clearTimeout(saveDebounce); saveDebounce = null; }
        const updated = Bridge.updateThought(thought.id, thought);
        thought = updated;
      }

      function scheduleSave() {
        if (saveDebounce) clearTimeout(saveDebounce);
        saveDebounce = setTimeout(doSave, 600);
      }

      // ---- rendering -------------------------------------------------------------

      function formatCreated(ts) {
        const diff = Date.now() - ts;
        if (diff < 60000) return "Just now";
        const settings = Store.settings;
        return DateUtil.formatDate(new Date(ts), settings.dateFormat) + " · " + DateUtil.formatTime(new Date(ts), settings.use24HourTime);
      }

      function buildHtml() {
        const settings = Store.settings;
        const moodInfo = thought.mood ? Catalog.moodByKey(thought.mood) : null;
        const mediaBase = Store.mediaBaseUrl;

        const topbar = '<div class="topbar">' +
          '<button class="icon-btn" data-action="back">' + Icon("arrow-left") + "</button>" +
          '<div class="topbar-back-title">Edit Thought</div>' +
          '<button class="icon-btn" data-action="delete">' + Icon("trash") + "</button>" +
          "</div>";

        const moodCard = '<div class="card" style="display:flex;align-items:center;gap:14px">' +
          '<button data-action="toggle-mood" style="width:56px;height:56px;border-radius:14px;border:2px solid var(--accent);background:transparent;display:flex;align-items:center;justify-content:center;font-size:1.6rem;flex-shrink:0;color:var(--accent)">' +
          (moodInfo ? moodInfo.emoji : Icon("lightbulb")) +
          "</button>" +
          '<div style="flex:1">' +
          '<div style="color:var(--text-secondary);font-size:0.85rem">Created: ' + formatCreated(thought.createdAt) + "</div>" +
          (settings.enableMoodOptions
            ? '<button class="btn btn--small btn--secondary" data-action="toggle-mood" style="margin-top:8px">' +
              (moodInfo ? moodInfo.emoji + " " + moodInfo.label : "Set mood") + "</button>"
            : "") +
          "</div></div>" +
          (moodPickerOpen && settings.enableMoodOptions
            ? '<div class="mood-picker" style="margin:-4px 0 14px">' + Catalog.MOODS.map((m) =>
              '<button class="mood-chip ' + (thought.mood === m.key ? "selected" : "") + '" data-mood="' + m.key + '">' + m.emoji + " " + m.label + "</button>"
            ).join("") + "</div>"
            : "");

        let attachmentsInner = (thought.images || []).map((img, i) =>
          '<div class="editor-attachment"><img src="' + mediaBase + img + '">' +
          '<button class="editor-attachment__remove" data-action="remove-image" data-index="' + i + '">' + Icon("x") + "</button></div>"
        ).join("");

        if (thought.audioPath) {
          attachmentsInner += '<div class="editor-attachment" style="display:flex;align-items:center;justify-content:center;color:var(--accent)">' +
            Icon("music") +
            '<button class="editor-attachment__remove" data-action="remove-audio">' + Icon("x") + "</button></div>";
        }

        attachmentsInner += '<button class="editor-add-attachment" data-action="attach-camera">' + Icon("camera") + "</button>";
        attachmentsInner += '<button class="editor-add-attachment" data-action="attach-gallery">' + Icon("image") + "</button>";
        if (!thought.audioPath && !recording) {
          attachmentsInner += '<button class="editor-add-attachment" data-action="attach-mic">' + Icon("mic") + "</button>";
        }

        let audioPlayer = "";
        if (thought.audioPath) {
          audioPlayer = '<div class="thought-card__audio" style="margin-top:12px">' + Icon("music") +
            '<audio controls src="' + mediaBase + thought.audioPath + '"></audio>' +
            (thought.audioDuration ? "<span>" + DateUtil.formatDuration(thought.audioDuration) + "</span>" : "") +
            "</div>";
        }
        if (thought.audioTranscript) {
          audioPlayer += '<div class="thought-card__transcript" style="margin-top:8px">“' + UI.escapeHtml(thought.audioTranscript) + '”</div>';
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

        const attachmentsCard = '<div class="card">' +
          '<h3 style="margin:0 0 12px;font-size:0.95rem;color:var(--text-secondary)">Attachments</h3>' +
          '<div class="editor-attachments">' + attachmentsInner + "</div>" +
          audioPlayer + recordingRow +
          "</div>";

        const words = thought.text.trim() ? thought.text.trim().split(/\s+/).length : 0;

        const editorBlock = '<div style="position:relative">' +
          '<textarea class="editor-textarea" id="editor-text" placeholder="Write your thought…">' + UI.escapeHtml(thought.text) + "</textarea>" +
          '<button class="icon-btn icon-btn--accent" data-action="ai-sparkle" style="position:absolute;bottom:12px;right:6px">' +
          (busy ? '<div class="spinner"></div>' : Icon("sparkles")) +
          "</button>" +
          "</div>" +
          '<div class="editor-footer"><span id="char-count">Characters: ' + thought.text.length + '</span><span id="word-count">Words: ' + words + "</span></div>" +
          '<div class="editor-ai-bar">' + AI_PRESETS.map((p) =>
            '<button class="editor-ai-chip" data-instruction="' + UI.escapeHtml(p.instruction) + '">' + Icon(p.icon) + " " + p.label + "</button>"
          ).join("") + "</div>";

        const screen = '<div class="screen">' + moodCard + attachmentsCard + editorBlock + "</div>";

        return topbar + screen;
      }

      // ---- actions ---------------------------------------------------------------

      function renderAll() {
        container.innerHTML = buildHtml();
        attach();
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
        if (!settings.useAudioContext || !Store.aiStatus.hasGeminiKey || !thought.audioPath) return;
        Bridge.transcribeAudio(thought.audioPath).then((res) => {
          if (res.success) {
            thought.audioTranscript = res.transcript;
            doSave();
            renderAll();
          }
        });
      }

      function attach() {
        const q = (sel) => container.querySelector(sel);

        const backBtn = q('[data-action="back"]');
        if (backBtn) backBtn.addEventListener("click", () => Router.pop());

        const deleteBtn = q('[data-action="delete"]');
        if (deleteBtn) deleteBtn.addEventListener("click", () => {
          UI.confirmDialog({
            title: "Delete thought?",
            message: "This will permanently delete this thought and any attached media.",
            confirmLabel: "Delete",
            danger: true,
            onConfirm: () => {
              if (saveDebounce) clearTimeout(saveDebounce);
              Bridge.deleteThought(thought.id);
              Router.pop();
            }
          });
        });

        container.querySelectorAll('[data-action="toggle-mood"]').forEach((btn) => {
          btn.addEventListener("click", () => {
            moodPickerOpen = !moodPickerOpen;
            renderAll();
          });
        });

        container.querySelectorAll("[data-mood]").forEach((chip) => {
          chip.addEventListener("click", () => {
            const key = chip.dataset.mood;
            thought.mood = thought.mood === key ? null : key;
            moodPickerOpen = false;
            doSave();
            renderAll();
          });
        });

        const textarea = q("#editor-text");
        if (textarea) {
          textarea.addEventListener("input", (e) => {
            thought.text = e.target.value;
            const charEl = q("#char-count");
            const wordEl = q("#word-count");
            const words = thought.text.trim() ? thought.text.trim().split(/\s+/).length : 0;
            if (charEl) charEl.textContent = "Characters: " + thought.text.length;
            if (wordEl) wordEl.textContent = "Words: " + words;
            scheduleSave();
          });
        }

        const aiSparkle = q('[data-action="ai-sparkle"]');
        if (aiSparkle) aiSparkle.addEventListener("click", () => runAiRewrite("Improve this journal entry: fix grammar and clarity while keeping the meaning and tone."));

        container.querySelectorAll(".editor-ai-chip").forEach((chip) => {
          chip.addEventListener("click", () => runAiRewrite(chip.dataset.instruction));
        });

        container.querySelectorAll('[data-action="remove-image"]').forEach((btn) => {
          btn.addEventListener("click", () => {
            const idx = Number(btn.dataset.index);
            const name = thought.images[idx];
            thought.images = thought.images.slice();
            thought.images.splice(idx, 1);
            Bridge.removeImage(name);
            doSave();
            renderAll();
          });
        });

        const removeAudio = q('[data-action="remove-audio"]');
        if (removeAudio) removeAudio.addEventListener("click", () => {
          Bridge.removeImage(thought.audioPath);
          thought.audioPath = null;
          thought.audioDuration = null;
          thought.audioTranscript = null;
          doSave();
          renderAll();
        });

        const cameraBtn = q('[data-action="attach-camera"]');
        if (cameraBtn) cameraBtn.addEventListener("click", () => {
          Bridge.pickImage("camera").then((res) => {
            if (res.success) {
              thought.images = (thought.images || []).concat(res.name);
              doSave();
              renderAll();
            } else if (res.error !== "cancelled") {
              UI.toast(res.error, true);
            }
          });
        });

        const galleryBtn = q('[data-action="attach-gallery"]');
        if (galleryBtn) galleryBtn.addEventListener("click", () => {
          Bridge.pickImage("gallery").then((res) => {
            if (res.success) {
              thought.images = (thought.images || []).concat(res.name);
              doSave();
              renderAll();
            } else if (res.error !== "cancelled") {
              UI.toast(res.error, true);
            }
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
              thought.audioPath = res.name;
              thought.audioDuration = res.durationMs;
              doSave();
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
      }

      function runAiRewrite(instruction) {
        if (busy) return;
        if (!thought.text.trim()) { UI.toast("Write something first"); return; }
        if (!Store.aiStatus.hasClaudeKey) { UI.toast("Add a Claude API key in AI Assistant Setup", true); return; }
        busy = true;
        renderAll();
        Bridge.aiRewrite({
          text: thought.text,
          instruction,
          images: thought.images,
          audioTranscript: thought.audioTranscript || ""
        }).then((res) => {
          busy = false;
          if (res.success) {
            thought.text = res.text;
            doSave();
          } else {
            UI.toast(res.error || "AI request failed", true);
          }
          renderAll();
        });
      }

      // ---- lifecycle ---------------------------------------------------------------

      renderAll();

      return () => {
        if (saveDebounce) { clearTimeout(saveDebounce); doSave(); }
        stopRecTimer();
        if (recording) Bridge.cancelRecording();
      };
    }
  };
})();
