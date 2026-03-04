# 🅒 C Implementation — FlipHash

A C implementation of the FlipHash consistent range-hashing algorithm, based on [arXiv:2402.17549](https://arxiv.org/abs/2402.17549) by Charles Masson & Homin K. Lee.

## 📄 Files

| File | Purpose |
|------|---------|
| `fliphash.c` | Full C implementation of FlipHash including the load balancer integration |
| `onlyfliphash.c` | Standalone, minimal FlipHash implementation — algorithm only, no server code |
| `test.c` | Test harness for validating FlipHash outputs |
| `makefile` | Build rules for all targets |
| `a.out` | Compiled binary of `fliphash.c` |
| `onlyfliphash` | Compiled binary of `onlyfliphash.c` |
| `original` | Original reference binary |
| `readme.txt` | Original plaintext notes |

---

## 🔬 Algorithm in C

The C implementation directly mirrors the algorithm from the paper:

```c
/* Seeding: combine two values into a single hash seed */
static uint64_t seeding(uint16_t a, uint16_t b) {
    return a + ((uint64_t)b << 16);
}

/* fliphashPow2: consistent hashing to [0, 2^twoPower) */
static uint64_t fliphashPow2(const char *key, int twoPower) {
    uint64_t a = xxh3_64bits_withSeed(key, strlen(key), seeding(0, 0))
                 & ((1ULL << twoPower) - 1);
    int b = 63 - __builtin_clzll(a);  // b = floor(log2(a))
    uint64_t c = xxh3_64bits_withSeed(key, strlen(key), seeding(b, 0))
                 & ((1ULL << b) - 1);
    return a + c;
}

/* fliphashGeneral: consistent hashing to [0, N) for any N */
static uint64_t fliphashGeneral(const char *key, uint64_t N) {
    int r = 64 - __builtin_clzll(N);  // r = ceil(log2(N))
    uint64_t d = fliphashPow2(key, r);
    if (d < N) return d;

    uint64_t halfRange = 1ULL << (r - 1);
    for (int i = 0; i < 64; i++) {
        uint64_t e = xxh3_64bits_withSeed(key, strlen(key), seeding(r-1, i))
                     & ((1ULL << r) - 1);
        if (e < halfRange) return fliphashPow2(key, r - 1);
        if (e < N)         return e;
    }
    return fliphashPow2(key, r - 1);
}
```

**C advantage:** The `__builtin_clzll` intrinsic computes `floor(log2(a))` in a single CPU instruction, making the C version even faster than the Java equivalent which uses a loop.

---

## 🛠️ Build & Run

### Using make

```bash
cd "C implementation."
make           # builds all targets
./onlyfliphash # run the standalone FlipHash binary
```

### Manual compile

```bash
# Standalone FlipHash only
gcc -O2 -o onlyfliphash onlyfliphash.c
./onlyfliphash

# Full version
gcc -O2 -o fliphash fliphash.c
./fliphash

# Test suite
gcc -O2 -o test test.c
./test
```

---

## 🔗 References

- **FlipHash Paper:** [arXiv:2402.17549](https://arxiv.org/abs/2402.17549)
- **XXH3:** [github.com/Cyan4973/xxHash](https://github.com/Cyan4973/xxHash)
