from datetime import date, datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from auth import verify_token
from database import get_db
from models import ExerciseRecord
from schemas import ExerciseRecordCreate, ExerciseRecordResponse, ExerciseRecordUpdate

router = APIRouter(prefix="/exercise", tags=["exercise"], dependencies=[Depends(verify_token)])


@router.get("/summary/today")
async def exercise_today_summary(db: AsyncSession = Depends(get_db)):
    today_start = datetime.combine(date.today(), datetime.min.time())
    tomorrow_start = today_start + timedelta(days=1)
    result = await db.execute(
        select(ExerciseRecord).where(
            ExerciseRecord.recorded_at >= today_start,
            ExerciseRecord.recorded_at < tomorrow_start,
        )
    )
    records = result.scalars().all()
    total_duration_min = sum(r.duration_min for r in records)
    return {
        "date": date.today().isoformat(),
        "total_duration_min": total_duration_min,
        "count": len(records),
        "records": [ExerciseRecordResponse.model_validate(r) for r in records],
    }


@router.get("/summary/week")
async def exercise_week_summary(db: AsyncSession = Depends(get_db)):
    today_start = datetime.combine(date.today(), datetime.min.time())
    week_ago = today_start - timedelta(days=7)
    result = await db.execute(
        select(ExerciseRecord).where(
            ExerciseRecord.recorded_at >= week_ago,
            ExerciseRecord.recorded_at < today_start + timedelta(days=1),
        ).order_by(ExerciseRecord.recorded_at.desc())
    )
    records = result.scalars().all()
    total_duration_min = sum(r.duration_min for r in records)
    daily = {}
    for r in records:
        day = r.recorded_at.date().isoformat()
        daily[day] = daily.get(day, 0) + r.duration_min
    return {
        "start_date": week_ago.date().isoformat(),
        "end_date": date.today().isoformat(),
        "total_duration_min": total_duration_min,
        "count": len(records),
        "daily": daily,
    }


@router.get("/", response_model=list[ExerciseRecordResponse])
async def list_exercise_records(db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(ExerciseRecord).order_by(ExerciseRecord.recorded_at.desc())
    )
    return result.scalars().all()


@router.get("/{record_id}", response_model=ExerciseRecordResponse)
async def get_exercise_record(record_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(ExerciseRecord).where(ExerciseRecord.id == record_id)
    )
    record = result.scalar_one_or_none()
    if record is None:
        raise HTTPException(status_code=404, detail="Exercise record not found")
    return record


@router.post("/", response_model=ExerciseRecordResponse)
async def create_exercise_record(
    record: ExerciseRecordCreate, db: AsyncSession = Depends(get_db)
):
    new_record = ExerciseRecord(**record.model_dump(exclude_unset=True))
    db.add(new_record)
    await db.commit()
    await db.refresh(new_record)
    return new_record


@router.put("/{record_id}", response_model=ExerciseRecordResponse)
async def update_exercise_record(
    record_id: int, record: ExerciseRecordUpdate, db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(ExerciseRecord).where(ExerciseRecord.id == record_id)
    )
    db_record = result.scalar_one_or_none()
    if db_record is None:
        raise HTTPException(status_code=404, detail="Exercise record not found")
    update_data = record.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(db_record, key, value)
    await db.commit()
    await db.refresh(db_record)
    return db_record


@router.delete("/{record_id}")
async def delete_exercise_record(record_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(ExerciseRecord).where(ExerciseRecord.id == record_id)
    )
    db_record = result.scalar_one_or_none()
    if db_record is None:
        raise HTTPException(status_code=404, detail="Exercise record not found")
    await db.delete(db_record)
    await db.commit()
    return {"detail": "Exercise record deleted"}
