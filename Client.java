

import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;
    
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            // Receive and print the hash sent by the server.
            String response = in.readLine();
            System.out.println("Server response: " + response);
        } catch (IOException e) {
            System.err.println("Client exception: " + e.getMessage());
        }
    }
}
