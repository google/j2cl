J2CL: Java to Closure Typed JavaScript
======================================

What J2CL is
------------
J2CL is a lightweight transpiler from Java to Closure style JavaScript.
J2CL makes it easy to reuse of Java libraries in JavaScript applications by
generating JavaScript code that optimizes tightly and type-checks in
[Google Closure Compiler](https://github.com/google/closure-compiler).

J2CL provides a rich [JavaScript interoperation](docs/jsinterop-by-example.md) layer that allows to produce and
consume Closure JavaScript abstractions.

J2CL supports [most](docs/limitations.md) of Java semantics.

Getting Started
---------------
You can build J2CL and samples from source by following the steps below:

- Install [Bazel](https://bazel.build/versions/master/docs/install.html).
- Clone this repository:

```shell
      $ git clone https://github.com/google/j2cl.git
```

- Build a sample app:

```shell
      $ bazel build samples/helloworld/java/com/google/j2cl/samples/helloworld:helloworld
```

- Look at the optimized sample app:

```shell
      $ cat bazel-bin/samples/helloworld/java/com/google/j2cl/samples/helloworld/helloworld.js
      document.write('Hello from Java! and JS!');
```


Contributing
------------
Read how to [contribute to J2CL](CONTRIBUTING.md).

Licensing
---------
Please refer to [the license file](LICENSE).

Disclaimers
-----------
J2CL is not an official Google product and is currently in 'alpha' release for developer preview.
