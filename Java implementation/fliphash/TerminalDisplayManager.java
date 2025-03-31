package fliphash;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class TerminalDisplayManager {
    // Maximum number of log lines to keep in memory.
    private static final int MAX_LOG_LINES = 100;
    private static final List<String> logBuffer = new ArrayList<>();
    private static final Object lock = new Object();
    private static final String LOG_FILE_NAME = "LoadBalancerOutputLog.txt";

    // Call this at startup to switch to the alternate screen.
    public static void enterAlternateScreen() {
        System.out.print("\033[?1049h");
        System.out.flush();
    }

    // Call this at shutdown to restore the normal screen.
    public static void exitAlternateScreen() {
        System.out.print("\033[?1049l");
        System.out.flush();
    }

    // Adds a log line. This method is thread-safe.
    public static void addLog(String log) {
        synchronized (lock) {
            logBuffer.add(log);
            if (logBuffer.size() >= MAX_LOG_LINES) {
                flushBufferToFile();
            }
        }
    }

    // Flushes the current buffer to a log file and then clears the buffer.
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

    // Parses the JSON string and returns an array with the metrics.
    // Expected keys: backendId, cpuLoad, cpuTemp, memoryUsage, clientCount.
    private static String[] parseMetrics(String json) {
        String backendId = extractValue(json, "backendId");
        String cpuLoad   = extractValue(json, "cpuLoad");
        String cpuTemp   = extractValue(json, "cpuTemp");
        String memUsage  = extractValue(json, "memoryUsage");
        String clientCnt = extractValue(json, "clientCount");
        return new String[]{backendId, cpuLoad, cpuTemp, memUsage, clientCnt};
    }
    
    // Simple helper to extract a value given a key from a JSON-like string.
    private static String extractValue(String json, String key) {
        // Look for the pattern "key": "value" or "key": value
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

    // Clears the screen and prints the live metrics table followed by all log lines.
    public static void updateDisplay(Map<String, String> backendMetrics) {
        synchronized (lock) {
            // Clear the alternate screen buffer (which we've switched into).
            System.out.print("\033[3J\033[H\033[2J");
            System.out.flush();
    
            // Print the metrics table header with separate columns.
            System.out.println("Backend Metrics Table (updated every 3 seconds):");
            System.out.println("+----------------------+--------------+--------------+------------------+--------------+");
            System.out.println("| Backend ID           | CPU Load (%) | CPU Temp (°C)| Memory Usage (%) | Client Count |");
            System.out.println("+----------------------+--------------+--------------+------------------+--------------+");
    
            // Print each metric row.
            for (Map.Entry<String, String> entry : backendMetrics.entrySet()) {
                String json = entry.getValue();
                String[] metrics = parseMetrics(json);
                System.out.printf("| %-20s | %-12s | %-12s | %-16s | %-12s |\n",
                        metrics[0], metrics[1], metrics[2], metrics[3], metrics[4]);
            }
            System.out.println("+----------------------+--------------+--------------+------------------+--------------+");
            System.out.println();
    
            // Print the current in-memory log buffer below the table.
            for (String log : logBuffer) {
                System.out.println(log);
            }
        }
    }
}
