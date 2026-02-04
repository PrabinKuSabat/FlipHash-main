package LoadBalancer;

import fliphash.TerminalDisplayManager;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Main class for the Load Balancer Server.
 * <p>
 * This server listens for backend registrations, client connections, and performance metrics.
 * It also periodically checks the health of registered backend servers.
 * </p>
 */
public class LoadBalancerServer {

    public static final int CLIENT_PORT = 5000;      // Port for client connections.
    public static final int REGISTRATION_PORT = 6001;  // Port for backend server registration.
    public static final int METRICS_PORT = 6003;       // Port for backend metrics.
    
    /**
     * Main method to start the load balancer.
     *
     * @param args command line arguments (not used).
     */
    public static void main(String[] args) {
        // Start display updater thread.
        new Thread(() -> {
            while (true) {
                Map<String, String> metrics = MetricsReceiver.getBackendMetrics();
                TerminalDisplayManager.updateDisplay(metrics);
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    TerminalDisplayManager.addLog("Display updater interrupted: " + e.getMessage());
                }
            }
        }).start();

        // Add shutdown hook to restore terminal state.
        Runtime.getRuntime().addShutdownHook(new Thread(TerminalDisplayManager::exitAlternateScreen));

        // Enter alternate screen for display.
        TerminalDisplayManager.enterAlternateScreen();

        // Start background threads.
        new Thread(new BackendRegistrationHandler(REGISTRATION_PORT)).start();
        new Thread(new ClientConnectionHandler(CLIENT_PORT)).start();
        new Thread(new MetricsReceiver(METRICS_PORT)).start();
        new Thread(new BackendHealthChecker()).start();
    }
}
