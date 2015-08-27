"""jsni_to_j2cl_converter.

This build extension defines a new rule jsni_to_j2cl_converter. This rule is take java files with
old GWT JSNI methods and generate j2cl native js files from that.

Here is an example use of jsni_to_j2cl_converter:

jsni_to_j2cl_converter(
    name = "jsniconverter",
    srcs = glob(["*.java"]),
)

"""

def _impl(ctx):
  java_files = ctx.files.srcs  # java files that need to be converted
  zip_file = ctx.new_file(ctx.label.name + "_native_js.zip") # output zip file

  if ctx.attr.debug:
    print("java files: " + str(java_files))
    print("zip result file: " + str(zip_file))

  converter_args = ["--output_file", zip_file.path]
  if ctx.attr.debug:
    converter_args += ["--verbose"]
  converter_args += [f.path for f in java_files]

  ctx.action(
    inputs=java_files,
    outputs=[zip_file],
    executable=ctx.executable.converter_tool,
    progress_message="Converting jsni methods into native js files",
    arguments=converter_args,
  )

  return struct(
    files=set([zip_file]),
  )


jsni_to_j2cl_converter = rule(
  attrs={
    "srcs": attr.label_list(
      mandatory=True,
      allow_files=FileType([".java"]),
    ),
    "debug": attr.bool(default=False),
    "converter_tool": attr.label(
      cfg=HOST_CFG,
      allow_files=True,
      executable=True,
      default=Label("//tools:jsni2js")
    )
  },
  implementation=_impl,
)
