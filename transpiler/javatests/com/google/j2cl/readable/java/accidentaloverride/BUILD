load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(licenses = ["notice"])

readable_example(
    srcs = glob(["*.java"]),
    # TODO(b/267597247):  Inconsistent @ObjCName annotation in a subclass
    build_kt_native_readables = False,
)
