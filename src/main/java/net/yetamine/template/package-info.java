/*
 * Copyright 2017 Yetamine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Provides a set of interfaces and their default implementations for dealing
 * with simple string templates.
 *
 * <p>
 * A simple string template consists of three types of components:
 *
 * <ul>
 * <li><i>literals</i> that have the same representation and value after
 * resolving the template,</li>
 * <li><i>constants</i> that always resolve to the same value regardless of the
 * resolution context, but their representation differs from their values (that
 * distinguishes them from literals),</li>
 * <li><i>references</i> to resolution context that resolve to values dependent
 * on the context (i.e., they have just unresolved values and need resolving to
 * provide any definite values).</li>
 * </ul>
 *
 * An example of a template could be a string like <i>This is ${what} that may
 * contain such $${placeholders}.</i> Here, <i>${what}</i> is a reference, but
 * <i>$${placeholder}</i> is a constant. The rest if the string is composed of
 * literals.
 */
package net.yetamine.template;
