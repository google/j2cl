"""gwt_incompatible_stripper build rule.

Looks for java source with the @GwtIncompatible annotation and removes the marked portions before
the source is processed by J2CL or Javac.
"""

STRIPPER_BINARY = "//internal_do_not_use:GwtIncompatibleStripper"

def _impl(ctx):
  stripper_args = [
      "-d=" + ctx.outputs.srcjar.path,
  ]
  stripper_args = stripper_args + [f.path for f in ctx.files.srcs]
  stripper_args_file = ctx.new_file(ctx.label.name + "_stripper.args")
  ctx.actions.write(
      output = stripper_args_file,
      content = "\n".join(stripper_args)
  )

  ctx.action(
      progress_message = "Stripping @GwtIncompatible",
      inputs=ctx.files.srcs + [stripper_args_file],
      outputs=[ctx.outputs.srcjar],
      executable=ctx.executable._stripper,
      arguments=["@" + stripper_args_file.path],
      env=dict(LANG="en_US.UTF-8"),
      execution_requirements={"supports-workers": "1"},
      mnemonic = "GwtIncompatibleStripper",
  )

gwt_incompatible_stripper = rule(
    attrs={
        "srcs": attr.label_list(
            mandatory=True,
            allow_files=[".java", ".srcjar", "-src.jar"],
        ),
        "_stripper": attr.label(
            cfg="host",
            executable=True,
            allow_files=True,
            default=Label(STRIPPER_BINARY),
        ),
    },
    implementation=_impl,
    outputs={
        "srcjar": "%{name}.srcjar",
    },
)
