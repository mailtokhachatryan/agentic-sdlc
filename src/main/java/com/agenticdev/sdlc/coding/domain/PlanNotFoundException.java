package com.agenticdev.sdlc.coding.domain;

import java.util.UUID;

public class PlanNotFoundException extends CodingRunException {

    public PlanNotFoundException(UUID planId) {
        super("plan_not_found", "No plan with id " + planId);
    }
}
