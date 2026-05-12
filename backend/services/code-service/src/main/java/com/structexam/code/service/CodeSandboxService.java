package com.structexam.code.service;

import com.structexam.common.dto.CodeExecuteRequest;
import com.structexam.common.dto.CodeExecuteResponse;
import com.structexam.common.dto.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

@Service
public class CodeSandboxService {

    private static final Logger logger = LoggerFactory.getLogger(CodeSandboxService.class);

    @Value("${sandbox.timeout:10}")
    private long defaultTimeout;

    @Value("${sandbox.maxMemory:256}")
    private int maxMemory;

    @Value("${sandbox.tempDir:./sandbox-temp}")
    private String tempDir;

    public CodeExecuteResponse executeCode(CodeExecuteRequest request) {
        CodeExecuteResponse response = new CodeExecuteResponse();
        response.setSuccess(false);
        response.setTestResults(new ArrayList<>());

        String code = request.getCode();
        String language = request.getLanguage() != null ? request.getLanguage().toLowerCase() : "java";
        List<TestCase> testCases = request.getTestCases();
        long timeout = request.getTimeout() != null ? request.getTimeout() : defaultTimeout;
        String userInput = request.getInput();

        logger.info("Executing {} code, timeout: {}s, has test cases: {}", language, timeout, 
                    testCases != null && !testCases.isEmpty());

        try {
            Path sandboxPath = createSandboxDirectory();
            logger.debug("Created sandbox directory: {}", sandboxPath);

            if ("java".equals(language)) {
                executeJavaCode(response, code, testCases, timeout, sandboxPath, userInput);
            } else if ("cpp".equals(language) || "c++".equals(language)) {
                executeCppCode(response, code, testCases, timeout, sandboxPath, userInput);
            } else if ("python".equals(language) || "py".equals(language)) {
                executePythonCode(response, code, testCases, timeout, sandboxPath, userInput);
            } else {
                response.setRuntimeError("Unsupported language: " + language);
                response.setMessage("Unsupported language");
                logger.warn("Unsupported language: {}", language);
            }

            cleanupSandbox(sandboxPath);
        } catch (Exception e) {
            response.setRuntimeError(e.getMessage());
            response.setMessage("Execution failed: " + e.getMessage());
            logger.error("Execution failed", e);
        }

        logger.info("Execution completed, success: {}, message: {}", response.isSuccess(), response.getMessage());
        return response;
    }

    private void executeJavaCode(CodeExecuteResponse response, String code, List<TestCase> testCases, 
                                  long timeout, Path sandboxPath, String userInput) {
        try {
            String className = extractClassName(code);
            
            if (className == null || className.isEmpty()) {
                className = "Main";
                code = wrapJavaCodeInClass(code);
            }

            String compileError = compileJavaCode(code, sandboxPath.toString(), className);
            if (compileError != null) {
                response.setCompileError(compileError);
                response.setMessage("Compilation failed");
                logger.warn("Java compilation failed: {}", compileError);
                return;
            }

            if (testCases != null && !testCases.isEmpty()) {
                List<CodeExecuteResponse.TestResult> results = new ArrayList<>();
                long totalTime = 0;

                for (TestCase testCase : testCases) {
                    CodeExecuteResponse.TestResult result = executeJavaWithTestCase(sandboxPath.toString(), className, testCase, timeout);
                    results.add(result);
                    totalTime += result.getExecutionTime() != null ? result.getExecutionTime() : 0;
                }

                response.setTestResults(results);
                response.setExecutionTime(totalTime);
                response.setSuccess(results.stream().allMatch(CodeExecuteResponse.TestResult::isPassed));
                response.setMessage(response.isSuccess() ? "All tests passed" : "Some tests failed");
            } else {
                String input = (userInput != null && !userInput.isEmpty()) ? userInput + "\n" : "HelloWorld\n";
                String output = executeJavaClass(sandboxPath.toString(), className, input, timeout);
                CodeExecuteResponse.TestResult result = new CodeExecuteResponse.TestResult();
                result.setPassed(true);
                result.setActualOutput(output);
                result.setExecutionTime(0L);
                response.getTestResults().add(result);
                response.setSuccess(true);
                response.setMessage("Code executed successfully");
            }
        } catch (Exception e) {
            response.setRuntimeError(e.getMessage());
            response.setMessage("Execution failed: " + e.getMessage());
            logger.error("Java execution failed", e);
        }
    }

