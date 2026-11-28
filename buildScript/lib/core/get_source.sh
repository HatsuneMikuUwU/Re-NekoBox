#!/bin/bash
set -e

source "buildScript/init/env.sh"
ENV_NB4A=1
source "buildScript/lib/core/get_source_env.sh"
pushd ..

####

if [ ! -d "sing-box" ]; then
  git clone --no-checkout "$SING_BOX_REPO" sing-box
fi
pushd sing-box
git fetch --depth 1 "$SING_BOX_REPO" "$COMMIT_SING_BOX"
git checkout --detach FETCH_HEAD
if [ "$CORE_CHANNEL" = "preview" ]; then
  git apply -C1 --ignore-whitespace --exclude=box.go --exclude=common/dialer/default.go "$SRC_ROOT/buildScript/lib/core/patches/android-preview.patch"
  python3 - <<'PY'
from pathlib import Path

p = Path('box.go')
s = p.read_text()
old = 'experimentalOptions.CacheFile.Enabled || options.PlatformLogWriter != nil'
if old in s:
    p.write_text(s.replace(old, 'experimentalOptions.CacheFile.Enabled'))

dp = Path('common/dialer/default.go')
ds = dp.read_text()
if 'var DoNotSelectInterface' not in ds:
    assert '
var (
' in ds, 'Expected var ( block in common/dialer/default.go'
    ds = ds.replace('
var (
', '
var DoNotSelectInterface = false

var (
', 1)
if 'if d.networkStrategy == nil {' in ds:
    ds = ds.replace('if d.networkStrategy == nil {', 'if DoNotSelectInterface || d.networkStrategy == nil {')
dp.write_text(ds)
PY
fi
git apply -C1 --ignore-whitespace "$SRC_ROOT/buildScript/lib/core/patches/core-compat.patch"
popd

####

if [ ! -d "libneko" ]; then
  git clone --no-checkout https://github.com/MatsuriDayo/libneko.git
fi
pushd libneko
git checkout "$COMMIT_LIBNEKO"
popd

####

popd
