package com.buildgraph.prototype.agent;

import java.util.List;

public record AgentRunProfile(
        AgentPurpose purpose,
        List<String> ragSourceTypes,
        List<String> toolNames,
        String summaryTarget
) {
}
