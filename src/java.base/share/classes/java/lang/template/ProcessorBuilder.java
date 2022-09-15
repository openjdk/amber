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

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import jdk.internal.javac.PreviewFeature;

/**
 * This builder class can be used to simplify the construction of a
 * {@link StringProcessor} or {@link SimpleProcessor}.
 * <p>
 * The user starts by creating a new instance of this class
 * using {@link java.lang.template.TemplateProcessor#builder()}. When the user is finished
 * composing the processor then they should invoke {@link ProcessorBuilder#build()}
 * on the instance returning a new instance of {@link StringProcessor}.
 * <p>
 * The {@link ProcessorBuilder#fragment} and {@link ProcessorBuilder#value}
 * methods can be used to validate and map the
 * {@link TemplatedString TemplatedString's} fragments and values.
 * <p>
 * Most instance methods in {@link ProcessorBuilder} return {@code this}
 * {@link ProcessorBuilder}, so it is possible to chain the methods.
 * Example: {@snippet :
 *      StringProcessor processor = TemplateProcessor.builder()
 *          .fragment(f -> f.toUpperCase())
 *          .value(v -> v instanceof Integer i ? Math.abs(i) : v)
 *          .build();
 * }
 * The {@link ProcessorBuilder#preliminary} method allows the processor to validate
 * and map the source {@link TemplatedString}.
 * <p>
 * The {@link ProcessorBuilder#format} method allows the processor to format
 * values. The {@code marker} specifies the string of characters marking the
 * beginning of a format specifier. The specifier is then passed to a
 * {@link BiFunction} along with the value to format.
 * Example: {@snippet :
 *      StringProcessor processor = TemplateProcessor.builder()
 *          .format("%", (specifier, value) ->
 *                  justify(String.valueOf(value), Integer.parseInt(specifier)))
 *          .build();
 *      int x = 10;
 *      int y = 12345;
 *      String result = processor.apply("%4\{x} + %4\{y} = %-5\{x + y}");
 * }
 * Output: {@code "  10 + **** = 12355"}
 * <p>
 * The {@link ProcessorBuilder#resolve} method adds a value map that resolves
 * {@link Supplier}, {@link Future} and {@link FutureTask} values before
 * passing resolve values on to the next value map.
 * <p>
 * The alternate form of {@link ProcessorBuilder#build(Function)} allows the
 * user to transform the {@link ProcessorBuilder} result to something other than
 * a (@link String}.
 * Example: {@snippet :
 *      SimpleProcessor<JSONObject> processor = TemplateProcessor.builder()
 *          .build(s -> new JSONObject(s));
 * }
 *
 * @implNote Due to the nature of lambdas, validating functions can only throw
 * unchecked exceptions, ex. {@link RuntimeException}.
 *
 * @since 20
 */
@PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
public class ProcessorBuilder {
	/**
	 * True if the processor is a simple concatenation processor. The default
	 * is {@code true}. This field gets set to false if any of the state is
	 * changed.
	 */
	private boolean isSimple;

	/**
	 * Function that can validate and map the source {@link TemplatedString}.
	 */
	private Function<TemplatedString, TemplatedString> preliminary;

	/**
	 * Function that can validate and map each fragment.
	 */
	private Function<String, String> mapFragment;

	/**
	 * Function that can validate and map each value.
	 */
	private Function<Object, Object> mapValue;

	/**
	 * String that marks the beginning of a format specifier. The
	 * default is the empty string indicating no formatting.
	 */
	private String marker;

	/**
	 * Binary function that can format the value using a user defined
	 * specifier.
	 */
	private BiFunction<String, Object, String> formatValue;

	/**
	 * Package-private constructor.
	 */
	ProcessorBuilder() {
		clear();
	}

	/**
	 * Resets the builder to the initial state.
	 *
	 * @return this Builder
	 */
	public ProcessorBuilder clear() {
		this.isSimple = true;
		this.preliminary = Function.identity();
		this.mapFragment = Function.identity();
		this.mapValue = Function.identity();
		this.marker = "";
		this.formatValue = (specifier, value) -> String.valueOf(value);

		return this;
	}

