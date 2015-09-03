# Description:
#   j2cl/jre: JRE subset for transpilation.
#
# Can depend on the native bootstrap JS but not on the VM opcode JS.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("/third_party/java_src/j2cl/build_def/j2cl_java_library", "j2cl_java_library")
load("/third_party/java_src/j2cl/build_def/j2cl_transpile_zip", "j2cl_transpile_zip")

# Transpiles the parts of the jre that are implemented as Java.
j2cl_java_library(
    name = "JavaJre",
    srcs = glob(["**/*.java"]),
    add_jre_dep = False,
    super_srcs = [
        "java/lang/Boolean.java",
        "java/lang/Class.java",
        "java/lang/Double.java",
        "java/lang/Number.java",
        "java/lang/Object.java",
        "java/lang/String.java",
    ],
)

# Bundles together both hand-rolled JS versions of Java classes with transpiled
# versions of Java classes into a complete JRE.
js_library(
    name = "jre",
    srcs = glob(["**/*.js"]),
    deps = [
        "//transpiler:nativebootstrap",
        ":JavaJre_js_library",
    ],
)

# TODO: this is example usage of j2cl_transpile_zip, remove when blaze supports
# zips in js_library rules and j2cl_transpile is replaced with
# j2cl_transpile_zip.
j2cl_transpile_zip(
    name = "JavaJreBundle",
    srcs = glob(["**/*.java"]),
    java_library = ":JavaJre",
)

load("/third_party/java_src/j2cl/build_def/j2cl_source_copy", "j2cl_source_copy")

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
        "java/lang/String.java",
        # TODO(dankurka): once JsInterop lands in GWT this class can be changed
        # to work with both GWT and j2cl
        "java/util/InternalJsMapFactory.java",
    ],
)

# TODO(dankurka): this target should be the java_lib target we hand to JDT for resolving compilation
java_library(
    name = "emul",
    srcs = [
        "java/util/InternalJsMapFactory.java",
        "javaemul/internal/JsDate.java",
        ":emul_dev",
        ":emul_user",
    ],
)

load("/third_party/java_src/j2cl/build_def/j2cl_tool", "jsni_to_j2cl_converter")

jsni_to_j2cl_converter(
    name = "convert_jsni_emul",
    srcs = [
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
    ],
)
