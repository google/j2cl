"""js_import build rule.

This build extension defines a new rule, js_import, that takes js containing
zip files as input and surfaces them into any referencing js_library tree.

Here is an example use of js_import:

js_import(
    name = "my_name",
    srczips = glob(["*.pintozip"]),
)

"""


def _impl(ctx):
  use_ajd = False
  js_provider = js_common.provider(
      ctx, ctx.files.srczips, [d.js for d in ctx.attr.deps], use_ajd)
  return struct(
      files=set(js_provider.depgraphs()),
      js=js_provider,
  )

# expose rule
js_import = rule(
    implementation=_impl,
    attrs={
        "deps": attr.label_list(
            allow_files=False,
            providers=["js"]),
        "srczips": attr.label_list(
            allow_files=FileType([".pintozip"])),
    },
)
