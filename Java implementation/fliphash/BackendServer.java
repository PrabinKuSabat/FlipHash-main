package backend;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

// OSHI imports (ensure OSHI is on your classpath)
import oshi.*;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import oshi.software.os.OperatingSystem;

// Import the Validator class from package fliphash.
import fliphash.Validator;

public class BackendServer {
    // The port on which this backend will accept proxied connections.
    private static final int BACKEND_PORT = 6002;
    // The load balancer’s registration port and host.
    private static final String LB_HOST = "10.110.230.244";
    private static final int LB_REGISTRATION_PORT = 6001;
    // The load balancer's metrics port.
    private static final int LB_METRICS_PORT = 6003;
    private static final int BUFFER_SIZE = 4096;
    // Directory to store the received JAR files.
    private static final String SANDBOX_DIR = "sandbox/";
    // Policy file name.
    private static final String POLICY_FILE = "sandbox.policy";
    
    // Counter for the number of active client connections (for JAR transfers)
    private static final AtomicInteger clientCount = new AtomicInteger(0);
    
    // OSHI system info (if available)
    private static final SystemInfo systemInfo = new SystemInfo();
    private static final HardwareAbstractionLayer hal = systemInfo.getHardware();
    private static final Sensors sensors = hal.getSensors();
    private static final OperatingSystem os = systemInfo.getOperatingSystem();

