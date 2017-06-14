package org.n52.sensorweb.awi.util;

import java.util.TimerTask;

/**
 * Simple implementation of {@link TimerTask} that wraps a {@link Runnable}
 *
 * @author Christian Autermann
 */
public class DelegatingTimerTask extends TimerTask {

    private final Runnable runnable;

    /**
     * Creates a new {@code DelegatingTimerTask}.
     *
     * @param runnable the {@link Runnable} to execute
     */
    public DelegatingTimerTask(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        this.runnable.run();
    }

}
