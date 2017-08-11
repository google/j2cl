"""j2cl_source_copy build rule

Copies Java source from one place in the repo to another.


Example use:

j2cl_source_copy(
   name = "my_copy",
   srcs = ["//path/to/files/A.java"],
   excludes = ["foo/bar/Baz.java"],
)

"""

def _impl(ctx):
  java_files = ctx.files.srcs
  excludes = ctx.attr.excludes

  java_out_files = []
  for java_file in java_files:
    out_file_name = java_file.path;
    if any([out_file_name.endswith(x) for x in excludes]):
      continue;

    java_file_artifact = ctx.new_file(out_file_name)
    java_out_files += [java_file_artifact]
    arguments = [
        java_file.path,
        ctx.configuration.bin_dir.path + "/" + ctx.label.package + "/" +
        out_file_name,
    ]

    ctx.action(
        inputs=[java_file],
        outputs=[java_file_artifact],
        command="cp $1 $2",
        arguments=arguments,
    )
  # We need to return the output files so that they get recognized as outputs
  # from blaze
  return struct(
      files=set(java_out_files),
  )


j2cl_source_copy = rule(
    attrs={
        "srcs": attr.label_list(mandatory=True, allow_files=[".java"]),
        "excludes": attr.string_list(),
    },
    implementation=_impl,
)