	/**
	 * Augment the function that is used to validate and map
	 * the source {@link TemplatedString}. Each successive call adds
	 * to the mapping, chaining the result from the previous function. The
	 * initial function is the identity function.
	 * Example: {@snippet :
	 *     StringProcessor processor = TemplateProcessor.builder()
	 *          .preliminary(ts -> {
	 *               List<String> fragments = ts.fragments().map(s -> s.toUpperCase()).toList();
	 *               List<Object> values = ts.values();
	 *               return TemplatedString.of(fragments, values);
	 *          })
	 *          .build();
	 * }
	 *
	 * @param preliminary  {@link Function} used to validate and map the source
	 *                     {@link TemplatedString}.
	 *
	 * @return this {@link ProcessorBuilder} instance
	 */
	public ProcessorBuilder preliminary(Function<TemplatedString, TemplatedString> preliminary) {
		Objects.requireNonNull(preliminary, "preliminary must not be null");
		final Function<TemplatedString, TemplatedString> oldPreliminary = this.preliminary;
		this.preliminary = ts -> preliminary.apply(oldPreliminary.apply(ts));
		this.isSimple = false;

		return this;
	}

	/**
	 * Augment the function that is used to validate and map
	 * {@link TemplatedString} fragments. Each successive call adds
	 * to the mapping, chaining the result from the previous function. The
	 * initial function is the identity function.
	 * Example: {@snippet :
	 *     StringProcessor processor = TemplateProcessor.builder()
	 *          .fragment(f -> f.toUpperCase())
	 *          .build();
	 * }
	 *
	 * @param mapFragment {@link Function} used to validate and map the fragment
	 *
	 * @return this {@link ProcessorBuilder} instance
	 */
	public ProcessorBuilder fragment(Function<String, String> mapFragment) {
		Objects.requireNonNull(mapFragment, "mapFragment must not be null");
		final Function<String, String> oldMapFragment = this.mapFragment;
		this.mapFragment = fragment ->
				mapFragment.apply(oldMapFragment.apply(fragment));
		this.isSimple = false;

		return this;
	}

	/**
	 * Augment the function that is used to validate and map
	 * {@link TemplatedString} values. Each successive call adds
	 * to the mapping, chaining the result from the previous function. The
	 * initial function is the identity function.
	 * Example: {@snippet :
	 *     StringProcessor processor = TemplateProcessor.builder()
	 *          .value(v -> v instanceof Integer i ? Math.abs(i) : v)
	 *          .build();
	 * }
	 *
	 * @param mapValue {@link Function} used to validate and map the fragment
	 *
	 * @return this {@link ProcessorBuilder} instance
	 */
	public ProcessorBuilder value(Function<Object, Object> mapValue) {
		Objects.requireNonNull(mapValue, "mapValue must not be null");
		final Function<Object, Object> oldMapValue = this.mapValue;
		this.mapValue = v -> mapValue.apply(oldMapValue.apply(v));
		this.isSimple = false;

		return this;
	}

