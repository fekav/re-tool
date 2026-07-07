# Composite Retrieval Policy

## Context

`NodeRetrievalService` still depends on exactly one `NodeRetrievalPolicy`.
The active policy is now `EvidenceBasedNodeRetrievalPolicy`, which combines
multiple retrieval policies behind that single domain interface.

The default policy order is:

1. `NodeNameRetrievalPolicy`
2. `TokenOverlapNodeRetrievalPolicy`

This keeps exact node-name retrieval as the strongest evidence while allowing
the MVP review flow to receive real KG candidates when names are similar but
not exact.

## Candidate Lookup Boundaries

`CandidateLookup` remains the exact-name lookup port. It is used only by
`NodeNameRetrievalPolicy` and keeps its existing score of `1.0` with evidence
name `nodeName`.

`CompatibleCandidateLookup` is the broader compatible-candidate lookup port.
It returns KG candidates with a compatible node type but does not score or
decide matches:

- `SUBJECT` and `OBJECT` load `Concept` candidates.
- `ACTION` loads `Predicate` candidates.
- `CONDITION` and `CONSTRAINT` load `Qualifier` candidates with the same
  `qualifierKind`.

`Neo4jNodeNameLookup` implements both ports because both are Neo4j reads over
the same candidate node naming model.

## Token Overlap Evidence

`TokenOverlapNodeRetrievalPolicy` tokenizes the selected term and candidate
label with lowercase `Locale.ROOT` text and the separator
`[^\p{L}\p{N}]+`. Empty tokens are ignored and duplicate tokens count once.

The score is query coverage:

```text
shared unique query tokens / unique query tokens
```

Candidates below `0.5` are filtered out. Scores are capped at `0.99`, so token
overlap can require review through the existing `ThresholdNodeMatchingPolicy`
but cannot auto-map by itself while the default auto-map threshold is `1.0`.

## Deduplication

`EvidenceBasedNodeRetrievalPolicy` deduplicates returned candidates by
`candidateKey`. The first policy wins, so exact `nodeName` evidence is kept
alone when the same candidate is also found by token overlap.
