# Run this to regenerate test cases.

# Build the generator binary.
blaze build //third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/integration/allsimplebridges/generator

# Run the generator binary, pointed at the test cases directory.
blaze-bin/third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/integration/allsimplebridges/generator/generator third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/integration/allsimplebridges

# Format generated test cases with google-java-format.
find third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/integration/allsimplebridges -name *.java -exec google-java-format -i {} +
