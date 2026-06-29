package com.buildgraph.prototype.agent;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AgentController {
    private final AgentQueryService agentQueryService;

    public AgentController(AgentQueryService agentQueryService) {
        this.agentQueryService = agentQueryService;
    }

    @PostMapping("/agent/sessions")
    Map<String, Object> createSession(@RequestBody(required = false) AgentSessionCreateRequest request) {
        return agentQueryService.createSession(request);
    }

    @PostMapping("/agent/sessions/{id}/run")
    Map<String, Object> runSession(@PathVariable String id) {
        return agentQueryService.runSession(id);
    }

    @GetMapping("/agent/sessions/{id}")
    Map<String, Object> getSession(@PathVariable String id) {
        return agentQueryService.session(id);
    }
}
