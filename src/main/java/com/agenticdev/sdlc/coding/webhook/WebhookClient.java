package com.agenticdev.sdlc.coding.webhook;

import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;

public interface WebhookClient {

    /** Fire-and-forget. Returns true if delivery succeeded (2xx response); never throws. */
    boolean notify(String url, CodingRunRecord record);
}
