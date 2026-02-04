package fliphash;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * TerminalDisplayManager handles logging and live terminal display updates for the Load Balancer.
 * <p>
 * It supports switching to an alternate screen buffer, buffering log messages,
 * flushing logs to a file when the buffer fills up, and updating a live display
 * with backend metrics formatted as a table.
 * </p>
 */
public class TerminalDisplayManager {
    /** Maximum number of log lines to keep in memory before flushing to file. */
    private static final int MAX_LOG_LINES = 10;
    
    /** In-memory buffer to store log messages. */
    private static final List<String> logBuffer = new ArrayList<>();
    
    /** Lock object for thread-safe log buffer operations. */
    private static final Object lock = new Object();
    
    /** Name of the log file to which logs are flushed. */
    private static final String LOG_FILE_NAME = "LoadBalancerOutputLog.txt";

    /**
     * Switches to the alternate screen buffer.
     */
    public static void enterAlternateScreen() {
        System.out.print("\033[?1049h");
        System.out.flush();
    }

    /**
     * Restores the normal screen from the alternate screen buffer.
     */
    public static void exitAlternateScreen() {
        System.out.print("\033[?1049l");
        System.out.flush();
    }

    /**
     * Adds a log message to the in-memory log buffer.
     * <p>
     * When the buffer reaches the maximum number of lines,
     * it is flushed to the log file.
     * </p>
     *
     * @param log the log message to add.
     */
    public static void addLog(String log) {
        synchronized (lock) {
            logBuffer.add(log);
            if (logBuffer.size() >= MAX_LOG_LINES) {
                flushBufferToFile();
            }
        }
    }

    /**
     * Flushes the current log buffer to a log file and clears the buffer.
     */
    private static void flushBufferToFile() {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE_NAME, true))) {
            for (String log : logBuffer) {
                out.println(log);
            }
        } catch (IOException e) {
            System.err.println("Error writing log file: " + e.getMessage());
        }
        logBuffer.clear();
    }

    /**
     * Parses a JSON-like string containing backend metrics and returns an array of values.
     * <p>
     * Expected keys: backendId, cpuLoad, cpuTemp, memoryUsage, clientCount.
     * </p>
     *
     * @param json the JSON-like string.
     * @return an array of metric values.
     */
    private static String[] parseMetrics(String json) {
        String backendId = extractValue(json, "backendId");
        String cpuLoad   = extractValue(json, "cpuLoad");
        String cpuTemp   = extractValue(json, "cpuTemp");
        String memUsage  = extractValue(json, "memoryUsage");
        String clientCnt = extractValue(json, "clientCount");
        return new String[]{backendId, cpuLoad, cpuTemp, memUsage, clientCnt};
    }
    
    /**
     * Extracts the value associated with a given key from a JSON-like string.
     *
     * @param json the JSON-like string.
     * @param key  the key whose value is to be extracted.
     * @return the extracted value or an empty string if the key is not found.
     */
    private static String extractValue(String json, String key) {
        // Look for the pattern "key": "value" or "key": value.
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return "";
        int start = idx + pattern.length();
        // Skip any whitespace and quotes.
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\"')) {
            start++;
        }
        int end = start;
        // If the value is quoted, read until the next quote.
        if (json.charAt(start - 1) == '\"') {
            end = json.indexOf("\"", start);
            if (end == -1) end = json.length();
        } else {
            // Otherwise, read until a comma or closing brace.
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
        }
        return json.substring(start, end).replaceAll("\"", "").trim();
    }

    /**
     * Clears the terminal screen and updates the display with a live metrics table
     * followed by the current log messages.
     *
     * @param backendMetrics a map of backend IDs to JSON-like metrics strings.
     */
    public static void updateDisplay(Map<String, String> backendMetrics) {
        synchronized (lock) {
            // Clear the alternate screen.
            System.out.print("\033[3J\033[H\033[2J");
            System.out.flush();
    
            // Print the metrics table header.
            System.out.println("Backend Metrics Table (updated every 3 seconds):");
            System.out.println("+----------------------+--------------+--------------+------------------+--------------+");
            System.out.println("| Backend ID           | CPU Load (%) | CPU Temp (°C)| Memory Usage (%) | Client Count |");
            System.out.println("+----------------------+--------------+--------------+------------------+--------------+");
    
            // Print each backend's metrics.
            for (Map.Entry<String, String> entry : backendMetrics.entrySet()) {
                String json = entry.getValue();
                String[] metrics = parseMetrics(json);
                System.out.printf("| %-20s | %-12s | %-12s | %-16s | %-12s |\n",
                        metrics[0], metrics[1], metrics[2], metrics[3], metrics[4]);
            }
            System.out.println("+----------------------+--------------+--------------+------------------+--------------+");
            System.out.println();
    
            // Print the in-memory log buffer.
            for (String log : logBuffer) {
                System.out.println(log);
            }
        }
    }
}
