/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.cli.ui;

public interface NetTrafficSink {

    void onBytesIn(final long bytes);

    void onBytesOut(final long bytes);
}
