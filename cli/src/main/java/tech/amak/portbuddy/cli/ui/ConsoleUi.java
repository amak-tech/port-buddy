/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.amak.portbuddy.cli.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.cli.config.ConfigurationService;
import tech.amak.portbuddy.common.ClientConfig;
import tech.amak.portbuddy.common.TunnelType;
import tech.amak.portbuddy.common.dto.auth.RegisterRequest;

@Slf4j
@RequiredArgsConstructor
public class ConsoleUi implements HttpLogSink, NetTrafficSink {

    public record HttpLog(String method, String url, int status) {
    }

    private final TunnelType tunnelType;
    private final String localDetails;
    private final String publicDetails;

    private Terminal terminal;
    private PrintWriter out;
    private final Deque<HttpLog> httpLogs = new ArrayDeque<>();
    private final AtomicLong inBytes = new AtomicLong();
    private final AtomicLong outBytes = new AtomicLong();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CountDownLatch exit = new CountDownLatch(1);
    private final ClientConfig config = ConfigurationService.INSTANCE.getConfig();

    private Thread renderThread;

    @Getter
    @Setter
    private Runnable onExit;

    /**
     * Prompts the user for registration details using the console.
     * When no API key is initialized, the user should only be asked for the email address.
     *
     * @return a RegisterRequest containing the input data (email only)
     * @throws IOException if an I/O error occurs or console is not available
     */
    public static RegisterRequest promptForUserRegistration() throws IOException {
        try (final var terminal = buildTerminal()) {
            final var reader = LineReaderBuilder.builder().terminal(terminal).build();

            String email;
            while (true) {
                email = reader.readLine("Email: ");
                if (isValidEmail(email)) {
                    break;
                }
                terminal.writer().println("Invalid email address. Please try again.");
                terminal.flush();
            }

            // Only email is required for registration. Name and password are set on the server side.
            return new RegisterRequest(email, null, null);
        }
    }

    private static boolean isValidEmail(final String email) {
        return email != null && email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    private static Terminal buildTerminal() throws IOException {
        final var terminal = TerminalBuilder.builder()
            .streams(System.in, System.out)
            .system(true)
            .jansi(true)
            .jna(true)
            .jni(true)
            .ffm(true)
            .exec(true)
            .dumb(true)
            .build();
        return terminal;
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
            terminal = buildTerminal();
            out = terminal.writer();
            terminal.handle(Terminal.Signal.INT, signal -> stop());
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to start console UI", e);
        }

        clear();

        out.printf("Port Buddy - Mode: %s%n", tunnelType.name().toLowerCase());
        out.println();
        out.printf("Local:  %s%n", localDetails);
        out.printf("Public: %s%n", publicDetails);
        out.println();
        out.println("Press Ctrl+C to exit");
        out.flush();

        if (config.isLogEnabled()) {
            out.println("----------------------------------------------");
            out.println();
            if (tunnelType == TunnelType.HTTP) {
                out.println("HTTP requests log:");
            } else {
                out.println(tunnelType + " traffic:");
            }
            out.flush();

            renderThread = new Thread(this::renderLoop, "port-buddy-ui");
            renderThread.setDaemon(true);
            renderThread.start();
        }
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
        inBytes.addAndGet(Math.max(0, bytes));
    }

    @Override
    public void onBytesOut(final long bytes) {
        outBytes.addAndGet(Math.max(0, bytes));
    }

    private void renderLoop() {
        final var frameDelay = Duration.ofMillis(config.getConsoleFrameDelayMs());
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

        clear();
    }

    private void clear() {
        try {
            terminal.puts(InfoCmp.Capability.clear_screen);
            terminal.flush();
        } catch (final Exception ignore) {
            // ignore
        }
    }

    private void render() {
        if (tunnelType == TunnelType.HTTP) {

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
            final var inKb = inBytes.get() / 1024.0;
            final var outKb = outBytes.get() / 1024.0;
            out.printf("IN %.2f KB | OUT %.2f KB%n", inKb, outKb);
        }

        out.flush();
    }

    private String safe(final String value) {
        return value == null ? "" : value;
    }
}
