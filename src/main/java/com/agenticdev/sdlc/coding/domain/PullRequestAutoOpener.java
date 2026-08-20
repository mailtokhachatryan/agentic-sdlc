package com.agenticdev.sdlc.coding.domain;

import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;

/**
 * Optional port: when a coding run completes with tests passing and
 * {@link CodingRunRecord#isAutoOpenPr()} is true, {@link CodingService} invokes
 * this hook. Implementations live in the {@code github/} slice.
 */
public interface PullRequestAutoOpener {
    void openFor(CodingRunRecord completedRun);
}
