import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Standalone verification for {@link WorktreeGradleLock}.
 *
 * <p>Run from the backend directory with {@code java gradle/WorktreeGradleLockVerification.java}.
 * It uses a temporary fake wrapper so it never starts a nested Gradle build.</p>
 */
public final class WorktreeGradleLockVerification {

    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(30);
    private static final String LOCKED_MARKER = "[YEOBAEK_GRADLE_LOCKED]";
    private static final String LOCK_ERROR_MARKER = "[YEOBAEK_GRADLE_LOCK_ERROR]";
    private static final String AI_ACTION = "AI_AGENT_ACTION=wait-for-owner-and-retry";

    private WorktreeGradleLockVerification() {
    }

    public static void main(String[] args) throws Exception {
        Path launcher = Path.of("gradle", "WorktreeGradleLock.java").toAbsolutePath().normalize();
        Path temporaryDirectory = Files.createTempDirectory("yeobaek-gradle-lock-verification-");
        Process owner = null;
        try {
            Path fakeWrapper = createFakeWrapper(temporaryDirectory);
            Path compiledLauncher = compileLauncher(launcher, fakeWrapper, temporaryDirectory);
            Path firstAppHome = Files.createDirectories(temporaryDirectory.resolve("worktree-a/backend"));
            Path secondAppHome = Files.createDirectories(temporaryDirectory.resolve("worktree-b/backend"));

            Path ownerReady = temporaryDirectory.resolve("owner.ready");
            Path ownerRelease = temporaryDirectory.resolve("owner.release");
            owner = startLauncher(launcher, fakeWrapper, firstAppHome, ownerReady, ownerRelease, "SECRET_ARGUMENT");
            awaitFile(ownerReady, owner, "The lock owner did not start.");

            ProcessResult collision = runLauncher(
                    launcher,
                    fakeWrapper,
                    firstAppHome,
                    temporaryDirectory.resolve("collision.ready"),
                    temporaryDirectory.resolve("collision.release")
            );
            require(collision.exitCode() == 75, "A same-worktree collision must exit with code 75.");
            require(collision.standardError().contains(LOCKED_MARKER), "The collision marker is missing.");
            require(collision.standardError().contains("ownerPid=" + owner.pid()), "The owner PID is missing.");
            require(collision.standardError().contains("ownerStartedAt="), "The owner start time is missing.");
            require(collision.standardError().contains("worktree=" + firstAppHome.getParent()), "The worktree is missing.");
            require(collision.standardError().contains(AI_ACTION), "The AI retry action is missing.");
            String metadata = readMetadata(firstAppHome.resolve(".gradle/yeobaek-worktree-gradle.lock"));
            require(!metadata.contains("SECRET_ARGUMENT"), "Gradle arguments must not be stored in lock metadata.");

            Path otherReady = temporaryDirectory.resolve("other.ready");
            Path otherRelease = Files.createFile(temporaryDirectory.resolve("other.release"));
            ProcessResult otherWorktree = runLauncher(
                    launcher,
                    fakeWrapper,
                    secondAppHome,
                    otherReady,
                    otherRelease
            );
            require(otherWorktree.exitCode() == 0, "A different worktree must run concurrently.");
            require(Files.exists(otherReady), "The different-worktree wrapper did not run.");

            Files.createFile(ownerRelease);
            awaitProcess(owner, "The lock owner did not finish.");
            require(owner.exitValue() == 0, "The lock owner failed.");
            require(readMetadata(firstAppHome.resolve(".gradle/yeobaek-worktree-gradle.lock")).isEmpty(),
                    "Owner metadata must be cleared before releasing the lock.");

            Path retryReady = temporaryDirectory.resolve("retry.ready");
            Path retryRelease = Files.createFile(temporaryDirectory.resolve("retry.release"));
            ProcessResult retry = runLauncher(
                    launcher,
                    fakeWrapper,
                    firstAppHome,
                    retryReady,
                    retryRelease
            );
            require(retry.exitCode() == 0, "The same command must succeed after the owner exits.");
            require(Files.exists(retryReady), "The retried wrapper did not run.");

            verifyInvalidOwnerMetadata(launcher, fakeWrapper, temporaryDirectory);
            verifyLockHandoff(compiledLauncher, fakeWrapper, temporaryDirectory);
            verifyRepositoryWrapperExitCode(launcher, fakeWrapper, temporaryDirectory);

            System.out.println("WorktreeGradleLock verification passed.");
        } finally {
            if (owner != null && owner.isAlive()) {
                owner.destroyForcibly();
                owner.waitFor(PROCESS_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            }
            deleteRecursively(temporaryDirectory);
        }
    }

    private static Path createFakeWrapper(Path temporaryDirectory) throws IOException {
        Path sourceDirectory = Files.createDirectories(temporaryDirectory.resolve("fake-source/org/gradle/wrapper"));
        Path classesDirectory = Files.createDirectories(temporaryDirectory.resolve("fake-classes"));
        Path source = sourceDirectory.resolve("GradleWrapperMain.java");
        Files.writeString(source, """
                package org.gradle.wrapper;

                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.util.concurrent.locks.LockSupport;

                public final class GradleWrapperMain {
                    private GradleWrapperMain() {
                    }

                    public static void main(String[] args) {
                        Path ready = Path.of(args[0]);
                        Path release = Path.of(args[1]);
                        try {
                            Files.writeString(ready, "ready");
                            while (!Files.exists(release)) {
                                LockSupport.parkNanos(10_000_000L);
                                if (Thread.interrupted()) {
                                    throw new InterruptedException("interrupted");
                                }
                            }
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    }
                }
                """);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        require(compiler != null, "A JDK is required to run this verification.");
        int compilationResult = compiler.run(null, null, null, "-d", classesDirectory.toString(), source.toString());
        require(compilationResult == 0, "The fake wrapper did not compile.");

        Path jar = temporaryDirectory.resolve("fake-gradle-wrapper.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar, StandardOpenOption.CREATE_NEW))) {
            Path classFile = classesDirectory.resolve("org/gradle/wrapper/GradleWrapperMain.class");
            output.putNextEntry(new JarEntry("org/gradle/wrapper/GradleWrapperMain.class"));
            Files.copy(classFile, output);
            output.closeEntry();
        }
        return jar;
    }

    private static Path compileLauncher(Path launcher, Path fakeWrapper, Path temporaryDirectory) throws IOException {
        Path classesDirectory = Files.createDirectories(temporaryDirectory.resolve("launcher-classes"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        require(compiler != null, "A JDK is required to run this verification.");
        int compilationResult = compiler.run(
                null,
                null,
                null,
                "-classpath",
                fakeWrapper.toString(),
                "-d",
                classesDirectory.toString(),
                launcher.toString()
        );
        require(compilationResult == 0, "The lock launcher did not compile.");
        return classesDirectory;
    }

    private static Process startLauncher(
            Path launcher,
            Path wrapperJar,
            Path appHome,
            Path ready,
            Path release,
            String... additionalArguments
    ) throws IOException {
        List<String> command = new java.util.ArrayList<>(List.of(
                javaExecutable().toString(),
                "-Dyeobaek.gradle.app-home=" + appHome,
                "--class-path",
                wrapperJar.toString(),
                launcher.toString(),
                ready.toString(),
                release.toString()
        ));
        command.addAll(List.of(additionalArguments));
        String outputSuffix = Long.toUnsignedString(System.nanoTime());
        return new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(
                        ready.getParent().resolve("launcher-" + outputSuffix + ".out").toFile()))
                .redirectError(ProcessBuilder.Redirect.appendTo(
                        ready.getParent().resolve("launcher-" + outputSuffix + ".err").toFile()))
                .start();
    }

    private static ProcessResult runLauncher(
            Path launcher,
            Path wrapperJar,
            Path appHome,
            Path ready,
            Path release
    ) throws Exception {
        Path standardOutput = appHome.resolve("run-" + System.nanoTime() + ".out");
        Path standardError = appHome.resolve("run-" + System.nanoTime() + ".err");
        Process process = new ProcessBuilder(
                javaExecutable().toString(),
                "-Dyeobaek.gradle.app-home=" + appHome,
                "--class-path",
                wrapperJar.toString(),
                launcher.toString(),
                ready.toString(),
                release.toString()
        )
                .redirectOutput(standardOutput.toFile())
                .redirectError(standardError.toFile())
                .start();
        awaitProcess(process, "A launcher process timed out.");
        return new ProcessResult(process.exitValue(), Files.readString(standardOutput), Files.readString(standardError));
    }

    private static void verifyRepositoryWrapperExitCode(
            Path launcher,
            Path fakeWrapper,
            Path temporaryDirectory
    ) throws Exception {
        Path appHome = launcher.getParent().getParent();
        Path ownerReady = temporaryDirectory.resolve("repository-owner.ready");
        Path ownerRelease = temporaryDirectory.resolve("repository-owner.release");
        Process owner = startLauncher(launcher, fakeWrapper, appHome, ownerReady, ownerRelease);
        try {
            awaitFile(ownerReady, owner, "The repository wrapper lock owner did not start.");
            ProcessResult collision = runRepositoryWrapper(appHome, temporaryDirectory);
            require(collision.exitCode() == 75, "The repository wrapper must preserve exit code 75.");
            require(collision.standardError().contains(LOCKED_MARKER), "The repository wrapper lost the marker.");

            ProcessResult localEnvironment = runLocalEnvironmentDev(appHome, temporaryDirectory);
            require(localEnvironment.exitCode() == 75,
                    "local-env dev must preserve exit code 75, but was " + localEnvironment.exitCode()
                            + ": " + localEnvironment.standardError());
            require(localEnvironment.standardError().contains(LOCKED_MARKER),
                    "local-env dev lost the collision marker.");
            String dockerCommands = Files.readString(temporaryDirectory.resolve("fake-docker.log"));
            require(dockerCommands.lines().noneMatch(line -> line.contains(" down")),
                    "A colliding local-env dev must not clean up the owner's Compose project.");
        } finally {
            Files.createFile(ownerRelease);
            awaitProcess(owner, "The repository wrapper lock owner did not finish.");
        }
        require(owner.exitValue() == 0, "The repository wrapper lock owner failed.");
    }

    private static void verifyInvalidOwnerMetadata(
            Path launcher,
            Path fakeWrapper,
            Path temporaryDirectory
    ) throws Exception {
        Path appHome = Files.createDirectories(temporaryDirectory.resolve("worktree-invalid/backend"));
        Path lockDirectory = Files.createDirectories(appHome.resolve(".gradle"));
        Path lockPath = lockDirectory.resolve("yeobaek-worktree-gradle.lock");
        try (FileChannel channel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
        ); FileLock ignored = channel.lock(0L, 1L, false)) {
            channel.position(1L);
            channel.write(StandardCharsets.UTF_8.encode("ownerPid=9223372036854775807\nownerStartedAt=partial"));
            channel.force(true);

            ProcessResult collision = runLauncher(
                    launcher,
                    fakeWrapper,
                    appHome,
                    temporaryDirectory.resolve("invalid.ready"),
                    temporaryDirectory.resolve("invalid.release")
            );
            require(collision.exitCode() == 1, "Invalid owner metadata must fail as a harness error.");
            require(collision.standardError().contains(LOCK_ERROR_MARKER),
                    "Invalid owner metadata must print the harness error marker.");
            require(collision.standardError().contains("owner-metadata-unavailable"),
                    "Invalid owner metadata must identify its reason.");
            require(!collision.standardError().contains(LOCKED_MARKER),
                    "Invalid owner metadata must not masquerade as a normal collision.");
        }
    }

    private static void verifyLockHandoff(
            Path compiledLauncher,
            Path fakeWrapper,
            Path temporaryDirectory
    ) throws Exception {
        Path appHome = Files.createDirectories(temporaryDirectory.resolve("worktree-handoff/backend"));
        Path lockDirectory = Files.createDirectories(appHome.resolve(".gradle"));
        Path lockPath = lockDirectory.resolve("yeobaek-worktree-gradle.lock");
        Path ready = temporaryDirectory.resolve("handoff.ready");
        Path release = Files.createFile(temporaryDirectory.resolve("handoff.release"));
        Path standardOutput = temporaryDirectory.resolve("handoff.out");
        Path standardError = temporaryDirectory.resolve("handoff.err");
        Process contender = null;

        try (FileChannel channel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
        )) {
            FileLock ownerLock = channel.lock(0L, 1L, false);
            try {
                channel.position(1L);
                channel.write(StandardCharsets.UTF_8.encode("ownerPid=stale"));
                channel.force(true);
                contender = startCompiledLauncher(
                        compiledLauncher,
                        fakeWrapper,
                        appHome,
                        ready,
                        release,
                        standardOutput,
                        standardError
                );
                Thread.sleep(500L);
                require(contender.isAlive() && !Files.exists(ready),
                        "The handoff contender must still be waiting for the lock.");
            } finally {
                ownerLock.close();
            }

            awaitProcess(contender, "The handoff contender did not finish.");
            require(contender.exitValue() == 0, "The handoff contender must acquire the released lock.");
            require(Files.exists(ready), "The handoff contender did not run the wrapper.");
        } finally {
            if (contender != null && contender.isAlive()) {
                contender.destroyForcibly();
                contender.waitFor(5L, TimeUnit.SECONDS);
            }
        }
    }

