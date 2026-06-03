package com.structexam.code.distributed.controller;

import com.structexam.code.distributed.config.DistributedJudgeProperties;
import com.structexam.code.distributed.dto.DistributedDashboardSnapshot;
import com.structexam.code.distributed.dto.JudgeTaskResponse;
import com.structexam.code.distributed.dto.LoadTestRequest;
import com.structexam.code.distributed.dto.LoadTestResponse;
import com.structexam.code.distributed.dto.TestJudgeTaskRequest;
import com.structexam.code.distributed.service.DistributedAdminService;
import com.structexam.code.distributed.service.SandboxNodeRegistry;
import com.structexam.common.dto.ApiResponse;
import com.structexam.common.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/code/distributed/admin")
public class DistributedAdminController {

    private final DistributedAdminService adminService;
    private final SandboxNodeRegistry nodeRegistry;
    private final DistributedJudgeProperties properties;

    public DistributedAdminController(DistributedAdminService adminService,
                                     SandboxNodeRegistry nodeRegistry,
                                     DistributedJudgeProperties properties) {
        this.adminService = adminService;
        this.nodeRegistry = nodeRegistry;
        this.properties = properties;
    }

    @GetMapping("/snapshot")
    public ApiResponse<DistributedDashboardSnapshot> snapshot(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return ApiResponse.success(adminService.snapshot());
    }

    @PostMapping("/test-task")
    public ApiResponse<JudgeTaskResponse> testTask(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody(required = false) TestJudgeTaskRequest request) {
        requireAdmin(role);
        TestJudgeTaskRequest safeRequest = request != null ? request : new TestJudgeTaskRequest();
        return ApiResponse.success("测试任务已入队", adminService.submitTestTask(safeRequest));
    }

    @PostMapping("/load-test")
    public ApiResponse<LoadTestResponse> loadTest(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody(required = false) LoadTestRequest request) {
        requireAdmin(role);
        LoadTestRequest safeRequest = request != null ? request : new LoadTestRequest();
        return ApiResponse.success("压测任务已批量入队", adminService.startLoadTest(safeRequest));
    }

