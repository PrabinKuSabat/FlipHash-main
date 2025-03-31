package client;

import java.io.*;
import java.net.*;

public class FlipHashClient {
    private static final String LOAD_BALANCER_HOST = "10.110.230.244";
    private static final int LOAD_BALANCER_PORT = 5000;
    private static final int BUFFER_SIZE = 4096;
    // Maximum number of connection attempts.
    private static final int MAX_RETRIES = 3;
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java FlipHashClient <jar_file_path>");
            return;
        }
        
        File jarFile = new File(args[0]);
        if (!jarFile.exists()) {
            System.err.println("File does not exist: " + jarFile.getAbsolutePath());
            return;
        }
        
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try (Socket socket = new Socket(LOAD_BALANCER_HOST, LOAD_BALANCER_PORT);
                 // Wrap the socket's input stream with InputStreamReader and BufferedReader
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 FileInputStream fis = new FileInputStream(jarFile)) {
                 
                // Wait for server's initial response.
                String initialResponse = in.readLine();
                if (!"OK".equals(initialResponse)) {
                    System.out.println("Server response: " + initialResponse);
                    return;
                }
                
                // Send file name and file size.
                dos.writeUTF(jarFile.getName());
                dos.writeLong(jarFile.length());
                
                // Stream the file contents.
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                dos.flush();
                // Signal end of output.
                socket.shutdownOutput();
                System.out.println("JAR file sent successfully.");
                
                // Read and print additional lines of response from the server.
                String responseLine;
                while ((responseLine = in.readLine()) != null) {
                    System.out.println("Server response: " + responseLine);
                }
                // Successful transfer; break out of retry loop.
                break;
                
            } catch (IOException e) {
                attempts++;
                System.err.println("Attempt " + attempts + " failed: " + e.getMessage());
                if (attempts >= MAX_RETRIES) {
                    System.err.println("Max retries reached. Exiting.");
                } else {
                    // Exponential backoff before retrying.
                    try {
                        Thread.sleep((long) Math.pow(2, attempts) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
}
