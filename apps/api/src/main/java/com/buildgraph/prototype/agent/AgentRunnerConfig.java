package com.buildgraph.prototype.agent;

import com.buildgraph.prototype.part.ToolCheckService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentRunnerConfig {
    @Bean
    AgentRunner agentRunner(
            AgentTraceService agentTraceService,
            AgentRagRetrievalService agentRagRetrievalService,
            OpenAiResponsesClient openAiResponsesClient,
            ToolCheckService toolCheckService,
            @Value("${agent.runner.mode:deterministic}") String runnerMode
    ) {
        String normalizedMode = runnerMode == null ? "deterministic" : runnerMode.trim().toLowerCase();
        return switch (normalizedMode) {
            case "deterministic" -> new DeterministicAgentRunner(agentTraceService, agentRagRetrievalService, toolCheckService);
            case "llm" -> new LlmAgentRunner(agentTraceService, agentRagRetrievalService, openAiResponsesClient, toolCheckService);
            default -> throw new IllegalArgumentException("지원하지 않는 AGENT_RUNNER_MODE입니다: " + runnerMode);
        };
    }
}
