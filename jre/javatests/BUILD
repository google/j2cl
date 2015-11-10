# Description:
#   j2cl/jre: JRE subset for transpilation.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("/third_party/java_src/j2cl/build_def/j2cl_source_copy", "j2cl_source_copy")

j2cl_source_copy(
    name = "emul_test_copy",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:emul_test"],
    base_strip_path = "third_party/java_src/gwt/svn/trunk/user/test/",
    excludes = [
        "com/google/gwt/emultest/java8/util/OptionalTest.java",
        "com/google/gwt/emultest/EmulJava8Suite.java",
    ],
)

j2cl_source_copy(
    name = "emul_test_javaemul_copy",
    srcs = ["//third_party/java_src/gwt/svn/trunk/dev:emul"],
    base_strip_path = "third_party/java_src/gwt/svn/trunk/user/dev/core/super/",
)

java_library(
    name = "emul_tests",
    testonly = 1,
    srcs = [
        "com/google/gwt/core/client/JavaScriptObject.java",
        "com/google/gwt/emultest/AllTests.java",
        "com/google/gwt/junit/DoNotRunWith.java",
        "com/google/gwt/junit/Platform.java",
        "com/google/gwt/junit/client/GWTTestCase.java",
        "com/google/gwt/junit/tools/GWTTestSuite.java",
        ":emul_test_copy",
        ":emul_test_javaemul_copy",
    ],
    deps = ["//third_party/java/junit"],
)

# This runs all GWTTestCases for emul in a pure JRE, this way we can make sure that our tests
# assert the right behaviour for the JRE emulation.
java_test(
    name = "AllTests",
    size = "small",
    test_class = "com.google.gwt.emultest.AllTests",
    runtime_deps = [
        ":emul_tests",
    ],
)
