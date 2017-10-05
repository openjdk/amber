/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.OperatorSymbol;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.util.*;

import static com.sun.tools.javac.code.TypeTag.BOOLEAN;

import static com.sun.tools.javac.code.TypeTag.BOT;
import static com.sun.tools.javac.code.TypeTag.CHAR;
import static com.sun.tools.javac.jvm.ByteCodes.*;

/** Helper class for constant folding, used by the attribution phase.
 *  This class is marked strictfp as mandated by JLS 15.4.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
strictfp public class ConstFold {
    protected static final Context.Key<ConstFold> constFoldKey = new Context.Key<>();

    private Symtab syms;
    private Types types;

    public static ConstFold instance(Context context) {
        ConstFold instance = context.get(constFoldKey);
        if (instance == null)
            instance = new ConstFold(context);
        return instance;
    }

    private ConstFold(Context context) {
        context.put(constFoldKey, this);

        syms = Symtab.instance(context);
        types = Types.instance(context);
    }

    static final Integer minusOne = -1;
    static final Integer zero     = 0;
    static final Integer one      = 1;

   /** Convert boolean to integer (true = 1, false = 0).
    */
    private static Integer b2i(boolean b) {
        return b ? one : zero;
    }
    private static int intValue(Object x) { return ((Number)x).intValue(); }
    private static long longValue(Object x) { return ((Number)x).longValue(); }
    private static float floatValue(Object x) { return ((Number)x).floatValue(); }
    private static double doubleValue(Object x) { return ((Number)x).doubleValue(); }
    public static String stringValue(TypeTag tag, Object x) {
        if (tag == BOT) {
            return "null";
        } else if (tag == BOOLEAN) {
            return ((Integer)x).intValue() == 0 ? "false" : "true";
        } else if (tag == CHAR) {
            return String.valueOf((char) ((Integer)x).intValue());
        } else {
            return x.toString();
        }
    }

    Type fold1(OperatorSymbol op, Type od) {
        if (op.opcode == nop) {
            return od;
        } else {
            Object v = fold1(op, od.constValue());
            Type foldType = foldType(op);
            return (foldType != null && v != null) ?
                    foldType(op).constType(v) :
                    null;
        }
    }

    /** Fold unary operation.
     *  @param op        The operator symbol.
     *                   opcode's ifeq to ifge are for postprocessing
     *                   xcmp; ifxx pairs of instructions.
     *  @param od        The operation's operand. Assumed to be non-null.
     */
    Object fold1(OperatorSymbol op, Object od) {
        int opcode = op.opcode;
        try {
            switch (opcode) {
                case ineg: // unary -
                    return -intValue(od);
                case ixor: // ~
                    return ~intValue(od);
                case bool_not: // !
                    return b2i(intValue(od) == 0);
                case ifeq:
                    return b2i(intValue(od) == 0);
                case ifne:
                    return b2i(intValue(od) != 0);
                case iflt:
                    return b2i(intValue(od) < 0);
                case ifgt:
                    return b2i(intValue(od) > 0);
                case ifle:
                    return b2i(intValue(od) <= 0);
                case ifge:
                    return b2i(intValue(od) >= 0);

                case lneg: // unary -
                    return Long.valueOf(-longValue(od));
                case lxor: // ~
                    return Long.valueOf(~longValue(od));

                case fneg: // unary -
                    return Float.valueOf(-floatValue(od));

                case dneg: // ~
                    return Double.valueOf(-doubleValue(od));

                default:
                    return null;
            }
        } catch (ArithmeticException e) {
            return null;
        }
    }

    Type fold2(OperatorSymbol op, Type left, Type right) {
        Object v = fold2(op, left.constValue(), right.constValue());
        Type foldType = foldType(op);
        return (foldType != null && v != null) ?
                foldType(op).constType(v) :
                null;
    }

    /** Fold binary operation.
     *  @param op        The operator symbol.
     *  @param l         The operation's left operand.
     *  @param r         The operation's right operand.
     */
    Object fold2(OperatorSymbol op, Object l, Object r) {
        int opcode = op.opcode;
        try {
            if (opcode > ByteCodes.preMask) {
                // we are seeing a composite instruction of the form xcmp; ifxx.
                // In this case fold both instructions separately.
                Object t1 = fold2(op.pre(types), l, r);
                return (t1 == null) ? t1
                    : fold1(op.post(types), t1);
            } else {
                switch (opcode) {
                case iadd:
                    return intValue(l) + intValue(r);
                case isub:
                    return intValue(l) - intValue(r);
                case imul:
                    return intValue(l) * intValue(r);
                case idiv:
                    return intValue(l) / intValue(r);
                case imod:
                    return intValue(l) % intValue(r);
                case iand:
                    return intValue(l) & intValue(r);
                case bool_and:
                    return b2i((intValue(l) & intValue(r)) != 0);
                case ior:
                    return intValue(l) | intValue(r);
                case bool_or:
                    return b2i((intValue(l) | intValue(r)) != 0);
                case ixor:
                    return intValue(l) ^ intValue(r);
                case ishl: case ishll:
                    return intValue(l) << intValue(r);
                case ishr: case ishrl:
                    return intValue(l) >> intValue(r);
                case iushr: case iushrl:
                    return intValue(l) >>> intValue(r);
                case if_icmpeq:
                    return b2i(intValue(l) == intValue(r));
                case if_icmpne:
                    return b2i(intValue(l) != intValue(r));
                case if_icmplt:
                    return b2i(intValue(l) < intValue(r));
                case if_icmpgt:
                    return b2i(intValue(l) > intValue(r));
                case if_icmple:
                    return b2i(intValue(l) <= intValue(r));
                case if_icmpge:
                    return b2i(intValue(l) >= intValue(r));

                case ladd:
                    return Long.valueOf(longValue(l) + longValue(r));
                case lsub:
                    return Long.valueOf(longValue(l) - longValue(r));
                case lmul:
                    return Long.valueOf(longValue(l) * longValue(r));
                case ldiv:
                    return Long.valueOf(longValue(l) / longValue(r));
                case lmod:
                    return Long.valueOf(longValue(l) % longValue(r));
                case land:
                    return Long.valueOf(longValue(l) & longValue(r));
                case lor:
                    return Long.valueOf(longValue(l) | longValue(r));
                case lxor:
                    return Long.valueOf(longValue(l) ^ longValue(r));
                case lshl: case lshll:
                    return Long.valueOf(longValue(l) << intValue(r));
                case lshr: case lshrl:
                    return Long.valueOf(longValue(l) >> intValue(r));
                case lushr:
                    return Long.valueOf(longValue(l) >>> intValue(r));
                case lcmp:
                    if (longValue(l) < longValue(r))
                        return minusOne;
                    else if (longValue(l) > longValue(r))
                        return one;
                    else
                        return zero;
                case fadd:
                    return Float.valueOf(floatValue(l) + floatValue(r));
                case fsub:
                    return Float.valueOf(floatValue(l) - floatValue(r));
                case fmul:
                    return Float.valueOf(floatValue(l) * floatValue(r));
                case fdiv:
                    return Float.valueOf(floatValue(l) / floatValue(r));
                case fmod:
                    return Float.valueOf(floatValue(l) % floatValue(r));
                case fcmpg: case fcmpl:
                    if (floatValue(l) < floatValue(r))
                        return minusOne;
                    else if (floatValue(l) > floatValue(r))
                        return one;
                    else if (floatValue(l) == floatValue(r))
                        return zero;
                    else if (opcode == fcmpg)
                        return one;
                    else
                        return minusOne;
                case dadd:
                    return Double.valueOf(doubleValue(l) + doubleValue(r));
                case dsub:
                    return Double.valueOf(doubleValue(l) - doubleValue(r));
                case dmul:
                    return Double.valueOf(doubleValue(l) * doubleValue(r));
                case ddiv:
                    return Double.valueOf(doubleValue(l) / doubleValue(r));
                case dmod:
                    return syms.doubleType.constType(
                        Double.valueOf(doubleValue(l) % doubleValue(r)));
                case dcmpg: case dcmpl:
                    if (doubleValue(l) < doubleValue(r))
                        return minusOne;
                    else if (doubleValue(l) > doubleValue(r))
                        return one;
                    else if (doubleValue(l) == doubleValue(r))
                        return zero;
                    else if (opcode == dcmpg)
                        return one;
                    else
                        return minusOne;
                case if_acmpeq:
                    return b2i(l.equals(r));
                case if_acmpne:
                    return b2i(!l.equals(r));
                case string_add: {
                    List<Type> params = op.type.getParameterTypes();
                    return stringValue(params.head.getTag(), l) + stringValue(params.tail.head.getTag(), r);
                }
                default:
                    return null;
                }
            }
        } catch (ArithmeticException e) {
            return null;
        }
    }

    /** Fold binary operation.
     *  @param opcode    The operation's opcode instruction (usually a byte code),
     *                   as entered by class Symtab.
     *                   opcode's ifeq to ifge are for postprocessing
     *                   xcmp; ifxx pairs of instructions.
     *  @param left      The type of the operation's left operand.
     *  @param right     The type of the operation's right operand.
     */
    Type foldType(OperatorSymbol op) {
        int opcode = op.opcode;
        if (opcode > ByteCodes.preMask) {
            // we are seeing a composite instruction of the form xcmp; ifxx.
            // In this case fold both instructions separately.
            return syms.booleanType;
        } else {
            switch (opcode) {
                case iadd: case isub: case imul: case idiv: case imod:
                case ishl: case ishll: case ishr: case ishrl: case iushr: case iushrl:
                case ineg: case lcmp: case fcmpg: case fcmpl: case dcmpg: case dcmpl:
                    return syms.intType;
                case bool_and: case bool_or: case if_icmpeq: case if_icmpne: case if_icmplt: case if_icmpgt:
                case if_icmple: case if_icmpge: case if_acmpeq: case if_acmpne: case bool_not: case ifeq:
                case ifne: case iflt: case ifgt: case ifle: case ifge:
                    return syms.booleanType;
                case ior: case iand: case ixor:
                    return (op.type.getParameterTypes().head.hasTag(BOOLEAN)
                      ? syms.booleanType : syms.intType);
                case ladd: case lsub: case lmul: case ldiv: case lmod: case land:
                case lor: case lxor: case lshl: case lshll: case lshr: case lshrl: case lneg:
                case lushr:
                    return syms.longType;
                case fadd: case fsub: case fmul: case fdiv: case fmod: case fneg:
                    return syms.floatType;
                case dadd: case dsub: case dmul: case ddiv: case dmod: case dneg:
                    return syms.doubleType;
                case string_add:
                    return syms.stringType;
                default:
                    return null;
            }
        }
    }

    /** Coerce constant type to target type.
     *  @param etype      The source type of the coercion,
     *                    which is assumed to be a constant type compatible with
     *                    ttype.
     *  @param ttype      The target type of the coercion.
     */
     Type coerce(Type etype, Type ttype) {
         // WAS if (etype.baseType() == ttype.baseType())
         if (etype.tsym.type == ttype.tsym.type)
             return etype;
         if (etype.isNumeric()) {
             Object n = etype.constValue();
             switch (ttype.getTag()) {
             case BYTE:
                 return syms.byteType.constType(0 + (byte)intValue(n));
             case CHAR:
                 return syms.charType.constType(0 + (char)intValue(n));
             case SHORT:
                 return syms.shortType.constType(0 + (short)intValue(n));
             case INT:
                 return syms.intType.constType(intValue(n));
             case LONG:
                 return syms.longType.constType(longValue(n));
             case FLOAT:
                 return syms.floatType.constType(floatValue(n));
             case DOUBLE:
                 return syms.doubleType.constType(doubleValue(n));
             }
         }
         return ttype;
     }
}
