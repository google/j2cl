"""Shared JVM flags for J2CL JVM-based workers."""

JVM_FLAGS = [
    "-XX:+TieredCompilation",
    "-Xss3M",
    # Disable bytecode verification to save from class-loading time.
    "-Xverify:none",
]
