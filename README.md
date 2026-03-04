<div align="center">

# 🔀 FlipHash

> *A dual-language implementation of the FlipHash consistent hashing algorithm — in C and Java.*

[![Language C](https://img.shields.io/badge/Language-C-00599C?style=for-the-badge&logo=c)](https://en.wikipedia.org/wiki/C_(programming_language))
[![Language Java](https://img.shields.io/badge/Language-Java-orange?style=for-the-badge&logo=openjdk)](https://www.java.com)
[![Algorithm](https://img.shields.io/badge/Algorithm-Consistent%20Hashing-blueviolet?style=for-the-badge)](https://en.wikipedia.org/wiki/Consistent_hashing)
[![Author](https://img.shields.io/badge/Author-Prabin%20Kumar%20Sabat-7c3aed?style=for-the-badge)](https://prabins.vercel.app)

</div>

---

## 🧠 What Is FlipHash?

**FlipHash** is a **consistent hashing algorithm** designed for efficient resource distribution in distributed systems. Unlike traditional modulo-based hashing (where adding/removing a resource reshuffles most assignments), FlipHash achieves **minimal disruption** — only the keys that *must* move are reassigned.

Key properties of FlipHash:
- **Consistent** — adding or removing a bucket minimally disturbs existing key assignments
- **Uniform** — keys are distributed evenly across all available buckets
- **Fast** — O(log n) lookup time
- **Zero-allocation friendly** — designed to work without heap allocations in hot paths

Typical use cases: **load balancers**, **distributed caches**, **database sharding**, **CDN routing**.

---

## 📁 Project Structure

```
FlipHash-main/
├── C implementation./     # Full FlipHash implementation in C with parallel processing
├── Java implementation/   # FlipHash implementation ported to Java
├── Final Submission/      # Official project submission artifacts and packaged zip files
├── Jar files/             # Java JAR packaging demo (Matrix Multiplication example)
├── Client.java            # Java socket client related to networking integration
├── Client.class           # Compiled bytecode for Client.java
└── Zero-Allocation-Hashing # Reference artifact related to the zero-allocation hashing concept
```

---

## ⚡ How It Works

The FlipHash implementation processes hashing requests using a **producer-consumer parallel pipeline**:

```
User inputs N resources
        ↓
Producer thread reads input packets → enqueues to shared queue
        ↓
Consumer thread dequeues packets → applies FlipHash
        ↓
Hashed bucket assignment output to stdout
```

This concurrent design ensures high throughput — input and hashing happen simultaneously rather than sequentially.

---

## 🛠️ Implementations

### C Implementation

```bash
cd "C implementation."
make
./a.out
```

Enter the number of resources when prompted, then provide input packets.

### Java Implementation

```bash
cd "Java implementation/fliphash"
javac *.java
java Main
```

---

## 📚 Algorithm Reference

FlipHash is based on the consistent hashing research in the distributed systems literature. Related concept: **Zero-Allocation Hashing** — a high-performance hashing library that avoids heap allocation during hash computation, ideal for latency-sensitive systems.

- [Consistent Hashing — Wikipedia](https://en.wikipedia.org/wiki/Consistent_hashing)
- [Zero-Allocation-Hashing (OpenHFT)](https://github.com/OpenHFT/Zero-Allocation-Hashing)

---

## 💻 Prerequisites

| Component | Requirement |
|-----------|-------------|
| C compiler | GCC or Clang (`gcc --version`) |
| Java | JDK 8+ (`java -version`) |
| Build tool | `make` for C implementation |

---

## 👨‍💻 Author

**Prabin Kumar Sabat** (Roll No: 24010203007)
[prabins.vercel.app](https://prabins.vercel.app) · [GitHub @PrabinKuSabat](https://github.com/PrabinKuSabat)
