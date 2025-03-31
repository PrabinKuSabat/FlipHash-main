package LoadBalancer;

import fliphash.TerminalDisplayManager;
import fliphash.FlipHash;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Handles client connections by forwarding them to an appropriate backend.
 */
public class ClientConnectionHandler implements Runnable {

    private final int clientPort;

    /**
     * Constructor.
     *
     * @param clientPort the port on which to accept client connections.
     */
    public ClientConnectionHandler(int clientPort) {
        this.clientPort = clientPort;
    }

    @Override
    public void run() {
        try (ServerSocket clientSocket = new ServerSocket(clientPort)) {
            TerminalDisplayManager.addLog("Load Balancer listening for clients on port " + clientPort);
            while (true) {
                Socket client = clientSocket.accept();
                new Thread(() -> handleClient(client)).start();
            }
        } catch (IOException e) {
            TerminalDisplayManager.addLog("Client listener error: " + e.getMessage());
        }
    }

    /**
     * Handles an individual client connection.
     *
     * @param client the client socket.
     */
    private void handleClient(Socket client) {
        Socket backendSocket = null;
        try {
            // Use the client's IP as key for fliphash.
            String clientKey = client.getInetAddress().getHostAddress();
            // Ensure there is at least one backend.
            if (BackendManager.getBackends().isEmpty()) {
                PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                writer.println("No backend server available");
                client.close();
                return;
            }
            // Use FlipHash to select a backend.
            FlipHash.Resource resource = new FlipHash.Resource(BackendManager.getBackends().size());
            long hash = FlipHash.fliphashGeneral(clientKey, resource);
            int index = (int) (hash % BackendManager.getBackends().size());
            BackendManager.BackendInfo backend = BackendManager.getBackends().get(index);

            TerminalDisplayManager.addLog("Forwarding client " + clientKey + " to backend " + backend);

            // Connect to the chosen backend.
            try {
                backendSocket = new Socket(backend.host, backend.port);
            } catch (IOException e) {
                BackendManager.removeBackend(backend);
                TerminalDisplayManager.addLog("Backend " + backend + " unreachable. Removed from list.");
                client.close();
                return;
            }

            // Acknowledge client.
            PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
            writer.println("OK");

            // Phase 1: Pipe client's request (JAR file upload) to backend.
            pipeStreams(client.getInputStream(), backendSocket.getOutputStream());
            // Signal backend that request transmission is complete.
            backendSocket.shutdownOutput();

            // Phase 2: Pipe backend's response (execution output) back to client.
            pipeStreams(backendSocket.getInputStream(), client.getOutputStream());

        } catch (Exception e) {
            TerminalDisplayManager.addLog("Client handling error: " + e.getMessage());
        } finally {
            try {
                if (!client.isClosed()) {
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

    /**
     * Pipes data from the input stream to the output stream until EOF.
     *
     * @param in  the input stream
     * @param out the output stream
     * @throws IOException if an I/O error occurs
     */
    private void pipeStreams(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
    }
}
