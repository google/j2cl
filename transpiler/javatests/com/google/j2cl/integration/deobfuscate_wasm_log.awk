# Usage:
# awk -f deobfuscate_wasm_log.awk <wat_file> <log_file>

# Process the log file which is the 2nd argument.
FILENAME == ARGV[2] {
  match($0, /<anonymous>:wasm-function\[([0-9]+)\]/, groups)
  print $0, functions[groups[1]]
}

# Collects function declarations
/^\(func/ {
  functions[last_function++] =  $2
}
