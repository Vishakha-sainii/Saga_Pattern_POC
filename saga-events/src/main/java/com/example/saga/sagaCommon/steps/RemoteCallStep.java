package com.example.saga.sagaCommon.steps;

import com.example.saga.sagaCommon.SagaStep;

public class RemoteCallStep implements SagaStep {

    private final Runnable action;
    private final Runnable compensation;

    public RemoteCallStep(Runnable action, Runnable compensation) {
        this.action = action;
        this.compensation = compensation;
    }

    public void execute() {
        action.run();
    }

    public void rollback() {
        compensation.run();
    }
}
