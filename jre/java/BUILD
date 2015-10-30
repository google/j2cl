# Description:
#   j2cl/jre: JRE subset for transpilation.
#
# Can depend on the native bootstrap JS but not on the VM opcode JS.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("/third_party/java_src/j2cl/build_def/j2cl_java_library", "j2cl_java_library")
load("/third_party/java_src/j2cl/build_def/j2cl_source_copy", "j2cl_source_copy")
load("/third_party/java_src/j2cl/build_def/jsni_to_j2cl_converter", "jsni_to_j2cl_converter")

j2cl_source_copy(
    name = "emul_dev",
    srcs = ["//third_party/java_src/gwt/svn/trunk/dev:emul"],
    base_strip_path = "third_party/java_src/gwt/svn/trunk/dev/core/super/",
)

j2cl_source_copy(
    name = "emul_user",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:emul"],
    base_strip_path = "third_party/java_src/gwt/svn/trunk/user/super/com/google/gwt/emul/",
    # Java classes that are different in j2cl and thus should not be copied from GWT
    excludes = [
        "java/lang/Class.java",
        "java/lang/Enum.java",
        "java/lang/Object.java",
        # TODO(dankurka): once JsInterop lands in GWT this class can be changed
        # to work with both GWT and j2cl
        "java/util/InternalJsIterator.java",
        "java/util/InternalJsIteratorEntry.java",
        "java/util/InternalJsMap.java",
        "java/util/InternalJsMapFactory.java",
    ],
)

JRE_SRC = [
    "java/lang/Class.java",
    "java/lang/Enum.java",
    "java/lang/Object.java",
    "java/util/InternalJsMapFactory.java",
    "java/nio/charset/CharsetEncoder.java",
    "java/nio/charset/CharsetDecoder.java",
    "javaemul/internal/JsDate.java",
    "java/lang/invoke/MethodHandles.java",
    ":emul_dev",
    ":emul_user",
]

# TODO(dankurka): this target should be the java_lib target we hand to JDT for resolving compilation
java_library(
    name = "emul",
    srcs = JRE_SRC,
)

jsni_to_j2cl_converter(
    name = "convert_jsni_emul_j2cl",
    srcs = [
        "java/lang/Boolean.java",
        "java/lang/Double.java",
        "java/lang/Number.java",
        "java/lang/String.java",
        "java/util/InternalJsMapFactory.java",
    ],
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

jsni_to_j2cl_converter(
    name = "convert_jsni_emul",
    srcs = [
        "java/lang/Enum.java",
        "java/lang/Object.java",
        "java/util/InternalJsIterator.java",
        "java/util/InternalJsIteratorEntry.java",
        "java/util/InternalJsMap.java",
        "java/util/InternalJsMapFactory.java",
        "javaemul/internal/JsDate.java",
        ":emul_dev",
        ":emul_user",
    ],
    # Files containing JSNI field / method references.
    # These would be rejected by the jsni converter.
    excludes = [
        "javaemul/internal/ArrayStamper.java",
        "javaemul/internal/HashCodes.java",
        "javaemul/internal/LongCompareHolder.java",
        "java/lang/Throwable.java",
        "javaemul/internal/JsDate.java",
    ],
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

genzip(
    name = "native_js.zip",
    deps = [":native_js_pkg_library"],
)

pkg_library(
    name = "native_js_pkg_library",
    srcs = [
        "java/lang/Throwable.native.js",
        "javaemul/internal/ArrayStamper.native.js",
        "javaemul/internal/HashCodes.native.js",
        "javaemul/internal/LongCompareHolder.native.js",
    ],
    flatten = 0,
)

# Transpiles the parts of the jre that are implemented as Java.
j2cl_java_library(
    name = "JavaJre",
    srcs = [
        # TODO(dankurka): investigate all classes here
        "java/lang/Class.java",
        "java/lang/Enum.java",
        "java/lang/Object.java",
        "java/lang/invoke/MethodHandles.java",
        "java/nio/charset/CharsetDecoder.java",
        "java/nio/charset/CharsetEncoder.java",
        "java/util/InternalJsIterator.java",
        "java/util/InternalJsIteratorEntry.java",
        "java/util/InternalJsMap.java",
        "java/util/InternalJsMapFactory.java",
        "javaemul/internal/JsDate.java",
        ":emul_dev",
        ":emul_user",
    ],
    add_jre_dep = False,
    native_sources_zips = [
        ":convert_jsni_emul",
        ":native_js.zip",
    ],
    super_srcs = [
        "java/lang/Class.java",
        "java/lang/Object.java",
        "javaemul/internal/JsDate.java",
    ],
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

# Bundles together both hand-rolled JS versions of Java classes with transpiled
# versions of Java classes into a complete JRE.
js_library(
    name = "jre",
    srcs = glob(
        ["**/*.js"],
        exclude = ["**/*.native.js"],
    ),
    deps = [
        "//transpiler:nativebootstrap",
        ":JavaJre_js_library",
    ],
)
