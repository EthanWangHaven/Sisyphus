import json
import os

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

security = HTTPBearer()


def get_token() -> str:
    token = os.environ.get("HAVEN_TOKEN")
    if token:
        return token
    config_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "config.json")
    if os.path.exists(config_path):
        try:
            with open(config_path, "r", encoding="utf-8") as f:
                config = json.load(f)
                return config.get("token", "haven-secret-token")
        except (json.JSONDecodeError, OSError):
            pass
    return "haven-secret-token"


async def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)) -> str:
    token = get_token()
    if credentials.credentials != token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return credentials.credentials
