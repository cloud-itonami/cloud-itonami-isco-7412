# cloud-itonami-isco-7412

Open Occupation Blueprint for **ISCO-08 7412**: Electrical Mechanics and Fitters.

This repository designs a forkable OSS business for an electrical mechanics/fitting service scheduling and logistics coordination practice: a service scheduling and supply-coordination robot manages technician/service-call records under a governor-gated actor, so an electrical mechanic/fitter crew keeps its own operating records instead of renting a closed field-service-management SaaS.

**Maturity: `:implemented`.** `src/elecmech/` implements the
`ElecMechActor` as a `langgraph.graph/state-graph`
(`elecmech.actor`) wired to an `Electrical Mechanic Advisor`
(`elecmech.advisor`) and an independent `ElecMechGovernor`
(`elecmech.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 27 tests / 59 assertions green (`clojure -M:test`).
HARD invariants (always hold, never overridable):
technician provenance, service-account provenance, no-actuation
(`:effect` must be `:propose`), a closed op-allowlist
(`:log-service-record`, `:schedule-service-operation`,
`:flag-safety-concern`, `:coordinate-supply-order` — nothing else may
ever be proposed), and a permanent, unconditional block on any
proposal that would directly finalize an
electrical-repair-execution decision (e.g. deciding to proceed
with a specific electrical repair), authorize or finalize a
lockout/tagout-clearance decision, or override or bypass an
electrical safety officer's judgment. Always-escalate
paths (human sign-off regardless of confidence, mapping this repo's
Trust Controls in [`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above
the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a service scheduling/logistics coordination robot performs technician scheduling, service-call/parts-usage/diagnostic-status record logging and electrical-parts supply-order coordination for an electrical mechanic/fitter crew, under an actor that proposes actions and an independent **Electrical Mechanic Governor** that gates them. The governor never
dispatches hardware itself, never performs electrical-repair work, and never finalizes an electrical-repair-execution decision, authorizes a lockout/tagout clearance, or overrides an electrical safety officer's judgment; `:high`/`:safety-critical` actions (such as a flagged arc-flash-risk/lockout-tagout-status/equipment-hazard concern, or an above-threshold supply order) require human sign-off. **This actor coordinates service scheduling/logistics only — it never performs electrical-repair work itself.**

## Core Contract

```text
technician roster + service-account registration + safety-reporting policy
        |
        v
Electrical Mechanic Advisor -> ElecMechGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, finalize
an electrical-repair-execution decision, authorize or finalize a
lockout/tagout-clearance decision, override or bypass an electrical safety
officer's judgment, suppress an operating record, or disclose sensitive data
without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7412`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
