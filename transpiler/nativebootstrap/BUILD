# Description:
#   j2cl/bootstrap: The lowest layer of JS utility functions.
#
# Should never depend on the JRE JS or on the VM opcode JS.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

js_library(
    name = "nativebootstrap",
    srcs = glob(["**/*.js"]),
    deps = [
        "//javascript/closure/math:long",
        "//javascript/closure:base",
    ],
)
