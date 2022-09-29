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

import java.lang.invoke.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import jdk.internal.javac.PreviewFeature;

/**
 * This builder class can be used to simplify the construction of a
 * {@link StringProcessor} or {@link SimpleProcessor}.
 * <p>
 * The user starts by creating a new instance of this class
 * using {@link TemplateProcessor#builder()}. When the user is finished
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
 *          .fragment(String::toUpperCase)
 *          .value(v -> v instanceof Integer i ? Math.abs(i) : v)
 *          .build();
 *}
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
 *      String result = processor.process("%4\{x} + %4\{y} = %-5\{x + y}");
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
 * @since 20
 */
@PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
public class ProcessorBuilder {
	/**
	 * {@link MethodHandle} to {@link Function#apply}.
	 */
	private static final MethodHandle FUNCTION_APPLY_MH;

	/**
	 * {@link MethodHandle} to {@link ProcessorBuilder#apply}.
	 */
	private static final MethodHandle LIST_APPLY_MH;

	/**
	 * {@link MethodHandle} to {@link TemplatedString#fragments}.
	 */
	private static final MethodHandle FRAGMENTS_MT;

	/**
	 * {@link MethodHandle} to {@link TemplatedString#values}.
	 */
	private static final MethodHandle VALUES_MT;

	/**
	 * {@link MethodHandle} to {@link TemplateRuntime#interpolate}.
	 */
	private static final MethodHandle INTERPOLATE_MH;

	/**
	 * {@link MethodHandle} to {@link ProcessorBuilder#format}.
	 */
	private static final MethodHandle FORMAT_MH;

	/*
	 * Initialize {@link MethodHandle MethodHandles}.
	 */
	static {
		try {
			MethodHandles.Lookup lookup = MethodHandles.lookup();

			MethodType mt = MethodType.methodType(Object.class,
					Object.class);
			FUNCTION_APPLY_MH = lookup.findVirtual(Function.class, "apply", mt);
			mt = MethodType.methodType(List.class, Function.class, List.class);
			LIST_APPLY_MH = lookup.findStatic(ProcessorBuilder.class, "apply", mt);
			mt = MethodType.methodType(List.class);
			FRAGMENTS_MT = lookup.findVirtual(TemplatedString.class, "fragments", mt);
			VALUES_MT = lookup.findVirtual(TemplatedString.class, "values", mt);
			mt = MethodType.methodType(String.class,
					List.class, List.class);
			INTERPOLATE_MH = lookup.findStatic(TemplateRuntime.class, "interpolate", mt);
			mt = MethodType.methodType(String.class,
					String.class, BiFunction.class, List.class, List.class);
			FORMAT_MH = lookup.findStatic(ProcessorBuilder.class, "format", mt);
		} catch (ReflectiveOperationException ex) {
			throw new AssertionError("template processor builder fail", ex);
		}
	}

	/**
	 * Function that can validate and map the {@link TemplatedString}.
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
	 * A binary function that can format the value using a user defined
	 * specifier.
	 */
	private BiFunction<String, Object, String> formatter;

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
		this.preliminary = null;
		this.mapFragment = null;
		this.mapValue = null;
		this.marker = "";
		this.formatter = null;

