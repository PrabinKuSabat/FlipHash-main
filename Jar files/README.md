# 📦 Jar Files

Java JAR (Java ARchive) packaging demo. This folder contains a working example of how to compile Java source, bundle it into a `.jar` file, and run it using a custom manifest.

---

## 📄 Files

| File | Purpose |
|------|---------|
| `MatrixMultiplication.java` | Sample Java program demonstrating matrix multiplication |
| `MatrixMultiplication.class` | Compiled bytecode |
| `manifest.txt` | JAR manifest file specifying the `Main-Class` entry point |

---

## 🚀 How to Build and Run the JAR

### Step 1: Compile

```bash
javac MatrixMultiplication.java
```

### Step 2: Package into a JAR

```bash
jar cfm MatrixMultiplication.jar manifest.txt MatrixMultiplication.class
```

### Step 3: Run the JAR

```bash
java -jar MatrixMultiplication.jar
```

---

## 📝 manifest.txt Format

The manifest file specifies which class contains the `main()` entry point:

```
Main-Class: MatrixMultiplication
```

> ⚠️ The manifest must end with a newline, otherwise the JAR will fail to recognize the entry point.

---

## 🧠 Why JAR Files?

JAR files bundle multiple `.class` files, resources, and metadata into a single distributable archive. They allow Java applications to be:

- **Distributed** as a single file
- **Run** with a single `java -jar` command
- **Included** as a library dependency in other projects via the CLASSPATH

> 💡 This folder serves as a **learning artifact** for JAR packaging, separate from the main FlipHash implementation.
