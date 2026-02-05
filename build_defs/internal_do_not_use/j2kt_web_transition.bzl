"""Defines the user-configuration transition to enable J2KT Web builds."""

visibility([
    "//transpiler/javatests/com/google/j2cl/readable",
    "//transpiler/javatests/com/google/j2cl/integration",
])

j2kt_web_transition = transition(
    implementation = lambda *_: {"//:j2kt_web_environment": "experimental"},
    inputs = [],
    outputs = ["//:j2kt_web_environment"],
)
