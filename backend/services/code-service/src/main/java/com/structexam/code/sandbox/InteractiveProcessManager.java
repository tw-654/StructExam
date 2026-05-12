package com.structexam.code.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import com.structexam.code.websocket.SandboxWebSocketHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Component
public class InteractiveProcessManager {

    private static final Logger logger = LoggerFactory.getLogger(InteractiveProcessManager.class);

    private final Map<String, InteractiveProcess> processes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    @PostConstruct
    public void init() {
        SandboxWebSocketHandler.setProcessManager(this);
    }

    public String createSession(String code, String language) {
        String sessionId = UUID.randomUUID().toString();
        return sessionId;
    }

    public InteractiveProcess startProcess(String sessionId, String code, String language, long timeout) {
        try {
            Path sandboxPath = createSandboxDirectory();
            InteractiveProcess process = new InteractiveProcess(sessionId, sandboxPath, timeout, scheduler);
            process.code = code;

            // 先编译，看是否有错误
            String compileError = null;
            if ("java".equals(language)) {
                compileError = process.compileJava(code);
            } else if ("cpp".equals(language) || "c++".equals(language)) {
                compileError = process.compileCpp(code);
            }

            // 如果编译有错误，清理资源后直接返回
            if (compileError != null) {
                process.cleanup();
                process.pendingCompileError = compileError;
                return process;
            }

            // 编译成功，启动执行
            process.startExecution(language);
            processes.put(sessionId, process);

            new Thread(() -> {
                try {
                    int exitCode = process.waitForProcess();
                    if (exitCode == 0) {
                        process.setStatus(ProcessStatus.TERMINATED);
                    } else {
                        process.setStatus(ProcessStatus.ERROR);
                    }
                } catch (InterruptedException e) {
                    process.setStatus(ProcessStatus.TERMINATED);
                }
            }).start();

            process.setTimeoutTask(scheduler.schedule(() -> {
                InteractiveProcess p = processes.get(sessionId);
                if (p != null && p.isAlive()) {
                    p.notifyStatus(ProcessStatus.TIMEOUT);
                    p.terminate();
                }
            }, timeout, TimeUnit.SECONDS));

            return process;
        } catch (Exception e) {
            logger.error("Failed to start process for language: {}", language, e);
            InteractiveProcess process = new InteractiveProcess(sessionId, null, timeout, scheduler);
            process.pendingCompileError = "启动失败: " + e.getMessage();
            return process;
        }
    }

    public void sendInput(String sessionId, String input) {
        InteractiveProcess process = processes.get(sessionId);
        if (process != null && process.isAlive()) {
            process.writeInput(input);
        }
    }

    public void terminateProcess(String sessionId) {
        InteractiveProcess process = processes.get(sessionId);
        if (process != null) {
            process.terminate();
            processes.remove(sessionId);
        }
    }

    public InteractiveProcess getProcess(String sessionId) {
        return processes.get(sessionId);
    }

    private Path createSandboxDirectory() throws IOException {
        Path tempPath = Paths.get("./sandbox-temp");
        if (!Files.exists(tempPath)) {
            Files.createDirectories(tempPath);
        }
        Path sandboxPath = Files.createTempDirectory(tempPath, "sandbox-");
        return sandboxPath;
    }

    public enum ProcessStatus {
        STARTING,
        RUNNING,
        WAITING_INPUT,
        TERMINATED,
        TIMEOUT,
        COMPILE_ERROR,
        ERROR
    }

    public static class InteractiveProcess {
        private final String sessionId;
        private final Path workingDir;
        private final long timeout;
        private final ScheduledExecutorService scheduler;
        private String code;
        private Process process;
        private ProcessStatus status = ProcessStatus.STARTING;
        private ScheduledFuture<?> timeoutTask;
        private ScheduledFuture<?> inputCheckTask;

        private OutputStream stdinWriter;

        private ProcessOutputListener outputListener;
        private ProcessStatusListener statusListener;
        private ProcessErrorListener errorListener;

        private volatile boolean running = true;
        private volatile boolean hasOutput = false;
        private volatile String pendingCompileError;

