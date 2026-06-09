# 🚀 Port Buddy — Secure Localhost Tunneling (HTTP, TCP, UDP)

[Official Website](https://portbuddy.dev)

[License](https://github.com/amak-tech/port-buddy/blob/master/LICENSE)

**Port Buddy** is a lightweight, high-performance **ngrok alternative** designed to instantly expose your local development servers to the public internet. Unlike standard tools, Port Buddy features native, zero-configuration support for **HTTP/HTTPS**, raw **TCP**, and **UDP** protocols right out of the box.

Whether you need to test webhooks, expose a local database, debug an SSH connection, or host a local multiplayer game server, Port Buddy creates a secure reverse tunnel with a single command—no router port forwarding required.

---

## ✨ Key Features

*   **🌐 HTTP & HTTPS Tunnels:** Easily share local web applications and APIs with auto-generated SSL certificates.
*   **🔌 Raw TCP Port Forwarding:** Expose local databases (PostgreSQL, MySQL), SSH, or custom TCP services securely.
*   **🎮 Native UDP Support:** Perfect for debugging IoT protocols, VoIP services, and hosting game servers (Minecraft, Rust, etc.).
*   **🔗 Custom & Static Domains:** Say goodbye to random URLs. Keep your endpoints permanent even across restarts.
*   **⚡ WebSockets & SSE:** Full support for real-time streaming, chat apps, and live data connections.
*   **🖥️ Background Daemon:** Run Port Buddy as a system service (`systemd` or Windows Task Scheduler) for persistent tunneling.

---

## ⚡ Quick Start: How to Expose Local Ports

Install the Port Buddy CLI and start tunneling in seconds.

### 1. Expose Local Web Server (HTTP)
To expose a standard local web app running on port 3000:
```bash
portbuddy 3000
```
*Outputs a public URL like: `https://my-web-site.portbuddy.dev`*

### 2. Expose Local TCP Port (Databases, SSH)
To expose a local TCP service, such as a PostgreSQL database running on port 5432:
```bash
portbuddy tcp 5432
```
*Outputs a public endpoint like: `net-proxy-1.portbuddy.dev:43452`*

### 3. Expose Local UDP Port (Gaming, VoIP)
To share a local UDP game server or service running on port 19132:
```bash
portbuddy udp 19132
```

### 4. Run with Docker

You can also run the PortBuddy CLI inside a Docker container.

#### Pull the Image
If you haven't built it locally, you can use the official image (replace with actual image name if applicable):
```bash
docker pull amaktech/portbuddy:latest
```
*Note: If you are developing locally, you can build the image using `Dockerfile-cli`.*

#### Authentication with Docker
To use PortBuddy in Docker, you should mount your token file from your host machine to the container. The CLI expects the token at `/root/.port-buddy/token`.

## 🛠️ CLI Usage

```text
Usage: portbuddy [options] [mode] [host:][port]

Modes:
  http (default), tcp, udp

Options:
  -d,  --domain=<domain>        Requested static subdomain (e.g. my-app)
  -pr, --port-reservation=<hp>  Use specific port reservation host:port for TCP/UDP
  -pc, --passcode=<passcode>    Protect tunnel with a passcode
  -v,  --verbose                Enable verbose logging
  -h,  --help                   Show help message
  -V,  --version                Show version info
```

## 🏗️ Architecture

PortBuddy is built as a multi-modular system:

- **`cli`**: GraalVM-native command-line application (Java 25).
- **`server`**: Spring Boot 3.5.7 API & Tunnel Management.
- **`net-proxy`**: High-performance TCP/UDP proxy.
- **`gateway`**: Webflux-based API Gateway.
- **`web`**: React-based dashboard (TypeScript + TailwindCSS).
- **`eureka`**: Service discovery.
- **`ssl-service`**: Automated SSL certificate management.
- **`common`**: Shared DTOs and utilities.

## 🛠️ Development

### Prerequisites
- Java 25
- Docker & Docker Compose
- Spring Boot 3
- Maven 3.9+
- Node.js & npm (for web module)

### Build
To build the entire project:
```bash
./mvnw clean install
```

### Run with Docker Compose
```bash
docker-compose up -d
```

## 📄 License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

## 🤝 Community Projects

Projects built by the PortBuddy community:

- **[PortBuddy GUI](https://github.com/quack-stuff/portbuddy-gui)** by Quack - A graphical user interface for PortBuddy on Windows.
