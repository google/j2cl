"""j2cl_transpile build rule.

Takes Java source and translates it into Closure style JS in a zip bundle. Java
library deps might be needed for reference resolution.

Example use:

j2cl_transpile(
    name = "my_transpile",
    srcs = ["MyJavaFile.java"],
    deps = [":some_dep"],
)

Note: in general you want to be using j2cl_library instead of using
j2cl_transpile directly.

"""


def _get_message(ctx):
  _MESSAGES = [
      "Re" + "ti" + "cu" + "la" + "ti" + "ng" + "Sp" + "li" + "ne" + "s",
      3 * ("\\" + "0" + "/ "),
      "Co" + "mp" + "ut" + "in" + "g " + "PI",
      "So" + " m" + "uch" + " Ja" + "va",
      "Sp" + "aw" + "ni" + "ng" + " m" + "or" + "e " + "ov" + "er" + "lo" + "rd" + "s",
      "So" + "lv" + "in" + "g " + "ha" + "lt" + "ing" + " p" + "ro" + "bl" + "em",
      "Ex" + "ec" + "ut" + "in" + "g " + "bu" + "sy" + " l" + "oo" + "p",
      "En" + "te" + "ri" + "ng" + " w" + "ar" + "p " + "sp" + "ee" + "d"
  ]
  index = len(ctx.attr.deps) + len(ctx.configuration.bin_dir.path)
  return _MESSAGES[index % len(_MESSAGES)] + " %s" % ctx.label

def _impl(ctx):
  separator = ctx.configuration.host_path_separator
  java_files = ctx.files.srcs  # java files that need to be compiled
  js_native_zip_files = ctx.files.native_srcs_zips
  deps = ctx.attr.deps
  dep_files = depset()
  deps_paths = []
  java_files_paths = []

  # gather transitive files and exported files in deps
  for dep in deps:
    dep_files += dep.files
    dep_files += dep.default_runfiles.files  # for exported libraries

  # convert files to paths
  for dep_file in dep_files:
    deps_paths += [dep_file.path]

  for java_file in java_files:
    java_files_paths += [java_file.path]

  # Intermediate target with a zip file that contains timestamps and is
  # therefore not deterministic.  This file is then sanitized to the final
  # js_zip_artifact.
  nondeterministic_js_zip_artifact = ctx.actions.declare_file(
      "__nondeterministic_%s.js.zip" % ctx.label.name)

  compiler_args = ["-d", nondeterministic_js_zip_artifact.path]

  if deps_paths:
    compiler_args += ["-cp", separator.join(deps_paths)]

  # Add the native zip file paths
  js_native_zip_files_paths = [js_native_zip_file.path for js_native_zip_file
                               in js_native_zip_files]
  if js_native_zip_files_paths:
    joined_paths = separator.join(js_native_zip_files_paths)
    compiler_args += ["-nativesourcepath", joined_paths]

  # Generate readable_maps
  if ctx.attr.readable_source_maps:
    compiler_args += ["-readablesourcemaps"]

  # Emit goog.module.declareLegacyNamespace(). This is a temporary measure
  # while onboarding Docs, do not use.
  if ctx.attr.declare_legacy_namespace:
    compiler_args += ["-declarelegacynamespaces"]

  # The transpiler expects each java file path as a separate argument.
  compiler_args += java_files_paths

  # Create an action to write the flag file
  compiler_args_file = ctx.new_file(ctx.label.name + "_compiler.args")
  ctx.actions.write(
      output = compiler_args_file,
      content = "\n".join(compiler_args)
  )

  inputs = java_files[:]
  inputs += list(dep_files)
  inputs += js_native_zip_files
  inputs += [compiler_args_file]

  # Note: the output of this rule is not detrerministic, which is why we fix it
  # with _sanitize_zip below.  Ideally, the transpiler would output a
  # deterministic file directly.
  ctx.action(
      progress_message = _get_message(ctx),
      inputs=inputs,
      outputs=[nondeterministic_js_zip_artifact],
      executable=ctx.executable.transpiler,
      arguments=["@" + compiler_args_file.path],
      env=dict(LANG="en_US.UTF-8"),
      execution_requirements={"supports-workers": "1" if ctx.attr.supports_workers_internal else "0"},
      mnemonic = "J2clTranspile",
  )

  _sanitize_zip(ctx, nondeterministic_js_zip_artifact, ctx.outputs.zip_file)

  return struct(
      files=depset([ctx.outputs.zip_file])
  )

def _sanitize_zip(
    ctx,
    nondeterministic_zip_file,
    output):
  """Removes timestamps from files and directories in the given ZIP archive.

  Args:
    ctx: Rule context
    nondeterministic_zip_file: The File to sanitize
    output: Target File
  """
  ctx.actions.run_shell(
      inputs = [
          nondeterministic_zip_file,
          ctx.executable._zip,
      ],
      outputs = [output],
      command = "\n".join([
          "TMPDIR=$(mktemp -d)",
          "unzip -q %s -d $TMPDIR" % nondeterministic_zip_file.path,
          "cwd=$PWD",
          "cd $TMPDIR",
          # Ensure the directory is nonempty, or zip errors out.
          "mkdir -p __dummy__",
          "$cwd/%s -jt -X -qr $cwd/%s ." % (
              ctx.executable._zip.path, output.path)
      ])
  )


"""j2cl_transpile: A J2CL transpile rule.

Args:
  srcs: Source files (.java or .srcjar) to compile.
  deps: Java jar files for reference resolution.
  native_srcs_zips: JS zip files providing Foo.native.js implementations.
  supports_workers_internal: A private option that allows workers to be disabled.
"""
# Private Args:
#   transpiler: J2CL compiler jar to use.
j2cl_transpile = rule(
    attrs={
        "deps": attr.label_list(allow_files=[".jar"]),
        "srcs": attr.label_list(
            mandatory=True,
            allow_files=[".java", ".srcjar", "-src.jar"],
        ),
        "native_srcs_zips": attr.label_list(
            allow_files=[".zip"],
        ),
        "readable_source_maps": attr.bool(default=False),
        "declare_legacy_namespace": attr.bool(default=False),
        "transpiler": attr.label(
            cfg="host",
            executable=True,
            allow_files=True,
            default=Label("//internal_do_not_use:J2clTranspiler"),
        ),
        "supports_workers_internal": attr.bool(default=True),
        "_zip": attr.label(
            executable=True,
            default=Label("//third_party/zip"),
            cfg="host"
        ),
    },
    implementation=_impl,
    # Declare each output artifact by name, otherwise they can not be
    # referenced by name when being used as inputs for other rules.
    outputs={
        "zip_file": "%{name}.js.zip",
    }
)
