#!/usr/bin/env bash
# Builds app/libs/libv2ray.aar from AndroidLibXrayLite (the gomobile-bound Xray-core
# used by XrayVpnService / CoreManager). Run this once before building the app —
# Gradle alone cannot do it because it needs Go + the Android NDK, not just the SDK.
#
# Requirements:
#   - Go 1.21+      (https://go.dev/dl/)
#   - Android SDK + NDK installed, with ANDROID_HOME / ANDROID_NDK_HOME exported
#   - gomobile:  go install golang.org/x/mobile/cmd/gomobile@latest
set -euo pipefail

REPO_URL="https://github.com/2dust/AndroidLibXrayLite.git"
WORKDIR="$(mktemp -d)"
ANDROID_API_LEVEL=24

cleanup() { rm -rf "$WORKDIR"; }
trap cleanup EXIT

if ! command -v go >/dev/null; then
  echo "Go is required. Install it from https://go.dev/dl/" >&2
  exit 1
fi

if ! command -v gomobile >/dev/null; then
  echo "Installing gomobile..."
  go install golang.org/x/mobile/cmd/gomobile@latest
  export PATH="$PATH:$(go env GOPATH)/bin"
fi

echo "Cloning $REPO_URL ..."
git clone --depth 1 "$REPO_URL" "$WORKDIR/AndroidLibXrayLite"
cd "$WORKDIR/AndroidLibXrayLite"

gomobile init
go mod tidy -v

echo "Building libv2ray.aar (this takes several minutes) ..."
gomobile bind -v -androidapi "$ANDROID_API_LEVEL" -trimpath \
  -ldflags='-s -w -buildid= -checklinkname=0' \
  -o libv2ray.aar ./

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mkdir -p "$PROJECT_ROOT/app/libs"
cp libv2ray.aar "$PROJECT_ROOT/app/libs/libv2ray.aar"

echo "Done: $PROJECT_ROOT/app/libs/libv2ray.aar"
