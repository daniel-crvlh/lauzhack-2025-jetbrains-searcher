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
    shortDescription: str

@app.get("/")
def read_root():
    return {"status": "ok", "message": "API is running"}

@app.post("/predict", response_model=PredictResponse)
def predictPost(req: PredictRequest):
    client = Together()

    completion = client.chat.completions.create(
    # model="openai/gpt-oss-20b",
    model="Qwen/Qwen3-Coder-480B-A35B-Instruct-FP8",
    messages=[{"role":"system", "content":f"You're an expert in {req.language} I need you to read the following code that is in {req.language} only answer with {req.language} code no explanation and fix the {req.function} error"},{"role": "user", "content": f"code:{req.code} --- error: {req.error} "}],
    )

    shortDescriptionRes = client.chat.completions.create(
    model="openai/gpt-oss-20b",
    messages=[{"role":"system", "content":f"You're an expert in {req.language} given the {req.function} error and {req.language} code, explain why the following solution is the best, answer with a short answer at most 1 sentences"},{"role": "user", "content": f"code:{req.code},  error:{req.error}, solution:{completion.choices[0].message.content}"}],
    )
    explanationRes = client.chat.completions.create(
    model="openai/gpt-oss-20b",
    messages=[{"role":"system", "content":f"You're an expert in {req.language} given the {req.function} error and {req.language} code, explain why the following solution is the best, try to explain shortly, you can extend if necessary"},{"role": "user", "content": f"code:{req.code},  error:{req.error}, solution:{completion.choices[0].message.content}"}],
    )

    return {"code": completion.choices[0].message.content, "explanation": explanationRes.choices[0].message.content, "shortDescription": shortDescriptionRes.choices[0].message.content}

if __name__ == "__main__":
    uvicorn.run("main:app", host="127.0.0.1", port=8000, log_level="info", reload=True)