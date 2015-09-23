/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.junit.client;

import junit.framework.TestCase;

/**
 * This is a slimmed down version of GWTTestCase, so that we can compile and run GWT's emul
 * tests in a JRE.
 */
@SuppressWarnings("unused")
public abstract class GWTTestCase extends TestCase {

  public GWTTestCase() {
  }

  public abstract String getModuleName();


  protected void gwtSetUp() throws Exception {
  }


  protected void gwtTearDown() throws Exception {
  }

  @Override
  protected final void setUp() throws Exception {
    gwtSetUp();
  }

  @Override
  protected final void tearDown() throws Exception {
    gwtTearDown();
  }
}