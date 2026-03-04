# ⚡ fliphash — Core Package

The complete source code for the FlipHash implementation. This is the heart of the project — the algorithm, a multithreaded pipeline, and a full load balancer simulation.

## 📄 Source Files

| File | Role |
|------|---------|
| `FlipHash.java` | **Core algorithm** — `fliphashPow2` and `fliphashGeneral` |
| `FlipHashQueue.java` | Thread-safe blocking queue for the input pipeline |
| `LoadBalancerServer.java` | Load balancer server that routes keys via FlipHash |
| `BackendServer.java` | Simulated backend server receiving routed requests |
| `FlipHashClient.java` | Test client sending requests through the load balancer |
| `TerminalDisplayManager.java` | Live terminal dashboard showing routing statistics |
| `Validator.java` | Input validation (numeric, string, range checks) |

## 📚 Dependencies

| JAR | Version | Purpose |
|-----|---------|----------|
| `jna-5.13.0.jar` | 5.13.0 | Java Native Access — native library bridging |
| `jna-platform-5.13.0.jar` | 5.13.0 | JNA platform extensions |
| `oshi-core-6.3.0.jar` | 6.3.0 | OS & Hardware Info — CPU/memory monitoring |
| `slf4j-api-2.0.9.jar` | 2.0.9 | Logging API (required by OSHI) |
| `slf4j-simple-2.0.9.jar` | 2.0.9 | Logging implementation |

---

## 🔬 Implementation Walkthrough

### `FlipHash.java` — The Algorithm

#### Constants

```java
private static final int VALUE_SIZE = 64;                  // Max key length in characters
private static final int MAX_CACHE_SIZE_FLIPHASH = 64000;  // Max cache file size (bytes)
private static final int KEY_SIZE_FLIPHASH = 64;            // Key size for hashing
```

#### Seeding Function

```java
private static long seeding(short a, short b) {
    return a + (b << 16);
}
```

Packs two 16-bit values into a 32-bit seed. Used to generate **independent hash values** at each step of the algorithm without maintaining state — `seed(0,0)` for the first hash, `seed(b,0)` for the second, `seed(r-1, i)` for the flip loop.

---

#### `fliphashPow2(key, r)` — Hash to [0, 2^r)

```java
private static long fliphashPow2(String key, int twoPower) {
    // Step 1: Hash key with seed(0,0) and mask to r bits
    Hasher64 xxh3 = XXH3_64.create(seeding((short) 0, (short) 0));
    long a = xxh3.hashCharsToLong(key) & ((1L << twoPower) - 1);

    // Step 2: Find b = floor(log₂(a)) by counting right-shifts until a ≤ 1
    long temp = a;
    int b = 0;
    while (temp > 1) { temp >>= 1; b++; }

    // Step 3: Hash key with seed(b,0) and mask to b bits
    xxh3 = XXH3_64.create(seeding((short) b, (short) 0));
    long c = xxh3.hashCharsToLong(key) & ((1L << b) - 1);

    // Step 4: Return a + c
    return a + c;
}
```

**What happens at each step:**

| Variable | Range | Meaning |
|----------|-------|---------|
| `a` | \[0, 2^r) | Primary hash output |
| `b` | \[0, r) | Bit-length of `a` minus 1 |
| `c` | \[0, 2^b) | Secondary hash — the "flip" component |
| `a+c` | \[0, 2^r + 2^(r-1)) | Final consistent bucket |

**Consistency property:** If `a` maps key to bucket `a`, then even if the range shrinks from 2^r to 2^(r-1), the key remaps predictably based on `b` and `c` — not randomly.

---

#### `fliphashGeneral(key, N)` — Hash to [0, N)

