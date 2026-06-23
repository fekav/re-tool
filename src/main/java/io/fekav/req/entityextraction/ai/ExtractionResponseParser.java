package io.fekav.req.entityextraction.ai;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.entityextraction.model.RequirementSyntax;
import io.fekav.req.shared.model.RequirementSyntaxType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ExtractionResponseParser {

    private final ObjectMapper mapper;

    @Inject
    public ExtractionResponseParser(ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

    public RequirementSyntax parse(String responseJson) {
        try {
            // ObjectMapper mapper = new ObjectMapper();

            JsonNode root = mapper.readTree(responseJson);
            String innerJson = root.get("response").asText();
            JsonNode inner = mapper.readTree(innerJson);

            Map<RequirementSyntaxType, String> syntaxElements = new EnumMap<>(RequirementSyntaxType.class);

            Map<String, RequirementSyntaxType> fieldMapping = Map.of(
                    "subject", RequirementSyntaxType.SUBJECT,
                    "action", RequirementSyntaxType.ACTION,
                    "object", RequirementSyntaxType.OBJECT,
                    "constraint", RequirementSyntaxType.CONSTRAINT,
                    "condition", RequirementSyntaxType.CONDITION);

            fieldMapping.forEach((field, type) -> {
                JsonNode node = inner.get(field);
                if (node != null && !node.isNull()) {
                    String value = node.asText().trim();
                    if (!value.isEmpty()) {
                        syntaxElements.put(type, value);
                    }
                }
            });

            return new RequirementSyntax(Collections.unmodifiableMap(syntaxElements));

        } catch (Exception e) {
            throw new ExtractionResponseParsingException("Failed to parse extraction response", e);
        }
    }

}
