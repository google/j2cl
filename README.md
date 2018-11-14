# [J2CL](http://j2cl.io)

Seamless Java in JavaScript applications that tightly optimizes with
[Closure Compiler](https://github.com/google/closure-compiler)

---
J2CL is a powerful, simple and lightweight transpiler from Java to Closure style
JavaScript.

* **Get the best out of Java and JavaScript.** You no longer need to choose between
the two or lock into a specific framework or a language. Choose the right language
at the right place and hire the best talent for the job.

* **Get it correct the first time.** The robust run-time type checking based on
the strong Java type system combined with the advanced cross language type checks
catches your mistakes early on.

* **Provides massive code reuse.** J2CL closely follows the Java language
[semantics](docs/limitations.md). This reduces surprises, enables reuse across
different platforms and brings most popular Java libraries into your toolkit
including [Guava](https://github.com/google/guava), [Dagger](https://google.github.io/dagger/)
and [AutoValue](https://github.com/google/auto/tree/master/value).

* **Modern, fresh and blazing fast.** Powered by [Bazel](https://bazel.build/),
J2CL provides a fast and modern development experience that will make you smile
and keep you productive.

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
      $ cd j2cl/samples/helloworld
      $ bazel build src/main/java/com/google/j2cl/samples/helloworld:helloworld
```

- Look at the optimized output:

```shell
      $ cat bazel-bin/src/main/java/com/google/j2cl/samples/helloworld/helloworld.js
      document.write('Hello from Java! and JS!');
```


Live-reload
---
ibazel is file-system watcher that auto-triggers bazel build when neeeded.
To use it, just replace ```bazel``` with ```ibazel``` in any command:

- Install [ibazel](https://github.com/bazelbuild/bazel-watcher#installation) and make sure it is in your path.

- Run the development server

```shell
      $ cd j2cl/samples/helloworld
      $ ibazel run src/main/java/com/google/j2cl/samples/helloworld:helloworld_dev_server
```
- Navigate to 'http://localhost:6006/helloworld_dev.html" in your browser.

- Edit any source (e.g. ```HelloWorld.java```), save and see the results.
You will enjoy it more as it warms up!

You like build-on-save but you would like to refresh on your own terms?
Pass ```-nolive_reload``` while running ibazel.


Setting up your first project
---
To start your first project, you can use the helloworld sample as a template:

```shell
      $ cp -R <j2cl-repo>/samples/helloworld <my-repo>/<app-name>
```

and you are done.


Guides
------
- [JsInterop Cookbook](docs/jsinterop-by-example.md)
- [J2CL Best Practices](docs/best-practices.md)
- [Emulation Limitations](docs/limitations.md)
- [Bazel Tutorial](https://docs.bazel.build/versions/master/tutorial/java.html)
- [Bazel Best Practices](https://docs.bazel.build/versions/master/best-practices.html)


Get Support
------
- Please subscribe to [J2CL announce](http://groups.google.com/forum/#!forum/j2cl-announce) for announcements (low traffic).
- Please report [bugs](https://github.com/google/j2cl/issues/new?template=bug_report.md&labels=bug)
or file [feature requests](https://github.com/google/j2cl/issues/new?template=feature_request.md&labels=enhancement)
via [issue tracker](https://github.com/google/j2cl/issues).
- For other questions you can also use the [issue tracker](https://github.com/google/j2cl/issues/new?template=question.md&labels=question) for now.


Caveat Emptor
-------------
J2CL is production ready and actively used by many of Google's products, but the
process of adapting workflows and tooling for the open-source version is not yet
finalized and breaking changes will most likely be introduced.
We are actively working on adapting more pieces including
[Junit4](https://junit.org/junit4/) emulation and faster pruning for an even
better development experience.
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

Test import PR
