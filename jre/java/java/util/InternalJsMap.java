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
package java.util;

class InternalJsMap<V> {
  protected InternalJsMap() { }
  public final  V get(int key) {return null;}/*-{ return this.get(key); }-*/;
  public final  V get(String key) {return null;}/*-{ return this.get(key); }-*/;
  public final  void set(int key, V value) {}/*-{ this.set(key, value); }-*/;
  public final  void set(String key, V value) {}/*-{ this.set(key, value); }-*/;
  // Calls delete via brackets to be workable with polyfills
  public final  void delete(int key) {}/*-{ this['delete'](key); }-*/;
  public final  void delete(String key) {}/*-{ this['delete'](key); }-*/;
  public final  InternalJsIterator<V> entries() {return null;}/*-{ return this.entries(); }-*/;
}
