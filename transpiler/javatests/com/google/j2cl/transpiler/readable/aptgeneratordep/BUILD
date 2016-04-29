package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "aptgeneratordep",
    srcs = ["Foo.java"],
    deps = [":dummy-processor-export-j2cl"],
)

# Wrap up the APT processor as a library.
java_library(
    name = "dummy-processor-lib",
    srcs = ["DummyProcessor.java"],
    deps = ["//third_party/java/jsr305_annotations"],
)

# Provide a java_plugin interface for that APT processor library.
java_plugin(
    name = "dummy-processor",
    processor_class = "com.google.j2cl.transpiler.readable.aptgeneratordep.DummyProcessor",
    deps = [":dummy-processor-lib"],
)

# Re-export it via a java_library target so that users can just dep on it
# instead of using a java_plugin attribute.
java_library(
    name = "dummy-processor-export",
    exported_plugins = [":dummy-processor"],
    exports = [":dummy-processor-lib"],
)
