package LoadBalancer;

import fliphash.TerminalDisplayManager;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the list of backend servers.
 */
public class BackendManager {

    // Thread-safe list to store backend servers.
    private static final List<BackendInfo> backendServers = new CopyOnWriteArrayList<>();

    /**
     * Adds a new backend to the list.
     *
     * @param backend the backend to add.
     */
    public static void addBackend(BackendInfo backend) {
        if (!backendServers.contains(backend)) {
            backendServers.add(backend);
            TerminalDisplayManager.addLog("Registered backend: " + backend);
        }
    }

    /**
     * Removes a backend from the list.
     *
     * @param backend the backend to remove.
     */
    public static void removeBackend(BackendInfo backend) {
        backendServers.remove(backend);
        TerminalDisplayManager.addLog("Backend removed: " + backend);
    }

    /**
     * Returns the current list of registered backends.
     *
     * @return a thread-safe list of BackendInfo.
     */
    public static List<BackendInfo> getBackends() {
        return backendServers;
    }

    /**
     * Checks if a backend is active by trying to open a socket connection.
     *
     * @param backend the backend to check.
     * @return true if the backend is active; false otherwise.
     */
    public static boolean isBackendActive(BackendInfo backend) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(backend.host, backend.port), 1000);
            try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
                dos.writeUTF("Health check");
                dos.flush();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Helper class that holds backend server information.
     */
    public static class BackendInfo {
        public final String host;
        public final int port;

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

        @Override
        public String toString() {
            return host + ":" + port;
        }
    }
}
