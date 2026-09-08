from datetime import date, datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict


# ---------- Note ----------
class NoteCreate(BaseModel):
    title: str
    content: str = ""
    archived: bool = False


class NoteUpdate(BaseModel):
    title: Optional[str] = None
    content: Optional[str] = None
    archived: Optional[bool] = None


class NoteResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    content: str
    created_at: datetime
    updated_at: datetime
    archived: bool


# ---------- Todo ----------
class TodoCreate(BaseModel):
    title: str
    done: bool = False
    priority: str = "medium"
    category: str = ""
    due_date: Optional[date] = None


class TodoUpdate(BaseModel):
    title: Optional[str] = None
    done: Optional[bool] = None
    priority: Optional[str] = None
    category: Optional[str] = None
    due_date: Optional[date] = None


class TodoResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    done: bool
    priority: str
    category: str
    due_date: Optional[date] = None
    created_at: datetime
    updated_at: datetime


# ---------- Plan ----------
class PlanCreate(BaseModel):
    title: str
    description: str = ""
    date: Optional[date] = None
    completed: bool = False


class PlanUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    date: Optional[date] = None
    completed: Optional[bool] = None


class PlanResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    description: str
    date: Optional[date] = None
    completed: bool
    created_at: datetime
    updated_at: datetime


# ---------- WaterRecord ----------
class WaterRecordCreate(BaseModel):
    amount_ml: int
    recorded_at: Optional[datetime] = None


class WaterRecordUpdate(BaseModel):
    amount_ml: Optional[int] = None
    recorded_at: Optional[datetime] = None


class WaterRecordResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    amount_ml: int
    recorded_at: datetime


# ---------- ExerciseRecord ----------
class ExerciseRecordCreate(BaseModel):
    exercise_type: str
    duration_min: int
    note: str = ""
    recorded_at: Optional[datetime] = None


class ExerciseRecordUpdate(BaseModel):
    exercise_type: Optional[str] = None
    duration_min: Optional[int] = None
    note: Optional[str] = None
    recorded_at: Optional[datetime] = None


class ExerciseRecordResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    exercise_type: str
    duration_min: int
    note: str
    recorded_at: datetime


# ---------- Moment ----------
class MomentCreate(BaseModel):
    content: str
    image_path: Optional[str] = None
    mood: Optional[str] = None
    recorded_at: Optional[datetime] = None


class MomentUpdate(BaseModel):
    content: Optional[str] = None
    image_path: Optional[str] = None
    mood: Optional[str] = None
    recorded_at: Optional[datetime] = None


class MomentResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    content: str
    image_path: Optional[str] = None
    mood: Optional[str] = None
    recorded_at: datetime


# ---------- FocusSession ----------
class FocusSessionCreate(BaseModel):
    started_at: Optional[datetime] = None
    ended_at: Optional[datetime] = None
    duration_min: Optional[int] = None
    label: Optional[str] = None


class FocusSessionUpdate(BaseModel):
    started_at: Optional[datetime] = None
    ended_at: Optional[datetime] = None
    duration_min: Optional[int] = None
    label: Optional[str] = None


class FocusSessionResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    started_at: datetime
    ended_at: Optional[datetime] = None
    duration_min: Optional[int] = None
    label: Optional[str] = None


# ---------- PlayRecord ----------
class PlayRecordCreate(BaseModel):
    song_id: str
    song_title: str
    artist: str
    played_at: Optional[datetime] = None
    duration_sec: int = 0


class PlayRecordUpdate(BaseModel):
    song_id: Optional[str] = None
    song_title: Optional[str] = None
    artist: Optional[str] = None
    played_at: Optional[datetime] = None
    duration_sec: Optional[int] = None


class PlayRecordResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    song_id: str
    song_title: str
    artist: str
    played_at: datetime
    duration_sec: int
