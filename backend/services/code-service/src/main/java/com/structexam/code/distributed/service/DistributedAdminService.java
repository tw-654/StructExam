package com.structexam.code.distributed.service;

import com.structexam.code.distributed.dto.DistributedDashboardSnapshot;
import com.structexam.code.distributed.dto.DistributedJudgeSubmitRequest;
import com.structexam.code.distributed.dto.JudgeTaskResponse;
import com.structexam.code.distributed.dto.LoadTestRequest;
import com.structexam.code.distributed.dto.LoadTestResponse;
import com.structexam.code.distributed.dto.TestJudgeTaskRequest;
import com.structexam.common.dto.TestCase;
import com.structexam.common.exception.BusinessException;
import com.structexam.code.distributed.websocket.DistributedSandboxWebSocketHandler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DistributedAdminService {

    private final JudgeTaskQueueService queueService;
    private final SandboxNodeRegistry nodeRegistry;
    private final DistributedSandboxWebSocketHandler webSocketHandler;
    private final DistributedJudgeService distributedJudgeService;

    public DistributedAdminService(JudgeTaskQueueService queueService,
                                   SandboxNodeRegistry nodeRegistry,
                                   DistributedSandboxWebSocketHandler webSocketHandler,
                                   DistributedJudgeService distributedJudgeService) {
        this.queueService = queueService;
        this.nodeRegistry = nodeRegistry;
        this.webSocketHandler = webSocketHandler;
        this.distributedJudgeService = distributedJudgeService;
    }

    public DistributedDashboardSnapshot snapshot() {
        DistributedDashboardSnapshot snapshot = new DistributedDashboardSnapshot();
        snapshot.setQueueSize(queueService.size());
        snapshot.setProcessingQueueSize(queueService.processingSize());
        snapshot.setNodes(nodeRegistry.nodeViews());
        snapshot.setRecentTasks(queueService.recentTasks());
        snapshot.setInteractiveSessions(webSocketHandler.activeSessions());
        return snapshot;
    }

    public JudgeTaskResponse submitTestTask(TestJudgeTaskRequest request) {
        DistributedJudgeSubmitRequest submitRequest = new DistributedJudgeSubmitRequest();
        submitRequest.setExamId(request.getExamId());
        submitRequest.setQuestionId(request.getQuestionId());
        submitRequest.setLanguage(request.getLanguage());
        submitRequest.setCode(request.getCode());
        submitRequest.setTestCases(request.getTestCases());
        submitRequest.setPersistResult(false);
        return distributedJudgeService.submit(request.getUserId(), submitRequest);
    }

    public LoadTestResponse startLoadTest(LoadTestRequest request) {
        int users = clamp(request.getUsers(), 1, 1000);
        int submissionsPerUser = clamp(request.getSubmissionsPerUser(), 1, 20);
        int requestedTasks = users * submissionsPerUser;
        List<String> taskIds = new ArrayList<>(requestedTasks);
        int rejected = 0;

        for (int userIndex = 0; userIndex < users; userIndex++) {
            long userId = 800000L + userIndex;
            for (int submissionIndex = 0; submissionIndex < submissionsPerUser; submissionIndex++) {
                DistributedJudgeSubmitRequest submitRequest = new DistributedJudgeSubmitRequest();
                submitRequest.setExamId(request.getExamId());
                submitRequest.setQuestionId(request.getQuestionIdStart() + submissionIndex);
                submitRequest.setLanguage(request.getLanguage());
                submitRequest.setCode(request.getCode());
                submitRequest.setPersistResult(false);
                TestCase testCase = new TestCase();
                testCase.setInput(request.getInput());
                testCase.setExpectedOutput(request.getExpectedOutput());
                submitRequest.setTestCases(List.of(testCase));
                try {
                    JudgeTaskResponse response = distributedJudgeService.submit(userId, submitRequest);
                    taskIds.add(response.getTaskId());
                } catch (BusinessException ex) {
                    rejected++;
                }
            }
        }

        LoadTestResponse response = new LoadTestResponse();
        response.setRequestedTasks(requestedTasks);
        response.setAcceptedTasks(taskIds.size());
        response.setRejectedTasks(rejected);
        response.setStartedAt(LocalDateTime.now());
        response.setTaskIds(taskIds);
        return response;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
