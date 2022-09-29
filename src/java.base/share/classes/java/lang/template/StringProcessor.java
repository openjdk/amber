/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang.template;

import java.util.Arrays;
import java.util.Objects;

import jdk.internal.javac.PreviewFeature;

/**
 * This interface simplifies declaration of
 * {@link java.lang.template.TemplateProcessor TemplateProcessors}
 * that do not throw checked exceptions and have a result type of {@link String}. For example:
 * {@snippet :
 * StringProcessor processor = ts -> ts.interpolate();
 * }
 *
 * @since 20
 */
@PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
@FunctionalInterface
public interface StringProcessor extends SimpleProcessor<String> {
	/**
	 * Chain template processors to produce a new processor that applies the supplied
	 * processors from right to left. The {@code head} processor is a {@link StringProcessor}
	 * The {@code tail} processors must return type {@link TemplatedString}.
	 *
	 * @param head  last {@link StringProcessor} to be applied, return type {@link String}
	 * @param tail  first processors to process, return type {@code TemplatedString}
	 *
	 * @return a new {@link StringProcessor} that applies the supplied processors
	 *         from right to left
	 *
	 * @throws NullPointerException if any of the arguments is null.
	 */
	@SuppressWarnings("varargs")
	@SafeVarargs
	public static StringProcessor
	chain(StringProcessor head,
		  TemplateProcessor<TemplatedString, RuntimeException>... tail) {
		Objects.requireNonNull(head, "head must not be null");
		Objects.requireNonNull(tail, "tail must not be null");

		if (tail.length == 0) {
			return head;
		}

		TemplateProcessor<TemplatedString, RuntimeException> last =
				TemplateProcessor.chain(tail[0], Arrays.copyOfRange(tail, 1, tail.length));

		return ts -> head.process(last.process(ts));
	}
}
