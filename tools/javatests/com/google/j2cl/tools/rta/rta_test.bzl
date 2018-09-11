"""Test macro for j2cl_rta"""

load("//build_def:j2cl_rta.bzl", "j2cl_rta")

def rta_test(
        name,
        targets,
        unused_types_golden_file,
        unused_members_golden_file):
    """Test macro used for testing j2cl_rta.

    The macro defines the j2cl_rta rule and then create a java_test for comparing the result of the
    RTA algorithm with the golden files passed as parameters.

    Args:
        name: name of the test
        targets: list of label that will be passed to the j2cl_rta rules
        unused_types_golden_file: golden file containing the expected list of unused types
        unused_members_golden_file: golden file containing the expected list of unused members
    """

    if not unused_types_golden_file:
        fail("missing golden file for unused types")
    if not unused_members_golden_file:
        fail("missing golden file for unused members")

    rta_rule_name = "%s_rta" % name

    j2cl_rta(
        name = rta_rule_name,
        targets = targets,
    )

    native.java_test(
        name = name,
        test_class = "com.google.j2cl.tools.rta.GoldenFileTester",
        runtime_deps = [
            "//tools/javatests/com/google/j2cl/tools/rta:golden_file_tester_lib",
        ],
        data = [
            ":%s_unused_types.list" % rta_rule_name,
            ":%s_unused_members.list" % rta_rule_name,
            unused_members_golden_file,
            unused_types_golden_file,
        ],
        jvm_flags = [
            "-Dunused_types_rta=$(location :%s_unused_types.list)" % rta_rule_name,
            "-Dunused_members_rta=$(location :%s_unused_members.list)" % rta_rule_name,
            "-Dunused_types_golden_file=$(location " + unused_types_golden_file + ")",
            "-Dunused_members_golden_file=$(location " + unused_members_golden_file + ")",
        ],
    )
