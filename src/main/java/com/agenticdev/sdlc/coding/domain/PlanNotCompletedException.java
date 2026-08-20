package com.agenticdev.sdlc.coding.domain;

import java.util.UUID;

public class PlanNotCompletedException extends CodingRunException {

    public PlanNotCompletedException(UUID planId, String status) {
        super("plan_not_completed",
                "Plan " + planId + " has status " + status + "; only COMPLETED plans can be coded");
    }
}
