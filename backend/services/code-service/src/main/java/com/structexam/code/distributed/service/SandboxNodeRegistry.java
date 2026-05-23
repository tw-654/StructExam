package com.structexam.code.distributed.service;

import com.structexam.code.distributed.config.DistributedJudgeProperties;
import com.structexam.code.distributed.dto.SandboxNodeView;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class SandboxNodeRegistry {

    private final DiscoveryClient discoveryClient;
    private final DistributedJudgeProperties properties;
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private final Map<String, NodeState> localStates = new ConcurrentHashMap<>();

    public SandboxNodeRegistry(ObjectProvider<DiscoveryClient> discoveryClientProvider,
                               DistributedJudgeProperties properties) {
        this.discoveryClient = discoveryClientProvider.getIfAvailable();
        this.properties = properties;
    }

    public List<ServiceInstance> healthyInstances() {
        List<ServiceInstance> instances = new ArrayList<>();
        if (discoveryClient == null) {
            addLocalSandboxNode(instances);
            return instances.stream().filter(this::canAcceptTask).collect(Collectors.toList());
        }
        instances.addAll(discoveryClient.getInstances(properties.getSandboxServiceName()));
        addLocalSandboxNode(instances);
        return instances.stream()
                .filter(this::canAcceptTask)
                .collect(Collectors.toList());
    }

    public Optional<ServiceInstance> selectRoundRobin() {
        List<ServiceInstance> nodes = healthyInstances();
        if (nodes.isEmpty()) {
            return Optional.empty();
        }
        int index = Math.floorMod(roundRobinIndex.getAndIncrement(), nodes.size());
        return Optional.of(nodes.get(index));
    }

    public Optional<ServiceInstance> selectLeastTasks() {
        return healthyInstances().stream()
                .min(Comparator.comparingInt(instance -> state(instance).runningTasks.get()));
    }

    public boolean hasHealthyNodeIgnoringLoad() {
        List<ServiceInstance> instances = new ArrayList<>();
        if (discoveryClient != null) {
            instances.addAll(discoveryClient.getInstances(properties.getSandboxServiceName()));
        }
        addLocalSandboxNode(instances);
        return instances.stream().anyMatch(this::isLocallyHealthy);
    }

    public void incrementRunningTasks(ServiceInstance instance) {
        state(instance).runningTasks.incrementAndGet();
    }

    public void decrementRunningTasks(ServiceInstance instance) {
        state(instance).runningTasks.updateAndGet(value -> Math.max(0, value - 1));
    }

    public void markSuccess(ServiceInstance instance) {
        NodeState state = state(instance);
        state.failureCount.set(0);
        state.unhealthyUntil = null;
    }

    public void markFailure(ServiceInstance instance) {
        NodeState state = state(instance);
        int failures = state.failureCount.incrementAndGet();
        if (failures >= properties.getNodeFailureThreshold()) {
            state.unhealthyUntil = Instant.now().plus(properties.getNodeRecoveryProbeInterval());
        }
    }

    public List<SandboxNodeView> nodeViews() {
        List<ServiceInstance> instances = new ArrayList<>();
        if (discoveryClient == null) {
            addLocalSandboxNode(instances);
            return instances.stream().map(this::toView).collect(Collectors.toList());
        }
        instances.addAll(discoveryClient.getInstances(properties.getSandboxServiceName()));
        addLocalSandboxNode(instances);
        return instances.stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    private SandboxNodeView toView(ServiceInstance instance) {
        NodeState state = state(instance);
        SandboxNodeView view = new SandboxNodeView();
        view.setServiceId(instance.getServiceId());
        view.setHost(instance.getHost());
        view.setPort(instance.getPort());
        view.setUri(instance.getUri());
        view.setHealthy(isLocallyHealthy(instance));
        view.setRunningTasks(state.runningTasks.get());
        view.setMetadata(instance.getMetadata());
        return view;
    }

    private boolean isLocallyHealthy(ServiceInstance instance) {
        NodeState state = state(instance);
        return state.unhealthyUntil == null || Instant.now().isAfter(state.unhealthyUntil);
    }

    private boolean canAcceptTask(ServiceInstance instance) {
        if (!isLocallyHealthy(instance)) {
            return false;
        }
        int maxConcurrency = maxConcurrency(instance);
        return maxConcurrency <= 0 || state(instance).runningTasks.get() < maxConcurrency;
    }

    private int maxConcurrency(ServiceInstance instance) {
        String value = instance.getMetadata().get("maxConcurrency");
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void addLocalSandboxNode(List<ServiceInstance> instances) {
        if (!properties.isLocalSandboxNodeEnabled()) {
            return;
        }
        instances.add(new StaticSandboxServiceInstance(
                properties.getSandboxServiceName(),
                properties.getLocalSandboxNodeHost(),
                properties.getLocalSandboxNodePort(),
                Map.of(
                        "mode", "local-code-service",
                        "maxConcurrency", String.valueOf(properties.getLocalSandboxNodeMaxConcurrency()),
                        "runPath", properties.getSandboxRunPath(),
                        "wsPath", properties.getSandboxWebSocketPath()
                )
        ));
    }

    private NodeState state(ServiceInstance instance) {
        return localStates.computeIfAbsent(nodeKey(instance), ignored -> new NodeState());
    }

    private String nodeKey(ServiceInstance instance) {
        return instance.getServiceId() + ":" + instance.getHost() + ":" + instance.getPort();
    }

    private static class NodeState {
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger runningTasks = new AtomicInteger(0);
        private volatile Instant unhealthyUntil;
    }
}
