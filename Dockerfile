# Use Python 3.10 slim image as the base
FROM python:3.10-slim

# Set working directory in the container
WORKDIR /app

# Copy the requirements file into the container
COPY requirements.txt .

# Install the Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy the project files into the container
COPY . .

# Set the command to run your application
CMD ["python3", "src/main.py"]