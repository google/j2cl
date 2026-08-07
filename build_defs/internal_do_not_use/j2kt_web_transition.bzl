"""Defines the user-configuration transition to enable J2KT Web builds."""

visibility([
    "//transpiler/javatests/com/google/j2cl/readable",
    "//transpiler/javatests/com/google/j2cl/integration",
    "//junit/generator/javatests/com/google/j2cl/junit/integration",
])

j2kt_web_transition = transition(
    implementation = lambda *_: {"//:j2kt_web_environment": "experimental"},
    inputs = [],
    outputs = ["//:j2kt_web_environment"],
)

def _j2kt_web_enabled_test_impl(ctx):
    # We need to copy the executable of the original test because starlark doesn't allow
    # providing an executable not created by the rule
    executable_src = ctx.executable.test
    executable_dst = ctx.actions.declare_file(ctx.label.name)
    ctx.actions.run_shell(
        mnemonic = "J2clIntegrationTestCopyExecutable",
        tools = [executable_src],
        outputs = [executable_dst],
        command = "cp %s %s" % (executable_src.path, executable_dst.path),
    )
    runfiles = ctx.attr.test[0][DefaultInfo].default_runfiles
    return [DefaultInfo(runfiles = runfiles, executable = executable_dst)]

_j2kt_web_enabled_test = rule(
    implementation = _j2kt_web_enabled_test_impl,
    attrs = {
        "test": attr.label(cfg = j2kt_web_transition, executable = True),
    },
    test = True,
)

def j2kt_web_enabled_test(name, test, tags = [], **kwargs):
    _j2kt_web_enabled_test(
        name = name,
        test = test,
        # "requires-net:external" set by web_test but not by transition
        tags = tags + [
            "j2cl",
            "j2kt-web",
            "requires-net:external",
            "not_run:arm",  # b/493227783
        ],
        **kwargs
    )
