"""jsni_to_j2cl_converter.

This build extension defines a new rule jsni_to_j2cl_converter. This rule is take java files with
old GWT JSNI methods and generate j2cl native js files from that.

Here is an example use of jsni_to_j2cl_converter:

jsni_to_j2cl_converter(
    name = "jsniconverter",
    srcs = glob(["*.java"]),
)

"""

def _should_be_included(java_file, excludes):
  for exclude in excludes:
    if java_file.path.endswith(exclude):
      return False
  return True;

def _impl(ctx):
  java_files = [f for f in ctx.files.srcs if _should_be_included(f, ctx.attr.excludes)]
  dep_targets = ctx.attr.deps
  zip_file = ctx.new_file(ctx.label.name + "_native_js.zip") # output zip file

  if ctx.attr.debug:
    print("java files: " + str(java_files))
    print("dep_targets: " + str(dep_targets))
    print("zip result file: " + str(zip_file))

  converter_args = ["--output_file", zip_file.path]
  if ctx.attr.debug:
    converter_args += ["--verbose"]
  dep_files = set()
  for dep_target in dep_targets:
    dep_files += dep_target.files
    dep_files += dep_target.default_runfiles.files # for exported libraries
  for dep_file in dep_files:
    converter_args += ["--class_path", dep_file.path]
  converter_args += [f.path for f in java_files]

  ctx.action(
    inputs=java_files + list(dep_files),
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
    "deps": attr.label_list(
        allow_files=FileType([".jar"]),
    ),
    "excludes": attr.string_list(default=[]),
    "converter_tool": attr.label(
      cfg=HOST_CFG,
      allow_files=True,
      executable=True,
      default=Label("//tools:jsni2js")
    )
  },
  implementation=_impl,
)
