load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "staticinitfailfast",
    srcs = glob([
        "*.java",
        "*.native.js",
    ]),
    enable_kt = False,
    # staticinitfailfast tests that we throw an error when there is JavaScript
    # code that instantiates a Java class through its synthetic constructor thus
    # skipping the proper Java initialization semantics. Because the class in
    # question is not accessible by JavaScript and is not referenced from the
    # Java code, RTA would actually remove it. Hence we disable RTA for this
    # test only.
    enable_rta = False,
    # JsInterop features are not supported in WASM.
    enable_wasm = False,
    suppress = ["JSC_BAD_PROTECTED_PROPERTY_ACCESS"],
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