        public InteractiveProcess(String sessionId, Path workingDir, long timeout, ScheduledExecutorService scheduler) {
            this.sessionId = sessionId;
            this.workingDir = workingDir;
            this.timeout = timeout;
            this.scheduler = scheduler;
        }

        public void setOutputListener(ProcessOutputListener listener) {
            this.outputListener = listener;
        }

        public void setStatusListener(ProcessStatusListener listener) {
            this.statusListener = listener;
            // 如果有待处理的编译错误，设置状态后立即发送
            if (pendingCompileError != null) {
                notifyStatus(ProcessStatus.COMPILE_ERROR);
            }
        }

        public void setErrorListener(ProcessErrorListener listener) {
            this.errorListener = listener;
            // 如果有待处理的编译错误，立即发送
            if (pendingCompileError != null) {
                notifyError("编译错误: " + pendingCompileError);
                pendingCompileError = null; // 发送后清除
            }
        }

        public String compileJava(String code) throws Exception {
            String className = extractClassName(code);
            if (className == null || className.isEmpty()) {
                className = "Main";
                code = wrapJavaCodeInClass(code);
            }

            Path sourceFile = workingDir.resolve(className + ".java");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            String javacPath = findJavacPath();
            if (javacPath == null) {
                String error = "Java compiler (javac) not found. Please install JDK and add javac to PATH.";
                logger.error(error);
                return error;
            }
            logger.debug("Found javac at: {}", javacPath);

            ProcessBuilder pb = new ProcessBuilder(javacPath, sourceFile.toString());
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);

            Process compileProcess = pb.start();
            StringBuilder error = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(compileProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            }

            int exitCode = compileProcess.waitFor();
            if (exitCode != 0) {
                String compileError = error.toString();
                logger.warn("Java compilation failed: {}", compileError);
                return compileError;
            }

            this.process = createJavaProcess(className);
            return null;
        }

        public String compileCpp(String code) throws Exception {
            Path sourceFile = workingDir.resolve("main.cpp");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);
            logger.info("[{}] Wrote C++ source to: {}", sessionId, sourceFile);

            String gppPath = findGppPath();
            if (gppPath == null) {
                String error = "C++ compiler (g++) not found. Please install MinGW or add g++ to PATH.";
                logger.error("[{}] {}", sessionId, error);
                return error;
            }
            logger.info("[{}] Found g++ at: {}", sessionId, gppPath);

