Want to contribute? Great! First, read this page.

### Before you contribute
Before we can use your code, you must sign the
[Google Individual Contributor License Agreement](https://cla.developers.google.com/about/google-individual)
(CLA), which you can do online.
You (or your employer) retain the copyright to your contribution, this simply
gives us permission to use and redistribute your contributions as part of the
project. Head over to https://cla.developers.google.com/ to see your current
agreements on file or to sign a new one.

You generally only need to submit a CLA once, so if you've already submitted
one (even if it was for a different project), you probably don't need to do it
again.

Before you start working on a larger contribution, you should get in touch with
us first through the issue tracker with your idea so that we can help out and
possibly guide you. Coordinating up front makes it much easier to avoid
frustration later on.


### Code reviews

All submissions, including submissions by project members, require a code
review. We use GitHub pull requests for this purpose.

### J2CL code priorities

- Transpiler runs fast (productivity)
- Output is understandable (ease integration and debugging)
- Output optimizes well with Closure compiler (performance)
- Output executes fast in the Browser (performance)

### Shell aliasing and running j2 script

- Alias J2CL's dev script (dev/j2.py). **Do this before anything else**.

```shell

    # alias j2 for ease of use
    alias j2='python3 dev/j2.py'
```

- Running `j2` in the shell will show the available commands for J2CL
development.

**Note:** `j2` script is not ported for open-source development yet. However
you can still test your contributions by running particular integration tests
using `bazel test <target>`. You can also run `./build_test.sh CI` script to
test againt full test suite available in the open-source repo.


### Working with the codebase

- When iterating quickly it's best to run `j2 test`  and `j2 gen` for individual
  targets.
- When starting a review always first run `j2 presubmit`, and of course if you
  see anything unexpected then correct it before beginning the review.
- It's important that you run both `j2 presubmit` after
  syncing. If you don't then the build.log's you've previously created might not
  reflect the current state of the repo. Also if you don't then the baseline
  against which size changes are compared in the size_report.txt won't be the
  right baseline.

