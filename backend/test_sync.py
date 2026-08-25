import os
import sys
import tempfile
import time
from pathlib import Path
from fastapi.testclient import TestClient

from main import app, SYNC_API_KEY, RECORDINGS_DIR, SUMMARIES_DIR
import database

client = TestClient(app)

def test_sync_flow():
    print("Testing database init...")
    database.init_db()

    # 1. Test unauthorized sync
    print("Testing unauthorized sync...")
    dummy_audio = b"\x00\x00\x00\x1cftypisom" + b"\x00" * 100
    res = client.post("/api/v1/sync", data={
        "title": "Unauthorized Test",
        "createdAt": int(time.time() * 1000),
        "durationSeconds": 30,
        "transcript": "Test",
        "summary": "Test"
    }, files={"audio": ("test.m4a", dummy_audio, "audio/mp4")})
    assert res.status_code == 401, f"Expected 401, got {res.status_code}"

    # 2. Test authorized sync
    print("Testing authorized sync...")
    now_ms = int(time.time() * 1000)
    res = client.post(
        "/api/v1/sync",
        headers={"Authorization": f"Bearer {SYNC_API_KEY}"},
        data={
            "title": "Quarterly Product Strategy",
            "createdAt": now_ms,
            "durationSeconds": 145,
            "transcript": "Speaker 1: Welcome everyone.\nSpeaker 2: Let's discuss Q3 roadmap.",
            "summary": "## Key Takeaways\n- Finalized Q3 roadmap\n- Launching mobile beta next week"
        },
        files={"audio": ("meeting.m4a", dummy_audio, "audio/mp4")}
    )
    assert res.status_code == 200, f"Sync failed: {res.text}"
    data = res.json()
    assert data["success"] is True
    meeting_id = data["meetingId"]
    audio_fn = data["audioFilename"]
    summary_fn = data["summaryFilename"]
    print(f"Sync successful! Meeting ID: {meeting_id}")
    print(f"Audio file saved: {audio_fn}")
    print(f"Summary file saved: {summary_fn}")

    # Check files on disk
    assert (RECORDINGS_DIR / audio_fn).exists(), "Audio file not found on disk"
    assert (SUMMARIES_DIR / summary_fn).exists(), "Summary JSON not found on disk"
    assert (SUMMARIES_DIR / f"{Path(summary_fn).stem}_summary.md").exists(), "Markdown summary not found on disk"

    # 3. Test Web Dashboard Login
    print("Testing Web Dashboard Login...")
    res_login = client.post("/login", data={"password": "admin123"}, follow_redirects=False)
    assert res_login.status_code == 303, f"Expected 303 redirect, got {res_login.status_code}"
    cookie = res_login.cookies.get("echominutes_session")
    assert cookie is not None, "Session cookie not set"

    # 4. Test Dashboard page with session
    res_dash = client.get("/dashboard", cookies={"echominutes_session": cookie})
    assert res_dash.status_code == 200
    assert "Quarterly Product Strategy" in res_dash.text

    print("All backend tests PASSED successfully! 🚀")

if __name__ == "__main__":
    test_sync_flow()
