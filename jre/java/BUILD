# Description:
#   j2cl/jre: JRE subset for transpilation.
#

load("//build_defs:rules.bzl", "j2cl_library", "j2wasm_library")
load("@bazel_tools//tools/jdk:default_java_toolchain.bzl", "default_java_toolchain")
load(":package_sources.bzl", "package_sources")
load(":jdk_system.bzl", "jdk_system")

package(
    default_visibility = [
        "//:__pkg__",
        "//jre/javatests:__pkg__",
        "//transpiler/javatests:__subpackages__",
    ],
    licenses = ["notice"],  # Apache 2.0
)

config_setting(
    name = "experimental_java11_support",
    flag_values = {
        "//:enable_experimental_java11_support": "True",
    },
)

JRE_JAVAC_OPTS = select({
    ":experimental_java11_support": [
        # Remove the system (aka pre Java9 'bootclasspath').
        # We need this to compile JRE otherwise it will result in a split package for JRE sources.
        "--system",
        "none",
        # Allow JRE classes are allowed to depend on the jsinterop annotations
        "--add-reads",
        "java.base=ALL-UNNAMED",
        # Enable Java 11 for the JRE sources
        "-source",
        "11",
        "-target",
        "11",
    ],
    "//conditions:default": [
        "-source",
        "8",
        "-target",
        "8",
    ],
})

package_sources(
    name = "jre_java_files",
    srcs = [
        "//third_party:gwt_java_emul",
        "//third_party:gwt_java_emul_internal",
    ],
    super_srcs = select({
        ":experimental_java11_support": glob(["**/*.java"]),
        "//conditions:default": glob(
            ["**/*.java"],
            exclude = ["module-info.java"],
        ),
    }),
)

filegroup(
    name = "jre_native_files",
    srcs = glob(["**/*.js"]) + [
        "//jre/java/javaemul/internal/vmbootstrap/primitives",
    ],
)

j2wasm_library(
    name = "jre-j2wasm",
    srcs = [
        ":jre_java_files",
    ],
    javacopts = JRE_JAVAC_OPTS,
    deps = [
        "//third_party:gwt-javaemul-internal-annotations-j2wasm",
        "//third_party:gwt-jsinterop-annotations-j2wasm",
    ],
)

j2cl_library(
    name = "jre",
    srcs = [
        ":jre_java_files",
        ":jre_native_files",
    ],
    javacopts = JRE_JAVAC_OPTS,
    deps = [
        "//third_party:closure_math_long",
        "//third_party:closure_reflect",
        "//third_party:gwt-javaemul-internal-annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

# Note that this target is separately compiled as a java_library to avoid circular
# dependency.
java_library(
    name = "jre_bootclasspath",
    srcs = [":jre_java_files"],
    javacopts = JRE_JAVAC_OPTS,
    deps = [
        "//third_party:gwt-javaemul-internal-annotations",
        "//third_party:gwt-jsinterop-annotations",
    ],
)

jdk_system(
    name = "jdk_system",
    bootclasspath = ":jre_bootclasspath",
)

JAVA_VERSION = select({
    ":experimental_java11_support": "11",
    "//conditions:default": "8",
})

default_java_toolchain(
    name = "j2cl_java_toolchain",
    bootclasspath = [":jdk_system"],
    source_version = JAVA_VERSION,
    target_version = JAVA_VERSION,
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
)
