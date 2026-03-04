# ☕ Java Implementation — FlipHash

This folder contains the complete Java implementation of the FlipHash consistent range-hashing algorithm, as described in [arXiv:2402.17549](https://arxiv.org/abs/2402.17549).

The implementation goes beyond the core algorithm — it includes a full **distributed load balancer simulation** built on top of FlipHash, demonstrating the algorithm in a realistic distributed systems context.

## Structure

```
Java implementation/
└── fliphash/          ← the main package — all source code lives here
    ├── FlipHash.java          Core algorithm
    ├── FlipHashQueue.java     Thread-safe queue
    ├── LoadBalancerServer.java Load balancer using FlipHash
    ├── BackendServer.java     Simulated backend server
    ├── FlipHashClient.java    Test client
    ├── TerminalDisplayManager.java Live dashboard
    ├── Validator.java         Input validation
    ├── xxh3Java/              XXH3-64 hashing library
    └── *.jar                  Dependency JARs
```

## Key Design Decisions

- **XXH3-64 as the hash primitive** — Fast, high-quality 64-bit hashing from the xxHash family. The algorithm requires a seeded hash function; XXH3 provides excellent avalanche effect and speed.
- **Producer-consumer threading** — Input reading and hash computation run on separate threads, decoupling I/O latency from processing throughput.
- **Socket-based client-server demo** — `LoadBalancerServer`, `BackendServer`, and `FlipHashClient` communicate over TCP sockets, simulating a real distributed system where consistent hashing matters.

## See Also

For full implementation details, code walkthroughs, and run instructions, see **[`fliphash/README.md`](./fliphash/README.md)**.

For the algorithm theory, see the **[root README](../README.md)**.
