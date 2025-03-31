package backend;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import oshi.software.os.OperatingSystem;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// Import the Validator class if using it for user input.
import fliphash.Validator;

/**
 * Gathers and sends performance metrics of the backend server.
 * <p>
 * Metrics include CPU load, CPU temperature, memory usage, and active client count.
 * </p>
 */
public class MetricsReporter {
    
    // OSHI objects for system information.
    private static final SystemInfo systemInfo = new SystemInfo();
    private static final HardwareAbstractionLayer hal = systemInfo.getHardware();
    private static final Sensors sensors = hal.getSensors();
    private static final OperatingSystem os = systemInfo.getOperatingSystem();
    
    /**
     * Periodically gathers system metrics and sends them to the load balancer.
     *
     * @param lbHost        the load balancer host
     * @param lbMetricsPort the load balancer metrics port
     * @param backendPort   the backend server port
     * @param clientCount   current active client count
     */
    public static void reportMetricsPeriodically(String lbHost, int lbMetricsPort, int backendPort, AtomicInteger clientCount) {
        // Capture the initial CPU ticks.
        long[] prevTicks = hal.getProcessor().getSystemCpuLoadTicks();
        while (true) {
            try {
                // Sleep for 10 seconds between reports.
                Thread.sleep(10000);
                
                // Gather system metrics.
                long[] ticks = hal.getProcessor().getSystemCpuLoadTicks();
                double cpuLoad = hal.getProcessor().getSystemCpuLoadBetweenTicks(prevTicks) * 100;
                prevTicks = ticks;
                
                double cpuTemp = sensors.getCpuTemperature();
                long totalMemory = hal.getMemory().getTotal();
                long availableMemory = hal.getMemory().getAvailable();
                double memoryUsage = 100.0 * (totalMemory - availableMemory) / totalMemory;
                int clients = clientCount.get();
                
                // Build the JSON string of metrics.
                String backendId = java.net.InetAddress.getLocalHost().getHostAddress() + ":" + backendPort;
                String metricsJson = String.format(
                    "{\"backendId\":\"%s\", \"cpuLoad\":%.2f, \"cpuTemp\":%.2f, \"memoryUsage\":%.2f, \"clientCount\":%d}",
                    backendId, cpuLoad, cpuTemp, memoryUsage, clients
                );
                
                // Send metrics to the load balancer.
                try (Socket metricsSocket = new Socket(lbHost, lbMetricsPort);
                     PrintWriter out = new PrintWriter(metricsSocket.getOutputStream(), true)) {
                    out.println(metricsJson);
                } catch (IOException e) {
                    System.err.println("Error sending metrics: " + e.getMessage());
                    if (askRetry("metrics transmission")) {
                        boolean sent = false;
                        while (!sent) {
                            try {
                                TimeUnit.SECONDS.sleep(10);
                                try (Socket metricsSocket = new Socket(lbHost, lbMetricsPort);
                                     PrintWriter out = new PrintWriter(metricsSocket.getOutputStream(), true)) {
                                    out.println(metricsJson);
                                    sent = true;
                                    System.out.println("Reconnected and metrics transmitted successfully.");
                                }
                            } catch (IOException | InterruptedException ex) {
                                System.err.println("Retrying metrics transmission failed: " + ex.getMessage());
                            }
                        }
                    } else {
                        System.out.println("Exiting metrics transmission as per user request.");
                        System.exit(0);
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
    
    /**
     * Prompts the user for retrying a failed action.
     *
     * @param action the name of the action that failed
     * @return true if the user wishes to retry; false otherwise
     */
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
}