    private void executeCppCode(CodeExecuteResponse response, String code, List<TestCase> testCases, 
                                 long timeout, Path sandboxPath, String userInput) {
        try {
            String compileError = compileCppCode(code, sandboxPath.toString());
            if (compileError != null) {
                response.setCompileError(compileError);
                response.setMessage("Compilation failed");
                logger.warn("C++ compilation failed: {}", compileError);
                return;
            }
            logger.debug("C++ compilation successful");

            if (testCases != null && !testCases.isEmpty()) {
                List<CodeExecuteResponse.TestResult> results = new ArrayList<>();
                long totalTime = 0;

                for (TestCase testCase : testCases) {
                    CodeExecuteResponse.TestResult result = executeCppWithTestCase(sandboxPath.toString(), testCase, timeout);
                    results.add(result);
                    totalTime += result.getExecutionTime() != null ? result.getExecutionTime() : 0;
                }

                response.setTestResults(results);
                response.setExecutionTime(totalTime);
                response.setSuccess(results.stream().allMatch(CodeExecuteResponse.TestResult::isPassed));
                response.setMessage(response.isSuccess() ? "All tests passed" : "Some tests failed");
            } else {
                String input = (userInput != null && !userInput.isEmpty()) ? userInput + "\n" : "HelloWorld\n";
                String output = executeCppProgram(sandboxPath.toString(), input, timeout);
                CodeExecuteResponse.TestResult result = new CodeExecuteResponse.TestResult();
                result.setPassed(true);
                result.setActualOutput(output);
                result.setExecutionTime(0L);
                response.getTestResults().add(result);
                response.setSuccess(true);
                response.setMessage("Code executed successfully");
            }
        } catch (Exception e) {
            response.setRuntimeError(e.getMessage());
            response.setMessage("Execution failed: " + e.getMessage());
            logger.error("C++ execution failed", e);
        }
    }

    private void executePythonCode(CodeExecuteResponse response, String code, List<TestCase> testCases, 
                                   long timeout, Path sandboxPath, String userInput) {
        try {
            String filename = "main.py";
            Path sourceFile = sandboxPath.resolve(filename);
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            if (testCases != null && !testCases.isEmpty()) {
                List<CodeExecuteResponse.TestResult> results = new ArrayList<>();
                long totalTime = 0;

                for (TestCase testCase : testCases) {
                    CodeExecuteResponse.TestResult result = executePythonWithTestCase(sandboxPath.toString(), filename, testCase, timeout);
                    results.add(result);
                    totalTime += result.getExecutionTime() != null ? result.getExecutionTime() : 0;
                }

                response.setTestResults(results);
                response.setExecutionTime(totalTime);
                response.setSuccess(results.stream().allMatch(CodeExecuteResponse.TestResult::isPassed));
                response.setMessage(response.isSuccess() ? "All tests passed" : "Some tests failed");
            } else {
                String input = (userInput != null && !userInput.isEmpty()) ? userInput + "\n" : "HelloWorld\n";
                String output = executePythonScript(sandboxPath.toString(), filename, input, timeout);
                CodeExecuteResponse.TestResult result = new CodeExecuteResponse.TestResult();
                result.setPassed(true);
                result.setActualOutput(output);
                result.setExecutionTime(0L);
                response.getTestResults().add(result);
                response.setSuccess(true);
                response.setMessage("Code executed successfully");
            }
        } catch (Exception e) {
            response.setRuntimeError(e.getMessage());
            response.setMessage("Execution failed: " + e.getMessage());
            logger.error("Python execution failed", e);
        }
    }

    private Path createSandboxDirectory() throws IOException {
        Path tempPath = Paths.get(tempDir);
        if (!Files.exists(tempPath)) {
            Files.createDirectories(tempPath);
        }
        Path sandboxPath = Files.createTempDirectory(tempPath, "sandbox-");
        return sandboxPath;
    }

