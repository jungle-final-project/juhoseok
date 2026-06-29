package com.buildgraph.prototype.ticket;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TicketController {
    private final TicketQueryService ticketQueryService;

    public TicketController(TicketQueryService ticketQueryService) {
        this.ticketQueryService = ticketQueryService;
    }

    @PostMapping("/as-tickets")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@RequestBody(required = false) Map<String, Object> request) {
        return ticketQueryService.create(request);
    }

    @GetMapping("/as-tickets/{id}")
    Map<String, Object> ticket(@PathVariable String id) {
        return ticketQueryService.ticket(id);
    }
}
