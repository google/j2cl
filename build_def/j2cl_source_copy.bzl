"""j2cl_source_copy build rule

Copies Java source from one place in the repo to another.


Example use:

j2cl_source_copy(
   name = "my_copy",
   srcs = ["//path/to/files/A.java"],
   base_strip_path = "path/to/",
   excludes = ["foo/bar/Baz.java"],
)

"""


def _impl(ctx):
  java_files = ctx.files.srcs
  excludes = ctx.attr.excludes

  java_out_files = []
  for java_file in java_files:
    out_file_name = java_file.path[len(ctx.attr.base_strip_path):]
    if out_file_name in excludes:
      continue

    java_file_artifact = ctx.new_file(out_file_name)
    java_out_files += [java_file_artifact]
    arguments = [
        java_file.path,
        ctx.configuration.bin_dir.path + "/" + ctx.attr.base_add_path + "/" +
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


_j2cl_source_copy = rule(
    attrs={
        "srcs": attr.label_list(
            mandatory=True,
            allow_files=FileType([".java"]),
        ),
        "excludes": attr.string_list(),
        "base_strip_path": attr.string(
            default="",
        ),
        "base_add_path": attr.string(
            default="",
        ),
    },
    implementation=_impl,
)


def j2cl_source_copy(name, srcs, base_strip_path, excludes=[]):
  _j2cl_source_copy(
      name=name,
      srcs=srcs,
      base_strip_path=base_strip_path,
      base_add_path=PACKAGE_NAME,
      excludes=excludes,
  )
