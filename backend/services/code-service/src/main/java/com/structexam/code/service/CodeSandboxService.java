package com.structexam.code.service;

import com.structexam.common.dto.CodeExecuteRequest;
import com.structexam.common.dto.CodeExecuteResponse;
import com.structexam.common.dto.TestCase;
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
        List<TestCase> testCases = request.getTestCases();
        long timeout = request.getTimeout() != null ? request.getTimeout() : defaultTimeout;

        try {
            Path sandboxPath = createSandboxDirectory();
            String className = extractClassName(code);
            
            if (className == null || className.isEmpty()) {
                className = "Main";
                code = wrapCodeInClass(code);
            }

            String compileError = compileCode(code, sandboxPath.toString(), className);
            if (compileError != null) {
                response.setCompileError(compileError);
                response.setMessage("Compilation failed");
                return response;
            }

            if (testCases != null && !testCases.isEmpty()) {
                List<CodeExecuteResponse.TestResult> results = new ArrayList<>();
                long totalTime = 0;

                for (TestCase testCase : testCases) {
                    CodeExecuteResponse.TestResult result = executeWithTestCase(sandboxPath.toString(), className, testCase, timeout);
                    results.add(result);
                    totalTime += result.getExecutionTime() != null ? result.getExecutionTime() : 0;
                }

                response.setTestResults(results);
                response.setExecutionTime(totalTime);
                response.setSuccess(results.stream().allMatch(CodeExecuteResponse.TestResult::isPassed));
                response.setMessage(response.isSuccess() ? "All tests passed" : "Some tests failed");
            } else {
                String output = executeClass(sandboxPath.toString(), className, "", timeout);
                CodeExecuteResponse.TestResult result = new CodeExecuteResponse.TestResult();
                result.setPassed(true);
                result.setActualOutput(output);
                result.setExecutionTime(0L);
                response.getTestResults().add(result);
                response.setSuccess(true);
                response.setMessage("Code executed successfully");
            }

            cleanupSandbox(sandboxPath);
        } catch (Exception e) {
            response.setRuntimeError(e.getMessage());
            response.setMessage("Execution failed: " + e.getMessage());
        }

        return response;
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

    private String wrapCodeInClass(String code) {
        return """
            public class Main {
                public static void main(String[] args) {
                    %s
                }
            }
            """.formatted(code);
    }

    private String compileCode(String code, String outputDir, String className) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return "No Java compiler available";
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
            return "Error writing source file: " + e.getMessage();
        } finally {
            try {
                fileManager.close();
            } catch (IOException ignored) {}
        }

        return null;
    }

    private CodeExecuteResponse.TestResult executeWithTestCase(String workingDir, String className, TestCase testCase, long timeout) {
        CodeExecuteResponse.TestResult result = new CodeExecuteResponse.TestResult();
        result.setInput(testCase.getInput());
        result.setExpectedOutput(testCase.getExpectedOutput());
        result.setDescription(testCase.getDescription());

        try {
            String output = executeClass(workingDir, className, testCase.getInput(), timeout);
            result.setActualOutput(output);
            result.setPassed(trimOutput(output).equals(trimOutput(testCase.getExpectedOutput())));
        } catch (Exception e) {
            result.setActualOutput("Error: " + e.getMessage());
            result.setPassed(false);
        }

        return result;
    }

    private String executeClass(String workingDir, String className, String input, long timeout) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("java", "-Xmx" + maxMemory + "m", "-cp", workingDir, className);
        pb.directory(new File(workingDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        if (input != null && !input.isEmpty()) {
            try (OutputStream os = process.getOutputStream()) {
                os.write(input.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            return output.toString();
        });

        try {
            String output = future.get(timeout, TimeUnit.SECONDS);
            int exitCode = process.waitFor(1, TimeUnit.SECONDS);
            if (exitCode != 0) {
                throw new Exception("Program exited with code " + exitCode + ": " + output);
            }
            return output;
        } catch (TimeoutException e) {
            process.destroyForcibly();
            throw new Exception("Execution timed out after " + timeout + " seconds");
        } finally {
            executor.shutdown();
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
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