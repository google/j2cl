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

public class InstanceQualifiers {
  public static int staticField = 100;
  public static int sideEffectCount = 0;

  public static int getStaticValue() {
    return staticField;
  }

  public static InstanceQualifiers getStaticInstanceQualifiers() {
    return null;
  }

  public static void main(String... args) {
    InstanceQualifiers main = new InstanceQualifiers();
    int i = 0;

    {
      staticField = 100;
    }

    {
      main.staticField = 100;
      main.getStaticInstanceQualifiers().staticField = 300;
    }

    {
      i = main.staticField;
      i = main.getStaticValue();
      i = main.getStaticInstanceQualifiers().staticField;
    }

    {
      main.staticField += 100;
      main.getStaticInstanceQualifiers().staticField += 100;
    }

    {
      main.getStaticInstanceQualifiers().getStaticInstanceQualifiers().staticField +=
          main.getStaticInstanceQualifiers().getStaticInstanceQualifiers().getStaticValue();
    }
  }
}
