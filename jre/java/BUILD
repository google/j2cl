# Description:
#   j2cl/jre: JRE subset for transpilation.
#
# Can depend on the native bootstrap JS but not on the VM opcode JS.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("/third_party/java_src/j2cl/build_def/j2cl_java_library", "j2cl_java_library")

# Transpiles the parts of the jre that are implemented as Java.
j2cl_java_library(
    name = "JavaJre",
    srcs = glob(["**/*.java"]),
    add_jre_dep = False,
    super_srcs = [
        "java/lang/Boolean.java",
        "java/lang/Class.java",
        "java/lang/Double.java",
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
