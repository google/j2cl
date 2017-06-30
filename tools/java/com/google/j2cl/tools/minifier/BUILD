# Description:
#  A minifier library for J2CL generated source. Applicable in dev servers.

package(default_visibility = [
    "//third_party/java/j2cl:__pkg__",
    "//:__subpackages__",
])

# Apache2
licenses(["notice"])

java_library(
    name = "minifier",
    srcs = ["J2clMinifier.java"],
    compatible_with = ["//buildenv/target:appengine"],
    deps = ["//third_party:guava"],
)
