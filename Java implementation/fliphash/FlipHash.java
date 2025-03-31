package fliphash;

import java.io.*;
import java.util.Scanner;
import fliphash.xxh3Java.hashing.*;

public class FlipHash {

    private static final int VALUE_SIZE = 64;
    private static final int MAX_CACHE_SIZE_FLIPHASH = 64000;
    private static final int KEY_SIZE_FLIPHASH = 64;
    //private static final String CACHE_FILE = "cachefile.txt";

    static class Resource {
        long count;

        public Resource(long count) {
            this.count = count;
        }
    }

    public static void main(String[] args) {
        long count = Validator.getLong("Please enter the total number of resources:");

        Resource resource = new Resource(count);
        FlipHashQueue queue = new FlipHashQueue();

        Thread inputThread = new Thread(() -> readFixedSizeInputsStd(queue));
        Thread processingThread = new Thread(() -> executeFliphash(queue, resource));

        inputThread.start();
        processingThread.start();
    }

    // Reads input lines from standard input (limiting to VALUE_SIZE characters) and enqueues them.
    private static void readFixedSizeInputsStd(FlipHashQueue queue) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String packet = scanner.next();
            // If the input is longer than VALUE_SIZE, take only the first VALUE_SIZE characters.
            if (packet.length() > VALUE_SIZE) {
                packet = packet.substring(0, VALUE_SIZE);
            }
            queue.enqueue(packet);
        }
    }

    // Continuously dequeues input, calculates its FlipHash value, and prints the result.
    private static void executeFliphash(FlipHashQueue queue, Resource resource) {
        while (true) {
            String temp = queue.dequeue();
            if (temp != null) {
                long result = fliphashGeneral(temp, resource);
                System.out.println(result);
                System.out.println(" Freed");
            }
        }
    }

    // Computes a seeding value based on two short values
    private static long seeding(short a, short b) {
        return a + (b << 16);
    }

    // Implements the fliphashPow2 function from the C code.
    private static long fliphashPow2(String key, int twoPower) {

        Hasher64 xxh3 = XXH3_64.create(seeding((short) 0, (short) 0));
        long a = xxh3.hashCharsToLong(key) & ((1L << twoPower) - 1);

        long temp = a;
        int b = 0;
        while (temp > 1) {
            temp >>= 1;
            b++;
        }
        //long c = xxh3_64bits_withSeed(key, KEY_SIZE_FLIPHASH, seeding((short) b, (short) 0)) & ((1L << b) - 1);
        xxh3 = XXH3_64.create(seeding((short) b, (short) 0));
        long c = xxh3.hashCharsToLong(key) & ((1L << b) - 1);
        return a + c;
    }

    // Implements the fliphashGeneral function using the number of resources.
    public static long fliphashGeneral(String key, Resource resource) {
        long numResources = resource.count;
        int r = 0;
        // Calculate the number of bits required (r is essentially log2(numResources) rounded up)
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

            for (int i = 0; i < 64; i++) {
                xxh3 = XXH3_64.create(seeding((short) (r-1) , (short) i));
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



    // Convenience method: xxh3_64bits without a seed (seed = 0)
    private static long xxh3_64bits(String input, int keySize) {
        return xxh3_64bits_withSeed(input, keySize, 0);
    }

    // Generates a key from the first line of a cache file using xxHash.
    private static long keyGenerator(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                return xxh3_64bits(line, VALUE_SIZE);
            }
            throw new IOException("Cache file is empty");
        }
    }

    // Checks if the file size is at or above the maximum cache size.
    private static boolean checkFileSize(File file) {
        return file.length() >= MAX_CACHE_SIZE_FLIPHASH;
    }

    // Checks the cache file size and renames the file if it exceeds the limit.
    private static void checkFile(String filename) {
        File file = new File(filename);
        if (checkFileSize(file)) {
            File newFile = new File(filename + "_copy");
            boolean success = file.renameTo(newFile);
            if (!success) {
                System.err.println("Failed to rename file");
            }
        }
    }
    private static long xxh3_64bits_withSeed(String input, int keySize, int seed) {
        byte[] bytes = input.getBytes();
        long hash = seed;
        int len = Math.min(bytes.length, keySize);
        for (int i = 0; i < len; i++) {
            // Using a simple mixing function; note that this is not the actual xxHash3 algorithm.
            hash = hash * 31 + (bytes[i] & 0xff);
        }
        return hash;
    }

}
