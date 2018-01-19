# Description:
#  A minifier library for J2CL generated source. Applicable in dev servers.

# Apache2
licenses(["notice"])

java_binary(
    name = "J2clMinifier",
    main_class = "com.google.j2cl.tools.minifier.J2clMinifier",
    runtime_deps = [":minifier"],
)

java_library(
    name = "minifier",
    srcs = ["J2clMinifier.java"],
    compatible_with = ["//buildenv/target:appengine"],
    visibility = [
        "//:__pkg__",
        "//tools/javatests/com/google/j2cl/tools/minifier:__pkg__",
    ],
    deps = ["//third_party:guava"],
)
