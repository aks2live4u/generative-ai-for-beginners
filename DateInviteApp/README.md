# Date Invite App 💌

A single-file, no-backend "will you go on a date with me?" web app, inspired by the
classic Replit dating-invite gag: a dodging "no" button, a confetti "yes", a
date/time + food picker, a dramatic acceptance message, and a joke "payment" screen.

Everything lives in [`index.html`](./index.html) — no build step, no server,
no dependencies. Just open the file in a browser, or host it anywhere static
(GitHub Pages, Netlify, Vercel, etc.) and send the link.

## Flow

1. **"Will you go on a date with me?"** — the `no` button dodges the cursor/finger.
2. Click **YES** → confetti burst → "wait you actually said yes??" reaction screen.
3. Pick a **day and time**.
4. Pick the food **vibe** (pizza, sushi, burgers, pasta, tacos, ramen).
5. A personalized **acceptance message** with the chosen date/time/food.
6. A joke **"one small fee"** screen — instead of asking for real money, the
   "payment" is 100 kisses sent over **WhatsApp**, and it fires a **push
   notification** to you via [ntfy.sh](https://ntfy.sh) so you know the moment
   someone accepts.
7. A final "it's official!" confirmation screen with a summary.

## Setup — edit the `CONFIG` block

Open `index.html` and find the `CONFIG` object near the top of the `<script>`
tag:

```js
const CONFIG = {
  SENDER_NAME: "me",
  WHATSAPP_NUMBER: "",       // e.g. "919876543210" (country code, no +, no spaces)
  KISS_COUNT: 100,
  FEE_INR: 49,
  UPI_ID: "",                 // optional real UPI VPA, see below
  NTFY_TOPIC: "aks2live4u-date-invite-7f3q9k2p"
};
```

- **`WHATSAPP_NUMBER`** — leave blank and the "pay" button opens WhatsApp's
  contact picker so the person can send the kisses message to anyone (you).
  Set your own number (international format, digits only) to pre-fill the
  chat directly with you.
- **`FEE_INR`** — the joke fee shown on the payment screen, in ₹.
- **`UPI_ID`** — optional. If you'd rather also offer a *real* Google
  Pay / PhonePe payment (e.g. an actual ₹49 UPI request) instead of just the
  kisses joke, set this to your UPI VPA (e.g. `yourname@okhdfcbank`). This
  reveals a secondary "pay the boring way" link that opens a standard
  `upi://pay` intent — Android will let the person choose Google Pay,
  PhonePe, or any other UPI app installed. This only works when opened on a
  phone with a UPI app installed (desktop browsers can't handle the
  `upi://` scheme).
- **`NTFY_TOPIC`** — the app pushes a free notification via
  [ntfy.sh](https://ntfy.sh) when someone completes the flow. To receive it:
  1. Install the **ntfy** app (Android/iOS) or just open
     `https://ntfy.sh/<your-topic>` in a browser.
  2. Subscribe to the exact topic name set in `NTFY_TOPIC`.
  3. **Change the topic to something private/hard-to-guess** before sharing
     the page — ntfy topics are public by default, so anyone who knows the
     name could subscribe to your notifications too.

## Personalizing further

- Swap the `💌` avatar emoji on the first screen for an actual photo by
  replacing the `<div class="avatar">💌</div>` with an `<img>` tag.
- Edit any of the copy directly in the `<div class="card screen ...">`
  blocks in `index.html` — it's plain HTML.

## Notes

- This is a static, client-side page — there is no server, database, or real
  payment processor involved. The "payment" is a WhatsApp message, not an
  actual money transfer, unless you configure `UPI_ID` for the optional real
  UPI link.
- ntfy.sh is a free, no-signup pub/sub notification service. Messages sent to
  a topic are not encrypted or private by default — don't put sensitive
  information in the notification payload, and pick a hard-to-guess topic
  name.
