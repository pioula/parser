from collections import defaultdict, deque
from datetime import datetime
from models.UserTagEvent import UserTagEvent
from models.Action import Action
from models.UserProfileResult import UserProfileResult
import pytz
import redis

utc=pytz.UTC
class UserTagEventStorage:
    def __init__(self):
        self.storage = defaultdict(lambda: deque(maxlen=200))
        self.max_events_per_cookie = 200

    def add_event(self, event: UserTagEvent):
        cookie = event.cookie
        self.storage[cookie].append(event)

    def query_events(self, cookie: str, time_range: str, limit: int = 200) -> UserProfileResult:
        if cookie not in self.storage:
            return UserProfileResult(cookie=cookie, views=[], buys=[])

        start_time, end_time = self._parse_time_range(time_range)
        start_time = start_time.replace(tzinfo=utc)
        end_time = end_time.replace(tzinfo=utc)
        events = self.storage[cookie]
        
        filtered_events = [
            event for event in events
            if start_time <= (event.time.replace(tzinfo=utc)) < end_time
        ]
        
        limited_events = filtered_events[:limit]
        views = [event for event in limited_events if event.action == Action.VIEW]
        buys = [event for event in limited_events if event.action == Action.BUY]
        
        return UserProfileResult(cookie=cookie, views=views, buys=buys)

    def _parse_time_range(self, time_range: str) -> tuple[datetime, datetime]:
        start_str, end_str = time_range.split('_')
        start_time = self._parse_datetime(start_str)
        end_time = self._parse_datetime(end_str)
        return start_time, end_time

    def _parse_datetime(self, dt_str: str) -> datetime:
        try:
            return datetime.strptime(dt_str, "%Y-%m-%dT%H:%M:%S.%f")
        except ValueError:
            return datetime.strptime(dt_str, "%Y-%m-%dT%H:%M:%S")