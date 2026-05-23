package com.structexam.sandbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.structexam.common.dto.CodeExecuteRequest;
import com.structexam.common.dto.CodeExecuteResponse;
import com.structexam.common.dto.TestCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SandboxRunService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${sandbox.tempDir:/tmp/structexam-sandbox}")
    private String tempDir;

    @Value("${sandbox.timeout:5}")
    private long defaultTimeout;

    @Value("${sandbox.maxMemory:256}")
    private int maxMemoryMb;

    public CodeExecuteResponse execute(CodeExecuteRequest request) {
        CodeExecuteResponse response = new CodeExecuteResponse();
        response.setSuccess(false);
        response.setTestResults(new ArrayList<>());

        String language = normalizeLanguage(request.getLanguage());
        long timeout = request.getTimeout() != null ? request.getTimeout() : defaultTimeout;

        try {
            Path tempRoot = Path.of(tempDir);
            Files.createDirectories(tempRoot);
            Path workDir = Files.createTempDirectory(tempRoot, "task-");
            try {
                String compileError = prepare(language, request.getCode(), workDir);
                if (compileError != null) {
                    response.setCompileError(compileError);
                    response.setMessage("Compilation failed");
                    return response;
                }

                List<TestCase> testCases = request.getTestCases();
                if (testCases == null || testCases.isEmpty()) {
                    TestCase testCase = new TestCase();
                    testCase.setInput(request.getInput() != null ? request.getInput() : "");
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
            } finally {
                cleanup(workDir);
            }
        } catch (Exception e) {
            response.setRuntimeError(e.getMessage());
            response.setMessage("Execution failed");
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
            command = List.of("java", "-Xmx" + maxMemoryMb + "m", "-cp", workDir.toString(), className);
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
        Process process = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .start();

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
        if (expected == null) {
            return true;
        }
        try {
            JsonNode actualJson = objectMapper.readTree(actual);
            JsonNode expectedJson = objectMapper.readTree(expected);
            return actualJson.equals(expectedJson);
        } catch (Exception e) {
            return normalizeOutput(actual).equals(normalizeOutput(expected));
        }
    }

    private String normalizeOutput(String output) {
        return output == null ? "" : output.trim().replaceAll("\\s+", " ");
    }

    private String normalizeLanguage(String language) {
        if (language == null) {
            return "java";
        }
        String value = language.toLowerCase();
        if ("py".equals(value)) {
            return "python";
        }
        if ("c++".equals(value)) {
            return "cpp";
        }
        return value;
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

    private record ProcessResult(String output, int exitCode, boolean timedOut) {
    }
}
