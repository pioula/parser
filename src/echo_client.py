from fastapi import FastAPI, Path, Query, Body, Response
from typing import Optional
from pydantic import BaseModel
from models.Action import Action
from models.Aggregate import Aggregate
from models.AggregatesQueryResult import AggregatesQueryResult
from models.UserProfileResult import UserProfileResult
from models.UserTagEvent import UserTagEvent

app = FastAPI()

@app.post("/user_tags")
async def add_user_tag(user_tag: Optional[UserTagEvent] = Body(None)):
    return Response(status_code=204)

@app.post("/user_profiles/{cookie}")
async def get_user_profile(
    cookie: str = Path(...),
    time_range: str = Query(...),
    limit: int = Query(200),
    expected_result: UserProfileResult = Body(...)
) -> UserProfileResult:
    return expected_result

@app.post("/aggregates")
async def get_aggregates(
    time_range: str = Query(...),
    action: Action = Query(...),
    aggregates: list[Aggregate] = Query(...),
    origin: Optional[str] = Query(None),
    brand_id: Optional[str] = Query(None),
    category_id: Optional[str] = Query(None),
    expected_result: AggregatesQueryResult = Body(...)
) -> AggregatesQueryResult:
    return expected_result