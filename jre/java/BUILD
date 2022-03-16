# Description:
#   j2cl/jre: JRE subset for transpilation.
#

load("//build_defs:rules.bzl", "j2cl_import", "j2cl_library", "j2wasm_library")
load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load(":j2cl_java_toolchain.bzl", "j2cl_java_toolchain")
load(":package_sources.bzl", "package_sources")

package(licenses = ["notice"])

config_setting(
    name = "experimental_java11_support",
    flag_values = {
        "//:enable_experimental_java11_support": "True",
    },
)

JRE_JAVAC_VERSION_OPTS = select({
    ":experimental_java11_support": [
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
        # Avoid optimized JVM String concat which introduces calls to unsupported APIs.
        "-XDstringConcat=inline",
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

JRE_JAVAC_OPTS = JRE_JAVAC_VERSION_OPTS + [
    # Avoid log site injection which introduces calls to unsupported APIs
    "-XDinjectLogSites=false",
]

java_library(
    name = "javaemul_internal_annotations",
    srcs = glob(["javaemul/internal/annotations/*.java"]),
    visibility = [
        "@com_google_jsinterop_base//:__subpackages__",
    ],
)

j2cl_import(
    name = "javaemul_internal_annotations-j2cl",
    jar = ":javaemul_internal_annotations",
    visibility = [
        "@com_google_jsinterop_base//:__subpackages__",
        "//transpiler/javatests:__subpackages__",
    ],
)

BASE_JRE_SRCS = select({
    ":experimental_java11_support": glob(
        ["java*/**/*.java"],
        exclude = ["javaemul/internal/annotations/**"],
    ) + ["module-info.java"],
    "//conditions:default": glob(
        ["java*/**/*.java"],
        exclude = ["javaemul/internal/annotations/**"],
    ),
})

package_sources(
    name = "jre_java_files",
    srcs = BASE_JRE_SRCS,
)

package_sources(
    name = "jre_java_files-j2wasm",
    srcs = BASE_JRE_SRCS,
    excludes = [
        "java/util/InternalHashCodeMap.java",  # J2WASM HashMap doesn't need it.
        "java/util/InternalStringMap.java",  # J2WASM HashMap doesn't need it.
        "java/util/AbstractHashMap.java",  # J2WASM HashMap doesn't need it.
        "javaemul/internal/Casts.java",  # J2WASM doesn't need it.
    ],
    super_srcs = glob(["super-wasm/**/*.java"]),
)

package_sources(
    name = "jre_java_files-j2kt",
    srcs = BASE_JRE_SRCS,  # TODO(b/222484176): Stop including J2CL JRE for JS.
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
    ],
    javacopts = JRE_JAVAC_OPTS,
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    deps = [
        ":javaemul_internal_annotations-j2wasm",
        "//third_party:gwt-jsinterop-annotations-j2wasm",
    ],
)

closure_js_library(
    name = "j2wasm_js",
    srcs = ["super-wasm/j2wasm.js"],
    visibility = ["//:__pkg__"],
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
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

# Note that these two targets below are separately compiled as a java_library
# to avoid circular dependency.
java_library(
    name = "jre_bootclasspath",
    srcs = [":jre_java_files"],
    javacopts = JRE_JAVAC_OPTS,
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler:__pkg__"],
    deps = [
        ":javaemul_internal_annotations",
        "//third_party:gwt-jsinterop-annotations",
    ],
)

java_library(
    name = "jre_bootclasspath-j2wasm",
    srcs = [":jre_java_files-j2wasm"],
    javacopts = JRE_JAVAC_OPTS,
    deps = [
        ":javaemul_internal_annotations",
        "//third_party:gwt-jsinterop-annotations",
    ],
)

java_library(
    name = "jre_bootclasspath-j2kt",
    srcs = [":jre_java_files-j2kt"],
    javacopts = JRE_JAVAC_OPTS,
    deps = [
        ":javaemul_internal_annotations",
        "//third_party:gwt-jsinterop-annotations",
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

j2cl_java_toolchain(
    name = "j2kt_java_toolchain",
    bootclasspath = ":jre_bootclasspath-j2kt",
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
)
