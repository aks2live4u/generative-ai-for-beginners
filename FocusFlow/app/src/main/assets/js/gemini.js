/* FocusFlow AI — Gemini API wrapper.
   Calls the Gemini REST API directly from the device using the user's own
   API key (stored locally, never sent anywhere but Google's endpoint).
   Get a free key at https://aistudio.google.com/apikey */

class MissingApiKeyError extends Error {
  constructor() {
    super('missing-api-key');
    this.name = 'MissingApiKeyError';
  }
}

const GEMINI_ENDPOINT = 'https://generativelanguage.googleapis.com/v1beta/models';

async function callGemini({ contents, systemInstruction, schema, temperature }) {
  const apiKey = Store.state.settings.geminiApiKey;
  if (!apiKey) throw new MissingApiKeyError();
  const model = Store.state.settings.geminiModel || 'gemini-2.0-flash';

  const body = {
    contents,
    generationConfig: {
      temperature: temperature ?? 0.6,
      ...(schema ? { responseMimeType: 'application/json', responseSchema: schema } : {})
    }
  };
  if (systemInstruction) {
    body.systemInstruction = { parts: [{ text: systemInstruction }] };
  }

  const res = await fetch(`${GEMINI_ENDPOINT}/${model}:generateContent?key=${encodeURIComponent(apiKey)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });

  if (!res.ok) {
    let detail = '';
    try { detail = (await res.json()).error?.message || ''; } catch (_) {}
    throw new Error(`Gemini request failed (${res.status}) ${detail}`);
  }

  const data = await res.json();
  const candidate = data.candidates && data.candidates[0];
  const text = candidate?.content?.parts?.map(p => p.text || '').join('') || '';
  if (candidate?.finishReason === 'SAFETY') {
    throw new Error('Gemini declined to respond to that (safety filter).');
  }
  return text;
}

async function generateJSON(opts) {
  const text = await callGemini(opts);
  try {
    return JSON.parse(text);
  } catch (e) {
    // Occasionally the model wraps JSON in markdown fences despite the schema hint.
    const match = text.match(/\{[\s\S]*\}|\[[\s\S]*\]/);
    if (match) return JSON.parse(match[0]);
    throw new Error('Gemini returned invalid JSON: ' + text.slice(0, 200));
  }
}

const AI = {
  MissingApiKeyError,

  async parseBrainDump(rawText) {
    const schema = {
      type: 'OBJECT',
      properties: {
        projectName: { type: 'STRING' },
        projectIcon: { type: 'STRING', description: 'One emoji representing the project' },
        tasks: {
          type: 'ARRAY',
          items: {
            type: 'OBJECT',
            properties: {
              title: { type: 'STRING' },
              category: { type: 'STRING', description: 'e.g. Research, Booking, Admin, Shopping, Follow-up' },
              bucket: { type: 'STRING', enum: ['now', 'today', 'tomorrow', 'week', 'later'] },
              estimateMin: { type: 'NUMBER' },
              dueHint: { type: 'STRING', description: 'natural language due date if mentioned, else empty' },
              isReminder: { type: 'BOOLEAN', description: 'true if this is a "remind me to..." style item, not a real task' }
            },
            required: ['title', 'bucket']
          }
        }
      },
      required: ['projectName', 'tasks']
    };
    return generateJSON({
      systemInstruction: `You are the planning brain inside FocusFlow AI, a task app built for people with ADHD.
The user will "brain dump" whatever is in their head in messy, run-on natural language.
Turn it into ONE coherent project with a short punchy name, a single representative emoji icon,
and a flat list of concrete, actionable tasks (small enough to start immediately — split big vague
asks like "plan trip" into their real sub-actions: research, book, buy, renew, ask, etc.).
Assign each task a bucket (now/today/tomorrow/week/later) based on urgency implied in the text.
Keep task titles short (under 8 words) and start with a verb. Do not add commentary.`,
      contents: [{ role: 'user', parts: [{ text: rawText }] }],
      schema,
      temperature: 0.4
    });
  },

  async breakdownTask(task) {
    const schema = {
      type: 'OBJECT',
      properties: {
        steps: {
          type: 'ARRAY',
          items: {
            type: 'OBJECT',
            properties: {
              title: { type: 'STRING' },
              estimateMin: { type: 'NUMBER' }
            },
            required: ['title']
          }
        }
      },
      required: ['steps']
    };
    return generateJSON({
      systemInstruction: `You help someone with ADHD who is frozen because a task feels too big or too vague to start.
Break the task into 4-7 tiny, concrete, sequential steps. The FIRST step must be almost trivially easy
(e.g. "Open the document", "Sit at your desk") to defeat task-initiation paralysis. Steps should read like
literal physical/mental actions, not abstractions. Keep each title under 8 words.`,
      contents: [{ role: 'user', parts: [{ text: `Task: "${task.title}"${task.notes ? `\nContext: ${task.notes}` : ''}` }] }],
      schema,
      temperature: 0.5
    });
  },

  async triageInboxItem(item) {
    const schema = {
      type: 'OBJECT',
      properties: {
        suggestedAction: { type: 'STRING', enum: ['create_project', 'create_task', 'reminder', 'research_later', 'archive', 'delegate'] },
        cleanTitle: { type: 'STRING' },
        reason: { type: 'STRING', description: 'One short sentence explaining the suggestion' }
      },
      required: ['suggestedAction', 'cleanTitle', 'reason']
    };
    return generateJSON({
      systemInstruction: `You triage a single item in an ADHD-friendly inbox. Decide the single best next action:
create_project (multi-step effort), create_task (one concrete action), reminder (time-based nudge),
research_later (needs more info before it's actionable), archive (no action needed), or delegate
(better handed to someone else). Also produce a clean, short title for the item.`,
      contents: [{ role: 'user', parts: [{ text: `Type: ${item.type}\nContent: ${item.content || item.title}` }] }],
      schema,
      temperature: 0.3
    });
  },

  async prioritizeTasks(tasks, context) {
    const schema = {
      type: 'OBJECT',
      properties: {
        missionTaskId: { type: 'STRING', description: 'the single most important task to feature as Today\'s Mission' },
        secondaryTaskIds: { type: 'ARRAY', items: { type: 'STRING' } },
        optionalTaskIds: { type: 'ARRAY', items: { type: 'STRING' } },
        ranked: {
          type: 'ARRAY',
          items: {
            type: 'OBJECT',
            properties: {
              id: { type: 'STRING' },
              bucket: { type: 'STRING', enum: ['now', 'today', 'tomorrow', 'week', 'later'] }
            },
            required: ['id', 'bucket']
          }
        }
      },
      required: ['missionTaskId', 'secondaryTaskIds', 'ranked']
    };
    const list = tasks.map(t => `- id:${t.id} | "${t.title}" | energy:${t.energy} | est:${t.estimateMin}m | due:${t.dueDate || 'none'} | bucket:${t.bucket}`).join('\n');
    return generateJSON({
      systemInstruction: `You are an executive-function coach prioritizing an ADHD user's open tasks.
Consider deadline urgency, importance, effort, and current energy level (${context.energyLevel}).
Pick exactly ONE task as "Today's Mission" (the single most important thing), up to 2 secondary tasks,
and a few optional/low-stakes tasks. Re-bucket every remaining task into now/today/tomorrow/week/later.
Never overload — a good day has one mission, not ten.`,
      contents: [{ role: 'user', parts: [{ text: `Open tasks:\n${list}\n\nTime left today: ${context.minutesLeftToday} min.` }] }],
      schema,
      temperature: 0.4
    });
  },

  async stuckRescue(task, reasonKey) {
    const schema = {
      type: 'OBJECT',
      properties: {
        empathyLine: { type: 'STRING', description: 'one warm, non-judgmental sentence acknowledging the feeling' },
        microStep: { type: 'STRING', description: 'a single next action under 2 minutes, extremely concrete' },
        tip: { type: 'STRING', description: 'one short practical tip tailored to the reason given' }
      },
      required: ['empathyLine', 'microStep', 'tip']
    };
    return generateJSON({
      systemInstruction: `You are the "I'm Stuck" rescue assistant in an ADHD productivity app. Never be preachy,
never say "just do it". Be warm, brief, and extremely concrete. Give one 2-minute micro-step that removes
the friction described.`,
      contents: [{ role: 'user', parts: [{ text: `Task: "${task.title}"\nWhy I'm stuck: ${reasonKey}` }] }],
      schema,
      temperature: 0.6
    });
  },

  async chatReply(history, message, context) {
    const contents = history.slice(-12).map(m => ({
      role: m.role === 'assistant' ? 'model' : 'user',
      parts: [{ text: m.text }]
    }));
    contents.push({ role: 'user', parts: [{ text: message }] });
    return callGemini({
      systemInstruction: `You are the AI Assistant inside FocusFlow AI, a warm, non-judgmental executive-function
coach for someone with ADHD. Keep replies short (2-4 sentences, or a short list). Be concrete and
action-oriented — help them plan, refocus, or simplify. Never shame them for procrastination or missed
tasks; treat it as data, not failure. Here is their current state for context:\n${context}`,
      contents,
      temperature: 0.7
    });
  },

  async coachTip(context) {
    return callGemini({
      systemInstruction: `You write a single short (under 20 words), encouraging, specific home-screen tip for
an ADHD user based on their current context. No generic platitudes — reference their actual situation.`,
      contents: [{ role: 'user', parts: [{ text: context }] }],
      temperature: 0.8
    });
  },

  async weeklyReview(stats) {
    const schema = {
      type: 'OBJECT',
      properties: {
        summary: { type: 'STRING' },
        wins: { type: 'ARRAY', items: { type: 'STRING' } },
        patterns: { type: 'ARRAY', items: { type: 'STRING' } },
        suggestions: { type: 'ARRAY', items: { type: 'STRING' } }
      },
      required: ['summary', 'wins', 'patterns', 'suggestions']
    };
    return generateJSON({
      systemInstruction: `Write a short, kind weekly review for an ADHD user based on their task data.
No guilt about missed/delayed tasks — frame everything as useful pattern data and gentle next-week
suggestions.`,
      contents: [{ role: 'user', parts: [{ text: JSON.stringify(stats) }] }],
      schema,
      temperature: 0.6
    });
  }
};

window.AI = AI;