    @PostMapping("/strategy")
    public ApiResponse<String> switchLoadBalanceStrategy(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody Map<String, String> request) {
        requireAdmin(role);
        String strategy = request.get("strategy");
        if (strategy == null || strategy.isBlank()) {
            throw new BusinessException(400, "策略参数不能为空");
        }
        
        if (!"roundRobin".equalsIgnoreCase(strategy) && !"leastTasks".equalsIgnoreCase(strategy)) {
            throw new BusinessException(400, "不支持的策略类型: " + strategy);
        }
        
        properties.setLoadBalanceStrategy(strategy);
        return ApiResponse.success("负载均衡策略已切换为: " + strategy, strategy);
    }

    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard() {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>StructExam Distributed Monitor</title>
                  <style>
                    body { margin:0; font-family: Arial, sans-serif; background:#f5f7fb; color:#172033; }
                    header { padding:18px 24px; background:#172033; color:white; display:flex; justify-content:space-between; align-items:center; }
                    main { padding:20px; display:grid; gap:16px; }
                    .grid { display:grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap:12px; }
                    .card { background:white; border:1px solid #dfe5ef; border-radius:8px; padding:14px; }
                    .metric { font-size:28px; font-weight:700; margin-top:8px; }
                    table { width:100%; border-collapse:collapse; font-size:13px; }
                    th, td { padding:9px 8px; border-bottom:1px solid #edf1f7; text-align:left; vertical-align:top; }
                    th { color:#5f6f89; font-weight:600; background:#fafbfd; }
                    button { border:0; background:#2563eb; color:white; border-radius:6px; padding:8px 12px; cursor:pointer; }
                    button.secondary { background:#475569; }
                    textarea, input, select { width:100%; box-sizing:border-box; border:1px solid #ccd6e5; border-radius:6px; padding:8px; }
                    textarea { min-height:110px; font-family: Consolas, monospace; }
                    .row { display:grid; grid-template-columns: 1fr 1fr 1fr; gap:10px; margin-bottom:10px; }
                    .status { font-weight:700; }
                    .ok { color:#15803d; }
                    .bad { color:#b91c1c; }
                    .muted { color:#64748b; }
                    @media (max-width: 900px) { .grid, .row { grid-template-columns:1fr; } }
                  </style>
                </head>
                <body>
                <header>
                  <div>
                    <strong>StructExam 分布式判题观察台</strong>
                    <span class="muted" id="updated"></span>
                  </div>
                  <div>
                    <button class="secondary" onclick="refresh()">刷新</button>
                    <button onclick="submitTestTask()">投递测试任务</button>
                    <button onclick="startLoadTest()">模拟并发</button>
                  </div>
                </header>
                <main>
                  <section class="grid">
                    <div class="card"><div>队列长度</div><div class="metric" id="queueSize">-</div></div>
                    <div class="card"><div>沙箱节点</div><div class="metric" id="nodeCount">-</div></div>
                    <div class="card"><div>健康节点</div><div class="metric" id="healthyCount">-</div></div>
                    <div class="card"><div>交互会话</div><div class="metric" id="sessionCount">-</div></div>
                  </section>
                  <section class="card">
                    <h3>并发模拟</h3>
                    <div class="row">
                      <input id="loadUsers" value="100" placeholder="用户数">
                      <input id="loadSubmissions" value="1" placeholder="每用户提交数">
                      <input id="loadExamId" value="880001" placeholder="考试ID">
                    </div>
                    <p class="muted" id="loadResult">模拟会批量创建不同用户的判题任务，用于观察队列、节点负载、重试与结果。</p>
                  </section>
                  <section class="card">
                    <h3>投递测试任务</h3>
                    <div class="row">
                      <input id="userId" value="900001" placeholder="userId">
                      <input id="examId" value="990001" placeholder="examId">
                      <input id="questionId" value="990001" placeholder="questionId">
                    </div>
                    <div class="row">
                      <select id="language"><option>python</option><option>java</option><option>cpp</option></select>
                      <input id="testInput" value="hello" placeholder="测试输入">
                      <input id="expectedOutput" value="hello" placeholder="期望输出">
                    </div>
                    <textarea id="code">print(input())</textarea>
                  </section>
                  <section class="card">
                    <h3>沙箱节点</h3>
                    <table><thead><tr><th>服务</th><th>地址</th><th>健康</th><th>运行任务</th><th>元数据</th></tr></thead><tbody id="nodes"></tbody></table>
                  </section>
                  <section class="card">
                    <h3>最近任务</h3>
                    <table><thead><tr><th>taskId</th><th>用户</th><th>考试/题目</th><th>语言</th><th>状态</th><th>重试</th><th>错误</th></tr></thead><tbody id="tasks"></tbody></table>
                  </section>
                  <section class="card">
                    <h3>交互会话</h3>
                    <table><thead><tr><th>网关会话</th><th>沙箱地址</th><th>状态</th></tr></thead><tbody id="sessions"></tbody></table>
                  </section>
                </main>
                <script>
                  async function refresh() {
                    const res = await fetch('/code/distributed/admin/snapshot');
                    const body = await res.json();
                    const data = body.data || {};
                    const nodes = data.nodes || [];
                    const sessions = data.interactiveSessions || [];
                    document.getElementById('queueSize').textContent = data.queueSize ?? 0;
                    document.getElementById('nodeCount').textContent = nodes.length;
                    document.getElementById('healthyCount').textContent = nodes.filter(n => n.healthy).length;
                    document.getElementById('sessionCount').textContent = sessions.length;
                    document.getElementById('updated').textContent = '  更新于 ' + new Date().toLocaleTimeString();
                    renderNodes(nodes);
                    renderTasks(data.recentTasks || []);
                    renderSessions(sessions);
                  }
                  function renderNodes(nodes) {
                    document.getElementById('nodes').innerHTML = nodes.map(n => `<tr><td>${n.serviceId || ''}</td><td>${n.uri || ''}</td><td class="${n.healthy ? 'ok' : 'bad'}">${n.healthy ? 'healthy' : 'unhealthy'}</td><td>${n.runningTasks ?? 0}</td><td>${JSON.stringify(n.metadata || {})}</td></tr>`).join('') || '<tr><td colspan="5" class="muted">暂无节点。请启用 Nacos discovery 并注册 sandbox-node。</td></tr>';
                  }
                  function renderTasks(tasks) {
                    document.getElementById('tasks').innerHTML = tasks.map(t => `<tr><td>${t.taskId || ''}</td><td>${t.userId || ''}</td><td>${t.examId || ''}/${t.questionId || ''}</td><td>${t.language || ''}</td><td class="status">${t.status || ''}</td><td>${t.retryCount ?? 0}</td><td>${t.error || ''}</td></tr>`).join('') || '<tr><td colspan="7" class="muted">暂无任务</td></tr>';
                  }
                  function renderSessions(sessions) {
                    document.getElementById('sessions').innerHTML = sessions.map(s => `<tr><td>${s.gatewaySessionId || ''}</td><td>${s.sandboxUri || ''}</td><td>${s.status || ''}</td></tr>`).join('') || '<tr><td colspan="3" class="muted">暂无交互会话</td></tr>';
                  }
                  async function submitTestTask() {
                    const payload = {
                      userId: Number(document.getElementById('userId').value),
                      examId: Number(document.getElementById('examId').value),
                      questionId: Number(document.getElementById('questionId').value),
                      language: document.getElementById('language').value,
                      code: document.getElementById('code').value,
                      testCases: [{ input: document.getElementById('testInput').value, expectedOutput: document.getElementById('expectedOutput').value }]
                    };
                    const res = await fetch('/code/distributed/admin/test-task', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload) });
                    const body = await res.json();
                    alert(body.message + (body.data ? ': ' + body.data.taskId : ''));
                    refresh();
                  }
                  async function startLoadTest() {
                    const payload = {
                      users: Number(document.getElementById('loadUsers').value),
                      submissionsPerUser: Number(document.getElementById('loadSubmissions').value),
                      examId: Number(document.getElementById('loadExamId').value),
                      language: document.getElementById('language').value,
                      code: document.getElementById('code').value,
                      input: document.getElementById('testInput').value,
                      expectedOutput: document.getElementById('expectedOutput').value
                    };
                    const res = await fetch('/code/distributed/admin/load-test', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload) });
                    const body = await res.json();
                    const data = body.data || {};
                    document.getElementById('loadResult').textContent = `已请求 ${data.requestedTasks || 0} 个任务，入队 ${data.acceptedTasks || 0} 个，拒绝 ${data.rejectedTasks || 0} 个`;
                    refresh();
                  }
                  refresh();
                  setInterval(refresh, 3000);
                </script>
                </body>
                </html>
                """;
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new BusinessException(403, "Only admins can access this API");
        }
    }
}
