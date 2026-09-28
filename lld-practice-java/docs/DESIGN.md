# Design note

## MVP and flow
Choose one of 3 problems → start attempt (pick format) → edit draft → submit → status `submitted → evaluating → evaluated | failed` → findings + per-category scores → history per problem, diffed against the previous evaluated attempt → new attempt. Failed evaluations can be retried.

## What a learner must provide
Classes/interfaces with responsibilities (methods) and relationships (field types, `extends`/`implements`). That is enough to check separation of concerns and extension points without runnable code. Two formats prove the seam: a text outline, and plain Java source (bodies may be empty).

## Useful feedback when several designs are valid
- The rubric checks **capabilities via aliases**, not a reference class diagram (`Slot` counts as `ParkingSpot`).
- A missing abstraction is a *warning* that says "if this was deliberate, note the trade-off", not a failure.
- Every finding has a message, a suggestion and a stable id (so attempts can be diffed). Strengths are reported too.

## Deterministic vs LLM
| Deterministic (`RubricEvaluator`) | LLM (`LlmEvaluator`) |
|---|---|
| Concept coverage, abstractions with real implementations, behaviours exist, god class, tiny design | Responsibility placement, change scenarios, over/under-engineering, trade-offs |
| Same input → same output; unit-testable; produces **all scores** | Advisory only: no scores, max 5 findings, output validated and untrusted-input-safe prompt |

## Key abstractions (all extension points are interfaces)
- `SubmissionParser`: new format = new class, evaluators untouched.
- `Evaluator` / `FeedbackEngine`: new approach (e.g. compare with a reference design, static metrics) = new class registered in `Main`.
- `LlmClient`: swap provider, or a lambda in tests.
- `AttemptRepository`, `Runner`: swap storage / execution.
- `Attempt` owns legal state transitions; `AttemptService` orchestrates; `Main` only translates HTTP.
- Problems are data.

## Slow or failing evaluation
- `submit` returns immediately; work runs on a 2-thread pool (`Runner.threaded`) and the UI polls. Tests use `Runner.sync()`.
- Unparsable input is rejected at submit, so it stays an editable draft.
- One evaluator failing (e.g. LLM timeout) → `CompositeEvaluator` returns the rest with `degraded=true` and a note. All failing → attempt `FAILED` with the reason; `retry` is allowed.
- A worker that dies leaves `EVALUATING`; `getAttempt` fails it after the timeout, so it is retryable. A late result is ignored if the attempt is no longer `EVALUATING`.
- Scaling (light HLD): replace `Runner.threaded` with a queue consumer; `AttemptService` is unchanged.

## Trade-offs
- Alias-based rubric is simple and explainable but brittle to unusual naming; an LLM "concept mapping" step is the natural upgrade.
- Structural checks cannot tell whether a design *works*; scenario walkthrough prompts would be next.
- Hand-rolled `Json` and in-memory storage keep the project dependency-free, at the cost of production readiness.
- One Java package for brevity; it would split into `domain / parse / eval / app / web` as it grows.
