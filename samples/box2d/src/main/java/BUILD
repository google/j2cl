load("//build_defs:rules.bzl", "j2cl_library", "j2kt_jvm_library", "j2kt_native_library")
load("@rules_java//java:defs.bzl", "java_library")

licenses(["notice"])

# This package is for internal testing/benchmarks, should not be used outside.
package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//:__subpackages__"],
)

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
    deps = [
        "//:jsinterop-annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
    ],
)

j2kt_jvm_library(
    name = "box2d_library-j2kt-jvm",
    srcs = glob(
        ["**/*.java"],
        exclude = ["**/gwtemul/**/*.java"],
    ),
    deps = [
        "//:jsinterop-annotations-j2kt-jvm",
        "//third_party:jspecify_annotations-j2kt-jvm",
    ],
)

j2kt_native_library(
    name = "box2d_library-j2kt-native",
    srcs = glob(
        ["**/*.java"],
        exclude = ["**/gwtemul/**/*.java"],
    ),
    target_compatible_with = ["//third_party/bazel_platforms/os:ios"],
    deps = [
        "//:jsinterop-annotations-j2kt-native",
        "//third_party:jspecify_annotations-j2kt-native",
    ],
)

java_library(
    name = "box2d_library",
    srcs = glob(
        ["**/*.java"],
        exclude = ["**/gwtemul/**/*.java"],
    ),
    javacopts = _JAVACOPTS,
    deps = [
        "//:jsinterop-annotations",
        "//third_party:jspecify_annotations",
    ],
)