	/**
	 * Augment the function that is used to validate and map
	 * {@link TemplatedString} values by adding a function that resolves
	 * {@link Supplier}, {@link Future} and {@link FutureTask} values before
	 * passing on to next map value function.
	 * Example: {@snippet :
	 *     StringProcessor processor = TemplateProcessor.builder()
	 *          .resolve()
	 *          .build();
	 * }
	 *
	 * @return this {@link ProcessorBuilder} instance
	 *
	 * @implNote The resolving function will throw a {@link RuntimeException}
	 * if a (@link Future} throws during resolution.
	 */
	public ProcessorBuilder resolve() {
		return value(value -> {
			if (value instanceof Future<?> future) {
				if (future instanceof FutureTask<?> task) {
					task.run();
				}

				try {
					return future.get();
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			} else if (value instanceof Supplier<?> supplier) {
				return supplier.get();
			}

			return value;
		});
	}

	/**
	 * Add a final formatting step before a value is added to
	 * the processor result. The {@code marker} string is used to locate any
	 * format specifier in the fragment prior to the value. The specifier
	 * is removed from the fragment and passed to the {@code formatValue}
	 * binary function along with the value. The {@code marker} is not
	 * included in the specifier.
	 * Example: {@snippet :
	 *      StringProcessor processor = TemplateProcessor.builder()
	 *          .format("%", (specifier, value) ->
	 *                  justify(String.valueOf(value), Integer.parseInt(specifier)))
	 *          .build();
	 * }
	 *
	 * @param marker       String used to mark the beginning of a format specifier
	 *                     or {@code ""} to indicate no format processing.
	 * @param formatValue  {@link BiFunction} used to format the value
	 *
	 * @return this {@link ProcessorBuilder} instance
	 */
	public ProcessorBuilder format(String marker, BiFunction<String, Object, String> formatValue) {
		this.marker = Objects.requireNonNull(marker, "marker must not be null");
		this.formatValue = Objects.requireNonNull(formatValue, "doFormat must not be null");
		this.isSimple = false;

		return this;
	}

	/**
	 * Construct a new {@link StringProcessor} using elements added to the builder.
	 * The processor will initially run the preliminary map function on the source
	 * {@link TemplatedString}. The processor will then iterate through fragments
	 * and values, applying the fragment map function and value map function
	 * on each iteration. If a format function is supplied, the value will be
	 * formatted before adding to the result. The result of the processor will
	 * be a string composed the mapped fragments and values.
	 * Example: {@snippet :
	 *      StringProcessor processor = TemplateProcessor.builder()
	 *          .build();
	 * }
	 *
	 * @return a new {@link StringProcessor} instance
	 */
	public StringProcessor build() {
		final Function<TemplatedString, TemplatedString> preliminary = this.preliminary;
		final Function<String, String> mapFragment = this.mapFragment;
		final Function<Object, Object> mapValue = this.mapValue;
		final String marker = this.marker;
		final BiFunction<String, Object, String> formatValue = this.formatValue;

		return isSimple ? ts -> ts.concat()
				: ts -> {
			ts = preliminary.apply(ts);
			List<String> fragments = ts.fragments();
			List<Object> values = ts.values();
			StringBuilder sb = new StringBuilder();
			Iterator<String> fragmentIterator = fragments.iterator();
			Iterator<Object> valuesIterator = values.iterator();

			if (!marker.isEmpty()) {
				while(valuesIterator.hasNext()) {
					String fragment = mapFragment.apply(fragmentIterator.next());
					Object value = mapValue.apply(valuesIterator.next());
					int index = fragment.indexOf(marker);

					if (index != -1) {
						String specifier = fragment.substring(index + marker.length());
						fragment = fragment.substring(0, index);
						value = formatValue.apply(specifier, value);
					}

					sb.append(fragment);
					sb.append(value);
				}
			} else {
				while(valuesIterator.hasNext()) {
					sb.append(mapFragment.apply(fragmentIterator.next()));
					sb.append(mapValue.apply(valuesIterator.next()));
				}
			}

			sb.append(mapFragment.apply(fragmentIterator.next()));

			return sb.toString();
		};
	}

	/**
	 * This version of build performs the same actions as
	 * {@link ProcessorBuilder#build()} but always for a final transformation
	 * step to convert the string result to a non-string result.
	 * Example: {@snippet :
	 * }
	 *
	 * @param transform {@link Function} used to map the string result
	 *                  to a non-string result.
	 *
	 * @return a new {@link SimpleProcessor} instance
	 *
	 * @param <R> type of result
	 */
	public <R> SimpleProcessor<R> build(Function<String, R> transform) {
		Objects.requireNonNull(transform, "transform must not be null");

		return ts -> transform.apply(build().apply(ts));
	}

}
