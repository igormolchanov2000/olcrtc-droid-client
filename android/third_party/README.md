# Third-party native sources

This directory vendors external native code required by the Android client.

## Included components

- `hev-socks5-tunnel/`
  - Source staged from the local `../v2rayng/hev-socks5-tunnel` checkout during initial integration
  - Upstream project: https://github.com/heiher/hev-socks5-tunnel
  - License: MIT (`hev-socks5-tunnel/LICENSE`)

The Android app builds the JNI library from this staged source via `scripts/build-tun2socks.sh`.
