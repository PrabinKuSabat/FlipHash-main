package LoadBalancer;

import fliphash.TerminalDisplayManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Handles registration of backend servers.
 */
public class BackendRegistrationHandler implements Runnable {

    private final int registrationPort;

    /**
     * Constructor.
     *
     * @param registrationPort port to listen for backend registrations.
     */
    public BackendRegistrationHandler(int registrationPort) {
        this.registrationPort = registrationPort;
    }

    @Override
    public void run() {
        try (ServerSocket regSocket = new ServerSocket(registrationPort)) {
            TerminalDisplayManager.addLog("Load Balancer waiting for backend registration on port " + registrationPort);
            while (true) {
                try (Socket backendRegSocket = regSocket.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(backendRegSocket.getInputStream()))) {
                    // Expect one line with the backend info in the format "host:port"
                    String info = reader.readLine();
                    if (info != null && info.contains(":")) {
                        String[] parts = info.split(":");
                        String host = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        BackendManager.addBackend(new BackendManager.BackendInfo(host, port));
                    }
                }
            }
        } catch (IOException e) {
            TerminalDisplayManager.addLog("Backend registration error: " + e.getMessage());
        }
    }
}
