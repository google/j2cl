# Description:
#   j2cl/jre: JRE subset for transpilation.
#

load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load("@rules_java//java:defs.bzl", "java_library")
load("//build_defs:rules.bzl", "j2cl_import", "j2cl_library", "j2wasm_library")
load("//build_defs/internal_do_not_use:j2cl_common.bzl", "DEFAULT_J2CL_JAVAC_OPTS")
load("//build_defs/internal_do_not_use:j2wasm_common.bzl", "DEFAULT_J2WASM_JAVAC_OPTS")
load(":j2cl_java_toolchain.bzl", "j2cl_java_toolchain")
load(":package_sources.bzl", "package_sources")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

JRE_JAVAC_OPTS = [
    # Remove the system (aka pre Java9 'bootclasspath').
    # We need this to compile JRE otherwise it will result in a split package for JRE sources.
    "--system",
    "none",
    # Make sure java.base could be redefined, since we are redeclaring files for java.base.
    "--patch-module",
    "java.base=.",
    # Allow JRE classes are allowed to depend on the jsinterop annotations
    "--add-reads",
    "java.base=ALL-UNNAMED",
    # Remove warnings that are not relevant to JRE since it is actually a JRE implementaiton.
    "-Xep:ShouldNotSubclass:OFF",
    # Remove crv since we don't want to dep on ErrorProne annotations from JRE.
    "-Xep:CheckReturnValue:OFF",
    "-Xep:MixedArrayDimensions:OFF",  # See go/lsc-mixedarraydimensions
]

java_library(
    name = "javaemul_internal_annotations",
    srcs = glob(["javaemul/internal/annotations/*.java"]),
    visibility = [
        "//visibility:public",
    ],
)

j2cl_import(
    name = "javaemul_internal_annotations-j2cl",
    jar = ":javaemul_internal_annotations",
    visibility = [
        "//visibility:public",
    ],
)

BASE_JRE_SRCS = glob(
    ["java*/**/*.java"],
    exclude = ["javaemul/internal/annotations/**"],
) + ["module-info.java"]

package_sources(
    name = "jre_java_files",
    srcs = BASE_JRE_SRCS,
)

package_sources(
    name = "jre_java_files-j2wasm",
    srcs = BASE_JRE_SRCS,
    excludes = [
        "java/util/InternalHashCodeMap.java",  # J2WASM HashMap doesn't need it.
        "java/util/InternalJsMap.java",  # J2WASM HashMap doesn't need it.
        "java/util/InternalStringMap.java",  # J2WASM HashMap doesn't need it.
        "java/util/AbstractHashMap.java",  # J2WASM HashMap doesn't need it.
        "javaemul/internal/Casts.java",  # J2WASM doesn't need it.
        "javaemul/internal/ArrayStamper.java",  # Unused in Wasm
        "javaemul/internal/Comparables.java",  # Unused in Wasm
        "javaemul/internal/Objects.java",  # Unused in Wasm
    ],
    super_srcs = glob(["super-wasm/**/*.java"]) + [
        "//jre/java/super-wasm/javaemul/internal/wasmarray",
    ],
)

filegroup(
    name = "jre_native_files",
    srcs = glob(
        ["java*/**/*.js"],
        exclude = [
            # TODO(b/202311596): Stop excluding externs.js when infra correctly handles them.
            "**/externs.js",
        ],
    ) + [
        "//jre/java/javaemul/internal/vmbootstrap/primitives",
    ],
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler:__pkg__"],
)

closure_js_library(
    name = "jre_externs",
    srcs = glob(
        ["**/externs.js"],
    ),
)

j2wasm_library(
    name = "jre-j2wasm",
    srcs = [
        ":jre_java_files-j2wasm",
    ] + glob(["super-wasm/*.js"]),
    javacopts = JRE_JAVAC_OPTS,
    visibility = [
        "//build_defs/internal_do_not_use:__pkg__",
        "//transpiler/javatests/com/google/j2cl/transpiler:__pkg__",
    ],
    deps = [
        ":javaemul_internal_annotations-j2wasm",
        "//third_party:jsinterop-annotations-j2wasm",
    ],
)

j2cl_library(
    name = "jre",
    srcs = [
        ":jre_java_files",
        ":jre_native_files",
    ],
    javacopts = JRE_JAVAC_OPTS,
    visibility = [
        "//:__pkg__",
        "//transpiler/javatests/com/google/j2cl/transpiler:__pkg__",
    ],
    exports = [
        # J2CL transpiler optimizations to AutoValue may add this dependency.
        "//third_party:closure_reflect",
    ],
    deps = [
        ":javaemul_internal_annotations-j2cl",
        ":jre_externs",
        "//third_party:closure_math_long",
        "//third_party:closure_reflect",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)

# Note that these two targets below are separately compiled as a java_library
# to avoid circular dependency.
java_library(
    name = "jre_bootclasspath",
    srcs = [":jre_java_files"],
    javacopts = JRE_JAVAC_OPTS + DEFAULT_J2CL_JAVAC_OPTS,
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler:__pkg__"],
    deps = [
        ":javaemul_internal_annotations",
        "//third_party:jsinterop-annotations-j2cl_jar",
    ],
)

java_library(
    name = "jre_bootclasspath-j2wasm",
    srcs = [":jre_java_files-j2wasm"],
    javacopts = JRE_JAVAC_OPTS + DEFAULT_J2CL_JAVAC_OPTS + DEFAULT_J2WASM_JAVAC_OPTS,
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler:__pkg__"],
    deps = [
        ":javaemul_internal_annotations",
        "//third_party:jsinterop-annotations-j2cl_jar",
    ],
)

j2cl_java_toolchain(
    name = "j2cl_java_toolchain",
    bootclasspath = ":jre_bootclasspath",
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
)

j2cl_java_toolchain(
    name = "j2wasm_java_toolchain",
    bootclasspath = ":jre_bootclasspath-j2wasm",
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
)
