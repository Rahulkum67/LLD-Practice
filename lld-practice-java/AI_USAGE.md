# AI usage

> Draft based on the AI-assisted build. **Edit it so it reflects what you actually asked, accepted and rejected**; the assignment grades your judgement, not the tool.

1. **Architecture shape.** AI proposed splitting parse → design model → evaluators. I accepted it because a new submission format or evaluator can be added without touching the others. I rejected a UML/diagram editor as out of scope for two days.
2. **Where the LLM belongs.** The first suggestion was to have the LLM score the whole design. I rejected that (non-deterministic, hard to test, penalises valid alternatives) and limited it to advisory findings; all scores come from the rubric.
3. **Rubric by capability, not by reference solution.** AI suggested comparing against a canonical class list. I changed it to alias-based capability checks with "warning + justify the trade-off" wording, to tolerate several valid designs.
4. **Failure handling.** AI suggested a job queue with retries. I kept a thread pool, a `FAILED` state with manual retry and a stale-`EVALUATING` timeout, which covers the practical cases without an HLD detour.
5. **Verification.** AI-written tests started with happy paths; I added failure cases (malformed LLM JSON, provider down, stuck evaluation, double submit). Running the server end to end also exposed a bug the unit tests missed: attempt ids contained `-`, which the route regex rejected. I fixed it and added a regression test.
