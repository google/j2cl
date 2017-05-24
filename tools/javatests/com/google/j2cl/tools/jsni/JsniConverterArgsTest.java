/*
 * Copyright 2015 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
package com.google.j2cl.tools.jsni;

import static com.google.j2cl.tools.jsni.ToolsTestUtils.getDataFilePath;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.kohsuke.args4j.CmdLineException;

/**
 * Class testing that the converter throws the right exception if the arguments passed to him are
 * not valid.
 */
@RunWith(JUnit4.class)
public class JsniConverterArgsTest extends BaseJsniConverterTest {
  /**
   * If one java file passed to the converter doesn't exist, the converter throws an
   * InvalidFlagValueException
   */
  @Test
  public void main_nonExistingJavaFile() throws CmdLineException {
    String[] args = {"--output_file", getZipFilePath(), "nonExistingJavaFile.java"};

    assertThrows(CmdLineException.class, () -> Runner.main(args));
  }

  /**
   * If one calls the converter without specifying java files to convert, the converter throws an
   * InvalidFlagValueException
   */
  @Test
  public void main_noJavaFile() throws CmdLineException {
    String[] args = {"--output_file", getZipFilePath()};

    assertThrows(CmdLineException.class, () -> Runner.main(args));
  }

  /**
   * If one calls the converter without passing the mandatory --output_file flags, the converter
   * throws an InvalidFlagValueException
   */
  @Test
  public void main_noOutputFileFlag() throws CmdLineException {
    String[] args = {getDataFilePath("SimpleClass.java")};

    assertThrows(CmdLineException.class, () -> Runner.main(args));
  }

  /** If the zip file cannot be created, the converter throws a RuntimeException */
  @Test
  public void main_invalidZipFile() throws CmdLineException {
    String nonExistingPath = "/I_m/sure/this/path/doesnt/exist/._jsni_/converter/result.zip";
    String[] args = {"--output_file", nonExistingPath, getDataFilePath("SimpleClass.java")};

    assertThrows(RuntimeException.class, () -> Runner.main(args));
  }
}
