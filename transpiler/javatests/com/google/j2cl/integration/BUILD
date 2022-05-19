# Description:
#   Integration tests.
#
# The IntegrationTestCase base class makes it extremely easy to define and
# verify integration tests that need to assert about output std or err logs.
# Other integration tests will want to run transpiled output as JS and
# should make use of j2cl_library macro.

package(licenses = ["notice"])

filegroup(
    name = "integration_tests_warnings_whitelist",
    srcs = ["integration_tests_warnings_whitelist.txt"],
    visibility = [":__subpackages__"],
)
