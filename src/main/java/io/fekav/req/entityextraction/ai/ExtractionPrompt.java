package io.fekav.req.entityextraction.ai;

import java.util.List;
import java.util.Map;

import io.fekav.req.shared.model.RequirementSyntaxType;


public class ExtractionPrompt {

    private final String rawRequirement;
    private final String taskDescription;
    public record FewShotPair(String input, Map<RequirementSyntaxType, String> expected) {}
    private final List<FewShotPair> fewShotPairs;
    private final Map<String, Object> outputSchema;
    private final String syntax;

    private ExtractionPrompt(Builder b) {
        this.rawRequirement = b.rawText;
        this.taskDescription = b.taskDescription;
        this.fewShotPairs = List.copyOf(b.fewShotPairs);
        this.outputSchema = Map.copyOf(b.outputSchema);
        this.syntax = b.syntax;
    }    

    // Getters
    public String getRawRequirement()             { return rawRequirement; }
    public String getTaskDescription()     { return taskDescription; }
    public List<FewShotPair> getFewShotPairs()         { return fewShotPairs; }
    public Map<String, Object> getOutputSchema()          { return outputSchema; }
    public String getSyntax()       { return syntax; }

    
    
    @Override
    public String toString() {
        StringBuilder prompt = new StringBuilder();
        prompt.append(this.taskDescription).append("\n\n");
        prompt.append(this.syntax).append("\n\n");
        prompt.append("Expected outuput:\n");
        prompt.append(outputSchema);

        if (!this.fewShotPairs.isEmpty()) {
            prompt.append("Examples:\n");
            for (FewShotPair example : this.fewShotPairs) {
                prompt.append("Input: ").append(example.input()).append("\n");
                prompt.append("Expected: ").append(example.expected()).append("\n\n");
            }
        }

        prompt.append("Entity Extraction for:\n").append(this.rawRequirement);        

        return prompt.toString();
    }

    public static Builder builder(String rawText) { return new Builder(rawText); }

    static class Builder {
        private final String rawText;
        private String taskDescription = "";
        private List<FewShotPair> fewShotPairs = List.of();
        private Map<String, Object> outputSchema = Map.of();
        private String syntax = "";

        private Builder(String rawText) { this.rawText = rawText; }
        public Builder taskDescription(String v)  { taskDescription = v; return this; }
        public Builder fewShotPairs(List<FewShotPair> v)         { fewShotPairs = v; return this; }
        public Builder outputSchema(Map<String, Object> v)          { outputSchema = v; return this; }
        public Builder syntax(String v)    { syntax = v; return this; }
        public ExtractionPrompt build()         { return new ExtractionPrompt(this); }
    }

}
