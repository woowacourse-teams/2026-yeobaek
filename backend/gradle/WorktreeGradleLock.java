import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.LockSupport;

import org.gradle.wrapper.GradleWrapperMain;

/**
 * Serializes Gradle wrapper executions inside one worktree.
 *
 * <p>This source-file launcher is intentionally kept outside the application source sets. The generated
 * wrapper scripts invoke it with the wrapper JAR on the classpath before Gradle starts.</p>
 */
public final class WorktreeGradleLock {

    private static final String APP_HOME_PROPERTY = "yeobaek.gradle.app-home";
    private static final String LOCKED_MARKER = "[YEOBAEK_GRADLE_LOCKED]";
    private static final String LOCK_ERROR_MARKER = "[YEOBAEK_GRADLE_LOCK_ERROR]";
    private static final int LOCKED_EXIT_CODE = 75;
    private static final long LOCK_POSITION = 0L;
    private static final long LOCK_SIZE = 1L;
    private static final long METADATA_POSITION = LOCK_POSITION + LOCK_SIZE;
    private static final Duration OWNER_METADATA_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration OWNER_METADATA_RETRY_DELAY = Duration.ofMillis(10);

    private WorktreeGradleLock() {
    }

    public static void main(String[] args) {
        LockHandle lockHandle;
        try {
            lockHandle = acquireLock();
        } catch (IOException | RuntimeException exception) {
            System.err.println(LOCK_ERROR_MARKER);
            System.err.println("reason=" + singleLine(exception.getMessage()));
            System.exit(1);
            return;
        }

        if (lockHandle == null) {
            System.exit(LOCKED_EXIT_CODE);
            return;
        }

        try (lockHandle) {
            GradleWrapperMain.main(args);
        } catch (IOException exception) {
            System.err.println(LOCK_ERROR_MARKER);
            System.err.println("reason=" + singleLine(exception.getMessage()));
            System.exit(1);
        }
    }

    private static LockHandle acquireLock() throws IOException {
        Path appHome = resolveAppHome();
        Path lockDirectory = appHome.resolve(".gradle");
        Files.createDirectories(lockDirectory);
        Path lockPath = lockDirectory.resolve("yeobaek-worktree-gradle.lock");

        FileChannel channel = FileChannel.open(
                lockPath,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.READ,
                java.nio.file.StandardOpenOption.WRITE
        );
        try {
            long deadline = System.nanoTime() + OWNER_METADATA_TIMEOUT.toNanos();
            do {
                FileLock lock = tryLock(channel);
                if (lock != null) {
                    try {
                        writeOwnerMetadata(channel, appHome);
                        return new LockHandle(channel, lock);
                    } catch (IOException | RuntimeException exception) {
                        lock.close();
                        throw exception;
                    }
                }

                String ownerMetadata;
                try {
                    ownerMetadata = readOwnerMetadata(channel);
                } catch (IOException ignored) {
                    ownerMetadata = "";
                }
                if (isCurrentOwnerMetadata(ownerMetadata, appHome)) {
                    printLockOwner(ownerMetadata);
                    channel.close();
                    return null;
                }

                LockSupport.parkNanos(OWNER_METADATA_RETRY_DELAY.toNanos());
            } while (!Thread.currentThread().isInterrupted() && System.nanoTime() < deadline);

            throw new IOException("owner-metadata-unavailable");
        } catch (IOException | RuntimeException exception) {
            channel.close();
            throw exception;
        }
    }

    private static Path resolveAppHome() throws IOException {
        String configuredAppHome = System.getProperty(APP_HOME_PROPERTY);
        if (configuredAppHome == null || configuredAppHome.isBlank()) {
            throw new IOException("missing-system-property:" + APP_HOME_PROPERTY);
        }
        return Path.of(configuredAppHome).toRealPath();
    }

    private static FileLock tryLock(FileChannel channel) throws IOException {
        try {
            return channel.tryLock(LOCK_POSITION, LOCK_SIZE, false);
        } catch (OverlappingFileLockException exception) {
            return null;
        }
    }

    private static void writeOwnerMetadata(FileChannel channel, Path appHome) throws IOException {
        ProcessHandle currentProcess = ProcessHandle.current();
        Instant startedAt = currentProcess.info().startInstant().orElseGet(Instant::now);
        String metadata = "ownerPid=" + currentProcess.pid() + System.lineSeparator()
                + "ownerStartedAt=" + startedAt + System.lineSeparator()
                + "worktree=" + worktreePath(appHome) + System.lineSeparator();
        ByteBuffer bytes = StandardCharsets.UTF_8.encode(metadata);

        channel.truncate(METADATA_POSITION);
        channel.position(METADATA_POSITION);
        while (bytes.hasRemaining()) {
            channel.write(bytes);
        }
        channel.force(true);
    }

    private static void printLockOwner(String ownerMetadata) {
        System.err.println(LOCKED_MARKER);
        System.err.print(ownerMetadata);
        System.err.println("AI_AGENT_ACTION=wait-for-owner-and-retry");
    }

    private static String readOwnerMetadata(FileChannel channel) throws IOException {
        channel.position(METADATA_POSITION);
        ByteBuffer bytes = ByteBuffer.allocate(4096);
        int read = channel.read(bytes);
        if (read <= 0) {
            return "";
        }
        bytes.flip();
        return StandardCharsets.UTF_8.decode(bytes).toString();
    }

    private static boolean isCurrentOwnerMetadata(String metadata, Path appHome) {
        Optional<String> ownerPid = metadata.lines()
                .filter(line -> line.startsWith("ownerPid="))
                .map(line -> line.substring("ownerPid=".length()))
                .findFirst();
        Optional<String> ownerStartedAt = metadata.lines()
                .filter(line -> line.startsWith("ownerStartedAt="))
                .map(line -> line.substring("ownerStartedAt=".length()))
                .findFirst();
        Optional<String> worktree = metadata.lines()
                .filter(line -> line.startsWith("worktree="))
                .map(line -> line.substring("worktree=".length()))
                .findFirst();
        if (ownerPid.isEmpty() || ownerStartedAt.isEmpty()
                || worktree.isEmpty() || !worktree.get().equals(worktreePath(appHome).toString())) {
            return false;
        }

        try {
            long pid = Long.parseLong(ownerPid.get());
            Instant startedAt = Instant.parse(ownerStartedAt.get());
            return ProcessHandle.of(pid)
                    .filter(ProcessHandle::isAlive)
                    .flatMap(process -> process.info().startInstant())
                    .filter(startedAt::equals)
                    .isPresent();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static Path worktreePath(Path appHome) {
        Path parent = appHome.getParent();
        return parent == null ? appHome : parent;
    }

    private static String singleLine(String message) {
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        return message.replace('\r', ' ').replace('\n', ' ');
    }

    private record LockHandle(FileChannel channel, FileLock lock) implements AutoCloseable {

        @Override
        public void close() throws IOException {
            try {
                channel.truncate(METADATA_POSITION);
                channel.force(true);
            } finally {
                try {
                    lock.close();
                } finally {
                    channel.close();
                }
            }
        }
    }
}