    public static void main(String[] args) {
        // Ensure the sandbox policy file exists.
        ensurePolicyFileExists();

        // First, register with the load balancer.
        registerWithLoadBalancer();

        // Start metrics reporting thread.
        new Thread(() -> reportMetricsPeriodically()).start();

        // Create the sandbox directory if it doesn't exist.
        File sandbox = new File(SANDBOX_DIR);
        if (!sandbox.exists()) {
            sandbox.mkdir();
        }

        // Now listen for proxied connections from the load balancer.
        try (ServerSocket serverSocket = new ServerSocket(BACKEND_PORT)) {
            System.out.println("Backend Server listening on port " + BACKEND_PORT);
            while (true) {
                Socket proxyConn = serverSocket.accept();
                // Increment client count for each new connection.
                clientCount.incrementAndGet();
                new Thread(() -> {
                    try {
                        handleProxyConnection(proxyConn);
                    } finally {
                        // Decrement when connection is closed.
                        clientCount.decrementAndGet();
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("Backend Server error: " + e.getMessage());
        }
    }

    // Creates a default sandbox.policy file if it doesn't exist.
    private static void ensurePolicyFileExists() {
        File policyFile = new File(POLICY_FILE);
        if (!policyFile.exists()) {
            try (FileWriter writer = new FileWriter(policyFile)) {
                // This default policy grants minimal read permissions.
                writer.write("grant {\n"
                        + "    permission java.util.PropertyPermission \"*\", \"read\";\n"
                        + "    permission java.io.FilePermission \"<<ALL FILES>>\", \"read\";\n"
                        + "};\n");
                System.out.println("Created default policy file: " + POLICY_FILE);
            } catch (IOException e) {
                System.err.println("Could not create policy file: " + e.getMessage());
            }
        }
    }

    // Registers this backend server with the load balancer.
    private static void registerWithLoadBalancer() {
        try (Socket lbSocket = new Socket(LB_HOST, LB_REGISTRATION_PORT);
             PrintWriter writer = new PrintWriter(lbSocket.getOutputStream(), true)) {
            // Send our IP and BACKEND_PORT in the format "host:port".
            String registrationInfo = InetAddress.getLocalHost().getHostAddress() + ":" + BACKEND_PORT;
            writer.println(registrationInfo);
            System.out.println("Registered with Load Balancer: " + registrationInfo);
        } catch (IOException e) {
            System.err.println("Registration failed: " + e.getMessage());
        }
    }

    // Periodically gathers performance metrics and sends them to the load balancer.
    private static void reportMetricsPeriodically() {
        // Initialize previous ticks.
        long[] prevTicks = hal.getProcessor().getSystemCpuLoadTicks();
        while (true) {
            try {
                // Sleep for 10 seconds between reports.
                Thread.sleep(10000);
                
                // Get current ticks.
                long[] ticks = hal.getProcessor().getSystemCpuLoadTicks();
                double cpuLoad = hal.getProcessor().getSystemCpuLoadBetweenTicks(prevTicks) * 100;
                // Update prevTicks for the next interval.
                prevTicks = ticks;
                
                double cpuTemp = sensors.getCpuTemperature();
                long totalMemory = hal.getMemory().getTotal();
                long availableMemory = hal.getMemory().getAvailable();
                double memoryUsage = 100.0 * (totalMemory - availableMemory) / totalMemory;
                int clients = clientCount.get();
                
                // Construct JSON metrics string.
                String backendId = InetAddress.getLocalHost().getHostAddress() + ":" + BACKEND_PORT;
                String metricsJson = String.format(
                    "{\"backendId\":\"%s\", \"cpuLoad\":%.2f, \"cpuTemp\":%.2f, \"memoryUsage\":%.2f, \"clientCount\":%d}",
                    backendId, cpuLoad, cpuTemp, memoryUsage, clients
                );
                
                // Send metrics to load balancer on LB_METRICS_PORT.
                try (Socket metricsSocket = new Socket(LB_HOST, LB_METRICS_PORT);
                    PrintWriter out = new PrintWriter(metricsSocket.getOutputStream(), true)) {
                    out.println(metricsJson);
                } catch (IOException e) {
                    System.err.println("Error sending metrics: " + e.getMessage());
                    if (askRetry("metrics transmission")) {
                        boolean sent = false;
                        while (!sent) {
                            try {
                                Thread.sleep(10000);
                                try (Socket metricsSocket = new Socket(LB_HOST, LB_METRICS_PORT);
                                    PrintWriter out = new PrintWriter(metricsSocket.getOutputStream(), true)) {
                                    out.println(metricsJson);
                                    sent = true;
                                }
                            } catch (IOException | InterruptedException ex) {
                                System.err.println("Retrying metrics transmission failed: " + ex.getMessage());
                            }
                        }
                    } else {
                        System.out.println("Exiting metrics transmission as per user request.");
                        break;
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException ioe) {
                System.err.println("Error getting backend ID: " + ioe.getMessage());
            }
        }
    }

    // Updated askRetry method using Validator for user input.
    private static boolean askRetry(String action) {
        while (true) {
            String response = Validator.getString("Failed " + action + ". Retry? (y/n): ").trim().toLowerCase();
            if (response.equals("y")) {
                return true;
            } else if (response.equals("n")) {
                return false;
            }
            System.out.println("Invalid input. Please enter 'y' or 'n'.");
        }
    }

    // Handle a proxied connection from the load balancer.
    private static void handleProxyConnection(Socket proxyConn) {
        try (DataInputStream dis = new DataInputStream(proxyConn.getInputStream());
            DataOutputStream dos = new DataOutputStream(proxyConn.getOutputStream())) {
            
            // Read the first UTF string.
            String initialMessage = dis.readUTF();
            // If this is a health check, simply return without processing further.
            if ("helath check".equals(initialMessage)) {
                return;
            }
            
            // Otherwise, process as a JAR file transfer.
            String fileName = initialMessage;
            long fileSize = dis.readLong(); // Expect the file size.

            File jarFile = new File(SANDBOX_DIR + fileName);
            try (FileOutputStream fos = new FileOutputStream(jarFile)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int)Math.min(BUFFER_SIZE, fileSize))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    fileSize -= bytesRead;
                }
            }
            System.out.println("Received and saved JAR: " + jarFile.getAbsolutePath());
            
            // Execute the JAR in a sandbox and capture its output.
            String jarOutput = runJarInSandbox(jarFile);
            System.out.println("Execution output: " + jarOutput);

            // Send the captured jar output back to the client.
            dos.writeUTF(jarOutput);
            dos.flush();
            
        } catch (IOException e) {
            String errorMsg = (e.getMessage() != null) ? e.getMessage() : "Connection closed (no error message)";
            System.err.println("Error handling proxied connection: " + errorMsg);
        } finally {
            try {
                proxyConn.close();
            } catch (IOException e) {
                // Ignore closing errors.
            }
        }
    }

    // Runs the given JAR file in a sandboxed environment (using the custom policy file)
    // and returns its output. Lines starting with "WARNING:" are filtered out.
    private static String runJarInSandbox(File jarFile) {
        StringBuilder outputBuilder = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-Djava.security.manager",
                "-Djava.security.policy=" + POLICY_FILE,
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
