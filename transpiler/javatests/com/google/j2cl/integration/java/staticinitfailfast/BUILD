load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "staticinitfailfast",
    srcs = glob([
        "*.java",
        "*.native.js",
    ]),
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    enable_kt = False,
    # staticinitfailfast tests that we throw an error when there is JavaScript
    # code that instantiates a Java class through its synthetic constructor thus
    # skipping the proper Java initialization semantics. Because the class in
    # question is not accessible by JavaScript and is not referenced from the
    # Java code, RTA would actually remove it. Hence we disable RTA for this
    # test only.
    enable_rta = False,
    enable_wasm = False,
    suppress = ["JSC_BAD_PROTECTED_PROPERTY_ACCESS"],
)
