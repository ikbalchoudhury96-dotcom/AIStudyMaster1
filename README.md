# AI Study Master — Android source project (free-only direction)

An Android starter app for generating study-guide PDFs in English, Bengali (বাংলা), or bilingual format. Subjects include History, Geography, English, Reasoning, Science, Mathematics, Polity, and user-entered topics.

## Free-only AI options

This project does not require a paid OpenAI API. The backend supports:

1. **Hugging Face hosted inference** — may have free usage, but requires a free account/token and availability, model access, quotas, and rate limits can change. It is not guaranteed to be unlimited or always available.
2. **Ollama with an open model** — no per-request API fee, but requires a computer/server with enough memory and a model downloaded. It is not a phone-only hosting solution for most users.

No AI provider token should ever be placed inside the APK. Set it as a server environment variable.

## Run the backend

Python 3.10+ recommended:

```bash
pip install flask requests
AI_PROVIDER=huggingface HF_TOKEN=your_free_huggingface_token python app.py
```

Optional environment variables:
- `HF_MODEL` (default `Qwen/Qwen2.5-7B-Instruct`)
- `AI_PROVIDER=ollama`
- `OLLAMA_URL` (default `http://127.0.0.1:11434`)
- `OLLAMA_MODEL` (default `qwen2.5:7b`)
- `PORT` (default `8000`)

Check `GET /health`; Android sends `POST /generate` with JSON fields `topic`, `subject`, `language`, and `detail`. Deploy the backend behind HTTPS before using it remotely. Add authentication, rate limiting, request quotas, and abuse protection before public release.

## Android build

This ZIP contains **source code, not an installable APK**. To build, use Android Studio with a compatible Android SDK and JDK. Open the `AIStudyMaster` folder, sync Gradle, then choose **Build > Build APK(s)**. A successful debug APK is typically created at `app/build/outputs/apk/debug/app-debug.apk`.

## Current limitations

- The Android app can call a configured `/generate` HTTPS endpoint and turn the returned text into a PDF.
- The PDF currently uses a basic text layout; proper Bengali font embedding, cartoon image generation/embedding, and full on-device rendering validation are not finished.
- Free hosted inference is quota-limited and provider/model availability may change. A truly unlimited, zero-cost, reliable hosted AI service cannot be promised.
- This project has not been built or tested into a working APK in this environment.

## Build APK using only an Android phone (cloud build)

A GitHub account is needed; the cloud build uses GitHub Actions' available free quota, which is subject to GitHub limits.

1. Extract this ZIP on the phone using a file manager that supports ZIP files.
2. In a mobile browser, create a new repository on GitHub.
3. Upload the **contents** of the `AIStudyMaster` folder to the repository root (including the hidden `.github` folder; if the mobile upload UI cannot upload hidden folders, use GitHub's web editor to create `.github/workflows/android.yml` with the workflow in this project).
4. Open the repository's **Actions** tab and select **Build AI Study Master APK**. Tap **Run workflow** if it has not started automatically.
5. When the run succeeds, open that run, find **Artifacts**, and download `AIStudyMaster-debug-apk.zip`. Extract it and install `app-debug.apk`. Android may ask you to allow installs from your browser/file manager.

This workflow builds a debug APK only. It does not deploy the Python AI backend. AI generation requires a deployed HTTPS backend endpoint configured in the app; without that, the app can build but live AI generation will not work. The app is not yet production-tested, and Bengali font embedding/cartoon illustration generation remain incomplete.
