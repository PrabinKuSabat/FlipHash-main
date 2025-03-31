package fliphash;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class FlipHashQueue {
    private static final int MAX_QUEUE_SIZE = 1000;
    private final BlockingQueue<String> queue;
    public FlipHashQueue(){
        queue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
    }

    public void enqueue(String input) {
        try {
            queue.put(input);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public String dequeue() {
        try {
            return queue.take(); // blocks until an element is available
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}