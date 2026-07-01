package io.fekav.req.conceptmatching.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionSet;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionStatus;
import io.fekav.req.conceptmatching.domain.ConceptMatchingPolicy;
import io.fekav.req.conceptmatching.domain.ConceptMatchingService;
import io.fekav.req.conceptmatching.domain.NewConceptProposal;
import io.fekav.req.shared.event.ConceptMatchReviewRequestedEvent;
import io.fekav.req.shared.event.CreateConceptRequestedEvent;
import io.fekav.req.shared.event.ExistingConceptProposedEvent;
import io.fekav.req.shared.event.MapExistingConceptRequestedEvent;
import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.SelectedTerm;

@ExtendWith(MockitoExtension.class)
class DecideConceptMatchesCommandHandlerTest {

    @Mock
    EventPublisher eventPublisher;

    @Test
    void returnsConceptMatchDecisionSet_whenMatchingSucceeds() {
        // Given
        List<CandidateConceptMatch> handledMatches = new ArrayList<>();
        ConceptMatchingPolicy policy = match -> {
            handledMatches.add(match);
            return decisionFor(match);
        };
        DecideConceptMatchesCommandHandler handler =
            handlerWith(policy);
        DecideConceptMatchesCommand command = new DecideConceptMatchesCommand(List.of(
            matchInput(
                new SelectedTerm(RequirementElement.SUBJECT,
                    " billing service "
                ),
                List.of(candidateInput(
                    " concept-1 ",
                    " Billing Service ",
                    " SystemComponent ",
                    " conceptName ",
                    " matched concept name ",
                    1.0
                ))
            ),
            matchInput(
                new SelectedTerm(RequirementElement.ACTION,
                    "must refund"
                ),
                List.of()
            )
        ));

        // When
        ConceptMatchDecisionSet result = handler.handle(command);

        // Then
        assertThat(result.decisions())
            .extracting(ConceptMatchDecision::selectedTerm, ConceptMatchDecision::status)
            .containsExactly(
                tuple(
                    new SelectedTerm(RequirementElement.SUBJECT, "billing service"),
                    ConceptMatchDecisionStatus.PROPOSE_EXISTING
                ),
                tuple(
                    new SelectedTerm(RequirementElement.ACTION, "must refund"),
                    ConceptMatchDecisionStatus.AUTO_CREATE_NEW
                )
            );
        assertThat(handledMatches).hasSize(2);
        assertThat(handledMatches.getFirst().selectedTerm())
            .isEqualTo(new SelectedTerm(RequirementElement.SUBJECT, "billing service"));
        assertThat(handledMatches.getFirst().candidates())
            .singleElement()
            .satisfies(candidate -> {
                assertThat(candidate.candidate().candidateKey()).isEqualTo("concept-1");
                assertThat(candidate.candidate().label()).isEqualTo("Billing Service");
                assertThat(candidate.candidate().conceptType()).isEqualTo("SystemComponent");
                assertThat(candidate.evidence())
                    .singleElement()
                    .satisfies(evidence -> {
                        assertThat(evidence.policyName()).isEqualTo("conceptName");
                        assertThat(evidence.evidenceText()).isEqualTo("matched concept name");
                        assertThat(evidence.score()).isEqualTo(1.0);
                    });
            });
        assertThat(handledMatches.get(1).selectedTerm())
            .isEqualTo(new SelectedTerm(RequirementElement.ACTION, "must refund"));
        assertThat(handledMatches.get(1).candidates()).isEmpty();
    }

