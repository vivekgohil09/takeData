# gpt_sql.py
import sys
from gpt4all import GPT4All

model = GPT4All("mistral-7b-instruct")  # Ensure model is downloaded
prompt = sys.argv[1]
sql = model.generate(f"Convert this to SQL: {prompt}", max_tokens=100)
print(sql.strip())
