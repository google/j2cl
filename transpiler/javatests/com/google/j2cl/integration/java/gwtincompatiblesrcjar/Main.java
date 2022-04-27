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
package gwtincompatiblesrcjar;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.io.Files;
import java.text.SimpleDateFormat;

public class Main {
  public static void main(String... args) {
    compatible();
    Other other = new Other();
    External ext = new External();
  }

  public static void compatible() {
    SomeEnum a = SomeEnum.COMPATIBLE;
  }

  @GwtIncompatible
  public static void incompatible() {
    SomeEnum a = SomeEnum.INCOMPATIBLE;
  }

  enum SomeEnum {
    COMPATIBLE {
      @Override
      void method() {}
    },

    @GwtIncompatible
    INCOMPATIBLE {
      @Override
      void method() {
        SimpleDateFormat unused = new SimpleDateFormat();
      }
    };

    abstract void method();
  }

  @GwtIncompatible
  public static Object incompatibleMissingInDep() throws Exception {
    return Files.toByteArray(null);
  }
}
