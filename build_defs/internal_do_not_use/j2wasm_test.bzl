"""j2wasm_test build macro
Works similarly to junit_test; see j2cl_test_common.bzl for details
"""

load(":j2cl_test_common.bzl", "j2cl_test_common")

# buildifier: disable=function-docstring-args
def j2wasm_test(
        name,
        data = [],
        tags = [],
        optimize = False,
        **kwargs):
    """Macro for running a JUnit test cross compiled as a web test.

    Args:
        optimize: Flag indicating if wasm compilation is optimized or not.
    """

    j2cl_test_common(
        name = name,
        data = data,
        compile = 0,
        enable_rta = False,
        platform = "WASM",
        optimize_wasm = optimize,
        default_browser = Label("//build_defs/internal_do_not_use/browser:chrome-wasm-linux"),
        tags = tags + ["j2wasm"],
        **kwargs
    )