    private static Process startCompiledLauncher(
            Path compiledLauncher,
            Path fakeWrapper,
            Path appHome,
            Path ready,
            Path release,
            Path standardOutput,
            Path standardError
    ) throws IOException {
        String classpath = compiledLauncher + System.getProperty("path.separator") + fakeWrapper;
        return new ProcessBuilder(
                javaExecutable().toString(),
                "-Dyeobaek.gradle.app-home=" + appHome,
                "-classpath",
                classpath,
                "WorktreeGradleLock",
                ready.toString(),
                release.toString()
        )
                .redirectOutput(standardOutput.toFile())
                .redirectError(standardError.toFile())
                .start();
    }

    private static ProcessResult runRepositoryWrapper(Path appHome, Path temporaryDirectory) throws Exception {
        List<String> command;
        if (System.getProperty("os.name").startsWith("Windows")) {
            command = List.of(
                    System.getenv().getOrDefault("COMSPEC", "cmd.exe"),
                    "/d",
                    "/c",
                    appHome.resolve("gradlew.bat").toString(),
                    "help",
                    "--no-daemon",
                    "--console=plain"
            );
        } else {
            command = List.of(
                    appHome.resolve("gradlew").toString(),
                    "help",
                    "--no-daemon",
                    "--console=plain"
            );
        }

        Path standardOutput = temporaryDirectory.resolve("repository-wrapper.out");
        Path standardError = temporaryDirectory.resolve("repository-wrapper.err");
        Process process = new ProcessBuilder(command)
                .directory(appHome.toFile())
                .redirectOutput(standardOutput.toFile())
                .redirectError(standardError.toFile())
                .start();
        awaitProcess(process, "The repository wrapper collision timed out.");
        return new ProcessResult(process.exitValue(), Files.readString(standardOutput), Files.readString(standardError));
    }

