"""j2cl_optimization_test macro

See BooleansTest for an example usage.

"""

load("//build_defs:rules.bzl", "j2cl_test")

def j2cl_optimization_test(name, defs = [], javacopts = []):
    j2cl_test(
        name = name,
        srcs = [name + ".java"],
        javacopts = javacopts,
        compile = 1,
        compiler = "//javascript/tools/jscompiler:head",
        extra_defs = [
            "--rewrite_polyfills=false",
            "--strict",
            "--variable_renaming=OFF",
            "--define=jre.checkedMode=DISABLED",
        ] + defs,
        deps_mgmt = "closure",
        deps = [
            ":shared",
            "//third_party:junit-j2cl",
            "//third_party:gwt-jsinterop-annotations-j2cl",
        ],
    )
