# PortBuddy 🚀

PortBuddy is a powerful yet simple tool that allows you to expose a port opened on your local host or in a private network to the public internet. It works as a secure tunnel, similar to ngrok, providing a public URL for your local services.

Whether you're developing a web app, testing webhooks, or sharing access to a local database, PortBuddy makes it easy and secure.

## ✨ Features

- **Multi-protocol support**: Tunnel HTTP, TCP, and UDP traffic.
- **SSL by default**: All HTTP tunnels are automatically secured with SSL.
- **Customizable**: Support for static subdomains and custom domains.
- **Websocket support**: Full support for real-time applications.
- **Private tunnels**: Secure your tunnels with passcodes.
- **Cross-platform CLI**: Lightweight CLI built with Java 25 and GraalVM (native executable).
- **Web Dashboard**: Manage your tunnels, subscriptions, and team members easily.

## 🚀 Quick Start

### 1. Installation

Download the latest version of the `portbuddy` CLI for your platform (Windows, Linux, or Mac).

### 2. Authentication

Before exposing ports, you need to authenticate your CLI.
1. Log in to your account at [portbuddy.dev](https://portbuddy.dev).
2. Generate an API Token in your dashboard.
3. Run the following command:
   ```bash
   portbuddy init {YOUR_API_TOKEN}
   ```

### 3. Expose a Port

#### HTTP (Default)
Expose a local web server running on port 3000:
```bash
portbuddy 3000
```
Output: `http://localhost:3000 exposed to: https://abc123.portbuddy.dev`

#### TCP
Expose a local PostgreSQL database:
```bash
portbuddy tcp 5432
```
Output: `tcp localhost:5432 exposed to: net-proxy-3.portbuddy.dev:43452`

#### UDP
Expose a local UDP service:
```bash
portbuddy udp 9000
```

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

## 💳 Subscription Plans

| Feature | Pro ($0/mo) | Team ($10/mo) |
| :--- | :--- | :--- |
| Tunnels | HTTP, TCP, UDP | Everything in Pro |
| SSL | Included | Included |
| Subdomains | Static | Static |
| Custom Domains | Supported | Supported |
| Team Members | - | Included |
| Free Tunnels | 1 at a time | 10 at a time |
| Extra Tunnels | $1/mo each | $1/mo each |
| Support | Standard | Priority |

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

Copyright © 2026 PortBuddy. All rights reserved.
