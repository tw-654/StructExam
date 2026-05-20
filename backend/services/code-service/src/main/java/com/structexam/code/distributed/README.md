# StructExam Distributed Judge Component

This package is an isolated backend component for distributed judging and sandbox observability.
It does not modify the Vue frontend.

## Admin Dashboard

Start `code-service`, then open:

```text
http://localhost:8083/code/distributed/admin/dashboard
```

The dashboard shows:

- Redis judge queue size
- Registered sandbox nodes
- Healthy node count
- Current interactive WebSocket sessions
- Recent judge tasks and their statuses
- A test task form for manual distributed-judge testing

## Admin APIs

```text
GET  /code/distributed/admin/snapshot
POST /code/distributed/admin/test-task
POST /code/distributed/admin/load-test
```

## Runtime APIs

```text
POST /code/distributed/submit
GET  /code/distributed/result/{taskId}
GET  /code/distributed/queue
GET  /code/distributed/nodes
POST /code/distributed/exam/{examId}/submit
WS   /ws/distributed-sandbox
```

## Sandbox Node Contract

Every sandbox node should register to service discovery as:

```text
sandbox-node
```

Each node should expose:

```text
POST /sandbox/run
WS   /ws/sandbox
```

`code-service` is the single frontend-facing gateway. The browser should not connect to each
sandbox node directly.

For local testing, this component exposes the current `code-service` as a static local
`sandbox-node`:

```text
http://localhost:8083
metadata.maxConcurrency = 4
```

This is only a development shortcut. A production deployment should run sandbox nodes as
separate containers or pods and set `distributed.judge.local-sandbox-node-enabled=false`.

## Port Strategy

Do not allocate one host port per user session.

Recommended development setup:

```text
sandbox-1: host 18091 -> container 8080
sandbox-2: host 18092 -> container 8080
sandbox-3: host 18093 -> container 8080
```

Recommended production setup:

```text
Kubernetes or Docker bridge network:
each sandbox has its own IP, and all containers use port 8080 internally.
```

## Test Flow

1. Start Redis.
2. Start Nacos if real node discovery is needed.
3. Start one or more sandbox nodes and register them as `sandbox-node`.
4. Start `code-service`.
5. Open `/code/distributed/admin/dashboard`.
6. Click `投递测试任务`.
7. Watch queue size, node count, task status, and result changes.

If no sandbox node is registered, the test task will be queued, retried, and eventually marked
`FAILED`. That is expected and proves the no-node failure path.
