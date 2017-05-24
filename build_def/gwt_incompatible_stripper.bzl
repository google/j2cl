"""gwt_incompatible_stripper build rule.

Looks for java source with the @GwtIncompatible annotation and removes the marked portions before
the source is processed by J2CL or Javac.

TODO(tdeegan): This tool should be made a worker for optimal performance.

"""

STRIPPER_BINARY = "//internal_do_not_use:GwtIncompatibleStripper"

def gwt_incompatible_stripper(name, srcs, **kwargs):
  native.genrule(
      name = name,
      srcs = srcs,
      outs = [name + ".srcjar"],
      tools = [STRIPPER_BINARY],
      cmd = "$(location " + STRIPPER_BINARY + ") -d=$@ $(SRCS)",
      message = "Stripping @GwtIncompatible",
      **kwargs
  )
