/*
 * Copyright 2022 Google Inc.
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
package exoticidentifiers

fun `foo-bar`() {}

// TODO(b/228454104): Conflicts with `foo-bar`
// fun `foo$bar`() {}

fun `bar$buzz`() {}

fun foo_bar() {}

// TODO(b/228454104): Sanitize this identifier
// fun `2times`() {}
// TODO(b/228454104): Sanitize this identifier
// fun `foo!bar`() {}
// TODO(b/228454104): Sanitize this identifier
// fun `foo%bar`() {}
// TODO(b/228454104): Sanitize this identifier
// fun `¯|_(ツ)_|¯`() {}
