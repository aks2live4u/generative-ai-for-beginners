/* FocusFlow AI — router, event wiring, AI orchestration, focus timer. */

const FF = {
  inboxFilter: 'all',
  projectFilter: 'all',
  listening: false,
  sending: false,
  pendingSpeechTarget: null, // 'dump' | 'capture'
  timer: null, // { taskId, remaining, totalSeconds, running, continuousSeconds, sessionId, hyperfocusStage }
  timerInterval: null,
  sheetTaskId: null,

  init() {
    Store.load();
    Store.touchStreak();
    this.bindGlobalEvents();
    window.addEventListener('hashchange', () => this.render());
    this.render();
    this.maybeFetchCoachTip();
  },

  route() {
    const hash = (location.hash || '#home').slice(1);
    const [name, param] = hash.split('/');
    return { name: name || 'home', param };
  },

  navigate(hash) {
    if (location.hash === '#' + hash) { this.render(); return; }
    location.hash = hash;
  },

  toast(message, type) {
    const stack = document.getElementById('toast-stack');
    const el = document.createElement('div');
    el.className = 'toast' + (type ? ' ' + type : '');
    el.textContent = message;
    stack.appendChild(el);
    setTimeout(() => el.remove(), 3400);
  },

  haptic(kind) {
    try {
      if (window.AndroidBridge) {
        if (kind === 'success') window.AndroidBridge.hapticSuccess();
        else if (kind === 'medium') window.AndroidBridge.hapticMedium();
        else window.AndroidBridge.hapticLight();
      }
    } catch (_) {}
  },

  requireApiKey() {
    if (Store.hasApiKey()) return true;
    this.toast('Add your Gemini API key in More → Gemini API to use AI features.', 'error');
    this.navigate('more');
    return false;
  },

  // ---------------- rendering ----------------

  clearFocusInterval() {
    if (this.timerInterval) { clearInterval(this.timerInterval); this.timerInterval = null; }
  },

  render() {
    this.clearFocusInterval();
    const { name, param } = this.route();
    const root = document.getElementById('screen-root');
    const state = Store.state;
    let html = '';

    switch (name) {
      case 'home':
        html = UI.renderHome({ state });
        break;
      case 'inbox':
        html = UI.renderInbox({ state, inboxFilter: this.inboxFilter });
        break;
      case 'braindump':
        html = UI.renderBrainDump({ state, listening: this.listening });
        break;
      case 'projects':
        html = UI.renderProjects({ state, projectFilter: this.projectFilter });
        break;
      case 'project':
        html = UI.renderProjectDetail({ state, projectId: param });
        break;
      case 'focus': {
        const taskId = param || state.today.missionTaskId;
        const focusTask = taskId ? Store.getTask(taskId) : null;
        if (focusTask && (!this.timer || this.timer.taskId !== taskId)) {
          this.beginFocusTimer(focusTask);
        }
        html = UI.renderFocus({ state, focusTask, timer: this.timer || { remaining: 0, totalSeconds: 1, running: false } });
        break;
      }
      case 'chat':
        html = UI.renderChat({ state, sending: this.sending });
        break;
      case 'more':
        html = UI.renderMore({ state });
        break;
      default:
        html = UI.renderHome({ state });
    }

    root.innerHTML = html;
    const unprocessed = state.inbox.filter(i => !i.processed).length;
    document.getElementById('bottom-nav').innerHTML = UI.renderBottomNav(name, unprocessed);
    root.scrollTop = 0;

    if (name === 'chat') {
      const scroller = document.getElementById('chat-scroll');
      if (scroller) scroller.scrollTop = scroller.scrollHeight;
    }
    if (name === 'focus' && this.timer && this.timer.running) {
      this.startFocusInterval();
    }
  },

  // ---------------- global click delegation ----------------

  bindGlobalEvents() {
    document.addEventListener('click', (e) => {
      const navEl = e.target.closest('[data-nav]');
      if (navEl) {
        const key = navEl.dataset.nav;
        this.navigate(key === 'home' ? 'home' : key);
        return;
      }
      const actEl = e.target.closest('[data-action]');
      if (actEl) {
        this.handleAction(actEl, e);
      }
    });

    document.addEventListener('keydown', (e) => {
      if (e.key !== 'Enter') return;
      if (e.target.id === 'chat-input') this.chatSend();
      if (e.target.id === 'quick-capture-input') this.submitCapture();
      if (e.target.id === 'waiting-input') this.addWaiting();
    });
  },

  async handleAction(el, e) {
    const action = el.dataset.action;
    const id = el.dataset.id;
    try {
      switch (action) {
        case 'quick-add': this.openSheet(UI.renderQuickAddSheet()); break;
        case 'quick-nav': this.quickNav(el.dataset.target); break;
        case 'plan-day': await this.runLoading(el, 'Planning…', () => this.planDay()); break;
        case 'start-working': this.startWorking(); break;
        case 'cycle-energy': this.cycleEnergy(); break;

        case 'mic-capture': this.startMic('capture'); break;
        case 'submit-capture': this.submitCapture(); break;
        case 'inbox-filter': this.inboxFilter = el.dataset.filter; this.render(); break;
        case 'toggle-inbox': this.toggleInboxOpen(id); break;
        case 'triage-suggest': await this.runLoading(el, 'Thinking…', () => this.triageSuggest(id)); break;
        case 'inbox-do': await this.runLoading(el, 'Working…', () => this.inboxDo(id, el.dataset.do)); break;

        case 'dump-mic': this.startMic('dump'); break;
        case 'dump-process': await this.runLoading(el, 'Building your plan…', () => this.dumpProcess()); break;

        case 'new-project': this.openSheet(UI.renderNewProjectSheet()); break;
        case 'create-project': this.createProjectFromSheet(); break;
        case 'project-filter': this.projectFilter = el.dataset.filter; this.render(); break;
        case 'open-project': this.navigate('project/' + id); break;
        case 'delete-project': this.confirmSheet('Delete project?', 'This removes the project and all its tasks.', () => { Store.removeProject(id); this.navigate('projects'); }); break;
        case 'add-project-task': this.addProjectTask(id); break;
        case 'toggle-task': this.toggleTask(id); break;
        case 'open-task': this.openTaskSheet(id); break;
        case 'toggle-subtask': Store.toggleSubtask(el.dataset.task, el.dataset.sub); this.refreshSheet(el.dataset.task); this.render(); break;
        case 'breakdown-task': await this.runLoading(el, 'Breaking it down…', () => this.breakdownTask(id)); break;
        case 'focus-this-task': this.closeSheet(); this.navigate('focus/' + id); break;
        case 'waiting-task': this.openWaitingSheet(id); break;
        case 'confirm-waiting': this.confirmWaitingTask(); break;
        case 'complete-task': Store.completeTask(id); this.haptic('success'); this.toast('+15 XP 🎉', 'success'); this.closeSheet(); this.render(); break;
        case 'delete-task': this.confirmSheet('Delete task?', 'This can\'t be undone.', () => { Store.removeTask(id); this.closeSheet(); this.render(); }); break;

        case 'focus-done': this.focusDone(); break;
        case 'focus-help': this.openSheet(UI.renderReasonSheet()); break;
        case 'pick-reason': await this.runReasonLoading(el.dataset.reason); break;
        case 'focus-skip': this.focusSkip(); break;
        case 'focus-pause': this.focusPause(); break;
        case 'focus-distracted': this.focusDistracted(); break;

        case 'chat-send': this.chatSend(); break;
        case 'chat-quick': this.chatSend(el.dataset.msg); break;

        case 'weekly-review': await this.runLoading(el, 'Reviewing your week…', () => this.weeklyReview()); break;
        case 'set-energy': Store.updateSettings({ energyLevel: el.dataset.energy }); this.render(); break;
        case 'add-waiting': this.addWaiting(); break;
        case 'resolve-waiting': Store.resolveWaiting(id); this.render(); break;
        case 'save-api-key': this.saveApiKey(); break;
        case 'save-name': this.saveName(); break;
        case 'reset-data': this.confirmSheet('Reset all data?', 'This deletes every project, task, and setting on this device.', () => { Store.reset(); this.closeSheet(); this.navigate('home'); }); break;

        case 'close-sheet': this.closeSheet(); break;
        case 'confirm-yes': { const fn = this._confirmFn; this.closeSheet(); if (fn) fn(); break; }
      }
    } catch (err) {
      console.error(err);
      if (err instanceof AI.MissingApiKeyError) {
        this.requireApiKey();
      } else {
        this.toast(String(err.message || err).slice(0, 140), 'error');
      }
    }
  },

  async runLoading(btn, label, fn) {
    const orig = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner"></span> ${label}`;
    try {
      await fn();
    } finally {
      if (document.body.contains(btn)) { btn.disabled = false; btn.innerHTML = orig; }
    }
  },

  // ---------------- sheets ----------------

  openSheet(innerHtml) {
    const root = document.getElementById('modal-root');
    root.innerHTML = `<div class="overlay" id="sheet-overlay"><div class="sheet">${innerHtml}</div></div>`;
    document.getElementById('sheet-overlay').addEventListener('click', (e) => {
      if (e.target.id === 'sheet-overlay') this.closeSheet();
    });
  },

  closeSheet() {
    document.getElementById('modal-root').innerHTML = '';
    this.sheetTaskId = null;
  },

  confirmSheet(title, body, onConfirm) {
    this._confirmFn = onConfirm;
    this.openSheet(`
      <div class="sheet-handle"></div>
      <h2>${UI.esc(title)}</h2>
      <div class="sub">${UI.esc(body)}</div>
      <div class="btn-row mt-14">
        <button class="btn btn-secondary" data-action="close-sheet">Cancel</button>
        <button class="btn btn-danger" data-action="confirm-yes">Confirm</button>
      </div>
    `);
  },

  quickNav(target) {
    this.closeSheet();
    if (target === 'new-project') { this.navigate('projects'); setTimeout(() => this.openSheet(UI.renderNewProjectSheet()), 0); return; }
    this.navigate(target);
  },

  // ---------------- home ----------------

  async planDay() {
    if (!this.requireApiKey()) return;
    const tasks = Store.openTasks();
    if (!tasks.length) { this.toast('Add some tasks or try an AI Brain Dump first.', 'error'); return; }
    const result = await AI.prioritizeTasks(tasks, {
      energyLevel: Store.state.settings.energyLevel,
      minutesLeftToday: UI.minutesUntilWorkEnd()
    });
    (result.ranked || []).forEach(r => { if (Store.getTask(r.id)) Store.updateTask(r.id, { bucket: r.bucket }); });
    Store.setToday({
      missionTaskId: result.missionTaskId,
      secondaryTaskIds: result.secondaryTaskIds || [],
      optionalTaskIds: result.optionalTaskIds || []
    });
    this.toast('Today\'s mission is set. 🎯', 'success');
    this.render();
  },

  startWorking() {
    const state = Store.state;
    const taskId = state.today.missionTaskId;
    if (!taskId || !Store.getTask(taskId)) { this.toast('No mission set yet — tap "Plan My Day" first.'); return; }
    const task = Store.getTask(taskId);
    this.beginFocusTimer(task);
    this.navigate('focus/' + taskId);
  },

  cycleEnergy() {
    const order = ['Low', 'Medium', 'High'];
    const cur = Store.state.settings.energyLevel;
    const next = order[(order.indexOf(cur) + 1) % order.length];
    Store.updateSettings({ energyLevel: next });
    this.render();
  },

  async maybeFetchCoachTip() {
    if (!Store.hasApiKey()) return;
    const today = new Date().toISOString().slice(0, 10);
    if (Store.state.coach.lastTipDate === today) return;
    try {
      const focusedToday = Store.minutesFocusedToday();
      const openCount = Store.openTasks().length;
      const context = `Energy: ${Store.state.settings.energyLevel}. Open tasks: ${openCount}. Minutes focused today: ${focusedToday}. Streak: ${Store.state.streak.count} days.`;
      const tip = await AI.coachTip(context);
      Store.state.coach.lastTip = tip.trim();
      Store.state.coach.lastTipDate = today;
      Store.save();
      const el = document.getElementById('coach-tip-text');
      if (el) el.textContent = Store.state.coach.lastTip;
    } catch (_) { /* silent — coach tip is a nice-to-have */ }
  },

  // ---------------- speech ----------------

  startMic(target) {
    this.pendingSpeechTarget = target;
    if (window.AndroidBridge && window.AndroidBridge.startListening) {
      this.listening = true;
      if (target === 'dump') this.render();
      window.AndroidBridge.startListening();
      return;
    }
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SR) { this.toast('Voice capture needs the Android app.', 'error'); return; }
    const rec = new SR();
    rec.lang = 'en-US';
    this.listening = true;
    if (target === 'dump') this.render();
    rec.onresult = (ev) => this.onSpeechResult(ev.results[0][0].transcript);
    rec.onerror = () => this.onSpeechResult('');
    rec.onend = () => { this.listening = false; };
    rec.start();
  },

  onSpeechResult(text) {
    this.listening = false;
    const target = this.pendingSpeechTarget;
    if (target === 'dump') {
      this.render();
      const ta = document.getElementById('dump-textarea');
      if (ta && text) ta.value = (ta.value ? ta.value + ' ' : '') + text;
    } else if (target === 'capture') {
      const input = document.getElementById('quick-capture-input');
      if (input && text) input.value = text;
    }
  },

  // ---------------- inbox ----------------

  submitCapture() {
    const input = document.getElementById('quick-capture-input');
    const text = (input?.value || '').trim();
    if (!text) return;
    Store.addInboxItem({ type: 'note', title: text.slice(0, 60), content: text });
    if (input) input.value = '';
    this.render();
  },

  toggleInboxOpen(id) {
    const item = Store.state.inbox.find(i => i.id === id);
    if (!item) return;
    Store.state.inbox.forEach(i => { if (i.id !== id) i._open = false; });
    item._open = !item._open;
    Store.save();
    this.render();
  },

  async triageSuggest(id) {
    if (!this.requireApiKey()) return;
    const item = Store.state.inbox.find(i => i.id === id);
    if (!item) return;
    const suggestion = await AI.triageInboxItem(item);
    Store.updateInboxItem(id, { aiSuggestion: suggestion });
    this.render();
  },

  async inboxDo(id, doAction) {
    const item = Store.state.inbox.find(i => i.id === id);
    if (!item) return;
    const title = item.aiSuggestion?.cleanTitle || item.title || item.content.slice(0, 60);

    if (doAction === 'create_project') {
      const p = Store.addProject({ name: title, icon: '📁' });
      Store.updateInboxItem(id, { processed: true, resultProjectId: p.id });
      this.toast('Project created.', 'success');
      this.navigate('project/' + p.id);
      return;
    }
    if (doAction === 'create_task') {
      Store.addTask({ title, bucket: 'today' });
      Store.updateInboxItem(id, { processed: true });
      this.toast('Task added to Today.', 'success');
    } else if (doAction === 'reminder') {
      Store.addTask({ title: '🔔 ' + title, bucket: 'today', notes: 'Reminder' });
      Store.updateInboxItem(id, { processed: true });
      this.toast('Reminder set for today.', 'success');
    } else if (doAction === 'research_later') {
      Store.addTask({ title, bucket: 'later', category: 'Research' });
      Store.updateInboxItem(id, { processed: true });
      this.toast('Queued for later research.', 'success');
    } else if (doAction === 'archive') {
      Store.updateInboxItem(id, { processed: true });
      this.toast('Archived.');
    } else if (doAction === 'delegate') {
      Store.addWaiting({ text: title, who: 'Delegated' });
      Store.updateInboxItem(id, { processed: true });
      this.toast('Added to Waiting List.', 'success');
    }
    this.render();
  },

  // ---------------- brain dump ----------------

  async dumpProcess() {
    if (!this.requireApiKey()) return;
    const ta = document.getElementById('dump-textarea');
    const text = (ta?.value || '').trim();
    if (!text) { this.toast('Say or type what\'s on your mind first.'); return; }

    const result = await AI.parseBrainDump(text);
    const project = Store.addProject({ name: result.projectName || 'New Project', icon: result.projectIcon || '📁' });
    (result.tasks || []).forEach(t => {
      Store.addTask({
        title: t.title,
        projectId: project.id,
        category: t.category || 'Tasks',
        bucket: t.bucket || 'later',
        estimateMin: t.estimateMin || 25,
        notes: t.dueHint || (t.isReminder ? 'Reminder' : '')
      });
    });
    Store.addInboxItem({ type: 'braindump', title: result.projectName, content: text, processed: true, resultProjectId: project.id });
    if (ta) ta.value = '';
    this.toast(`Created "${result.projectName}" with ${result.tasks?.length || 0} tasks.`, 'success');
    this.navigate('project/' + project.id);
  },

  // ---------------- projects / tasks ----------------

  createProjectFromSheet() {
    const input = document.getElementById('proj-name-input');
    const name = (input?.value || '').trim();
    if (!name) return;
    const p = Store.addProject({ name });
    this.closeSheet();
    this.navigate('project/' + p.id);
  },

  addProjectTask(projectId) {
    const input = document.getElementById('new-task-input');
    const title = (input?.value || '').trim();
    if (!title) return;
    Store.addTask({ title, projectId, bucket: 'later' });
    if (input) input.value = '';
    this.render();
  },

  toggleTask(id) {
    const t = Store.getTask(id);
    if (!t) return;
    if (t.done) {
      Store.updateTask(id, { done: false, completedAt: null });
    } else {
      Store.completeTask(id);
      this.haptic('success');
      this.toast('+15 XP 🎉', 'success');
    }
    this.render();
  },

  openTaskSheet(id) {
    const t = Store.getTask(id);
    if (!t) return;
    this.sheetTaskId = id;
    this.openSheet(UI.renderTaskSheet(t));
  },

  refreshSheet(taskId) {
    if (this.sheetTaskId !== taskId) return;
    const t = Store.getTask(taskId);
    const sheet = document.querySelector('#modal-root .sheet');
    if (sheet && t) sheet.innerHTML = UI.renderTaskSheet(t);
  },

  async breakdownTask(id) {
    if (!this.requireApiKey()) return;
    const t = Store.getTask(id);
    if (!t) return;
    const result = await AI.breakdownTask(t);
    const subtasks = (result.steps || []).map(s => ({ id: FF_UID('sub'), title: s.title, done: false, estimateMin: s.estimateMin || 5 }));
    Store.updateTask(id, { subtasks });
    this.sheetTaskId = id;
    const sheet = document.querySelector('#modal-root .sheet');
    if (sheet) sheet.innerHTML = UI.renderTaskSheet(Store.getTask(id));
    this.render();
  },

  openWaitingSheet(taskId) {
    this._waitingTaskId = taskId;
    this.openSheet(`
      <div class="sheet-handle"></div>
      <h2>⏳ Mark Waiting On</h2>
      <div class="sub">Who or what are you waiting for?</div>
      <input class="ff-input" id="waiting-who-input" placeholder="e.g. Vendor, Manager, Amazon refund" />
      <button class="btn btn-primary mt-14" data-action="confirm-waiting">Save</button>
    `);
  },

  confirmWaitingTask() {
    const input = document.getElementById('waiting-who-input');
    const who = (input?.value || '').trim();
    if (!who || !this._waitingTaskId) { this.closeSheet(); return; }
    Store.updateTask(this._waitingTaskId, { waitingOn: who });
    Store.addWaiting({ text: Store.getTask(this._waitingTaskId).title, who });
    this.closeSheet();
    this.render();
  },

  // ---------------- focus mode ----------------

  beginFocusTimer(task) {
    const seconds = Math.max(60, (task.estimateMin || 25) * 60);
    const session = Store.startFocusSession(task.id);
    this.timer = {
      taskId: task.id,
      sessionId: session.id,
      totalSeconds: seconds,
      remaining: seconds,
      running: true,
      continuousSeconds: 0,
      hyperfocusStage: 0
    };
  },

  startFocusInterval() {
    this.clearFocusInterval();
    this.timerInterval = setInterval(() => this.tickFocus(), 1000);
  },

  tickFocus() {
    if (!this.timer || !this.timer.running) return;
    this.timer.remaining = Math.max(0, this.timer.remaining - 1);
    this.timer.continuousSeconds += 1;
    this.updateFocusDom();

    if (this.timer.remaining === 0) {
      this.timer.running = false;
      this.clearFocusInterval();
      this.haptic('medium');
      this.toast('Time\'s up! Wrap up or hit Done. ⏰', 'success');
      this.render();
      return;
    }
    if (this.timer.continuousSeconds > 0 && this.timer.continuousSeconds % 7200 === 0) {
      this.timer.hyperfocusStage += 1;
      this.openSheet(UI.renderBreakReminderSheet(Math.round(this.timer.continuousSeconds / 60)));
    }
  },

  updateFocusDom() {
    const digits = document.querySelector('.timer-digits');
    const wrap = document.querySelector('.timer-wrap');
    if (digits) digits.textContent = UI.mmss(this.timer.remaining);
    if (wrap) {
      const pct = this.timer.totalSeconds > 0 ? 1 - (this.timer.remaining / this.timer.totalSeconds) : 0;
      wrap.setAttribute('style', UI.timerRingStyle(pct));
    }
  },

  endCurrentSession(outcome) {
    if (this.timer) {
      Store.endFocusSession(this.timer.sessionId, outcome);
    }
  },

  focusDone() {
    if (!this.timer) return;
    Store.completeTask(this.timer.taskId);
    this.endCurrentSession('done');
    this.clearFocusInterval();
    this.timer = null;
    this.haptic('success');
    this.toast('Nice work! +15 XP 🎉', 'success');
    this.navigate('home');
  },

  focusSkip() {
    if (!this.timer) return;
    const skippedId = this.timer.taskId;
    Store.updateTask(skippedId, { bucket: 'later' });
    this.endCurrentSession('skipped');
    this.clearFocusInterval();
    this.timer = null;
    const state = Store.state;
    const next = (state.today.secondaryTaskIds || []).map(x => Store.getTask(x)).find(t => t && !t.done && t.id !== skippedId);
    if (next) {
      this.beginFocusTimer(next);
      this.navigate('focus/' + next.id);
    } else {
      this.navigate('home');
    }
  },

  focusPause() {
    if (!this.timer) return;
    this.timer.running = !this.timer.running;
    if (this.timer.running) this.startFocusInterval(); else this.clearFocusInterval();
    this.render();
  },

  focusDistracted() {
    if (!this.timer) return;
    this.timer.running = false;
    this.clearFocusInterval();
    this.toast('Take a breath. Tap Resume when you\'re ready.', 'error');
    this.render();
  },

  async runReasonLoading(reasonKey) {
    if (!this.requireApiKey()) return;
    const task = this.timer ? Store.getTask(this.timer.taskId) : null;
    if (!task) { this.closeSheet(); return; }
    const label = (UI.REASONS.find(r => r[0] === reasonKey) || [, , reasonKey])[2];
    this.openSheet(`<div class="sheet-handle"></div><div class="empty-state"><span class="spinner"></span><div class="mt-8">Thinking of the smallest next step…</div></div>`);
    try {
      const result = await AI.stuckRescue(task, label);
      this.openSheet(UI.renderRescueResult(result));
    } catch (err) {
      this.closeSheet();
      throw err;
    }
  },

  // ---------------- chat ----------------

  async chatSend(prefill) {
    if (!this.requireApiKey()) return;
    const input = document.getElementById('chat-input');
    const text = prefill || (input?.value || '').trim();
    if (!text) return;
    if (input) input.value = '';
    const historyBefore = Store.state.chat.slice();
    Store.addChatMessage('user', text);
    this.sending = true;
    this.render();
    try {
      const state = Store.state;
      const mission = state.today.missionTaskId ? Store.getTask(state.today.missionTaskId) : null;
      const context = [
        `User name: ${state.settings.userName}`,
        `Energy level: ${state.settings.energyLevel}`,
        `Today's mission: ${mission ? mission.title : 'none set'}`,
        `Open tasks: ${Store.openTasks().length}`,
        `Minutes focused today: ${Store.minutesFocusedToday()}`,
        `Minutes left in workday: ${UI.minutesUntilWorkEnd()}`
      ].join('\n');
      const reply = await AI.chatReply(historyBefore, text, context);
      Store.addChatMessage('assistant', reply.trim());
    } catch (err) {
      Store.addChatMessage('assistant', 'Sorry, I hit an error reaching Gemini. Mind trying again?');
      throw err;
    } finally {
      this.sending = false;
      this.render();
    }
  },

  // ---------------- more / settings ----------------

  addWaiting() {
    const input = document.getElementById('waiting-input');
    const text = (input?.value || '').trim();
    if (!text) return;
    Store.addWaiting({ text });
    if (input) input.value = '';
    this.render();
  },

  saveApiKey() {
    const input = document.getElementById('api-key-input');
    Store.updateSettings({ geminiApiKey: (input?.value || '').trim() });
    this.toast('Gemini API key saved.', 'success');
    this.maybeFetchCoachTip();
  },

  saveName() {
    const input = document.getElementById('name-input');
    Store.updateSettings({ userName: (input?.value || '').trim() || 'there' });
    this.toast('Saved.', 'success');
    this.render();
  },

  async weeklyReview() {
    if (!this.requireApiKey()) return;
    const weekAgo = Date.now() - 7 * 86400000;
    const tasks = Store.state.tasks.filter(t => t.createdAt >= weekAgo || (t.completedAt && t.completedAt >= weekAgo));
    const stats = {
      completed: tasks.filter(t => t.done).map(t => t.title),
      delayed: tasks.filter(t => !t.done && t.bucket !== 'later').map(t => t.title),
      focusMinutes: Store.state.focusSessions.filter(s => s.startedAt >= weekAgo).reduce((s, x) => s + (x.durationMin || 0), 0),
      sessions: Store.state.focusSessions.filter(s => s.startedAt >= weekAgo).length,
      streak: Store.state.streak.count
    };
    const result = await AI.weeklyReview(stats);
    this.openSheet(UI.renderWeeklyReviewSheet(result));
  }
};

window.onNativeSpeechResult = function (text) { FF.onSpeechResult(text); };

document.addEventListener('DOMContentLoaded', () => FF.init());
