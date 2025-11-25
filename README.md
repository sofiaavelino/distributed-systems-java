# Distributed Systems — Java Implementations

This repository contains three distributed systems prototypes implemented in Java as part of an academic assignment. Each scenario explores a core distributed algorithm using multiple peers running on different machines and communicating over TCP sockets.

## 📌 Implemented Scenarios
- **Mutual Exclusion — Token Ring Algorithm**  
  Exclusive access to a shared remote server is ensured by circulating a token through peers organized in a logical ring.

- **Data Dissemination — Anti-Entropy Algorithm**  
  Peers periodically synchronize data (random words) using push–pull gossip.

- **Totally Ordered Multicast — Lamport Clocks**  
  All peers deliver broadcast messages in the exact same order by using logical timestamps.

A Poisson event generator is used across all scenarios to simulate realistic distributed timings.

---

## ⚙️ Before Running Any Exercise

You must **run all commands from outside the `ds` directory**.

### Compile the Poisson process (required for all exercises):
```bash
javac ds/poisson/src/poisson/PoissonProcess.java
```

## 🟦 Exercise 1 — Token Ring Mutual Exclusion

### 🔨 Compile

```bash
javac -cp .:ds/poisson/src ds/assign/ring/Peer.java
javac ds/assign/ring/Server.java
javac ds/assign/ring/Injector.java
```

### ▶️ Run
#### 1. Start the Server

```bash
java ds.assign.ring.Server <server_port> localhost
```


