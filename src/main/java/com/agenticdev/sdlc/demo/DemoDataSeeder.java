package com.agenticdev.sdlc.demo;

import com.agenticdev.sdlc.coding.domain.CodingBudget;
import com.agenticdev.sdlc.coding.domain.CodingResult;
import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;
import com.agenticdev.sdlc.coding.persistence.CodingRunRepository;
import com.agenticdev.sdlc.github.domain.MergeStrategy;
import com.agenticdev.sdlc.github.persistence.PullRequestRecord;
import com.agenticdev.sdlc.github.persistence.PullRequestRepository;
import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.planning.domain.*;
import com.agenticdev.sdlc.planning.persistence.PlanRecord;
import com.agenticdev.sdlc.planning.persistence.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.demo.seed-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final PlanRepository planRepo;
    private final CodingRunRepository codingRunRepo;
    private final PullRequestRepository prRepo;

    public DemoDataSeeder(PlanRepository planRepo,
                          CodingRunRepository codingRunRepo,
                          PullRequestRepository prRepo) {
        this.planRepo = planRepo;
        this.codingRunRepo = codingRunRepo;
        this.prRepo = prRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (planRepo.count() > 0) {
            log.info("Demo data already present ({} plans), skipping seed", planRepo.count());
            return;
        }
        log.info("Seeding demo data...");
        seedPlans();
        seedCodingRuns();
        seedPullRequests();
        log.info("Demo data seeded: {} plans, {} coding runs, {} pull requests",
                planRepo.count(), codingRunRepo.count(), prRepo.count());
    }

    private void seedPlans() {
        // Plan 1: COMPLETED — rate limiter
        PlanRecord p1 = PlanRecord.promptPending(Provider.LMSTUDIO, "openai/gpt-oss-20b",
                "Add a rate limiter to /search allowing 10 req/min per API key with Redis backing");
        p1.markCompleted(new PlanResult(
                "Implement Redis-backed rate limiting on /search",
                "Sliding window counter in Redis per API key, with a servlet filter and circuit breaker fallback.",
                List.of(
                        new PlanTask("Add spring-boot-starter-data-redis", "Maven dependency + Redis connection config", "S"),
                        new PlanTask("Create RateLimiterService", "Sliding window using INCR+EXPIRE, 10 req/min per key", "M"),
                        new PlanTask("Create RateLimiterFilter", "Extract API key from header, check Redis, return 429", "M"),
                        new PlanTask("Add rate limit response headers", "X-RateLimit-Limit, Remaining, Reset on all /search responses", "S"),
                        new PlanTask("Unit + integration tests", "MockRedis unit tests, Testcontainers Redis integration", "M")
                ),
                List.of(
                        fc("pom.xml", "MODIFY", "Add Redis starter"),
                        fc("src/main/java/com/example/ratelimit/RateLimiterService.java", "CREATE", "Redis sliding window"),
                        fc("src/main/java/com/example/ratelimit/RateLimiterFilter.java", "CREATE", "HTTP filter"),
                        fc("src/main/resources/application.yml", "MODIFY", "Redis connection config"),
                        fc("src/test/java/com/example/ratelimit/RateLimiterServiceTest.java", "CREATE", "Unit tests")
                ),
                List.of(
                        new PlanRisk("Redis unavailability blocks all requests", "Circuit breaker: if Redis down, allow through with warning log"),
                        new PlanRisk("Clock skew across instances", "Use Redis server time via EVAL, not client clock")
                ),
                List.of("Per-user or per-API-key limits?", "Different limits for authenticated vs anonymous?"),
                "## Rate Limiter\nRedis sliding window, 10 req/min per API key."
        ), 4182);
        planRepo.save(p1);

        // Plan 2: COMPLETED — JWT migration (Jira)
        PlanRecord p2 = PlanRecord.promptPending(Provider.ANTHROPIC, "claude-sonnet-4-6",
                "Migrate session-based auth to stateless JWT");
        p2.markCompleted(new PlanResult(
                "Migrate session-based auth to stateless JWT",
                "Replace HttpSession with JWT access+refresh tokens. Add JwtTokenProvider, JwtAuthFilter, update SecurityConfig.",
                List.of(
                        new PlanTask("Create JwtTokenProvider", "Generate/validate JWT with configurable secret and expiry", "M"),
                        new PlanTask("Create JwtAuthenticationFilter", "OncePerRequestFilter extracting Bearer token", "M"),
                        new PlanTask("Update SecurityConfig", "Disable sessions, add JWT filter to chain", "S"),
                        new PlanTask("Update login endpoint", "Return JWT instead of creating session", "S"),
                        new PlanTask("Refresh token support", "Longer-lived refresh token in HttpOnly cookie", "L")
                ),
                List.of(
                        fc("src/main/java/com/example/auth/JwtTokenProvider.java", "CREATE", "JWT generation"),
                        fc("src/main/java/com/example/auth/JwtAuthFilter.java", "CREATE", "Request filter"),
                        fc("src/main/java/com/example/config/SecurityConfig.java", "MODIFY", "Stateless session")
                ),
                List.of(new PlanRisk("Existing sessions invalidated on deploy", "Dual-auth migration period")),
                List.of("Token expiry duration?"),
                "## JWT Migration\nStateless auth with access + refresh tokens."
        ), 6891);
        planRepo.save(p2);

        // Plan 3: FAILED
        PlanRecord p3 = PlanRecord.promptPending(Provider.OPENAI, "gpt-4o",
                "Refactor the monolithic OrderService into separate bounded contexts");
        p3.markFailed("llm_call_failed", "Model returned empty structured output after 3 retries", 1200);
        planRepo.save(p3);

        // Plan 4: PENDING
        PlanRecord p4 = PlanRecord.promptPending(Provider.LMSTUDIO, "openai/gpt-oss-20b",
                "Add OpenTelemetry distributed tracing across all microservices");
        planRepo.save(p4);

        // Plan 5: COMPLETED — health check
        PlanRecord p5 = PlanRecord.promptPending(Provider.LMSTUDIO, "openai/gpt-oss-20b",
                "Create a Kubernetes health check endpoint that reports database, Redis, and external API status");
        p5.markCompleted(new PlanResult(
                "Composite health endpoint for K8s readiness probes",
                "Spring Boot Actuator custom HealthIndicators for DB, Redis, and external API, with configurable timeouts.",
                List.of(
                        new PlanTask("Custom DatabaseHealthIndicator", "Check connection pool and run SELECT 1", "S"),
                        new PlanTask("Custom RedisHealthIndicator", "PING command with 2s timeout", "S"),
                        new PlanTask("Custom ExternalApiHealthIndicator", "HEAD request to external API with configurable URL", "S"),
                        new PlanTask("Configure actuator endpoints", "Expose /health/readiness for K8s with grouped indicators", "S")
                ),
                List.of(
                        fc("src/main/java/com/example/health/DatabaseHealthIndicator.java", "CREATE", "DB health check"),
                        fc("src/main/java/com/example/health/RedisHealthIndicator.java", "CREATE", "Redis ping"),
                        fc("src/main/resources/application.yml", "MODIFY", "Actuator group config")
                ),
                List.of(new PlanRisk("Slow external API causes readiness probe timeout", "2s timeout with fallback to UNKNOWN status")),
                List.of(),
                "## Health Check Endpoint\nComposite K8s readiness probe."
        ), 3200);
        planRepo.save(p5);

        this.plans = List.of(p1, p2, p3, p4, p5);
    }

    private List<PlanRecord> plans;

    private void seedCodingRuns() {
        PlanRecord p1 = plans.get(0), p2 = plans.get(1), p3 = plans.get(2), p5 = plans.get(4);

        String diff1 = """
                diff --git a/pom.xml b/pom.xml
                index abc..def 100644
                --- a/pom.xml
                +++ b/pom.xml
                @@ -45,6 +45,10 @@
                +        <dependency>
                +            <groupId>org.springframework.boot</groupId>
                +            <artifactId>spring-boot-starter-data-redis</artifactId>
                +        </dependency>
                diff --git a/src/main/java/com/example/ratelimit/RateLimiterService.java b/src/main/java/com/example/ratelimit/RateLimiterService.java
                new file mode 100644
                --- /dev/null
                +++ b/src/main/java/com/example/ratelimit/RateLimiterService.java
                @@ -0,0 +1,28 @@
                +package com.example.ratelimit;
                +
                +import org.springframework.data.redis.core.StringRedisTemplate;
                +import org.springframework.stereotype.Service;
                +import java.time.Duration;
                +
                +@Service
                +public class RateLimiterService {
                +    private final StringRedisTemplate redis;
                +    private static final int MAX_REQUESTS = 10;
                +    private static final Duration WINDOW = Duration.ofMinutes(1);
                +
                +    public RateLimiterService(StringRedisTemplate redis) {
                +        this.redis = redis;
                +    }
                +
                +    public boolean isAllowed(String apiKey) {
                +        String key = "ratelimit:" + apiKey;
                +        Long count = redis.opsForValue().increment(key);
                +        if (count == 1) redis.expire(key, WINDOW);
                +        return count <= MAX_REQUESTS;
                +    }
                +}
                diff --git a/src/main/java/com/example/ratelimit/RateLimiterFilter.java b/src/main/java/com/example/ratelimit/RateLimiterFilter.java
                new file mode 100644
                --- /dev/null
                +++ b/src/main/java/com/example/ratelimit/RateLimiterFilter.java
                @@ -0,0 +1,20 @@
                +package com.example.ratelimit;
                +
                +@Component
                +public class RateLimiterFilter extends OncePerRequestFilter {
                +    @Override
                +    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) {
                +        String apiKey = req.getHeader("X-API-Key");
                +        if (apiKey != null && !rateLimiterService.isAllowed(apiKey)) {
                +            res.setStatus(429);
                +            return;
                +        }
                +        chain.doFilter(req, res);
                +    }
                +}""";

        // Run 1: COMPLETED, tests passed
        CodingRunRecord r1 = CodingRunRecord.pending(p1.getId(), Provider.LMSTUDIO, "openai/gpt-oss-20b",
                "https://github.com/agenticdev/search-service.git", "main", null);
        r1.markInProgress();
        r1.markCompleted(new CodingResult(diff1, 3, true, 8, 47200, List.of("pom.xml", "RateLimiterService.java", "RateLimiterFilter.java"), null), 42000);
        codingRunRepo.save(r1);

        // Run 2: COMPLETED, tests passed, auto-open PR
        String diff2 = """
                diff --git a/src/main/java/com/example/auth/JwtTokenProvider.java b/src/main/java/com/example/auth/JwtTokenProvider.java
                new file mode 100644
                +package com.example.auth;
                +
                +import io.jsonwebtoken.Jwts;
                +import io.jsonwebtoken.security.Keys;
                +import org.springframework.stereotype.Component;
                +
                +@Component
                +public class JwtTokenProvider {
                +    public String generateToken(String userId) {
                +        return Jwts.builder()
                +            .subject(userId)
                +            .issuedAt(new Date())
                +            .expiration(new Date(System.currentTimeMillis() + 3600000))
                +            .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                +            .compact();
                +    }
                +}""";
        CodingRunRecord r2 = CodingRunRecord.pending(p2.getId(), Provider.ANTHROPIC, "claude-sonnet-4-6",
                "https://github.com/agenticdev/auth-service.git", "main", null,
                true, "feat: Migrate session auth to JWT", null);
        r2.markInProgress();
        r2.markCompleted(new CodingResult(diff2, 2, true, 5, 31400, List.of("JwtTokenProvider.java", "JwtAuthFilter.java"), null), 28000);
        codingRunRepo.save(r2);

        // Run 3: TIMED_OUT
        CodingRunRecord r3 = CodingRunRecord.pending(p1.getId(), Provider.LMSTUDIO, "openai/gpt-oss-20b",
                "https://github.com/agenticdev/search-service.git", "develop", null);
        r3.markInProgress();
        r3.markCompleted(new CodingResult("", 0, false, 20, 198000, List.of(), CodingBudget.ExhaustReason.ITERATION_LIMIT), 900000);
        codingRunRepo.save(r3);

        // Run 4: IN_PROGRESS
        CodingRunRecord r4 = CodingRunRecord.pending(p5.getId(), Provider.LMSTUDIO, "openai/gpt-oss-20b",
                "https://github.com/agenticdev/platform-service.git", "main", null);
        r4.markInProgress();
        codingRunRepo.save(r4);

        // Run 5: FAILED
        CodingRunRecord r5 = CodingRunRecord.pending(p3.getId(), Provider.OPENAI, "gpt-4o",
                "https://github.com/agenticdev/order-service.git", "main", null);
        r5.markInProgress();
        r5.markFailed("repo_clone_failed", "Repository not found or no access", 1800);
        codingRunRepo.save(r5);

        this.runs = List.of(r1, r2, r3, r4, r5);
    }

    private List<CodingRunRecord> runs;

    private void seedPullRequests() {
        CodingRunRecord r1 = runs.get(0), r2 = runs.get(1), r5 = runs.get(4);

        // PR 1: OPEN
        PullRequestRecord pr1 = PullRequestRecord.pending(r1.getId(),
                "https://github.com/agenticdev/search-service.git", "main",
                "feat: Add Redis-backed rate limiter to /search",
                "## Summary\nRedis sliding window rate limiter, 10 req/min per API key.\n\n_Generated by Agentic SDLC_",
                false, List.of("agentic-sdlc", "feature", "rate-limiting"), null);
        pr1.markPushed("agentic/" + r1.getId(), "a1b2c3d4e5");
        pr1.markOpen(42, "https://github.com/agenticdev/search-service/pull/42",
                List.of("alice-dev", "bob-reviewer"));
        pr1.setDurationMs(8200L);
        prRepo.save(pr1);

        // PR 2: MERGED
        PullRequestRecord pr2 = PullRequestRecord.pending(r2.getId(),
                "https://github.com/agenticdev/auth-service.git", "main",
                "feat: Migrate session auth to JWT tokens",
                "## Summary\nStateless JWT authentication.\n\n_Generated by Agentic SDLC_",
                false, List.of("agentic-sdlc", "security", "breaking-change"), null);
        pr2.markPushed("agentic/" + r2.getId(), "e5f6g7h8i9");
        pr2.markOpen(87, "https://github.com/agenticdev/auth-service/pull/87", List.of("charlie-sec"));
        pr2.markMerged("abc123def456789", MergeStrategy.SQUASH);
        pr2.setDurationMs(6100L);
        prRepo.save(pr2);

        // PR 3: DRAFT
        PullRequestRecord pr3 = PullRequestRecord.pending(r1.getId(),
                "https://github.com/agenticdev/search-service.git", "main",
                "feat: Rate limiter circuit breaker fallback (WIP)", null,
                true, List.of("agentic-sdlc", "wip"), null);
        pr3.markPushed("agentic/draft-circuit-breaker", "x9y0z1a2b3");
        pr3.markOpen(43, "https://github.com/agenticdev/search-service/pull/43", List.of());
        pr3.setDurationMs(4300L);
        prRepo.save(pr3);

        // PR 4: FAILED
        PullRequestRecord pr4 = PullRequestRecord.pending(r5.getId(),
                "https://github.com/agenticdev/order-service.git", "main",
                "feat: Bounded context refactoring", null,
                false, List.of("agentic-sdlc"), null);
        pr4.markFailed("push_failed", "Repository not found or insufficient permissions", 1200L);
        prRepo.save(pr4);
    }

    private static FileChange fc(String path, String change, String reason) {
        return new FileChange(path, FileChange.ChangeType.valueOf(change), reason);
    }
}
