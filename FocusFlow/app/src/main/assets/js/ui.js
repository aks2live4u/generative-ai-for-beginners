/* FocusFlow AI — presentational helpers & screen templates (pure functions
   returning HTML strings). Wiring/events/state live in app.js. */

const BUCKET_LABEL = { now: 'Do Now', today: 'Today', tomorrow: 'Tomorrow', week: 'This Week', later: 'Later' };
const BUCKET_COLOR = { now: 'var(--danger)', today: 'var(--warning)', tomorrow: 'var(--info)', week: 'var(--accent-2)', later: 'var(--text-muted)' };
const INBOX_ICON = { voice: '🎙️', photo: '🖼️', email: '✉️', link: '🔗', note: '📝', idea: '💡', meeting: '🗒️', braindump: '🌊' };
const ENERGY_ICON = { High: '⚡', Medium: '🌤️', Low: '🌙' };
const REASONS = [
  ['dont_know', "❓", "Don't know where to start"],
  ['boring', '😐', 'Too boring'],
  ['hard', '🥵', 'Too difficult'],
  ['tired', '🔋', 'Too tired'],
  ['motivation', '❤️', 'Need motivation'],
  ['anxious', '😰', 'Anxious / overwhelmed'],
  ['perfectionism', '🌀', 'Perfectionism']
];

