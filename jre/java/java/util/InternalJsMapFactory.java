/*
 * Copyright 2014 Google Inc.
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

/**
 * A factory to create JavaScript Map instances.
 */
// TODO(dankurka): This type is a copy from GWT's version since the GWT version still uses
// JavaScriptObject, as soon as we can use JsInterop in GWT code these types need to get united
// again. For now these files are just placeholders and need to be updated to be actually runable
// with j2cl.
class InternalJsMapFactory {

  public static native <V> InternalJsMap<V> newJsMap() /*-{
    return new @InternalJsMapFactory::jsMapCtor;
  }-*/;

  static class InternalJsIterator<V> {
    protected InternalJsIterator() { }
    public final native InternalJsIteratorEntry<V> next() /*-{ return this.next(); }-*/;
  }

  static class InternalJsIteratorEntry<V> {
    protected InternalJsIteratorEntry() { }
    public final native boolean done() /*-{ return this.done; }-*/;
    public final native String getKey() /*-{ return this.value[0]; }-*/;
    public final native V getValue() /*-{ return this.value[1]; }-*/;
  }

  static class InternalJsMap<V> {
    protected InternalJsMap() { }
    public final native V get(int key) /*-{ return this.get(key); }-*/;
    public final native V get(String key) /*-{ return this.get(key); }-*/;
    public final native void set(int key, V value) /*-{ this.set(key, value); }-*/;
    public final native void set(String key, V value) /*-{ this.set(key, value); }-*/;
    // Calls delete via brackets to be workable with polyfills
    public final native void delete(int key) /*-{ this['delete'](key); }-*/;
    public final native void delete(String key) /*-{ this['delete'](key); }-*/;
    public final native InternalJsIterator<V> entries() /*-{ return this.entries(); }-*/;
  }

  private InternalJsMapFactory() {
    // Hides the constructor.
  }
}

