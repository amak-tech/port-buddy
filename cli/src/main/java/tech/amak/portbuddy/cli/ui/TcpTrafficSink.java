package tech.amak.portbuddy.cli.ui;

public interface TcpTrafficSink {

    void onBytesIn(final long bytes);

    void onBytesOut(final long bytes);
}
