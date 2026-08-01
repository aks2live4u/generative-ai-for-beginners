/* FocusFlow AI — local-first data layer.
   Everything lives in localStorage. No account, no server; only the
   Gemini API calls (js/gemini.js) leave the device, and only when the
   user explicitly triggers an AI action. */

const STORAGE_KEY = 'focusflow_state_v1';

function uid(prefix) {
  return (prefix ? prefix + '_' : '') + Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
}

function todayStr(d) {
  const dt = d || new Date();
  return dt.toISOString().slice(0, 10);
}

function defaultState() {
  return {
    settings: {
      userName: 'there',
      geminiApiKey: '',
      geminiModel: 'gemini-2.0-flash',
      energyLevel: 'Medium',
      dailyFocusMinutes: 480,
      onboarded: false
    },
    streak: { count: 0, lastActiveDate: null },
    gamification: { xp: 0, coins: 0, level: 1, sessionsToday: 0, lastSessionDate: null },
    projects: [],
    tasks: [],
    inbox: [],
    waitingList: [],
    focusSessions: [],
    chat: [],
    today: { dateGenerated: null, missionTaskId: null, secondaryTaskIds: [], optionalTaskIds: [] },
    coach: { lastTip: '', lastTipDate: null }
  };
}

function migrateState(state) {
  const fresh = defaultState();
  // Shallow-merge top level, deep-merge settings/gamification/streak/today so new fields land.
  const merged = Object.assign({}, fresh, state);
  merged.settings = Object.assign({}, fresh.settings, state.settings || {});
  merged.gamification = Object.assign({}, fresh.gamification, state.gamification || {});
  merged.streak = Object.assign({}, fresh.streak, state.streak || {});
  merged.today = Object.assign({}, fresh.today, state.today || {});
  merged.coach = Object.assign({}, fresh.coach, state.coach || {});
  for (const key of ['projects', 'tasks', 'inbox', 'waitingList', 'focusSessions', 'chat']) {
    merged[key] = Array.isArray(state[key]) ? state[key] : fresh[key];
  }
  return merged;
}

