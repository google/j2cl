#!/bin/bash
set -eu

content=$(< "$1")
[[ "$content" == *'@J2clTestInput(example.Outer$Inner.class)'* ]]
