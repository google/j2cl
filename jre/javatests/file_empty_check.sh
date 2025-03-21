#!/bin/bash

if [ -s "$1" ]; then
  echo "File $1 is not empty:"
  cat "$1"
  exit 1
fi
