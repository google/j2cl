package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

# TODO: remove this hand-rolled verification rule and add transpile
# and verify targets when build automation exists.
subinclude("//javascript/closure/builddefs:BUILD")

js_binary(
    name = "EmptyClassBinary",
    srcs = ["EmptyClass.js"],
    defs = CLOSURE_COMPILER_FLAGS_FULL_TYPED + [
        "--language_in=ECMASCRIPT6",
        "--language_out=ECMASCRIPT5",
        "--jscomp_off=nonStandardJsDocs",
        "--remove_dead_code",
        "--remove_unused_vars",
        "--remove_unused_local_vars=ON",
        "--remove_dead_assignments",
    ],
    externs_list = ["//javascript/externs:common"],
    deps = [
        "//transpiler:nativebootstrap",
        "//jre",
    ],
)
