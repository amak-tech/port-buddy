#!/bin/bash

#
# Copyright (c) 2026 AMAK Inc. All rights reserved.
#

set -e

# Port Buddy Installation Script
# This script detects the platform and architecture, then downloads and installs
# the latest version of Port Buddy from GitHub releases.

OWNER="amak-tech"
REPO="port-buddy"
BINARY_NAME="portbuddy"
INSTALL_DIR="/usr/local/bin"

# Detect OS
OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
case "${OS}" in
  linux*)   PLATFORM="linux" ;;
  darwin*)  PLATFORM="macos" ;;
  *)        echo "Unsupported OS: ${OS}"; exit 1 ;;
esac

# Detect Architecture
ARCH_RAW="$(uname -m)"
case "${ARCH_RAW}" in
  x86_64)   ARCH="x64" ;;
  aarch64|arm64) ARCH="arm64" ;;
  *)        echo "Unsupported architecture: ${ARCH_RAW}"; exit 1 ;;
esac

ASSET_NAME="${BINARY_NAME}-${PLATFORM}-${ARCH}"

echo "Detecting latest version..."
LATEST_RELEASE_URL="https://api.github.com/repos/${OWNER}/${REPO}/releases/latest"
VERSION=$(curl -s "${LATEST_RELEASE_URL}" | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/')

if [ -z "${VERSION}" ]; then
  echo "Error: Could not determine the latest version."
  exit 1
fi

echo "Latest version: ${VERSION}"
DOWNLOAD_URL="https://github.com/${OWNER}/${REPO}/releases/download/${VERSION}/${ASSET_NAME}"

echo "Downloading ${ASSET_NAME}..."
curl -L "${DOWNLOAD_URL}" -o "${BINARY_NAME}"

echo "Installing to ${INSTALL_DIR}..."
chmod +x "${BINARY_NAME}"
sudo mv "${BINARY_NAME}" "${INSTALL_DIR}/${BINARY_NAME}"

echo "Successfully installed Port Buddy ${VERSION} to ${INSTALL_DIR}/${BINARY_NAME}"
echo "Run 'portbuddy --help' to get started."
