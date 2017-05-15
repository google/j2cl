"""gwt_incompatible_stripper build rule.

Looks for java source with the @GwtIncompatible annotation and removes the marked portions before
the source is processed by J2CL or Javac.

TODO(tdeegan): This tool should be made a worker for optimal performance.

"""

def _impl(ctx):
  separator = ctx.configuration.host_path_separator
  sources = ctx.files.srcs  # java files that need to be compiled

  # gather transitive files and exported files in deps
  dep_files = set()
  for dep in ctx.attr.deps:
    dep_files += dep.files
    dep_files += dep.default_runfiles.files  # for exported libraries

  # convert files to paths
  deps_paths = []
  for dep_file in dep_files:
    deps_paths += [dep_file.path]

  src_jar_stripped_name = "%s.srcjar" % ctx.label.name
  src_jar_stripped_name_artifact = ctx.new_file(src_jar_stripped_name)

  compiler_args = [
      "-d",
      ctx.configuration.bin_dir.path + "/" + ctx.label.package + "/" +
      src_jar_stripped_name,
  ]

  if deps_paths:
    compiler_args += ["-cp", separator.join(deps_paths)]

  # The stripper expects each java file path as a separate argument.
  compiler_args += [source.path for source in sources]

  ctx.action(
      progress_message = "Stripping @GwtIncompatible",
      inputs= sources + list(dep_files),
      outputs=[src_jar_stripped_name_artifact],
      arguments= compiler_args,
      executable=ctx.executable.strip_tool,
      env=dict(LANG="en_US.UTF-8"),
      mnemonic = "GwtIncompatibleStripper",
  )

  return struct(
      files=set([src_jar_stripped_name_artifact])
  )

gwt_incompatible_stripper = rule(
    implementation=_impl,
    attrs={
        "deps": attr.label_list(allow_files=[".jar"]),
        "srcs": attr.label_list(
            mandatory=True,
            allow_files=[".java", ".srcjar"],
        ),
        "strip_tool": attr.label(
            cfg="host",
            allow_files=True,
            executable=True,
            default=Label("//internal_do_not_use:GwtIncompatibleStripper")
        )
    },
    # Declare each output artifact by name, otherwise they can not be
    # referenced by name when being used as inputs for other rules.
    outputs={
        "jar_file": "%{name}.srcjar",
    }
)
