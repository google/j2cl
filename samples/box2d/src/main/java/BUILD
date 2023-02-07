load("@com_google_j2cl//build_defs:rules.bzl", "j2cl_library")

licenses(["notice"])

# This package is for internal testing/benchmarks, should not be used outside.
package(default_visibility = ["@com_google_j2cl//:__subpackages__"])

_JAVACOPTS = [
    "-Xep:EqualsHashCode:OFF",  # See go/equals-hashcode-lsc
]

j2cl_library(
    name = "box2d_library-j2cl",
    srcs = glob(
        ["**/*.java"],
        exclude = [
            # Supersourced.
            "**/StrictMath.java",
            "**/PlatformMathUtils.java",
            "**/Timer.java",
        ],
    ) + glob(["**/gwtemul/**/*.java"]),
    javacopts = _JAVACOPTS,
    deps = ["@com_google_j2cl//:jsinterop-annotations-j2cl"],
)

java_library(
    name = "box2d_library",
    srcs = glob(
        ["**/*.java"],
        exclude = ["**/gwtemul/**/*.java"],
    ),
    javacopts = _JAVACOPTS,
    deps = ["@com_google_j2cl//third_party/java/gwt:gwt-jsinterop-annotations"],
)