    private String extractClassName(String code) {
        String pattern = "class\\s+(\\w+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
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

    private String compileJavaCode(String code, String outputDir, String className) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            String javaHome = System.getenv("JAVA_HOME");
            String javaVersion = System.getProperty("java.version");
            String error = "No Java compiler available. JAVA_HOME: " + javaHome + ", Java version: " + javaVersion;
            logger.error(error);
            return error;
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);

        try {
            Path sourceFile = Paths.get(outputDir, className + ".java");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(
                    Collections.singletonList(sourceFile.toString())
            );

            List<String> options = new ArrayList<>();
            options.add("-d");
            options.add(outputDir);
            options.add("-Xlint");

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnostics, options, null, compilationUnits
            );

            boolean success = task.call();

            if (!success) {
                StringBuilder error = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    error.append(String.format("Line %d: %s%n",
                            diagnostic.getLineNumber(),
                            diagnostic.getMessage(null)));
                }
                return error.toString();
            }
        } catch (IOException e) {
            String error = "Error writing source file: " + e.getMessage();
            logger.error(error, e);
            return error;
        } finally {
            try {
                fileManager.close();
            } catch (IOException ignored) {}
        }

        return null;
    }

    private String compileCppCode(String code, String outputDir) throws IOException {
        Path sourceFile = Paths.get(outputDir, "main.cpp");
        Files.writeString(sourceFile, code, StandardCharsets.UTF_8);
        logger.debug("Wrote C++ source file to: {}", sourceFile);

        String gppPath = findGppPath();
        if (gppPath == null) {
            String error = "C++ compiler (g++) not found. Please install MinGW or add g++ to PATH.";
            logger.error(error);
            return error;
        }
        logger.debug("Found g++ at: {}", gppPath);

        File outputDirFile = new File(outputDir);
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(gppPath, "-o", "main.exe", "main.cpp", "-std=c++11");
        pb.directory(outputDirFile);
        pb.redirectErrorStream(true);

        logger.debug("Compiling C++ code with command: {} in directory: {}", pb.command(), outputDir);
        
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        try {
            int exitCode = process.waitFor();
            logger.debug("C++ compilation exit code: {}", exitCode);
            if (exitCode != 0) {
                String errorMsg = "Compilation failed: " + output.toString();
                logger.error(errorMsg);
                return errorMsg;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Compilation interrupted";
        }

        File exeFile = new File(outputDir, "main.exe");
        if (!exeFile.exists()) {
            String error = "Compilation succeeded but main.exe not found!";
            logger.error(error);
            return error;
        }
        logger.debug("C++ compilation successful, executable at: {}", exeFile.getAbsolutePath());

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

    private CodeExecuteResponse.TestResult executeJavaWithTestCase(String workingDir, String className, TestCase testCase, long timeout) {
        CodeExecuteResponse.TestResult result = new CodeExecuteResponse.TestResult();
        result.setInput(testCase.getInput());
        result.setExpectedOutput(testCase.getExpectedOutput());
        result.setDescription(testCase.getDescription());

        try {
            String output = executeJavaClass(workingDir, className, testCase.getInput(), timeout);
            result.setActualOutput(output);
            result.setPassed(trimOutput(output).equals(trimOutput(testCase.getExpectedOutput())));
        } catch (Exception e) {
            result.setActualOutput("Error: " + e.getMessage());
            result.setPassed(false);
            logger.warn("Java test case failed: {}", e.getMessage());
        }

        return result;
    }

    private CodeExecuteResponse.TestResult executeCppWithTestCase(String workingDir, TestCase testCase, long timeout) {
        CodeExecuteResponse.TestResult result = new CodeExecuteResponse.TestResult();
        result.setInput(testCase.getInput());
        result.setExpectedOutput(testCase.getExpectedOutput());
        result.setDescription(testCase.getDescription());

        try {
            String output = executeCppProgram(workingDir, testCase.getInput(), timeout);
            result.setActualOutput(output);
            result.setPassed(trimOutput(output).equals(trimOutput(testCase.getExpectedOutput())));
        } catch (Exception e) {
            result.setActualOutput("Error: " + e.getMessage());
            result.setPassed(false);
            logger.warn("C++ test case failed: {}", e.getMessage());
        }

        return result;
    }

    private CodeExecuteResponse.TestResult executePythonWithTestCase(String workingDir, String filename, TestCase testCase, long timeout) {
        CodeExecuteResponse.TestResult result = new CodeExecuteResponse.TestResult();
        result.setInput(testCase.getInput());
        result.setExpectedOutput(testCase.getExpectedOutput());
        result.setDescription(testCase.getDescription());

        try {
            String output = executePythonScript(workingDir, filename, testCase.getInput(), timeout);
            result.setActualOutput(output);
            result.setPassed(trimOutput(output).equals(trimOutput(testCase.getExpectedOutput())));
        } catch (Exception e) {
            result.setActualOutput("Error: " + e.getMessage());
            result.setPassed(false);
            logger.warn("Python test case failed: {}", e.getMessage());
        }

        return result;
    }

    private String executeJavaClass(String workingDir, String className, String input, long timeout) throws Exception {
        File workingDirFile = new File(workingDir);
        String absoluteWorkingDir = workingDirFile.getAbsolutePath();
        
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("java", "-Xmx" + maxMemory + "m", "-cp", absoluteWorkingDir, className);
        pb.directory(workingDirFile);
        pb.redirectErrorStream(true);

        return runProcess(pb, input, timeout);
    }

    private String executeCppProgram(String workingDir, String input, long timeout) throws Exception {
        File workingDirFile = new File(workingDir);
        String absoluteWorkingDir = workingDirFile.getAbsolutePath();
        String exePath = new File(absoluteWorkingDir, "main.exe").getAbsolutePath();
        
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(exePath);
        pb.directory(workingDirFile);
        pb.redirectErrorStream(true);

        return runProcess(pb, input, timeout);
    }

    private String executePythonScript(String workingDir, String filename, String input, long timeout) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("python", "-u", filename);
        pb.directory(new File(workingDir));
        pb.redirectErrorStream(true);

        return runProcess(pb, input, timeout);
    }

    private String runProcess(ProcessBuilder pb, String input, long timeout) throws Exception {
        logger.debug("Running process: {}, working dir: {}, input: {}", pb.command(), 
                pb.directory() != null ? pb.directory().getAbsolutePath() : "null",
                input != null ? input.length() + " chars" : "none");
        
        Process process = pb.start();
        long pid = process.pid();
        logger.debug("Process started with PID: {}", pid);

        Thread inputThread = null;
        if (input != null && !input.isEmpty()) {
            inputThread = new Thread(() -> {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(input.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    logger.debug("Wrote {} bytes to process input", input.length());
                } catch (Exception e) {
                    logger.debug("Error writing to process input: {}", e.getMessage());
                }
            });
            inputThread.start();
        }

        StringBuilder outputBuilder = new StringBuilder();
        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buffer = new char[1024];
                int len;
                int totalBytesRead = 0;
                
                while ((len = reader.read(buffer)) != -1) {
                    outputBuilder.append(buffer, 0, len);
                    totalBytesRead += len;
                    logger.debug("Read {} bytes, total: {}", len, totalBytesRead);
                }
            } catch (Exception e) {
                logger.debug("Error reading process output: {}", e.getMessage());
            }
        });
        readerThread.start();

        try {
            boolean exited = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!exited) {
                logger.warn("Process PID {} timed out after {} seconds, destroying...", pid, timeout);
                process.destroyForcibly();
            }
            
            readerThread.join(2000);
            if (inputThread != null) {
                inputThread.join(500);
            }
            
        } finally {
            if (process.isAlive()) {
                logger.debug("Forcefully destroying process PID {}", pid);
                process.destroyForcibly();
                try {
                    process.waitFor(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.debug("Error waiting for process destruction: {}", e.getMessage());
                }
            }
        }

        int exitCode = process.exitValue();
        String output = outputBuilder.toString();
        logger.info("Process PID {} completed - exit code: {}, output length: {}", pid, exitCode, output.length());
        
        return output;
    }

    private String trimOutput(String output) {
        if (output == null) {
            return "";
        }
        return output.trim().replaceAll("\\s+", " ").replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
    }

    private void cleanupSandbox(Path sandboxPath) {
        try {
            Files.walk(sandboxPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ignored) {}
    }
}
