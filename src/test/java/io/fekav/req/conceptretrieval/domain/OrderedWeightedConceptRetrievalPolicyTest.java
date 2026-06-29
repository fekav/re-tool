package io.fekav.req.conceptretrieval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.fekav.req.conceptretrieval.application.CandidateLookupScope;
import io.fekav.req.conceptretrieval.application.ConceptCandidateLookup;

class OrderedWeightedConceptRetrievalPolicyTest {

    private final SelectedTerm selectedTerm = new SelectedTerm("SUBJECT", "billing service");

    @Test
    void returnsExactLabelCandidatesAndStops_whenExactLookupMatches() {
        // Given
        List<String> invokedLookupMethods = new ArrayList<>();
        ConceptCandidateLookup exactLookup = lookup(
            "exactLabel",
            1.0,
            invokedLookupMethods,
            term -> List.of(
                hit("concept-2", "Billing API", "exactLabel"),
                hit("concept-1", "Billing Service", "exactLabel")
            )
        );
        ConceptCandidateLookup aliasLookup = lookup(
            "alias",
            0.7,
            invokedLookupMethods,
            term -> List.of(hit("alias-1", "Payments", "alias"))
        );
        OrderedWeightedConceptRetrievalPolicy policy =
            new OrderedWeightedConceptRetrievalPolicy(List.of(aliasLookup, exactLookup));

        // When
        CandidateConceptMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(invokedLookupMethods).containsExactly("exactLabel");
        assertThat(match.selectedTerm()).isEqualTo(selectedTerm);
        assertThat(match.candidates())
            .extracting(candidate -> candidate.candidate().candidateKey())
            .containsExactly("concept-2", "concept-1");
        assertThat(match.candidates())
            .allSatisfy(candidate -> {
                assertThat(candidate.evidence()).singleElement().satisfies(evidence -> {
                    assertThat(evidence.policyName()).isEqualTo("orderedWeighted");
                    assertThat(evidence.score()).isEqualTo(1.0);
                });
            });
    }

    @Test
    void returnsAliasCandidates_whenExactLookupMisses() {
        // Given
        List<String> invokedLookupMethods = new ArrayList<>();
        OrderedWeightedConceptRetrievalPolicy policy =
            new OrderedWeightedConceptRetrievalPolicy(List.of(
                lookup("exactLabel", 1.0, invokedLookupMethods, term -> List.of()),
                lookup(
                    "alias",
                    0.7,
                    invokedLookupMethods,
                    term -> List.of(
                        hit("concept-1", "Billing Service", "alias"),
                        hit("concept-2", "Billing API", "alias")
                    )
                )
            ));

        // When
        CandidateConceptMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(invokedLookupMethods).containsExactly("exactLabel", "alias");
        assertThat(match.candidates())
            .extracting(
                candidate -> candidate.candidate().candidateKey(),
                candidate -> candidate.evidence().getFirst().score()
            )
            .containsExactly(
                tuple("concept-1", 0.7),
                tuple("concept-2", 0.7)
            );
    }

    @Test
    void returnsEmptyCandidates_whenEveryLookupMisses() {
        // Given
        List<String> invokedLookupMethods = new ArrayList<>();
        OrderedWeightedConceptRetrievalPolicy policy =
            new OrderedWeightedConceptRetrievalPolicy(List.of(
                lookup("exactLabel", 1.0, invokedLookupMethods, term -> List.of()),
                lookup("alias", 0.7, invokedLookupMethods, term -> List.of())
            ));

        // When
        CandidateConceptMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(invokedLookupMethods).containsExactly("exactLabel", "alias");
        assertThat(match.candidates()).isEmpty();
        assertThat(match.selectedTerm()).isEqualTo(selectedTerm);
    }

    @Test
    void sumsWeightsByCandidateKey_whenMultipleNonExactLookupsMatchSameCandidate() {
        // Given
        List<String> invokedLookupMethods = new ArrayList<>();
        OrderedWeightedConceptRetrievalPolicy policy =
            new OrderedWeightedConceptRetrievalPolicy(List.of(
                lookup("exactLabel", 1.0, invokedLookupMethods, term -> List.of()),
                lookup(
                    "alias",
                    0.7,
                    invokedLookupMethods,
                    term -> List.of(hit("concept-1", "Billing Service", "alias"))
                ),
                lookup(
                    "semanticNeighbor",
                    0.2,
                    invokedLookupMethods,
                    term -> List.of(hit(
                        "concept-1",
                        "Billing Service",
                        "semanticNeighbor"
                    ))
                )
            ));

        // When
        CandidateConceptMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(invokedLookupMethods)
            .containsExactly("exactLabel", "alias", "semanticNeighbor");
        assertThat(match.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.candidate().candidateKey()).isEqualTo("concept-1");
            assertThat(candidate.evidence()).singleElement().satisfies(evidence -> {
                assertThat(evidence.score()).isEqualTo(0.9);
            });
        });
    }

    @Test
    void rejectsPolicyConfiguration_whenNonExactWeightsCanReachFullScore() {
        // Given
        ConceptCandidateLookup exactLookup = lookup(
            "exactLabel",
            1.0,
            new ArrayList<>(),
            term -> List.of()
        );
        ConceptCandidateLookup aliasLookup = lookup(
            "alias",
            0.7,
            new ArrayList<>(),
            term -> List.of()
        );
        ConceptCandidateLookup embeddingLookup = lookup(
            "embedding",
            0.3,
            new ArrayList<>(),
            term -> List.of()
        );

        // When / Then
        assertThatThrownBy(() -> new OrderedWeightedConceptRetrievalPolicy(List.of(
            exactLookup,
            aliasLookup,
            embeddingLookup
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("non-exact lookup weights must sum to less than 1.0");
    }

    @Test
    void rejectsPolicyConfiguration_whenRequiredLookupIsMissing() {
        // Given
        ConceptCandidateLookup exactLookup = lookup(
            "exactLabel",
            1.0,
            new ArrayList<>(),
            term -> List.of()
        );

        // When / Then
        assertThatThrownBy(() -> new OrderedWeightedConceptRetrievalPolicy(List.of(exactLookup)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ordered weighted retrieval requires alias lookup");
    }

    private CandidateLookupHit hit(
        String candidateKey,
        String label,
        String lookupMethodName
    ) {
        return new CandidateLookupHit(
            new CandidateConcept(candidateKey, label, "SystemComponent"),
            lookupMethodName,
            "Matched " + label
        );
    }

    private ConceptCandidateLookup lookup(
        String lookupMethodName,
        double weight,
        List<String> invokedLookupMethods,
        Function<SelectedTerm, List<CandidateLookupHit>> hits
    ) {
        return new ConceptCandidateLookup() {
            @Override
            public String lookupMethodName() {
                return lookupMethodName;
            }

            @Override
            public double lookupWeight() {
                return weight;
            }

            @Override
            public List<CandidateLookupHit> findCandidates(
                SelectedTerm term,
                CandidateLookupScope scope
            ) {
                Objects.requireNonNull(scope, "scope must not be null");
                invokedLookupMethods.add(lookupMethodName);
                return new ArrayList<>(hits.apply(term));
            }
        };
    }
}
