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
    # TODO(b/202513859): Add support for experimental_optimize_autovalue in J2KT.
    generate_kt_readables = False,
    javacopts = ["-Acom.google.auto.value.OmitIdentifiers"],
    optimize_autovalue = True,
    deps = [
        "//third_party:auto_value-j2cl",
        "//third_party:jsr305_annotations-j2cl",
    ],
)
