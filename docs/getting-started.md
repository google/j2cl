
 <!-- TOC -->

# Getting Started

## Introduction

J2CL enables seamless use of Java in your JavaScript applications. One of the
things that makes J2CL unique is: it gives you the complete freedom of choice!
You can use J2CL to just make some Java code accessible from JavaScript or go
all the way to create a whole application with it; whatever best suits your
needs.

To understand what this means please take a look at the simple hello world
sample that demonstrates how extensively Java and JavaScript could work together
in J2CL:

https://github.com/google/j2cl/tree/master/samples/helloworld

## Building your first application

Tip: If you are not familiar with using Java with Bazel, it might be helpful to
start with the
[Bazel Java Tutorial](https://bazel.build/tutorials/java)
since J2CL closely follows Bazel Java idioms.

Let's get started with cloning the repository and build a sample app from
source:

-   Install [Bazelisk](https://github.com/bazelbuild/bazelisk):

```shell
    $ npm install -g @bazel/bazelisk
    $ alias bazel=bazelisk
```

-   Clone this repository:

```shell
      $ git clone https://github.com/google/j2cl.git
```

-   Build the sample app:

```shell
      $ cd j2cl/samples/helloworld
      $ bazel build src/main/java/com/google/j2cl/samples/helloworld:helloworld
```

Since we already built our first application, let's take a quick look at how the
optimized output looks like:

```shell
      $ cat bazel-bin/src/main/java/com/google/j2cl/samples/helloworld/helloworld.js
      Output:
      document.body.innerText='Hello from Java! and JS!';
```

Amazing, isn't it? When we say J2CL tightly optimizes, we really mean it!

The optimizations in the J2CL world frequently crosses the boundaries of two
languages and drop anything you don't need in production to give you the leanest
application possible. This is only feasible because J2CL is very closely
integrated with Closure Compiler which enables the optimization of the whole app
together.

Let's run the development server to see our application in action:

-   Run the development server

```shell
      $ bazel run src/main/java/com/google/j2cl/samples/helloworld:helloworld_dev_server
```

-   Navigate to 'http://localhost:6006/helloworld_dev.html" in your browser.

Please take the time to play with the code and get a better feeling of how
things are working.

## Live-reload

ibazel is file-system watcher that auto-triggers bazel build when needed. To use
it, just replace `bazel` with `ibazel` in any command:

-   Install [ibazel](https://github.com/bazelbuild/bazel-watcher#installation)
    and make sure it is in your path.

-   Run the development server

```shell
      $ ibazel run src/main/java/com/google/j2cl/samples/helloworld:helloworld_dev_server
```

-   Navigate to 'http://localhost:6006/helloworld_dev.html" in your browser.

-   Edit any source (e.g. `HelloWorld.java`), save and see the results. You will
    enjoy it more as it warms up!

You like build-on-save but you would like to refresh on your own terms? Pass
`-nolive_reload` while running ibazel.

## Unit testing

You can also write regular JUnit tests for your application and run them under
J2CL.

To run the sample unit test for our sample app:

```shell
      $ bazel test src/test/java/com/google/j2cl/samples/helloworldlib:HelloWorldTest
```

## Setting up your first project

To setup your first project you can use the sample as template:

```shell
      $ cp -R <j2cl-repo>/samples/helloworld <my-repo>/<app-name>
      $ mv <my-repo>/<app-name>/WORKSPACE.remote <my-repo>/<app-name>/WORKSPACE
```

and you are done.

## Next steps

*   [JsInterop Cookbook](jsinterop-by-example.md) for examples on how to interop
    with Java and JavaScript.
*   [Guava Example](https://github.com/google/j2cl/tree/master/samples/guava)
    for a Hello World example using Guava.
*   [J2CL Best Practices](best-practices.md) to make informed decisions for your
    project.
*   [J2CL Transpiler Readable Repo](https://github.com/google/j2cl/tree/master/transpiler/javatests/com/google/j2cl/readable)
    to dive into internals and see how J2CL generated code looks like in
    different situations.
