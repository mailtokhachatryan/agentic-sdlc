package com.agenticdev.sdlc.coding.agent;

import com.agenticdev.sdlc.coding.domain.CodeExecutor;
import com.agenticdev.sdlc.coding.domain.CodingBudget;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolboxBudgetTest {

    @Test
    void readFile_succeedsUnderBudget() {
        CodeExecutor executor = mock(CodeExecutor.class);
        when(executor.readFile("foo")).thenReturn("hello");
        CodingBudget budget = new CodingBudget(1_000_000L, 100, Duration.ofMinutes(10), "1g", 1.0);
        LangChain4jCodingAgent.Toolbox toolbox = new LangChain4jCodingAgent.Toolbox(executor, budget, Instant.now());

        assertThat(toolbox.readFile("foo")).isEqualTo("hello");
    }

    @Test
    void iterationLimit_triggersBudgetExhausted() {
        CodeExecutor executor = mock(CodeExecutor.class);
        when(executor.readFile("foo")).thenReturn("hello");
        CodingBudget budget = new CodingBudget(1_000_000L, 1, Duration.ofMinutes(10), "1g", 1.0);
        LangChain4jCodingAgent.Toolbox toolbox = new LangChain4jCodingAgent.Toolbox(executor, budget, Instant.now());

        toolbox.readFile("foo"); // iteration 1, ok
        assertThatThrownBy(() -> toolbox.readFile("foo"))
                .hasMessageContaining("ITERATION_LIMIT");
    }

    @Test
    void timeLimit_triggersBudgetExhausted() {
        CodeExecutor executor = mock(CodeExecutor.class);
        when(executor.readFile("foo")).thenReturn("hello");
        CodingBudget budget = new CodingBudget(1_000_000L, 100, Duration.ofMillis(1), "1g", 1.0);
        Instant pastStart = Instant.now().minusSeconds(10);
        LangChain4jCodingAgent.Toolbox toolbox = new LangChain4jCodingAgent.Toolbox(executor, budget, pastStart);

        assertThatThrownBy(() -> toolbox.readFile("foo"))
                .hasMessageContaining("TIME_LIMIT");
    }

    @Test
    void tokenBudget_triggersBudgetExhausted() throws Exception {
        CodeExecutor executor = mock(CodeExecutor.class);
        when(executor.readFile("foo")).thenReturn("hello");
        CodingBudget budget = new CodingBudget(100L, 100, Duration.ofMinutes(10), "1g", 1.0);
        LangChain4jCodingAgent.Toolbox toolbox = new LangChain4jCodingAgent.Toolbox(executor, budget, Instant.now());
        // simulate tokens exhausted by reflecting into the atomic
        Field f = LangChain4jCodingAgent.Toolbox.class.getDeclaredField("tokensUsed");
        f.setAccessible(true);
        ((java.util.concurrent.atomic.AtomicLong) f.get(toolbox)).set(200L);

        assertThatThrownBy(() -> toolbox.readFile("foo"))
                .hasMessageContaining("TOKEN_BUDGET");
    }
}
