package backend;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Executes a received JAR file in a sandboxed environment.
 * <p>
 * The JAR is run using the specified policy file, and any lines starting with "WARNING:" are filtered out.
 * </p>
 */
public class JarExecutor {

    /**
     * Executes the provided JAR file under a sandbox security policy.
     *
     * @param jarFile   the JAR file to execute
     * @param policyFile the policy file to apply during execution
     * @return the execution output from the JAR file, with warnings filtered out
     */
    public static String runJarInSandbox(File jarFile, String policyFile) {
        StringBuilder outputBuilder = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-Djava.security.manager",
                "-Djava.security.policy=" + policyFile,
                "-jar",
                jarFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("WARNING:")) {
                        outputBuilder.append(line).append("\n");
                    }
                }
            }
            
            // Wait for the process to finish (timeout after 60 seconds).
            boolean finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                outputBuilder.append("Execution timed out and was terminated.\n");
            }
        } catch (IOException | InterruptedException e) {
            outputBuilder.append("Error during execution: ").append(e.getMessage());
        }
        return outputBuilder.toString();
    }
}
