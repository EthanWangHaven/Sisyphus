from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from auth import verify_token
from database import get_db
from models import Plan
from schemas import PlanCreate, PlanResponse, PlanUpdate

router = APIRouter(prefix="/plans", tags=["plans"], dependencies=[Depends(verify_token)])


@router.get("/", response_model=list[PlanResponse])
async def list_plans(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Plan).order_by(Plan.created_at.desc()))
    return result.scalars().all()


@router.get("/{plan_id}", response_model=PlanResponse)
async def get_plan(plan_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Plan).where(Plan.id == plan_id))
    plan = result.scalar_one_or_none()
    if plan is None:
        raise HTTPException(status_code=404, detail="Plan not found")
    return plan


@router.post("/", response_model=PlanResponse)
async def create_plan(plan: PlanCreate, db: AsyncSession = Depends(get_db)):
    new_plan = Plan(**plan.model_dump(exclude_unset=True))
    db.add(new_plan)
    await db.commit()
    await db.refresh(new_plan)
    return new_plan


@router.put("/{plan_id}", response_model=PlanResponse)
async def update_plan(plan_id: int, plan: PlanUpdate, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Plan).where(Plan.id == plan_id))
    db_plan = result.scalar_one_or_none()
    if db_plan is None:
        raise HTTPException(status_code=404, detail="Plan not found")
    update_data = plan.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(db_plan, key, value)
    await db.commit()
    await db.refresh(db_plan)
    return db_plan


@router.delete("/{plan_id}")
async def delete_plan(plan_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Plan).where(Plan.id == plan_id))
    db_plan = result.scalar_one_or_none()
    if db_plan is None:
        raise HTTPException(status_code=404, detail="Plan not found")
    await db.delete(db_plan)
    await db.commit()
    return {"detail": "Plan deleted"}
