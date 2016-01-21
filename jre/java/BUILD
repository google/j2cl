# Description:
#   j2cl/jre: JRE subset for transpilation.
#
# Can depend on the native bootstrap JS but not on the VM opcode JS.

package(default_visibility = [
    "//:__subpackages__",
    "//third_party/java/j2cl:__pkg__",
])

# Google owns the copyright
licenses(["unencumbered"])

load("//build_def:j2cl_source_copy.bzl", "j2cl_source_copy")
load("//build_def:jsni_to_j2cl_converter.bzl", "jsni_to_j2cl_converter")

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
        "java/lang/Character.java",
        "java/lang/Class.java",
        "java/lang/Enum.java",
        "java/lang/Math.java",
        "java/lang/Object.java",
        "java/lang/String.java",
        # TODO(dankurka): once JsInterop lands in GWT this class can be changed
        # to work with both GWT and j2cl
        "java/util/InternalJsIterator.java",
        "java/util/InternalJsIteratorEntry.java",
        "java/util/InternalJsMap.java",
        "java/util/InternalJsMapFactory.java",
    ],
)

JRE_SRC = [
    "java/lang/Character.java",
    "java/lang/Class.java",
    "java/lang/Enum.java",
    "java/lang/Math.java",
    "java/lang/Object.java",
    "java/lang/String.java",
    "java/util/InternalJsMapFactory.java",
    "javaemul/internal/JsDate.java",
    "java/lang/invoke/MethodHandles.java",
    ":emul_dev",
    ":emul_user",
]

jsni_to_j2cl_converter(
    name = "convert_jsni_emul",
    srcs = [
        "java/lang/Character.java",
        "java/lang/Enum.java",
        "java/lang/Math.java",
        "java/lang/Object.java",
        "java/lang/String.java",
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
        ":JavaJre",
    ],
)
