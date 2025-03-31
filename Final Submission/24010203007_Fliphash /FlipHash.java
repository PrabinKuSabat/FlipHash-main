package fliphash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;
import fliphash.xxh3Java.hashing.Hasher64;
import fliphash.xxh3Java.hashing.XXH3_64;

/**
 * Implements the FlipHash algorithm using a simulated xxHash3-based approach.
 * <p>
 * The fliphash algorithm computes a hash value and maps it to a range determined by the number
 * of resources available. It also includes functionality for reading input packets,
 * maintaining a processing queue, and simple cache file management.
 * </p>
 */
public class FlipHash {

    private static final int VALUE_SIZE = 64;
    private static final int MAX_CACHE_SIZE_FLIPHASH = 64000;
    private static final int KEY_SIZE_FLIPHASH = 64;

    /**
     * A resource wrapper used to denote the total number of resources.
     */
    public static class Resource {
        private final long count;

        /**
         * Constructor for Resource.
         *
         * @param count the total number of resources.
         */
        public Resource(long count) {
            this.count = count;
        }

        /**
         * Returns the resource count.
         *
         * @return the count.
         */
        public long getCount() {
            return count;
        }
    }

    /**
     * Main method for running the FlipHash demonstration.
     *
     * @param args command-line arguments (not used).
     */
    public static void main(String[] args) {
        // Get the total number of resources from the user.
        long count = Validator.getLong("Please enter the total number of resources: ");
        Resource resource = new Resource(count);
        FlipHashQueue queue = new FlipHashQueue();

        // Start threads to read input and process fliphash values.
        Thread inputThread = new Thread(() -> readFixedSizeInputsStd(queue));
        Thread processingThread = new Thread(() -> processFlipHash(queue, resource));

        inputThread.start();
        processingThread.start();
    }

    /**
     * Reads input lines from standard input, limiting each packet to VALUE_SIZE characters,
     * and enqueues them into the provided FlipHashQueue.
     *
     * @param queue the FlipHashQueue to enqueue inputs.
     */
    private static void readFixedSizeInputsStd(FlipHashQueue queue) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String packet = scanner.next();
            if (packet.length() > VALUE_SIZE) {
                packet = packet.substring(0, VALUE_SIZE);
            }
            queue.enqueue(packet);
        }
    }

    /**
     * Continuously dequeues input packets, calculates their FlipHash values, and prints the result.
     *
     * @param queue    the FlipHashQueue to dequeue inputs.
     * @param resource the total resource count.
     */
    private static void processFlipHash(FlipHashQueue queue, Resource resource) {
        while (true) {
            String packet = queue.dequeue();
            if (packet != null) {
                long result = fliphashGeneral(packet, resource);
                System.out.println("Hash value: " + result);
                System.out.println(" Freed");
            }
        }
    }

    /**
     * Computes a seeding value based on two short values.
     *
     * @param a the first short.
     * @param b the second short.
     * @return a combined seed value.
     */
    private static long createSeed(short a, short b) {
        return a + (b << 16);
    }

    /**
     * Computes the fliphash value using a power-of-two range.
     * <p>
     * This method simulates part of the xxHash3 algorithm using the given twoPower value.
     * </p>
     *
     * @param key      the input key.
     * @param twoPower the power to which 2 is raised.
     * @return the computed hash value.
     */
    private static long fliphashPow2(String key, int twoPower) {
        Hasher64 xxh3 = XXH3_64.create(createSeed((short) 0, (short) 0));
        long a = xxh3.hashCharsToLong(key) & ((1L << twoPower) - 1);

        long temp = a;
        int b = 0;
        while (temp > 1) {
            temp >>= 1;
            b++;
        }
        xxh3 = XXH3_64.create(createSeed((short) b, (short) 0));
        long c = xxh3.hashCharsToLong(key) & ((1L << b) - 1);
        return a + c;
    }

    /**
     * Computes the general fliphash value given an input key and a resource.
     * <p>
     * This method maps the hash value to the available resources.
     * </p>
     *
     * @param key      the input key.
     * @param resource the resource wrapper containing the total count.
     * @return the final hash value mapped to the resource range.
     */
    public static long fliphashGeneral(String key, Resource resource) {
        long numResources = resource.getCount();
        int r = 0;
        for (long temp = numResources; temp > 0; temp >>= 1) {
            r++;
        }
        long d = fliphashPow2(key, r);
        if (d < numResources) {
            return d;
        } else {
            long rNegative1 = 1L << (r - 1);
            long e;
            Hasher64 xxh3;
            // Attempt multiple rehashes if the value is not in range.
            for (int i = 0; i < 64; i++) {
                xxh3 = XXH3_64.create(createSeed((short) (r - 1), (short) i));
                e = xxh3.hashCharsToLong(key) & ((1L << r) - 1);

                if (e < rNegative1) {
                    return fliphashPow2(key, r - 1);
                } else if (e < numResources) {
                    return e;
                }
            }
            return fliphashPow2(key, r - 1);
        }
    }

    /**
     * Convenience method to compute an xxHash value without a seed.
     *
     * @param input   the input string.
     * @param keySize the maximum key size.
     * @return the computed hash value.
     */
    private static long xxh3_64bits(String input, int keySize) {
        return xxh3_64bitsWithSeed(input, keySize, 0);
    }

    /**
     * Reads the first line from a cache file and computes a key using xxHash.
     *
     * @param file the cache file.
     * @return the generated key.
     * @throws IOException if the file cannot be read or is empty.
     */
    private static long keyGenerator(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                return xxh3_64bits(line, VALUE_SIZE);
            }
            throw new IOException("Cache file is empty");
        }
    }

    /**
     * Checks whether the file size is at or above the maximum cache size.
     *
     * @param file the file to check.
     * @return true if the file is large; false otherwise.
     */
    private static boolean isFileSizeExceeded(File file) {
        return file.length() >= MAX_CACHE_SIZE_FLIPHASH;
    }

    /**
     * Checks the cache file size and renames the file if it exceeds the limit.
     *
     * @param filename the name of the cache file.
     */
    private static void checkCacheFile(String filename) {
        File file = new File(filename);
        if (isFileSizeExceeded(file)) {
            File newFile = new File(filename + "_copy");
            boolean success = file.renameTo(newFile);
            if (!success) {
                System.err.println("Failed to rename file");
            }
        }
    }

    /**
     * Computes a simple xxHash3-like 64-bit hash with the specified seed.
     * <p>
     * Note: This is a simplified mixing function and not the full xxHash3 algorithm.
     * </p>
     *
     * @param input   the input string.
     * @param keySize the maximum key size.
     * @param seed    the seed value.
     * @return the computed hash value.
     */
    private static long xxh3_64bitsWithSeed(String input, int keySize, int seed) {
        byte[] bytes = input.getBytes();
        long hash = seed;
        int len = Math.min(bytes.length, keySize);
        for (int i = 0; i < len; i++) {
            hash = hash * 31 + (bytes[i] & 0xff);
        }
        return hash;
    }
}