const Store = {
  _state: null,

  load() {
    if (this._state) return this._state;
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      this._state = raw ? migrateState(JSON.parse(raw)) : defaultState();
    } catch (e) {
      console.error('FocusFlow: failed to load state, resetting', e);
      this._state = defaultState();
    }
    return this._state;
  },

  save() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this._state));
    document.dispatchEvent(new CustomEvent('ff:state-changed'));
  },

  get state() {
    return this.load();
  },

  reset() {
    this._state = defaultState();
    this.save();
  },

  // ---------- settings ----------
  updateSettings(patch) {
    Object.assign(this.state.settings, patch);
    this.save();
  },

  hasApiKey() {
    return !!(this.state.settings.geminiApiKey && this.state.settings.geminiApiKey.trim());
  },

  // ---------- streak / gamification ----------
  touchStreak() {
    const s = this.state.streak;
    const today = todayStr();
    if (s.lastActiveDate === today) return;
    const yesterday = todayStr(new Date(Date.now() - 86400000));
    s.count = s.lastActiveDate === yesterday ? s.count + 1 : 1;
    s.lastActiveDate = today;
    this.save();
  },

  addXp(amount) {
    const g = this.state.gamification;
    g.xp += amount;
    g.coins += Math.max(1, Math.round(amount / 5));
    const nextLevelAt = g.level * 100;
    if (g.xp >= nextLevelAt) {
      g.level += 1;
    }
    this.save();
    return g;
  },

  // ---------- inbox ----------
  addInboxItem(item) {
    const entry = Object.assign({
      id: uid('inbox'),
      type: 'note',
      title: '',
      content: '',
      createdAt: Date.now(),
      processed: false,
      aiSuggestion: null
    }, item);
    this.state.inbox.unshift(entry);
    this.save();
    return entry;
  },

  updateInboxItem(id, patch) {
    const item = this.state.inbox.find(i => i.id === id);
    if (!item) return null;
    Object.assign(item, patch);
    this.save();
    return item;
  },

  removeInboxItem(id) {
    this.state.inbox = this.state.inbox.filter(i => i.id !== id);
    this.save();
  },

  // ---------- projects ----------
  addProject(project) {
    const entry = Object.assign({
      id: uid('proj'),
      name: 'Untitled Project',
      icon: '📁',
      color: '#7C5CFC',
      status: 'active',
      sections: [],
      createdAt: Date.now()
    }, project);
    this.state.projects.unshift(entry);
    this.save();
    return entry;
  },

  getProject(id) {
    return this.state.projects.find(p => p.id === id) || null;
  },

  updateProject(id, patch) {
    const p = this.getProject(id);
    if (!p) return null;
    Object.assign(p, patch);
    this.save();
    return p;
  },

  removeProject(id) {
    this.state.projects = this.state.projects.filter(p => p.id !== id);
    this.state.tasks = this.state.tasks.filter(t => t.projectId !== id);
    this.save();
  },

  projectProgress(id) {
    const tasks = this.tasksForProject(id);
    if (!tasks.length) return 0;
    const done = tasks.filter(t => t.done).length;
    return Math.round((done / tasks.length) * 100);
  },

  tasksForProject(id) {
    return this.state.tasks.filter(t => t.projectId === id);
  },

  // ---------- tasks ----------
  addTask(task) {
    const entry = Object.assign({
      id: uid('task'),
      title: 'Untitled task',
      projectId: null,
      sectionId: null,
      subtasks: [],
      done: false,
      bucket: 'later', // now | today | tomorrow | week | later
      priorityScore: 0,
      estimateMin: 25,
      energy: 'Medium',
      dueDate: null,
      notes: '',
      waitingOn: null,
      createdAt: Date.now(),
      completedAt: null
    }, task);
    this.state.tasks.unshift(entry);
    this.save();
    return entry;
  },

  getTask(id) {
    return this.state.tasks.find(t => t.id === id) || null;
  },

  updateTask(id, patch) {
    const t = this.getTask(id);
    if (!t) return null;
    Object.assign(t, patch);
    this.save();
    return t;
  },

  removeTask(id) {
    this.state.tasks = this.state.tasks.filter(t => t.id !== id);
    this.save();
  },

  completeTask(id) {
    const t = this.updateTask(id, { done: true, completedAt: Date.now() });
    if (t) this.addXp(15);
    return t;
  },

  toggleSubtask(taskId, subtaskId) {
    const t = this.getTask(taskId);
    if (!t) return;
    const st = t.subtasks.find(s => s.id === subtaskId);
    if (!st) return;
    st.done = !st.done;
    this.save();
  },

  openTasks() {
    return this.state.tasks.filter(t => !t.done);
  },

  tasksByBucket(bucket) {
    return this.openTasks().filter(t => t.bucket === bucket);
  },

  // ---------- waiting list ----------
  addWaiting(entry) {
    const w = Object.assign({
      id: uid('wait'),
      text: '',
      who: '',
      since: Date.now(),
      followUpDate: null,
      resolved: false
    }, entry);
    this.state.waitingList.unshift(w);
    this.save();
    return w;
  },

  resolveWaiting(id) {
    const w = this.state.waitingList.find(x => x.id === id);
    if (!w) return;
    w.resolved = true;
    this.save();
  },

  // ---------- focus sessions ----------
  startFocusSession(taskId) {
    const session = { id: uid('sess'), taskId, startedAt: Date.now(), endedAt: null, durationMin: 0, outcome: null };
    this.state.focusSessions.unshift(session);
    this.save();
    return session;
  },

  endFocusSession(id, outcome) {
    const s = this.state.focusSessions.find(x => x.id === id);
    if (!s) return null;
    s.endedAt = Date.now();
    s.durationMin = Math.round((s.endedAt - s.startedAt) / 60000);
    s.outcome = outcome;
    const g = this.state.gamification;
    const today = todayStr();
    g.sessionsToday = g.lastSessionDate === today ? g.sessionsToday + 1 : 1;
    g.lastSessionDate = today;
    this.save();
    return s;
  },

  minutesFocusedToday() {
    const today = todayStr();
    return this.state.focusSessions
      .filter(s => s.endedAt && todayStr(new Date(s.startedAt)) === today)
      .reduce((sum, s) => sum + s.durationMin, 0);
  },

  // ---------- chat ----------
  addChatMessage(role, text) {
    const msg = { id: uid('msg'), role, text, ts: Date.now() };
    this.state.chat.push(msg);
    if (this.state.chat.length > 200) this.state.chat.shift();
    this.save();
    return msg;
  },

  // ---------- today's mission ----------
  setToday(payload) {
    Object.assign(this.state.today, payload, { dateGenerated: todayStr() });
    this.save();
  },

  needsFreshToday() {
    return this.state.today.dateGenerated !== todayStr();
  }
};

window.Store = Store;
window.FF_UID = uid;
