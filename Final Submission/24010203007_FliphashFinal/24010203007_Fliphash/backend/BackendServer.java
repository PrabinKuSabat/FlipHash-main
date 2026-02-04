package backend;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main class for the Backend Server.
 * <p>
 * This server registers itself with a load balancer, reports performance metrics,
 * and listens for proxied connections for receiving and executing JAR files.
 * </p>
 */
public class BackendServer {
    // Ports and configuration constants.
    public static final int BACKEND_PORT = 6002;
    public static final String LB_HOST = "10.110.230.244";
    public static final int LB_REGISTRATION_PORT = 6001;
    public static final int LB_METRICS_PORT = 6003;
    public static final String SANDBOX_DIR = "sandbox/";
    public static final String POLICY_FILE = "sandbox.policy";
    
    // Counter for the number of active client connections.
    public static final AtomicInteger clientCount = new AtomicInteger(0);
    
    /**
     * Main entry point.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        // Set up the environment by ensuring the policy file and sandbox directory exist.
        EnvironmentSetup.ensurePolicyFileExists(POLICY_FILE);
        EnvironmentSetup.createSandboxDirectory(SANDBOX_DIR);
        
        // Register this backend with the load balancer.
        RegistrationHandler.registerWithLoadBalancer(LB_HOST, LB_REGISTRATION_PORT, BACKEND_PORT);
        
        // Start a background thread to report performance metrics.
        new Thread(() -> MetricsReporter.reportMetricsPeriodically(LB_HOST, LB_METRICS_PORT, BACKEND_PORT, clientCount)).start();
        
        // Listen for proxied connections from the load balancer.
        try (ServerSocket serverSocket = new ServerSocket(BACKEND_PORT)) {
            System.out.println("Backend Server listening on port " + BACKEND_PORT);
            while (true) {
                Socket proxyConn = serverSocket.accept();
                clientCount.incrementAndGet();
                // Create a new thread to handle each proxied connection.
                new Thread(() -> {
                    try {
                        ProxyConnectionHandler.handleProxyConnection(proxyConn, SANDBOX_DIR, POLICY_FILE);
                    } finally {
                        clientCount.decrementAndGet();
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("Backend Server error: " + e.getMessage());
        }
    }
}
