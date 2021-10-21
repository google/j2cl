# Description:
#   j2cl/jre: JRE subset for transpilation.
#

load("//build_defs:rules.bzl", "j2cl_library", "j2wasm_library")
load("@bazel_tools//tools/jdk:default_java_toolchain.bzl", "default_java_toolchain")
load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load(":package_sources.bzl", "package_sources")
load(":jdk_system.bzl", "jdk_system")

package(
    default_visibility = ["//transpiler/javatests:__subpackages__"],
    licenses = ["notice"],  # Apache 2.0
)

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

package_sources(
    name = "jre_java_files",
    srcs = [
        "//third_party:gwt_java_emul",
        "//third_party:gwt_java_emul_internal",
    ],
    excludes = glob(["super-wasm/**"]),
    super_srcs = select({
        ":experimental_java11_support": glob(["**/*.java"]),
        "//conditions:default": glob(
            ["**/*.java"],
            exclude = ["module-info.java"],
        ),
    }),
)

package_sources(
    name = "jre_java_files-j2wasm",
    srcs = [
        "//third_party:gwt_java_emul",
        "//third_party:gwt_java_emul_internal",
    ],
    excludes = [
        "java/util/InternalHashCodeMap.java",  # J2WASM HashMap doesn't need it.
        "java/util/InternalStringMap.java",  # J2WASM HashMap doesn't need it.
        "java/util/AbstractHashMap.java",  # J2WASM HashMap doesn't need it.
        "Casts.java",  # J2WASM doesn't need it.
    ],
    super_srcs = select({
        ":experimental_java11_support": glob(
            ["**/*.java"],
            exclude = glob(["**/super-wasm/*.java"]) + [
                "javaemul/internal/LongUtils.java",  # GWT version actually works for WASM in this case.
            ],
        ),
        "//conditions:default": glob(
            ["**/*.java"],
            exclude = ["module-info.java"],
        ),
    }),
    super_wasm_srcs = glob(["super-wasm/**/*.java"]),
)

filegroup(
    name = "jre_native_files",
    srcs = glob(
        ["**/*.js"],
        exclude = [
            "super-wasm/**/*",
            # TODO(b/202311596): Stop excluding externs.js when infra correctly handles them.
            "**/externs.js",
        ],
    ) + [
        "//jre/java/javaemul/internal/vmbootstrap/primitives",
    ],
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
        "//third_party:gwt-javaemul-internal-annotations-j2wasm",
        "//third_party:gwt-jsinterop-annotations-j2wasm",
    ],
)

closure_js_library(
    name = "j2wasm_js",
    srcs = ["super-wasm/j2wasm.js"],
    # TODO(goktug): Remove after open source closure compiler update.
    lenient = True,
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
        "//transpiler/javatests:__subpackages__",
    ],
    deps = [
        ":jre_externs",
        "//third_party:closure_math_long",
        "//third_party:closure_reflect",
        "//third_party:gwt-javaemul-internal-annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

# Note that these two targets below are separately compiled as a java_library
# to avoid circular dependency.
java_library(
    name = "jre_bootclasspath",
    srcs = [":jre_java_files"],
    javacopts = JRE_JAVAC_OPTS,
    visibility = ["//transpiler/javatests:__subpackages__"],
    deps = [
        "//third_party:gwt-javaemul-internal-annotations",
        "//third_party:gwt-jsinterop-annotations",
    ],
)

java_library(
    name = "jre_bootclasspath-j2wasm",
    srcs = [":jre_java_files-j2wasm"],
    javacopts = JRE_JAVAC_OPTS,
    visibility = ["//transpiler/javatests:__subpackages__"],
    deps = [
        "//third_party:gwt-javaemul-internal-annotations",
        "//third_party:gwt-jsinterop-annotations",
    ],
)

jdk_system(
    name = "jdk_system",
    bootclasspath = ":jre_bootclasspath",
)

jdk_system(
    name = "jdk_system-j2wasm",
    bootclasspath = ":jre_bootclasspath-j2wasm",
)

default_java_toolchain(
    name = "j2cl_java_toolchain_8",
    bootclasspath = [":jdk_system"],
    source_version = "8",
    target_version = "8",
)

default_java_toolchain(
    name = "j2cl_java_toolchain_11",
    bootclasspath = [":jdk_system"],
    source_version = "11",
    target_version = "11",
)

alias(
    name = "j2cl_java_toolchain",
    actual = select({
        ":experimental_java11_support": ":j2cl_java_toolchain_11",
        "//conditions:default": ":j2cl_java_toolchain_8",
    }),
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
)

default_java_toolchain(
    name = "j2wasm_java_toolchain_8",
    bootclasspath = [":jdk_system-j2wasm"],
    source_version = "8",
    target_version = "8",
)

default_java_toolchain(
    name = "j2wasm_java_toolchain_11",
    bootclasspath = [":jdk_system-j2wasm"],
    source_version = "11",
    target_version = "11",
)

alias(
    name = "j2wasm_java_toolchain",
    actual = select({
        ":experimental_java11_support": ":j2wasm_java_toolchain_11",
        "//conditions:default": ":j2wasm_java_toolchain_8",
    }),
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
)
