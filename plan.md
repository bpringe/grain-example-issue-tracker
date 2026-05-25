# Grain Issue Tracker — Learning Plan (Tutor Mode)

## Context

Brandon is building an example project to help people (and himself) learn the [Grain](https://github.com/ObneyAI/grain) event-sourcing framework for Clojure. The project will live in `/Users/brandon/development/grain-issue-tracker` (currently empty).

Brandon is new to Grain and wants to **understand it deeply**, so he will write the code himself. Claude's role is **tutor**, not implementer: explain concepts, answer questions, review code, point to docs, unblock — do *not* generate the project's Grain code unless explicitly asked for a specific snippet.

The project is a deliberately small **single-tenant issue tracker** — chosen because the domain is naturally event-rich (issues open, get assigned, labeled, commented on, closed, reopened) and familiar enough that learners don't have to absorb the domain *and* the framework simultaneously. Multi-tenancy, Datastar UI, and richer features are explicitly deferred until v1 is understood.

## Agreed scope (v1)

- **Single tenant** (fixed tenant id, like Grain's `example-service`).
- **Domain**: Issues with id, title, body, status (`:open` / `:closed`), assignee (string handle), labels (set), comments (vector).
- **~6 commands** — each command exercises a distinct pattern:
  1. open issue (creation)
  2. assign issue (mutation referencing existing entity)
  3. comment on issue (append to collection)
  4. label issue (set membership)
  5. close issue (state transition)
  6. reopen issue (state transition back)
- **2 read models** — to demonstrate that one event stream feeds many views:
  - canonical `issues` map (id → full current state)
  - `issues-by-assignee` index
- **3 queries** — list all issues, get one by id, list-for-assignee.
- **1 todo processor** — react to `:comment-added`, extract `@mentions` from body, emit `:user-mentioned` events. This is the one piece of "agentic / async reactive" behavior in v1; without it the example doesn't showcase Grain's signature feature.
- **No HTTP UI required for v1** — REPL-driven via `cp/process-command` and `qp/process-query` is enough to learn the model. HTTP `/command` and `/query` endpoints fall out of using `command-request-handler-v2` and are a nice stretch.
- **Deliverable**: working code + README. README should include a short event-sourcing primer (audience is mixed: some learners know ES, some don't).

## Suggested learning order

These are **milestones to understand**, not steps to implement. Brandon decides when he's ready to move on. At each milestone, the goal is *understanding what good looks like*, not *finishing fast*.

### Milestone 0 — Get oriented
- Read [docs/core-concepts.md](https://github.com/ObneyAI/grain/blob/main/docs/core-concepts.md) end-to-end.
- Read the entire `example-service` component (it's small): commands, queries, read-models, todo-processors, periodic-tasks, schemas.
- Run Grain's own example app (`development/src/example_app_demo.clj`) in a REPL until creating a counter and projecting `:example/counters` feels natural.
- **You'll know you're ready when**: you can explain, without notes, the difference between a *command*, an *event*, a *read model*, and a *projection* — and why events (not state) are the source of truth.

### Milestone 1 — Project skeleton
- Plain `deps.edn` project — no Polylith, no extra tooling. One `src/` tree, one `dev/` (or `development/`) directory for REPL helpers.
- Mirror Grain's `example-service` *namespace* shape inside `src/issue_tracker/`: a `core/` subtree (`commands.clj`, `queries.clj`, `read_models.clj`, `todo_processors.clj`) and an `interface/` subtree (`schemas.clj`, `read_models.clj`). The split is a convention worth keeping even without Polylith — `interface` is the public surface, `core` is the implementation.
- Add Grain as a `deps.edn` dependency (git coord against the ObneyAI/grain repo).
- **You'll know you're ready when**: `clj` starts cleanly and the namespaces compile, even though they contain no logic yet.

### Milestone 2 — First command + event + projection (vertical slice)
- Pick *one* command — `open-issue` — and implement it end-to-end:
  - Schema in `interface/schemas.clj` (`defschemas`).
  - Command handler in `core/commands.clj` (`defcommand`, returns `{:command-result/events [(->event …)]}`).
  - Event schema entry.
  - Read model in `core/read_models.clj` (`defreadmodel`, pure `(state, event) -> state`).
  - Interface re-export in `interface/read_models.clj`.
- Wire a minimal entry namespace (e.g., `issue-tracker.service`) that can start/stop a Grain service with an in-memory event store.
- From the REPL: `(service/start)`, `cp/process-command`, then `(es/read event-store …)` to inspect the appended event, then project the read model.
- **You'll know you're ready when**: you can replay the event store and watch the projection rebuild from scratch — and you understand *why* that's the whole point of event sourcing.

### Milestone 3 — Fill out commands
- Add the other 5 commands one at a time, each with its event and schema.
- Notice the natural patterns: validation (don't close an already-closed issue → return a cognetict anomaly), entity references (assign requires issue exists), set membership (labeling).
- Tag events with `:tags #{[:issue issue-id]}` so per-issue projections become possible later.
- **You'll know you're ready when**: you can articulate why a command may return an anomaly *or* events but not both, and why command results should describe *what changed* rather than *what the new state is*.

### Milestone 4 — Second read model + queries
- Add `issues-by-assignee` projection — it derives from a *subset* of event types.
- Add `defquery` handlers wrapping `(rmp/project ctx :issue-tracker/…)` calls.
- **You'll know you're ready when**: you can explain why projections are versioned and what `:version` does, and why the cache is automatic.

### Milestone 5 — Todo processor (async reactive)
- Implement the `@mention` extractor: subscribe to `:comment-added`, parse body for `@\w+` tokens, append one `:user-mentioned` event per mention.
- Decide consciously: at-most-once vs. at-least-once checkpointing. For mention extraction (idempotent enough, pure events), the pure-events return pattern is fine.
- Trigger a comment with `@alice and @bob` and watch the resulting mention events appear in the store.
- **You'll know you're ready when**: you can articulate the difference between a *command* and a *processor* — both emit events, but commands respond to external input synchronously while processors react to existing events asynchronously.

### Milestone 6 — README + polish
- Write a README aimed at both ES newcomers and Grain newcomers: short ES primer, quickstart, walkthrough chapters, file-by-file pointer, next steps (multi-tenant, Datastar UI, HTTP endpoints, periodic task).
- Add a few comment-block REPL recipes in `development/` so a new reader can interact in 30 seconds.

## Resources to reach for first

When Brandon is stuck or wants to compare against canonical patterns, in roughly this order:

1. **`example-service` source** — exact same structure he'll be writing; the closest reference.
2. **`docs/core-concepts.md`** — concept-level "why," with macro signatures.
3. **`development/src/example_app_demo.clj`** — REPL patterns for driving a Grain service.
4. **`docs/datastar.md`** — only when adding a UI (post-v1).
5. **`docs/distributed-coordination.md`** — only when going multi-node (post-v1).

## How Claude will help

- Explain Grain concepts, ES/CQRS theory, and the *why* behind framework choices.
- Decode error messages, schema-validation failures, registry-not-found errors.
- Review Brandon's code: point out idiom mismatches, missed conventions, subtle bugs (e.g., forgetting `:tags`, using `random-uuid` instead of `->event`'s UUID v7).
- Suggest the next milestone when current one is done; never push faster than Brandon wants.
- Look up Grain source/docs to answer specific questions.
- **Will not**: write Grain command/event/read-model/processor code unsolicited. If Brandon explicitly asks for a snippet, generate the smallest possible one and explain it.

## Verification (how we know v1 is done)

- `clj -X:something` starts the service cleanly.
- All 6 commands callable from REPL; each produces the expected events.
- Both projections return correct state, including after a manual event-store replay (delete cache, re-project).
- `@mention` processor fires on a comment with mentions; resulting `:user-mentioned` events visible in the store.
- README walks a fresh reader from clone to working REPL session.

## Open / deferred

These are intentionally **out of scope** for v1 but the structure should accommodate them:

- Multi-tenancy (orgs/projects map to Grain tenants).
- Datastar SSE UI.
- HTTP `/command` and `/query` endpoints via `command-request-handler-v2`.
- Periodic task (e.g., daily "stale issues" sweeper).
- SQLite / Postgres backends (v1 stays in-memory).
- Tutorial-chapter prose (v1 README is enough; deeper teaching docs are a v2 deliverable).
