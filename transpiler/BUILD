# Description:
#   j2cl/transpiler: Java to Closure style JS transpiler.

package(default_visibility = [
    "//third_party/java/j2cl:__pkg__",
    "//:__subpackages__",
])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "transpiler",
    runtime_deps = ["//transpiler/java/com/google/j2cl/transpiler"],
)

# Re-expose the nativebootstrap JS library at the top level.
js_library(
    name = "nativebootstrap",
    deps = ["//transpiler/nativebootstrap"],
)

js_library(
    name = "vmbootstrap",
    deps = ["//transpiler/vmbootstrap"],
)
