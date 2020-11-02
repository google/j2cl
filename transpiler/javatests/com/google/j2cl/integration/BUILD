# Description:
#   Integration tests.
#
# The IntegrationTestCase base class makes it extremely easy to define and
# verify integration tests that need to assert about output std or err logs.
# Other integration tests will want to run transpiled output as JS and
# should make use of j2cl_library macro.

load("//testing/web/build_defs:web.bzl", "web_test_config")

package(
    licenses = ["notice"],
)

config_setting(
    name = "tree_artifact_enabled",
    flag_values = {
        "//:enable_experimental_tree_artifact_mode": "True",
    },
)

genrule(
    name = "tree_artifact_integration_tests_warnings_whitelist",
    srcs = ["integration_tests_warnings_whitelist.txt"],
    outs = ["tree_artifact_integration_tests_warnings_whitelist.txt"],
    cmd = " sed -e 's/\\(third_party\\/java_src\\/j2cl\\/transpiler\\/javatests\\/com\\/google\\/j2cl\\/integration.*\\)\\.js.zip!/\\1_j2cl_js/g' " +
          " $< > $@",
    message = "Generating tree artifact whitelist",
)

filegroup(
    name = "integration_tests_warnings_whitelist",
    srcs = select({
        ":tree_artifact_enabled": ["tree_artifact_integration_tests_warnings_whitelist.txt"],
        "//conditions:default": ["integration_tests_warnings_whitelist.txt"],
    }),
    visibility = [":__subpackages__"],
)

web_test_config(
    name = "enable_wasmgc",
    metadata = "chrome_linux_enable_wasmgc.json",
    visibility = [":__subpackages__"],
)
