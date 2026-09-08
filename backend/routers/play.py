from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from auth import verify_token
from database import get_db
from models import PlayRecord
from schemas import PlayRecordCreate, PlayRecordResponse, PlayRecordUpdate

router = APIRouter(prefix="/play", tags=["play"], dependencies=[Depends(verify_token)])


@router.get("/", response_model=list[PlayRecordResponse])
async def list_play_records(db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(PlayRecord).order_by(PlayRecord.played_at.desc())
    )
    return result.scalars().all()


@router.get("/{record_id}", response_model=PlayRecordResponse)
async def get_play_record(record_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(PlayRecord).where(PlayRecord.id == record_id))
    record = result.scalar_one_or_none()
    if record is None:
        raise HTTPException(status_code=404, detail="Play record not found")
    return record


@router.post("/", response_model=PlayRecordResponse)
async def create_play_record(
    record: PlayRecordCreate, db: AsyncSession = Depends(get_db)
):
    new_record = PlayRecord(**record.model_dump(exclude_unset=True))
    db.add(new_record)
    await db.commit()
    await db.refresh(new_record)
    return new_record


@router.put("/{record_id}", response_model=PlayRecordResponse)
async def update_play_record(
    record_id: int, record: PlayRecordUpdate, db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(PlayRecord).where(PlayRecord.id == record_id))
    db_record = result.scalar_one_or_none()
    if db_record is None:
        raise HTTPException(status_code=404, detail="Play record not found")
    update_data = record.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(db_record, key, value)
    await db.commit()
    await db.refresh(db_record)
    return db_record


@router.delete("/{record_id}")
async def delete_play_record(record_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(PlayRecord).where(PlayRecord.id == record_id))
    db_record = result.scalar_one_or_none()
    if db_record is None:
        raise HTTPException(status_code=404, detail="Play record not found")
    await db.delete(db_record)
    await db.commit()
    return {"detail": "Play record deleted"}
