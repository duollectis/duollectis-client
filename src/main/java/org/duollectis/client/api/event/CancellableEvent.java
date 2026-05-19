package org.duollectis.client.api.event;

import lombok.Getter;

public abstract class CancellableEvent implements Event {

    @Getter
    private boolean cancelled;

    public void cancel() {
        cancelled = true;
    }

}