    private static ProcessResult runLocalEnvironmentDev(Path appHome, Path temporaryDirectory) throws Exception {
        Path fakeBinaryDirectory = Files.createDirectories(temporaryDirectory.resolve("fake-bin"));
        Path dockerLog = temporaryDirectory.resolve("fake-docker.log");
        Files.deleteIfExists(dockerLog);

        List<String> command;
        if (System.getProperty("os.name").startsWith("Windows")) {
            Path fakeDocker = fakeBinaryDirectory.resolve("docker.cmd");
            Files.writeString(fakeDocker, "@echo off\r\necho %*>>\"%YEOBAEK_FAKE_DOCKER_LOG%\"\r\nexit /b 0\r\n");
            command = List.of(
                    "pwsh.exe",
                    "-NoLogo",
                    "-NoProfile",
                    "-File",
                    appHome.resolve("local-env.ps1").toString(),
                    "dev"
            );
        } else {
            Path fakeDocker = fakeBinaryDirectory.resolve("docker");
            Files.writeString(fakeDocker, "#!/bin/sh\nprintf '%s\\n' \"$*\" >> \"$YEOBAEK_FAKE_DOCKER_LOG\"\n");
            require(fakeDocker.toFile().setExecutable(true), "The fake Docker command must be executable.");
            command = List.of("sh", appHome.resolve("local-env.sh").toString(), "dev");
        }

        Path standardOutput = temporaryDirectory.resolve("local-env.out");
        Path standardError = temporaryDirectory.resolve("local-env.err");
        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(appHome.toFile())
                .redirectOutput(standardOutput.toFile())
                .redirectError(standardError.toFile());
        String existingPath = processBuilder.environment().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("PATH"))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse("");
        processBuilder.environment().put(
                "PATH",
                fakeBinaryDirectory + System.getProperty("path.separator") + existingPath
        );
        processBuilder.environment().put("YEOBAEK_FAKE_DOCKER_LOG", dockerLog.toString());
        Process process = processBuilder.start();
        awaitProcess(process, "local-env dev timed out.");
        return new ProcessResult(process.exitValue(), readUtf8Lenient(standardOutput), readUtf8Lenient(standardError));
    }

    private static String readUtf8Lenient(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void awaitProcess(Process process, String timeoutMessage) throws InterruptedException {
        if (process.waitFor(PROCESS_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
            return;
        }
        process.toHandle().descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        if (!process.waitFor(5L, TimeUnit.SECONDS)) {
            process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            process.waitFor(5L, TimeUnit.SECONDS);
        }
        throw new IllegalStateException(timeoutMessage);
    }

    private static void awaitFile(Path path, Process process, String failureMessage) throws InterruptedException {
        Instant deadline = Instant.now().plus(PROCESS_TIMEOUT);
        while (!Files.exists(path) && Instant.now().isBefore(deadline)) {
            require(process.isAlive(), failureMessage);
            Thread.sleep(10L);
        }
        require(Files.exists(path), failureMessage);
    }

    private static Path javaExecutable() {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }

    private static String readMetadata(Path lockFile) throws IOException {
        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.READ)) {
            channel.position(1L);
            ByteBuffer bytes = ByteBuffer.allocate(4096);
            channel.read(bytes);
            bytes.flip();
            return StandardCharsets.UTF_8.decode(bytes).toString();
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private record ProcessResult(int exitCode, String standardOutput, String standardError) {
    }
}
