package io.fekav.req.conceptmatching.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionStatus;
import io.fekav.req.conceptmatching.domain.ConceptMatchingPolicy;
import io.fekav.req.conceptmatching.domain.ConceptMatchingService;
import io.fekav.req.shared.event.ConceptMatchEvaluatedEvent;
import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.RequirementElement;

@ExtendWith(MockitoExtension.class)
class EvaluateConceptMatchCommandHandlerTest {

    @Mock
    EventPublisher eventPublisher;

    @Test
    void returnsConceptMatchEvaluatedEventAndPublishesSameEvent_whenMatchingSucceeds() {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        List<CandidateConceptMatch> handledMatches = new ArrayList<>();
        ConceptMatchingPolicy policy = match -> {
            handledMatches.add(match);
            return decisionFor(match);
        };
        EvaluateConceptMatchCommandHandler handler = handlerWith(policy);
        CandidateConceptMatch match = matchInput(
            new RequirementElement(RequirementElementType.SUBJECT, " billing service "),
            List.of(candidateInput(
                " concept-1 ",
                " Billing Service ",
                " SystemComponent ",
                " conceptName ",
                " matched concept name ",
                1.0
            ))
        );
        EvaluateConceptMatchCommand command = new EvaluateConceptMatchCommand(correlationId, match);

        // When
        ConceptMatchEvaluatedEvent result = handler.handle(command);

        // Then
        assertThat(result.correlationId()).isEqualTo(correlationId);
        assertThat(result.decision().requirementElement())
            .isEqualTo(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));
        assertThat(result.decision().status())
            .isEqualTo(ConceptMatchDecisionStatus.PROPOSE_EXISTING);
        assertThat(handledMatches).containsExactly(match);
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
        verify(eventPublisher).publish(result);
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    void returnsAutoCreateDecisionReceipt_whenMatchHasNoCandidates() {
        // Given
        EvaluateConceptMatchCommandHandler handler = handlerWith(this::decisionFor);
        CandidateConceptMatch match = matchInput(
            new RequirementElement(RequirementElementType.ACTION, "must refund"),
            List.of()
        );

        // When
        ConceptMatchEvaluatedEvent result =
            handler.handle(new EvaluateConceptMatchCommand(match));

        // Then
        assertThat(result.decision().requirementElement()).isEqualTo(match.requirementElement());
        assertThat(result.decision().status())
            .isEqualTo(ConceptMatchDecisionStatus.AUTO_CREATE_NEW);
        assertThat(result.decision().candidates()).isEmpty();
        verify(eventPublisher).publish(result);
    }

    @Test
    void rejectsCommand_whenMatchIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new EvaluateConceptMatchCommand(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("match must not be null");
    }

    @Test
    void rejectsSelectedTerm_whenTextIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new RequirementElement(RequirementElementType.SUBJECT, " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("selected term text must not be blank");
    }

    @Test
    void returnsCommandType() {
        // Given
        EvaluateConceptMatchCommandHandler handler =
            handlerWith(this::decisionFor);

        // When
        Class<EvaluateConceptMatchCommand> commandType = handler.commandType();

        // Then
        assertThat(commandType).isEqualTo(EvaluateConceptMatchCommand.class);
    }

    private ConceptMatchDecision decisionFor(CandidateConceptMatch match) {
        if (match.candidates().isEmpty()) {
            return new ConceptMatchDecision(
                match.requirementElement(),
                ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
                List.of(),
                "No existing candidates found"
            );
        }

        RetrievedCandidateConcept candidate = match.candidates().getFirst();
        return new ConceptMatchDecision(
            match.requirementElement(),
            ConceptMatchDecisionStatus.PROPOSE_EXISTING,
            List.of(candidate),
            "Best candidate was proposed"
        );
    }

    private EvaluateConceptMatchCommandHandler handlerWith(ConceptMatchingPolicy policy) {
        return new EvaluateConceptMatchCommandHandler(
            eventPublisher,
            new ConceptMatchingService(policy)
        );
    }

    private CandidateConceptMatch matchInput(
        RequirementElement selectedTerm,
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
}
