/* ==========================================================================
   Brain Dump — Chat with AI screen
   ========================================================================== */

(function () {
  window.Views = window.Views || {};

  Views.chat = {
    render(container) {
      let messages = Bridge.getChatMessages();
      let busy = false;
      let includeContext = true;
      let draftText = "";

      function renderBubble(m) {
        const cls = m.role === "user" ? "chat-bubble--user" : "chat-bubble--assistant";
        return '<div class="chat-bubble ' + cls + '">' + UI.escapeHtml(m.content) + "</div>";
      }

      function buildHtml() {
        const topbar = '<div class="topbar">' +
          '<button class="icon-btn" data-action="back">' + Icon("arrow-left") + "</button>" +
          '<div class="topbar-back-title">Chat with AI</div>' +
          '<button class="icon-btn" data-action="clear">' + Icon("trash") + "</button>" +
          "</div>";

        let body;
        if (messages.length === 0 && !busy) {
          body = '<div class="empty-state">' + Icon("chat") +
            "<h2>Chat with your AI assistant</h2>" +
            "<p>Ask questions, get suggestions, or talk through what's on your mind.</p>" +
            "</div>";
        } else {
          body = '<div class="chat-messages">' + messages.map(renderBubble).join("") +
            (busy ? '<div class="chat-typing"><span></span><span></span><span></span></div>' : "") +
            "</div>";
        }

        const contextToggle = '<div class="chat-context-toggle">' +
          '<button class="switch ' + (includeContext ? "on" : "") + '" data-action="toggle-context"></button>' +
          "<span>Include recent journal entries as context</span>" +
          "</div>";

        const screen = '<div class="screen">' + contextToggle + body + "</div>";

        const canSend = draftText.trim().length > 0 && !busy;
        const inputBar = '<div class="input-bar-wrap"><div class="input-bar">' +
          '<textarea class="input-bar__field" id="chat-text" placeholder="Message your AI assistant…" rows="1">' + UI.escapeHtml(draftText) + "</textarea>" +
          '<button class="input-bar__send ' + (canSend ? "active" : "") + '" data-action="send">' + Icon("send") + "</button>" +
          "</div></div>";

        return topbar + screen + inputBar;
      }

      function scrollToBottom() {
        const el = container.querySelector(".screen");
        if (el) el.scrollTop = el.scrollHeight;
      }

      function renderAll(focusInput) {
        container.innerHTML = buildHtml();
        attach();
        scrollToBottom();
        if (focusInput) {
          const ta = container.querySelector("#chat-text");
          if (ta) ta.focus();
        }
      }

      function sendMessage() {
        const text = draftText.trim();
        if (!text || busy) return;
        if (!Store.aiStatus.hasClaudeKey) { UI.toast("Add a Claude API key in AI Assistant Setup", true); return; }

        messages = messages.concat(Bridge.addChatMessage("user", text));
        draftText = "";
        busy = true;
        renderAll();

        const payload = {
          messages: messages.map((m) => ({ role: m.role, content: m.content })),
          includeJournalContext: includeContext
        };

        Bridge.aiChat(payload).then((res) => {
          busy = false;
          if (res.success) {
            messages = messages.concat(Bridge.addChatMessage("assistant", res.text));
          } else {
            UI.toast(res.error || "AI request failed", true);
          }
          renderAll(true);
        });
      }

      function attach() {
        const q = (sel) => container.querySelector(sel);

        const backBtn = q('[data-action="back"]');
        if (backBtn) backBtn.addEventListener("click", () => Router.pop());

        const clearBtn = q('[data-action="clear"]');
        if (clearBtn) clearBtn.addEventListener("click", () => {
          if (messages.length === 0) return;
          UI.confirmDialog({
            title: "Clear chat history?",
            message: "This will permanently delete this conversation.",
            confirmLabel: "Clear",
            danger: true,
            onConfirm: () => {
              Bridge.clearChatHistory();
              messages = [];
              renderAll();
            }
          });
        });

        const contextToggle = q('[data-action="toggle-context"]');
        if (contextToggle) contextToggle.addEventListener("click", () => {
          includeContext = !includeContext;
          contextToggle.classList.toggle("on", includeContext);
        });

        const textarea = q("#chat-text");
        if (textarea) {
          textarea.addEventListener("input", (e) => {
            draftText = e.target.value;
            const sendBtn = q('[data-action="send"]');
            if (sendBtn) sendBtn.classList.toggle("active", draftText.trim().length > 0 && !busy);
          });
        }

        const sendBtn = q('[data-action="send"]');
        if (sendBtn) sendBtn.addEventListener("click", sendMessage);
      }

      renderAll();

      return () => {};
    }
  };
})();
