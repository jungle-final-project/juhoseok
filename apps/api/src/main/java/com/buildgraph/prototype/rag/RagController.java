package com.buildgraph.prototype.rag;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RagController {
    private final RagQueryService ragQueryService;

    public RagController(RagQueryService ragQueryService) {
        this.ragQueryService = ragQueryService;
    }

    @GetMapping("/rag/search")
    Map<String, Object> search(@RequestParam(value = "q", required = false) String query) {
        return ragQueryService.search(query);
    }

    @GetMapping("/rag/evidence/{id}")
    Map<String, Object> evidence(@PathVariable String id) {
        return ragQueryService.evidence(id);
    }
}