            ProcessBuilder pb = new ProcessBuilder(gppPath, "-o", "main.exe", "main.cpp", "-std=c++11");
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);

            logger.info("[{}] Compiling C++: {} in directory: {}", sessionId, pb.command(), workingDir);
            Process compileProcess = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(compileProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = compileProcess.waitFor();
            logger.info("[{}] C++ compilation exit code: {}", sessionId, exitCode);
            
            if (exitCode != 0) {
                String compileError = output.toString();
                logger.error("[{}] C++ compilation failed: {}", sessionId, compileError);
                return compileError;
            }

            Path exePath = workingDir.resolve("main.exe");
            if (!Files.exists(exePath)) {
                String error = "Compilation succeeded but main.exe not found at: " + exePath;
                logger.error("[{}] {}", sessionId, error);
                return error;
            }
            logger.info("[{}] C++ compilation successful, executable at: {}", sessionId, exePath);

            return null;
        }

        private String findJavacPath() {
            String javaHome = System.getenv("JAVA_HOME");
            if (javaHome != null) {
                String javacPath = Paths.get(javaHome, "bin", "javac.exe").toString();
                if (new File(javacPath).exists()) {
                    return javacPath;
                }
            }

            String[] possiblePaths = {
                "javac",
                "C:\\Program Files\\Java\\jdk*\\bin\\javac.exe",
                "C:\\Program Files (x86)\\Java\\jdk*\\bin\\javac.exe"
            };

            for (String path : possiblePaths) {
                if (path.contains("*")) {
                    String parentDir = path.substring(0, path.indexOf("*"));
                    File parent = new File(parentDir);
                    if (parent.exists() && parent.isDirectory()) {
                        File[] jdkDirs = parent.listFiles((dir, name) -> name.startsWith("jdk"));
                        if (jdkDirs != null && jdkDirs.length > 0) {
                            String fullPath = Paths.get(parentDir.replace("*", ""), jdkDirs[0].getName(), "bin", "javac.exe").toString();
                            if (new File(fullPath).exists()) {
                                return fullPath;
                            }
                        }
                    }
                } else {
                    try {
                        ProcessBuilder pb = new ProcessBuilder(path, "-version");
                        Process process = pb.start();
                        int exitCode = process.waitFor();
                        if (exitCode == 0) {
                            return path;
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            return null;
        }

        private String findGppPath() {
            String[] possiblePaths = {
                "g++",
                "C:\\MinGW\\bin\\g++.exe",
                "C:\\MinGW64\\bin\\g++.exe",
                "C:\\Program Files\\MinGW\\bin\\g++.exe",
                "C:\\Program Files (x86)\\MinGW\\bin\\g++.exe",
                "C:\\ProgramData\\chocolatey\\lib\\mingw\\tools\\install\\mingw64\\bin\\g++.exe"
            };

            for (String path : possiblePaths) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(path, "--version");
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        return path;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            return null;
        }

        public void startExecution(String language) throws Exception {
            if ("java".equals(language)) {
                if (this.process == null) {
                    this.process = createJavaProcess("Main");
                }
            } else if ("python".equals(language) || "py".equals(language)) {
                Path sourceFile = workingDir.resolve("main.py");
                Files.writeString(sourceFile, code, StandardCharsets.UTF_8);
                this.process = createPythonProcess();
            } else if ("cpp".equals(language) || "c++".equals(language)) {
                if (this.process == null) {
                    this.process = createCppProcess();
                }
            }

            if (this.process == null) {
                throw new Exception("无法创建进程");
            }

            this.stdinWriter = process.getOutputStream();

            hasOutput = false;
            startOutputReader(process.getInputStream(), false);
            startOutputReader(process.getErrorStream(), true);

            running = true;
            this.status = ProcessStatus.RUNNING;
            if (statusListener != null) {
                statusListener.onStatusChange(sessionId, ProcessStatus.RUNNING);
            }

            inputCheckTask = scheduler.schedule(() -> {
                if (running && !hasOutput && process.isAlive()) {
                    this.status = ProcessStatus.WAITING_INPUT;
                    if (statusListener != null) {
                        statusListener.onStatusChange(sessionId, ProcessStatus.WAITING_INPUT);
                    }
                }
            }, 500, TimeUnit.MILLISECONDS);
        }

        public void markHasOutput() {
            this.hasOutput = true;
            if (inputCheckTask != null) {
                inputCheckTask.cancel(false);
            }
        }

        public int waitForProcess() throws InterruptedException {
            if (process != null) {
                return process.waitFor();
            }
            return -1;
        }

        private Process createJavaProcess(String className) throws Exception {
            ProcessBuilder pb = new ProcessBuilder("java", "-Xmx256m", "-cp", workingDir.toString(), className);
            pb.directory(workingDir.toFile());
            pb.environment().put("PYTHONUNBUFFERED", "1");
            return pb.start();
        }

        private Process createCppProcess() throws IOException {
            logger.info("[{}] Creating C++ process: main.exe in directory: {}", sessionId, workingDir);
            
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "main.exe");
            pb.directory(workingDir.toFile());
            pb.environment().put("PYTHONUNBUFFERED", "1");
            
            Process proc = pb.start();
            logger.info("[{}] C++ process started with PID: {}", sessionId, proc.pid());
            return proc;
        }

        private Process createPythonProcess() throws IOException {
            ProcessBuilder pb = new ProcessBuilder("python", "-u", "main.py");
            pb.directory(workingDir.toFile());
            pb.environment().put("PYTHONUNBUFFERED", "1");
            return pb.start();
        }

        private void startOutputReader(InputStream inputStream, boolean isError) {
            logger.info("[{}] Starting {} output reader", sessionId, isError ? "error" : "stdout");
            new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int len;
                    long lastActivityTime = System.currentTimeMillis();
                    
                    while (running && !Thread.currentThread().isInterrupted()) {
                        int available = inputStream.available();
                        if (available > 0) {
                            len = inputStream.read(buffer);
                            if (len > 0) {
                                String output = new String(buffer, 0, len);
                                markHasOutput();
                                lastActivityTime = System.currentTimeMillis();
                                logger.info("[{}] Read {} bytes from {}: {}", sessionId, len, isError ? "error" : "stdout", output);
                                if (outputListener != null) {
                                    outputListener.onOutput(sessionId, output, isError);
                                }
                            }
                        } else {
                            if (!process.isAlive()) {
                                logger.info("[{}] Process {} is not alive, checking remaining output", sessionId, isError ? "error" : "stdout");
                                long endTime = System.currentTimeMillis();
                                while (endTime - lastActivityTime < 500) {
                                    if (inputStream.available() > 0) {
                                        len = inputStream.read(buffer);
                                        if (len > 0) {
                                            String output = new String(buffer, 0, len);
                                            markHasOutput();
                                            logger.info("[{}] Read {} remaining {} output: {}", sessionId, len, isError ? "error" : "stdout", output);
                                            if (outputListener != null) {
                                                outputListener.onOutput(sessionId, output, isError);
                                            }
                                        }
                                        lastActivityTime = System.currentTimeMillis();
                                    }
                                    Thread.sleep(50);
                                    endTime = System.currentTimeMillis();
                                }
                                logger.info("[{}] Finished reading {} output", sessionId, isError ? "error" : "stdout");
                                break;
                            }
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("[{}] Error reading {} output: {}", sessionId, isError ? "error" : "stdout", e.getMessage(), e);
                    if (running && errorListener != null) {
                        errorListener.onError(sessionId, e.getMessage());
                    }
                }
            }).start();
        }

        public void writeInput(String input) {
            try {
                if (stdinWriter != null && process.isAlive()) {
                    stdinWriter.write(input.getBytes(StandardCharsets.UTF_8));
                    stdinWriter.flush();
                    notifyStatus(ProcessStatus.RUNNING);
                }
            } catch (Exception e) {
                if (errorListener != null) {
                    errorListener.onError(sessionId, "输入失败: " + e.getMessage());
                }
            }
        }

        public void terminate() {
            running = false;
            try {
                if (inputCheckTask != null) {
                    inputCheckTask.cancel(false);
                }
                if (process != null && process.isAlive()) {
                    process.destroyForcibly();
                }
                cleanup();
                notifyStatus(ProcessStatus.TERMINATED);
            } catch (Exception e) {
                if (errorListener != null) {
                    errorListener.onError(sessionId, "终止失败: " + e.getMessage());
                }
            }
        }

        public boolean isAlive() {
            return process != null && process.isAlive();
        }

        public void setStatus(ProcessStatus status) {
            this.status = status;
            notifyStatus(status);
        }

        public void notifyStatus(ProcessStatus status) {
            this.status = status;
            if (statusListener != null) {
                statusListener.onStatusChange(sessionId, status);
            }
        }

        public void notifyError(String error) {
            if (errorListener != null) {
                errorListener.onError(sessionId, error);
            }
        }

        public void setTimeoutTask(ScheduledFuture<?> task) {
            this.timeoutTask = task;
        }

        public ProcessStatus getStatus() {
            return status;
        }

        public String getSessionId() {
            return sessionId;
        }

        private void cleanup() {
            running = false;
            try {
                if (stdinWriter != null) stdinWriter.close();
            } catch (Exception ignored) {}

            if (timeoutTask != null) {
                timeoutTask.cancel(false);
            }

            if (workingDir != null && Files.exists(workingDir)) {
                try {
                    Files.walk(workingDir)
                            .sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException ignored) {}
            }
        }

        private String extractClassName(String code) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("class\\s+(\\w+)");
            java.util.regex.Matcher m = p.matcher(code);
            if (m.find()) {
                return m.group(1);
            }
            return null;
        }

        private String wrapJavaCodeInClass(String code) {
            return """
                public class Main {
                    public static void main(String[] args) {
                        %s
                    }
                }
                """.formatted(code);
        }
    }

    public interface ProcessOutputListener {
        void onOutput(String sessionId, String data, boolean isError);
    }

    public interface ProcessStatusListener {
        void onStatusChange(String sessionId, ProcessStatus status);
    }

    public interface ProcessErrorListener {
        void onError(String sessionId, String error);
    }
}
