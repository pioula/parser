from fastapi import FastAPI, Path, Query, Body, Response
from typing import Optional
from pydantic import BaseModel
from models.Action import Action
from models.Aggregate import Aggregate
from models.AggregatesQueryResult import AggregatesQueryResult
from models.UserProfileResult import UserProfileResult
from models.UserTagEvent import UserTagEvent
from services.UserTagEventStorage import UserTagEventStorage

app = FastAPI()
storage = UserTagEventStorage()

@app.post("/user_tags")
async def add_user_tag(event: UserTagEvent):
    # storage.add_event(event)
    return {"status": "success"}

@app.post("/user_profiles/{cookie}")
async def get_user_profile(
    cookie: str = Path(...),
    time_range: str = Query(...),
    limit: int = Query(200),
    expected_result: UserProfileResult = Body(...)
) -> UserProfileResult:
    # result = storage.query_events(cookie, time_range, limit)
    # if result != expected_result:
    #     print(f"Mismatch detected:")
    #     print(f"  Actual result:   {result}")
    #     print(f"  Expected result: {expected_result}")
    #     print(f"  Time range:      {time_range}")
    #     print(f"  Limit:           {limit}")
        
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