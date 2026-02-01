#!/bin/bash

#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#

# Check if running as root
if [ "$EUID" -ne 0 ]; then
  echo "Please run as root (sudo)"
  exit 1
fi

# Parse arguments
POSITIONAL_ARGS=()
CUSTOM_NAME=""

while [[ $# -gt 0 ]]; do
  case $1 in
    --name)
      CUSTOM_NAME="$2"
      shift # past argument
      shift # past value
      ;;
    *)
      POSITIONAL_ARGS+=("$1") # save positional arg
      shift # past argument
      ;;
  esac
done

set -- "${POSITIONAL_ARGS[@]}" # restore positional parameters

if [ "$#" -lt 2 ]; then
    echo "Usage: $0 [options] <mode> <port> [host]"
    echo "Options:"
    echo "  --name <name>    Custom name for the service"
    echo "Example: $0 tcp 22"
    echo "Example: $0 --name my-ssh-service tcp 22"
    exit 1
fi

MODE=$1
PORT=$2
HOST=$3

# Construct the argument for portbuddy
if [ -z "$HOST" ]; then
    TARGET="$PORT"
else
    TARGET="$HOST:$PORT"
fi

if [ -n "$CUSTOM_NAME" ]; then
    SERVICE_NAME="$CUSTOM_NAME"
else
    SERVICE_NAME="portbuddy-${MODE}-${PORT}"
fi
# Get the real user if running under sudo
REAL_USER=${SUDO_USER:-$USER}
# Get home dir of the real user
REAL_USER_HOME=$(getent passwd "$REAL_USER" | cut -d: -f6)

# Locate portbuddy binary
PORTBUDDY_BIN=$(which portbuddy)
if [ -z "$PORTBUDDY_BIN" ]; then
    # Try common location
    if [ -f "/usr/local/bin/portbuddy" ]; then
        PORTBUDDY_BIN="/usr/local/bin/portbuddy"
    else 
        echo "Error: portbuddy binary not found in PATH or /usr/local/bin"
        echo "Please make sure portbuddy is installed and available."
        exit 1
    fi
fi

echo "Setting up Port Buddy service..."
echo "User: $REAL_USER"
echo "Binary: $PORTBUDDY_BIN"
echo "Command: $PORTBUDDY_BIN $MODE $TARGET"

# Create systemd service file
cat <<EOF > /etc/systemd/system/$SERVICE_NAME.service
[Unit]
Description=Port Buddy Service
After=network.target

[Service]
Type=simple
User=$REAL_USER
ExecStart=$PORTBUDDY_BIN $MODE $TARGET -n
Restart=on-failure
RestartSec=5
Environment=HOME=$REAL_USER_HOME

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd, enable and start service
systemctl daemon-reload
systemctl enable $SERVICE_NAME
systemctl restart $SERVICE_NAME

echo "----------------------------------------"
echo "Service $SERVICE_NAME has been installed and started."
echo "Check status with: systemctl status $SERVICE_NAME"
echo "View logs with: journalctl -u $SERVICE_NAME -f"
