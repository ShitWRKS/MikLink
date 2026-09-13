# Evidence Contracts

The v1 contracts are local files, not network endpoints:

- `session-manifest.schema.json`: one bounded device/build session.
- `scenario-result.schema.json`: one named or exploratory outcome.
- `trace-event.schema.json`: one NDJSON event; each line validates independently.

All contracts use JSON Schema 2020-12 and begin at version `1.0.0`. Producers MUST
write UTF-8, use UTC RFC 3339 timestamps, store only relative artifact paths, redact
before serialization, and write final JSON atomically. Consumers MUST reject unknown
major schema versions and ignore unknown fields within a supported major version.
The session manifest is a terminal document: `endedAt` and `cleanup` are mandatory.
A release manifest may describe only one external `release-smoke` scenario, cannot
authorize destructive policy, and cannot index enhanced NDJSON trace artifacts.
Scenario results enforce their terminal semantics: PASS cannot contain failed steps
or missing required prerequisites, NOT_RUN/SKIP cannot contain evaluated actions or
assertions, and a cleanup failure necessarily makes the result FAIL. Artifact paths
are unique, normalized, relative, and contained.

The artifact directory layout is:

```text
<session-id>/
|-- session-manifest.json
|-- scenarios/<scenario-id>/scenario-result.json
|-- scenarios/<scenario-id>/ui-hierarchy.xml
|-- scenarios/<scenario-id>/screenshot.png
|-- scenarios/<scenario-id>/probe-trace.ndjson
`-- scenarios/<scenario-id>/generated-report.pdf
```

Files are present only when applicable; every present file is indexed by the
manifest. No backup export is retained as acceptance evidence because it may contain
probe credentials.
