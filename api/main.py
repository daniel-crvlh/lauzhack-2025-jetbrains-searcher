from typing import Optional
from fastapi import FastAPI, Query
from pydantic import BaseModel
from together import Together
import uvicorn

# Install dependencies: pip install fastapi uvicorn

# TOGETHER_API_KEY = "tgp_v1_AG0Kne-Rpt_OEmiCsHtWHENze5YXCeQiZoG2EB6n_pI"


app = FastAPI(title="Simple Callable API")


class PredictRequest(BaseModel):
    code: Optional[str] = None
    error: Optional[str] = None
    lineNb: int
    language: str
    function: str


class PredictResponse(BaseModel):
    data: str

@app.get("/")
def read_root():
    return {"status": "ok", "message": "API is running"}

# @app.post("/predict", response_model=PredictResponse)
# def predict(req: PredictRequest):
#     client = Together()

#     completion = client.chat.completions.create(
#     model="openai/gpt-oss-20b",
#     messages=[{"role": "user", "content": "What are the top 3 things to do in New York?"}],
#     )

#     print(completion.choices[0].message.content)

@app.post("/predict", response_model=PredictResponse)
def predictPost(req: PredictRequest):
    client = Together()

    clientMessage = ""

    print(req)


    completion = client.chat.completions.create(
    # model="openai/gpt-oss-20b",
    model="Qwen/Qwen3-Coder-480B-A35B-Instruct-FP8",
    messages=[{"role":"system", f"content":"You're an expert in {req.language} I need you to {req.fuction} the following code that is in {req.language} only answer with {req.language} code no explanation"},{"role": "user", "content": f"{req.code} "}],
    )
    return {"data": completion.choices[0].message.content}

if __name__ == "__main__":
    uvicorn.run("main:app", host="127.0.0.1", port=8000, log_level="info", reload=True)