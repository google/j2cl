/*
 * Copyright 2022 Google Inc.
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
package javakotlininterop;

/** Super classes in Java to test the bridge construction algorithms. */
public class FromJava {

  interface Consumer<T> {
    default String consume(T t) {
      return "Consumer.consume default";
    }
  }

  public abstract static class AbstractConsumer<T> implements Consumer<T> {
    @Override
    public String consume(T t) {
      return "AbstractConsumer.consume(T)";
    }
  }

  public static class TConsumer<T extends Integer> extends AbstractConsumer<T>
      implements Consumer<T> {
    @Override
    public String consume(T t) {
      return "TConsumer.consume(T)";
    }

    public String consume(int i) {
      return "TConsumer.consume(int)";
    }
  }
}
