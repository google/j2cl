# Usage:
# awk -f deobfuscate_wasm_log.awk <wat_file|symbol_map_file> <log_file>

# Process the log file which is the 2nd argument.
FILENAME == ARGV[2] {
  match($0, /<anonymous>:wasm-function\[([0-9]+)\]/, groups)
  print $0, functions[groups[1]]
}

# Collects function declarations from a wat file
/^\(func/ {
  functions[last_function++] =  $2
}

# Collects function declarations from a wasm-as symbol map
/^[0-9]+:/ {
  match($0,/([0-9]+):(.*)/, m)
  functions[m[1]] =  m[2]
}
