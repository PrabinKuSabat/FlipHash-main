package backend;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

// Import the Validator class if using it for user input.
import fliphash.Validator;

/**
 * Handles backend server registration with the load balancer.
 * <p>
 * The registration is done by sending the backend's IP and port in the format "host:port".
 * </p>
 */
public class RegistrationHandler {

    /**
     * Registers this backend server with the load balancer.
     *
     * @param lbHost             the load balancer host
     * @param lbRegistrationPort the load balancer registration port
     * @param backendPort        the port on which this backend listens
     */
    public static void registerWithLoadBalancer(String lbHost, int lbRegistrationPort, int backendPort) {
        boolean registered = false;
        while (!registered) {
            try (Socket lbSocket = new Socket(lbHost, lbRegistrationPort);
                 PrintWriter writer = new PrintWriter(lbSocket.getOutputStream(), true)) {
                // Send the registration information.
                String registrationInfo = InetAddress.getLocalHost().getHostAddress() + ":" + backendPort;
                writer.println(registrationInfo);
                System.out.println("Registered with Load Balancer: " + registrationInfo);
                registered = true;
            } catch (IOException e) {
                System.err.println("Registration failed: " + e.getMessage());
                String response = Validator.getString("Registration failed. Retry? (y/n): ").trim().toLowerCase();
                if (!response.equals("y")) {
                    System.out.println("Exiting registration.");
                    System.exit(0);
                }
                // If the user responds with "y", the loop will retry the registration.
            }
        }
    }
}
