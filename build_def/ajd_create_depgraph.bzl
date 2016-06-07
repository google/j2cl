"""ajd_create_depgraph build macro.

Takes JS files and translates them into a single depgraph.


Example use:

ajd_create_depgraph(
    name = "my_depgraph",
    srcs = ["foo.js", "bar.js"],
)

"""


# TODO(stalcup): disable verification as soon as that is possible in AJD.
def ajd_create_depgraph(name, srcs):
  """Takes JS files and translates them into a single depgraph."""
  output_target_name = ":" + name + ".depgraph"

  # Works by running AJD extract_js_depgraph once per JS file and running AJD
  # merge_depgraphs once at the end to combine the intermediate results.
  #
  # The reason the genrule command is left in charge of the per-file looping is
  # because only the $(SRCS) variable is capable of expanding passed filegroups
  # into individual JS files. Skylark itself is not capable.
  command = "\n".join([
      # Create a temp dir.
      "TMPDIR=$$(mktemp -d $@.tmp.XXXXXX)",

      # Prepare some paths.
      "ajd_binary=$(location //tools/js/blaze:autojsdeps_release)",
      "output_file=$(location %s)" % output_target_name,

      # Process each src.
      "dg_flags=()",
      "for src in $(SRCS); do",

      # Prepare an output depgraph file for this src.
      "  out_dir=\"$$TMPDIR/depgraphs/$$src\"",
      "  mkdir -p $$out_dir",
      "  out_file=\"$$out_dir/depgraph\"",

      # Create the output depgraph file.
      "  $$ajd_binary extract_js_depgraph --out_dg $$out_file --input $$src &",
      "  dg_flags+=(\"--dg $$out_file\")",
      "done",
      "wait",

      # Merge the individual depgraphs.
      "$$ajd_binary merge_depgraphs --out $$output_file $${dg_flags[*]}",
  ])

  native.genrule(
      name=name,
      srcs=srcs,
      tools=["//tools/js/blaze:autojsdeps_release"],
      outs=[output_target_name],
      cmd=command,
  )
