package fliphash;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class LoadBalancerServer {
    private static final int CLIENT_PORT = 5000;      // Port for client connections.
    private static final int REGISTRATION_PORT = 6001;  // Port for backend server registration.
    private static final int METRICS_PORT = 6003;       // Port for backend metrics.
    private static final int BUFFER_SIZE = 4096;        // Buffer size for streaming.
    // Timeout in milliseconds for checking backend connectivity.
    private static final int CHECK_TIMEOUT = 1000;

    // Use a thread-safe list for backends.
    private static final List<BackendInfo> backendServers = new CopyOnWriteArrayList<>();
    // Map to hold the latest metrics per backend, keyed by backend id.
    private static final ConcurrentHashMap<String, String> backendMetrics = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        // Start the display updater thread.
        TerminalDisplayManager.enterAlternateScreen();

        // Register a shutdown hook to restore the normal screen when the application exits.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            TerminalDisplayManager.exitAlternateScreen();
        }));

        // Start background threads.
        new Thread(() -> {
            while (true) {
                TerminalDisplayManager.updateDisplay(getBackendMetrics());
                try {
                    Thread.sleep(3000); // Update every 3 seconds.
                } catch (InterruptedException e) {
                    TerminalDisplayManager.addLog("Display updater interrupted: " + e.getMessage());
                }
            }
        }).start();

        new Thread(() -> acceptBackendRegistrations()).start();
        new Thread(() -> acceptClientConnections()).start();
        new Thread(() -> acceptBackendMetrics()).start();
        new Thread(() -> checkActiveBackends()).start();
    }
    
    // Expose the backend metrics so TerminalDisplayManager can read them.
    public static Map<String, String> getBackendMetrics() {
        return backendMetrics;
    }
    
    // Accept backend registrations on REGISTRATION_PORT.
    private static void acceptBackendRegistrations() {
        try (ServerSocket regSocket = new ServerSocket(REGISTRATION_PORT)) {
            TerminalDisplayManager.addLog("Load Balancer waiting for backend registration on port " + REGISTRATION_PORT);
            while (true) {
                Socket backendRegSocket = regSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(backendRegSocket.getInputStream()));
                // Expect one line with the backend info in the format "host:port"
                String info = reader.readLine();
                if (info != null && info.contains(":")) {
                    String[] parts = info.split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    BackendInfo backend = new BackendInfo(host, port);
                    backendServers.add(backend);
                    TerminalDisplayManager.addLog("Registered backend: " + backend);
                }
                backendRegSocket.close();
            }
        } catch (IOException e) {
            TerminalDisplayManager.addLog("Backend registration error: " + e.getMessage());
        }
    }
    
    // Accept client connections on CLIENT_PORT.
    private static void acceptClientConnections() {
        try (ServerSocket clientSocket = new ServerSocket(CLIENT_PORT)) {
            TerminalDisplayManager.addLog("Load Balancer listening for clients on port " + CLIENT_PORT);
            while (true) {
                Socket client = clientSocket.accept();
                new Thread(() -> handleClient(client)).start();
            }
        } catch (IOException e) {
            TerminalDisplayManager.addLog("Client listener error: " + e.getMessage());
        }
    }
    
    // Handle each client connection by selecting a backend and piping data.
    private static void handleClient(Socket client) {
        Socket backendSocket = null;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Build a client key (using client's IP address).
            String clientKey = client.getInetAddress().getHostAddress();
            
            // Create a Resource with current number of backend servers.
            FlipHash.Resource resource;
            BackendInfo backend;
            if (backendServers.isEmpty()) {
                // Send error message to the client and close connection.
                PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                writer.println("No backend server available");
                client.close();
                return;
            }
            resource = new FlipHash.Resource(backendServers.size());
            long hash = FlipHash.fliphashGeneral(clientKey, resource);
            int index = (int)(hash % backendServers.size());
            backend = backendServers.get(index);
            
            TerminalDisplayManager.addLog("Forwarding client " + clientKey + " to backend " + backend);
            
            // Attempt to connect to the chosen backend.
            try {
                backendSocket = new Socket(backend.host, backend.port);
            } catch (IOException e) {
                backendServers.remove(backend);
                TerminalDisplayManager.addLog("Backend " + backend + " unreachable. Removed from list.");
                client.close();
                return;
            }
            
            // Send acknowledgment to the client.
            PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
            writer.println("OK");
            
            // Start bidirectional streaming between client and backend.
            Future<?> clientToBackend = executor.submit(() -> {
                try {
                    pipeStreams(client.getInputStream(), backendSocket.getOutputStream());
                } catch (IOException e) {
                    TerminalDisplayManager.addLog("Error piping client to backend: " + e.getMessage());
                }
            });
            Future<?> backendToClient = executor.submit(() -> {
                try {
                    pipeStreams(backendSocket.getInputStream(), client.getOutputStream());
                } catch (IOException e) {
                    TerminalDisplayManager.addLog("Error piping backend to client: " + e.getMessage());
                }
            });
            // Wait for both piping tasks to complete.
            clientToBackend.get();
            backendToClient.get();
            
        } catch (Exception e) {
            TerminalDisplayManager.addLog("Client handling error: " + e.getMessage());
        } finally {
            executor.shutdownNow();
            try {
                if (client != null && !client.isClosed()) {
                    client.close();
                }
                if (backendSocket != null && !backendSocket.isClosed()) {
                    backendSocket.close();
                }
            } catch (IOException ex) {
                // Ignore cleanup exceptions.
            }
        }
    }

    // Periodically accepts performance metrics from backend servers on METRICS_PORT.
    private static void acceptBackendMetrics() {
        try (ServerSocket metricsSocket = new ServerSocket(METRICS_PORT)) {
            TerminalDisplayManager.addLog("Load Balancer listening for backend metrics on port " + METRICS_PORT);
            while (true) {
                Socket socket = metricsSocket.accept();
                new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                        String metricsLine;
                        while ((metricsLine = reader.readLine()) != null) {
                            // Parse backend id using a regex pattern.
                            String backendId = extractBackendId(metricsLine);
                            if (backendId != null) {
                                // Update metrics map.
                                backendMetrics.put(backendId, metricsLine);
                                
                                // Ensure this backend is in the backendServers list.
                                String[] parts = backendId.split(":");
                                if (parts.length == 2) {
                                    String host = parts[0];
                                    int port = Integer.parseInt(parts[1]);
                                    BackendInfo backend = new BackendInfo(host, port);
                                    if (!backendServers.contains(backend)) {
                                        backendServers.add(backend);
                                        TerminalDisplayManager.addLog("Backend re-added via metrics: " + backend);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        TerminalDisplayManager.addLog("Error reading backend metrics: " + e.getMessage());
                    }
                }).start();
            }
        } catch (IOException e) {
            TerminalDisplayManager.addLog("Metrics socket error: " + e.getMessage());
        }
    }
    
    // Checks every 3 seconds which backends are still active.
    private static void checkActiveBackends() {
        while (true) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                TerminalDisplayManager.addLog("Backend health check interrupted: " + e.getMessage());
                break;
            }
            for (BackendInfo backend : backendServers) {
                if (!isBackendActive(backend)) {
                    backendServers.remove(backend);
                    TerminalDisplayManager.addLog("Backend removed (inactive): " + backend);
                }
            }
        }
    }
    
    // Checks if a backend is active by trying to connect with a short timeout.
    private static boolean isBackendActive(BackendInfo backend) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(backend.host, backend.port), CHECK_TIMEOUT);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF("health check");
            dos.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Helper to extract the backendId from a JSON string using regex.
    private static String extractBackendId(String json) {
        Pattern pattern = Pattern.compile("\"backendId\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    // Pipe data from one stream to another.
    private static void pipeStreams(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        try {
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } finally {
            try { in.close(); } catch (IOException ignored) { }
            try { out.close(); } catch (IOException ignored) { }
        }
    }
    
    // Helper class to store backend server information.
    static class BackendInfo {
        String host;
        int port;
        public BackendInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            BackendInfo that = (BackendInfo) obj;
            return port == that.port && Objects.equals(host, that.host);
        }
        @Override
        public int hashCode() {
            return Objects.hash(host, port);
        }
        public String toString() {
            return host + ":" + port;
        }
    }
}
