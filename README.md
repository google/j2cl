> # PLEASE BE ADVISED: This repository does not contain a finished product and has limited access. Early access is provided to individual contributors for the purpose of migration of the GWT-SDK and checking compatibility of a few common open source GWT libraries.

J2CL
====
J2CL is a transpiler from Java to Closure style JavaScript.
The project enables easy reuse of Java libraries in JS applications by
generating JS that tightly optimizes and correctly type-checks in
[Google Closure Compiler](https://github.com/google/closure-compiler)

Compile with Bazel
------------------

You can build the transpiler from source by following the steps below:

- Install [Bazel](https://bazel.build/versions/master/docs/install.html).
- Clone this repository with git: `git clone https://github.com/google/j2cl.git`
- Build the jar file inside the repository with Bazel:

      $ bazel build //transpiler/java/com/google/j2cl/transpiler:J2clTranspiler_deploy.jar

Run the transpiler
------------------

- The generated jar file can be found at `bazel-bin/transpiler/java/com/google/j2cl/transpiler/J2clTranspiler_deploy.jar`
- The main method is located in `com.google.j2cl.transpiler.J2clTranspiler`

To compile and resolve the calls; J2CL does need bytecode of deps, just like javac.
So the transpiler takes following as input:

- java sources (individual files or src jar)
- native srcs (a zip file containing native.js files)
- classpath (bytecode jar of deps)
- bootclasspath (bytecode jar of jre)

and produces:

- output (invidiual js files for each class or a zip)
- sourcemaps

You can look into `build_def/j2cl_transpile.bzl` and `build_def/j2cl_library.bzl` to have more insight on how we use the transpiler internally.

Contributing
------------
Please refer to [the contributing document](CONTRIBUTING.md).

Licensing
---------
Please refer to [the license file](LICENSE).

Disclaimer
----------
This is not an official Google product.
