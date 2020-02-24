load("//build_defs:rules.bzl", "j2cl_library")

package(
    licenses = ["reciprocal"],  # Eclipse Public License - v 1.0
)

j2cl_library(
    name = "junit_emul",
    srcs = glob(["**/*.java"]),
    javacopts = [
        "-Xep:DoNotCall:OFF",
    ],
    visibility = ["//:__pkg__"],
)
