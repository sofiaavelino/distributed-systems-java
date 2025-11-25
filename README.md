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

#### 2. Start each peer (one per machine), forming the ring

```bash
java -cp .:ds/poisson/src ds.assign.ring.Peer <own_port> localhost <next_peer_port> <server_port>
```

#### 3. Start the injector (peer that initially holds the token)

```bash
java ds.assign.ring.Injector <peer_port_with_initial_token>
```

## 🟩 Exercise 2 — Anti-Entropy Data Dissemination

### 🔨 Compile

```bash
javac -cp .:ds/poisson/src ds/assign/entropy/Peer.java
javac ds/assign/entropy/Starter.java
```

### ▶️ Run

#### 1. Start each peer (listing its neighbors)

```bash
java -cp .:ds/poisson/src ds.assign.entropy.Peer <own_port> localhost <neighbor_port_1> <neighbor_port_2> ...
```

#### 2. Start the Starter

```bash
java ds.assign.entropy.Starter <port_peer1> <port_peer2> <port_peer3> ...
```

## 🟥 Exercise 3 — Totally Ordered Multicast (Lamport Clocks)

### 🔨 Compile

```bash
javac -cp .:ds/poisson/src ds/assign/chat/Peer.java
javac ds/assign/chat/Starter.java
```

### ▶️ Run

#### 1. Start each peer (listing all other peers)

```bash
java -cp .:ds/poisson/src ds.assign.chat.Peer <own_port> localhost <port_peer2> <port_peer3> ...
```

#### 2. Start the Starter

```bash
java ds.assign.chat.Starter <port_peer1> <port_peer2> <port_peer3> ...
```

## 🛑 Stopping All Peers Simultaneously

In Exercise 3, to compare message lists correctly, stop all peers at the same time:

### 1. Identify processes:

```bash
ps aux | grep <peer_port>
```

### 2. Kill them all:

```bash
kill -9 <pid1> <pid2> <pid3> ...
```
