# Governance

`cloud-itonami-isco-7412` is an OSS open-occupation blueprint. Governance
covers both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions or disclose records.
- ElecMechGovernor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval.
- an electrical-repair-execution decision, a lockout/tagout-clearance
  decision, and any override or bypass of an electrical safety
  officer's judgment, stay permanently outside this actor's
  op-allowlist.
- every commit, hold and approval path is auditable.
- real technician/service-account/operator data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification, license, or
the closed op-allowlist should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and data-flow
review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling technician/service-account/operator data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
- attempting to route an electrical-repair-execution decision, a
  lockout/tagout-clearance decision, or an
  electrical-safety-officer-judgment override/bypass, through this
  actor
