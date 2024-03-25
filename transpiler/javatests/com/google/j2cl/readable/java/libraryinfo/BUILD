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
    # TODO(b/204793387): Figure out LibraryInfo for J2KT.
    generate_kt_readables = False,
    generate_library_info = True,
    deps = [
        "//third_party:jsinterop-annotations-j2cl",
    ],
)
