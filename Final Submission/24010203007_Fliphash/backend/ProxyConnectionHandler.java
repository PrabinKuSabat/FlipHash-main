package backend;

import java.io.*;
import java.net.Socket;

/**
 * Handles incoming proxied connections from the load balancer.
 * <p>
 * A proxied connection is used to transfer a JAR file, which is then executed in a sandbox.
 * </p>
 */
public class ProxyConnectionHandler {

    // Buffer size for data transfer.
    private static final int BUFFER_SIZE = 4096;
    
    /**
     * Processes a proxied connection.
     *
     * @param proxyConn the socket connection from the load balancer
     * @param sandboxDir the directory to store received JAR files
     * @param policyFile the security policy file to use when executing JAR files
     */
    public static void handleProxyConnection(Socket proxyConn, String sandboxDir, String policyFile) {
        try (DataInputStream dis = new DataInputStream(proxyConn.getInputStream());
             DataOutputStream dos = new DataOutputStream(proxyConn.getOutputStream())) {
            
            // Read the initial message.
            String initialMessage = dis.readUTF();
            // If it's a health check, respond and exit quietly.
            if ("health check".equals(initialMessage)) {
                dos.writeUTF("OK");
                dos.flush();
                return;
            }
            
            // Otherwise, treat the message as a JAR file name.
            String fileName = initialMessage;
            long fileSize = dis.readLong();
            
            // Save the received JAR file.
            File jarFile = new File(sandboxDir + fileName);
            try (FileOutputStream fos = new FileOutputStream(jarFile)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int)Math.min(BUFFER_SIZE, fileSize))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    fileSize -= bytesRead;
                }
            }
            System.out.println("Received and saved JAR: " + jarFile.getAbsolutePath());
            
            // Execute the received JAR file.
            String jarOutput = JarExecutor.runJarInSandbox(jarFile, policyFile);
            System.out.println("Execution output: " + jarOutput);
            
            // Send the execution output back to the client.
            dos.writeUTF(jarOutput);
            dos.flush();
        } catch (IOException e) {
            // Check if this is an expected connection closure.
            if (e instanceof EOFException || 
               (e.getMessage() != null && e.getMessage().toLowerCase().contains("closed"))) {
                // Do not output anything for normal connection closures.
            } else {
                String errorMsg = (e.getMessage() != null) ? e.getMessage() : "Connection closed (no error message)";
                System.err.println("Error handling proxied connection: " + errorMsg);
            }
        } finally {
            try {
                proxyConn.close();
            } catch (IOException e) {
                // Ignore errors during socket close.
            }
        }
    }
}
