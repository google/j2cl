load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = glob(["*.java"]),
    # The native method (for jsinterop) would break the kt native (but not jvm/closure) build
    build_kt_native_readables = False,
    generate_library_info = True,
    deps = [
        "//third_party:jsinterop-annotations-j2cl",
    ],
)
