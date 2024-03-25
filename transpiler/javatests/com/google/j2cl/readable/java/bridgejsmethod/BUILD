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
    # Java code contains infinite bound type parameter `Parent<T extends Parent<?>>`. This is not
    # allowed in Kotlin and the generated code does not compile.
    # TODO(b/311430445): enable j2kt-jvm build if `Parent<T extends Parent<?>>` is transpiled to
    # something valid in Kotlin.
    build_kt_readables = False,
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)
