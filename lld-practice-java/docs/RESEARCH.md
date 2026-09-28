# Research note

> Written from general familiarity with the space. **Before submitting, spend 30-45 minutes actually trying 3-4 of these and replace my impressions with yours** — reviewers will ask what you observed.

## Learner problem
LLD is open-ended: many designs are valid, so learners can't tell whether their class breakdown is good. Typical failure modes: (1) copying a memorised solution, (2) a "god class" holding all logic, (3) patterns applied by name rather than need, (4) no way to know if attempt 2 is better than attempt 1. Existing practice is either *reading* solutions or *interview mock sessions* with a human, both weak on fast, repeatable feedback.

## What exists (to verify hands-on)
- **Course/article platforms** (e.g. Educative's OOD courses, Design Gurus, AlgoMaster-style LLD guides, GitHub "awesome LLD" repos): strong explanations and reference solutions; no evaluation of the learner's own design.
- **Coding judges** (LeetCode-style, incl. its OOD-style "Design X" problems): auto-graded by test cases, which rewards passing behaviour not design quality; hidden test APIs constrain the design.
- **Diagramming tools** (draw.io, Excalidraw, PlantUML): free-form, no feedback.
- **General LLM chat**: flexible feedback but inconsistent between runs, not tied to a rubric, no history.
- **Human mock interviews / code review**: high quality, expensive, not repeatable.

## Gaps
1. Feedback on the learner's *own* design, not just a reference answer.
2. Consistency: same design should get the same objective findings every time.
3. Tolerance for multiple valid designs (don't penalise naming or a different-but-sound structure).
4. Progress across attempts.

## Direction
Rubric-anchored feedback with an LLM used for judgement only:
- Learner submits a **structured outline or a Python skeleton** (fast to write, machine-readable; no runnable code needed).
- **Deterministic checks** verify facts: core concepts present, extension points have real abstractions, behaviours have a home, god classes.
- **LLM review** covers what rules can't: cohesion, change scenarios, over/under-engineering. It sees the deterministic findings so it doesn't repeat them, and its output is validated.
- **History with diff** ("2 resolved, 1 new") makes retry the core loop.
