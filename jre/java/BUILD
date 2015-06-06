# Description:
#   j2cl/jre: JRE subset for transpilation.
#
# Can depend on the native bootstrap JS but not on the VM opcode JS.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

# In the future this will be a combination of js_library and j2cl_java_library
# rules since the JRE will be a mixture of hand rolled Java and JS source.
js_library(
    name = "jre",
    srcs = glob(["**/*.js"]),
    deps = ["//transpiler:nativebootstrap"],
)
