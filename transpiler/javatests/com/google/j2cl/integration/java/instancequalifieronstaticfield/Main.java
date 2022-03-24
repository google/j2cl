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
package instancequalifieronstaticfield;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

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
      assertTrue(staticField == 50);
    }

    // Simple inline.
    {
      main.staticField = 100;
      assertTrue(main.staticField == 100);
      assertTrue(main.getStaticValue() == 100);
    }

    // Right hand side assignment.
    {
      i = main.staticField;
      assertTrue(i == 100);
      i = main.getStaticValue();
      assertTrue(i == 100);
    }

    // Left hand side, simple assignment and side effects
    {
      {
        main.getWithSideEffects().staticField = 200;
        assertTrue(staticField == 200);
        assertTrue(sideEffectCount == 1);
      }
      {
        main.getWithSideEffects().getStaticMain().staticField = 300;
        assertTrue(staticField == 300);
        assertTrue(sideEffectCount == 2);
      }
    }

    // Left hand side, compound assignment and side effects
    {
      {
        main.getWithSideEffects().staticField += 100;
        assertTrue(staticField == 400);
        assertTrue(sideEffectCount == 3);
      }
      {
        main.getWithSideEffects().getStaticMain().staticField += 100;
        assertTrue(staticField == 500);
        assertTrue(sideEffectCount == 4);
      }
    }
    
    // Test side effects on binary operator
    {
      {
        main.getWithSideEffects().staticField++;
        assertTrue(staticField == 501);
        assertTrue(sideEffectCount == 5);
      }
      {
        main.getWithSideEffects().getStaticMain().staticField++;
        assertTrue(staticField == 502);
        assertTrue(sideEffectCount == 6);
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
      assertTrue(staticField == 1004);
      assertTrue(sideEffectCount == 12);
    }
  }
}
