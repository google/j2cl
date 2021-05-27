# Description:
#   Integration tests.
#
# The IntegrationTestCase base class makes it extremely easy to define and
# verify integration tests that need to assert about output std or err logs.
# Other integration tests will want to run transpiled output as JS and
# should make use of j2cl_library macro.

package(
    licenses = ["notice"],
)

config_setting(
    name = "tree_artifacts_enabled",
    define_values = {
        "J2CL_TREE_ARTIFACTS": "1",
    },
)

genrule(
    name = "tree_artifact_integration_tests_warnings_whitelist",
    srcs = ["integration_tests_warnings_whitelist.txt"],
    outs = ["tree_artifact_integration_tests_warnings_whitelist.txt"],
    cmd = " sed -e 's/\\(third_party\\/java_src\\/j2cl\\/transpiler\\/javatests\\/com\\/google\\/j2cl\\/integration.*\\.js\\).zip!/\\1/g' " +
          " $< > $@",
    message = "Generating tree artifact whitelist",
)

filegroup(
    name = "integration_tests_warnings_whitelist",
    srcs = select({
        ":tree_artifacts_enabled": ["tree_artifact_integration_tests_warnings_whitelist.txt"],
        "//conditions:default": ["integration_tests_warnings_whitelist.txt"],
    }),
    visibility = [":__subpackages__"],
)
