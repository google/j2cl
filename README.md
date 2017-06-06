
J2CL
====
> *THIS REPOSITORY IS AN EARLY_ACCESS OF J2CL AND SHOULD NOT BE DISCUSSED
PUBLICLY.*

J2CL is a Java to closure style javascript transpiler.
The project aims to refresh GWT by enabling easy reuse of Java libraries in JS
applications and by generating JS that tightly optimizes and correctly
type-checks in [Google closure compiler](https://github.com/google/closure-compiler)

Compile with Bazel
------------------

You can build the transpiler from source by following the steps below:

- You need to install [Bazel](https://bazel.build/versions/master/docs/install.html).
- clone this repository with git: `git clone https://github.com/google/j2cl.git`
- Inside the repository, have bazel build the jar file:

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
