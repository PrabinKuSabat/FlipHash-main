<div align="center">

# 🔀 FlipHash — Constant-Time Consistent Range-Hashing

> *A Java & C implementation of the FlipHash algorithm from the research paper by Charles Masson & Homin K. Lee.*

[![Paper](https://img.shields.io/badge/Paper-arXiv%3A2402.17549-red?style=for-the-badge)](https://arxiv.org/abs/2402.17549)
[![Language Java](https://img.shields.io/badge/Java-Implementation-orange?style=for-the-badge&logo=openjdk)](./Java%20implementation/)
[![Language C](https://img.shields.io/badge/C-Implementation-blue?style=for-the-badge&logo=c)](./C%20implementation./)
[![Author](https://img.shields.io/badge/Author-Prabin%20Kumar%20Sabat-7c3aed?style=for-the-badge)](https://prabins.vercel.app)

</div>

---

## 📜 Research Paper

**Title:** FlipHash: A Constant-Time Consistent Range-Hashing Algorithm  
**Authors:** Charles Masson, Homin K. Lee  
**Published:** arXiv:2402.17549 \[cs.DS\] — February 27, 2024  
**DOI:** [10.48550/arXiv.2402.17549](https://doi.org/10.48550/arXiv.2402.17549)  
**Full Paper:** [arxiv.org/abs/2402.17549](https://arxiv.org/abs/2402.17549)

---

## 🎯 What is FlipHash?

**Consistent range-hashing** is a foundational technique in distributed systems. It assigns keys (requests, data chunks, user IDs) to one of **N numbered resources** (servers, cache nodes, shards) such that:

1. Keys are **evenly distributed** across all N resources
2. When N changes (a server is added or removed), only the **minimum required keys** are remapped — no unnecessary reshuffling occurs

FlipHash solves this with **O(1) constant-time complexity** — beating the widely-used [Jump Consistent Hash](https://arxiv.org/abs/1406.2294) which runs in **O(log N)**.

### Real-World Use Cases

- **Load balancers** routing HTTP requests to backend servers
- **Distributed caches** (like Memcached clusters) assigning keys to nodes
- **Sharded databases** mapping records to shards
- **Content delivery networks** routing assets to edge servers

---

## 🧠 The Algorithm Explained

FlipHash is composed of two functions: `fliphashPow2` for power-of-2 resource counts and `fliphashGeneral` for any arbitrary N.

### 🔑 Seeding

All hash calls use a two-dimensional seed:

```
seed(a, b) = a + (b << 16)
```

This lets the algorithm derive multiple **independent** hash values for the same key without storing state, simply by varying `a` and `b`.

---

### Step 1 — `fliphashPow2(key, r)` → bucket in [0, 2ʳ)

Maps a key to range \[0, 2^r) **consistently**:

```
1.  a  =  H(key, seed(0, 0))  mod  2^r
             ↓
2.  b  =  ⌊log₂(a)⌋                   ← position of highest set bit in a
             ↓
3.  c  =  H(key, seed(b, 0))  mod  2^b
             ↓
4.  return  a + c
```

**Intuition:** `a` falls in the range \[2^b, 2^(b+1)). Adding `c ∈ [0, 2^b)` fills in all buckets in \[2^b, 2^(b+1) + 2^b) uniformly. The second hash ensures that when N shrinks or grows, keys are remapped predictably.

**Java code from `FlipHash.java`:**
```java
private static long fliphashPow2(String key, int twoPower) {
    Hasher64 xxh3 = XXH3_64.create(seeding((short) 0, (short) 0));
    long a = xxh3.hashCharsToLong(key) & ((1L << twoPower) - 1);  // a mod 2^r

    long temp = a; int b = 0;
    while (temp > 1) { temp >>= 1; b++; }    // b = floor(log₂(a))

    xxh3 = XXH3_64.create(seeding((short) b, (short) 0));
    long c = xxh3.hashCharsToLong(key) & ((1L << b) - 1);         // c mod 2^b
    return a + c;
}
```

---

### Step 2 — `fliphashGeneral(key, N)` → bucket in [0, N)

Handles any arbitrary N using `fliphashPow2` as a building block:

```
1.  r  =  ceil(log₂(N))                    ← smallest r with 2^r ≥ N
            ↓
2.  d  =  fliphashPow2(key, r)
            ↓
3.  d < N?  →  return d                   ← direct hit ✅
            ↓
4.  d ≥ N (forbidden zone [N, 2^r)):
    For i = 0 → 63:
        e = H(key, seed(r-1, i)) mod 2^r

        e < 2^(r-1)  →  return fliphashPow2(key, r-1)    ← "flip down" 🔃
        e < N        →  return e                           ← remap to valid zone ✅
            ↓
5.  Fallback: return fliphashPow2(key, r-1)
```

**The "flip" insight:** When a key lands in the forbidden zone \[N, 2^r), the algorithm flips it either down to \[0, 2^(r-1)) or maps it directly to the valid upper zone \[2^(r-1), N). This is the namesake operation that gives FlipHash its O(1) complexity — no iteration over the full N range.

**Java code from `FlipHash.java`:**
```java
public static long fliphashGeneral(String key, Resource resource) {
    long N = resource.count;
    int r = 0;
    for (long temp = N; temp > 0; temp >>= 1) { r++; }  // r = ⌈log₂(N)⌉

    long d = fliphashPow2(key, r);
    if (d < N) return d;                   // direct hit

    long halfRange = 1L << (r - 1);        // 2^(r-1)
    for (int i = 0; i < 64; i++) {
        Hasher64 xxh3 = XXH3_64.create(seeding((short)(r-1), (short)i));
        long e = xxh3.hashCharsToLong(key) & ((1L << r) - 1);

        if (e < halfRange) return fliphashPow2(key, r - 1);  // flip down
        if (e < N)         return e;                          // remap
    }
    return fliphashPow2(key, r - 1);  // fallback
}
```

---

## 🏆 FlipHash vs Jump Consistent Hash

| Property | Jump Consistent Hash | FlipHash |
|----------|---------------------|----------|
| **Time Complexity** | O(log N) | **O(1)** |
| Memory | O(1) | O(1) |
| Even distribution | ✅ | ✅ |
| Minimal remapping on change | ✅ | ✅ |
| Sequential resource indexing | Required | Required |
| Speed at large N | Slower | **Faster** |

> From the paper: *"FlipHash beats Jump Consistent Hash's cost both theoretically and in experiments over practical settings."*

---

## 📁 Project Structure

```
FlipHash-main/
├── Java implementation/     # Full Java implementation with load balancer simulation
│   └── fliphash/            # Core package: FlipHash algorithm + server/client demo
├── C implementation./       # C implementation of the FlipHash algorithm
├── Final Submission/        # Final academic project submission package
├── Jar files/               # Precompiled standalone JAR files
├── Client.java              # Standalone client entrypoint
└── README.md                # This file
```

---

## 🚀 Quick Start

### Java

```bash
cd "Java implementation/fliphash"

# Compile
javac -cp ".:jna-5.13.0.jar:oshi-core-6.3.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" *.java

# Run the core algorithm
java -cp ".:jna-5.13.0.jar:oshi-core-6.3.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" fliphash.FlipHash
```

> On Windows replace `:` with `;` in the classpath.

### C

```bash
cd "C implementation."
make
./onlyfliphash
```

---

## 🙏 Credits & Acknowledgements

### 📚 FlipHash Algorithm
**Authors:** Charles Masson & Homin K. Lee  
**Paper:** [FlipHash: A Constant-Time Consistent Range-Hashing Algorithm](https://arxiv.org/abs/2402.17549)  
**arXiv:** [arXiv:2402.17549 \[cs.DS\]](https://arxiv.org/abs/2402.17549)  
This implementation is based directly on the algorithm described in this paper.

### ⚡ XXH3 Hashing
**Author:** Yann Collet  
**Repository:** [github.com/Cyan4973/xxHash](https://github.com/Cyan4973/xxHash)  
The XXH3-64 hash function is used as the underlying hash primitive in all FlipHash operations.

### 💻 OSHI — Operating System & Hardware Information
**Repository:** [github.com/oshi/oshi](https://github.com/oshi/oshi)  
Used for real-time CPU and memory monitoring in the load balancer demonstration.

### 🔗 JNA — Java Native Access
**Repository:** [github.com/java-native-access/jna](https://github.com/java-native-access/jna)  
Enables the Java implementation to interface with native system libraries.

---

<div align="center">

Implemented by [Prabin Kumar Sabat](https://prabins.vercel.app)  
Based on research by [Charles Masson & Homin K. Lee](https://arxiv.org/abs/2402.17549)

</div>

---

<div align="center">
  <br/>
  <a href="https://www.sssihl.edu.in" target="_blank" rel="noopener noreferrer">
    <img src="assets/sssihl-logo.png" alt="Sri Sathya Sai Institute of Higher Learning" width="140" />
    <br/>
    <b>Sri Sathya Sai Institute of Higher Learning</b>
  </a>
  <br/>
  <sub>Prasanthi Nilayam (Puttaparthi) — Department of Mathematics and Computer Science</sub>
</div>
