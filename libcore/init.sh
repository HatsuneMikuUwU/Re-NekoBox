#!/usr/bin/env bash
set -euo pipefail
source ../buildScript/lib/core/get_source_env.sh
export GOPATH="${GOPATH:-$(go env GOPATH)}"
export PATH="$GOPATH/bin:$PATH"
mkdir -p "$GOPATH/bin"
if [ ! -d gomobile ]; then
  git clone --no-checkout https://github.com/MatsuriDayo/gomobile.git gomobile
fi
git -C gomobile checkout "$COMMIT_GOMOBILE"
go mod edit -replace golang.org/x/mobile=./gomobile
(
  cd gomobile
  go build -o "$GOPATH/bin/gomobile-matsuri" ./cmd/gomobile
  go build -o "$GOPATH/bin/gobind-matsuri" ./cmd/gobind
)
cp -f "$GOPATH/bin/gobind-matsuri" "$GOPATH/bin/gobind"
go mod edit -droprequire github.com/sagernet/gvisor
go mod tidy
