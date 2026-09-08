from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from database import Base, engine
from routers import dashboard, exercise, focus, moments, notes, plans, play, todos, water


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield


app = FastAPI(title="Haven API", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(notes.router, prefix="/api")
app.include_router(todos.router, prefix="/api")
app.include_router(plans.router, prefix="/api")
app.include_router(water.router, prefix="/api")
app.include_router(exercise.router, prefix="/api")
app.include_router(moments.router, prefix="/api")
app.include_router(focus.router, prefix="/api")
app.include_router(play.router, prefix="/api")
app.include_router(dashboard.router, prefix="/api")


@app.get("/")
async def root():
    return {"status": "ok"}
