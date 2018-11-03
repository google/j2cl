"""Build test macro for j2cl_library targets. Not intended for public use."""

def build_test(target, tags):
    """Create a <target>_build_test that verifies the provied J2CL target"""

    # No-op. Open-source uses incremental type check which takes care of this.
