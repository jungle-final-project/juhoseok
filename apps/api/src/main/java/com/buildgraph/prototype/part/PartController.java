package com.buildgraph.prototype.part;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class PartController {
    private final PartQueryService partQueryService;
    private final NaverShoppingOfferService naverShoppingOfferService;

    public PartController(PartQueryService partQueryService, NaverShoppingOfferService naverShoppingOfferService) {
        this.partQueryService = partQueryService;
        this.naverShoppingOfferService = naverShoppingOfferService;
    }

    @GetMapping("/parts")
    Map<String, Object> parts(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "manufacturer", required = false) String manufacturer,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "minPrice", required = false) Integer minPrice,
            @RequestParam(value = "maxPrice", required = false) Integer maxPrice,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        return partQueryService.parts(category, query, manufacturer, status, minPrice, maxPrice, page, size, sort);
    }

    @GetMapping("/parts/{id}")
    Map<String, Object> part(@PathVariable String id) {
        return partQueryService.part(id);
    }

    @GetMapping("/parts/{id}/price-history")
    Map<String, Object> priceHistory(
            @PathVariable String id,
            @RequestParam(value = "days", required = false) Integer days,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return partQueryService.priceHistory(id, days, source, limit);
    }

    @PostMapping("/admin/parts/external-offers/refresh")
    Map<String, Object> refreshExternalOffers(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "force", required = false) Boolean force,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);
        return naverShoppingOfferService.refreshOffers(category, limit, force);
    }

    @PostMapping("/admin/parts/catalog/refresh")
    Map<String, Object> refreshCatalog(
            @RequestParam(value = "category") String category,
            @RequestParam(value = "limitPerQuery", required = false) Integer limitPerQuery,
            @RequestParam(value = "publish", required = false) Boolean publish,
            @RequestParam(value = "q", required = false) String query,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);
        return naverShoppingOfferService.refreshCatalog(category, limitPerQuery, publish, query);
    }

    @PostMapping("/tools/compatibility/check")
    Map<String, Object> compatibility(@RequestBody(required = false) Map<String, Object> request) {
        return tool("compatibility", request);
    }

    @PostMapping("/tools/power/check")
    Map<String, Object> power(@RequestBody(required = false) Map<String, Object> request) {
        return tool("power", request);
    }

    @PostMapping("/tools/size/check")
    Map<String, Object> size(@RequestBody(required = false) Map<String, Object> request) {
        return tool("size", request);
    }

    @PostMapping("/tools/performance/check")
    Map<String, Object> performance(@RequestBody(required = false) Map<String, Object> request) {
        return tool("performance", request);
    }

    @PostMapping("/tools/price/check")
    Map<String, Object> price(@RequestBody(required = false) Map<String, Object> request) {
        return tool("price", request);
    }

    private Map<String, Object> tool(String tool, Map<String, Object> request) {
        return partQueryService.toolResult(tool);
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
