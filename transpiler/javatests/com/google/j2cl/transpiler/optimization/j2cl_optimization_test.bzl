"""j2cl_optimization_test macro

See BooleansTest for an example usage.

"""

load("/third_party/java/j2cl/j2cl_library", "j2cl_library")
load("/third_party/java/j2cl/j2cl_test", "j2cl_test")

def j2cl_optimization_test(name, defs=[], javacopts=[]):
  j2cl_test(
      name=name,
      srcs=[name + ".java"],
      javacopts=javacopts,
      compile=1,
      compiler="//javascript/tools/jscompiler:head",
      data=["//testing/matrix/nativebrowsers/chrome:stable_data"],
      extra_defs=[
          "--rewrite_polyfills=false",
          "--strict",
          "--variable_renaming=OFF",
          "--define=jre.checkedMode=DISABLED",
      ] + defs,
      deps_mgmt="closure",
      externs_list=["//javascript/externs:common"],
      jvm_flags=["-Dcom.google.testing.selenium.browser=CHROME_LINUX"],
      deps=[
          ":shared",
          "//third_party/java/junit:junit-j2cl",
          "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
      ],)
