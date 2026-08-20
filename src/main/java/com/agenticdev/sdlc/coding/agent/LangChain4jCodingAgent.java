package com.agenticdev.sdlc.coding.agent;

import com.agenticdev.sdlc.coding.domain.CodeExecutor;
import com.agenticdev.sdlc.coding.domain.CodingAgent;
import com.agenticdev.sdlc.coding.domain.CodingBudget;
import com.agenticdev.sdlc.coding.domain.CodingResult;
import com.agenticdev.sdlc.planning.domain.PlanResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LangChain4jCodingAgent implements CodingAgent {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jCodingAgent.class);
    private static final int MAX_TOOL_OUTPUT_CHARS = 10_000;

    private final CodingAgentProperties props;
    private final ObjectMapper jsonMapper;

    public LangChain4jCodingAgent(CodingAgentProperties props) {
        this.props = props;
        this.jsonMapper = new ObjectMapper();
    }

    interface AgentProxy {
        @UserMessage("{{instructions}}")
        String run(@V("instructions") String instructions);
    }

    @Override
    public CodingResult execute(ChatModel chatModel,
                                PlanResult plan,
                                CodeExecutor executor,
                                CodingBudget budget) {
        Instant started = Instant.now();
        Toolbox toolbox = new Toolbox(executor, budget, started);

        AgentProxy proxy = AiServices.builder(AgentProxy.class)
                .chatModel(chatModel)
                .systemMessageProvider(memoryId -> props.agent().systemPrompt())
                .tools(toolbox)
                .build();

        String instructions = buildInstructions(plan);
        CodingBudget.ExhaustReason exhaust = null;
        try {
            String finalResponse = proxy.run(instructions);
            log.info("Coding agent finished in {} iterations. Final response: {}",
                    toolbox.iterations.get(),
                    finalResponse == null ? "(none)"
                            : finalResponse.substring(0, Math.min(200, finalResponse.length())));
        } catch (BudgetExhaustedException e) {
            exhaust = e.reason;
            log.info("Coding agent halted: budget exhausted ({})", exhaust);
        } catch (RuntimeException e) {
            log.warn("Coding agent loop raised runtime exception: {}", e.getMessage());
        }

        boolean testsPassed = runFinalTestCommand(executor);
        String diff = executor.getDiff();
        int filesChanged = executor.countChangedFiles();

        return new CodingResult(
                diff == null ? "" : diff,
                filesChanged,
                testsPassed,
                toolbox.iterations.get(),
                toolbox.tokensUsed.get(),
                toolbox.filesModified,
                exhaust
        );
    }

    private boolean runFinalTestCommand(CodeExecutor executor) {
        String testCmd = props.agent().testCommand();
        if (testCmd == null || testCmd.isBlank()) return false;
        try {
            CodeExecutor.CommandResult r = executor.runCommand(testCmd);
            return r.succeeded();
        } catch (RuntimeException e) {
            log.warn("Final test command failed to execute: {}", e.getMessage());
            return false;
        }
    }

    private String buildInstructions(PlanResult plan) {
        try {
            String planJson = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(plan);
            return "Implement the following plan in the repository workspace.\n\n"
                    + "PLAN:\n" + planJson + "\n\n"
                    + "Use the provided tools to read files, make changes, and run the build/test command. "
                    + "When tests pass and all plan tasks are implemented, reply with a brief summary.";
        } catch (JsonProcessingException e) {
            return "Implement the plan. (failed to serialize plan: " + e.getMessage() + ")";
        }
    }

    /**
     * Tools exposed to the LLM. Every tool invocation first enforces the budget —
     * if exhausted, {@link BudgetExhaustedException} propagates out of the LangChain4j
     * loop and is caught by the agent.
     */
    public static class Toolbox {

        private final CodeExecutor executor;
        private final CodingBudget budget;
        private final Instant started;
        final AtomicInteger iterations = new AtomicInteger();
        final AtomicLong tokensUsed = new AtomicLong();
        final List<String> filesModified = new ArrayList<>();

        Toolbox(CodeExecutor executor, CodingBudget budget, Instant started) {
            this.executor = executor;
            this.budget = budget;
            this.started = started;
        }

        @Tool("Read a file from the repository and return its contents.")
        public String readFile(@P("path relative to repo root") String path) {
            checkBudget();
            return truncate(executor.readFile(path));
        }

        @Tool("Write content to a file in the repository. Overwrites existing content; creates parent directories as needed.")
        public String writeFile(@P("path relative to repo root") String path,
                                @P("full file content") String content) {
            checkBudget();
            executor.writeFile(path, content);
            if (!filesModified.contains(path)) filesModified.add(path);
            return "wrote " + content.length() + " bytes to " + path;
        }

        @Tool("List files in a directory (pass recursive=true to traverse subdirectories).")
        public List<String> listFiles(@P("path") String path, @P("recursive") boolean recursive) {
            checkBudget();
            return executor.listFiles(path, recursive);
        }

        @Tool("Run a shell command in the repo root. Returns stdout+stderr combined. Use this for 'mvn test', 'npm test', etc.")
        public String runCommand(@P("shell command") String command) {
            checkBudget();
            CodeExecutor.CommandResult r = executor.runCommand(command);
            return "exit=" + r.exitCode() + "\n" + truncate(r.output());
        }

        private void checkBudget() {
            int it = iterations.incrementAndGet();
            if (it > budget.maxIterations()) {
                throw new BudgetExhaustedException(CodingBudget.ExhaustReason.ITERATION_LIMIT);
            }
            Duration elapsed = Duration.between(started, Instant.now());
            if (elapsed.compareTo(budget.maxDuration()) >= 0) {
                throw new BudgetExhaustedException(CodingBudget.ExhaustReason.TIME_LIMIT);
            }
            if (tokensUsed.get() >= budget.maxTokens()) {
                throw new BudgetExhaustedException(CodingBudget.ExhaustReason.TOKEN_BUDGET);
            }
        }

        private static String truncate(String s) {
            if (s == null) return "";
            if (s.length() <= MAX_TOOL_OUTPUT_CHARS) return s;
            return s.substring(0, MAX_TOOL_OUTPUT_CHARS)
                    + "\n...[truncated " + (s.length() - MAX_TOOL_OUTPUT_CHARS) + " chars]";
        }
    }

    private static final class BudgetExhaustedException extends RuntimeException {
        private final CodingBudget.ExhaustReason reason;
        BudgetExhaustedException(CodingBudget.ExhaustReason reason) {
            super("budget exhausted: " + reason);
            this.reason = reason;
        }
    }
}
