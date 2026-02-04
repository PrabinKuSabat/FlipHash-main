package backend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility class for setting up the backend environment.
 * <p>
 * Responsible for creating the sandbox directory and default policy file if they do not exist.
 * </p>
 */
public class EnvironmentSetup {

    /**
     * Creates the sandbox directory if it does not exist.
     *
     * @param sandboxDir the directory to use as a sandbox
     */
    public static void createSandboxDirectory(String sandboxDir) {
        File sandbox = new File(sandboxDir);
        if (!sandbox.exists()) {
            sandbox.mkdir();
            System.out.println("Created sandbox directory: " + sandboxDir);
        }
    }

    /**
     * Creates a default policy file if it doesn't exist.
     *
     * @param policyFileName the name of the policy file
     */
    public static void ensurePolicyFileExists(String policyFileName) {
        File policyFile = new File(policyFileName);
        if (!policyFile.exists()) {
            try (FileWriter writer = new FileWriter(policyFile)) {
                // This default policy grants minimal read permissions.
                writer.write("grant {\n"
                        + "    permission java.util.PropertyPermission \"*\", \"read\";\n"
                        + "    permission java.io.FilePermission \"<<ALL FILES>>\", \"read\";\n"
                        + "};\n");
                System.out.println("Created default policy file: " + policyFileName);
            } catch (IOException e) {
                System.err.println("Could not create policy file: " + e.getMessage());
            }
        }
    }
}
