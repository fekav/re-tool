package io.fekav.req.conceptmatching.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionSet;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionStatus;
import io.fekav.req.conceptmatching.domain.ConceptMatchingPolicy;
import io.fekav.req.conceptmatching.domain.ConceptMatchingService;
import io.fekav.req.conceptmatching.domain.NewConceptProposal;
import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.SelectedTerm;

class DecideConceptMatchesCommandHandlerTest {

    @Test
    void returnsConceptMatchDecisionSet_whenMatchingSucceeds() {
        // Given
        List<CandidateConceptMatch> handledMatches = new ArrayList<>();
        ConceptMatchingPolicy policy = match -> {
            handledMatches.add(match);
            return decisionFor(match);
        };
        DecideConceptMatchesCommandHandler handler =
            new DecideConceptMatchesCommandHandler(new ConceptMatchingService(policy));
        DecideConceptMatchesCommand command = new DecideConceptMatchesCommand(List.of(
            matchInput(
                new SelectedTerm(
                    " SUBJECT ",
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
                new SelectedTerm(
                    "ACTION",
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
                    new SelectedTerm("SUBJECT", "billing service"),
                    ConceptMatchDecisionStatus.PROPOSE_EXISTING
                ),
                tuple(
                    new SelectedTerm("ACTION", "must refund"),
                    ConceptMatchDecisionStatus.AUTO_CREATE_NEW
                )
            );
        assertThat(handledMatches).hasSize(2);
        assertThat(handledMatches.getFirst().selectedTerm())
            .isEqualTo(new SelectedTerm("SUBJECT", "billing service"));
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
            .isEqualTo(new SelectedTerm("ACTION", "must refund"));
        assertThat(handledMatches.get(1).candidates()).isEmpty();
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
        assertThatThrownBy(() -> new SelectedTerm("SUBJECT", " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("selected term text must not be blank");
    }

    @Test
    void rejectsCommand_whenSelectedTermIsDuplicated() {
        // Given
        SelectedTerm selectedTerm = new SelectedTerm("SUBJECT", "billing service");

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
            new DecideConceptMatchesCommandHandler(
                new ConceptMatchingService(this::decisionFor)
            );

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
                    match.selectedTerm().syntaxRole()
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
}
