#!/data/data/com.termux/files/usr/bin/bash
# One-time setup for running the app directly on an Android phone with Termux.
# Usage (inside Termux):  bash termux-setup.sh
set -e

pkg update -y
pkg install -y python ffmpeg

# Lets Termux write to the phone's shared storage (~/storage/downloads).
termux-setup-storage || true

pip install -r "$(dirname "$0")/requirements.txt"

echo
echo "Setup done. Start the app with:  python app.py"
echo "Then open http://localhost:5000 in your phone's browser."
