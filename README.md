# [J2CL](https://github.com/google/j2cl)

Seamless Java in JavaScript applications that tightly optimizes with
[Closure](https://github.com/google/closure-compiler)

---
J2CL is a powerful, simple, lightweight transpiler from Java to Closure style JavaScript.

* **Get best out of Java and JavaScript.** You no longer need to choose between
the two or lock into a specific framework or a language. Choose the right language
at the right place and hire the best talent for the job.

* **Do it correct the first time.** The robust run-time type checking based on the
strong Java type system integrated with the advanced cross language checks catches
your mistakes early on.

* **Modern, fresh and blazing fast.** Powered by [Bazel](https://bazel.build/),
J2CL provides fast and modern development experience that will make you smile and
keep you productive.

* **Provides massive code reuse.** J2CL closely follows the Java language
[semantics](docs/limitations.md). This reduces suprises, enables reuse across
different platforms and brings most popular Java libraries into your toolkit
including [Guava](https://github.com/google/guava), [Dagger](https://google.github.io/dagger/)
and [AutoValue](https://github.com/google/auto/tree/master/value).

* **Road tested and trusted.** J2CL is the underlying technology of the most
advanced GSuite apps developed by Google including GMail, Inbox, Docs, Slides
and Calendar.


Getting Started
---
Clone the repository and build a sample app from source:

- Install [Bazel](https://bazel.build/versions/master/docs/install.html).
- Clone this repository:

```shell
      $ git clone https://github.com/google/j2cl.git
```

- Build a sample app:

```shell
      $ bazel build samples/helloworld/java/com/google/j2cl/samples/helloworld:helloworld
```

- Look at the optimized output:

```shell
      $ cat bazel-bin/samples/helloworld/java/com/google/j2cl/samples/helloworld/helloworld.js

      document.write('Hello from Java! and JS!');
```

Caveat Emptor
-------------
J2CL is production ready, but open-sourcing is not yet finalized and breaking
changes will be introduced.
We are actively working on adapting more pieces to open-source including
[Junit4](https://junit.org/junit4/) emulation and faster pruning for even better development experience.
Stay tuned!

Contributing
------------
Read how to [contribute to J2CL](CONTRIBUTING.md).

Licensing
---------
Please refer to [the license file](LICENSE).

Disclaimers
-----------
J2CL is not an official Google product and is currently in 'alpha' release for developer preview.
