/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.integration.allsimplebridges.generator;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Generates test example classes that are different combinations of properties toggling in a 3
 * class hierarchy (child class, parent class, interface):
 *
 * <ul>
 * <li>childExtendsParent
 * <li>childImplementsInterface
 * <li>childSpecializesGeneric
 * <li>childOverridesParentMethod
 * <li>childOverridesInterfaceMethod
 * <li>parentIsAbstract
 * <li>parentIsGeneric
 * <li>parentIsJsType
 * <li>interfaceHasDefaultMethod
 * <li>interfaceIsJsType
 * </ul>
 */
public class TestCasesGenerator {

  private static boolean[] booleans = {true, false};
  private static int classNameCount = 0;
  private static int combinationNumber = 0;
  private static URLClassLoader customClassLoader;
  private static int interfaceNameCount = 0;
  private static Set<String> methodsThatExist = new HashSet<>();
  private static File outputDir;
  private static File tempDir;
  private static Set<String> uniqueTestCases = new HashSet<>();

  public static void main(String... args) throws Exception {
    if (args.length != 1) {
      System.err.println("Must pass just the output directory as an argument");
      System.exit(1);
    }

    outputDir = new File(args[0]);
    tempDir = Files.createTempDirectory("bridge-methods").toFile();
    customClassLoader =
        new URLClassLoader(
            new URL[] {tempDir.toURI().toURL()}, TestCasesGenerator.class.getClassLoader());

    wipeTestCases();
    createTestCases();
  }

  private static void compileClass(String classFileName, String code) throws Exception {
    Files.write(Paths.get(classFileName), code.getBytes(StandardCharsets.UTF_8));

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
    List<JavaFileObject> compilationUnits =
        Lists.<JavaFileObject>newArrayList(
            fileManager.getJavaFileObjectsFromFiles(Lists.newArrayList(new File(classFileName))));
    compiler
        .getTask(CharStreams.nullWriter(), fileManager, null, null, null, compilationUnits)
        .call();
  }

  private static String createChildClass(
      boolean childExtendsParent,
      boolean childImplementsInterface,
      boolean childSpecializesGeneric,
      boolean childOverridesParentMethod,
      boolean childOverridesInterfaceMethod,
      String parentClassName,
      String interfaceName,
      StringBuilder testerClassTemplate) {
    String childClassName;
    childClassName = createClassName();
    testerClassTemplate.append("  @SuppressWarnings(\"unchecked\")\n");
    testerClassTemplate.append("  static class " + childClassName + " ");
    if (childExtendsParent) {
      testerClassTemplate.append(" extends " + parentClassName + "");
      if (childSpecializesGeneric) {
        testerClassTemplate.append("<String>");
      }
    }
    if (childImplementsInterface) {
      testerClassTemplate.append(" implements " + interfaceName);
    }
    testerClassTemplate.append(" {\n");

    if (childOverridesParentMethod) {
      testerClassTemplate.append("    @SuppressWarnings(\"MissingOverride\")\n");
      testerClassTemplate.append("    public String get(");
      if (childSpecializesGeneric) {
        testerClassTemplate.append("String");
        methodsThatExist.add(childClassName + " String");
      } else {
        testerClassTemplate.append("Object");
        methodsThatExist.add(childClassName + " Object");
      }
      testerClassTemplate.append(" value) { return \"" + childClassName + ".get\"; }\n");
    }

    if (childOverridesInterfaceMethod) {
      if (!methodsThatExist.contains(childClassName + " String")) {
        testerClassTemplate.append("    @SuppressWarnings(\"MissingOverride\")\n");
        testerClassTemplate.append(
            "    public String get(String value) { return \"" + childClassName + ".get\"; }\n");
        methodsThatExist.add(childClassName + " String");
      }
    }

    testerClassTemplate.append("  }\n");
    return childClassName;
  }

  private static String createClassName() {
    classNameCount++;
    return "C" + classNameCount;
  }

  private static String createGetResultsFunction(
      String parentClassName, String interfaceName, String childClassName) {
    StringBuilder getResultsFunction = new StringBuilder();

    getResultsFunction.append("  public static Object getResults() {\n");
    getResultsFunction.append("    " + childClassName + " s = new " + childClassName + "();\n");
    getResultsFunction.append(
        "    java.util.Map<String, String> results = new java.util.HashMap<>();\n");

    if (methodsThatExist.contains(childClassName + " Object")) {
      getResultsFunction.append(
          "    results.put(\"" + childClassName + " Object\", s.get(new Object()));\n");
    }
    if (methodsThatExist.contains(childClassName + " String")) {
      getResultsFunction.append(
          "    results.put(\"" + childClassName + " String\", s.get(\"\"));\n");
    }
    if (methodsThatExist.contains(parentClassName + " Object")) {
      getResultsFunction.append(
          "    results.put(\""
              + parentClassName
              + " Object\", (("
              + parentClassName
              + ")s).get(\"\"));\n");
    }
    if (methodsThatExist.contains(interfaceName + " String")) {
      getResultsFunction.append(
          "    results.put(\""
              + interfaceName
              + " String\", (("
              + interfaceName
              + ")s).get(\"\"));\n");
    }

    getResultsFunction.append("    return results;\n");
    getResultsFunction.append("  }\n");
    return getResultsFunction.toString();
  }

