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
package iteratormethodresolution;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import java.util.Iterator;

/**
 * Test for iterator method resolution
 */
public class Main {

  public interface MyList extends Iterable<String> {}

  private static class Base implements MyList {

    private String[] content = new String[] {"1", "2", "3"};

    @Override
    public Iterator<String> iterator() {
      return new Iterator<String>() {
        int current = 0;

        @Override
        public boolean hasNext() {
          return current < content.length;
        }

        @Override
        public String next() {
          int last = current;
          current++;
          return content[last];
        }

        @Override
        public void remove() {
          throw new RuntimeException();
        }
      };
    }
  }

  private static class Concrete extends Base {
    // does not have an implementation of iterator()
  }

  public static void main(String... args) {
    int count = 1;
    for (String string : new Concrete()) {
      assertTrue(string.equals(String.valueOf(count)));
      count++;
    }

    // Refer to the type by interface
    MyList myList = new Concrete();
    count = 1;
    for (String string : myList) {
      assertTrue(string.equals(String.valueOf(count)));
      count++;
    }
  }
}
