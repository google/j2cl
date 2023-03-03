"""Contains the common J2clInfo provider definition."""

J2clInfo = provider(
    "Provider for the J2CL compilation.\n" +
    "NOTE: Data under '_private_' is considered private internal data so do not use.",
    fields = ["_private_", "_is_j2cl_provider"],
)

J2wasmInfo = provider(
    "Provider for the J2Wasm compilation.\n" +
    "NOTE: Data under '_private_' is considered private internal data so do not use.",
    fields = ["_private_", "_is_j2cl_provider"],
)

J2ktInfo = provider(
    "Provider for the J2KT compilation.\n" +
    "NOTE: Data under '_private_' is considered private internal data so do not use.",
    fields = ["_private_"],
)
