---
name: Bug report
about: Create a report to help us improve J2CL
title: ''
labels: ''
assignees: ''

---

**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Please try send us the minimal Java code snippet that reproduces the problem.

If the bug is related to generated code behavior, pls try to include generated JavaScript as well.
Here is the instructions to produce the JavaScript:
 - ``` $ bazel build <package>:<target>.js.zip```
 - ``` $ file-roller bazel-bin/<package>/<target>.js.zip```
And find the <java-class>.impl.java.js from the zip file.

**Bazel version**
Please include version of Bazel that you are running J2CL with:
 ``` $ bazel version```

**Expected behavior**
A clear and concise description of what you expected to happen.