  private static String createInterfaceName() {
    interfaceNameCount++;
    return "I" + interfaceNameCount;
  }

  @SuppressWarnings("unchecked")
  private static void createTestCase(
      boolean childExtendsParent,
      boolean childImplementsInterface,
      boolean childSpecializesGeneric,
      boolean childOverridesParentMethod,
      boolean childOverridesInterfaceMethod,
      boolean parentIsAbstract,
      boolean parentIsGeneric,
      boolean parentIsJsType,
      boolean interfaceHasDefaultMethod,
      boolean interfaceIsJsType)
      throws Exception {
    combinationNumber++;
    interfaceNameCount = 0;
    classNameCount = 0;
    methodsThatExist.clear();

    String parentClassName = null;
    String childClassName = null;
    String interfaceName = null;

    String testerClassName = "Tester" + combinationNumber;
    String testerClassFileName =
        tempDir.getAbsolutePath() + File.separator + testerClassName + ".java";

    StringBuilder testerClassTemplate = new StringBuilder();

    // Tester class.
    {
      testerClassTemplate.append("// #package#\n");
      testerClassTemplate.append("import jsinterop.annotations.JsType;\n");

      testerClassTemplate.append("public class " + testerClassName + " {\n");

      // Interface def.
      interfaceName =
          maybeCreateInterface(
              childImplementsInterface,
              interfaceHasDefaultMethod,
              interfaceIsJsType,
              interfaceName,
              testerClassTemplate);

      // Parent class.
      parentClassName =
          maybeCreateParentClass(
              childExtendsParent,
              parentIsAbstract,
              parentIsGeneric,
              parentIsJsType,
              parentClassName,
              testerClassTemplate);

      // Child class.
      childClassName =
          createChildClass(
              childExtendsParent,
              childImplementsInterface,
              childSpecializesGeneric,
              childOverridesParentMethod,
              childOverridesInterfaceMethod,
              parentClassName,
              interfaceName,
              testerClassTemplate);

      // The #function# will be swapped out with either a results reporter function or a results
      // verifier function depending.
      testerClassTemplate.append("// #function#");

      testerClassTemplate.append("}\n");
    }

    if (methodsThatExist.isEmpty()) {
      // There are no methods to test
      return;
    }

    Map<String, String> returnValuesByMethod =
        gatherRealResultsFromJvm(
            parentClassName,
            childClassName,
            interfaceName,
            testerClassTemplate,
            testerClassName,
            testerClassFileName);

    if (returnValuesByMethod == null) {
      // Output was not unique or did not compile.
      return;
    }

    writeTesterClassThatVerifiesReturnValues(
        parentClassName,
        interfaceName,
        childClassName,
        testerClassName,
        testerClassTemplate,
        returnValuesByMethod);

    System.out.println(testerClassName + ".test();");
  }

