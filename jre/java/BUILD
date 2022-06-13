# Description:
#   j2cl/jre: JRE subset for transpilation.
#

load("//build_defs:rules.bzl", "j2cl_import", "j2cl_library", "j2kt_jvm_library", "j2kt_native_library", "j2wasm_library")
load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load(":j2cl_java_toolchain.bzl", "j2cl_java_toolchain")
load(":package_sources.bzl", "package_sources")

package(licenses = ["notice"])

# Packages that may use j2kt_jvm_library/j2kt_native_library and need visibility
# for the J2KT JRE targets.
ALLOWED_J2KT_PACKAGES = [
    "//third_party/java_src/jsinterop_annotations/java/jsinterop/annotations:__pkg__",
    "//transpiler/javatests:__subpackages__",
    "//benchmarking:__subpackages__",
    "//third_party/java_src/xplat/kmpbench:__subpackages__",
]

JRE_JAVAC_VERSION_OPTS = [
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
    # Remove warnings that are not relevant to JRE since it is actually a JRE implementaiton.
    "-Xep:ShouldNotSubclass:OFF",
    # Enable Java 11 for the JRE sources
    "-source",
    "11",
    "-target",
    "11",
]

JRE_JAVAC_OPTS = JRE_JAVAC_VERSION_OPTS + [
    # Avoid log site injection which introduces calls to unsupported APIs
    "-XDinjectLogSites=false",
]

java_library(
    name = "javaemul_internal_annotations",
    srcs = glob(["javaemul/internal/annotations/*.java"]),
    visibility = [
        "//jre/javatests:__subpackages__",
        "@com_google_jsinterop_base//:__subpackages__",
    ],
)

j2cl_import(
    name = "javaemul_internal_annotations-j2cl",
    jar = ":javaemul_internal_annotations",
    visibility = [
        "//jre/javatests:__subpackages__",
        "@com_google_jsinterop_base//:__subpackages__",
        "//transpiler/javatests:__subpackages__",
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
        "java/util/InternalStringMap.java",  # J2WASM HashMap doesn't need it.
        "java/util/AbstractHashMap.java",  # J2WASM HashMap doesn't need it.
        "javaemul/internal/Casts.java",  # J2WASM doesn't need it.
    ],
    super_srcs = glob(["super-wasm/**/*.java"]),
)

# Note that j2kt uses different sources for bootclasspath (shared between j2kt
# native and j2kt jvm) and runtime jre (native using a mix of transpiled and
# hand-written Kotlin JRE emulation code, jvm omitting most JRE emulation code
# and falling back on the Java JRE).
package_sources(
    name = "bootclasspath_java_files-j2kt",
    srcs = BASE_JRE_SRCS,  # TODO(b/222484176): Stop including J2CL JRE for JS.
    excludes = [
        # J2KT doesn't need these/they don't work with the super-kt supersourced types.
        "java/lang/JsException.java",
        "java/lang/IntegralToString.java",
        "java/lang/RealToString.java",
        "javaemul/internal/Casts.java",
    ],
    # TODO(b/222484176): Expand to all of super-kt once we're not including J2CL
    #     JRE sources anymore. The excluded files in super-kt are incompatible
    #     with J2CL JRE sources and were accidentally submitted before the move
    #     from J2CL JRE to J2KT
    super_srcs = glob(
        ["super-kt/java/**/*.java"],
    ) + [
        "super-kt/javaemul/internal/Exceptions.java",
        "super-kt/javaemul/internal/Primitives.java",
    ],
)

package_sources(
    name = "jre_java_files-j2kt-native",
    # TODO(b/222484176): Eventually, J2KT JRE should stop using J2CL JRE-for-JS sources.
    srcs = glob(
        [
            "java/lang/*.java",
            "java/lang/annotations/*.java",
        ],
        exclude = [
            # Package-private helper that's not necessary for J2KT
            "java/lang/IntegralToString.java",
            # Does not apply to J2KT
            "java/lang/JsException.java",
            # TODO(b/228304843): Blocked by lack of InternalPreconditions
            # Also relies on String concat optimizations only implemented in JS VMs.
            "java/lang/AbstractStringBuilder.java",
            # Needs AbstractStringBuilder. Excessive String concat. Also http://b/224969395.
            "java/lang/StringBuffer.java",
            # TODO(b/223774683): Blocked by lack of java.io.Serializable plan
            "java/lang/StackTraceElement.java",
            # TODO(b/224765929): System needs emulation different from J2CL
            "java/lang/System.java",
            # TODO(b/227554489): java.util.function.* not available yet
            "java/lang/ThreadLocal.java",
        ],
    ),
    super_srcs = glob(
        ["super-kt/java/**/*.java"],
    ),
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

filegroup(
    name = "j2kt_support_files-j2kt",
    srcs = glob(
        [
            "super-kt/javaemul/**/*.kt",
            "super-kt/javaemul/**/*.java",
        ],
    ),
)

filegroup(
    name = "jre_native_files-j2kt-native",
    srcs = glob([
        "super-kt/java/**/*.kt",
        "super-kt/kotlin/jvm/**/*.kt",
    ]),
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

j2kt_jvm_library(
    name = "jre-j2kt-jvm",
    srcs = [
        ":j2kt_support_files-j2kt",
    ],
    javacopts = JRE_JAVAC_OPTS,
    visibility = ALLOWED_J2KT_PACKAGES,
)

j2kt_native_library(
    name = "jre-j2kt-native",
    srcs = [
        ":j2kt_support_files-j2kt",
        ":jre_java_files-j2kt-native",
        ":jre_native_files-j2kt-native",
    ],
    javacopts = JRE_JAVAC_OPTS,
    visibility = ALLOWED_J2KT_PACKAGES,
    deps = [
        ":javaemul_internal_annotations-j2kt-native",
        "//third_party:gwt-jsinterop-annotations-j2kt-native",
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
    srcs = [":bootclasspath_java_files-j2kt"],
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
