package com.structexam.sandbox.service;

import com.structexam.common.dto.CodeExecuteRequest;
import com.structexam.common.dto.CodeExecuteResponse;
import com.structexam.common.dto.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SandboxPoolService {

    private static final Logger logger = LoggerFactory.getLogger(SandboxPoolService.class);

    @Value("${sandbox.pool.size:10}")
    private int poolSize;

    @Value("${sandbox.tempDir:/tmp/structexam-sandbox}")
    private String tempDir;

    @Value("${sandbox.timeout:5}")
    private long defaultTimeout;

    @Value("${sandbox.maxMemory:256}")
    private int maxMemoryMb;

    @Value("${sandbox.maxCpuCores:1}")
    private int maxCpuCores;

    private BlockingQueue<SandboxWorker> workerPool;
    private AtomicInteger workerIdCounter = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        logger.info("Initializing sandbox worker pool with size: {}", poolSize);
        workerPool = new ArrayBlockingQueue<>(poolSize);
        
        for (int i = 0; i < poolSize; i++) {
            SandboxWorker worker = new SandboxWorker("worker-" + workerIdCounter.incrementAndGet());
            workerPool.offer(worker);
            logger.info("Created sandbox worker: {}", worker.getId());
        }
        logger.info("Sandbox worker pool initialized successfully");
    }

    @PreDestroy
    public void destroy() {
        logger.info("Shutting down sandbox worker pool");
        while (!workerPool.isEmpty()) {
            SandboxWorker worker = workerPool.poll();
            if (worker != null) {
                worker.shutdown();
            }
        }
        logger.info("Sandbox worker pool shutdown completed");
    }

    @Scheduled(fixedDelay = 30000)
    public void healthCheck() {
        int available = (int) workerPool.stream().filter(w -> !w.isBusy()).count();
        int busy = poolSize - available;
        logger.debug("Sandbox pool health check - Total: {}, Available: {}, Busy: {}", 
                poolSize, available, busy);
        
        // 检查并重建故障的worker
        List<SandboxWorker> toRemove = new ArrayList<>();
        for (SandboxWorker worker : workerPool) {
            if (!worker.isHealthy()) {
                toRemove.add(worker);
            }
        }
        for (SandboxWorker worker : toRemove) {
            workerPool.remove(worker);
            worker.shutdown();
            SandboxWorker newWorker = new SandboxWorker("worker-" + workerIdCounter.incrementAndGet());
            workerPool.offer(newWorker);
            logger.warn("Replaced unhealthy worker: {} -> {}", worker.getId(), newWorker.getId());
        }
    }

    public CodeExecuteResponse execute(CodeExecuteRequest request) {
        CodeExecuteResponse response = new CodeExecuteResponse();
        response.setSuccess(false);
        response.setTestResults(new ArrayList<>());

        String language = normalizeLanguage(request.getLanguage());
        long timeout = request.getTimeout() != null ? request.getTimeout() : defaultTimeout;

        SandboxWorker worker = null;
        try {
            // 从池中获取worker，最多等待5秒
            worker = workerPool.poll(5, TimeUnit.SECONDS);
            if (worker == null) {
                response.setRuntimeError("All sandbox workers are busy");
                response.setMessage("Service is busy, please try again later");
                return response;
            }

            worker.markBusy();
            return worker.execute(language, request.getCode(), request.getTestCases(), timeout);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setRuntimeError("Request interrupted");
            response.setMessage("Request interrupted");
        } catch (Exception e) {
            logger.error("Execution failed", e);
            response.setRuntimeError(e.getMessage());
            response.setMessage("Execution failed");
        } finally {
            if (worker != null) {
                worker.markIdle();
                workerPool.offer(worker);
            }
        }
        return response;
    }

    private String normalizeLanguage(String language) {
        if (language == null) return "java";
        String value = language.toLowerCase();
        if ("py".equals(value)) return "python";
        if ("c++".equals(value)) return "cpp";
        return value;
    }

    private class SandboxWorker {
        private final String id;
        private volatile boolean busy = false;
        private volatile boolean healthy = true;
        private volatile LocalDateTime lastUsedTime;

        public SandboxWorker(String id) {
            this.id = id;
            this.lastUsedTime = LocalDateTime.now();
        }

        public String getId() {
            return id;
        }

        public boolean isBusy() {
            return busy;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public void markBusy() {
            this.busy = true;
            this.lastUsedTime = LocalDateTime.now();
        }

        public void markIdle() {
            this.busy = false;
        }

        public void shutdown() {
            this.healthy = false;
        }

        public CodeExecuteResponse execute(String language, String code, List<TestCase> testCases, long timeout) {
            CodeExecuteResponse response = new CodeExecuteResponse();
            response.setSuccess(false);
            response.setTestResults(new ArrayList<>());

            Path workDir = null;
            try {
                Path tempRoot = Path.of(tempDir);
                Files.createDirectories(tempRoot);
                workDir = Files.createTempDirectory(tempRoot, "task-");

                String compileError = prepare(language, code, workDir);
                if (compileError != null) {
                    response.setCompileError(compileError);
                    response.setMessage("Compilation failed");
                    return response;
                }

                if (testCases == null || testCases.isEmpty()) {
                    TestCase testCase = new TestCase();
                    testCase.setInput("");
                    testCase.setExpectedOutput(null);
                    testCases = List.of(testCase);
                }

                long totalTime = 0L;
                for (TestCase testCase : testCases) {
                    long started = System.nanoTime();
                    ProcessResult processResult = run(language, workDir, testCase.getInput(), timeout);
                    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                    totalTime += elapsedMs;

                    CodeExecuteResponse.TestResult result = new CodeExecuteResponse.TestResult();
                    result.setInput(testCase.getInput());
                    result.setExpectedOutput(testCase.getExpectedOutput());
                    result.setActualOutput(processResult.output());
                    result.setExecutionTime(elapsedMs);
                    result.setDescription(testCase.getDescription());
                    result.setPassed(processResult.exitCode() == 0 && outputMatches(processResult.output(), testCase.getExpectedOutput()));
                    response.getTestResults().add(result);

                    if (processResult.timedOut()) {
                        response.setRuntimeError("Time limit exceeded");
                        response.setMessage("Time limit exceeded");
                        break;
                    }
                    if (processResult.exitCode() != 0) {
                        response.setRuntimeError(processResult.output());
                        response.setMessage("Runtime error");
                        break;
                    }
                }

                response.setExecutionTime(totalTime);
                response.setSuccess(response.getTestResults().stream().allMatch(CodeExecuteResponse.TestResult::isPassed));
                response.setMessage(response.isSuccess() ? "All tests passed" : "Some tests failed");

            } catch (Exception e) {
                healthy = false;
                response.setRuntimeError(e.getMessage());
                response.setMessage("Execution failed");
            } finally {
                if (workDir != null) {
                    cleanup(workDir);
                }
            }
            return response;
        }

        private String prepare(String language, String code, Path workDir) throws Exception {
            if (!StringUtils.hasText(code)) {
                return "Code is required";
            }
            if ("python".equals(language)) {
                Files.writeString(workDir.resolve("main.py"), code, StandardCharsets.UTF_8);
                return null;
            }
            if ("java".equals(language)) {
                String className = className(code);
                String source = className == null ? wrapJava(code) : code;
                className = className != null ? className : "Main";
                Files.writeString(workDir.resolve(className + ".java"), source, StandardCharsets.UTF_8);
                return compile(workDir, "javac", className + ".java");
            }
            if ("c".equals(language)) {
                Files.writeString(workDir.resolve("main.c"), code, StandardCharsets.UTF_8);
                return compile(workDir, "gcc", "-O2", "-o", "main", "main.c");
            }
            if ("cpp".equals(language)) {
                Files.writeString(workDir.resolve("main.cpp"), code, StandardCharsets.UTF_8);
                return compile(workDir, "g++", "-O2", "-std=c++17", "-o", "main", "main.cpp");
            }
            return "Unsupported language: " + language;
        }

        private ProcessResult run(String language, Path workDir, String input, long timeoutSeconds) throws Exception {
            List<String> command;
            if ("python".equals(language)) {
                command = List.of("python3", "-u", "main.py");
            } else if ("java".equals(language)) {
                String className = findJavaMainClass(workDir);
                command = List.of("java", "-Xmx" + maxMemoryMb + "m", "-Xms" + (maxMemoryMb / 2) + "m", "-cp", workDir.toString(), className);
            } else {
                command = List.of("./main");
            }
            return runProcess(workDir, command, input, timeoutSeconds);
        }

        private String compile(Path workDir, String... command) throws Exception {
            ProcessResult result = runProcess(workDir, List.of(command), null, 10L);
            return result.exitCode() == 0 ? null : result.output();
        }

        private ProcessResult runProcess(Path workDir, List<String> command, String input, long timeoutSeconds) throws Exception {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            
            // 设置资源限制
            pb.environment().put("OMP_NUM_THREADS", String.valueOf(maxCpuCores));

            Process process = pb.start();

            if (input != null) {
                try (OutputStream stdin = process.getOutputStream()) {
                    stdin.write(input.getBytes(StandardCharsets.UTF_8));
                    if (!input.endsWith("\n")) {
                        stdin.write('\n');
                    }
                }
            }

            StringBuilder output = new StringBuilder();
            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        output.append(line).append('\n');
                    }
                } catch (IOException ignored) {
                }
            });
            reader.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }
            reader.join(1000);
            int exitCode = finished ? process.exitValue() : -1;
            return new ProcessResult(output.toString(), exitCode, !finished);
        }

        private boolean outputMatches(String actual, String expected) {
            if (expected == null) return true;
            return actual.trim().replaceAll("\\s+", " ").equals(expected.trim().replaceAll("\\s+", " "));
        }

        private String className(String code) {
            Matcher matcher = Pattern.compile("class\\s+(\\w+)").matcher(code);
            return matcher.find() ? matcher.group(1) : null;
        }

        private String wrapJava(String code) {
            return "public class Main { public static void main(String[] args) throws Exception { " + code + " } }";
        }

        private String findJavaMainClass(Path workDir) throws IOException {
            try (var stream = Files.list(workDir)) {
                return stream
                        .filter(path -> path.getFileName().toString().endsWith(".class"))
                        .map(path -> path.getFileName().toString().replace(".class", ""))
                        .findFirst()
                        .orElse("Main");
            }
        }

        private void cleanup(Path workDir) {
            try {
                Files.walk(workDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException ignored) {
            }
        }

        private record ProcessResult(String output, int exitCode, boolean timedOut) {}
    }
}