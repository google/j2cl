"""j2cl_test build macro"""

# buildifier: disable=unused-variable
def j2cl_test_common(name, **kwargs):
    """Macro for running a JUnit test cross compiled as a web test"""

    # No-op until unit testing support implemented for open-source.
    native.genrule(
        name = name,
        deprecation = "\nCAUTION: This is a placeholder. " +
                      "j2cl_test has not ported to opensource yet." +
                      "\nHENCE WE DO *NOT* KNOW IF YOUR TEST IS PASSING OR NOT!",
        cmd = "echo Empty > $@",
        tags = ["manual"],
        outs = [name + ".out"],
    )
