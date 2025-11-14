package tech.amak.portbuddy.cli.ui;

import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.cli.config.ConfigurationService;
import tech.amak.portbuddy.common.ClientConfig;
import tech.amak.portbuddy.common.Mode;

@Slf4j
public class ConsoleUi implements HttpLogSink, TcpTrafficSink {

    public record HttpLog(String method, String url, int status) {
    }

    private final Mode mode;
    private final String localDetails;
    private final String publicDetails;

    private Terminal terminal;
    private PrintWriter out;
    private final Deque<HttpLog> httpLogs = new ArrayDeque<>();
    private final AtomicLong tcpInBytes = new AtomicLong();
    private final AtomicLong tcpOutBytes = new AtomicLong();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CountDownLatch exit = new CountDownLatch(1);
    private final ClientConfig config = ConfigurationService.INSTANCE.getConfig();

    private Thread renderThread;

    @Getter
    @Setter
    private Runnable onExit;

    public ConsoleUi(final Mode mode, final String localDetails, final String publicDetails) {
        this.mode = mode;
        this.localDetails = localDetails;
        this.publicDetails = publicDetails;
    }

    /**
     * Starts the Console UI. This method initializes the terminal for interactive
     * output, sets up signal handling for interrupt signals, and spawns a new thread
     * to handle the rendering loop.
     * The method ensures that the UI is started only once by using an atomic flag. If
     * the UI is already running, the method exits early.
     * In case of initialization failure, such as terminal setup issues, an
     * IllegalStateException is thrown to indicate that the UI failed to start.
     * Once started, the rendering process runs on a daemon thread that periodically
     * updates the terminal output. The rendering loop works until instructed to stop
     * by external signals or method calls.
     */
    public void start() {
        if (running.getAndSet(true)) {
            return;
        }
        try {
            terminal = TerminalBuilder.builder().system(true).build();
            out = terminal.writer();
            terminal.handle(Terminal.Signal.INT, signal -> {
                stop();
            });
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to start console UI", e);
        }

        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();

        out.printf("Port Buddy - Mode: %s%n", mode.name().toLowerCase());
        out.println();
        out.printf("Local:  %s%n", localDetails);
        out.printf("Public: %s%n", publicDetails);
        out.println();
        out.println("Press Ctrl+C to exit");
        out.println("----------------------------------------------");
        out.println();
        out.println("HTTP requests log:");


        renderThread = new Thread(this::renderLoop, "port-buddy-ui");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    /**
     * Waits for the termination signal of the Console UI. This method blocks the
     * current thread until the internal exit condition is triggered, allowing
     * proper synchronization for dependent operations.
     * In case the thread is interrupted while waiting, the interrupt status is
     * restored to ensure the interruption can be detected by other parts of the
     * application.
     */
    public void waitForExit() {
        try {
            exit.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stops the Console UI. This method transitions the system out of a running state,
     * allowing for a clean shutdown. It ensures that the rendering loop halts,
     * decrements the exit latch, and executes the optional exit callback if provided.
     * The method is thread-safe and idempotent; it ensures that stopping the UI
     * multiple times has no adverse effects. If the UI is not currently running,
     * the method exits without performing any operations.
     * If an {@link Runnable} callback is set via {@link #setOnExit(Runnable)},
     * the callback is executed upon stopping. Any exceptions thrown by the callback
     * are caught and logged to avoid disrupting the shutdown process.
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }
        exit.countDown();
        if (onExit != null) {
            try {
                onExit.run();
            } catch (final Exception e) {
                log.warn("onExit handler failed: {}", e.toString());
            }
        }
    }

    @Override
    public void onHttpLog(final String method, final String url, final int status) {
        synchronized (httpLogs) {
            if (httpLogs.size() == config.getLogLinesCount()) {
                httpLogs.removeFirst();
            }
            httpLogs.addLast(new HttpLog(method, url, status));
        }
    }

    @Override
    public void onBytesIn(final long bytes) {
        tcpInBytes.addAndGet(Math.max(0, bytes));
    }

    @Override
    public void onBytesOut(final long bytes) {
        tcpOutBytes.addAndGet(Math.max(0, bytes));
    }

    private void renderLoop() {
        final var frameDelay = Duration.ofMillis(200);
        while (running.get()) {
            try {
                terminal.puts(InfoCmp.Capability.cursor_address, 9, 0);
                terminal.flush();
                render();

                Thread.sleep(frameDelay.toMillis());
            } catch (final Exception e) {
                log.debug("Render loop error: {}", e.toString());
            }
        }
        try {
            terminal.puts(InfoCmp.Capability.clear_screen);
            terminal.flush();
        } catch (final Exception ignore) {
            // ignore
        }
    }

    private void render() {
        if (mode == Mode.HTTP) {

            synchronized (httpLogs) {
                if (httpLogs.isEmpty()) {
                    out.println("(no requests yet)");
                } else {
                    httpLogs.forEach(httpLog -> {
                        terminal.puts(InfoCmp.Capability.clr_eol);
                        terminal.flush();
                        out.printf("%-6s %-3d %s%n", safe(httpLog.method()), httpLog.status(), safe(httpLog.url()));
                    });
                }
            }
        } else {
            final var inKb = tcpInBytes.get() / 1024.0;
            final var outKb = tcpOutBytes.get() / 1024.0;
            out.printf("TCP traffic: IN %.2f KB | OUT %.2f KB%n", inKb, outKb);
        }

        out.flush();
    }

    private String safe(final String value) {
        return value == null ? "" : value;
    }
}
