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
    # TODO(b/418843331): Extract cases with explicit qualifier in constructor
    #  calls to a separate readable test which will be disabled for Kotlin
    #  only, and then enable this test.
    generate_kt_readables = False,
)