  private static void createTestCases() throws Exception {
    System.out.println("Writing test cases to " + outputDir.getAbsolutePath());

    for (boolean childExtendsParent : booleans) {
      for (boolean childImplementsInterface : booleans) {
        for (boolean childSpecializesGeneric : booleans) {
          for (boolean childOverridesParentMethod : booleans) {
            for (boolean childOverridesInterfaceMethod : booleans) {

              for (boolean parentIsAbstract : booleans) {
                for (boolean parentIsGeneric : booleans) {
                  for (boolean parentIsJsType : booleans) {

                    for (boolean interfaceHasDefaultMethod : booleans) {
                      for (boolean interfaceIsJsType : booleans) {
                        createTestCase(
                            childExtendsParent,
                            childImplementsInterface,
                            childSpecializesGeneric,
                            childOverridesParentMethod,
                            childOverridesInterfaceMethod,
                            parentIsAbstract,
                            parentIsGeneric,
                            parentIsJsType,
                            interfaceHasDefaultMethod,
                            interfaceIsJsType);
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private static StringBuilder createVerifyResultsMethod(
      String parentClassName,
      String interfaceName,
      String childClassName,
      Map<String, String> results) {
    StringBuilder verifyResultsMethod = new StringBuilder();

    verifyResultsMethod.append("  @SuppressWarnings(\"unchecked\")\n");
    verifyResultsMethod.append("  public static void test() {\n");
    verifyResultsMethod.append("    " + childClassName + " s = new " + childClassName + "();\n");

    if (results.containsKey(childClassName + " Object")) {
      verifyResultsMethod.append(
          "    assert s.get(new Object()).equals(\""
              + results.get(childClassName + " Object")
              + "\");\n");
    }
    if (results.containsKey(childClassName + " String")) {
      verifyResultsMethod.append(
          "    assert s.get(\"\").equals(\"" + results.get(childClassName + " String") + "\");\n");
    }
    if (results.containsKey(parentClassName + " Object")) {
      verifyResultsMethod.append(
          "    assert (("
              + parentClassName
              + ")s).get(\"\").equals(\""
              + results.get(parentClassName + " Object")
              + "\");\n");
    }
    if (results.containsKey(interfaceName + " String")) {
      verifyResultsMethod.append(
          "    assert (("
              + interfaceName
              + ")s).get(\"\").equals(\""
              + results.get(interfaceName + " String")
              + "\");\n");
    }

    verifyResultsMethod.append("  }\n");
    return verifyResultsMethod;
  }

  private static Map<String, String> gatherRealResultsFromJvm(
      String parentClassName,
      String childClassName,
      String interfaceName,
      StringBuilder testerClassTemplate,
      String testerClassName,
      String testerClassFileName)
      throws Exception {
    String testerClassThatReturnsValues =
        testerClassTemplate
            .toString()
            .replace(
                "// #function#",
                createGetResultsFunction(parentClassName, interfaceName, childClassName));
    compileClass(testerClassFileName, testerClassThatReturnsValues);
    Map<String, String> results =
        runTesterClassInJvmToGetResults(testerClassName, testerClassThatReturnsValues);
    return results;
  }

  private static String maybeCreateInterface(
      boolean childImplementsInterface,
      boolean interfaceHasDefaultMethod,
      boolean interfaceIsJsType,
      String interfaceName,
      StringBuilder testerClassTemplate) {
    if (childImplementsInterface) {
      if (interfaceIsJsType) {
        testerClassTemplate.append("  @JsType\n");
      }
      interfaceName = createInterfaceName();
      testerClassTemplate.append("  static interface " + interfaceName + " {\n");
      testerClassTemplate.append(
          "    "
              + (interfaceHasDefaultMethod ? "default " : "")
              + "String get(String value)"
              + (interfaceHasDefaultMethod ? " { return \"" + interfaceName + ".get\"; }" : ";")
              + "\n");
      testerClassTemplate.append("  }\n");
      methodsThatExist.add(interfaceName + " String");
    }
    return interfaceName;
  }

  private static String maybeCreateParentClass(
      boolean childExtendsParent,
      boolean parentIsAbstract,
      boolean parentIsGeneric,
      boolean parentIsJsType,
      String parentClassName,
      StringBuilder testerClassTemplate) {
    if (childExtendsParent) {
      if (parentIsJsType) {
        testerClassTemplate.append("  @JsType\n");
      }
      parentClassName = createClassName();
      testerClassTemplate.append(
          "  "
              + (parentIsAbstract ? "abstract " : "")
              + "static class "
              + parentClassName
              + " "
              + (parentIsGeneric ? "<T>" : "")
              + "{\n");
      testerClassTemplate.append(
          "    public "
              + (parentIsAbstract ? "abstract " : "")
              + "String get("
              + (parentIsGeneric ? "T" : "Object")
              + " value) "
              + (parentIsAbstract ? ";" : "{ return \"" + parentClassName + ".get\"; }")
              + "\n");
      methodsThatExist.add(parentClassName + " Object");
      testerClassTemplate.append("  }\n");
    }
    return parentClassName;
  }

  private static Map<String, String> runTesterClassInJvmToGetResults(
      String testerClassName, String testerClassThatReturnsValues) {
    Map<String, String> results = null;
    try {
      Class<?> testerClass = customClassLoader.loadClass(testerClassName);
      Method getMethod = testerClass.getMethod("getResults");
      results = (Map<String, String>) getMethod.invoke(null);

      String uniqueResult = testerClassThatReturnsValues.replaceAll("Tester.*", "");
      if (!uniqueTestCases.add(uniqueResult)) {
        // Output was not unique
        return null;
      }

    } catch (Exception e) {
      // Output does not compile
      return null;
    }
    return results;
  }

  private static void wipeTestCases() {
    System.out.println("Deleting test cases in " + outputDir.getAbsolutePath());

    for (File file : outputDir.listFiles()) {
      if (file.getName().endsWith(".java") && file.getName().startsWith("Tester")) {
        file.delete();
      }
    }
  }

  private static void writeTesterClassThatVerifiesReturnValues(
      String parentClassName,
      String interfaceName,
      String childClassName,
      String testerClassName,
      StringBuilder testerClassTemplate,
      Map<String, String> returnValuesByMethod)
      throws IOException {
    StringBuilder verifyReturnValuesMethod =
        createVerifyResultsMethod(
            parentClassName, interfaceName, childClassName, returnValuesByMethod);

    String testerClassThatVerifiesReturnValues =
        testerClassTemplate
            .toString()
            .replace("// #function#", verifyReturnValuesMethod.toString())
            .replace(
                "// #package#", "package com.google.j2cl.transpiler.integration.allsimplebridges;");

    Files.write(
        Paths.get(outputDir.getAbsolutePath() + File.separator + testerClassName + ".java"),
        testerClassThatVerifiesReturnValues.getBytes(StandardCharsets.UTF_8));
  }
}
