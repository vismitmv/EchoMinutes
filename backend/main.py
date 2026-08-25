import os
import re
import json
import time
import datetime
from pathlib import Path
from typing import Optional

from fastapi import FastAPI, Request, Response, Form, File, UploadFile, Header, Depends, HTTPException, status
from fastapi.responses import HTMLResponse, JSONResponse, RedirectResponse, FileResponse, StreamingResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from itsdangerous import URLSafeTimedSerializer, BadSignature, SignatureExpired

import database

# Configurations
BASE_DIR = Path(__file__).resolve().parent
STORAGE_DIR = Path(os.environ.get("STORAGE_DIR", BASE_DIR / "storage"))
RECORDINGS_DIR = STORAGE_DIR / "recordings"
SUMMARIES_DIR = STORAGE_DIR / "summaries"

RECORDINGS_DIR.mkdir(parents=True, exist_ok=True)
SUMMARIES_DIR.mkdir(parents=True, exist_ok=True)

SYNC_API_KEY = os.environ.get("SYNC_API_KEY", "echominutes_secret_sync_key_2026")
DASHBOARD_PASSWORD = os.environ.get("DASHBOARD_PASSWORD", "admin123")
SECRET_KEY = os.environ.get("SECRET_KEY", "echominutes_super_secure_session_key_998877")

serializer = URLSafeTimedSerializer(SECRET_KEY)
SESSION_COOKIE = "echominutes_session"
SESSION_MAX_AGE = 30 * 24 * 3600  # 30 days

app = FastAPI(title="EchoMinutes Sync Server", version="1.1.0-beta")
templates = Jinja2Templates(directory=str(BASE_DIR / "templates"))

# Initialize database on startup
@app.on_event("startup")
def on_startup():
    database.init_db()

# Helper: sanitize titles for clean file naming
def sanitize_filename(name: str) -> str:
    cleaned = re.sub(r'[\\/*?:"<>| ]', '_', name)
    cleaned = re.sub(r'_+', '_', cleaned).strip('_')
    return cleaned[:50] if cleaned else "Meeting"

# Auth dependency for Dashboard
def get_current_user(request: Request) -> bool:
    cookie = request.cookies.get(SESSION_COOKIE)
    if not cookie:
        return False
    try:
        data = serializer.loads(cookie, max_age=SESSION_MAX_AGE)
        return data.get("authenticated", False)
    except (BadSignature, SignatureExpired):
        return False

# Auth dependency for Sync API
def verify_sync_key(authorization: Optional[str] = Header(None), x_sync_key: Optional[str] = Header(None)):
    token = None
    if authorization and authorization.startswith("Bearer "):
        token = authorization[7:].strip()
    elif x_sync_key:
        token = x_sync_key.strip()
    
    if not token or token != SYNC_API_KEY:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing Sync API Key"
        )
    return True

# ----------------- WEB DASHBOARD ROUTES ----------------- #

@app.get("/", response_class=HTMLResponse)
def index(request: Request):
    if get_current_user(request):
        return RedirectResponse(url="/dashboard", status_code=302)
    return RedirectResponse(url="/login", status_code=302)

@app.get("/login", response_class=HTMLResponse)
def login_page(request: Request):
    if get_current_user(request):
        return RedirectResponse(url="/dashboard", status_code=302)
    return templates.TemplateResponse(
        request=request,
        name="login.html",
        context={"error": None}
    )

@app.post("/login", response_class=HTMLResponse)
def login_submit(request: Request, password: str = Form(...)):
    if password == DASHBOARD_PASSWORD:
        token = serializer.dumps({"authenticated": True})
        response = RedirectResponse(url="/dashboard", status_code=303)
        response.set_cookie(
            key=SESSION_COOKIE,
            value=token,
            max_age=SESSION_MAX_AGE,
            httponly=True,
            samesite="lax",
            secure=request.url.scheme == "https"
        )
        return response
    return templates.TemplateResponse(
        request=request,
        name="login.html",
        context={"error": "Invalid password. Please try again."}
    )

@app.get("/logout")
def logout():
    response = RedirectResponse(url="/login", status_code=302)
    response.delete_cookie(SESSION_COOKIE)
    return response

@app.get("/dashboard", response_class=HTMLResponse)
def dashboard(request: Request):
    if not get_current_user(request):
        return RedirectResponse(url="/login", status_code=302)
    meetings = database.get_all_meetings()
    return templates.TemplateResponse(
        request=request,
        name="dashboard.html",
        context={
            "meetings": meetings,
            "total_meetings": len(meetings)
        }
    )