```java
public static long fliphashGeneral(String key, Resource resource) {
    long N = resource.count;

    // Compute r = number of bits needed to represent N
    int r = 0;
    for (long temp = N; temp > 0; temp >>= 1) { r++; }

    // Try direct mapping
    long d = fliphashPow2(key, r);
    if (d < N) return d;              // Lucky: lands in valid zone

    // d >= N: key landed in forbidden zone [N, 2^r)
    long halfRange = 1L << (r - 1);   // 2^(r-1)

    for (int i = 0; i < 64; i++) {
        Hasher64 xxh3 = XXH3_64.create(seeding((short)(r-1), (short)i));
        long e = xxh3.hashCharsToLong(key) & ((1L << r) - 1);

        if (e < halfRange)  return fliphashPow2(key, r - 1);  // flip down to [0, 2^(r-1))
        if (e < N)          return e;                           // remap to [2^(r-1), N)
    }

    return fliphashPow2(key, r - 1);  // rare fallback after 64 attempts
}
```

**Decision flow:**

```
key + N
  │
  ├─ d = fliphashPow2(key, r)
  │      ├─ d < N  →→→→→→→→→→→→→→→→  return d  (most common case)
  │      └─ d ≥ N  (forbidden zone)
  │              └─ loop i = 0..63:
  │                    e = H(key, seed(r-1, i)) mod 2^r
  │                    ├─ e < 2^(r-1)  →  fliphashPow2(key, r-1)  [flip down]
  │                    └─ e < N        →  return e               [valid remap]
  └─ fallback: fliphashPow2(key, r-1)
```

The loop is bounded at 64 iterations but converges in very few (the paper shows expected constant time). The fallback is hit only in extreme edge cases.

---

### `FlipHashQueue.java` — Thread-Safe Queue

A simple producer-consumer queue used between the input-reading thread and the hash-processing thread. The `enqueue` method adds keys, `dequeue` retrieves them. This decouples I/O latency from computation, ensuring throughput is never blocked by slow input.

---

### `LoadBalancerServer.java` — The Demo

A TCP socket server that:
1. Receives request keys from `FlipHashClient`
2. Calls `FlipHash.fliphashGeneral(key, N)` to determine the backend index
3. Forwards the request to the corresponding `BackendServer`
4. Returns the response to the client

This demonstrates FlipHash in a real distributed context — consistent routing means the same key **always reaches the same backend**, enabling session affinity, cache locality, and graceful scaling.

---

### `TerminalDisplayManager.java` — Live Dashboard

Renders a real-time terminal display showing:
- Number of active backend servers
- Request distribution across servers
- CPU and memory usage via OSHI
- FlipHash routing decisions in real time

---

## 🚀 How to Run

### 1. Compile Everything

```bash
cd "Java implementation/fliphash"

# Linux / macOS
javac -cp ".:jna-5.13.0.jar:oshi-core-6.3.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" \
    -sourcepath . FlipHash.java FlipHashQueue.java LoadBalancerServer.java \
    BackendServer.java FlipHashClient.java TerminalDisplayManager.java Validator.java

# Windows (replace : with ;)
javac -cp ".;jna-5.13.0.jar;oshi-core-6.3.0.jar;slf4j-api-2.0.9.jar;slf4j-simple-2.0.9.jar" *.java
```

### 2. Run Core FlipHash (standalone)

```bash
java -cp ".:jna-5.13.0.jar:oshi-core-6.3.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" \
    fliphash.FlipHash
# Enter total number of resources, then type keys line by line
```

### 3. Run Full Load Balancer Demo (3 terminals)

**Terminal 1 — Start backend servers:**
```bash
java -cp ".:jna-5.13.0.jar:oshi-core-6.3.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" \
    fliphash.BackendServer
```

**Terminal 2 — Start load balancer:**
```bash
java -cp ".:jna-5.13.0.jar:oshi-core-6.3.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" \
    fliphash.LoadBalancerServer
```

**Terminal 3 — Start client:**
```bash
java -cp ".:jna-5.13.0.jar:oshi-core-6.3.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" \
    fliphash.FlipHashClient
```

### 4. Or Run Prebuilt JAR

```bash
java -jar Backend.jar
```

---

## 🔗 References

- **FlipHash Paper:** [arXiv:2402.17549](https://arxiv.org/abs/2402.17549) — Charles Masson & Homin K. Lee
- **XXH3:** [github.com/Cyan4973/xxHash](https://github.com/Cyan4973/xxHash)
- **OSHI:** [github.com/oshi/oshi](https://github.com/oshi/oshi)
- **JNA:** [github.com/java-native-access/jna](https://github.com/java-native-access/jna)