    @Test
    void publishesStatusSpecificEventForEachDecision_whenMatchingSucceeds() {
        // Given
        SelectedTerm autoMapTerm = new SelectedTerm(RequirementElement.SUBJECT, "billing service");
        SelectedTerm proposeTerm = new SelectedTerm(RequirementElement.ACTION, "refund payment");
        SelectedTerm reviewTerm = new SelectedTerm(RequirementElement.OBJECT, "customer account");
        SelectedTerm autoCreateTerm = new SelectedTerm(RequirementElement.CONDITION, "after timeout");
        RetrievedCandidateConcept concept1 = candidateInput(
            "concept-1",
            "Billing Service",
            "SystemComponent",
            "conceptName",
            "matched billing service",
            1.0
        );
        RetrievedCandidateConcept concept2 = candidateInput(
            "concept-2",
            "Customer Account",
            "DataObject",
            "conceptName",
            "matched customer account",
            0.8
        );
        RetrievedCandidateConcept concept3 = candidateInput(
            "concept-3",
            "Account Profile",
            "DataObject",
            "conceptName",
            "matched account profile",
            0.7
        );
        ConceptMatchDecision autoMapDecision = existingDecision(
            autoMapTerm,
            ConceptMatchDecisionStatus.AUTO_MAP_EXISTING,
            concept1,
            "Best candidate exceeded the auto-map threshold"
        );
        ConceptMatchDecision proposeDecision = existingDecision(
            proposeTerm,
            ConceptMatchDecisionStatus.PROPOSE_EXISTING,
            concept1,
            "Best candidate was proposed"
        );
        ConceptMatchDecision reviewDecision = new ConceptMatchDecision(
            reviewTerm,
            ConceptMatchDecisionStatus.REVIEW_REQUIRED,
            List.of(concept2, concept3),
            List.of(),
            "Multiple candidates require review"
        );
        ConceptMatchDecision autoCreateDecision = new ConceptMatchDecision(
            autoCreateTerm,
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            List.of(new NewConceptProposal(autoCreateTerm.text(), autoCreateTerm.requirementElement())),
            "No existing candidates found"
        );
        Map<SelectedTerm, ConceptMatchDecision> decisions = Map.of(
            autoMapTerm, autoMapDecision,
            proposeTerm, proposeDecision,
            reviewTerm, reviewDecision,
            autoCreateTerm, autoCreateDecision
        );
        DecideConceptMatchesCommandHandler handler = handlerWith(match -> decisions.get(match.selectedTerm()));
        DecideConceptMatchesCommand command = new DecideConceptMatchesCommand(List.of(
            matchInput(autoMapTerm, List.of(concept1)),
            matchInput(proposeTerm, List.of(concept1)),
            matchInput(reviewTerm, List.of(concept2, concept3)),
            matchInput(autoCreateTerm, List.of())
        ));

        // When
        ConceptMatchDecisionSet result = handler.handle(command);

        // Then
        assertThat(result.decisions())
            .containsExactly(autoMapDecision, proposeDecision, reviewDecision, autoCreateDecision);

        ArgumentCaptor<List<ApplicationEvent>> applicationEvents = eventCaptor();
        verify(eventPublisher).publishApplicationEvents(applicationEvents.capture());
        assertThat(applicationEvents.getValue())
            .satisfiesExactly(
                applicationEvent -> {
                    assertThat(applicationEvent).isInstanceOf(MapExistingConceptRequestedEvent.class);
                    MapExistingConceptRequestedEvent event =
                        (MapExistingConceptRequestedEvent) applicationEvent;
                    assertThat(event.selectedTerm()).isEqualTo(autoMapTerm);
                    assertThat(event.existingConcept()).isEqualTo(concept1.candidate());
                    assertThat(event.rationale())
                        .isEqualTo("Best candidate exceeded the auto-map threshold");
                },
                applicationEvent -> {
                    assertThat(applicationEvent).isInstanceOf(ExistingConceptProposedEvent.class);
                    ExistingConceptProposedEvent event =
                        (ExistingConceptProposedEvent) applicationEvent;
                    assertThat(event.selectedTerm()).isEqualTo(proposeTerm);
                    assertThat(event.existingConcept()).isEqualTo(concept1.candidate());
                    assertThat(event.rationale()).isEqualTo("Best candidate was proposed");
                },
                applicationEvent -> {
                    assertThat(applicationEvent).isInstanceOf(ConceptMatchReviewRequestedEvent.class);
                    ConceptMatchReviewRequestedEvent event =
                        (ConceptMatchReviewRequestedEvent) applicationEvent;
                    assertThat(event.selectedTerm()).isEqualTo(reviewTerm);
                    assertThat(event.rationale()).isEqualTo("Multiple candidates require review");
                },
                applicationEvent -> {
                    assertThat(applicationEvent).isInstanceOf(CreateConceptRequestedEvent.class);
                    CreateConceptRequestedEvent event =
                        (CreateConceptRequestedEvent) applicationEvent;
                    assertThat(event.selectedTerm()).isEqualTo(autoCreateTerm);
                    assertThat(event.rationale()).isEqualTo("No existing candidates found");
                }
            );
    }

