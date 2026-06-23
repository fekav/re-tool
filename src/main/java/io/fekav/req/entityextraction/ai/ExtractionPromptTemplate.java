package io.fekav.req.entityextraction.ai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.fekav.platform.llm.PromptExample;
import io.fekav.platform.llm.PromptTemplate;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExtractionPromptTemplate {

    private static final String TASK = """
            Analysiere die folgende Softwareanforderung und extrahiere die \
            syntaktischen Bausteine gemäß der Rupp-Schablone.
            """;

    private static final String INSTRUCTIONS = """
            Gib ausschließlich ein JSON-Objekt zurück - kein Erklärungstext.
            Verwende null, wenn optionale Bausteine fehlen.

            Rupp-Schablone: [Wenn <CONDITION>,] <SUBJECT> <ACTION> <OBJECT> [<CONSTRAINT>].
            SUBJECT  = handelndes System oder Person (z.B. "System", "Nutzer")
            ACTION = Modalverb + Infinitiv (soll/muss/kann + Verb)
            OBJECT = betroffene Entität (Nomen/Nominalphrase)
            CONSTRAINT = zeitliche oder qualitative Einschränkung (optional)
            CONDITION  = Auslösebedingung / Vorbedingung (optional)
            """;

    private static final List<PromptExample> EXAMPLES = List.of(
            new PromptExample(
                    "Das System soll die Bestellung innerhalb von 2 Sekunden bestätigen.",
                    jsonObject(
                            "subject", "System",
                            "action", "soll bestätigen",
                            "object", "Bestellung",
                            "constraint", "innerhalb von 2 Sekunden",
                            "condition", null)),
            new PromptExample(
                    "Wenn der Nutzer eingeloggt ist, muss die Anwendung " +
                            "den Dashboard-Bereich anzeigen.",
                    jsonObject(
                            "condition", "Nutzer ist eingeloggt",
                            "subject", "Anwendung",
                            "action", "muss anzeigen",
                            "object", "Dashboard-Bereich",
                            "constraint", null)));

    private static final Map<String, Object> OUTPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "subject", Map.of("type", "string"),
                    "action", Map.of("type", "string"),
                    "object", Map.of("type", "string"),
                    "constraint", Map.of("type", List.of("string", "null")),
                    "condition", Map.of("type", List.of("string", "null"))),
            "required", List.of("subject", "action", "object"));

    private static final PromptTemplate TEMPLATE = PromptTemplate.builder()
            .inputLabel("Softwareanforderung")
            .taskDescription(TASK)
            .instructions(INSTRUCTIONS)
            .examples(EXAMPLES)
            .outputSchema(OUTPUT_SCHEMA)
            .build();

    public PromptTemplate template() {
        return TEMPLATE;
    }

    private static Map<String, Object> jsonObject(Object... keysAndValues) {
        Map<String, Object> values = new LinkedHashMap<>();

        for (int i = 0; i < keysAndValues.length; i += 2) {
            values.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }

        return Collections.unmodifiableMap(values);
    }
}
