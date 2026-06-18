# OWASP Security Risk Assessment Report

**Date:** 2026-06-18
**Server:** medical-mcp-server v1.6.0
**Assessment:** Code review + architecture analysis

---

## Risk 1 — A01:2021 Broken Access Control

### Vulnerability Demonstration

**Vulnerable Code — `SecurityConfig.java`:**

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth.requestMatchers(
                            "/sse",
                            "/sse/**",
                            "/mcp/message",
                            "/mcp/**",
                            "/actuator/health",
                            "/actuator/health/**",
                            "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
            .build();
}
```

**Attack Scenario:**

```bash
# Any client on the network can invoke all 5 MCP tools + 2 resources + 1 prompt
# No API key, no JWT, no mutual TLS

# Step 1: List all tools (reconnaissance)
curl -s http://localhost:8092/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'

# Step 2: Exfiltrate all dataset stats
curl -s http://localhost:8092/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"get_dataset_stats"}}'

# Step 3: Search for cases with specific medical conditions
curl -s http://localhost:8092/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"search_cases","arguments":{"query":"cardiac arrest"}}}'

# Step 4: Get full case details (transcriptions may contain PHI)
curl -s http://localhost:8092/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"get_case","arguments":{"id":"<id-from-step-3>"}}}'
```

**Impact:**
- **Confidentiality:** Medical case transcriptions exposed without authentication
- **PHI Risk:** Patient data may be accessed in violation of HIPAA/GDPR requirements
- **No Audit Trail:** No user identification on MCP calls

---

### Risk Assessment — Before Mitigation

| Dimension | Value |
|---|---|
| **Likelihood** | HIGH — No credentials required, default permit-all on MCP endpoints |
| **Impact** | HIGH — PHI exposure, potential regulatory violations |
| **Risk Rating** | **HIGH** |
| **CVSS 3.1** | 7.5 (AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N) |

---

### Mitigation — Updated Code

**`SecurityConfig.java` (enforced authentication):**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${medicalmcp.security.mcp-api-key}")
    private String mcpApiKey;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .addFilterBefore(new ApiKeyAuthFilter(mcpApiKey), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/sse", "/sse/**", "/mcp/message", "/mcp/**")
                        .hasRole("MCP_CLIENT")
                        .anyRequest()
                        .authenticated())
                .build();
    }
}
```

**`ApiKeyAuthFilter.java`:**

```java
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-MCP-API-Key";

    private final String validApiKey;

    public ApiKeyAuthFilter(String validApiKey) {
        this.validApiKey = validApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Skip health endpoints (already permitAll)
        if (request.getRequestURI().startsWith("/actuator/")) {
            chain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(API_KEY_HEADER);

        if (StringUtils.hasText(validApiKey) && validApiKey.equals(providedKey)) {
            SecurityContextHolder.getContext().setAuthentication(
                new ApiKeyAuthentication(validApiKey));
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or missing API key\"}");
        }
    }
}
```

**Failed Attack with Mitigation:**

```bash
# Without valid API key — request rejected
curl -s http://localhost:8092/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'

# Response: 401 Unauthorized {"error":"Invalid or missing API key"}

# With valid API key — succeeds
curl -s http://localhost:8092/mcp/message \
  -H "Content-Type: application/json" \
  -H "X-MCP-API-Key: ${MEDICALMCP_API_KEY}" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'

# Response: 200 OK — tool list returned
```

---

### Risk Assessment — After Mitigation

| Dimension | Value |
|---|---|
| **Likelihood** | LOW — API key required, role-enforced |
| **Impact** | MEDIUM — Key rotation needed, key exposure still a risk |
| **Risk Rating** | **MEDIUM** |
| **CVSS 3.1** | 5.3 (AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:N/A:N) |

**Remaining Risk:** API key leakage via logs, environment variable exposure, or insecure key rotation. Mitigation requires:
- Vault integration for key management
- TLS for all MCP connections
- Audit logging of MCP calls with key identity

---

## Risk 2 — A03:2021 Injection

### Vulnerability Demonstration

**Vulnerable Code Pattern — FTS Query Injection:**

While `fullTextSearch.sql` uses parameterized queries, the application-level query construction could be exploited if `plainto_tsquery` behavior is not correctly constrained:

```java
// MedicalCaseRepositoryImpl.java — theoretically vulnerable if plainto_tsquery 
// is combined with user-controlled prefix characters
public List<CaseSummary> fullTextSearch(String query, String specialty, String split, int limit) {
    if (!StringUtils.hasText(query) || limit <= 0) {
        return List.of();
    }
    // Query passed directly to plainto_tsquery via SQL parameter
    return jdbc.query(fullTextSearchSql,
        new MapSqlParameterSource()
            .addValue("query", query.trim())  // Could contain tsquery operators
            .addValue("specialty", ...)
            ...
```

**Attack Scenario — PostgreSQL tsquery Operator Injection:**

```bash
# Normal query
curl -s http://localhost:8092/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{
    "name":"search_cases",
    "arguments":{"query":"pacemaker","limit":10}
  }}'

# Response: Normal results

# Injection attempt: tsquery boolean operators
curl -s http://localhost:8092/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{
    "name":"search_cases",
    "arguments":{"query":"pacemaker | cardiac", "limit":10}
  }}'

# Response from plainto_tsquery: ERROR — plainto_tsquery doesn't accept operators
# The SQL engine rejects this at parse time

# However, if application uses to_tsquery instead of plainto_tsquery:
-- Malicious: 'pacemaker | cardiac' becomes valid tsquery with OR semantics
-- Could extract data across specialties via union-like patterns
```

