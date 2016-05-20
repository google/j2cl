"""j2cl_optimization_test macro

See BooleansTest for an example usage.

"""

load("/javascript/closure/builddefs", "CLOSURE_COMPILER_FLAGS_FULL_TYPED")
load("/third_party/java/j2cl/j2cl_library", "j2cl_library")
load("/third_party/java/j2cl/j2cl_test", "j2cl_test")

_CLOSURE_COMPILER_FLAGS_FULL_TYPED = [
    flag
    for flag in CLOSURE_COMPILER_FLAGS_FULL_TYPED
    if flag != "--variable_renaming=ALL"
]


def j2cl_optimization_test(name, defs=[]):
  j2cl_test(
    name = name,
    srcs = [name + ".java"],
    compile = 1,
    compiler = "//javascript/tools/jscompiler:head",
    data = ["//testing/matrix/nativebrowsers/chrome:stable_data"],
    defs = _CLOSURE_COMPILER_FLAGS_FULL_TYPED + [
        "--j2cl_pass",
        "--export_test_functions=true",
        "--language_in=ECMASCRIPT6_STRICT",
        "--language_out=ECMASCRIPT5",
        "--property_renaming=OFF",
        "--pretty_print",
        "--norewrite_polyfills",
        "--strict",
        "--variable_renaming=OFF",
    ] + defs,
    deps_mgmt = "closure",
    externs_list = ["//javascript/externs:common"],
    jvm_flags = ["-Dcom.google.testing.selenium.browser=CHROME_LINUX"],
    deps = [
        ":shared",
        "//third_party/java/junit",
        "//jre/java:gwt-jsinterop-annotations",
    ],
)
