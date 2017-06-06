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
package com.google.j2cl.transpiler.integration.instancequalifieronstaticfield;

public class Main {
  public static int staticField = 100;
  public static int sideEffectCount = 0;
  public static Main staticMain;

  public static int getStaticValue() {
    return staticField;
  }

  public static Main getStaticMain() {
    return staticMain;
  }

  public Main getWithSideEffects() {
    sideEffectCount++;
    return this;
  }

  public static void main(String... args) {
    Main main = new Main();
    staticMain = main;
    int i = 0;

    // Simplest.
    {
      staticField = 50;
      assert staticField == 50;
    }

    // Simple inline.
    {
      main.staticField = 100;
      assert main.staticField == 100;
      assert main.getStaticValue() == 100;
    }

    // Right hand side assignment.
    {
      i = main.staticField;
      assert i == 100;
      i = main.getStaticValue();
      assert i == 100;
    }

    // Left hand side, simple assignment and side effects
    {
      {
        main.getWithSideEffects().staticField = 200;
        assert staticField == 200;
        assert sideEffectCount == 1;
      }
      {
        main.getWithSideEffects().getStaticMain().staticField = 300;
        assert staticField == 300;
        assert sideEffectCount == 2;
      }
    }

    // Left hand side, compound assignment and side effects
    {
      {
        main.getWithSideEffects().staticField += 100;
        assert staticField == 400;
        assert sideEffectCount == 3;
      }
      {
        main.getWithSideEffects().getStaticMain().staticField += 100;
        assert staticField == 500;
        assert sideEffectCount == 4;
      }
    }
    
    // Test side effects on binary operator
    {
      {
        main.getWithSideEffects().staticField++;
        assert staticField == 501;
        assert sideEffectCount == 5;
      }
      {
        main.getWithSideEffects().getStaticMain().staticField++;
        assert staticField == 502;
        assert sideEffectCount == 6;
      }
    }

    // Stress
    {
      main.getWithSideEffects()
              .getStaticMain()
              .getWithSideEffects()
              .getStaticMain()
              .getWithSideEffects()
              .getStaticMain()
              .staticField +=
          main.getWithSideEffects()
              .getStaticMain()
              .getWithSideEffects()
              .getStaticMain()
              .getWithSideEffects()
              .getStaticMain()
              .staticField;
      assert staticField == 1004;
      assert sideEffectCount == 12;
    }
  }
}
