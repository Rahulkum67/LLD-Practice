package com.lld;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.*;

/** Thin HTTP adapter over AttemptService (JDK built-in server). */
public class Main {
    private static final Pattern ATTEMPT = Pattern.compile("/api/attempts/(\\w+)(?:/(submit|retry|delta))?");
    private static final Pattern PROBLEM = Pattern.compile("/api/problems/([\\w-]+)");
    private final AttemptService svc;
    private final boolean aiEnabled;

    Main(AttemptService svc, boolean aiEnabled) { this.svc = svc; this.aiEnabled = aiEnabled; }

    public static void main(String[] args) throws IOException {
        List<Evaluator> evaluators = new ArrayList<>(List.of(new RubricEvaluator()));
        String key = System.getenv("ANTHROPIC_API_KEY");
        boolean ai = key != null && !key.isBlank();
        if (ai) evaluators.add(new LlmEvaluator(new AnthropicClient(key, "claude-sonnet-4-6", Duration.ofSeconds(30))));
        AttemptService svc = new AttemptService(new InMemoryAttemptRepository(), Catalog.defaultCatalog(),
                new ParserRegistry(new OutlineParser(), new JavaSkeletonParser()), new CompositeEvaluator(evaluators),
                Runner.threaded(2), Clock.systemUTC(), 90_000);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8000"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        Main app = new Main(svc, ai);
        server.createContext("/", app::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("http://localhost:" + port + "  (AI review: " + (ai ? "on" : "off - set ANTHROPIC_API_KEY") + ")");
    }

    void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath(), method = ex.getRequestMethod();
            if (method.equals("GET") && path.equals("/")) {
                try (InputStream in = Main.class.getResourceAsStream("/static/index.html")) { send(ex, 200, in.readAllBytes(), "text/html; charset=utf-8"); }
                return;
            }
            Map<String, Object> body = new HashMap<>();
            String raw = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (!raw.isBlank()) {
                try { body = castMap(Json.parse(raw)); }
                catch (RuntimeException e) { send(ex, 400, Map.of("error", "invalid JSON")); return; }
            }
            send(ex, 200, route(method, path, query(ex.getRequestURI().getRawQuery()), body));
        } catch (Errors.NotFound e) { send(ex, 404, Map.of("error", e.getMessage()));
        } catch (Errors.Validation e) { send(ex, 422, Map.of("error", e.getMessage()));
        } catch (Errors.InvalidTransition e) { send(ex, 409, Map.of("error", e.getMessage()));
        } catch (Exception e) { send(ex, 500, Map.of("error", "internal error")); }
    }

    private Object route(String m, String path, Map<String, String> q, Map<String, Object> b) {
        if (m.equals("GET") && path.equals("/api/config")) return Map.of("formats", svc.parsers().formats(), "ai_enabled", aiEnabled);
        if (m.equals("GET") && path.equals("/api/problems"))
            return svc.catalog().list().stream().map(p -> Map.of("id", p.id(), "title", p.title(), "summary", p.summary(), "difficulty", p.difficulty())).toList();
        Matcher pm = PROBLEM.matcher(path);
        if (m.equals("GET") && pm.matches()) {
            Problem p = svc.catalog().get(pm.group(1));
            return Map.of("id", p.id(), "title", p.title(), "summary", p.summary(), "requirements", p.requirements());
        }
        if (m.equals("POST") && path.equals("/api/attempts"))
            return view(svc.startAttempt(str(b, "learner_id"), str(b, "problem_id"), b.containsKey("format") ? str(b, "format") : "outline"));
        if (m.equals("GET") && path.equals("/api/attempts"))
            return svc.history(q.getOrDefault("learner_id", ""), q.get("problem_id")).stream().map(this::view).toList();
        Matcher am = ATTEMPT.matcher(path);
        if (am.matches()) {
            String id = am.group(1), act = am.group(2);
            if (m.equals("GET") && act == null) return view(svc.getAttempt(id));
            if (m.equals("GET") && act.equals("delta")) return svc.compareWithPrevious(id).map(this::view).orElse(null);
            if (m.equals("PUT") && act == null) return view(svc.saveDraft(id, str(b, "content")));
            if (m.equals("POST") && "submit".equals(act)) return view(svc.submit(id));
            if (m.equals("POST") && "retry".equals(act)) return view(svc.retry(id));
        }
        throw new Errors.NotFound("no such route");
    }

    private Map<String, Object> view(Attempt a) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", a.id); v.put("learner_id", a.learnerId); v.put("problem_id", a.problemId);
        v.put("problem_title", svc.catalog().get(a.problemId).title());
        v.put("fmt", a.format); v.put("number", a.number); v.put("content", a.content());
        v.put("status", a.status().value()); v.put("error", a.error());
        v.put("created_at", a.createdAt); v.put("submitted_at", a.submittedAt());
        Feedback f = a.feedback();
        v.put("feedback", f == null ? null : Map.of("summary", f.summary, "degraded", f.degraded, "notes", f.notes, "scores", f.scores,
                "findings", f.findings.stream().map(x -> Map.of("id", x.id(), "category", x.category(), "severity", x.severity(),
                        "message", x.message(), "suggestion", x.suggestion(), "source", x.source())).toList()));
        return v;
    }

    private Map<String, Object> view(Delta d) {
        return Map.of("previous_attempt", d.previousAttempt(), "resolved", d.resolved(), "new", d.added(),
                "persisting", d.persisting(), "score_changes", d.scoreChanges());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object o) {
        if (!(o instanceof Map)) throw new IllegalArgumentException("expected object");
        return (Map<String, Object>) o;
    }

    private static String str(Map<String, Object> b, String k) { return b.get(k) instanceof String s ? s : ""; }

    private static Map<String, String> query(String raw) {
        Map<String, String> q = new HashMap<>();
        if (raw == null) return q;
        for (String kv : raw.split("&")) {
            String[] p = kv.split("=", 2);
            q.put(java.net.URLDecoder.decode(p[0], StandardCharsets.UTF_8), p.length > 1 ? java.net.URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "");
        }
        return q;
    }

    private static void send(HttpExchange ex, int code, Object json) throws IOException {
        send(ex, code, Json.write(json).getBytes(StandardCharsets.UTF_8), "application/json");
    }

    private static void send(HttpExchange ex, int code, byte[] bytes, String type) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