		return this;
	}

	/**
	 * Augment the function that is used to validate and map
	 * the source {@link TemplatedString}. Each successive call adds
	 * to the mapping, chaining the result from the previous function. The
	 * initial function is the identity function.
	 * Example: {@snippet :
	 * StringProcessor processor = TemplateProcessor.builder()
	 *      .preliminary(ts -> {
	 *           List<String> fragments = ts.fragments().map(s -> s.toUpperCase()).toList();
	 *           List<Object> values = ts.values();
	 *           return TemplatedString.of(fragments, values);
	 *      })
	 *      .build();
	 * }
	 * @param function  {@link Function} used to validate and map the source
	 *                  {@link TemplatedString}.
	 *
	 * @return this {@link ProcessorBuilder} instance
	 */
	public ProcessorBuilder preliminary(Function<TemplatedString, TemplatedString> function) {
		Objects.requireNonNull(function, "function must not be null");
		preliminary = preliminary == null ? function : function.compose(preliminary);

		return this;
	}

	/**
	 * Augment the function that is used to validate and map
	 * {@link TemplatedString} fragments. Each successive call adds
	 * to the mapping, chaining the result from the previous function. The
	 * initial function is the identity function.
	 * Example: {@snippet :
	 * StringProcessor processor = TemplateProcessor.builder()
	 *      .fragment(String::toUpperCase)
	 *      .build();
	 *}
	 * @param function {@link Function} used to validate and map the fragment
	 *
	 * @return this {@link ProcessorBuilder} instance
	 */
	public ProcessorBuilder fragment(Function<String, String> function) {
		Objects.requireNonNull(function, "function must not be null");
		mapFragment = mapFragment == null ? function : function.compose(mapFragment);

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
	 * @param function {@link Function} used to validate and map the fragment
	 *
	 * @return this {@link ProcessorBuilder} instance
	 */
	public ProcessorBuilder value(Function<Object, Object> function) {
		Objects.requireNonNull(function, "function must not be null");
		mapValue = mapValue == null ? function : function.compose(mapValue);

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
	 * StringProcessor processor = TemplateProcessor.builder()
	 *     .format("%", (specifier, value) ->
	 *             justify(String.valueOf(value), Integer.parseInt(specifier)))
	 *     .build();
	 * }
	 *
	 * @param marker       String used to mark the beginning of a format specifier
	 *                     or {@code ""} to indicate no format processing.
	 * @param formatValue  {@link BiFunction} used to format the value
	 *
	 * @return this {@link ProcessorBuilder} instance
	 */
	public ProcessorBuilder formatter(String marker, BiFunction<String, Object, String> formatValue) {
		this.marker = Objects.requireNonNull(marker, "marker must not be null");
		this.formatter = Objects.requireNonNull(formatValue, "doFormat must not be null");

		if (marker.isEmpty()) {
			throw new RuntimeException("marker must not be empty");
		}

		return this;
	}

	/**
	 * Apply a transform function to a list of elements.
	 *
	 * @param function  transformation function
	 * @param elements  elements to be transformed
	 *
	 * @return transformed list
	 *
	 * @param <T> type before transformation
	 * @param <R> type after transformation
	 */
	private static <T, R> List<R> apply(Function<T, R> function, List<T> elements) {
		List<R> result = new ArrayList<>(elements.size());
		for (T element : elements) {
			result.add(function.apply(element));
		}
		return result;
	}

	/**
	 * Format values based on a specification found in the fragment.
	 *
	 * @param marker       string indicating the beginning of a specification
	 * @param formatValue  function that formats the value
	 * @param fragments    source of specification
	 * @param values       values to be formatted
	 *
	 * @return formatted interpolation
	 */
	private static String format(String marker, BiFunction<String, Object, String> formatValue,
								 List<String> fragments, List<Object> values) {
		int size = fragments.size();
		List<String> newFragments = new ArrayList<>(size);
		List<Object> newValues = new ArrayList<>(size - 1);
		Iterator<String> fragmentsIter = fragments.iterator();

		for (Object value : values) {
			String fragment = fragmentsIter.next();
			int index = fragment.lastIndexOf(marker);
			String specifier = "";

			if (index != -1) {
				specifier = fragment.substring(index + marker.length());
				fragment = fragment.substring(0, index);
			}

			value = formatValue.apply(specifier, value);
			newFragments.add(fragment);
			newValues.add(value);
		}
		newFragments.add(fragmentsIter.next());

		return TemplateRuntime.interpolate(newFragments, newValues);
	}

	/**
	 * {@return true if processor is simple enough to use interpolation.
	 */
	private boolean isSimple() {
		return preliminary == null &&
				mapFragment == null &&
				mapValue == null &&
				formatter == null;
	}

	/**
	 * Build an process {@link MethodHandle} for the processor.
	 *
	 * @param transform function to transform interpolation
	 *
	 * @return {@link MethodHandle} incorporating processor specifications.
	 */
	private <R> MethodHandle processMethodHandle(Function<String, R> transform) {
		MethodType mt;
		MethodHandle mh;

		// Test for formatter presence.
		if (formatter == null) {
			// Just interpolation
			mh = INTERPOLATE_MH;
		} else {
			// Formatting interpolation
			mh = MethodHandles.insertArguments(FORMAT_MH, 0, marker, formatter);
		}

		// If transformation present then transform interpolation to final result.
		if (transform != null) {
			mt = MethodType.methodType(Object.class, String.class);
			mh = MethodHandles.filterReturnValue(mh, FUNCTION_APPLY_MH.bindTo(transform).asType(mt));
		}

		// Map values if function present.
		if (mapValue != null) {
			mh = MethodHandles.filterArguments(mh, 1, LIST_APPLY_MH.bindTo(mapValue));
		}

		// Map fragments if function present.
		if (mapFragment != null) {
			mh = MethodHandles.filterArguments(mh, 0, LIST_APPLY_MH.bindTo(mapFragment));
		}

		// Get fragments and values from TemplatedString
		mh = MethodHandles.filterArguments(mh, 0, FRAGMENTS_MT, VALUES_MT);

		// Spread copies of TemplatedString to each getter.
		mt = MethodType.methodType(mh.type().returnType(), TemplatedString.class);
		mh = MethodHandles.permuteArguments(mh, mt, 0, 0);

		// Preliminary processing if preliminary function present.
		if (preliminary != null) {
			mt = MethodType.methodType(TemplatedString.class, TemplatedString.class);
			mh = MethodHandles.filterArguments(mh, 0,  FUNCTION_APPLY_MH.bindTo(preliminary).asType(mt));
		}

		return mh;
	}

	/**
	 * Construct a new {@link SimpleProcessor} using elements added to the builder.
	 * The processor will initially run the preliminary map functions on the source
	 * {@link TemplatedString}. The processor will then iterate through fragments
	 * and values, applying the fragment map functions and value map functions
	 * on each iteration. If a formatter function is supplied, the value will be
	 * formatted before adding to a string result. The string result will be
	 * go through a final transformation with the transform function.
	 * Example: {@snippet :
	 *      SimpleProcessor<String> processor = TemplateProcessor.builder()
	 *          .build(String::toUpperCase);
	 *}
	 * @param transform {@link Function} used to map the string result
	 *                  to a non-string result.
	 * @param <R> type of processor result
	 *
	 * @return a new {@link SimpleProcessor} instance
	 */
	@SuppressWarnings("unchecked")
	public <R> SimpleProcessor<R> build(Function<String, R> transform) {
		Objects.requireNonNull(transform, "transform must not be null");

		if (isSimple()) {
			return ts-> transform.apply(ts.interpolate());
		}

		MethodHandle processMH = processMethodHandle(transform);

		return ts -> {
			try {
				return (R)processMH.invokeExact(ts);
			} catch (Throwable ex) {
				throw new RuntimeException(ex);
			}
		};
	}

	/**
	 * Construct a new {@link StringProcessor} using elements added to the builder.
	 * The processor will initially run the preliminary map functions on the source
	 * {@link TemplatedString}. The processor will then iterate through fragments
	 * and values, applying the fragment map functions and value map functions
	 * on each iteration. If a formatter function is supplied, the value will be
	 * formatted before adding to the string result.
	 * Example: {@snippet :
	 *      StringProcessor processor = TemplateProcessor.builder()
	 *          .build();
	 *}
	 * @return a new {@link SimpleProcessor} instance
	 */
	public StringProcessor build() {
		if (isSimple()) {
			return TemplatedString.STR;
		}

		MethodHandle processMH = processMethodHandle(null);

		return ts -> {
			try {
				return (String)processMH.invokeExact(ts);
			} catch (Throwable ex) {
				throw new RuntimeException(ex);
			}
		};
	}

}
