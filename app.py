"""AI Study Master backend with free/open-source provider options.

Free hosted option (subject to provider availability/quotas):
  pip install flask requests
  AI_PROVIDER=huggingface HF_TOKEN=... python app.py

Free self-hosted option (requires a machine/server running Ollama):
  pip install flask requests
  AI_PROVIDER=ollama OLLAMA_URL=http://127.0.0.1:11434 OLLAMA_MODEL=qwen2.5:7b python app.py

No paid API is used by this backend. Hosted free tiers can have limits and may
require a free account/token. Keep tokens in server environment variables, not APK.
"""
import os
import requests
from flask import Flask, request, jsonify

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 64 * 1024

SYSTEM_PROMPT = (
    "You are a careful educational tutor. Create accurate, structured study notes. "
    "Do not invent facts; flag uncertainty. Use readable Markdown headings and lists. "
    "For History include chronology and important people/events; Geography include "
    "place-based examples and simple diagram descriptions; English include rules and "
    "examples; Reasoning include step-by-step solved examples. Include key terms, "
    "memory tips, revision summary, and 10 multiple-choice questions with answers "
    "and short explanations. If the requested language is Bengali, write in Bengali "
    "script (বাংলা), not transliteration. If bilingual, give both English and Bengali "
    "for each major concept. Finish with 3 prompts for relevant educational cartoon "
    "illustrations; do not claim images have been generated or embedded."
)


def make_prompt(data):
    topic = str(data.get("topic", "")).strip()[:180]
    subject = str(data.get("subject", "General")).strip()[:80]
    language = str(data.get("language", "English")).strip()[:60]
    detail = str(data.get("detail", "AI decides")).strip()[:60]
    if not topic:
        raise ValueError("topic is required")
    return topic, f"Subject: {subject}\nTopic: {topic}\nOutput language: {language}\nDetail level: {detail}\nCreate a thorough, exam-oriented study guide."


def generate_huggingface(user_prompt):
    token = os.environ.get("HF_TOKEN", "").strip()
    if not token:
        raise RuntimeError("HF_TOKEN is missing. Create a free Hugging Face account/token and set it on the server.")
    model = os.environ.get("HF_MODEL", "Qwen/Qwen2.5-7B-Instruct")
    url = "https://router.huggingface.co/v1/chat/completions"
    response = requests.post(url, headers={"Authorization": f"Bearer {token}"}, json={
        "model": model,
        "messages": [{"role": "system", "content": SYSTEM_PROMPT}, {"role": "user", "content": user_prompt}],
        "temperature": 0.4,
        "max_tokens": 5000
    }, timeout=120)
    if response.status_code >= 400:
        raise RuntimeError(f"Free hosted AI request failed ({response.status_code}). Free provider quotas/model availability may vary: {response.text[:400]}")
    payload = response.json()
    return payload["choices"][0]["message"]["content"]


def generate_ollama(user_prompt):
    base = os.environ.get("OLLAMA_URL", "http://127.0.0.1:11434").rstrip("/")
    model = os.environ.get("OLLAMA_MODEL", "qwen2.5:7b")
    response = requests.post(f"{base}/api/chat", json={
        "model": model,
        "messages": [{"role": "system", "content": SYSTEM_PROMPT}, {"role": "user", "content": user_prompt}],
        "stream": False,
        "options": {"temperature": 0.4}
    }, timeout=180)
    if response.status_code >= 400:
        raise RuntimeError(f"Ollama request failed ({response.status_code}): {response.text[:300]}")
    return response.json()["message"]["content"]


@app.get("/health")
def health():
    return jsonify({"ok": True, "provider": os.environ.get("AI_PROVIDER", "huggingface"), "mode": "free/open-source provider"})


@app.post("/generate")
def generate():
    try:
        data = request.get_json(silent=True) or {}
        _, user_prompt = make_prompt(data)
        provider = os.environ.get("AI_PROVIDER", "huggingface").lower().strip()
        if provider == "huggingface":
            content = generate_huggingface(user_prompt)
        elif provider == "ollama":
            content = generate_ollama(user_prompt)
        else:
            return jsonify({"error": "Unsupported provider. Use huggingface or ollama."}), 400
        return jsonify({"content": content, "provider": provider})
    except ValueError as exc:
        return jsonify({"error": str(exc)}), 400
    except requests.Timeout:
        return jsonify({"error": "AI provider timed out. Try again or reduce the requested detail level."}), 504
    except Exception as exc:
        app.logger.exception("Study guide generation failed")
        return jsonify({"error": "Free AI generation failed", "detail": str(exc)[:700]}), 502


if __name__ == "__main__":
    # For local development only. Deploy behind HTTPS for remote use.
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", "8000")), debug=False)
