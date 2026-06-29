package com.buildgraph.prototype.admin;

import com.buildgraph.prototype.agent.AgentQueryService;
import com.buildgraph.prototype.price.PriceQueryService;
import com.buildgraph.prototype.rag.RagQueryService;
import com.buildgraph.prototype.ticket.TicketQueryService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminQueryService adminQueryService;
    private final AgentQueryService agentQueryService;
    private final RagQueryService ragQueryService;
    private final TicketQueryService ticketQueryService;
    private final PriceQueryService priceQueryService;

    public AdminController(
            AdminQueryService adminQueryService,
            AgentQueryService agentQueryService,
            RagQueryService ragQueryService,
            TicketQueryService ticketQueryService,
            PriceQueryService priceQueryService
    ) {
        this.adminQueryService = adminQueryService;
        this.agentQueryService = agentQueryService;
        this.ragQueryService = ragQueryService;
        this.ticketQueryService = ticketQueryService;
        this.priceQueryService = priceQueryService;
    }

    @GetMapping("/dashboard")
    Map<String, Object> dashboard(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return adminQueryService.dashboard();
    }

    @GetMapping("/audit-logs/recent")
    Map<String, Object> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return adminQueryService.auditLogs();
    }

    @GetMapping("/agent-sessions")
    Map<String, Object> agentSessions(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return agentQueryService.agentSessions();
    }

    @GetMapping("/agent-sessions/{id}")
    Map<String, Object> agentSession(@PathVariable String id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return agentQueryService.adminSession(id);
    }

    @GetMapping("/tool-invocations")
    Map<String, Object> toolInvocations(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return agentQueryService.toolInvocations();
    }

    @GetMapping("/tool-invocations/{id}")
    Map<String, Object> toolInvocation(@PathVariable String id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return agentQueryService.toolInvocation(id);
    }

    @GetMapping("/rag-evidence/{id}")
    Map<String, Object> ragEvidence(@PathVariable String id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return ragQueryService.adminEvidence(id);
    }

    @GetMapping("/as-tickets")
    Map<String, Object> tickets(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return Map.of("items", ticketQueryService.tickets());
    }

    @GetMapping("/as-tickets/{id}")
    Map<String, Object> ticket(@PathVariable String id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return ticketQueryService.ticket(id);
    }

    @PatchMapping("/as-tickets/{id}")
    Map<String, Object> updateTicket(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);
        return ticketQueryService.update(id, request);
    }

    @GetMapping("/price-jobs")
    Map<String, Object> priceJobs(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return priceQueryService.priceJobs();
    }

    @PostMapping("/price-jobs/run")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> runPriceJob(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return priceQueryService.runPriceJob();
    }

    private static void requireAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer demo-access-")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (!authorization.contains("demo-access-admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
    }
}
