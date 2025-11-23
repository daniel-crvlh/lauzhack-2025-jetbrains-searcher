from typing import Optional
from fastapi import FastAPI, Query
from pydantic import BaseModel
from together import Together
import uvicorn

app = FastAPI(title="Simple Callable API")


class PredictRequest(BaseModel):
    code: Optional[str] = None
    error: Optional[str] = None
    lineNb: int
    language: str
    function: str


class PredictResponse(BaseModel):
    code: str
    explanation : str

@app.get("/")
def read_root():
    return {"status": "ok", "message": "API is running"}

@app.post("/predict", response_model=PredictResponse)
def predictPost(req: PredictRequest):
    client = Together()

    completion = client.chat.completions.create(
    # model="openai/gpt-oss-20b",
    model="Qwen/Qwen3-Coder-480B-A35B-Instruct-FP8",
    messages=[{"role":"system", f"content":"You're an expert in {req.language} I need you to {req.fuction} the following code that is in {req.language} only answer with {req.language} code no explanation"},{"role": "user", "content": f"{req.code} "}],
    )
    return {"code": completion.choices[0].message.content, "explanation": ""}

if __name__ == "__main__":
    uvicorn.run("main:app", host="127.0.0.1", port=8000, log_level="info", reload=True)