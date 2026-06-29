package com.buildgraph.prototype.agent;

public interface AgentRunner {
    void run(String sessionId, AgentSessionRoot root, AgentRunProfile profile);
}
