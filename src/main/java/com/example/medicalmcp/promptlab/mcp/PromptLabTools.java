package com.example.medicalmcp.promptlab.mcp;

import com.example.medicalmcp.promptlab.config.PromptLabProperties;
import com.example.medicalmcp.promptlab.domain.ClassificationEvalSummary;
import com.example.medicalmcp.promptlab.domain.EvalRun;
import com.example.medicalmcp.promptlab.domain.ImprovePromptResult;
import com.example.medicalmcp.promptlab.domain.PromptTemplate;
import com.example.medicalmcp.promptlab.domain.TemplateCompareEntry;
import com.example.medicalmcp.promptlab.service.MetaPromptImprovementService;
import com.example.medicalmcp.promptlab.service.SpecialtyPromptClassificationService;
import com.example.medicalmcp.promptlab.template.PromptTemplateRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prompt-lab")
public class PromptLabTools {

    private final SpecialtyPromptClassificationService classificationService;
    private final MetaPromptImprovementService metaPromptImprovementService;
    private final PromptTemplateRegistry templateRegistry;
    private final PromptLabProperties properties;

    public PromptLabTools(
            SpecialtyPromptClassificationService classificationService,
            MetaPromptImprovementService metaPromptImprovementService,
            PromptTemplateRegistry templateRegistry,
            PromptLabProperties properties) {
        this.classificationService = classificationService;
        this.metaPromptImprovementService = metaPromptImprovementService;
        this.templateRegistry = templateRegistry;
        this.properties = properties;
    }

    @McpTool(
            name = "evaluate_specialty_prompt",
            description = "Evaluate a specialty-classification prompt template on dataset cases (prompt-lab only).",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public ClassificationEvalSummary evaluateSpecialtyPrompt(
            @McpToolParam(description = "Template id from list_prompt_templates", required = true) String templateId,
            @McpToolParam(description = "Dataset split: train | validation | test", required = false) String split,
            @McpToolParam(description = "Max cases to evaluate (default 10)", required = false) Integer limit,
            @McpToolParam(description = "Persist run id for improve_specialty_prompt", required = false)
                    Boolean saveRunId) {
        String evalSplit = split != null ? split : properties.getEvalSplit();
        int evalLimit = limit != null ? limit : properties.getDefaultLimit();
        boolean save = saveRunId == null || saveRunId;
        EvalRun run = classificationService.evaluate(templateId, evalSplit, evalLimit, save);
        return run.summary();
    }

    @McpTool(
            name = "compare_specialty_prompts",
            description = "Compare multiple prompt templates by accuracy on the same case sample (prompt-lab only).",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public List<TemplateCompareEntry> compareSpecialtyPrompts(
            @McpToolParam(description = "Template ids to compare", required = true) List<String> templateIds,
            @McpToolParam(description = "Dataset split (default validation)", required = false) String split,
            @McpToolParam(description = "Max cases per template (default 10)", required = false) Integer limit) {
        String evalSplit = split != null ? split : properties.getEvalSplit();
        int evalLimit = limit != null ? limit : properties.getDefaultLimit();
        List<TemplateCompareEntry> entries = new ArrayList<>();
        for (String templateId : templateIds) {
            EvalRun run = classificationService.evaluate(templateId, evalSplit, evalLimit, false);
            ClassificationEvalSummary summary = run.summary();
            entries.add(new TemplateCompareEntry(
                    templateId,
                    summary.accuracy(),
                    summary.correct(),
                    summary.total(),
                    summary.meetsGate(properties.getMinAccuracy())));
        }
        entries.sort(Comparator.comparingDouble(TemplateCompareEntry::accuracy).reversed());
        return entries;
    }

    @McpTool(
            name = "improve_specialty_prompt",
            description = "Meta-improve a template using failure examples from a prior eval run (prompt-lab only).",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false))
    public ImprovePromptResult improveSpecialtyPrompt(
            @McpToolParam(description = "Base template id", required = true) String baseTemplateId,
            @McpToolParam(description = "Eval run id from evaluate_specialty_prompt (latest if omitted)", required = false)
                    String evalRunId) {
        return metaPromptImprovementService.improve(baseTemplateId, evalRunId);
    }

    @McpTool(
            name = "gate_specialty_prompt",
            description = "Gate a template on the test split against min-accuracy (prompt-lab only).",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public ClassificationEvalSummary gateSpecialtyPrompt(
            @McpToolParam(description = "Template id to gate", required = true) String templateId,
            @McpToolParam(description = "Split to use (default test)", required = false) String split,
            @McpToolParam(description = "Max cases (default 10)", required = false) Integer limit) {
        String gateSplit = split != null ? split : "test";
        int evalLimit = limit != null ? limit : properties.getDefaultLimit();
        EvalRun run = classificationService.evaluate(templateId, gateSplit, evalLimit, true);
        return run.summary();
    }

    @McpTool(
            name = "list_prompt_templates",
            description = "List built-in and meta-improved specialty classification templates (prompt-lab only).",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public List<PromptTemplate> listPromptTemplates() {
        return templateRegistry.all();
    }
}
