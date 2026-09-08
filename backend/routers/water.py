from datetime import date, datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from auth import verify_token
from database import get_db
from models import WaterRecord
from schemas import WaterRecordCreate, WaterRecordResponse, WaterRecordUpdate

router = APIRouter(prefix="/water", tags=["water"], dependencies=[Depends(verify_token)])


@router.get("/summary/today")
async def water_today_summary(db: AsyncSession = Depends(get_db)):
    today_start = datetime.combine(date.today(), datetime.min.time())
    tomorrow_start = today_start + timedelta(days=1)
    result = await db.execute(
        select(WaterRecord).where(
            WaterRecord.recorded_at >= today_start,
            WaterRecord.recorded_at < tomorrow_start,
        )
    )
    records = result.scalars().all()
    total_ml = sum(r.amount_ml for r in records)
    return {
        "date": date.today().isoformat(),
        "total_ml": total_ml,
        "count": len(records),
        "records": [WaterRecordResponse.model_validate(r) for r in records],
    }


@router.get("/summary/week")
async def water_week_summary(db: AsyncSession = Depends(get_db)):
    today_start = datetime.combine(date.today(), datetime.min.time())
    week_ago = today_start - timedelta(days=7)
    result = await db.execute(
        select(WaterRecord).where(
            WaterRecord.recorded_at >= week_ago,
            WaterRecord.recorded_at < today_start + timedelta(days=1),
        ).order_by(WaterRecord.recorded_at.desc())
    )
    records = result.scalars().all()
    total_ml = sum(r.amount_ml for r in records)
    daily = {}
    for r in records:
        day = r.recorded_at.date().isoformat()
        daily[day] = daily.get(day, 0) + r.amount_ml
    return {
        "start_date": week_ago.date().isoformat(),
        "end_date": date.today().isoformat(),
        "total_ml": total_ml,
        "count": len(records),
        "daily": daily,
    }


@router.get("/", response_model=list[WaterRecordResponse])
async def list_water_records(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(WaterRecord).order_by(WaterRecord.recorded_at.desc()))
    return result.scalars().all()


@router.get("/{record_id}", response_model=WaterRecordResponse)
async def get_water_record(record_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(WaterRecord).where(WaterRecord.id == record_id))
    record = result.scalar_one_or_none()
    if record is None:
        raise HTTPException(status_code=404, detail="Water record not found")
    return record


@router.post("/", response_model=WaterRecordResponse)
async def create_water_record(record: WaterRecordCreate, db: AsyncSession = Depends(get_db)):
    new_record = WaterRecord(**record.model_dump(exclude_unset=True))
    db.add(new_record)
    await db.commit()
    await db.refresh(new_record)
    return new_record


@router.put("/{record_id}", response_model=WaterRecordResponse)
async def update_water_record(
    record_id: int, record: WaterRecordUpdate, db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(WaterRecord).where(WaterRecord.id == record_id))
    db_record = result.scalar_one_or_none()
    if db_record is None:
        raise HTTPException(status_code=404, detail="Water record not found")
    update_data = record.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(db_record, key, value)
    await db.commit()
    await db.refresh(db_record)
    return db_record


@router.delete("/{record_id}")
async def delete_water_record(record_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(WaterRecord).where(WaterRecord.id == record_id))
    db_record = result.scalar_one_or_none()
    if db_record is None:
        raise HTTPException(status_code=404, detail="Water record not found")
    await db.delete(db_record)
    await db.commit()
    return {"detail": "Water record deleted"}
