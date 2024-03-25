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
    # TODO(b/267597247): Inconsistent @ObjCName annotations in overrides
    build_kt_native_readables = False,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)
