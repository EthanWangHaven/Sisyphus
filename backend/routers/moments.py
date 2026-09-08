import os
import uuid
from datetime import date, datetime

from fastapi import APIRouter, Depends, File, HTTPException, UploadFile
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from auth import verify_token
from database import get_db
from models import Moment
from schemas import MomentCreate, MomentResponse, MomentUpdate

router = APIRouter(prefix="/moments", tags=["moments"], dependencies=[Depends(verify_token)])

UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "uploads")


@router.post("/upload")
async def upload_image(file: UploadFile = File(...)):
    os.makedirs(UPLOAD_DIR, exist_ok=True)
    ext = os.path.splitext(file.filename)[1] if file.filename else ""
    filename = f"{uuid.uuid4().hex}{ext}"
    file_path = os.path.join(UPLOAD_DIR, filename)
    content = await file.read()
    with open(file_path, "wb") as f:
        f.write(content)
    return {"image_path": f"/uploads/{filename}"}


@router.get("/", response_model=list[MomentResponse])
async def list_moments(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Moment).order_by(Moment.recorded_at.desc()))
    return result.scalars().all()


@router.get("/{moment_id}", response_model=MomentResponse)
async def get_moment(moment_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Moment).where(Moment.id == moment_id))
    moment = result.scalar_one_or_none()
    if moment is None:
        raise HTTPException(status_code=404, detail="Moment not found")
    return moment


@router.post("/", response_model=MomentResponse)
async def create_moment(moment: MomentCreate, db: AsyncSession = Depends(get_db)):
    new_moment = Moment(**moment.model_dump(exclude_unset=True))
    db.add(new_moment)
    await db.commit()
    await db.refresh(new_moment)
    return new_moment


@router.put("/{moment_id}", response_model=MomentResponse)
async def update_moment(
    moment_id: int, moment: MomentUpdate, db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(Moment).where(Moment.id == moment_id))
    db_moment = result.scalar_one_or_none()
    if db_moment is None:
        raise HTTPException(status_code=404, detail="Moment not found")
    update_data = moment.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(db_moment, key, value)
    await db.commit()
    await db.refresh(db_moment)
    return db_moment


@router.delete("/{moment_id}")
async def delete_moment(moment_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Moment).where(Moment.id == moment_id))
    db_moment = result.scalar_one_or_none()
    if db_moment is None:
        raise HTTPException(status_code=404, detail="Moment not found")
    await db.delete(db_moment)
    await db.commit()
    return {"detail": "Moment deleted"}
