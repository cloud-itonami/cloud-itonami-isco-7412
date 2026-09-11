# Contributing

`cloud-itonami-isco-7412` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
kbb -M:test
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real technician, service-account or operator data, credentials
  or operating documents.
- Keep production writes and disclosures behind ElecMechGovernor.
- Treat this occupation's workflows as high-risk: add tests for permission,
  scope-exclusion, safety-escalation and audit logging.
- Never widen the closed op-allowlist to include an
  electrical-repair-execution op, a lockout/tagout-clearance op, or an
  electrical-safety-officer-judgment override/bypass op, without a
  dedicated ADR and explicit human review.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
