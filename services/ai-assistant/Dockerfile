FROM python:3.14.5-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .
RUN addgroup --system appuser && adduser --system --ingroup appuser appuser && chown -R appuser:appuser /app
USER appuser

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]