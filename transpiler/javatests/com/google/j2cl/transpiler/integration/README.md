# Contained here are integration tests.

To make assertions about the std or err output of the Transpile process see
//third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/integration:SyntaxErrorTest
for an example.

To execute output as a JS test and track its optimized size see
third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/integration/interfaces
for an example. Even though these tests are executed as JS they are not JUnit
tests and are not run using the j2cl_java_test() rule type (to avoid test
harness overhead interfering with optimized size change tracking.) These tests
must have a Main class with a main() function.

## Expectations
- Include an updated size_report.txt in every change.
