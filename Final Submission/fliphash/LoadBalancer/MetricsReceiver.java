package LoadBalancer;

import fliphash.TerminalDisplayManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Receives performance metrics from backend servers.
 */
public class MetricsReceiver implements Runnable {

    private final int metricsPort;
    private static final ConcurrentHashMap<String, String> backendMetrics = new ConcurrentHashMap<>();

    /**
     * Constructor.
     *
     * @param metricsPort the port on which to receive metrics.
     */
    public MetricsReceiver(int metricsPort) {
        this.metricsPort = metricsPort;
    }

    @Override
    public void run() {
        try (ServerSocket metricsSocket = new ServerSocket(metricsPort)) {
            TerminalDisplayManager.addLog("Load Balancer listening for backend metrics on port " + metricsPort);
            while (true) {
                Socket socket = metricsSocket.accept();
                new Thread(() -> processMetrics(socket)).start();
            }
        } catch (IOException e) {
            TerminalDisplayManager.addLog("Metrics socket error: " + e.getMessage());
        }
    }

    /**
     * Processes metrics received from a backend.
     *
     * @param socket the socket connected to the backend.
     */
    private void processMetrics(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String metricsLine;
            while ((metricsLine = reader.readLine()) != null) {
                // Extract backend id from JSON using regex.
                String backendId = extractBackendId(metricsLine);
                if (backendId != null) {
                    backendMetrics.put(backendId, metricsLine);

                    // Ensure the backend is registered.
                    String[] parts = backendId.split(":");
                    if (parts.length == 2) {
                        String host = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        BackendManager.addBackend(new BackendManager.BackendInfo(host, port));
                    }
                }
            }
        } catch (IOException e) {
            TerminalDisplayManager.addLog("Error reading backend metrics: " + e.getMessage());
        }
    }

    /**
     * Returns the current backend metrics.
     *
     * @return a map of backend IDs to metrics JSON strings.
     */
    public static Map<String, String> getBackendMetrics() {
        return backendMetrics;
    }

    /**
     * Extracts the backendId value from a JSON string.
     *
     * @param json the JSON string.
     * @return the backendId if found; null otherwise.
     */
    private String extractBackendId(String json) {
        Pattern pattern = Pattern.compile("\"backendId\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
