# Description:
#   Integration tests.
#
# The IntegrationTestCase base class makes it extremely easy to define and
# verify integration tests that need to assert about output std or err logs.
# Other integration tests will want to run transpiled output as JS and
# should make use of j2cl_library macro.

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

filegroup(
    name = "integration_tests_warnings_allowlist",
    srcs = ["integration_tests_warnings_allowlist.txt"],
    visibility = [":__subpackages__"],
)
