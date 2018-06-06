"""j2cl_transpile build rule.

Takes Java source and translates it into Closure style JS in a zip bundle. Java
library deps might be needed for reference resolution.

Example use:

j2cl_transpile(
    name = "my_transpile",
    javalib = ":some_lib",
)

Note: in general you want to be using j2cl_library instead of using
j2cl_transpile directly.

"""


def _impl(ctx):
  separator = ctx.configuration.host_path_separator
  java_provider = ctx.attr.javalib[java_common.provider]
  js_native_zip_files = ctx.files.native_srcs_zips

  # Using source_jars of the java_library since that includes APT generated src.
  java_src_jars = java_provider.source_jars
  java_deps = java_provider.compilation_info.compilation_classpath

  # convert files to paths
  deps_paths = [j.path for j in java_deps]
  java_src_paths = [j.path for j in java_src_jars]

  compiler_args = ["-d", ctx.outputs.zip_file.path]

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

  if ctx.var.get("GROK_ELLIPSIS_BUILD", None):
    compiler_args += ["-generatekytheindexingmetadata"]

  # The transpiler expects each java file path as a separate argument.
  compiler_args += java_src_paths

  # Create an action to write the flag file
  compiler_args_file = ctx.new_file(ctx.label.name + "_compiler.args")
  ctx.actions.write(
      output = compiler_args_file,
      content = "\n".join(compiler_args)
  )

  inputs = java_src_jars[:]
  inputs += list(java_deps)
  inputs += js_native_zip_files
  inputs += [compiler_args_file]

  ctx.action(
      progress_message = "Transpiling to JavaScript %s" % ctx.label,
      inputs=inputs,
      outputs=[ctx.outputs.zip_file],
      executable=ctx.executable.transpiler,
      arguments=["@" + compiler_args_file.path],
      env=dict(LANG="en_US.UTF-8"),
      execution_requirements={"supports-workers": "1" if ctx.attr.supports_workers_internal else "0"},
      mnemonic = "J2clTranspile",
  )

  return struct(
      files=depset([ctx.outputs.zip_file])
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
        "javalib": attr.label(providers=[java_common.provider]),
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
