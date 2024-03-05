"""j2cl_multi_test build rule

Creates a j2cl_test target for compiled and uncompiled mode.


Example use:

j2cl_multi_test(
    name = "my_test",
    test_class = ["MyJavaTestFile.java"],
)

"""

load("//build_defs:rules.bzl", "j2cl_test", "j2kt_jvm_test", "j2kt_native_test", "j2wasm_test")

def j2cl_multi_test(name, test_class, deps, enable_kt_jvm = False, enable_kt_native = True, enable_wasm = True, **kwargs):
    j2cl_test(
        name = name,
        test_class = test_class,
        generate_build_test = False,
        runtime_deps = deps,
        **kwargs
    )
    j2cl_test(
        name = name + "_compiled",
        test_class = test_class,
        compile = 1,
        generate_build_test = False,
        runtime_deps = deps,
        browsers = [
            "//testing/web/browsers:chrome-linux",
            "//jre/javatests:firefox-linux",
            "//testing/web/browsers:safari-macos",
        ],
        **kwargs
    )

    if enable_kt_jvm:
        j2kt_jvm_deps = [dep + "-j2kt-jvm" for dep in deps]
        j2kt_jvm_test(
            name = name + "-j2kt-jvm",
            test_class = test_class,
            runtime_deps = j2kt_jvm_deps,
            **kwargs
        )

    if enable_kt_native:
        j2kt_native_deps = [dep + "-j2kt-native" for dep in deps]
        j2kt_native_test(
            name = name + "-j2kt-native",
            test_class = test_class,
            runtime_deps = j2kt_native_deps,
            **kwargs
        )

    if enable_wasm:
        j2wasm_deps = [dep + "-j2wasm" for dep in deps]
        j2wasm_defines = {"jre.checks.checkLevel": "NORMAL"}
        j2wasm_test(
            name = name + "-j2wasm",
            test_class = test_class,
            runtime_deps = j2wasm_deps,
            wasm_defs = j2wasm_defines,
            **kwargs
        )
        j2wasm_test(
            name = name + "-j2wasm_optimized",
            test_class = test_class,
            runtime_deps = j2wasm_deps,
            optimize = 1,
            wasm_defs = j2wasm_defines,
            browsers = [
                "//build_defs/internal_do_not_use/browser:chrome-wasm-linux",
                "//build_defs/internal_do_not_use/browser:chrome-wasm-dev-linux",
            ],
            **kwargs
        )
