/* ==========================================================================
   Brain Dump — Lock screen (PIN + biometric)
   ========================================================================== */

(function () {
  const PIN_LENGTH = 4;

  window.Views = window.Views || {};

  Views.lock = {
    render(container, { onUnlock }) {
      let entered = "";
      let error = false;
      let checking = false;

      function html() {
        const bio = Store.security && Store.security.biometricEnabled;
        let dots = "";
        for (let i = 0; i < PIN_LENGTH; i++) {
          dots += '<div class="pin-dot ' + (i < entered.length ? "filled" : "") + (error ? " error" : "") + '"></div>';
        }
        const keys = [1, 2, 3, 4, 5, 6, 7, 8, 9];
        let keypad = keys.map((n) => '<button class="pin-key" data-key="' + n + '">' + n + "</button>").join("");
        keypad += '<div class="pin-key pin-key--ghost"></div>';
        keypad += '<button class="pin-key" data-key="0">0</button>';
        keypad += '<button class="pin-key" data-key="back">' + Icon("arrow-left") + "</button>";

        return (
          '<div class="lock-screen">' +
          '<div class="lock-screen__logo">' + Icon("lightbulb") + "</div>" +
          '<div class="lock-screen__title">Brain Dump</div>' +
          '<div class="lock-screen__subtitle">Enter your PIN to unlock</div>' +
          '<div class="pin-dots">' + dots + "</div>" +
          '<div class="pin-keypad">' + keypad + "</div>" +
          (bio ? '<button class="lock-screen__bio" data-action="bio">' + Icon("fingerprint") + " Use fingerprint</button>" : "") +
          '<div class="lock-screen__error">' + (error ? "Incorrect PIN" : "") + "</div>" +
          "</div>"
        );
      }

      function rerender() {
        container.innerHTML = html();
        attach();
      }

      function attach() {
        container.querySelectorAll("[data-key]").forEach((btn) => {
          btn.addEventListener("click", () => {
            if (checking) return;
            const key = btn.dataset.key;
            if (key === "back") {
              entered = entered.slice(0, -1);
              error = false;
              rerender();
              return;
            }
            if (entered.length >= PIN_LENGTH) return;
            entered += key;
            error = false;
            if (entered.length === PIN_LENGTH) {
              checking = true;
              rerender();
              setTimeout(checkPin, 80);
            } else {
              rerender();
            }
          });
        });
        const bioBtn = container.querySelector('[data-action="bio"]');
        if (bioBtn) bioBtn.addEventListener("click", tryBiometric);
      }

      function checkPin() {
        const result = Bridge.verifyPin(entered);
        checking = false;
        if (result.valid) {
          onUnlock();
        } else {
          error = true;
          entered = "";
          rerender();
          setTimeout(() => { error = false; rerender(); }, 900);
        }
      }

      function tryBiometric() {
        Bridge.authenticate().then((res) => {
          if (res.success) onUnlock();
        });
      }

      rerender();
      if (Store.security && Store.security.biometricEnabled) {
        setTimeout(tryBiometric, 350);
      }
    }
  };
})();
