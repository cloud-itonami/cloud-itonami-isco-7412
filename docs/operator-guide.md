# Operator Guide

## First Deployment

1. Define the operator's service-account coverage and technician intake
   process.
2. Define consent and purpose categories for technician/service-account
   records.
3. Run synthetic operating cases (service-log entry, service-operation
   scheduling, supply coordination, safety-concern flagging).
4. Enable human-reviewed sign-off for `:high`/`:safety-critical`
   actions (all flagged safety concerns, above-threshold supply
   orders).
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path (arc-flash risk, lockout/tagout
  status, equipment hazard)
- provenance for all operating records (technician and service account
  both independently registered, technician record including
  certification status)
- human review for high-risk cases
- audit export for all gated actions
- a hard, unconditional block on any attempt to route an
  electrical-repair-execution decision, a lockout/tagout-clearance
  decision, or an electrical-safety-officer-judgment override/bypass,
  through this actor — those decisions stay a certified electrical
  safety officer's exclusive authority end to end

## Certification

Certified operators must prove that the governor gates every
safety-critical robot action, that safety-critical risks escalate to
humans, and that no deployment configuration can route an
electrical-repair-execution decision, a lockout/tagout-clearance
decision, or an electrical-safety-officer-judgment override/bypass
through this actor.
