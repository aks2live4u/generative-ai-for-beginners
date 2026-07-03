from fastapi import APIRouter, Depends

from app.models import User
from app.schemas import SalaryInsightOut, SalaryInsightRequest
from app.security import get_current_user
from app.services.ai_service import ai_service
from app.services.salary_service import estimate_salary

router = APIRouter(prefix="/api/salary", tags=["salary"])


@router.post("/insight", response_model=SalaryInsightOut)
def salary_insight(payload: SalaryInsightRequest, current_user: User = Depends(get_current_user)):
    market = estimate_salary(payload.title, payload.country)
    location = ", ".join(filter(None, [payload.city, payload.country])) or "global remote"

    commentary = f"Market demand for {payload.title} roles is {market['market_demand']}."
    if ai_service.is_available:
        try:
            commentary = ai_service.complete(
                system_prompt="In 2 sentences, comment on market demand and negotiation leverage for this role/location.",
                user_prompt=f"Title: {payload.title}\nLocation: {location}\nMarket data: {market}",
            )
        except Exception:
            pass

    return SalaryInsightOut(
        title=payload.title,
        location=location,
        estimated_min=market["estimated_min"],
        estimated_median=market["estimated_median"],
        estimated_max=market["estimated_max"],
        currency=market["currency"],
        market_demand=market["market_demand"],
        commentary=commentary,
    )
