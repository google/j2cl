licenses(["notice"])  # Apache 2.0

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    srcs = [
        "Foo.srcjar",  # Provides Foo.java
        "Srcjar.java",  # References Foo
    ],
)
