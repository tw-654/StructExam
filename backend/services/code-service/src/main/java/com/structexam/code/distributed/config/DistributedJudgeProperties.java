package com.structexam.code.distributed.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "distributed.judge")
public class DistributedJudgeProperties {

    private String queueKey = "judge:queue";
    private String processingQueueKey = "judge:queue:processing";
    private String resultKeyPrefix = "judge:result:";
    private String taskSummaryKeyPrefix = "judge:task:";
    private String recentTaskListKey = "judge:recent:tasks";
    private String lockKeyPrefix = "lock:judge:";
    private String examSubmitLockKeyPrefix = "lock:exam:submit:";
    private String sandboxServiceName = "sandbox-node";
    private Duration judgeLockTtl = Duration.ofMinutes(10);
    private Duration examSubmitLockTtl = Duration.ofSeconds(5);
    private Duration resultTtl = Duration.ofHours(4);
    private int recentTaskLimit = 50;
    private Duration nodeRecoveryProbeInterval = Duration.ofSeconds(30);
    private Duration schedulerDelay = Duration.ofMillis(100);
    private int maxRetryCount = 3;
    private int nodeFailureThreshold = 3;
    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 10000;
    private long defaultRunTimeoutSeconds = 5L;
    private boolean schedulerEnabled = true;
    private int dispatcherThreads = 8;
    private int schedulerBatchSize = 8;
    private String sandboxRunPath = "/sandbox/run";
    private String sandboxWebSocketPath = "/ws/sandbox";
    private boolean localSandboxNodeEnabled = false;
    private String localSandboxNodeHost = "localhost";
    private int localSandboxNodePort = 8083;
    private int localSandboxNodeMaxConcurrency = 4;
    private String loadBalanceStrategy = "roundRobin";

    public String getLoadBalanceStrategy() {
        return loadBalanceStrategy;
    }

    public void setLoadBalanceStrategy(String loadBalanceStrategy) {
        this.loadBalanceStrategy = loadBalanceStrategy;
    }

    public String getQueueKey() {
        return queueKey;
    }

    public void setQueueKey(String queueKey) {
        this.queueKey = queueKey;
    }

    public String getProcessingQueueKey() {
        return processingQueueKey;
    }

    public void setProcessingQueueKey(String processingQueueKey) {
        this.processingQueueKey = processingQueueKey;
    }

    public String getResultKeyPrefix() {
        return resultKeyPrefix;
    }

    public void setResultKeyPrefix(String resultKeyPrefix) {
        this.resultKeyPrefix = resultKeyPrefix;
    }

    public String getTaskSummaryKeyPrefix() {
        return taskSummaryKeyPrefix;
    }

    public void setTaskSummaryKeyPrefix(String taskSummaryKeyPrefix) {
        this.taskSummaryKeyPrefix = taskSummaryKeyPrefix;
    }

    public String getRecentTaskListKey() {
        return recentTaskListKey;
    }

    public void setRecentTaskListKey(String recentTaskListKey) {
        this.recentTaskListKey = recentTaskListKey;
    }

    public String getLockKeyPrefix() {
        return lockKeyPrefix;
    }

    public void setLockKeyPrefix(String lockKeyPrefix) {
        this.lockKeyPrefix = lockKeyPrefix;
    }

    public String getExamSubmitLockKeyPrefix() {
        return examSubmitLockKeyPrefix;
    }

    public void setExamSubmitLockKeyPrefix(String examSubmitLockKeyPrefix) {
        this.examSubmitLockKeyPrefix = examSubmitLockKeyPrefix;
    }

    public String getSandboxServiceName() {
        return sandboxServiceName;
    }

    public void setSandboxServiceName(String sandboxServiceName) {
        this.sandboxServiceName = sandboxServiceName;
    }

    public Duration getJudgeLockTtl() {
        return judgeLockTtl;
    }

    public void setJudgeLockTtl(Duration judgeLockTtl) {
        this.judgeLockTtl = judgeLockTtl;
    }

    public Duration getExamSubmitLockTtl() {
        return examSubmitLockTtl;
    }

    public void setExamSubmitLockTtl(Duration examSubmitLockTtl) {
        this.examSubmitLockTtl = examSubmitLockTtl;
    }

    public Duration getResultTtl() {
        return resultTtl;
    }

    public void setResultTtl(Duration resultTtl) {
        this.resultTtl = resultTtl;
    }

    public int getRecentTaskLimit() {
        return recentTaskLimit;
    }

    public void setRecentTaskLimit(int recentTaskLimit) {
        this.recentTaskLimit = recentTaskLimit;
    }

    public Duration getNodeRecoveryProbeInterval() {
        return nodeRecoveryProbeInterval;
    }

    public void setNodeRecoveryProbeInterval(Duration nodeRecoveryProbeInterval) {
        this.nodeRecoveryProbeInterval = nodeRecoveryProbeInterval;
    }

    public Duration getSchedulerDelay() {
        return schedulerDelay;
    }

    public void setSchedulerDelay(Duration schedulerDelay) {
        this.schedulerDelay = schedulerDelay;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public int getNodeFailureThreshold() {
        return nodeFailureThreshold;
    }

    public void setNodeFailureThreshold(int nodeFailureThreshold) {
        this.nodeFailureThreshold = nodeFailureThreshold;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public long getDefaultRunTimeoutSeconds() {
        return defaultRunTimeoutSeconds;
    }

    public void setDefaultRunTimeoutSeconds(long defaultRunTimeoutSeconds) {
        this.defaultRunTimeoutSeconds = defaultRunTimeoutSeconds;
    }

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean schedulerEnabled) {
        this.schedulerEnabled = schedulerEnabled;
    }

    public int getDispatcherThreads() {
        return dispatcherThreads;
    }

    public void setDispatcherThreads(int dispatcherThreads) {
        this.dispatcherThreads = dispatcherThreads;
    }

    public int getSchedulerBatchSize() {
        return schedulerBatchSize;
    }

    public void setSchedulerBatchSize(int schedulerBatchSize) {
        this.schedulerBatchSize = schedulerBatchSize;
    }

    public String getSandboxRunPath() {
        return sandboxRunPath;
    }

    public void setSandboxRunPath(String sandboxRunPath) {
        this.sandboxRunPath = sandboxRunPath;
    }

    public String getSandboxWebSocketPath() {
        return sandboxWebSocketPath;
    }

    public void setSandboxWebSocketPath(String sandboxWebSocketPath) {
        this.sandboxWebSocketPath = sandboxWebSocketPath;
    }

    public boolean isLocalSandboxNodeEnabled() {
        return localSandboxNodeEnabled;
    }

    public void setLocalSandboxNodeEnabled(boolean localSandboxNodeEnabled) {
        this.localSandboxNodeEnabled = localSandboxNodeEnabled;
    }

    public String getLocalSandboxNodeHost() {
        return localSandboxNodeHost;
    }

    public void setLocalSandboxNodeHost(String localSandboxNodeHost) {
        this.localSandboxNodeHost = localSandboxNodeHost;
    }

    public int getLocalSandboxNodePort() {
        return localSandboxNodePort;
    }

    public void setLocalSandboxNodePort(int localSandboxNodePort) {
        this.localSandboxNodePort = localSandboxNodePort;
    }

    public int getLocalSandboxNodeMaxConcurrency() {
        return localSandboxNodeMaxConcurrency;
    }

    public void setLocalSandboxNodeMaxConcurrency(int localSandboxNodeMaxConcurrency) {
        this.localSandboxNodeMaxConcurrency = localSandboxNodeMaxConcurrency;
    }
}
