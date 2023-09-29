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
        use_legacy_wasm_spec = True,
        **kwargs):
    """Macro for running a JUnit test cross compiled as a web test.

    Args:
        optimize: Flag indicating if wasm compilation is optimized or not.
    """

    is_optimized_suffix = "" if optimize else "_dev"
    extra_data = [":" + name + "_generated_suite_j2wasm_application" + is_optimized_suffix]

    j2cl_test_common(
        name = name,
        data = data + extra_data,
        compile = 0,
        enable_rta = False,
        platform = "WASM",
        optimize_wasm = optimize,
        use_legacy_wasm_spec = use_legacy_wasm_spec,
        default_browser = Label("//build_defs/internal_do_not_use/browser:chrome-wasm-linux"),
        tags = tags + ["j2wasm"],
        **kwargs
    )
