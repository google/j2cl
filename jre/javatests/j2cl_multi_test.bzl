"""j2cl_multi_test build rule

Creates a j2cl_test target for compiled and uncompiled mode.


Example use:

j2cl_multi_test(
    name = "my_test",
    test_class = ["MyJavaTestFile.java"],
)

"""

load("@rules_java//java:defs.bzl", "java_test")
load("//build_defs:rules.bzl", "j2cl_test", "j2kt_native_test", "j2wasm_test")

def j2cl_multi_test(name, test_class, deps, enable_jvm = True, enable_j2kt_native = True, enable_wasm = True, **kwargs):
    tests = [name + "-j2cl", name + "-j2cl_compiled"]

    j2cl_deps = [dep + "-j2cl" for dep in deps]
    j2cl_test(
        name = name + "-j2cl",
        test_class = test_class,
        generate_build_test = False,
        runtime_deps = j2cl_deps,
        browsers = [
            "//build_defs/internal_do_not_use/browser:chrome-wasm-linux",
        ],
        **kwargs
    )
    j2cl_test(
        name = name + "-j2cl_compiled",
        test_class = test_class,
        compile = 1,
        generate_build_test = False,
        runtime_deps = j2cl_deps,
        browsers = [
            "//build_defs/internal_do_not_use/browser:chrome-wasm-linux",
        ],
        browser_overrides = {
        },
        **kwargs
    )

    if enable_jvm:
        tests.append(name + "-jvm")
        java_test(
            name = name + "-jvm",
            test_class = test_class,
            runtime_deps = deps,
            **kwargs
        )

    if enable_wasm:
        tests += [name + "-j2wasm", name + "-j2wasm_optimized"]
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
            ],
            **kwargs
        )

    native.test_suite(name = name, tests = tests, tags = ["manual"] + kwargs.get("tags", []))
