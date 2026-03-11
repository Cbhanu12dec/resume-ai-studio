#!/bin/bash
echo "Initializing LocalStack resources..."

# Create S3 buckets
awslocal s3 mb s3://resumeai-uploads
awslocal s3 mb s3://resumeai-pdfs
awslocal s3 mb s3://resumeai-latex

# Create SQS queues
awslocal sqs create-queue --queue-name resumeai-parse-jobs.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=true

awslocal sqs create-queue --queue-name resumeai-parse-jobs-dlq.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=true

# Create Secrets Manager secret for local dev
awslocal secretsmanager create-secret \
  --name resumeai/anthropic-api-key \
  --secret-string '{"apiKey":"sk-ant-placeholder"}'

echo "LocalStack initialization complete."
