load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

# Creates verification target
readable_example(
    srcs = glob(["*.java"]),
    # TODO(b/202449098): Add support to import JsType.
    generate_kt_readables = False,
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