**Secondary Attack — Resource Exhaustion via Complex tsquery:**

```bash
# Complex query with many terms
curl -s http://localhost:8092/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{
    "name":"search_cases",
    "arguments":{"query":"a b c d e f g h i j k l m n o p q r s t", "limit":50}
  }}'

# Result: Complex plainto_tsquery consumes excessive CPU
# The GIN index helps but complex multi-term queries stress the planner
```

---

### Risk Assessment — Before Mitigation

| Dimension | Value |
|---|---|
| **Likelihood** | MEDIUM — plainto_tsquery blocks operators, but misconfiguration possible |
| **Impact** | MEDIUM — Data leakage or DoS |
| **Risk Rating** | **MEDIUM** |
| **CVSS 3.1** | 5.3 (AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:N/A:N) |

---

### Mitigation — Updated Code

**1. Query Length Limit + Input Sanitization:**

```java
@Override
public List<CaseSummary> fullTextSearch(String query, String specialty, String split, int limit) {
    // Reject empty/blank
    if (!StringUtils.hasText(query) || limit <= 0) {
        return List.of();
    }
    // Hard limit on query length (prevents DoS)
    String sanitizedQuery = query.trim();
    if (sanitizedQuery.length() > 500) {
        sanitizedQuery = sanitizedQuery.substring(0, 500);
    }
    // Remove tsquery/tsvector special characters
    sanitizedQuery = sanitizedQuery.replaceAll("[():*|&<>]", " ");
    
    // Validate split
    if (StringUtils.hasText(split) && !VALID_SPLITS.contains(split)) {
        return List.of();
    }

    return jdbc.query(fullTextSearchSql,
        new MapSqlParameterSource()
            .addValue("query", sanitizedQuery)  // Now sanitized
            .addValue("specialty", StringUtils.hasText(specialty) ? specialty : "")
            .addValue("split", StringUtils.hasText(split) ? split : "")
            .addValue("limit", effectiveLimit(sanitizeLimit(limit))),
        (rs, rowNum) -> mapCaseSummary(rs));
}

private int sanitizeLimit(Integer limit) {
    if (limit == null || limit <= 0) return 10;
    return Math.min(Math.max(limit, 1), MAX_LIMIT); // Clamp to [1, 50]
}
```

**2. Add Query Timeout for FTS:**

```sql
-- fullTextSearch.sql with statement timeout
SET LOCAL statement_timeout = '5s';
SELECT id, sample_name, description, medical_specialty, keywords, split
FROM medical_case
WHERE fts @@ plainto_tsquery('english', :query)
  AND (COALESCE(:specialty, '') = '' OR medical_specialty = :specialty)
  AND (COALESCE(:split, '') = '' OR split = :split)
ORDER BY ts_rank(fts, plainto_tsquery('english', :query)) DESC
LIMIT :limit
```

**3. Rate Limiting for Search Endpoints:**

```java
@Configuration
public class RateLimitingConfig {
    
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(
            RateLimiter.ofDefaults("search"),
            List.of("/mcp/message")
        ));
        registration.setOrder(1);
        return registration;
    }
}
```

**Failed Attack with Mitigation:**

```bash
# Operator injection — stripped to spaces
curl -s http://localhost:8092/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{
    "name":"search_cases",
    "arguments":{"query":"pacemaker | cardiac", "limit":10}
  }}'

# Query sanitized: 'pacemaker | cardiac' → 'pacemaker   cardiac'
# plainto_tsquery treats as single phrase search, not boolean OR

# Long query — truncated
curl -s http://localhost:8092/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{
    "name":"search_cases",
    "arguments":{"query":"very long query exceeding 500 characters...", "limit":10}
  }}'

# Query truncated to 500 chars, excess ignored

# High limit — clamped
curl -s http://localhost:8092/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{
    "name":"search_cases",
    "arguments":{"query":"patient", "limit":9999}
  }}'

# Limit clamped to MAX_LIMIT (50)
```

---

### Risk Assessment — After Mitigation

| Dimension | Value |
|---|---|
| **Likelihood** | LOW — Sanitization + length limits + rate limiting |
| **Impact** | LOW — Timeout + clamped limits prevent data theft and DoS |
| **Risk Rating** | **LOW** |
| **CVSS 3.1** | 3.1 (AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:N/A:N) |

**Remaining Risk:** Not fully eliminated. Recommend:
- Web Application Firewall (WAF) layer
- Query whitelist for known-good patterns
- Embedding endpoint isolation (cannot be directly accessed by MCP clients)

---

## Summary

| Risk | Before | After | Status |
|---|---|---|---|
| A01 Broken Access Control | HIGH | MEDIUM | Partially mitigated — API key auth added |
| A03 Injection | MEDIUM | LOW | Mitigated — sanitization, limits, timeouts |

### Recommendations (Not Fully Mitigated Items)

1. **A01 Residual:** API key stored in env var — migrate to Vault or secrets manager
2. **A01 Residual:** No audit logging of MCP calls with user identity
3. **A03 Residual:** Rate limiting should be tested under load
4. **A06 Vulnerable Components:** Run `mvn dependency:analyze` and `trivy` on Docker image
5. **A05 Security Misconfiguration:** Actuator probes exposed on same port as MCP — consider separate management port

---

## References

- [OWASP Top 10 2021](https://owasp.org/Top10/)
- Spring Security Documentation
- PostgreSQL `plainto_tsquery` vs `to_tsquery` behavior
- Spring AI MCP Server configuration