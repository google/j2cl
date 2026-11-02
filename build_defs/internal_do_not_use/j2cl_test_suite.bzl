"""Hermetic actions for preparing generated J2CL test suites."""

_TOOL = Label("//tools/java/com/google/j2cl/tools/junit:j2cl_test_suite_tool")

def _run_tool(ctx, operation, mnemonic):
    args = ctx.actions.args()
    args.add(operation)
    args.add(ctx.file.src)
    args.add(ctx.outputs.out)
    ctx.actions.run(
        executable = ctx.executable._tool,
        inputs = [ctx.file.src],
        outputs = [ctx.outputs.out],
        arguments = [args],
        tools = [ctx.attr._tool[DefaultInfo].files_to_run],
        mnemonic = mnemonic,
    )
    return [DefaultInfo(files = depset([ctx.outputs.out]))]

def _archive_impl(ctx):
    return _run_tool(ctx, "archive", "J2clTestSuiteArchive")

def _extract_impl(ctx):
    return _run_tool(ctx, "extract", "J2clTestSuiteExtract")

j2cl_test_suite_archive = rule(
    implementation = _archive_impl,
    attrs = {
        "src": attr.label(allow_single_file = True, mandatory = True),
        "out": attr.output(mandatory = True),
        "_tool": attr.label(
            default = _TOOL,
            cfg = "exec",
            executable = True,
        ),
    },
)

j2cl_extract_single_js = rule(
    implementation = _extract_impl,
    attrs = {
        "src": attr.label(allow_single_file = True, mandatory = True),
        "out": attr.output(mandatory = True),
        "_tool": attr.label(
            default = _TOOL,
            cfg = "exec",
            executable = True,
        ),
    },
)
