from datetime import date, datetime, timedelta

from fastapi import APIRouter, Depends
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from auth import verify_token
from database import get_db
from models import ExerciseRecord, FocusSession, Moment, PlayRecord, Todo, WaterRecord
from schemas import (
    ExerciseRecordResponse,
    FocusSessionResponse,
    MomentResponse,
    PlayRecordResponse,
)

router = APIRouter(prefix="/dashboard", tags=["dashboard"], dependencies=[Depends(verify_token)])


@router.get("/")
async def get_dashboard(db: AsyncSession = Depends(get_db)):
    today_start = datetime.combine(date.today(), datetime.min.time())
    tomorrow_start = today_start + timedelta(days=1)

    # --- Todo completion ---
    todo_total_result = await db.execute(select(func.count(Todo.id)))
    todo_total = todo_total_result.scalar() or 0
    todo_done_result = await db.execute(
        select(func.count(Todo.id)).where(Todo.done == True)  # noqa: E712
    )
    todo_done = todo_done_result.scalar() or 0
    completion_rate = round(todo_done / todo_total, 4) if todo_total > 0 else 0.0

    # --- Water total (today) ---
    water_result = await db.execute(
        select(WaterRecord).where(
            WaterRecord.recorded_at >= today_start,
            WaterRecord.recorded_at < tomorrow_start,
        )
    )
    water_records = water_result.scalars().all()
    water_total_ml = sum(r.amount_ml for r in water_records)

    # --- Exercise total (today) ---
    exercise_result = await db.execute(
        select(ExerciseRecord).where(
            ExerciseRecord.recorded_at >= today_start,
            ExerciseRecord.recorded_at < tomorrow_start,
        )
    )
    exercise_records = exercise_result.scalars().all()
    exercise_total_min = sum(r.duration_min for r in exercise_records)

    # --- Focus total + count (today) ---
    focus_result = await db.execute(
        select(FocusSession).where(
            FocusSession.started_at >= today_start,
            FocusSession.started_at < tomorrow_start,
        )
    )
    focus_sessions = focus_result.scalars().all()
    focus_total_min = sum(s.duration_min or 0 for s in focus_sessions)

    # --- Latest 3 moments ---
    moments_result = await db.execute(
        select(Moment).order_by(Moment.recorded_at.desc()).limit(3)
    )
    latest_moments = moments_result.scalars().all()

    # --- Latest playing record ---
    play_result = await db.execute(
        select(PlayRecord).order_by(PlayRecord.played_at.desc()).limit(1)
    )
    latest_play = play_result.scalar_one_or_none()

    return {
        "date": date.today().isoformat(),
        "todos": {
            "total": todo_total,
            "done": todo_done,
            "completion_rate": completion_rate,
        },
        "water": {
            "total_ml": water_total_ml,
            "count": len(water_records),
        },
        "exercise": {
            "total_duration_min": exercise_total_min,
            "count": len(exercise_records),
        },
        "focus": {
            "total_duration_min": focus_total_min,
            "count": len(focus_sessions),
        },
        "moments": [MomentResponse.model_validate(m) for m in latest_moments],
        "latest_play": PlayRecordResponse.model_validate(latest_play) if latest_play else None,
    }
