package com.example.saga.sagaCommon;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class SagaContext {

    private static final ThreadLocal<SagaContext> HOLDER =
            ThreadLocal.withInitial(SagaContext::new);

    private final Deque<SagaStep> steps = new ArrayDeque<>();

    private String sagaId;

    private SagaContext() {}

    public static SagaContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /* =====================
       Saga ID
       ===================== */

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getSagaId() {
        return sagaId;
    }

    /* =====================
       Steps
       ===================== */

    public void addStep(SagaStep step) {
        steps.push(step);
    }

    public void execute() {
        for (Iterator<SagaStep> it = steps.descendingIterator(); it.hasNext(); ) {
            SagaStep step = it.next();
            step.execute();
        }
    }

    public void rollback() {
        while (!steps.isEmpty()) {
            try {
                steps.pop().rollback();
            } catch (Exception ignored) {
            }
        }
    }
}