# ----------------- SYNC API ROUTES ----------------- #

@app.post("/api/v1/sync")
async def sync_meeting(
    title: str = Form(...),
    createdAt: int = Form(...),
    durationSeconds: int = Form(...),
    transcript: str = Form(...),
    summary: str = Form(...),
    audio: UploadFile = File(...),
    authorized: bool = Depends(verify_sync_key)
):
    try:
        # Convert timestamp to human-readable date for filename
        dt = datetime.datetime.fromtimestamp(createdAt / 1000.0)
        timestamp_str = dt.strftime("%Y-%m-%d_%H-%M-%S")
        safe_title = sanitize_filename(title)
        
        # Audio filename
        ext = Path(audio.filename or "recording.m4a").suffix or ".m4a"
        audio_filename = f"{timestamp_str}_{safe_title}{ext}"
        audio_path = RECORDINGS_DIR / audio_filename

        # Save audio file
        contents = await audio.read()
        with open(audio_path, "wb") as f:
            f.write(contents)

        # Summary and transcript JSON file
        summary_filename = f"{timestamp_str}_{safe_title}.json"
        summary_path = SUMMARIES_DIR / summary_filename
        
        # Markdown summary file for easy inspection
        md_filename = f"{timestamp_str}_{safe_title}_summary.md"
        md_path = SUMMARIES_DIR / md_filename

        metadata = {
            "title": title,
            "createdAt": createdAt,
            "createdDate": dt.isoformat(),
            "durationSeconds": durationSeconds,
            "audioFilename": audio_filename,
            "transcript": transcript,
            "summary": summary,
            "syncedAt": int(time.time() * 1000)
        }

        with open(summary_path, "w", encoding="utf-8") as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)

        with open(md_path, "w", encoding="utf-8") as f:
            f.write(f"# {title}\n\n")
            f.write(f"**Date**: {dt.strftime('%B %d, %Y at %I:%M %p')}\n")
            f.write(f"**Duration**: {durationSeconds} seconds\n\n")
            f.write(f"## Summary\n\n{summary}\n\n")
            f.write(f"## Verbatim Transcript\n\n{transcript}\n")

        # Save to SQLite
        meeting_id = database.save_meeting(
            title=title,
            created_at=createdAt,
            duration_seconds=durationSeconds,
            audio_filename=audio_filename,
            summary_filename=summary_filename,
            transcript=transcript,
            summary=summary,
            synced_at=metadata["syncedAt"]
        )

        return {
            "success": True,
            "meetingId": meeting_id,
            "audioFilename": audio_filename,
            "summaryFilename": summary_filename,
            "message": "Meeting synced successfully"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Sync error: {str(e)}")

@app.get("/api/v1/meetings")
def list_meetings(request: Request):
    if not get_current_user(request):
        raise HTTPException(status_code=401, detail="Unauthorized")
    return database.get_all_meetings()

@app.get("/api/v1/meetings/{meeting_id}")
def get_meeting(meeting_id: int, request: Request):
    if not get_current_user(request):
        raise HTTPException(status_code=401, detail="Unauthorized")
    meeting = database.get_meeting_by_id(meeting_id)
    if not meeting:
        raise HTTPException(status_code=404, detail="Meeting not found")
    return meeting

@app.delete("/api/v1/meetings/{meeting_id}")
def delete_meeting_endpoint(meeting_id: int, request: Request):
    if not get_current_user(request):
        raise HTTPException(status_code=401, detail="Unauthorized")
    meeting = database.delete_meeting(meeting_id)
    if not meeting:
        raise HTTPException(status_code=404, detail="Meeting not found")
    
    # Also delete files if they exist
    audio_path = RECORDINGS_DIR / meeting["audio_filename"]
    summary_path = SUMMARIES_DIR / meeting["summary_filename"]
    md_path = SUMMARIES_DIR / f"{Path(meeting['summary_filename']).stem}_summary.md"
    
    if audio_path.exists(): audio_path.unlink()
    if summary_path.exists(): summary_path.unlink()
    if md_path.exists(): md_path.unlink()

    return {"success": True, "message": "Meeting and associated files deleted"}

@app.get("/api/v1/recordings/{filename}")
def stream_audio(filename: str, request: Request):
    if not get_current_user(request):
        raise HTTPException(status_code=401, detail="Unauthorized")
    
    file_path = RECORDINGS_DIR / filename
    if not file_path.exists():
        raise HTTPException(status_code=404, detail="Audio file not found")

    return FileResponse(
        path=str(file_path),
        media_type="audio/mp4",
        filename=filename
    )
