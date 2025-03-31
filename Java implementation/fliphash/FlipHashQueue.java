package fliphash;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A thread-safe queue for storing fixed-size input packets for the FlipHash process.
 * <p>
 * This queue uses an {@link ArrayBlockingQueue} internally to enforce a maximum queue size.
 * It provides methods to enqueue and dequeue string inputs, blocking if necessary.
 * </p>
 */
public class FlipHashQueue {

    /** Maximum number of elements the queue can hold. */
    private static final int MAX_QUEUE_SIZE = 1000;

    /** The underlying blocking queue used for storage. */
    private final BlockingQueue<String> queue;

    /**
     * Constructs a new FlipHashQueue with a fixed maximum size.
     */
    public FlipHashQueue() {
        queue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
    }

    /**
     * Enqueues an input string into the queue.
     * <p>
     * If the queue is full, this method blocks until space is available.
     * </p>
     *
     * @param input the string input to be enqueued.
     */
    public void enqueue(String input) {
        try {
            queue.put(input);
        } catch (InterruptedException e) {
            // Restore interrupted status and exit.
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Dequeues and returns a string from the queue.
     * <p>
     * If the queue is empty, this method blocks until an element is available.
     * </p>
     *
     * @return the dequeued string, or {@code null} if interrupted.
     */
    public String dequeue() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            // Restore interrupted status and return null.
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
