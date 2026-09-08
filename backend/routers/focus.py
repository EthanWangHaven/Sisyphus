from datetime import date, datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from auth import verify_token
from database import get_db
from models import FocusSession
from schemas import FocusSessionCreate, FocusSessionResponse, FocusSessionUpdate

router = APIRouter(prefix="/focus", tags=["focus"], dependencies=[Depends(verify_token)])


@router.get("/stats/daily")
async def focus_daily_stats(
    target_date: date = Query(default=None), db: AsyncSession = Depends(get_db)
):
    if target_date is None:
        target_date = date.today()
    day_start = datetime.combine(target_date, datetime.min.time())
    day_end = day_start + timedelta(days=1)
    result = await db.execute(
        select(FocusSession).where(
            FocusSession.started_at >= day_start,
            FocusSession.started_at < day_end,
        ).order_by(FocusSession.started_at.desc())
    )
    sessions = result.scalars().all()
    total_duration_min = sum(s.duration_min or 0 for s in sessions)
    return {
        "date": target_date.isoformat(),
        "total_duration_min": total_duration_min,
        "count": len(sessions),
        "sessions": [FocusSessionResponse.model_validate(s) for s in sessions],
    }


@router.get("/stats/weekly")
async def focus_weekly_stats(db: AsyncSession = Depends(get_db)):
    today_start = datetime.combine(date.today(), datetime.min.time())
    week_ago = today_start - timedelta(days=7)
    result = await db.execute(
        select(FocusSession).where(
            FocusSession.started_at >= week_ago,
            FocusSession.started_at < today_start + timedelta(days=1),
        ).order_by(FocusSession.started_at.desc())
    )
    sessions = result.scalars().all()
    total_duration_min = sum(s.duration_min or 0 for s in sessions)
    daily = {}
    for s in sessions:
        day = s.started_at.date().isoformat()
        daily[day] = daily.get(day, 0) + (s.duration_min or 0)
    return {
        "start_date": week_ago.date().isoformat(),
        "end_date": date.today().isoformat(),
        "total_duration_min": total_duration_min,
        "total_count": len(sessions),
        "daily": daily,
    }


@router.get("/", response_model=list[FocusSessionResponse])
async def list_focus_sessions(db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(FocusSession).order_by(FocusSession.started_at.desc())
    )
    return result.scalars().all()


@router.get("/{session_id}", response_model=FocusSessionResponse)
async def get_focus_session(session_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(FocusSession).where(FocusSession.id == session_id)
    )
    session = result.scalar_one_or_none()
    if session is None:
        raise HTTPException(status_code=404, detail="Focus session not found")
    return session


@router.post("/", response_model=FocusSessionResponse)
async def create_focus_session(
    session: FocusSessionCreate, db: AsyncSession = Depends(get_db)
):
    new_session = FocusSession(**session.model_dump(exclude_unset=True))
    db.add(new_session)
    await db.commit()
    await db.refresh(new_session)
    return new_session


@router.put("/{session_id}", response_model=FocusSessionResponse)
async def update_focus_session(
    session_id: int, session: FocusSessionUpdate, db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(FocusSession).where(FocusSession.id == session_id)
    )
    db_session = result.scalar_one_or_none()
    if db_session is None:
        raise HTTPException(status_code=404, detail="Focus session not found")
    update_data = session.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(db_session, key, value)
    await db.commit()
    await db.refresh(db_session)
    return db_session


@router.delete("/{session_id}")
async def delete_focus_session(session_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(FocusSession).where(FocusSession.id == session_id)
    )
    db_session = result.scalar_one_or_none()
    if db_session is None:
        raise HTTPException(status_code=404, detail="Focus session not found")
    await db.delete(db_session)
    await db.commit()
    return {"detail": "Focus session deleted"}
