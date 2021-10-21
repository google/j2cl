#!/bin/bash
command -v wasm-opt >/dev/null 2>&1 \
  || { echo "Binaryen (wasm-opt) is not available. Aborting."; exit 1; }
wasm-opt "$@"
