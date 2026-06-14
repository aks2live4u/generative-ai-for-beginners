/* ==========================================================================
   Brain Dump — Bridge
   Thin Promise-based wrapper around the native AndroidBridge interface.
   Synchronous bridge methods return JSON strings; async methods take a
   requestId and resolve later via window.__bridgeResolve.
   ========================================================================== */

(function () {
  const pending = new Map();
  let counter = 0;

  window.__bridgeResolve = function (requestId, result) {
    const entry = pending.get(requestId);
    if (!entry) return;
    pending.delete(requestId);
    entry(result);
  };

  function nextRequestId() {
    counter += 1;
    return "req_" + Date.now() + "_" + counter;
  }

  function callAsync(method, ...args) {
    return new Promise((resolve) => {
      const requestId = nextRequestId();
      pending.set(requestId, resolve);
      window.AndroidBridge[method](requestId, ...args);
    });
  }

  function callJson(method, ...args) {
    return JSON.parse(window.AndroidBridge[method](...args));
  }

  window.Bridge = {
    // ---- Thoughts ------------------------------------------------------
    getThoughts(query = {}) {
      return callJson("getThoughts", JSON.stringify(query));
    },
    getThought(id) {
      return callJson("getThought", id);
    },
    addThought(thought) {
      return callJson("addThought", JSON.stringify(thought));
    },
    updateThought(id, thought) {
      return callJson("updateThought", id, JSON.stringify(thought));
    },
    deleteThought(id) {
      return callJson("deleteThought", id);
    },
    getStats() {
      return callJson("getStats");
    },
    getMoodStats() {
      return callJson("getMoodStats");
    },

    // ---- Settings -------------------------------------------------------
    getSettings() {
      return callJson("getSettings");
    },
    saveSettings(update) {
      return callJson("saveSettings", JSON.stringify(update));
    },

    // ---- Security ---------------------------------------------------------
    getSecurityStatus() {
      return callJson("getSecurityStatus");
    },
    setPin(pin) {
      return callJson("setPin", pin);
    },
    verifyPin(pin) {
      return callJson("verifyPin", pin);
    },
    removePin() {
      return callJson("removePin");
    },
    setBiometricEnabled(enabled) {
      return callJson("setBiometricEnabled", enabled);
    },

    // ---- AI configuration -------------------------------------------------
    getAiStatus() {
      return callJson("getAiStatus");
    },
    saveApiKey(provider, key) {
      return callJson("saveApiKey", provider, key);
    },
    clearApiKey(provider) {
      return callJson("clearApiKey", provider);
    },

    // ---- AI chat history ----------------------------------------------------
    getChatMessages() {
      return callJson("getChatMessages");
    },
    addChatMessage(role, content) {
      return callJson("addChatMessage", role, content);
    },
    clearChatHistory() {
      return callJson("clearChatHistory");
    },

    // ---- Misc -------------------------------------------------------------
    getQuote(refresh = false) {
      return callJson("getQuote", refresh);
    },
    fetchOnlineQuote() {
      return callAsync("fetchOnlineQuote");
    },
    getAppInfo() {
      return callJson("getAppInfo");
    },
    deleteAllData() {
      return callJson("deleteAllData");
    },
    removeImage(name) {
      return callJson("removeImage", name);
    },
    getMediaBaseUrl() {
      return window.AndroidBridge.getMediaBaseUrl();
    },
    cancelRecording() {
      return callJson("cancelRecording");
    },

    // ---- Async: attachments -------------------------------------------------
    pickImage(source) {
      return callAsync("pickImage", source);
    },
    startRecording() {
      return callAsync("startRecording");
    },
    stopRecording() {
      return callAsync("stopRecording");
    },

    // ---- Async: AI ----------------------------------------------------------
    aiChat(payload) {
      return callAsync("aiChat", JSON.stringify(payload));
    },
    aiRewrite(payload) {
      return callAsync("aiRewrite", JSON.stringify(payload));
    },
    transcribeAudio(audioName) {
      return callAsync("transcribeAudio", audioName);
    },

    // ---- Async: security & data ----------------------------------------------
    authenticate() {
      return callAsync("authenticate");
    },
    exportData() {
      return callAsync("exportData");
    },
    importData() {
      return callAsync("importData");
    }
  };
})();