    @Test
    void rejectsCommand_whenMatchesAreEmpty() {
        // Given / When / Then
        assertThatThrownBy(() -> new DecideConceptMatchesCommand(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("decide concept matches command has no matches");
    }

    @Test
    void rejectsCommand_whenMatchIsNull() {
        // Given
        List<CandidateConceptMatch> matches = new ArrayList<>();
        matches.add(null);

        // When / Then
        assertThatThrownBy(() -> new DecideConceptMatchesCommand(matches))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("concept match command matches must not contain null");
    }

    @Test
    void rejectsSelectedTerm_whenTextIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new SelectedTerm(RequirementElement.SUBJECT, " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("selected term text must not be blank");
    }

    @Test
    void rejectsCommand_whenSelectedTermIsDuplicated() {
        // Given
        SelectedTerm selectedTerm = new SelectedTerm(RequirementElement.SUBJECT, "billing service");

        // When / Then
        assertThatThrownBy(() -> new DecideConceptMatchesCommand(List.of(
            matchInput(selectedTerm, List.of()),
            matchInput(selectedTerm, List.of(candidateInput(
                "concept-1",
                "Billing Service",
                "SystemComponent",
                "conceptName",
                "matched concept name",
                1.0
            ))
        ))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("decide concept matches command has duplicate selected terms");
    }

    @Test
    void returnsCommandType() {
        // Given
        DecideConceptMatchesCommandHandler handler =
            handlerWith(this::decisionFor);

        // When
        Class<DecideConceptMatchesCommand> commandType = handler.commandType();

        // Then
        assertThat(commandType).isEqualTo(DecideConceptMatchesCommand.class);
    }

    private ConceptMatchDecision decisionFor(CandidateConceptMatch match) {
        if (match.candidates().isEmpty()) {
            return new ConceptMatchDecision(
                match.selectedTerm(),
                ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
                List.of(),
                List.of(new NewConceptProposal(
                    match.selectedTerm().text(),
                    match.selectedTerm().requirementElement()
                )),
                "No existing candidates found"
            );
        }

        RetrievedCandidateConcept candidate = match.candidates().getFirst();
        return new ConceptMatchDecision(
            match.selectedTerm(),
            ConceptMatchDecisionStatus.PROPOSE_EXISTING,
            List.of(candidate),
            List.of(),
            "Best candidate was proposed"
        );
    }

    private ConceptMatchDecision existingDecision(
        SelectedTerm selectedTerm,
        ConceptMatchDecisionStatus status,
        RetrievedCandidateConcept candidate,
        String rationale
    ) {
        return new ConceptMatchDecision(
            selectedTerm,
            status,
            List.of(candidate),
            List.of(),
            rationale
        );
    }

    private DecideConceptMatchesCommandHandler handlerWith(ConceptMatchingPolicy policy) {
        return new DecideConceptMatchesCommandHandler(
            eventPublisher,
            new ConceptMatchingService(policy)
        );
    }

    private CandidateConceptMatch matchInput(
        SelectedTerm selectedTerm,
        List<RetrievedCandidateConcept> candidates
    ) {
        return new CandidateConceptMatch(selectedTerm, candidates);
    }

    private RetrievedCandidateConcept candidateInput(
        String candidateKey,
        String label,
        String conceptType,
        String policyName,
        String evidenceText,
        double score
    ) {
        return new RetrievedCandidateConcept(
            new CandidateConcept(
                candidateKey,
                label,
                conceptType
            ),
            List.of(new RetrievalEvidence(
                policyName,
                evidenceText,
                score
            ))
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<List<ApplicationEvent>> eventCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }
}