function esc(str) {
  return String(str ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function fmtClock(ts) {
  return new Date(ts).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
}

function fmtDay(ts) {
  const d = new Date(ts);
  const today = new Date();
  const diffDays = Math.floor((today.setHours(0,0,0,0) - new Date(d).setHours(0,0,0,0)) / 86400000);
  if (diffDays === 0) return fmtClock(ts);
  if (diffDays === 1) return 'Yesterday';
  if (diffDays < 7) return d.toLocaleDateString([], { weekday: 'short' });
  return d.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

function minutesToHM(min) {
  min = Math.max(0, Math.round(min));
  const h = Math.floor(min / 60), m = min % 60;
  if (h <= 0) return `${m}m`;
  return `${h}h ${m}m`;
}

function minutesUntilWorkEnd() {
  const now = new Date();
  const end = new Date();
  end.setHours(21, 0, 0, 0);
  return Math.max(0, Math.round((end - now) / 60000));
}

function greetingWord() {
  const h = new Date().getHours();
  if (h < 12) return 'Good Morning';
  if (h < 17) return 'Good Afternoon';
  return 'Good Evening';
}

function mmss(totalSeconds) {
  totalSeconds = Math.max(0, Math.round(totalSeconds));
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

function timerRingStyle(pct) {
  const deg = Math.round(pct * 360);
  return `background: conic-gradient(var(--accent) ${deg}deg, rgba(255,255,255,0.06) ${deg}deg); border-radius: 50%;`;
}

/* ---------------- bottom nav ---------------- */

const NAV_ITEMS = [
  ['home', '🏠', 'Home'],
  ['projects', '📁', 'Projects'],
  ['focus', '🎯', 'Focus'],
  null, // center fab slot
  ['inbox', '📥', 'Inbox'],
  ['more', '⋯', 'More']
];

function renderBottomNav(route, unprocessedCount) {
  const active = route.split('/')[0];
  return NAV_ITEMS.map(item => {
    if (!item) {
      return `<button class="nav-fab" data-action="quick-add">+</button>`;
    }
    const [key, icon, label] = item;
    const isActive = active === key || (key === 'home' && active === '');
    const dot = key === 'inbox' && unprocessedCount > 0 ? `<span class="nav-dot"></span>` : '';
    return `<button class="nav-item ${isActive ? 'active' : ''}" data-nav="${key}">
      <span class="nav-icon">${icon}</span><span>${label}</span>${dot}
    </button>`;
  }).join('');
}

/* ---------------- home ---------------- */

function renderHome(ctx) {
  const { state } = ctx;
  const missionTask = state.today.missionTaskId ? Store.getTask(state.today.missionTaskId) : null;
  const secondary = (state.today.secondaryTaskIds || []).map(id => Store.getTask(id)).filter(t => t && !t.done);
  const minutesLeft = minutesUntilWorkEnd();
  const focusedToday = Store.minutesFocusedToday();

  const plannedDoneCount = [missionTask, ...secondary].filter(t => t && t.done).length;
  const plannedTotal = [missionTask, ...secondary].filter(Boolean).length || 1;
  const ringPct = Math.round((plannedDoneCount / plannedTotal) * 100);

  const missionBlock = missionTask ? `
    <div class="mission-header">
      <div class="mission-eyebrow">🎯 Today's Mission</div>
      <div class="ring" style="${timerRingStyle(ringPct / 100)}">
        <div style="width:40px;height:40px;border-radius:50%;background:var(--card);display:flex;align-items:center;justify-content:center;">${ringPct}%</div>
      </div>
    </div>
    <div class="mission-title">${esc(missionTask.title)}</div>
    <span class="tag">${missionTask.energy === 'High' ? 'High Impact' : missionTask.energy + ' energy'}</span>
    ${secondary[0] ? `
    <div class="next-up">
      <div>
        <div class="next-up-label">Next up</div>
        <div class="next-up-title">${esc(secondary[0].title)}</div>
      </div>
      <div class="muted small">⏱ ${secondary[0].estimateMin}m</div>
    </div>` : ''}
  ` : `
    <div class="mission-eyebrow">🎯 Today's Focus</div>
    <div class="mission-title">No mission set yet</div>
    <p class="muted small" style="margin:2px 0 14px;">Let AI look at your open tasks and pick what matters most today.</p>
    <button class="btn btn-primary" data-action="plan-day">✨ Plan My Day with AI</button>
  `;

  return `
    <div class="topbar"><h1>&nbsp;</h1><button class="icon-btn" data-nav="more">⚙️</button></div>
    <div class="greeting">${greetingWord()}, ${esc(state.settings.userName)} 👋</div>
    <div class="greeting-sub">Let's make today count.</div>

    <div class="card mission-card">${missionBlock}</div>

    <div class="stat-row">
      <div class="stat-tile"><div class="stat-label">Time Left Today</div><div class="stat-value">${minutesToHM(minutesLeft)}</div></div>
      <div class="stat-tile" data-action="cycle-energy"><div class="stat-label">Energy Level</div><div class="stat-value">${ENERGY_ICON[state.settings.energyLevel]} ${state.settings.energyLevel}</div></div>
      <div class="stat-tile"><div class="stat-label">Focus Streak</div><div class="stat-value">🔥 ${state.streak.count}d</div></div>
    </div>

    <div class="card coach-card mt-14">
      <div class="coach-emoji">🧠</div>
      <div class="grow">
        <div class="coach-title">AI Coach</div>
        <div class="coach-text" id="coach-tip-text">${esc(state.coach.lastTip) || `You've focused ${minutesToHM(focusedToday)} today. Nice work — keep the streak going.`}</div>
      </div>
    </div>

    <button class="btn btn-primary start-btn" data-action="start-working">▶ Start Working</button>
  `;
}

/* ---------------- inbox ---------------- */

function renderInboxItem(item) {
  const icon = INBOX_ICON[item.type] || '📝';
  const title = item.title || (item.content || '').slice(0, 60);
  const actionsOpen = item._open;
  return `
  <div class="inbox-item" data-inbox-id="${item.id}">
    <div class="inbox-icon" style="background:var(--accent-soft);">${icon}</div>
    <div class="inbox-body" data-action="toggle-inbox" data-id="${item.id}">
      <div class="row between">
        <div class="inbox-title truncate" style="max-width:70%;">${esc(title)}</div>
        <div class="inbox-time">${fmtDay(item.createdAt)}</div>
      </div>
      <div class="inbox-content">${esc((item.content || '').slice(0, 120))}</div>
      <div class="inbox-meta">
        ${item.processed ? `<span class="tag">✓ Processed</span>` : `<span class="muted">Unprocessed</span>`}
      </div>
      ${actionsOpen ? `
        <div class="mt-14" onclick="event.stopPropagation()">
          <div class="muted small mt-8" style="margin-bottom:8px;">What do you want me to do with this?</div>
          <div class="chip-row tight" style="margin:0 0 8px;padding:0;">
            <button class="chip" data-action="triage-suggest" data-id="${item.id}">✨ AI Suggest</button>
          </div>
          <div class="btn-row" style="flex-wrap:wrap;gap:8px;">
            <button class="btn btn-sm btn-secondary" data-action="inbox-do" data-id="${item.id}" data-do="create_project">📁 Create Project</button>
            <button class="btn btn-sm btn-secondary" data-action="inbox-do" data-id="${item.id}" data-do="create_task">✅ Create Task</button>
            <button class="btn btn-sm btn-secondary" data-action="inbox-do" data-id="${item.id}" data-do="reminder">🔔 Reminder</button>
            <button class="btn btn-sm btn-secondary" data-action="inbox-do" data-id="${item.id}" data-do="research_later">🔍 Research Later</button>
            <button class="btn btn-sm btn-secondary" data-action="inbox-do" data-id="${item.id}" data-do="archive">🗄 Archive</button>
            <button class="btn btn-sm btn-secondary" data-action="inbox-do" data-id="${item.id}" data-do="delegate">🤝 Delegate</button>
          </div>
          ${item.aiSuggestion ? `<div class="muted small mt-8">💡 ${esc(item.aiSuggestion.reason)}</div>` : ''}
        </div>
      ` : ''}
    </div>
  </div>`;
}

function renderInbox(ctx) {
  const { state, inboxFilter } = ctx;
  let items = state.inbox;
  if (inboxFilter === 'unprocessed') items = items.filter(i => !i.processed);
  if (inboxFilter === 'processed') items = items.filter(i => i.processed);
  const unprocessedCount = state.inbox.filter(i => !i.processed).length;

  return `
    <div class="topbar"><h1>✨ AI Inbox</h1></div>
    <div class="chip-row">
      <button class="chip ${inboxFilter === 'all' ? 'active' : ''}" data-action="inbox-filter" data-filter="all">All</button>
      <button class="chip ${inboxFilter === 'unprocessed' ? 'active' : ''}" data-action="inbox-filter" data-filter="unprocessed">Unprocessed ${unprocessedCount ? `(${unprocessedCount})` : ''}</button>
      <button class="chip ${inboxFilter === 'processed' ? 'active' : ''}" data-action="inbox-filter" data-filter="processed">Processed</button>
    </div>
    <div class="card mt-14">
      <div class="row" style="gap:8px;">
        <input class="ff-input" id="quick-capture-input" placeholder="Capture a thought…" />
        <button class="icon-btn" data-action="mic-capture" style="background:var(--accent-soft);color:var(--accent-2);">🎙️</button>
        <button class="icon-btn" data-action="submit-capture">➤</button>
      </div>
    </div>
    ${items.length ? `<div class="card mt-14" style="padding:6px 18px;">${items.map(renderInboxItem).join('')}</div>` :
      `<div class="empty-state"><div class="emoji">📭</div>Nothing here. Capture a thought, photo, or idea and AI will help you decide what to do with it.</div>`}
  `;
}

/* ---------------- brain dump ---------------- */

function renderBrainDump(ctx) {
  const { state, listening } = ctx;
  const dumps = state.inbox.filter(i => i.type === 'braindump').slice(0, 8);
  return `
    <div class="topbar"><h1>🌊 AI Brain Dump</h1></div>
    <div class="dump-mic-wrap">
      <button class="dump-mic ${listening ? 'listening' : ''}" data-action="dump-mic">🎙️</button>
      <div class="dump-hint">${listening ? 'Listening…' : 'Tap to speak, or type below'}</div>
    </div>
    <textarea class="ff-input" id="dump-textarea" rows="5" placeholder="I'm going to Goa next month, need hotels, renew passport, buy sunscreen, book flights and remind me to ask Raj about leave…"></textarea>
    <button class="btn btn-primary mt-14" data-action="dump-process">✨ Turn Into a Plan</button>

    <div class="section-label">Recent Dumps</div>
    ${dumps.length ? dumps.map(d => `
      <div class="card" style="padding:14px 16px;">
        <div class="dump-recent-item" style="border:none;padding:0;">
          <div class="grow">
            <div class="small" style="line-height:1.4;">${esc(d.content.slice(0, 140))}${d.content.length > 140 ? '…' : ''}</div>
            <div class="row mt-8" style="gap:6px;">
              ${d.processed ? `<span class="tag">✓ AI Processed</span>` : `<span class="muted small">Not processed</span>`}
              <span class="muted small">· ${fmtDay(d.createdAt)}</span>
            </div>
          </div>
        </div>
      </div>
    `).join('') : `<div class="empty-state"><div class="emoji">🌊</div>Nothing dumped yet — say whatever's in your head.</div>`}
  `;
}

/* ---------------- projects ---------------- */

function renderProjectCard(p) {
  const progress = Store.projectProgress(p.id);
  const total = Store.tasksForProject(p.id).length;
  const done = Store.tasksForProject(p.id).filter(t => t.done).length;
  return `
  <div class="card project-card" data-action="open-project" data-id="${p.id}">
    <div class="project-icon" style="background:${p.color}22;">${p.icon}</div>
    <div class="project-info">
      <div class="project-name">${esc(p.name)}</div>
      <div class="project-sub">${done}/${total} tasks · ${progress}%</div>
      <div class="progress-track"><div class="progress-fill" style="width:${progress}%;"></div></div>
    </div>
    <div class="task-arrow">›</div>
  </div>`;
}

function renderProjects(ctx) {
  const { state, projectFilter } = ctx;
  let projects = state.projects;
  if (projectFilter !== 'all') projects = projects.filter(p => p.status === projectFilter);

  return `
    <div class="topbar"><h1>Projects</h1><button class="icon-btn" data-action="new-project">+</button></div>
    <div class="chip-row">
      <button class="chip ${projectFilter === 'all' ? 'active' : ''}" data-action="project-filter" data-filter="all">All</button>
      <button class="chip ${projectFilter === 'active' ? 'active' : ''}" data-action="project-filter" data-filter="active">Active</button>
      <button class="chip ${projectFilter === 'planning' ? 'active' : ''}" data-action="project-filter" data-filter="planning">Planning</button>
      <button class="chip ${projectFilter === 'archived' ? 'active' : ''}" data-action="project-filter" data-filter="archived">Archived</button>
    </div>
    <div class="mt-14">
    ${projects.length ? projects.map(renderProjectCard).join('') :
      `<div class="empty-state"><div class="emoji">📁</div>No projects yet. Try an AI Brain Dump — say everything on your mind and AI will build the plan.</div>`}
    </div>
  `;
}

function renderTaskRow(t) {
  return `
  <div class="task-row" data-action="open-task" data-id="${t.id}">
    <button class="task-check ${t.done ? 'done' : ''}" data-action="toggle-task" data-id="${t.id}">${t.done ? '✓' : ''}</button>
    <div class="task-body">
      <div class="task-title ${t.done ? 'done' : ''}">${esc(t.title)}</div>
      <div class="task-meta">
        <span style="color:${BUCKET_COLOR[t.bucket]};">${BUCKET_LABEL[t.bucket]}</span>
        <span>⏱ ${t.estimateMin}m</span>
        ${t.subtasks.length ? `<span>${t.subtasks.filter(s=>s.done).length}/${t.subtasks.length} steps</span>` : ''}
        ${t.waitingOn ? `<span>⏳ ${esc(t.waitingOn)}</span>` : ''}
      </div>
    </div>
    <div class="task-arrow">›</div>
  </div>`;
}

function renderProjectDetail(ctx) {
  const { projectId, state } = ctx;
  const p = Store.getProject(projectId);
  if (!p) return `<div class="empty-state">Project not found.</div>`;
  const tasks = Store.tasksForProject(projectId);
  const progress = Store.projectProgress(projectId);
  const groups = {};
  tasks.forEach(t => {
    const key = t.category || 'Tasks';
    (groups[key] = groups[key] || []).push(t);
  });

  return `
    <div class="topbar">
      <button class="icon-btn" data-nav="projects">←</button>
      <h1 class="truncate">${p.icon} ${esc(p.name)}</h1>
      <button class="icon-btn" data-action="delete-project" data-id="${p.id}">🗑</button>
    </div>
    <div class="card">
      <div class="row between"><div class="muted small">${tasks.filter(t=>t.done).length}/${tasks.length} tasks complete</div><div class="muted small">${progress}%</div></div>
      <div class="progress-track mt-8"><div class="progress-fill" style="width:${progress}%;"></div></div>
    </div>

    <div class="card mt-14">
      <div class="row" style="gap:8px;">
        <input class="ff-input" id="new-task-input" placeholder="Add a task…" />
        <button class="icon-btn" data-action="add-project-task" data-id="${p.id}">➤</button>
      </div>
    </div>

    ${Object.keys(groups).map(g => `
      <div class="section-label">${esc(g)}</div>
      <div class="card" style="padding:4px 18px;">${groups[g].map(renderTaskRow).join('')}</div>
    `).join('') || `<div class="empty-state"><div class="emoji">✅</div>No tasks yet.</div>`}
  `;
}

/* ---------------- task detail sheet ---------------- */

function renderTaskSheet(t) {
  const p = t.projectId ? Store.getProject(t.projectId) : null;
  return `
  <div class="sheet-handle"></div>
  <h2>${esc(t.title)}</h2>
  <div class="sub">${p ? p.icon + ' ' + esc(p.name) + ' · ' : ''}${BUCKET_LABEL[t.bucket]} · ⏱ ${t.estimateMin}m · ${t.energy} energy</div>

  ${t.subtasks.length ? `
    <div class="section-label" style="margin-top:6px;">Steps</div>
    <div class="card" style="padding:4px 16px;">
      ${t.subtasks.map(s => `
        <div class="task-row">
          <button class="task-check ${s.done ? 'done' : ''}" data-action="toggle-subtask" data-task="${t.id}" data-sub="${s.id}">${s.done ? '✓' : ''}</button>
          <div class="task-body"><div class="task-title ${s.done ? 'done' : ''}" style="font-size:13.5px;">${esc(s.title)}</div></div>
        </div>
      `).join('')}
    </div>
  ` : ''}

  <div class="btn-row mt-14">
    <button class="btn btn-secondary" data-action="breakdown-task" data-id="${t.id}">🧩 Break Into Steps</button>
    <button class="btn btn-primary" data-action="focus-this-task" data-id="${t.id}">▶ Focus</button>
  </div>
  <div class="btn-row mt-8">
    <button class="btn btn-secondary" data-action="waiting-task" data-id="${t.id}">⏳ Mark Waiting On</button>
    <button class="btn btn-success" data-action="complete-task" data-id="${t.id}" ${t.done ? 'disabled' : ''}>${t.done ? '✓ Done' : 'Mark Done'}</button>
  </div>
  <button class="btn btn-danger mt-8" data-action="delete-task" data-id="${t.id}">Delete Task</button>
  `;
}

/* ---------------- focus mode ---------------- */

function renderFocus(ctx) {
  const { state, focusTask, timer } = ctx;
  if (!focusTask) {
    return `
      <div class="topbar"><h1>Focus Mode</h1></div>
      <div class="empty-state"><div class="emoji">🎯</div>No task selected.<br/><br/>
      <button class="btn btn-primary" data-action="plan-day">✨ Plan My Day with AI</button></div>
    `;
  }
  const secondary = (state.today.secondaryTaskIds || []).map(id => Store.getTask(id)).filter(t => t && t.id !== focusTask.id && !t.done)[0];
  const pct = timer.totalSeconds > 0 ? 1 - (timer.remaining / timer.totalSeconds) : 0;

  return `
    <div class="topbar"><h1>Focus Mode</h1><button class="icon-btn" data-nav="home">✕</button></div>
    <div class="focus-screen">
      <span class="tag">Deep Work</span>
      <div class="timer-wrap" style="${timerRingStyle(pct)}">
        <div style="position:absolute;inset:8px;border-radius:50%;background:var(--bg-elevated);"></div>
        <div class="timer-center">
          <div class="timer-digits">${mmss(timer.remaining)}</div>
          <div class="timer-task">${esc(focusTask.title)}</div>
          <div class="timer-state">${timer.running ? 'Focus session' : 'Paused'}</div>
        </div>
      </div>

      ${secondary ? `
      <div class="card focus-upnext">
        <div class="row between">
          <div><div class="next-up-label">Up Next</div><div class="next-up-title">${esc(secondary.title)}</div></div>
          <div class="muted small">⏱ ${secondary.estimateMin}m</div>
        </div>
      </div>` : ''}

      <div class="focus-controls">
        <button class="btn btn-primary" data-action="focus-done">Done ✓</button>
        <div class="btn-row">
          <button class="btn btn-secondary" data-action="focus-help">✨ Need Help</button>
          <button class="btn btn-success" data-action="focus-skip">Skip ⏭</button>
        </div>
        <div class="btn-row">
          <button class="btn btn-warning" data-action="focus-pause">${timer.running ? 'Pause ⏸' : 'Resume ▶'}</button>
          <button class="btn btn-danger" data-action="focus-distracted">Distracted</button>
        </div>
        <div class="card row between mt-8">
          <div><div class="muted small">Focus Stats</div><div style="font-weight:700;">Today: ${minutesToHM(Store.minutesFocusedToday())}</div></div>
          <div><div class="muted small">Sessions</div><div style="font-weight:700;text-align:right;">${state.gamification.sessionsToday}</div></div>
        </div>
      </div>
    </div>
  `;
}

function renderReasonSheet() {
  return `
    <div class="sheet-handle"></div>
    <h2>✨ I'm Stuck</h2>
    <div class="sub">What's making it hard to start?</div>
    <div class="reason-grid">
      ${REASONS.map(([key, icon, label]) => `
        <button class="reason-chip" data-action="pick-reason" data-reason="${key}"><span>${icon}</span> ${label}</button>
      `).join('')}
    </div>
  `;
}

function renderRescueResult(r) {
  return `
    <div class="sheet-handle"></div>
    <h2>💜 Let's unblock this</h2>
    <p class="small" style="color:var(--text-secondary);line-height:1.5;">${esc(r.empathyLine)}</p>
    <div class="card mt-8" style="background:var(--accent-soft);border-color:rgba(124,92,252,0.3);">
      <div class="tag" style="margin-bottom:8px;">Try this next</div>
      <div style="font-weight:700;">${esc(r.microStep)}</div>
    </div>
    <p class="small muted mt-8">💡 ${esc(r.tip)}</p>
    <button class="btn btn-primary mt-14" data-action="close-sheet">Got it, continue</button>
  `;
}

/* ---------------- chat ---------------- */

function renderChatMsg(m) {
  return `<div class="msg ${m.role === 'user' ? 'user' : 'assistant'}">${esc(m.text).replace(/\n/g, '<br/>')}</div>`;
}

function renderChat(ctx) {
  const { state, sending } = ctx;
  const empty = state.chat.length === 0;
  return `
    <div class="topbar"><h1>🧠 AI Assistant</h1></div>
    <div class="chat-scroll" id="chat-scroll">
      ${empty ? `
        <div class="empty-state" style="padding-top:20px;">
          <div class="emoji">🧠</div>
          Ask me anything — "What should I work on?", "I'm overwhelmed", "I only have 30 minutes".
        </div>
      ` : state.chat.map(renderChatMsg).join('')}
      ${sending ? `<div class="msg assistant"><span class="spinner"></span></div>` : ''}
    </div>
    <div class="chip-row" style="padding-bottom:8px;">
      <button class="chip" data-action="chat-quick" data-msg="What should I work on right now?">What should I work on?</button>
      <button class="chip" data-action="chat-quick" data-msg="I'm feeling overwhelmed, help me simplify.">I'm overwhelmed</button>
      <button class="chip" data-action="chat-quick" data-msg="I only have 30 minutes right now. What should I do?">I have 30 minutes</button>
      <button class="chip" data-action="chat-quick" data-msg="Prepare me for tomorrow.">Prepare for tomorrow</button>
    </div>
    <div class="chat-input-bar">
      <input id="chat-input" placeholder="Ask me anything…" />
      <button class="send-btn" data-action="chat-send">➤</button>
    </div>
  `;
}

/* ---------------- more / settings ---------------- */

function renderWaitingItem(w) {
  return `
  <div class="task-row">
    <div class="task-body">
      <div class="task-title" style="${w.resolved ? 'text-decoration:line-through;color:var(--text-muted);' : ''}">${esc(w.text)}</div>
      <div class="task-meta"><span>Waiting on ${esc(w.who || 'someone')}</span><span>· since ${fmtDay(w.since)}</span></div>
    </div>
    ${!w.resolved ? `<button class="btn btn-sm btn-secondary" data-action="resolve-waiting" data-id="${w.id}">Resolve</button>` : ''}
  </div>`;
}

function renderMore(ctx) {
  const { state } = ctx;
  const openWaiting = state.waitingList.filter(w => !w.resolved);
  return `
    <div class="topbar"><h1>More</h1></div>

    <div class="card">
      <button class="btn btn-primary" data-nav="chat">🧠 Chat with your AI Assistant</button>
    </div>

    <div class="section-label">Energy Level</div>
    <div class="energy-picker">
      ${['Low','Medium','High'].map(e => `<button class="energy-btn ${state.settings.energyLevel===e?'active':''}" data-action="set-energy" data-energy="${e}">${ENERGY_ICON[e]} ${e}</button>`).join('')}
    </div>

    <div class="section-label">Progress</div>
    <div class="card">
      <div class="settings-row"><div class="settings-label">Level ${state.gamification.level}</div><div class="muted small">${state.gamification.xp} XP · 🪙 ${state.gamification.coins}</div></div>
      <div class="settings-row"><div class="settings-label">Focus Streak</div><div class="muted small">🔥 ${state.streak.count} day${state.streak.count === 1 ? '' : 's'}</div></div>
      <div class="settings-row" style="border-bottom:none;"><button class="btn btn-secondary" data-action="weekly-review">📊 AI Weekly Review</button></div>
    </div>

    <div class="section-label">Waiting List</div>
    <div class="card">
      <div class="row" style="gap:8px;">
        <input class="ff-input" id="waiting-input" placeholder="Waiting on… (e.g. Vendor to reply)" />
        <button class="icon-btn" data-action="add-waiting">➤</button>
      </div>
      ${openWaiting.length ? `<div class="mt-8">${openWaiting.map(renderWaitingItem).join('')}</div>` : `<div class="muted small mt-8">Nothing you're waiting on. 🎉</div>`}
    </div>

    <div class="section-label">Gemini API</div>
    <div class="card">
      <div class="settings-sub" style="margin-bottom:10px;">FocusFlow AI calls Gemini directly from your device using your own API key. Get a free key at aistudio.google.com/apikey — it's stored only on this device.</div>
      <input class="ff-input" id="api-key-input" type="password" placeholder="Paste your Gemini API key" value="${esc(state.settings.geminiApiKey)}" />
      <button class="btn btn-primary mt-8" data-action="save-api-key">Save Key</button>
    </div>

    <div class="section-label">Your Name</div>
    <div class="card">
      <input class="ff-input" id="name-input" placeholder="What should I call you?" value="${esc(state.settings.userName)}" />
      <button class="btn btn-secondary mt-8" data-action="save-name">Save</button>
    </div>

    <div class="section-label">Data</div>
    <div class="card">
      <button class="btn btn-danger" data-action="reset-data">Reset All Data</button>
    </div>

    <div class="center muted small mt-20" style="padding-bottom:10px;">FocusFlow AI · your second brain for getting things done.</div>
  `;
}

/* ---------------- misc sheets ---------------- */

function renderQuickAddSheet() {
  return `
    <div class="sheet-handle"></div>
    <h2>Quick Capture</h2>
    <div class="sub">Everything starts in the inbox — sort it out later.</div>
    <div class="btn-row" style="flex-wrap:wrap;gap:10px;">
      <button class="btn btn-secondary" data-action="quick-nav" data-target="braindump">🌊 Brain Dump</button>
      <button class="btn btn-secondary" data-action="quick-nav" data-target="inbox">📥 Quick Note</button>
    </div>
    <div class="btn-row mt-8" style="flex-wrap:wrap;gap:10px;">
      <button class="btn btn-secondary" data-action="quick-nav" data-target="new-project">📁 New Project</button>
      <button class="btn btn-secondary" data-action="quick-nav" data-target="chat">🧠 Ask AI</button>
    </div>
  `;
}

function renderNewProjectSheet() {
  return `
    <div class="sheet-handle"></div>
    <h2>New Project</h2>
    <input class="ff-input mt-8" id="proj-name-input" placeholder="Project name" />
    <button class="btn btn-primary mt-14" data-action="create-project">Create</button>
  `;
}

function renderBreakReminderSheet(minutes) {
  return `
    <div class="sheet-handle"></div>
    <h2>👀 You've been deep in it</h2>
    <div class="sub">${minutes}+ minutes of continuous focus. Great work — take a beat.</div>
    <div class="btn-row" style="flex-wrap:wrap;gap:10px;">
      <div class="chip">💧 Drink water</div>
      <div class="chip">🧘 Stretch</div>
      <div class="chip">🚻 Bathroom</div>
      <div class="chip">💊 Medicine check</div>
    </div>
    <button class="btn btn-primary mt-14" data-action="close-sheet">Continue Focusing</button>
  `;
}

function renderWeeklyReviewSheet(r) {
  return `
    <div class="sheet-handle"></div>
    <h2>📊 Weekly Review</h2>
    <p class="small" style="color:var(--text-secondary);line-height:1.5;">${esc(r.summary)}</p>
    ${r.wins?.length ? `<div class="section-label">Wins</div>` + r.wins.map(w => `<div class="small mt-8">✅ ${esc(w)}</div>`).join('') : ''}
    ${r.patterns?.length ? `<div class="section-label">Patterns</div>` + r.patterns.map(w => `<div class="small mt-8">🔎 ${esc(w)}</div>`).join('') : ''}
    ${r.suggestions?.length ? `<div class="section-label">Next Week</div>` + r.suggestions.map(w => `<div class="small mt-8">💡 ${esc(w)}</div>`).join('') : ''}
    <button class="btn btn-primary mt-14" data-action="close-sheet">Close</button>
  `;
}

window.UI = {
  esc, fmtClock, fmtDay, minutesToHM, minutesUntilWorkEnd, greetingWord, mmss, timerRingStyle,
  BUCKET_LABEL, BUCKET_COLOR, INBOX_ICON, ENERGY_ICON, REASONS,
  renderBottomNav, renderHome, renderInbox, renderBrainDump, renderProjects, renderProjectDetail,
  renderTaskSheet, renderFocus, renderReasonSheet, renderRescueResult, renderChat, renderMore,
  renderQuickAddSheet, renderNewProjectSheet, renderBreakReminderSheet, renderWeeklyReviewSheet
};
