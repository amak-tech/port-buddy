#!/bin/sh
echo "Starting" && \
  ls -al /app/dist/ && \
  mkdir -p /app/pb-web && \
  echo "Dir /app/pb-web/ (re)created" && \
  rm -rf /app/pb-web/* && \
  echo "Dir /app/pb-web/ cleaned" && \
  cp -a /app/dist/. /app/pb-web/ && \
  echo "Files copied to /app/pb-web/ folder" && \
  ls -al /app/pb-web/
