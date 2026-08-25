import sqlite3
import os
from typing import List, Optional, Dict, Any

DB_PATH = os.environ.get("DB_PATH", os.path.join(os.path.dirname(__file__), "storage", "echominutes.db"))

def get_db():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    with get_db() as conn:
        conn.execute("""
            CREATE TABLE IF NOT EXISTS meetings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                duration_seconds INTEGER NOT NULL,
                audio_filename TEXT NOT NULL,
                summary_filename TEXT NOT NULL,
                transcript TEXT NOT NULL,
                summary TEXT NOT NULL,
                synced_at INTEGER NOT NULL
            )
        """)
        conn.commit()

def save_meeting(title: str, created_at: int, duration_seconds: int,
                 audio_filename: str, summary_filename: str,
                 transcript: str, summary: str, synced_at: int) -> int:
    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO meetings (
                title, created_at, duration_seconds,
                audio_filename, summary_filename,
                transcript, summary, synced_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, (title, created_at, duration_seconds, audio_filename, summary_filename, transcript, summary, synced_at))
        conn.commit()
        return cursor.lastrowid

def get_all_meetings() -> List[Dict[str, Any]]:
    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM meetings ORDER BY created_at DESC")
        rows = cursor.fetchall()
        return [dict(row) for row in rows]

def get_meeting_by_id(meeting_id: int) -> Optional[Dict[str, Any]]:
    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM meetings WHERE id = ?", (meeting_id,))
        row = cursor.fetchone()
        return dict(row) if row else None

def delete_meeting(meeting_id: int) -> Optional[Dict[str, Any]]:
    meeting = get_meeting_by_id(meeting_id)
    if meeting:
        with get_db() as conn:
            conn.execute("DELETE FROM meetings WHERE id = ?", (meeting_id,))
            conn.commit()
    return meeting
