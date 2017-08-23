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

java_binary(
    name = "J2clMinifier",
    main_class = "com.google.j2cl.tools.minifier.J2clMinifier",
    runtime_deps = [":minifier"],
)
