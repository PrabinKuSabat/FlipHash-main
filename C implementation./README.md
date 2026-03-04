# 🔵 C Implementation

The C implementation of the **FlipHash consistent hashing algorithm**, featuring a parallel producer-consumer pipeline for high-throughput packet hashing.

---

## 📄 Files

| File | Purpose |
|------|---------|
| `fliphash.c` | Full FlipHash implementation with parallel queue-based processing |
| `onlyfliphash.c` | Stripped-down version containing only the core FlipHash logic |
| `test.c` | Test harness for verifying hash correctness |
| `makefile` | Build script — compiles the project with `make` |
| `a.out` | Compiled binary from `fliphash.c` |
| `onlyfliphash` | Compiled binary from `onlyfliphash.c` |
| `original` | Original reference binary |
| `readme.txt` | Original implementation notes |

---

## ⚙️ How It Works

1. The program takes the **number of resources (N)** as input from the user
2. A **producer thread** reads input packets and enqueues them into a shared queue
3. A **consumer thread** simultaneously dequeues packets and hashes them using FlipHash
4. Hash outputs (bucket assignments) are printed to `stdout`

This producer-consumer design allows input and hashing to run **in parallel**, maximizing throughput.

```
[stdin] → Producer → [Queue] → Consumer → FlipHash(packet) → [stdout]
```

---

## 🚀 How to Build & Run

### Full implementation

```bash
cd "C implementation."
make
./a.out
```

### Only FlipHash core (no parallel processing)

```bash
gcc -o onlyfliphash onlyfliphash.c
./onlyfliphash
```

### Run tests

```bash
gcc -o test test.c
./test
```

---

## 📝 Makefile Targets

```bash
make        # Build the main executable
make clean  # Remove compiled binaries
```

---

> 💡 The `onlyfliphash.c` file is useful for understanding the pure algorithm without the threading/queue boilerplate. Start there if you want to understand the hash logic.
