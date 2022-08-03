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
        # J2KT doesn't need these
        "java/io/HasSerializableTypeMarker.java",
        "java/lang/JsException.java",
        "java/lang/IntegralToString.java",
        "java/lang/RealToString.java",
        "javaemul/internal/Casts.java",
        "java/util/AbstractHashMap.java",
        "java/util/AbstractMapEntry.java",
        "java/util/InternalHashCodeMap.java",
        "java/util/InternalJsMap.java",
        "java/util/InternalStringMap.java",
    ] + glob(
        ["javaemul/internal/ktstubs/*.java"],
    ) + [
        # TODO(b/238299508): Include sources for transpiled collections
        "java/util/AbstractQueue.java",
        "java/util/AbstractSequentialList.java",
        "java/util/EnumMap.java",
        "java/util/EnumSet.java",
        "java/util/IdentityHashMap.java",
        "java/util/LinkedList.java",
        "java/util/NavigableMap.java",
        "java/util/NavigableSet.java",
        "java/util/PriorityQueue.java",
        "java/util/SortedSet.java",
        "java/util/Spliterators.java",
        "java/util/Stack.java",
        "java/util/TreeMap.java",
        "java/util/TreeSet.java",
        "java/util/Vector.java",
        "java/util/concurrent/ConcurrentHashMap.java",
    ] + glob(
        # TODO(b/237650063): Add Streams
        # The Streams implementation uses missing collection APIs (Map.merge)
        ["java/util/stream/*.java"],
    ),
    super_srcs = glob(
        ["super-kt/java*/**/*.java"],
    ) + ["super-kt/module-info.java"],
)

# Files that cannot be included in the JRE runtime yet because they rely on
# features that are missing (other JRE runtime APIs or J2KT transpiler
# features). Exclusions should either have a bug number attached, or point to
# another excluded file that is a dependency.
# When removing a file from this list, remove other files that were only
# excluded because of it. If that's not possible add a new bug link.
J2KT_JRE_TEMPORARILY_EXCLUDED_FILES = [
    "java/math/BigDecimal.java",  # TODO(b/237503140): Make BigDecimal/BigInteger work
    "java/math/BigInteger.java",  # TODO(b/237503140): Make BigDecimal/BigInteger work
    "java/math/BitLevel.java",  # BigInteger
    "java/math/Conversion.java",  # Division, BigInteger
    "java/math/Division.java",  # BitLevel
    "java/math/Elementary.java",  # BigInteger
    "java/math/Logical.java",  # BigInteger
    "java/math/Multiplication.java",  # BigInteger
    "java/math/Primality.java",  # BigInteger, Random
] + [
    # TODO(b/238299508): Include sources for transpiled collections
    "java/util/AbstractQueue.java",
    "java/util/AbstractSequentialList.java",
    "java/util/BitSet.java",  # TODO(b/239684874): Implement BitSet when necessary
    "java/util/Deque.java",
    "java/util/EnumMap.java",
    "java/util/EnumSet.java",
    "java/util/IdentityHashMap.java",
    "java/util/LinkedList.java",
    "java/util/NavigableMap.java",  # TODO(b/237414940): Can be supersourced after Comparator
    "java/util/NavigableSet.java",  # TODO(b/237414940): Can be supersourced after Comparator
    "java/util/PriorityQueue.java",
    "java/util/Queue.java",
    "java/util/SortedMap.java",  # TODO(b/237414940): Can be supersourced after Comparator
    "java/util/SortedSet.java",  # TODO(b/237414940): Can be supersourced after Comparator
    "java/util/Stack.java",
    "java/util/TreeMap.java",
    "java/util/TreeSet.java",
    "java/util/Vector.java",
] + [
    "java/util/logging/Logger.java",  # LogManager
    "java/util/logging/LogManager.java",  # HashMap, List
] + [
    "java/util/concurrent/atomic/AtomicReferenceArray.java",  # java.util.Arrays
    "java/util/concurrent/ConcurrentMap.java",  # java.util.Map
    "java/util/concurrent/ConcurrentHashMap.java",  # java.util.HashMap
    "java/util/concurrent/ExecutorService.java",  # java.util.Collection
    "java/util/concurrent/ScheduledExecutorService.java",  # ExecutorService
] + [
    # TODO(b/237650063): Support Streams/Spliterator,
    "java/util/Spliterators.java",
    "java/util/stream/BaseStream.java",
    "java/util/stream/Collector.java",
    "java/util/stream/Collectors.java",
    "java/util/stream/CollectorImpl.java",
    "java/util/stream/DoubleStream.java",
    "java/util/stream/DoubleStreamImpl.java",
    "java/util/stream/IntStream.java",
    "java/util/stream/IntStreamImpl.java",
    "java/util/stream/LongStream.java",
    "java/util/stream/LongStreamImpl.java",
    "java/util/stream/Stream.java",
    "java/util/stream/StreamImpl.java",
    "java/util/stream/StreamSupport.java",
    "java/util/stream/TerminatableStream.java",
]

package_sources(
    name = "jre_java_files-j2kt-native",
    # TODO(b/222484176): Eventually, J2KT JRE should stop using J2CL JRE-for-JS sources.
    srcs = glob(
        [
            "java/**/*.java",
        ],
        exclude = [
            "java/io/HasSerializableTypeMarker.java",
            "java/lang/IntegralToString.java",  # J2KT String/Integer/Long don't need it.
            "java/lang/JsException.java",  # J2KT doesn't need it
            "java/lang/AbstractStringBuilder.java",  # J2KT StringBuilder doesn't need it
            "java/util/AbstractHashMap.java",  # J2KT doesn't need it
            "java/util/InternalHashCodeMap.java",  # J2KT HashMap doesn't need it.
            "java/util/AbstractMapEntry.java",  # J2KT doesn't need it.
            "java/util/InternalJsMap.java",  # J2KT HashMap doesn't need it.
            "java/util/InternalStringMap.java",  # J2KT HashMap doesn't need it.
            "java/util/logging/SimpleConsoleLogHandler.java",  # J2KT doesn't need it.
            # Excessive String concat that only JS should use. Also http://b/224969395.
            "java/lang/StringBuffer.java",
            "java/util/Comparators.java",  # J2KT Comparator doesn't need it
        ] + glob(["javaemul/internal/ktstubs/*.java"]) + J2KT_JRE_TEMPORARILY_EXCLUDED_FILES,
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
    srcs = ["javaemul/internal/InternalPreconditions.java"] + glob(
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
        "super-kt/kotlin/**/*.kt",
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
