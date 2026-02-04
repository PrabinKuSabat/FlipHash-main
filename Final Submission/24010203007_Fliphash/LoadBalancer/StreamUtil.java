package LoadBalancer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/**
 * Utility class for stream operations.
 */
public class StreamUtil {

    private static final int BUFFER_SIZE = 4096;

    /**
     * Pipes data from an input stream to an output stream.
     *
     * @param in  the input stream.
     * @param out the output stream.
     * @throws IOException if an I/O error occurs.
     */
    public static void pipeStreams(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        try {
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {}
            try {
                out.close();
            } catch (IOException ignored) {}
        }
    }
}
