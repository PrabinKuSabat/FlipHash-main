# 📦 Jar Files

Precompiled JAR files for running the FlipHash Java implementation without needing to compile from source.

## Contents

This folder contains one or more standalone `.jar` files that can be run directly with:

```bash
java -jar <filename>.jar
```

## When to Use These

Use the JARs in this folder when you:
- Want to **run the demo quickly** without setting up a Java development environment
- Need a **prebuilt artifact** for deployment or testing
- Are on a machine where `javac` is not available but `java` (JRE) is installed

## Full JAR (with dependencies)

The `Backend.jar` inside `Java implementation/fliphash/` is a fat JAR that includes all dependencies (OSHI, JNA, SLF4J) and can be run standalone:

```bash
java -jar "Java implementation/fliphash/Backend.jar"
```

## Building Your Own JAR

To rebuild the JAR from source:

```bash
cd "Java implementation/fliphash"
javac -cp ".:jna-5.13.0.jar:oshi-core-6.3.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" *.java
jar cfm MyFlipHash.jar META-INF/MANIFEST.MF *.class fliphash/ xxh3Java/ hashing/ backend/ client/
```

## 🔗 References

- Full source: [`../Java implementation/fliphash/`](../Java%20implementation/fliphash/)
- Algorithm explained: [`../README.md`](../README.md)
