load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "lambdas",
    srcs = glob(["*.java"]),
    # A crash in Kotlin frontend:
    # Exception in thread "main" java.lang.ClassCastException:
    # class com.google.j2cl.transpiler.ast.AutoValue_ArrayTypeDescriptor cannot be cast to class com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
    enable_kt_web = False,
)
