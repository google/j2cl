"""Shared JVM flags for J2CL JVM-based workers."""

JVM_FLAGS = [
    "-XX:+TieredCompilation",
    # This should match the javac constant since we compile the same files.
    "-Xss7M",
    # Disable bytecode verification to save from class-loading time.
    "-Xverify:none",
    # Disable UL logging to stdout, which would break the worker protocol.
    "-Xlog:disable",
    "-Xlog:all=warning:stderr",
]
