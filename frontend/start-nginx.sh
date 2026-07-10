#!/bin/sh

# 等待网关服务可用，最多等待60秒
echo "等待网关服务启动..."
count=0
max_wait=60
while [ $count -lt $max_wait ]; do
    if ping -c 1 gateway > /dev/null 2>&1; then
        echo "网关服务已就绪"
        break
    fi
    count=$((count + 1))
    sleep 1
done

# 启动nginx
echo "启动nginx..."
exec nginx -g 'daemon off;'