package io.fekav.req.entityextraction.ai;

import java.util.List;
import java.util.Map;

import io.fekav.req.shared.model.RequirementSyntaxType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExtractionPromptFactory {

    private static final String TASK = """
            Analysiere die folgende Softwareanforderung und extrahiere die \
            syntaktischen Bausteine gemäß der Rupp-Schablone.
            Gib ausschließlich ein JSON-Objekt zurück - kein Erklärungstext.
            """;

    private static final String SYNTAX = """
            Rupp-Schablone: [Wenn <CONDITION>,] <SUBJECT> <ACTION> <OBJECT> [<CONSTRAINT>].
            SUBJECT  = handelndes System oder Person (z.B. "System", "Nutzer")
            ACTION = Modalverb + Infinitiv (soll/muss/kann + Verb)
            OBJECT = betroffene Entität (Nomen/Nominalphrase)
            CONSTRAINT = zeitliche oder qualitative Einschränkung (optional)
            CONDITION  = Auslösebedingung / Vorbedingung (optional)
            """;

    private static final List<ExtractionPrompt.FewShotPair> FEW_SHOT = List.of(
            new ExtractionPrompt.FewShotPair(
                    "Das System soll die Bestellung innerhalb von 2 Sekunden bestätigen.",
                    Map.of(
                            RequirementSyntaxType.SUBJECT, "System",
                            RequirementSyntaxType.ACTION, "soll bestätigen",
                            RequirementSyntaxType.OBJECT, "Bestellung",
                            RequirementSyntaxType.CONSTRAINT, "innerhalb von 2 Sekunden")),
            new ExtractionPrompt.FewShotPair(
                    "Wenn der Nutzer eingeloggt ist, muss die Anwendung " +
                            "den Dashboard-Bereich anzeigen.",
                    Map.of(
                            RequirementSyntaxType.CONDITION, "Nutzer ist eingeloggt",
                            RequirementSyntaxType.SUBJECT, "Anwendung",
                            RequirementSyntaxType.ACTION, "muss anzeigen",
                            RequirementSyntaxType.OBJECT, "Dashboard-Bereich")));

    private static final Map<String, Object> OUTPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "subject", Map.of("type", "string"),
                    "action", Map.of("type", "string"),
                    "object", Map.of("type", "string"),
                    "constraint", Map.of("type", new String[] { "string", "null" }),
                    "condition", Map.of("type", new String[] { "string", "null" })),
            "required", new String[] { "subject", "action", "object" });

    public ExtractionPrompt create(String rawText) {
        return ExtractionPrompt.builder(rawText)
                .taskDescription(TASK)
                .syntax(SYNTAX)
                .fewShotPairs(FEW_SHOT)
                .outputSchema(OUTPUT_SCHEMA)
                .build();
    }
}