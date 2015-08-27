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

import com.google.common.flags.Flags;
import com.google.common.flags.InvalidFlagValueException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Class testing that the converter throws the right exception if the arguments passed to him are
 * not valid.
 */
@RunWith(JUnit4.class)
public class JsniConverterArgsTest extends BaseJsniConverterTest {
  @Before
  public void resetFlags() {
    Flags.disableStateCheckingForTest();
    Flags.resetFlagForTest(JsniConverter.class, "verbose");
    Flags.resetFlagForTest(JsniConverter.class, "output_file");
    Flags.enableStateCheckingForTest();
  }

  /**
   * If one java file passed to the converter doesn't exist, the converter throws an
   * InvalidFlagValueException
   */
  @Test(expected = InvalidFlagValueException.class)
  public void main_nonExistingJavaFile() throws InvalidFlagValueException {
    String[] args = {"--output_file", getZipFilePath(), "nonExistingJavaFile.java"};

    JsniConverter.main(args);
  }

  /**
   * If one calls the converter without specifying java files to convert, the converter throws an
   * InvalidFlagValueException
   */
  @Test(expected = InvalidFlagValueException.class)
  public void main_noJavaFile() throws InvalidFlagValueException {
    String[] args = {"--output_file", getZipFilePath()};

    JsniConverter.main(args);
  }

  /**
   * If one calls the converter without passing the mandatory --output_file flags, the converter
   * throws an InvalidFlagValueException
   */
  @Test(expected = InvalidFlagValueException.class)
  public void main_noOutputFileFlag() throws InvalidFlagValueException {
    String[] args = {getDataFilePath("SimpleClass.java")};

    JsniConverter.main(args);
  }

  /**
   * If the zip file cannot be created, the converter throws a RuntimeException
   */
  @Test(expected = RuntimeException.class)
  public void main_invalidZipFile() throws InvalidFlagValueException {
    String nonExistingPath = "/I_m/sure/this/path/doesnt/exist/._jsni_/converter/result.zip";
    String[] args = {"--output_file", nonExistingPath, getDataFilePath("SimpleClass.java")};

    JsniConverter.main(args);
  }
}
