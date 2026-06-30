package com.buildgraph.prototype.build;

import com.buildgraph.prototype.agent.AgentRunProfile;
import com.buildgraph.prototype.agent.AgentRunProfiles;
import com.buildgraph.prototype.agent.AgentRagEvidenceDraft;
import com.buildgraph.prototype.agent.AgentRagRetrievalService;
import com.buildgraph.prototype.agent.AgentRunner;
import com.buildgraph.prototype.agent.AgentSessionRoot;
import com.buildgraph.prototype.agent.AgentSessionRootType;
import com.buildgraph.prototype.agent.AgentStatus;
import com.buildgraph.prototype.agent.AgentTraceService;
import com.buildgraph.prototype.agent.OpenAiResponsesClient;
import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import com.buildgraph.prototype.part.ToolBuildPart;
import com.buildgraph.prototype.part.ToolCheckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BuildQueryService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern BUDGET_MANWON = Pattern.compile("([0-9]{2,4})\\s*만\\s*원?");
    private static final Pattern BUDGET_NUMBER = Pattern.compile("([0-9][0-9,]{5,})\\s*원?");
    private static final String REQUIREMENT_PARSE_SYSTEM_PROMPT = """
            당신은 BuildGraph AI의 PC 요구사항 파싱 Agent입니다.
            RAG 근거와 사용자 입력만 사용해 추천 전에 필요한 구조화 JSON을 만드십시오.
            확인되지 않은 예산, 해상도, 제조사, 용도는 지어내지 말고 null 또는 빈 배열로 두십시오.
            usageTags는 GAMING, DEVELOPMENT, VIDEO_EDIT, AI_DEV, GENERAL 중에서만 고르십시오.
            performanceTier는 ENTHUSIAST, PERFORMANCE, STANDARD 중 하나를 고르십시오.
            budgetPolicy는 USER_BUDGET, OPEN_BUDGET, DEFAULT_BUDGET 중 하나를 고르십시오.
            performanceTier와 budgetPolicy는 RAG 근거와 사용자 입력으로 판단하고, 근거가 부족하면 STANDARD와 DEFAULT_BUDGET을 사용하십시오.
            mustHave는 WIFI, LOW_NOISE 중 확인된 조건만 넣으십시오.
            confidence 값은 LOW, MEDIUM, HIGH 중 하나만 쓰십시오.
            응답은 설명 문장 없이 JSON object 하나만 반환하십시오.
            """;
    private static final int DEFAULT_BUDGET = 2_000_000;
    private static final int ENTHUSIAST_OPEN_BUDGET = 12_000_000;
    private static final List<String> BUILD_CATEGORIES = List.of(
            "CPU", "MOTHERBOARD", "RAM", "GPU", "STORAGE", "PSU", "CASE", "COOLER"
    );
    private static final List<BuildPlan> DEFAULT_BUILD_PLANS = List.of(
            new BuildPlan("가성비형 추천 Build", "예산 안에서 핵심 성능을 먼저 확보", 0, 0.78, "MEDIUM"),
            new BuildPlan("균형형 추천 Build", "게임, 개발, 안정성을 균형 있게 반영", 1, 0.96, "HIGH"),
            new BuildPlan("고성능형 추천 Build", "성능 우선 조건과 업그레이드 여유 확보", 2, 1.14, "MEDIUM")
    );
    private static final List<BuildPlan> ENTHUSIAST_BUILD_PLANS = List.of(
            new BuildPlan("끝판왕 추천 Build", "예산 제한 없이 내부 자산의 최상위 성능을 우선", 3, 1.14, "HIGH"),
            new BuildPlan("하이엔드 균형 Build", "플래그십 성능과 구성 안정성을 균형 있게 확보", 2, 0.96, "HIGH"),
            new BuildPlan("프리미엄 안정 Build", "과한 지출을 줄이되 하이엔드 체급을 유지", 1, 0.78, "MEDIUM")
    );

    private final JdbcTemplate jdbcTemplate;
    private final AgentTraceService agentTraceService;
    private final AgentRunner agentRunner;
    private final AgentRagRetrievalService agentRagRetrievalService;
    private final OpenAiResponsesClient openAiResponsesClient;
    private final ToolCheckService toolCheckService;

    public BuildQueryService(
            JdbcTemplate jdbcTemplate,
            AgentTraceService agentTraceService,
            AgentRunner agentRunner,
            AgentRagRetrievalService agentRagRetrievalService,
            OpenAiResponsesClient openAiResponsesClient,
            ToolCheckService toolCheckService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.agentTraceService = agentTraceService;
        this.agentRunner = agentRunner;
        this.agentRagRetrievalService = agentRagRetrievalService;
        this.openAiResponsesClient = openAiResponsesClient;
        this.toolCheckService = toolCheckService;
    }

    public Map<String, Object> parse(Map<String, Object> request) {
        Map<String, Object> body = request == null ? Map.of() : request;
        String message = text(body.get("message"));
        if (message == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자연어 요구사항은 필수입니다.");
        }

        Map<String, Object> fallbackContext = deterministicParsedContext(body, message);
        Integer fallbackBudget = numberValue(fallbackContext.get("budget"));
        List<String> fallbackUsageTags = stringList(fallbackContext.get("usageTags"));
        Map<String, Object> pendingContext = new LinkedHashMap<>(fallbackContext);
        pendingContext.put("parseMode", "RAG_PENDING");
        pendingContext.put("parser", "requirement-parse-agent-v1");
        String id = jdbcTemplate.queryForObject("""
                INSERT INTO requirements (user_id, raw_message, budget, usage_tags, parsed_context)
                VALUES (
                  (SELECT id FROM users WHERE email = 'user@example.com'),
                  ?,
                  ?,
                  string_to_array(?, ','),
                  ?::jsonb
                )
                RETURNING public_id::text
                """, String.class, message, fallbackBudget, String.join(",", fallbackUsageTags), json(pendingContext));

        RequirementParseResult parseResult = runRequirementParseAgent(id, message, body, fallbackContext);
        Map<String, Object> parsedContext = parseResult.parsedContext();
        Integer budget = numberValue(parsedContext.get("budget"));
        List<String> usageTags = stringList(parsedContext.get("usageTags"));
        jdbcTemplate.update("""
                UPDATE requirements
                SET budget = ?,
                    usage_tags = string_to_array(?, ','),
                    parsed_context = ?::jsonb
                WHERE public_id = ?::uuid
                """, budget, String.join(",", usageTags), json(parsedContext), id);

        return MockData.map(
                "id", id,
                "rawMessage", message,
                "budget", budget,
                "usageTags", usageTags,
                "parsedContext", parsedContext,
                "questions", questions(parsedContext),
                "agentSessionId", parseResult.agentSessionId(),
                "agentSummary", parseResult.agentSummary(),
                "evidenceIds", parseResult.evidenceIds()
        );
    }

    public Map<String, Object> recommendations(Map<String, Object> request) {
        String requirementId = text(request == null ? null : request.get("requirementId"));
        if (requirementId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requirementId가 필요합니다.");
        }
        RequirementRow requirement = requirement(requirementId);
        Map<String, Object> answers = objectMap(request == null ? null : request.get("answers"));
        int budget = effectiveBudget(requirement, answers);
        List<BuildPlan> buildPlans = buildPlansFor(requirement);
        List<String> createdBuildIds = new ArrayList<>();
        for (BuildPlan plan : buildPlans) {
            List<PartCandidate> parts = selectBuildParts(requirement, answers, plan, budget);
            List<Map<String, Object>> toolResults = evaluateTools(parts, budget);
            List<Map<String, Object>> warnings = warningsFor(toolResults, total(parts), budget);
            createdBuildIds.add(insertBuild(requirement.internalId(), plan, parts, warnings));
        }

        String agentSessionId = runAgent(new AgentSessionRoot(AgentSessionRootType.REQUIREMENT, requirement.publicId()));
        List<Map<String, Object>> recommendations = createdBuildIds.stream()
                .map(this::buildDetail)
                .map(build -> with(build, "agentSessionId", agentSessionId))
                .toList();
        return MockData.map(
                "agentSessionId", agentSessionId,
                "recommendations", recommendations,
                "warnings", combinedWarnings(recommendations),
                "evidenceIds", agentSessionId == null ? List.of() : evidenceIdsBySession(agentSessionId),
                "toolResults", agentSessionId == null ? List.of() : toolResultsBySession(agentSessionId)
        );
    }

    public List<Map<String, Object>> builds() {
        return jdbcTemplate.queryForList("""
                        SELECT public_id::text AS id, name, total_price, confidence, warnings, created_at
                        FROM builds
                        ORDER BY created_at DESC, id DESC
                        LIMIT 30
                        """)
                .stream()
                .map(this::buildSummary)
                .toList();
    }

    public Map<String, Object> buildDetail(String id) {
        Map<String, Object> row = buildRow(id);
        Map<String, Object> summary = buildSummary(row);
        return MockData.map(
                "id", summary.get("id"),
                "name", summary.get("name"),
                "recommendedFor", summary.get("recommendedFor"),
                "summary", summary.get("summary"),
                "totalPrice", summary.get("totalPrice"),
                "confidence", summary.get("confidence"),
                "items", summary.get("items"),
                "warnings", summary.get("warnings"),
                "evidenceIds", summary.get("evidenceIds"),
                "agentSessionId", summary.get("agentSessionId"),
                "agentSummary", summary.get("agentSummary"),
                "changeableCategories", summary.get("changeableCategories"),
                "createdAt", summary.get("createdAt"),
                "toolResults", summary.get("toolResults")
        );
    }

    public Map<String, Object> changePart(String id, Map<String, Object> request) {
        String category = normalizeCategory(text(request == null ? null : request.get("category")));
        String selectedPartId = text(request == null ? null : request.get("partId"));
        if (selectedPartId == null) {
            selectedPartId = defaultPartId(category);
        }
        if (selectedPartId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "변경할 부품을 찾을 수 없습니다.");
        }

        Map<String, Object> beforeBuild = buildDetail(id);
        List<PartCandidate> beforeParts = buildPartCandidates(id);
        PartCandidate selectedPart = partByPublicId(selectedPartId);
        List<PartCandidate> afterParts = replaceCategory(beforeParts, category, selectedPart);
        int beforeTotal = total(beforeParts);
        int afterTotal = total(afterParts);
        List<Map<String, Object>> toolResults = evaluateTools(afterParts, afterTotal);
        String agentSessionId = runAgent(new AgentSessionRoot(AgentSessionRootType.BUILD, id));
        Map<String, Object> agentSession = agentSessionId == null ? Map.of() : agentSession(agentSessionId);

        return MockData.map(
                "buildId", id,
                "category", category,
                "previousPartId", previousPartId(beforeParts, category),
                "selectedPartId", selectedPart.publicId(),
                "totalPrice", afterTotal,
                "diff", MockData.map("price", afterTotal - beforeTotal),
                "beforeBuild", beforeBuild,
                "afterBuild", MockData.map(
                        "id", id,
                        "name", beforeBuild.get("name"),
                        "totalPrice", afterTotal,
                        "items", afterParts.stream().map(this::partItem).toList()
                ),
                "diffRows", diffRows(beforeParts, afterParts, category, beforeTotal, afterTotal),
                "toolResults", toolResults,
                "agentSessionId", agentSessionId,
                "agentSummary", agentSession.get("summary"),
                "warnings", warningsFor(toolResults, afterTotal, afterTotal)
        );
    }

    private RequirementRow requirement(String publicId) {
        return jdbcTemplate.queryForList("""
                        SELECT id AS internal_id,
                               public_id::text AS id,
                               raw_message,
                               budget,
                               coalesce(array_to_string(usage_tags, ','), '') AS usage_tags,
                               parsed_context
                        FROM requirements
                        WHERE public_id = ?::uuid
                        """, publicId)
                .stream()
                .findFirst()
                .map(row -> new RequirementRow(
                        numberLong(row.get("internal_id")),
                        DbValueMapper.string(row, "id"),
                        DbValueMapper.string(row, "raw_message"),
                        DbValueMapper.integer(row, "budget"),
                        csv(DbValueMapper.string(row, "usage_tags")),
                        objectMap(DbValueMapper.json(row, "parsed_context", Map.of()))
                ))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요구사항을 찾을 수 없습니다."));
    }

    private String insertBuild(Long requirementInternalId, BuildPlan plan, List<PartCandidate> parts, List<Map<String, Object>> warnings) {
        int totalPrice = total(parts);
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                INSERT INTO builds (requirement_id, name, total_price, confidence, warnings)
                VALUES (?, ?, ?, ?, ?::jsonb)
                RETURNING id, public_id::text AS public_id
                """, requirementInternalId, plan.name(), totalPrice, plan.confidence(), json(warnings));
        Long buildInternalId = numberLong(row.get("id"));
        for (PartCandidate part : parts) {
            jdbcTemplate.update("""
                    INSERT INTO build_items (build_id, part_id, category, price)
                    VALUES (?, ?, ?, ?)
                    """, buildInternalId, part.internalId(), part.category(), part.price());
        }
        return DbValueMapper.string(row, "public_id");
    }

    private List<PartCandidate> selectBuildParts(
            RequirementRow requirement,
            Map<String, Object> answers,
            BuildPlan plan,
            int budget
    ) {
        List<PartCandidate> cpuCandidates = partCandidates("CPU");
        PartCandidate cpu = chooseByTarget(cpuCandidates, target(budget, plan, 0.17));
        String socket = stringAttr(cpu, "socket");
        List<PartCandidate> motherboardCandidates = filter(partCandidates("MOTHERBOARD"),
                part -> socket == null || socket.equalsIgnoreCase(stringAttr(part, "socket")));
        PartCandidate motherboard = chooseByTarget(orFallback(motherboardCandidates, partCandidates("MOTHERBOARD")), target(budget, plan, 0.11));
        String memoryType = firstText(stringAttr(motherboard, "memoryType"), "DDR5");
        List<PartCandidate> ramCandidates = filter(partCandidates("RAM"),
                part -> memoryType.equalsIgnoreCase(firstText(stringAttr(part, "memoryType"), memoryType)));
        PartCandidate ram = chooseByTarget(orFallback(ramCandidates, partCandidates("RAM")), target(budget, plan, 0.07));
        PartCandidate gpu = chooseByTarget(partCandidates("GPU"), target(budget, plan, 0.39));
        PartCandidate storage = chooseByTarget(partCandidates("STORAGE"), target(budget, plan, 0.07));
        int recommendedPsu = Math.max(intAttr(gpu, "requiredSystemPowerW", 650), estimatedWattage(List.of(cpu, gpu)) + 180);
        List<PartCandidate> psuCandidates = filter(partCandidates("PSU"), part -> intAttr(part, "capacityW", 0) >= recommendedPsu);
        PartCandidate psu = chooseByTarget(orFallback(psuCandidates, partCandidates("PSU")), target(budget, plan, 0.08));
        int gpuLength = intAttr(gpu, "lengthMm", 0);
        List<PartCandidate> caseCandidates = filter(partCandidates("CASE"), part -> intAttr(part, "maxGpuLengthMm", 0) >= gpuLength + 20);
        PartCandidate pcCase = chooseByTarget(orFallback(caseCandidates, partCandidates("CASE")), target(budget, plan, 0.06));
        int cpuTdp = intAttr(cpu, "tdpW", intAttr(cpu, "wattage", 120));
        List<PartCandidate> coolerCandidates = filter(partCandidates("COOLER"), part ->
                intAttr(part, "tdpW", 0) >= cpuTdp
                        && socketSupported(part, socket)
                        && intAttr(part, "heightMm", intAttr(part, "coolerHeightMm", 0)) <= intAttr(pcCase, "maxCpuCoolerHeightMm", 190)
        );
        PartCandidate cooler = chooseByTarget(orFallback(coolerCandidates, partCandidates("COOLER")), target(budget, plan, 0.05));
        return List.of(cpu, motherboard, ram, gpu, storage, psu, pcCase, cooler);
    }

    private List<Map<String, Object>> evaluateTools(List<PartCandidate> parts, int budget) {
        return toolCheckService.checkBuild(parts.stream().map(this::toolPart).toList(), budget);
    }

    private Map<String, Object> buildSummary(Map<String, Object> row) {
        String id = DbValueMapper.string(row, "id");
        List<PartCandidate> parts = buildPartCandidates(id);
        Integer totalPrice = DbValueMapper.integer(row, "total_price");
        int budget = budgetForBuild(id, totalPrice == null ? total(parts) : totalPrice);
        List<Map<String, Object>> toolResults = evaluateTools(parts, budget);
        String agentSessionId = agentSessionIdForBuild(id);
        Map<String, Object> agentSession = agentSessionId == null ? Map.of() : agentSession(agentSessionId);
        return MockData.map(
                "id", id,
                "name", DbValueMapper.string(row, "name"),
                "recommendedFor", recommendedFor(DbValueMapper.string(row, "name")),
                "summary", summaryText(DbValueMapper.string(row, "name")),
                "totalPrice", totalPrice,
                "confidence", DbValueMapper.string(row, "confidence"),
                "items", parts.stream().map(this::partItem).toList(),
                "warnings", DbValueMapper.json(row, "warnings", List.of()),
                "evidenceIds", agentSessionId == null ? evidenceIds(id) : evidenceIdsBySession(agentSessionId),
                "agentSessionId", agentSessionId,
                "agentSummary", agentSession.get("summary"),
                "changeableCategories", List.of("CPU", "GPU", "RAM", "STORAGE", "PSU", "CASE", "COOLER"),
                "createdAt", DbValueMapper.timestamp(row, "created_at"),
                "toolResults", toolResults
        );
    }

    private Map<String, Object> partItem(PartCandidate part) {
        return MockData.map(
                "id", part.publicId(),
                "partId", part.publicId(),
                "category", part.category(),
                "name", part.name(),
                "manufacturer", part.manufacturer(),
                "price", part.price(),
                "status", "ACTIVE",
                "attributes", part.attributes()
        );
    }

    private List<PartCandidate> buildPartCandidates(String buildId) {
        return jdbcTemplate.queryForList("""
                        SELECT p.id AS internal_id,
                               p.public_id::text AS id,
                               p.category,
                               p.name,
                               p.manufacturer,
                               bi.price,
                               p.attributes
                        FROM build_items bi
                        JOIN builds b ON b.id = bi.build_id
                        JOIN parts p ON p.id = bi.part_id
                        WHERE b.public_id = ?::uuid
                        ORDER BY bi.id
                        """, buildId)
                .stream()
                .map(this::partCandidate)
                .toList();
    }

    private List<PartCandidate> partCandidates(String category) {
        return jdbcTemplate.queryForList("""
                        SELECT id AS internal_id,
                               public_id::text AS id,
                               category,
                               name,
                               manufacturer,
                               price,
                               attributes
                        FROM parts
                        WHERE category = ?
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                          AND coalesce((attributes->>'toolReady')::boolean, false) = true
                        ORDER BY price ASC, id ASC
                        """, category)
                .stream()
                .map(this::partCandidate)
                .toList();
    }

    private PartCandidate partByPublicId(String publicId) {
        return jdbcTemplate.queryForList("""
                        SELECT id AS internal_id,
                               public_id::text AS id,
                               category,
                               name,
                               manufacturer,
                               price,
                               attributes
                        FROM parts
                        WHERE public_id = ?::uuid
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                        """, publicId)
                .stream()
                .findFirst()
                .map(this::partCandidate)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "변경 대상 부품을 찾을 수 없습니다."));
    }

    private PartCandidate partCandidate(Map<String, Object> row) {
        return new PartCandidate(
                numberLong(row.get("internal_id")),
                DbValueMapper.string(row, "id"),
                DbValueMapper.string(row, "category"),
                DbValueMapper.string(row, "name"),
                DbValueMapper.string(row, "manufacturer"),
                DbValueMapper.integer(row, "price"),
                objectMap(DbValueMapper.json(row, "attributes", Map.of()))
        );
    }

    private ToolBuildPart toolPart(PartCandidate part) {
        return new ToolBuildPart(
                part.internalId(),
                part.publicId(),
                part.category(),
                part.name(),
                part.manufacturer(),
                part.price(),
                part.attributes()
        );
    }

    private Map<String, Object> buildRow(String id) {
        return jdbcTemplate.queryForList("""
                        SELECT public_id::text AS id, name, total_price, confidence, warnings, created_at
                        FROM builds
                        WHERE public_id = ?::uuid
                        """, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build를 찾을 수 없습니다."));
    }

    private String runAgent(AgentSessionRoot root) {
        return runAgent(root, AgentRunProfiles.forRoot(root));
    }

    private String runAgent(AgentSessionRoot root, AgentRunProfile profile) {
        try {
            String sessionId = agentTraceService.createQueuedSession(root, "SYSTEM", profile.purpose());
            agentTraceService.advanceStatus(sessionId, AgentStatus.RUNNING, "SYSTEM", "agent run requested for " + profile.purpose());
            agentRunner.run(sessionId, root, profile);
            return sessionId;
        } catch (RuntimeException error) {
            return null;
        }
    }

    private RequirementParseResult runRequirementParseAgent(
            String requirementId,
            String message,
            Map<String, Object> request,
            Map<String, Object> fallbackContext
    ) {
        AgentSessionRoot root = new AgentSessionRoot(AgentSessionRootType.REQUIREMENT, requirementId);
        AgentRunProfile profile = AgentRunProfiles.requirementParse();
        String sessionId = null;
        try {
            sessionId = agentTraceService.createQueuedSession(root, "SYSTEM", profile.purpose());
            String activeSessionId = sessionId;
            agentTraceService.advanceStatus(sessionId, AgentStatus.RUNNING, "SYSTEM", "requirement parse agent requested");
            List<AgentRagEvidenceDraft> evidenceSet = agentRagRetrievalService.retrieveEvidenceSet(root, profile);
            List<String> evidenceIds = evidenceSet.stream()
                    .map(evidence -> agentTraceService.recordRagEvidence(activeSessionId, evidence))
                    .toList();
            agentTraceService.advanceStatus(sessionId, AgentStatus.RAG_SEARCHED, "SYSTEM", "requirement parse RAG evidence set retrieved");

            if (openAiResponsesClient.isConfigured()) {
                try {
                    Map<String, Object> llmContext = llmParsedContext(message, request, fallbackContext, evidenceIds, evidenceSet);
                    Map<String, Object> parsedContext = withAgentParseMetadata(
                            normalizeParsedContext(llmContext, fallbackContext),
                            "AGENT_RAG_LLM",
                            sessionId,
                            evidenceIds,
                            evidenceSet,
                            text(llmContext.get("parseNotes")),
                            null
                    );
                    String summary = firstText(
                            text(parsedContext.get("parseNotes")),
                            "LLM structured parser generated requirement context from RAG evidence."
                    );
                    agentTraceService.advanceStatus(sessionId, AgentStatus.TOOLS_CALLED, "SYSTEM", "requirement parse does not require hardware tools");
                    agentTraceService.updateSummary(sessionId, summary);
                    agentTraceService.advanceStatus(sessionId, AgentStatus.SUMMARY_READY, "SYSTEM", "requirement parse context generated");
                    agentTraceService.advanceStatus(sessionId, AgentStatus.SUCCEEDED, "SYSTEM", "requirement parse agent completed");
                    return new RequirementParseResult(parsedContext, sessionId, summary, evidenceIds);
                } catch (RuntimeException llmError) {
                    Map<String, Object> parsedContext = withAgentParseMetadata(
                            fallbackContext,
                            "AGENT_RAG_FALLBACK",
                            sessionId,
                            evidenceIds,
                            evidenceSet,
                            "LLM structured parse failed; deterministic normalizer kept the request usable.",
                            safeReason(llmError)
                    );
                    String summary = "RAG evidence retrieved, but LLM structured parse failed. Deterministic normalized context was used.";
                    agentTraceService.updateSummary(sessionId, summary);
                    agentTraceService.advanceStatus(sessionId, AgentStatus.FALLBACK_READY, "SYSTEM", "requirement parse LLM failed");
                    agentTraceService.advanceStatus(sessionId, AgentStatus.SUCCEEDED, "SYSTEM", "requirement parse fallback completed");
                    return new RequirementParseResult(parsedContext, sessionId, summary, evidenceIds);
                }
            }

            Map<String, Object> parsedContext = withAgentParseMetadata(
                    fallbackContext,
                    "AGENT_RAG_DETERMINISTIC",
                    sessionId,
                    evidenceIds,
                    evidenceSet,
                    "RAG evidence retrieved; deterministic normalizer produced the structured context because OpenAI is not configured.",
                    null
            );
            String summary = "RAG evidence retrieved; deterministic normalizer generated the requirement context.";
            agentTraceService.advanceStatus(sessionId, AgentStatus.TOOLS_CALLED, "SYSTEM", "requirement parse does not require hardware tools");
            agentTraceService.updateSummary(sessionId, summary);
            agentTraceService.advanceStatus(sessionId, AgentStatus.SUMMARY_READY, "SYSTEM", "requirement parse context generated");
            agentTraceService.advanceStatus(sessionId, AgentStatus.SUCCEEDED, "SYSTEM", "requirement parse agent completed");
            return new RequirementParseResult(parsedContext, sessionId, summary, evidenceIds);
        } catch (RuntimeException error) {
            Map<String, Object> parsedContext = withAgentParseMetadata(
                    fallbackContext,
                    "DETERMINISTIC_FALLBACK",
                    sessionId,
                    List.of(),
                    List.of(),
                    "Requirement parse Agent failed before RAG evidence could be attached; deterministic normalizer was used.",
                    safeReason(error)
            );
            return new RequirementParseResult(parsedContext, sessionId, null, List.of());
        }
    }

    private Map<String, Object> llmParsedContext(
            String message,
            Map<String, Object> request,
            Map<String, Object> fallbackContext,
            List<String> evidenceIds,
            List<AgentRagEvidenceDraft> evidenceSet
    ) {
        String output = openAiResponsesClient.createSummary(
                REQUIREMENT_PARSE_SYSTEM_PROMPT,
                json(MockData.map(
                        "rawMessage", message,
                        "optionalInputs", request,
                        "fallbackNormalizer", fallbackContext,
                        "ragEvidenceSet", evidenceItems(evidenceIds, evidenceSet),
                        "requiredJsonShape", MockData.map(
                                "budget", "integer or null",
                                "usageTags", List.of("GAMING", "DEVELOPMENT", "VIDEO_EDIT", "AI_DEV", "GENERAL"),
                                "resolution", "FHD | QHD | 4K | null",
                                "preferredVendors", List.of("NVIDIA", "AMD", "INTEL"),
                                "priority", "string or null",
                                "performanceTier", "ENTHUSIAST | PERFORMANCE | STANDARD",
                                "budgetPolicy", "USER_BUDGET | OPEN_BUDGET | DEFAULT_BUDGET",
                                "mustHave", List.of("WIFI", "LOW_NOISE"),
                                "confidence", MockData.map("budget", "LOW|MEDIUM|HIGH", "usageTags", "LOW|MEDIUM|HIGH", "resolution", "LOW|MEDIUM|HIGH", "preferredVendors", "LOW|MEDIUM|HIGH"),
                                "parseNotes", "short Korean sentence"
                        )
                ))
        );
        return parseJsonObject(output);
    }

    private static Map<String, Object> deterministicParsedContext(Map<String, Object> body, String message) {
        Integer budget = numberValue(body.get("budget"));
        if (budget == null) {
            budget = inferBudget(message);
        }
        List<String> usageTags = usageTags(body.get("usageTags"), message);
        String resolution = firstText(text(body.get("resolution")), inferResolution(message));
        List<String> preferredVendors = preferredVendors(body.get("preferredVendors"), message);
        String priority = text(body.get("priority"));
        List<String> mustHave = mustHave(message);
        return MockData.map(
                "usageTags", usageTags,
                "budget", budget,
                "resolution", resolution,
                "preferredVendors", preferredVendors,
                "priority", priority,
                "performanceTier", "STANDARD",
                "budgetPolicy", budget == null ? "DEFAULT_BUDGET" : "USER_BUDGET",
                "mustHave", mustHave,
                "confidence", MockData.map(
                        "usageTags", usageTags.isEmpty() ? "LOW" : "HIGH",
                        "budget", budget == null ? "LOW" : "HIGH",
                        "resolution", resolution == null ? "LOW" : "MEDIUM",
                        "preferredVendors", preferredVendors.isEmpty() ? "LOW" : "MEDIUM"
                )
        );
    }

    private static Map<String, Object> withAgentParseMetadata(
            Map<String, Object> context,
            String parseMode,
            String sessionId,
            List<String> evidenceIds,
            List<AgentRagEvidenceDraft> evidenceSet,
            String parseNotes,
            String fallbackReason
    ) {
        AgentRagEvidenceDraft primaryEvidence = primaryEvidence(evidenceSet);
        Map<String, Object> result = new LinkedHashMap<>(normalizeParsedContext(context, context));
        result.put("parseMode", parseMode);
        result.put("parser", "requirement-parse-agent-v1");
        result.put("agentSessionId", sessionId);
        result.put("evidenceIds", evidenceIds);
        result.put("ragGuidance", primaryEvidence == null ? null : primaryEvidence.summary());
        result.put("ragSourceId", primaryEvidence == null ? null : primaryEvidence.sourceId());
        result.put("ragSourceIds", evidenceSet.stream().map(AgentRagEvidenceDraft::sourceId).toList());
        result.put("parseEvidenceSummary", evidenceSummary(evidenceSet));
        result.put("parseNotes", parseNotes);
        if (fallbackReason != null) {
            result.put("fallbackReason", fallbackReason);
        }
        return result;
    }

    private static AgentRagEvidenceDraft primaryEvidence(List<AgentRagEvidenceDraft> evidenceSet) {
        return evidenceSet == null || evidenceSet.isEmpty() ? null : evidenceSet.get(0);
    }

    private static String evidenceSummary(List<AgentRagEvidenceDraft> evidenceSet) {
        if (evidenceSet == null || evidenceSet.isEmpty()) {
            return null;
        }
        return evidenceSet.stream()
                .map(AgentRagEvidenceDraft::summary)
                .filter(summary -> summary != null && !summary.isBlank())
                .limit(3)
                .reduce((left, right) -> left + " | " + right)
                .orElse(null);
    }

    private static List<Map<String, Object>> evidenceItems(List<String> ids, List<AgentRagEvidenceDraft> evidenceSet) {
        return java.util.stream.IntStream.range(0, evidenceSet.size())
                .mapToObj(index -> {
                    AgentRagEvidenceDraft evidence = evidenceSet.get(index);
                    return MockData.map(
                            "id", ids.get(index),
                            "sourceId", evidence.sourceId(),
                            "summary", evidence.summary(),
                            "chunkText", evidence.chunkText(),
                            "score", evidence.score(),
                            "metadata", evidence.metadata()
                    );
                })
                .toList();
    }

    private static Map<String, Object> normalizeParsedContext(Map<String, Object> source, Map<String, Object> fallback) {
        Integer budget = firstNumber(source.get("budget"), fallback.get("budget"));
        List<String> usageTags = normalizeUsageTags(stringList(source.get("usageTags")));
        if (usageTags.isEmpty()) {
            usageTags = normalizeUsageTags(stringList(fallback.get("usageTags")));
        }
        String resolution = normalizeResolution(firstText(text(source.get("resolution")), text(fallback.get("resolution"))));
        List<String> preferredVendors = normalizeVendors(stringList(source.get("preferredVendors")));
        if (preferredVendors.isEmpty()) {
            preferredVendors = normalizeVendors(stringList(fallback.get("preferredVendors")));
        }
        String priority = firstText(text(source.get("priority")), text(fallback.get("priority")));
        String performanceTier = normalizePerformanceTier(firstText(text(source.get("performanceTier")), text(fallback.get("performanceTier"))));
        String budgetPolicy = normalizeBudgetPolicy(firstText(text(source.get("budgetPolicy")), text(fallback.get("budgetPolicy"))));
        List<String> mustHave = normalizeMustHave(stringList(source.get("mustHave")));
        if (mustHave.isEmpty()) {
            mustHave = normalizeMustHave(stringList(fallback.get("mustHave")));
        }
        Map<String, Object> confidence = normalizeConfidence(
                objectMap(source.get("confidence")),
                objectMap(fallback.get("confidence"))
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("usageTags", usageTags.isEmpty() ? List.of("GENERAL") : usageTags);
        result.put("budget", budget);
        result.put("resolution", resolution);
        result.put("preferredVendors", preferredVendors);
        result.put("priority", priority);
        result.put("performanceTier", performanceTier);
        result.put("budgetPolicy", budgetPolicy);
        result.put("mustHave", mustHave);
        result.put("confidence", confidence);
        String parseNotes = firstText(text(source.get("parseNotes")), text(fallback.get("parseNotes")));
        if (parseNotes != null) {
            result.put("parseNotes", parseNotes);
        }
        return result;
    }

    private Map<String, Object> agentSession(String id) {
        return jdbcTemplate.queryForList("""
                        SELECT public_id::text AS id, status, summary, state_timeline
                        FROM agent_sessions
                        WHERE public_id = ?::uuid
                        """, id)
                .stream()
                .findFirst()
                .map(row -> MockData.map(
                        "id", DbValueMapper.string(row, "id"),
                        "status", DbValueMapper.string(row, "status"),
                        "summary", DbValueMapper.string(row, "summary"),
                        "stateTimeline", DbValueMapper.json(row, "state_timeline", List.of())
                ))
                .orElse(Map.of());
    }

    private String agentSessionIdForBuild(String buildId) {
        List<String> rows = jdbcTemplate.queryForList("""
                SELECT s.public_id::text
                FROM agent_sessions s
                JOIN builds b ON b.requirement_id = s.requirement_id OR b.id = s.build_id
                WHERE b.public_id = ?::uuid
                ORDER BY s.created_at DESC, s.id DESC
                LIMIT 1
                """, String.class, buildId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Map<String, Object>> toolResultsBySession(String sessionId) {
        return jdbcTemplate.queryForList("""
                        SELECT ti.tool_name, ti.status, ti.confidence, ti.summary, ti.result_payload
                        FROM tool_invocations ti
                        JOIN agent_sessions s ON s.id = ti.agent_session_id
                        WHERE s.public_id = ?::uuid
                        ORDER BY ti.id
                        """, sessionId)
                .stream()
                .map(row -> MockData.map(
                        "tool", DbValueMapper.string(row, "tool_name"),
                        "status", DbValueMapper.string(row, "status"),
                        "confidence", DbValueMapper.string(row, "confidence"),
                        "summary", DbValueMapper.string(row, "summary"),
                        "details", DbValueMapper.json(row, "result_payload", Map.of())
                ))
                .toList();
    }

    private List<String> evidenceIdsBySession(String sessionId) {
        return jdbcTemplate.queryForList("""
                        SELECT re.public_id::text
                        FROM rag_evidence re
                        JOIN agent_sessions s ON s.id = re.agent_session_id
                        WHERE s.public_id = ?::uuid
                        ORDER BY re.id
                        """, String.class, sessionId);
    }

    private List<String> evidenceIds(String buildId) {
        return jdbcTemplate.queryForList("""
                        SELECT re.public_id::text
                        FROM rag_evidence re
                        JOIN agent_sessions s ON s.id = re.agent_session_id
                        JOIN builds b ON b.id = s.build_id OR b.requirement_id = s.requirement_id
                        WHERE b.public_id = ?::uuid
                        ORDER BY re.id
                        """, String.class, buildId);
    }

    private int budgetForBuild(String buildId, int fallback) {
        Integer budget = jdbcTemplate.queryForObject("""
                SELECT r.budget
                FROM builds b
                JOIN requirements r ON r.id = b.requirement_id
                WHERE b.public_id = ?::uuid
                """, Integer.class, buildId);
        return budget == null || budget <= 0 ? fallback : budget;
    }

    private String defaultPartId(String category) {
        List<String> rows = jdbcTemplate.queryForList("""
                SELECT public_id::text
                FROM parts
                WHERE category = ?
                  AND status = 'ACTIVE'
                  AND deleted_at IS NULL
                  AND coalesce((attributes->>'toolReady')::boolean, false) = true
                ORDER BY price ASC, id ASC
                LIMIT 1
                """, String.class, category);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static List<Map<String, Object>> questions(Map<String, Object> parsedContext) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (parsedContext.get("resolution") == null) {
            result.add(question("monitorResolution", "주 모니터 해상도", List.of("FHD", "QHD", "4K")));
        }
        result.add(question("noisePreference", "소음 민감도", List.of("상관없음", "조용한 편", "매우 조용하게")));
        result.add(question("upgradePlan", "업그레이드 여유", List.of("최소", "보통", "넉넉하게")));
        List<?> usageTags = parsedContext.get("usageTags") instanceof List<?> list ? list : List.of();
        if (usageTags.size() > 1) {
            result.add(question("workloadRatio", "게임과 작업 비중", List.of("게임 우선", "작업 우선", "반반")));
        }
        return result;
    }

    private static Map<String, Object> question(String key, String label, List<String> options) {
        return MockData.map("key", key, "label", label, "options", options, "required", false);
    }

    private static List<Map<String, Object>> warningsFor(List<Map<String, Object>> toolResults, int totalPrice, int budget) {
        List<Map<String, Object>> warnings = new ArrayList<>();
        for (Map<String, Object> result : toolResults) {
            String status = String.valueOf(result.get("status"));
            if ("WARN".equals(status) || "FAIL".equals(status)) {
                warnings.add(MockData.map(
                        "code", result.get("tool") + "_" + status,
                        "message", result.get("summary"),
                        "severity", status
                ));
            }
        }
        if (budget > 0 && totalPrice > budget) {
            warnings.add(MockData.map(
                    "code", "OVER_BUDGET",
                    "message", "예산보다 " + (totalPrice - budget) + "원 높습니다.",
                    "severity", totalPrice <= Math.round(budget * 1.08) ? "WARN" : "FAIL"
            ));
        }
        return warnings;
    }

    private static List<Map<String, Object>> combinedWarnings(List<Map<String, Object>> builds) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> build : builds) {
            Object warnings = build.get("warnings");
            if (!(warnings instanceof List<?> list)) {
                continue;
            }
            for (Object warning : list) {
                if (warning instanceof Map<?, ?> map) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    map.forEach((key, value) -> item.put(String.valueOf(key), value));
                    result.add(item);
                    if (result.size() >= 8) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    private List<Map<String, Object>> diffRows(List<PartCandidate> beforeParts, List<PartCandidate> afterParts, String category, int beforeTotal, int afterTotal) {
        PartCandidate before = byCategory(beforeParts).get(category);
        PartCandidate after = byCategory(afterParts).get(category);
        return List.of(
                MockData.map("label", category, "before", before == null ? "-" : before.name(), "after", after == null ? "-" : after.name(), "diff", priceDiff((after == null ? 0 : after.price()) - (before == null ? 0 : before.price())), "status", "PASS"),
                MockData.map("label", "총액", "before", price(beforeTotal), "after", price(afterTotal), "diff", priceDiff(afterTotal - beforeTotal), "status", afterTotal >= beforeTotal ? "WARN" : "PASS"),
                MockData.map("label", "예상 성능", "before", before == null ? "-" : shortSpec(before), "after", after == null ? "-" : shortSpec(after), "diff", "Tool 재검증", "status", "PASS")
        );
    }

    private static List<PartCandidate> replaceCategory(List<PartCandidate> parts, String category, PartCandidate selectedPart) {
        List<PartCandidate> result = new ArrayList<>();
        boolean replaced = false;
        for (PartCandidate part : parts) {
            if (category.equals(part.category())) {
                result.add(selectedPart);
                replaced = true;
            } else {
                result.add(part);
            }
        }
        if (!replaced) {
            result.add(selectedPart);
        }
        return result;
    }

    private static String previousPartId(List<PartCandidate> parts, String category) {
        return parts.stream()
                .filter(part -> category.equals(part.category()))
                .map(PartCandidate::publicId)
                .findFirst()
                .orElse(null);
    }

    private static int effectiveBudget(RequirementRow requirement, Map<String, Object> answers) {
        Integer budget = requirement.budget();
        if (budget == null) {
            Object parsedBudget = requirement.parsedContext().get("budget");
            budget = numberValue(parsedBudget);
        }
        if (budget == null || budget <= 0) {
            budget = isOpenBudgetEnthusiast(requirement) ? ENTHUSIAST_OPEN_BUDGET : DEFAULT_BUDGET;
        }
        if ("넉넉하게".equals(answers.get("upgradePlan"))) {
            budget = (int) Math.round(budget * 1.06);
        }
        return budget;
    }

    private static List<BuildPlan> buildPlansFor(RequirementRow requirement) {
        return isOpenBudgetEnthusiast(requirement) ? ENTHUSIAST_BUILD_PLANS : DEFAULT_BUILD_PLANS;
    }

    private static boolean isOpenBudgetEnthusiast(RequirementRow requirement) {
        Map<String, Object> context = requirement.parsedContext();
        String performanceTier = normalizePerformanceTier(text(context.get("performanceTier")));
        String budgetPolicy = normalizeBudgetPolicy(text(context.get("budgetPolicy")));
        return ("ENTHUSIAST".equals(performanceTier) || "OPEN_BUDGET".equals(budgetPolicy))
                && requirement.budget() == null
                && numberValue(context.get("budget")) == null;
    }

    private static int target(int budget, BuildPlan plan, double allocation) {
        return (int) Math.round(budget * plan.budgetRatio() * allocation);
    }

    private static PartCandidate chooseByTarget(List<PartCandidate> parts, int targetPrice) {
        if (parts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "추천에 필요한 내부 자산이 부족합니다.");
        }
        return parts.stream()
                .filter(part -> part.price() <= targetPrice)
                .max(Comparator.comparingInt(PartCandidate::price))
                .orElse(parts.get(0));
    }

    private static List<PartCandidate> filter(List<PartCandidate> parts, java.util.function.Predicate<PartCandidate> predicate) {
        return parts.stream().filter(predicate).toList();
    }

    private static List<PartCandidate> orFallback(List<PartCandidate> preferred, List<PartCandidate> fallback) {
        return preferred.isEmpty() ? fallback : preferred;
    }

    private static Map<String, PartCandidate> byCategory(List<PartCandidate> parts) {
        Map<String, PartCandidate> result = new LinkedHashMap<>();
        for (PartCandidate part : parts) {
            result.put(part.category(), part);
        }
        return result;
    }

    private static int total(List<PartCandidate> parts) {
        return parts.stream().mapToInt(part -> part.price() == null ? 0 : part.price()).sum();
    }

    private static int estimatedWattage(List<PartCandidate> parts) {
        return parts.stream()
                .mapToInt(BuildQueryService::estimatedPartPowerDraw)
                .sum() + 60;
    }

    private static int estimatedPartPowerDraw(PartCandidate part) {
        if (part == null) {
            return 0;
        }
        return switch (part.category()) {
            case "CPU" -> Math.max(intAttr(part, "wattage", 0), intAttr(part, "tdpW", 65));
            case "GPU" -> intAttr(part, "wattage", 0);
            case "MOTHERBOARD" -> intAttr(part, "wattage", 50);
            case "RAM" -> intAttr(part, "wattage", 10);
            case "STORAGE" -> intAttr(part, "wattage", 8);
            case "COOLER" -> firstPositive(
                    intAttr(part, "electricalW", 0),
                    intAttr(part, "pumpW", 0),
                    intAttr(part, "fanW", 0),
                    8
            );
            case "CASE" -> firstPositive(intAttr(part, "fanW", 0), intAttr(part, "wattage", 0), 10);
            case "PSU" -> 0;
            default -> intAttr(part, "wattage", 0);
        };
    }

    private static int firstPositive(int... values) {
        for (int value : values) {
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private static boolean socketSupported(PartCandidate cooler, String socket) {
        if (socket == null) {
            return true;
        }
        Object support = cooler.attributes().get("socketSupport");
        if (support instanceof List<?> list) {
            return list.stream().anyMatch(item -> socket.equalsIgnoreCase(String.valueOf(item)));
        }
        return true;
    }

    private static boolean same(String left, String right) {
        if (left == null || right == null) {
            return true;
        }
        return left.equalsIgnoreCase(right);
    }

    private static String stringAttr(PartCandidate part, String key) {
        if (part == null) {
            return null;
        }
        Object value = part.attributes().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static int intAttr(PartCandidate part, String key, int fallback) {
        if (part == null) {
            return fallback;
        }
        Object value = part.attributes().get(key);
        Integer parsed = numberValue(value);
        return parsed == null ? fallback : parsed;
    }

    private static String shortSpec(PartCandidate part) {
        return firstText(stringAttr(part, "shortSpec"), part.name());
    }

    private static String recommendedFor(String name) {
        if (name == null) {
            return "맞춤 추천";
        }
        if (name.contains("끝판왕")) {
            return "최상위 성능 우선";
        }
        if (name.contains("하이엔드")) {
            return "하이엔드 균형";
        }
        if (name.contains("프리미엄")) {
            return "프리미엄 안정";
        }
        if (name.contains("가성비")) {
            return "예산 우선";
        }
        if (name.contains("고성능")) {
            return "성능 우선";
        }
        return "균형 우선";
    }

    private static String summaryText(String name) {
        return recommendedFor(name) + " 조건으로 내부 자산과 저장된 현재가를 조합했습니다.";
    }

    private static String normalizeCategory(String value) {
        String category = firstText(value, "GPU").toUpperCase(Locale.ROOT);
        if (!BUILD_CATEGORIES.contains(category)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "변경 가능한 부품 카테고리가 아닙니다.");
        }
        return category;
    }

    private static Integer inferBudget(String message) {
        Matcher manwon = BUDGET_MANWON.matcher(message);
        if (manwon.find()) {
            return Integer.parseInt(manwon.group(1)) * 10_000;
        }
        Matcher number = BUDGET_NUMBER.matcher(message);
        if (number.find()) {
            return Integer.parseInt(number.group(1).replace(",", ""));
        }
        return null;
    }

    private static List<String> usageTags(Object value, String message) {
        List<String> provided = stringList(value);
        if (!provided.isEmpty()) {
            return provided;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        Set<String> tags = new LinkedHashSet<>();
        if (lower.contains("게임") || lower.contains("배그") || lower.contains("qhd") || lower.contains("gaming")) {
            tags.add("GAMING");
        }
        if (lower.contains("개발") || lower.contains("ide") || lower.contains("dev")) {
            tags.add("DEVELOPMENT");
        }
        if (lower.contains("영상") || lower.contains("편집")) {
            tags.add("VIDEO_EDIT");
        }
        if (lower.contains("ai")) {
            tags.add("AI_DEV");
        }
        if (tags.isEmpty()) {
            tags.add("GENERAL");
        }
        return new ArrayList<>(tags);
    }

    private static String inferResolution(String message) {
        String upper = message.toUpperCase(Locale.ROOT);
        if (upper.contains("4K") || upper.contains("UHD")) {
            return "4K";
        }
        if (upper.contains("QHD")) {
            return "QHD";
        }
        if (upper.contains("FHD")) {
            return "FHD";
        }
        return null;
    }

    private static List<String> preferredVendors(Object value, String message) {
        List<String> provided = stringList(value);
        if (!provided.isEmpty()) {
            return provided;
        }
        String upper = message.toUpperCase(Locale.ROOT);
        List<String> vendors = new ArrayList<>();
        if (upper.contains("NVIDIA") || upper.contains("RTX")) {
            vendors.add("NVIDIA");
        }
        if (upper.contains("AMD") || upper.contains("RADEON") || upper.contains("RYZEN")) {
            vendors.add("AMD");
        }
        if (upper.contains("INTEL")) {
            vendors.add("INTEL");
        }
        return vendors;
    }

    private static List<String> mustHave(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("와이파이")) {
            result.add("WIFI");
        }
        if (lower.contains("저소음") || lower.contains("조용")) {
            result.add("LOW_NOISE");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonObject(String output) {
        try {
            Object parsed = OBJECT_MAPPER.readValue(extractJsonObject(output), Object.class);
            if (parsed instanceof Map<?, ?> map) {
                Map<String, Object> result = new LinkedHashMap<>();
                map.forEach((key, value) -> result.put(String.valueOf(key), value));
                return result;
            }
            throw new IllegalArgumentException("JSON object가 아닙니다.");
        } catch (Exception e) {
            throw new IllegalArgumentException("LLM structured parse JSON을 해석할 수 없습니다.", e);
        }
    }

    private static String extractJsonObject(String output) {
        String text = text(output);
        if (text == null) {
            throw new IllegalArgumentException("LLM 응답이 비어 있습니다.");
        }
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("LLM 응답에서 JSON object를 찾을 수 없습니다.");
        }
        return text.substring(start, end + 1);
    }

    private static Integer firstNumber(Object first, Object fallback) {
        Integer value = safeNumberValue(first);
        return value == null ? safeNumberValue(fallback) : value;
    }

    private static Integer safeNumberValue(Object value) {
        try {
            Integer parsed = numberValue(value);
            if (parsed != null) {
                return parsed;
            }
        } catch (RuntimeException ignored) {
            // Try Korean budget patterns below.
        }
        String text = text(value);
        return text == null ? null : inferBudget(text);
    }

    private static List<String> normalizeUsageTags(List<String> values) {
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = value.toUpperCase(Locale.ROOT);
            if (normalized.contains("GAME") || normalized.contains("GAMING") || value.contains("게임") || value.contains("배그")) {
                result.add("GAMING");
            } else if (normalized.contains("DEV") || value.contains("개발") || value.contains("IDE")) {
                result.add("DEVELOPMENT");
            } else if (normalized.contains("VIDEO") || value.contains("영상") || value.contains("편집")) {
                result.add("VIDEO_EDIT");
            } else if (normalized.contains("AI")) {
                result.add("AI_DEV");
            } else if (normalized.contains("GENERAL") || value.contains("일반") || value.contains("사무")) {
                result.add("GENERAL");
            }
        }
        return new ArrayList<>(result);
    }

    private static String normalizeResolution(String value) {
        String upper = value == null ? null : value.toUpperCase(Locale.ROOT);
        if (upper == null) {
            return null;
        }
        if (upper.contains("4K") || upper.contains("UHD")) {
            return "4K";
        }
        if (upper.contains("QHD")) {
            return "QHD";
        }
        if (upper.contains("FHD")) {
            return "FHD";
        }
        return null;
    }

    private static List<String> normalizeVendors(List<String> values) {
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String upper = value.toUpperCase(Locale.ROOT);
            if (upper.contains("NVIDIA") || upper.contains("RTX")) {
                result.add("NVIDIA");
            }
            if (upper.contains("AMD") || upper.contains("RADEON") || upper.contains("RYZEN")) {
                result.add("AMD");
            }
            if (upper.contains("INTEL")) {
                result.add("INTEL");
            }
        }
        return new ArrayList<>(result);
    }

    private static List<String> normalizeMustHave(List<String> values) {
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String lower = value.toLowerCase(Locale.ROOT);
            if (lower.contains("wifi") || lower.contains("wi-fi") || value.contains("와이파이")) {
                result.add("WIFI");
            }
            if (lower.contains("noise") || value.contains("저소음") || value.contains("조용") || "LOW_NOISE".equalsIgnoreCase(value)) {
                result.add("LOW_NOISE");
            }
        }
        return new ArrayList<>(result);
    }

    private static String normalizePerformanceTier(String value) {
        String upper = value == null ? null : value.toUpperCase(Locale.ROOT);
        if ("ENTHUSIAST".equals(upper) || "PERFORMANCE".equals(upper) || "STANDARD".equals(upper)) {
            return upper;
        }
        return "STANDARD";
    }

    private static String normalizeBudgetPolicy(String value) {
        String upper = value == null ? null : value.toUpperCase(Locale.ROOT);
        if ("USER_BUDGET".equals(upper) || "OPEN_BUDGET".equals(upper) || "DEFAULT_BUDGET".equals(upper)) {
            return upper;
        }
        return "DEFAULT_BUDGET";
    }

    private static Map<String, Object> normalizeConfidence(Map<String, Object> source, Map<String, Object> fallback) {
        return MockData.map(
                "usageTags", confidenceValue(source.get("usageTags"), fallback.get("usageTags"), "MEDIUM"),
                "budget", confidenceValue(source.get("budget"), fallback.get("budget"), "LOW"),
                "resolution", confidenceValue(source.get("resolution"), fallback.get("resolution"), "LOW"),
                "preferredVendors", confidenceValue(source.get("preferredVendors"), fallback.get("preferredVendors"), "LOW")
        );
    }

    private static String confidenceValue(Object value, Object fallback, String defaultValue) {
        String text = firstText(text(value), text(fallback));
        if (text == null) {
            return defaultValue;
        }
        String upper = text.toUpperCase(Locale.ROOT);
        if ("HIGH".equals(upper) || "MEDIUM".equals(upper) || "LOW".equals(upper)) {
            return upper;
        }
        return defaultValue;
    }

    private static String safeReason(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(String::trim).filter(item -> !item.isBlank()).toList();
        }
        String text = text(value);
        if (text == null) {
            return List.of();
        }
        return List.of(text.split(",")).stream().map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private static List<String> csv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(",")).stream().map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
            return result;
        }
        return new LinkedHashMap<>();
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private static String firstText(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private static Integer numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = text(value);
        if (text == null) {
            return null;
        }
        return Integer.valueOf(text.replace(",", ""));
    }

    private static Long numberLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>(source);
        result.put(key, value);
        return result;
    }

    private static String price(int value) {
        return String.format("%,d원", value);
    }

    private static String priceDiff(int value) {
        return (value > 0 ? "+" : "") + price(value);
    }

    private static String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 변환에 실패했습니다.", e);
        }
    }

    private record RequirementRow(
            Long internalId,
            String publicId,
            String rawMessage,
            Integer budget,
            List<String> usageTags,
            Map<String, Object> parsedContext
    ) {
    }

    private record RequirementParseResult(
            Map<String, Object> parsedContext,
            String agentSessionId,
            String agentSummary,
            List<String> evidenceIds
    ) {
    }

    private record PartCandidate(
            Long internalId,
            String publicId,
            String category,
            String name,
            String manufacturer,
            Integer price,
            Map<String, Object> attributes
    ) {
    }

    private record BuildPlan(String name, String recommendedFor, int tier, double budgetRatio, String confidence) {
    }
}
