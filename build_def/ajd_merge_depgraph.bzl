"""ajd_merge_depgraph build macro.

Takes depgraph files and merges them into a single depgraph.

Normally AJD verifies correctness in every merge but when synthesizing a
depgraph file that is a combination of a generated depgraph file for a ZIP and
extracted an depgraph from handrolled JS, the result only becomes valid after
the merge has completed (because of crosswise and circular dependencies).

While it's reasonable for the depgraphs of end user hand written JS to be
subject to constant verification, tools that synthesize JS and try to delegate
the depgraph construction process need to temporarily suspend verification to
be able to arrive at a final correct state. Thus the existence of this macro.


Example use:

ajd_merge_depgraph(
    name = "my_depgraph",
    depgraphs = ["foo.depgraph", "bar.depgraph"],
)

"""


# TODO(stalcup): disable verification as soon as that is possible in AJD.
def ajd_merge_depgraph(name, depgraphs):
  """Creates a merged depgraph for the given depgraphs."""
  output_target_name = ":" + name + ".depgraph"

  command = "\n".join([
      # Prepare some paths.
      "ajd_binary=$(location //tools/js/blaze:autojsdeps_release)",
      "output_file=$(location %s)" % output_target_name,

      # Format depgraph flags.
      "dg_flags=()",
      "for depgraph in $(SRCS); do",
      "  dg_flags+=(\"--dg $$depgraph\")",
      "done",

      # Merge the individual depgraphs.
      "$$ajd_binary merge_depgraphs --out $$output_file $${dg_flags[*]}",
  ])

  native.genrule(
      name=name,
      srcs=depgraphs,
      tools=["//tools/js/blaze:autojsdeps_release"],
      outs=[output_target_name],
      cmd=command,
  )
