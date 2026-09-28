# LLD Practice Platform (MVP), Java

Practice a low-level design problem, submit it, get explainable feedback, and see whether the next attempt improved.

**Loop:** choose problem → design (text outline or Java source) → submit → feedback → review history/diff → try again.

## Run
Java 17+ and Maven. No runtime dependencies.
```
mvn test
mvn package && java -jar target/lld-practice-1.0.0.jar     # http://localhost:8000  (PORT=... to change)
```
Optional AI review: `export ANTHROPIC_API_KEY=...` before starting. Without it the app runs rule-based only and the UI says so.

## Code map (`src/main/java/com/lld`)
| Type | Responsibility |
|---|---|
| `Problem`, `Catalog` | Problem data + rubric (concepts, variation points, behaviours). Adding a problem is data only |
| `ClassSpec`, `DesignModel` | Format-independent view of a submitted design; the pivot between parsers and evaluators |
| `SubmissionParser` → `OutlineParser`, `JavaSkeletonParser` | One parser per submission format; `ParserRegistry` looks them up |
| `Evaluator` → `RubricEvaluator`, `LlmEvaluator`; `CompositeEvaluator` (`FeedbackEngine`) | Deterministic vs LLM evaluation; composite isolates failures |
| `LlmClient` → `AnthropicClient` | Provider port |
| `Attempt`, `AttemptStatus` | Aggregate that owns its state machine |
| `AttemptService`, `Runner` | The practice loop, retry, timeout healing, diff; `Runner` = sync / threaded |
| `AttemptRepository` → `InMemoryAttemptRepository` | Storage port |
| `Main` | Thin HTTP adapter (JDK `HttpServer`) + `static/index.html` |

Docs: `docs/RESEARCH.md`, `docs/DESIGN.md`, `AI_USAGE.md`.

## Limitations
- Text/Java submissions only (no diagram editor). The Java parser is regex-based and reads structure, not semantics (no generics-aware type resolution, no nested-class ownership).
- Rubric matching is alias-based: it can miss unusual naming and cannot judge whether behaviour is correct. The LLM step helps but is advisory.
- In-memory storage (attempts are lost on restart), no auth (learner id is a browser-local token), single process.
- Three problems with hand-written rubrics.
