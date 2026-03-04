# ☕ Java Implementation

The Java port of the **FlipHash consistent hashing algorithm**.

---

## 📁 Structure

```
Java implementation/
└── fliphash/       # Java source files for the FlipHash algorithm
```

---

## 📄 What's Inside `fliphash/`

The `fliphash/` subfolder contains the Java source files implementing the FlipHash algorithm. The Java version mirrors the logic of the C implementation and provides the same consistent hashing functionality in a JVM-based environment.

---

## 🚀 How to Compile & Run

```bash
cd fliphash
javac *.java
java Main
```

---

## 🔄 C vs Java: Key Differences

| Aspect | C Implementation | Java Implementation |
|--------|-----------------|---------------------|
| Threading | POSIX threads (`pthread`) | Java `Thread` / `Runnable` |
| Queue | Manual linked-list queue | `LinkedList` / `ArrayDeque` |
| Memory | Manual allocation/free | Garbage collected |
| Build | `make` | `javac` |
| Performance | Lower overhead | Portable, JIT-compiled |

---

> 💡 The Java implementation is more portable and easier to extend. Use the C version for maximum performance in a Linux environment.
