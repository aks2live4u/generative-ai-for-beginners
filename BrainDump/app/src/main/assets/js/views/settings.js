/* ==========================================================================
   Brain Dump — Settings screen + Theme picker, Font picker, AI Assistant Setup
   ========================================================================== */

(function () {
  window.Views = window.Views || {};

  // ---- shared row builders ----------------------------------------------------

  function backTopbar(title) {
    return '<div class="topbar">' +
      '<button class="icon-btn" data-action="back">' + Icon("arrow-left") + "</button>" +
      '<div class="topbar-back-title">' + title + "</div>" +
      '<div style="width:40px"></div>' +
      "</div>";
  }

  function section(title, itemsHtml) {
    return '<div class="settings-section"><div class="settings-section__title">' + title + '</div>' +
      '<div class="settings-group">' + itemsHtml + "</div></div>";
  }

  function itemShell(icon, title, subtitle, controlHtml, extraAttrs) {
    return '<div class="settings-item"' + (extraAttrs || "") + '>' +
      '<div class="settings-item__icon">' + Icon(icon) + "</div>" +
      '<div class="settings-item__text"><div class="settings-item__title">' + title + "</div>" +
      (subtitle ? '<div class="settings-item__subtitle">' + subtitle + "</div>" : "") +
      "</div>" +
      '<div class="settings-item__control">' + controlHtml + "</div>" +
      "</div>";
  }

  function switchItem(icon, title, subtitle, on, action) {
    return itemShell(icon, title, subtitle, '<button class="switch ' + (on ? "on" : "") + '" data-action="' + action + '"></button>');
  }

  function stepperItem(icon, title, subtitle, value, action) {
    return itemShell(icon, title, subtitle,
      '<div class="stepper">' +
      '<button class="stepper__btn" data-action="' + action + '" data-step="-1">-</button>' +
      '<span class="stepper__value">' + value + "</span>" +
      '<button class="stepper__btn" data-action="' + action + '" data-step="1">+</button>' +
      "</div>");
  }

  function sliderItem(icon, title, value, display, min, max, step, action) {
    return itemShell(icon, title, '<span id="' + action + '-label">' + display + "</span>",
      '<input type="range" class="slider" data-action="' + action + '" min="' + min + '" max="' + max + '" step="' + step + '" value="' + value + '">');
  }

  function selectItem(icon, title, subtitle, value, options, action) {
    const optionsHtml = options.map((o) =>
      '<option value="' + o.value + '"' + (o.value === value ? " selected" : "") + ">" + o.label + "</option>"
    ).join("");
    return itemShell(icon, title, subtitle, '<select class="select" data-action="' + action + '">' + optionsHtml + "</select>");
  }

  function buttonItem(icon, title, subtitle, action, danger) {
    return '<div class="settings-item settings-item--button" data-action="' + action + '">' +
      '<div class="settings-item__icon">' + Icon(icon) + "</div>" +
      '<div class="settings-item__text"><div class="settings-item__title"' + (danger ? ' style="color:var(--danger)"' : "") + ">" + title + "</div>" +
      (subtitle ? '<div class="settings-item__subtitle">' + subtitle + "</div>" : "") +
      "</div>" +
      Icon("chevron-right", 'class="chevron"') +
      "</div>";
  }

  // ---- Main settings screen ----------------------------------------------------

  Views.settings = {
    render(container) {
      function buildHtml() {
        const s = Store.settings;
        const sec = Store.security;
        const ai = Store.aiStatus;

        const appPrefs = section("App Preferences",
          sliderItem("sliders", "Font Size", s.fontSizePercent, s.fontSizePercent + "%", 80, 150, 10, "font-size") +
          switchItem("smile", "Enable Mood Options", "Show mood picker when writing thoughts", s.enableMoodOptions, "toggle-mood-options") +
          stepperItem("edit", "Input Box Lines", "Default height of the composer", s.inputMaxLines, "input-max-lines") +
          selectItem("image", "Image Quality", "Higher quality uses more storage", s.imageQuality, [
            { value: "low", label: "Low" },
            { value: "medium", label: "Medium" },
            { value: "high", label: "High" }
          ], "image-quality")
        );

        const thoughtList = section("Thought List",
          stepperItem("file-text", "Text Preview Lines", "Max lines shown per thought on the home screen", s.thoughtTextMaxLines, "thought-max-lines") +
          sliderItem("images", "Image Height", s.imageHeight, s.imageHeight + "px", 80, 240, 10, "image-height") +
          switchItem("crop", "Crop Images to Fill", "Fill the row height, cropping overflow", s.cropImagesToFill, "toggle-crop-images")
        );

        const homeHeader = section("Home Header",
          switchItem("clock", "24-Hour Time", "", s.use24HourTime, "toggle-24h") +
          selectItem("calendar", "Date Format", "", s.dateFormat, Catalog.DATE_FORMATS.map((f) => ({ value: f, label: DateUtil.formatDate(new Date(), f) })), "date-format")
        );

        const aiSection = section("AI Assistance",
          buttonItem("bot", "AI Assistant Setup",
            (ai.hasClaudeKey ? "Claude connected" : "Claude not configured") + " · " + (ai.hasGeminiKey ? "Gemini connected" : "Gemini not configured"),
            "open-ai-setup") +
          switchItem("image", "Use Image Context", "Let the AI see attached images when rewriting", s.useImageContext, "toggle-image-context") +
          switchItem("mic", "Use Audio Context", "Let the AI use voice transcripts (via Gemini)", s.useAudioContext, "toggle-audio-context")
        );

        const appearance = section("Appearance",
          selectItem("contrast", "Theme Mode", "", s.themeMode, [
            { value: "system", label: "System" },
            { value: "light", label: "Light" },
            { value: "dark", label: "Dark" }
          ], "theme-mode") +
          buttonItem("type", "Font", (Catalog.FONTS.find((f) => f.key === s.fontFamily) || Catalog.FONTS[0]).name, "open-font-picker") +
          buttonItem("palette", "Theme Color", (Catalog.THEMES.find((t) => t.key === s.themeColor) || Catalog.THEMES[0]).name, "open-theme-picker")
        );

        let securityItems = switchItem("lock", "PIN Protection", "Require a PIN to open the app", sec.hasPin, "toggle-pin");
        if (sec.hasPin) {
          securityItems += buttonItem("key", "Change PIN", "", "change-pin");
          if (sec.biometricAvailable) {
            securityItems += switchItem("fingerprint", "Fingerprint Unlock", "", sec.biometricEnabled, "toggle-biometric");
          }
        }
        const security = section("Security", securityItems);

        const dataManagement = section("Data Management",
          buttonItem("download", "Export Data", "Save all your thoughts and settings to a file", "export-data") +
          buttonItem("upload", "Import Data", "Add thoughts from a backup file", "import-data") +
          buttonItem("trash", "Delete All Data", "Permanently erase every thought and attachment", "delete-all", true)
        );

        const about = section("About",
          buttonItem("info", "About Brain Dump", "", "about")
        );

        const screen = '<div class="screen">' + appPrefs + thoughtList + homeHeader + aiSection + appearance + security + dataManagement + about + "</div>";

        return backTopbar("Settings") + screen;
      }

      function renderAll() {
        container.innerHTML = buildHtml();
        attach();
      }

      function openPinModal(title) {
        const modal = UI.showModal(
          "<h3>" + UI.escapeHtml(title) + "</h3>" +
          "<p>Enter a 4-digit PIN. You'll use this to unlock Brain Dump.</p>" +
          '<input type="password" inputmode="numeric" maxlength="4" pattern="[0-9]*" placeholder="New PIN" id="pin-new" class="text-input" style="margin-bottom:12px">' +
          '<input type="password" inputmode="numeric" maxlength="4" pattern="[0-9]*" placeholder="Confirm PIN" id="pin-confirm" class="text-input" style="margin-bottom:12px">' +
          '<div class="modal__actions">' +
          '<button class="btn btn--secondary" data-action="cancel">Cancel</button>' +
          '<button class="btn btn--primary" data-action="save">Save</button>' +
          "</div>"
        );
        modal.querySelector('[data-action="cancel"]').addEventListener("click", UI.hideModal);
        modal.querySelector('[data-action="save"]').addEventListener("click", () => {
          const a = modal.querySelector("#pin-new").value;
          const b = modal.querySelector("#pin-confirm").value;
          if (!/^\d{4}$/.test(a)) { UI.toast("PIN must be 4 digits", true); return; }
          if (a !== b) { UI.toast("PINs do not match", true); return; }
          Bridge.setPin(a);
          Store.refreshSecurity();
          UI.hideModal();
          renderAll();
        });
      }

      function attach() {
        const q = (sel) => container.querySelector(sel);

        const backBtn = q('[data-action="back"]');
        if (backBtn) backBtn.addEventListener("click", () => Router.pop());

        // sliders
        container.querySelectorAll("input.slider").forEach((el) => {
          el.addEventListener("input", () => {
            const label = q("#" + el.dataset.action + "-label");
            if (!label) return;
            if (el.dataset.action === "font-size") label.textContent = el.value + "%";
            if (el.dataset.action === "image-height") label.textContent = el.value + "px";
          });
          el.addEventListener("change", () => {
            if (el.dataset.action === "font-size") Store.saveSettings({ fontSizePercent: Number(el.value) });
            if (el.dataset.action === "image-height") Store.saveSettings({ imageHeight: Number(el.value) });
          });
        });

        // steppers
        container.querySelectorAll(".stepper__btn").forEach((btn) => {
          btn.addEventListener("click", () => {
            const action = btn.dataset.action;
            const step = Number(btn.dataset.step);
            if (action === "input-max-lines") {
              Store.saveSettings({ inputMaxLines: Math.min(6, Math.max(1, Store.settings.inputMaxLines + step)) });
            } else if (action === "thought-max-lines") {
              Store.saveSettings({ thoughtTextMaxLines: Math.min(10, Math.max(1, Store.settings.thoughtTextMaxLines + step)) });
            }
            renderAll();
          });
        });

        // selects
        container.querySelectorAll("select.select").forEach((el) => {
          el.addEventListener("change", () => {
            const action = el.dataset.action;
            if (action === "image-quality") Store.saveSettings({ imageQuality: el.value });
            else if (action === "date-format") Store.saveSettings({ dateFormat: el.value });
            else if (action === "theme-mode") Store.saveSettings({ themeMode: el.value });
            renderAll();
          });
        });

        // simple switches
        const switchHandlers = {
          "toggle-mood-options": () => Store.saveSettings({ enableMoodOptions: !Store.settings.enableMoodOptions }),
          "toggle-crop-images": () => Store.saveSettings({ cropImagesToFill: !Store.settings.cropImagesToFill }),
          "toggle-24h": () => Store.saveSettings({ use24HourTime: !Store.settings.use24HourTime }),
          "toggle-image-context": () => Store.saveSettings({ useImageContext: !Store.settings.useImageContext }),
          "toggle-audio-context": () => Store.saveSettings({ useAudioContext: !Store.settings.useAudioContext })
        };
        container.querySelectorAll(".switch").forEach((btn) => {
          const handler = switchHandlers[btn.dataset.action];
          if (handler) btn.addEventListener("click", () => { handler(); renderAll(); });
        });

        // PIN protection
        const pinToggle = q('[data-action="toggle-pin"]');
        if (pinToggle) pinToggle.addEventListener("click", () => {
          if (Store.security.hasPin) {
            UI.confirmDialog({
              title: "Remove PIN?",
              message: "This will disable PIN and fingerprint lock for Brain Dump.",
              confirmLabel: "Remove",
              danger: true,
              onConfirm: () => {
                Bridge.removePin();
                Store.refreshSecurity();
                renderAll();
              }
            });
          } else {
            openPinModal("Set a PIN");
          }
        });

        const changePinBtn = q('[data-action="change-pin"]');
        if (changePinBtn) changePinBtn.addEventListener("click", () => openPinModal("Change PIN"));

        const bioToggle = q('[data-action="toggle-biometric"]');
        if (bioToggle) bioToggle.addEventListener("click", () => {
          Bridge.setBiometricEnabled(!Store.security.biometricEnabled);
          Store.refreshSecurity();
          renderAll();
        });

        // navigation
        const aiSetupBtn = q('[data-action="open-ai-setup"]');
        if (aiSetupBtn) aiSetupBtn.addEventListener("click", () => Router.push("aiSetup"));

        const fontBtn = q('[data-action="open-font-picker"]');
        if (fontBtn) fontBtn.addEventListener("click", () => Router.push("fontPicker"));

        const themeBtn = q('[data-action="open-theme-picker"]');
        if (themeBtn) themeBtn.addEventListener("click", () => Router.push("themePicker"));

        // data management
        const exportBtn = q('[data-action="export-data"]');
        if (exportBtn) exportBtn.addEventListener("click", () => {
          Bridge.exportData().then((res) => {
            if (res.success) UI.toast("Data exported");
            else if (res.error !== "cancelled") UI.toast(res.error || "Export failed", true);
          });
        });

        const importBtn = q('[data-action="import-data"]');
        if (importBtn) importBtn.addEventListener("click", () => {
          UI.confirmDialog({
            title: "Import data?",
            message: "Thoughts from the selected backup file will be added to your existing entries, and settings will be overwritten.",
            confirmLabel: "Choose File",
            onConfirm: () => {
              Bridge.importData().then((res) => {
                if (res.success) {
                  Store.settings = Bridge.getSettings();
                  Store.applyTheme();
                  UI.toast("Imported " + res.imported + " thought" + (res.imported === 1 ? "" : "s"));
                  renderAll();
                } else if (res.error !== "cancelled") {
                  UI.toast(res.error || "Import failed", true);
                }
              });
            }
          });
        });

        const deleteAllBtn = q('[data-action="delete-all"]');
        if (deleteAllBtn) deleteAllBtn.addEventListener("click", () => {
          UI.confirmDialog({
            title: "Delete all data?",
            message: "This will permanently delete every thought, attachment, and chat message. This cannot be undone.",
            confirmLabel: "Delete Everything",
            danger: true,
            onConfirm: () => {
              Bridge.deleteAllData();
              Bridge.clearChatHistory();
              UI.toast("All data deleted");
              Router.reset("home");
            }
          });
        });

        const aboutBtn = q('[data-action="about"]');
        if (aboutBtn) aboutBtn.addEventListener("click", () => {
          const info = Bridge.getAppInfo();
          const modal = UI.showModal(
            '<div style="text-align:center">' +
            '<div class="lock-screen__logo" style="margin:0 auto 14px">' + Icon("lightbulb") + "</div>" +
            "<h3>Brain Dump</h3>" +
            "<p>Version " + UI.escapeHtml(info.versionName) + " (build " + info.versionCode + ")</p>" +
            "<p>A private, local-first journal with on-device storage and optional AI assistance. Your thoughts never leave your phone unless you use a feature that calls Claude or Gemini.</p>" +
            "</div>" +
            '<div class="modal__actions"><button class="btn btn--primary" data-action="close">Close</button></div>'
          );
          modal.querySelector('[data-action="close"]').addEventListener("click", UI.hideModal);
        });
      }

      renderAll();
      return () => {};
    }
  };

  // ---- Theme picker --------------------------------------------------------------

  Views.themePicker = {
    render(container) {
      function buildHtml() {
        const list = '<div class="theme-list">' + Catalog.THEMES.map((t) => {
          const selected = Store.settings.themeColor === t.key;
          return '<div class="theme-item ' + (selected ? "selected" : "") + '" data-key="' + t.key + '">' +
            '<div class="theme-item__swatch accent-gradient" data-color="' + t.key + '"></div>' +
            '<div class="theme-item__name">' + t.name + "</div>" +
            (selected ? Icon("check", 'class="check-icon"') : "") +
            "</div>";
        }).join("") + "</div>";

        return backTopbar("Theme Color") + '<div class="screen">' + list + "</div>";
      }

      function renderAll() {
        container.innerHTML = buildHtml();
        attach();
      }

      function attach() {
        const backBtn = container.querySelector('[data-action="back"]');
        if (backBtn) backBtn.addEventListener("click", () => Router.pop());

        container.querySelectorAll(".theme-item").forEach((item) => {
          item.addEventListener("click", () => {
            Store.saveSettings({ themeColor: item.dataset.key });
            renderAll();
          });
        });
      }

      renderAll();
      return () => {};
    }
  };

  // ---- Font picker ----------------------------------------------------------------

  Views.fontPicker = {
    render(container) {
      function buildHtml() {
        const list = '<div class="font-list">' + Catalog.FONTS.map((f) => {
          const selected = Store.settings.fontFamily === f.key;
          return '<div class="font-item ' + (selected ? "selected" : "") + '" data-key="' + f.key + '">' +
            '<div class="font-item__preview" style="font-family:' + f.css + '">Aa</div>' +
            '<div class="font-item__name">' + f.name + "</div>" +
            (selected ? Icon("check", 'class="check-icon"') : "") +
            "</div>";
        }).join("") + "</div>";

        return backTopbar("Font") + '<div class="screen">' + list + "</div>";
      }

      function renderAll() {
        container.innerHTML = buildHtml();
        attach();
      }

      function attach() {
        const backBtn = container.querySelector('[data-action="back"]');
        if (backBtn) backBtn.addEventListener("click", () => Router.pop());

        container.querySelectorAll(".font-item").forEach((item) => {
          item.addEventListener("click", () => {
            Store.saveSettings({ fontFamily: item.dataset.key });
            renderAll();
          });
        });
      }

      renderAll();
      return () => {};
    }
  };

  // ---- AI Assistant Setup -----------------------------------------------------------

  Views.aiSetup = {
    render(container) {
      function buildHtml() {
        const s = Store.settings;
        const ai = Store.aiStatus;

        const claudeCard = '<div class="card">' +
          "<h3 style=\"margin:0 0 4px;font-size:0.95rem;display:flex;align-items:center;gap:8px\">" + Icon("bot") + "Claude API Key</h3>" +
          '<p class="settings-note" style="padding:0 0 10px">Used for chat, journal rewriting and AI suggestions.</p>' +
          '<div class="input-with-action">' +
          '<input type="password" class="text-input" id="claude-key" placeholder="sk-ant-…" autocomplete="off">' +
          '<button class="input-with-action__btn" data-action="toggle-claude-visibility">' + Icon("eye") + "</button>" +
          "</div>" +
          '<div class="key-status' + (ai.hasClaudeKey ? " key-status--connected" : "") + '">' +
          Icon(ai.hasClaudeKey ? "shield-check" : "shield") +
          "<span>" + (ai.hasClaudeKey ? "API key saved" : "No API key configured") + "</span></div>" +
          '<div class="modal__actions" style="margin-top:14px">' +
          '<button class="btn btn--secondary" data-action="clear-claude-key"' + (ai.hasClaudeKey ? "" : " disabled") + ">Clear</button>" +
          '<button class="btn btn--primary" data-action="save-claude-key">Save</button>' +
          "</div></div>";

        const geminiCard = '<div class="card">' +
          "<h3 style=\"margin:0 0 4px;font-size:0.95rem;display:flex;align-items:center;gap:8px\">" + Icon("mic") + "Gemini API Key</h3>" +
          '<p class="settings-note" style="padding:0 0 10px">Used only for transcribing voice notes (Claude cannot process raw audio).</p>' +
          '<div class="input-with-action">' +
          '<input type="password" class="text-input" id="gemini-key" placeholder="AIza…" autocomplete="off">' +
          '<button class="input-with-action__btn" data-action="toggle-gemini-visibility">' + Icon("eye") + "</button>" +
          "</div>" +
          '<div class="key-status' + (ai.hasGeminiKey ? " key-status--connected" : "") + '">' +
          Icon(ai.hasGeminiKey ? "shield-check" : "shield") +
          "<span>" + (ai.hasGeminiKey ? "API key saved" : "No API key configured") + "</span></div>" +
          '<div class="modal__actions" style="margin-top:14px">' +
          '<button class="btn btn--secondary" data-action="clear-gemini-key"' + (ai.hasGeminiKey ? "" : " disabled") + ">Clear</button>" +
          '<button class="btn btn--primary" data-action="save-gemini-key">Save</button>' +
          "</div></div>";

        const modelCard = '<div class="card">' +
          "<h3 style=\"margin:0 0 4px;font-size:0.95rem;display:flex;align-items:center;gap:8px\">" + Icon("sliders") + "Model Settings</h3>" +
          '<div class="field-label">Claude Model</div>' +
          '<input type="text" class="text-input" id="ai-model" value="' + UI.escapeHtml(s.aiModel) + '">' +
          '<div class="field-label">Gemini Model (audio transcription)</div>' +
          '<input type="text" class="text-input" id="gemini-model" value="' + UI.escapeHtml(s.geminiModel) + '">' +
          '<div class="field-label">System Prompt</div>' +
          '<textarea class="textarea-field" id="system-prompt" rows="5">' + UI.escapeHtml(s.aiSystemPrompt) + "</textarea>" +
          '<button class="btn btn--primary" style="margin-top:12px" data-action="save-model-settings">Save</button>' +
          "</div>";

        return backTopbar("AI Assistant Setup") + '<div class="screen">' + claudeCard + geminiCard + modelCard + "</div>";
      }

      function renderAll() {
        container.innerHTML = buildHtml();
        attach();
      }

      function attach() {
        const q = (sel) => container.querySelector(sel);

        const backBtn = q('[data-action="back"]');
        if (backBtn) backBtn.addEventListener("click", () => Router.pop());

        function bindVisibilityToggle(toggleAction, inputId) {
          const btn = q('[data-action="' + toggleAction + '"]');
          if (!btn) return;
          btn.addEventListener("click", () => {
            const input = q("#" + inputId);
            const isPassword = input.type === "password";
            input.type = isPassword ? "text" : "password";
            btn.innerHTML = Icon(isPassword ? "eye-off" : "eye");
          });
        }
        bindVisibilityToggle("toggle-claude-visibility", "claude-key");
        bindVisibilityToggle("toggle-gemini-visibility", "gemini-key");

        const saveClaudeBtn = q('[data-action="save-claude-key"]');
        if (saveClaudeBtn) saveClaudeBtn.addEventListener("click", () => {
          const val = q("#claude-key").value.trim();
          if (!val) { UI.toast("Enter an API key first", true); return; }
          Bridge.saveApiKey("claude", val);
          Store.refreshAiStatus();
          UI.toast("Claude API key saved");
          renderAll();
        });

        const clearClaudeBtn = q('[data-action="clear-claude-key"]');
        if (clearClaudeBtn) clearClaudeBtn.addEventListener("click", () => {
          Bridge.clearApiKey("claude");
          Store.refreshAiStatus();
          UI.toast("Claude API key removed");
          renderAll();
        });

        const saveGeminiBtn = q('[data-action="save-gemini-key"]');
        if (saveGeminiBtn) saveGeminiBtn.addEventListener("click", () => {
          const val = q("#gemini-key").value.trim();
          if (!val) { UI.toast("Enter an API key first", true); return; }
          Bridge.saveApiKey("gemini", val);
          Store.refreshAiStatus();
          UI.toast("Gemini API key saved");
          renderAll();
        });

        const clearGeminiBtn = q('[data-action="clear-gemini-key"]');
        if (clearGeminiBtn) clearGeminiBtn.addEventListener("click", () => {
          Bridge.clearApiKey("gemini");
          Store.refreshAiStatus();
          UI.toast("Gemini API key removed");
          renderAll();
        });

        const saveModelBtn = q('[data-action="save-model-settings"]');
        if (saveModelBtn) saveModelBtn.addEventListener("click", () => {
          const aiModel = q("#ai-model").value.trim() || "claude-sonnet-4-6";
          const geminiModel = q("#gemini-model").value.trim() || "gemini-2.0-flash";
          const aiSystemPrompt = q("#system-prompt").value;
          Store.saveSettings({ aiModel, geminiModel, aiSystemPrompt });
          UI.toast("AI settings saved");
        });
      }

      renderAll();
      return () => {};
    }
  };
})();
