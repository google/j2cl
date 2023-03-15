load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = [
        "Foo.srcjar",  # Provides Foo.java
        "Srcjar.java",  # References Foo
    ],
    # The command that auto-generates the ObjectiveC file to force build
    # does not handle source jars.
    build_kt_native_readables = False,
)
