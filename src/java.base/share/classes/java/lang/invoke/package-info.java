/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * The {@code java.lang.invoke} package contains dynamic language support provided directly by
 * the Java core class libraries and virtual machine.
 *
 * <p>
 * As described in the Java Virtual Machine Specification,
 * certain types in this package have special relations to dynamic
 * language support in the virtual machine:
 * <ul>
 * <li>The classes {@link java.lang.invoke.MethodHandle MethodHandle}
 * {@link java.lang.invoke.VarHandle VarHandle} contain
 * <a href="MethodHandle.html#sigpoly">signature polymorphic methods</a>
 * which can be linked regardless of their type descriptor.
 * Normally, method linkage requires exact matching of type descriptors.
 * </li>
 *
 * <li>The JVM bytecode format supports immediate constants of
 * the classes {@link java.lang.invoke.MethodHandle MethodHandle} and {@link java.lang.invoke.MethodType MethodType}.
 * </li>
 * </ul>
 *
 * <h1><a id="jvm_mods"></a>Summary of relevant Java Virtual Machine changes</h1>
 * The following low-level information summarizes relevant parts of the
 * Java Virtual Machine specification.  For full details, please see the
 * current version of that specification.
 *
 * Each occurrence of an {@code invokedynamic} instruction is called a <em>dynamic call site</em>.
 * Each occurrence of an {@code CONSTANT_ConstantDynamic} constant pool entry is called a <em>dynamic constant</em>.
 *
 * <h2><a id="indyinsn"></a>{@code invokedynamic} instructions</h2>
 * Bytecode may contain <em>dynamic call sites</em> equipped with
 * bootstrap methods which perform their resolution.
 * A dynamic call site is originally in an unlinked state.  In this state, there is
 * no target method for the call site to invoke.
 * <p>
 * Before the JVM can execute a dynamic call site (an {@code invokedynamic} instruction),
 * the call site must first be <em>linked</em>.
 * Linking is accomplished by calling a <em>bootstrap method</em>
 * which is given the static information content of the call site,
 * and which must produce a {@link java.lang.invoke.MethodHandle method handle}
 * that gives the behavior of the call site.
 * <p>
 * Each {@code invokedynamic} instruction statically specifies its own
 * bootstrap method as a constant pool reference.
 * The constant pool reference also specifies the call site's name and method type descriptor,
 * just like {@code invokestatic} and the other invoke instructions.
 *
 * <h2><a id="condycon"></a>constants with tag {@code CONSTANT_ConstantDynamic}</h2>
 * The constant pool may contain constants tagged {@code CONSTANT_ConstantDynamic},
 * equipped with bootstrap methods which perform their resolution.
 * Such a <em>dynamic constant</em> is originally in an unresolved state.
 * Before the JVM can evaluate a dynamic constant, it must first be <em>resolved</em>.
 * Dynamic constant resolution is accomplished by calling a <em>bootstrap method</em>
 * which is given the static information content of the constant,
 * and which must produce a value of the constant's statically declared type.
 * <p>
 * Each {@code CONSTANT_ConstantDynamic} constant statically specifies its own
 * bootstrap method as a constant pool reference.
 * The constant pool reference also specifies the constant's name and field type descriptor,
 * just like {@code getstatic} and the other field reference instructions.
 * (Roughly speaking, a dynamic constant is to a dynamic call descriptor
 * as a {@code CONSTANT_Fieldref} is to a {@code CONSTANT_Methodref}.)
 *
 * <h2><a id="bsm"></a>execution of bootstrap methods</h2>
 * Linking a dynamic call site or dynamic constant
 * starts with resolving constants from the constant pool for the
 * following items:
 * <ul>
 * <li>the bootstrap method, either a {@code CONSTANT_MethodHandle}
 * or a {@code CONSTANT_ConstantDynamic} entry</li>
 * <li>if linking a dynamic call site, the {@code MethodType} derived from
 * type component of the {@code CONSTANT_NameAndType} descriptor of the call</li>
 * <li>if linking a dynamic constant, the {@code Class} derived from
 * type component of the {@code CONSTANT_NameAndType} descriptor of the constant</li>
 * </ul>
 * This resolution process may trigger class loading.
 * It may therefore throw an error if a class fails to load.
 * This error becomes the abnormal termination of the dynamic
 * call site execution or dynamic constant evaluation.
 * Linkage does not trigger class initialization.
 * Static arguments, if any, are resolved in the following phase.
 * (Note that static arguments can themselves be dynamic constants.)
 * <p>
 * The arity of the bootstrap method determines its invocation for linkage.
 * If the bootstrap method accepts at least three parameters, or if it
 * is a variable-arity method handle, the linkage information is <em>pushed</em>
 * into the bootstrap method, by invoking it on these values:
 * <ul>
 * <li>a {@code MethodHandles.Lookup}, which is a lookup object on the <em>caller class</em>
 * in which dynamic call site or constant occurs </li>
 * <li>a {@code String}, the method or constant name mentioned in the call site or constant </li>
 * <li>a {@code MethodType}, the resolved type descriptor of the call site, if it is a dynamic call site </li>
 * <li>a {@code Class}, the resolved type descriptor of the constant, if it is a dynamic constant </li>
 * <li>optionally, any number of additional static arguments taken from the constant pool </li>
 * </ul>
 * In this case the static arguments are resolved before the bootstrap method
 * is invoked.  If this resolution causes exceptions they are processed without
 * calling the bootstrap method.
 * <p>
 * If the bootstrap method accepts two parameters, and it is <em>not</em>
 * a variable-arity method handle, then the linkage information is presented
 * to the bootstrap method by a API which allows it to <em>pull</em> the static arguments.
 * This allows the bootstrap logic the ability to order the resolution of constants and catch
 * linkage exceptions.  For this mode of linkage, the bootstrap method is
 * is invoked on just two values:
 * <ul>
 * <li>a {@code MethodHandles.Lookup}, a lookup object on the <em>caller class</em>
 * (as in the "push" mode) </li>
 * <li>a {@link java.lang.invoke.BootstrapCallInfo BootstrapCallInfo} object
 * describing the linkage parameters of the dynamic call site or constant </li>
 * </ul>
 * <p>
 * In all cases, bootstrap method invocation is as if by
 * {@link java.lang.invoke.MethodHandle#invokeWithArguments MethodHandle.invokeWithArguments},
 * (This is also equivalent to
 * {@linkplain java.lang.invoke.MethodHandle#invoke generic invocation}
 * if the number of arguments is small enough.)
 * <p>
 * For an {@code invokedynamic} instruction, the
 * returned result must be convertible to a non-null reference to a
 * {@link java.lang.invoke.CallSite CallSite}.
 * If the returned result cannot be converted to the expected type,
 * {@link java.lang.BootstrapMethodError BootstrapMethodError} is thrown.
 * The type of the call site's target must be exactly equal to the type
 * derived from the dynamic call site's type descriptor and passed to
 * the bootstrap method, otherwise a {@code BootstrapMethodError} is thrown.
 * On success the call site then becomes permanently linked to the dynamic call
 * site.
 * <p>
 * For a {@code ConstantDynamic} constant, the result returned from the
 * bootstrap method must be convertible (by the same conversions as
 * {@linkplain java.lang.invoke.MethodHandle#asType a method handle transformed by {@code asType}})
 * to the statically declared type of the {@code ConstantDynamic} constant.
 * On success the constant then becomes permanently linked to the
 * converted result of the bootstrap method.
 * <p>
 * If an exception, {@code E} say, occurs when linking the call site or constant then the
 * linkage fails and terminates abnormally. {@code E} is rethrown if the type of
 * {@code E} is {@code Error} or a subclass, otherwise a
 * {@code BootstrapMethodError} that wraps {@code E} is thrown.
 * If this happens, the same {@code Error} or subclass will the thrown for all
 * subsequent attempts to execute the dynamic call site or load the dynamic constant.
 * <h2>timing of linkage</h2>
 * A dynamic call site is linked just before its first execution.
 * A dynamic constant is linked just before the first time it is used
 * (by pushing on the stack or linking it as a bootstrap method parameter).
 * The bootstrap method call implementing the linkage occurs within
 * a thread that is attempting a first execution or first use.
 * <p>
 * If there are several such threads, the bootstrap method may be
 * invoked in several threads concurrently.
 * Therefore, bootstrap methods which access global application
 * data must take the usual precautions against race conditions.
 * In any case, every {@code invokedynamic} instruction is either
 * unlinked or linked to a unique {@code CallSite} object.
 * <p>
 * In an application which requires dynamic call sites with individually
 * mutable behaviors, their bootstrap methods should produce distinct
 * {@link java.lang.invoke.CallSite CallSite} objects, one for each linkage request.
 * Alternatively, an application can link a single {@code CallSite} object
 * to several {@code invokedynamic} instructions, in which case
 * a change to the target method will become visible at each of
 * the instructions.
 * <p>
 * If several threads simultaneously execute a bootstrap method for a single dynamic
 * call site or constant, the JVM must choose one bootstrap method result and install it visibly to
 * all threads.  Any other bootstrap method calls are allowed to complete, but their
 * results are ignored, and their dynamic call site invocations
 * or constant loads proceed with the originally
 * chosen target object.

 * <p style="font-size:smaller;">
 * <em>Discussion:</em>
 * These rules do not enable the JVM to duplicate dynamic call sites,
 * or to issue &ldquo;causeless&rdquo; bootstrap method calls.
 * Every dynamic call site transitions at most once from unlinked to linked,
 * just before its first invocation.
 * There is no way to undo the effect of a completed bootstrap method call.
 *
 * <h2>types of bootstrap methods</h2>
 * As long as each bootstrap method can be correctly invoked
 * by {@code MethodHandle.invoke}, its detailed type is arbitrary.
 * For example, the first argument could be {@code Object}
 * instead of {@code MethodHandles.Lookup}, and the return type
 * could also be {@code Object} instead of {@code CallSite}.
 * (Note that the types and number of the stacked arguments limit
 * the legal kinds of bootstrap methods to appropriately typed
 * static methods and constructors of {@code CallSite} subclasses.)
 * <p>
 * If a given {@code invokedynamic} instruction specifies no static arguments,
 * the instruction's bootstrap method will be invoked on three arguments,
 * conveying the instruction's caller class, name, and method type.
 * If the {@code invokedynamic} instruction specifies one or more static arguments,
 * those values will be passed as additional arguments to the method handle.
 * (Note that because there is a limit of 255 arguments to any method,
 * at most 251 extra arguments can be supplied to a non-varargs bootstrap method,
 * since the bootstrap method
 * handle itself and its first three arguments must also be stacked.)
 * The bootstrap method will be invoked as if by {@code MethodHandle.invokeWithArguments}.
 * A variable-arity bootstrap method can accept thousands of static arguments,
 * subject only by limits imposed by the class-file format.
 * <p>
 * The normal argument conversion rules for {@code MethodHandle.invoke} apply to all stacked arguments.
 * For example, if a pushed value is a primitive type, it may be converted to a reference by boxing conversion.
 * If the bootstrap method is a variable arity method (its modifier bit {@code 0x0080} is set),
 * then some or all of the arguments specified here may be collected into a trailing array parameter.
 * (This is not a special rule, but rather a useful consequence of the interaction
 * between {@code CONSTANT_MethodHandle} constants, the modifier bit for variable arity methods,
 * and the {@link java.lang.invoke.MethodHandle#asVarargsCollector asVarargsCollector} transformation.)
 * <p>
 * Given these rules, here are examples of legal bootstrap method declarations,
 * given various numbers {@code N} of extra arguments.
 * The first row (marked {@code *}) will work for any number of extra arguments.
 * <table class="plain" style="vertical-align:top">
 * <caption style="display:none">Static argument types</caption>
 * <thead>
 * <tr><th scope="col">N</th><th scope="col">Sample bootstrap method</th></tr>
 * </thead>
 * <tbody>
 * <tr><th scope="row" style="font-weight:normal; vertical-align:top">*</th><td>
 *     <ul style="list-style:none; padding-left: 0; margin:0">
 *     <li><code>CallSite bootstrap(Lookup caller, String name, MethodType type, Object... args)</code>
 *     <li><code>CallSite bootstrap(Object... args)</code>
 *     <li><code>CallSite bootstrap(Object caller, Object... nameAndTypeWithArgs)</code>
 *     <li><code>CallSite bootstrap(Lookup caller, BootstrapCallInfo bsci)</code>
 *     </ul></td></tr>
 * <tr><th scope="row" style="font-weight:normal; vertical-align:top">0</th><td>
 *     <ul style="list-style:none; padding-left: 0; margin:0">
 *     <li><code>CallSite bootstrap(Lookup caller, String name, MethodType type)</code>
 *     <li><code>CallSite bootstrap(Lookup caller, Object... nameAndType)</code>
 *     </ul></td></tr>
 * <tr><th scope="row" style="font-weight:normal; vertical-align:top">1</th><td>
 *     <code>CallSite bootstrap(Lookup caller, String name, MethodType type, Object arg)</code></td></tr>
 * <tr><th scope="row" style="font-weight:normal; vertical-align:top">2</th><td>
 *     <ul style="list-style:none; padding-left: 0; margin:0">
 *     <li><code>CallSite bootstrap(Lookup caller, String name, MethodType type, Object... args)</code>
 *     <li><code>CallSite bootstrap(Lookup caller, String name, MethodType type, String... args)</code>
 *     <li><code>CallSite bootstrap(Lookup caller, String name, MethodType type, String x, int y)</code>
 *     </ul></td></tr>
 * </tbody>
 * </table>
 * The last example assumes that the extra arguments are of type
 * {@code CONSTANT_String} and {@code CONSTANT_Integer}, respectively.
 * The second-to-last example assumes that all extra arguments are of type
 * {@code CONSTANT_String}.
 * The other examples work with all types of extra arguments.
 * <p>
 * The fourth example (which is the only example that does
 * <em>not</em> use variable arity <em>and</em> takes two arguments)
 * is the normal signature under which a "pull" mode bootstrap method
 * is invoked.  For such a bootstrap method, the two parameters may
 * also be {@code Object}, since that is a supertype of {@code Lookup}
 * and {@link java.lang.invoke.BootstrapCallInfo BootstrapCallInfo}.
 * See {@linkplain java.lang.invoke.BootstrapCallInfo BootstrapCallInfo}
 * the documentation for that interface} for further notes about the
 * relation between the two modes of bootstrap method invocation.
 * <p>
 * As noted above, the actual method type of the bootstrap method can vary.
 * For example, the fourth argument could be {@code MethodHandle},
 * if that is the type of the corresponding constant in
 * the {@code CONSTANT_InvokeDynamic} entry.
 * In that case, the {@code MethodHandle.invoke} call will pass the extra method handle
 * constant as an {@code Object}, but the type matching machinery of {@code MethodHandle.invoke}
 * will cast the reference back to {@code MethodHandle} before invoking the bootstrap method.
 * (If a string constant were passed instead, by badly generated code, that cast would then fail,
 * resulting in a {@code BootstrapMethodError}.)
 * <p>
 * Since dynamic constants can be provided as static arguments to bootstrap
 * methods, there are no limitations on the types of bootstrap arguments.
 * However, arguments of type {@code boolean}, {@code byte}, {@code short}, or {@code char}
 * cannot be <em>directly</em> supplied by {@code CONSTANT_Integer}
 * constant pool entries, since the {@code asType} conversions do
 * not perform the necessary narrowing primitive conversions.
 * <p>
 * In the above examples, the return type is always {@code CallSite},
 * but that is not a necessary feature of bootstrap methods.
 * In the case of a dynamic call site, the only requirement is that
 * the value returned by the bootstrap method must be convertible
 * (using the {@code asType} conversions) to {@code CallSite}, which
 * means the bootstrap method return type might be {@code Object} or
 * {@code ConstantCallSite}
 * In the case of a dynamic constant, the result of the bootstrap
 * method must be convertible to the type of the constant, as
 * represented by its field type descriptor.  For example, if the
 * dynamic constant has a field type descriptor of {@code "C"}
 * ({@code char}) then the bootstrap method return type could be
 * {@code Object}, {@code Character}, or {@code char}, but not
 * {@code int} or {@code Integer}.
 * <p>
 * Extra bootstrap method arguments are intended to allow language implementors
 * to safely and compactly encode metadata.
 * In principle, the name and extra arguments are redundant,
 * since each call site could be given its own unique bootstrap method.
 * Such a practice would be likely to produce large class files and constant pools.
 *
 * @author John Rose, JSR 292 EG
 * @since 1.7
 */

package java.lang.invoke;
