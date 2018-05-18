"""j2cl_java_lib build rule.

This is similar to java_library but based on java_common.provider so we could
drive the compilation from skylark.

"""

def _impl(ctx):
  srcs = ctx.files.srcs
  deps = [dep[JavaInfo] for dep in ctx.attr.deps]
  exports = [export[JavaInfo] for export in ctx.attr.exports]
  plugins = [p[JavaInfo] for p in ctx.attr.plugins]
  exported_plugins = [p[JavaInfo] for p in ctx.attr.exported_plugins]

  java_provider = java_common.compile(
      ctx,
      source_files = ctx.files.srcs_hack,
      source_jars = srcs,
      output = ctx.outputs.jar,
      javac_opts = java_common.default_javac_opts(
          ctx,
          java_toolchain_attr = "_java_toolchain"),
      deps = deps,
      exports = exports,
      plugins = plugins,
      exported_plugins = exported_plugins,
      java_toolchain = ctx.attr._java_toolchain,
      host_javabase = ctx.attr._host_javabase,
  )

  return [
      DefaultInfo(files=depset([ctx.outputs.jar])),
      java_provider
  ]

j2cl_java_library = rule(
  implementation = _impl,
  attrs = {
      "srcs": attr.label_list(allow_files=True),
      "srcs_hack": attr.label_list(allow_files=True),
      "deps": attr.label_list(providers=[JavaInfo]),
      "exports": attr.label_list(providers=[JavaInfo]),
      "plugins": attr.label_list(providers=[JavaInfo]),
      "exported_plugins": attr.label_list(providers=[JavaInfo]),
      "javacopts": attr.string_list(),
      "resources": attr.label_list(allow_files=True), # TODO(goktug): remove
      "licenses": attr.license(), # TODO(goktug): remove
      "_java_toolchain": attr.label(
          default = Label("//tools/jdk:toolchain")
      ),
      "_host_javabase": attr.label(
          default = Label("//tools/jdk:current_host_java_runtime"),
          cfg = "host"
      )
  },
  fragments = ["java"],
  outputs = {
      "jar": "lib%{name}.jar",
      "srcjar": "lib%{name}-src.jar",
  },
)
