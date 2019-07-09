/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

#ifndef SHARE_VM_OOPS_RECORDPARAMINFO_HPP
#define SHARE_VM_OOPS_RECORDPARAMINFO_HPP

#include "oops/constantPool.hpp"
#include "oops/typeArrayOop.hpp"
#include "classfile/vmSymbols.hpp"

// This class represents the parameter information contained in the recordParams
// array of an InstanceKlass.  Currently it's laid on top an array of
// Java shorts but in the future it could simply be used as a real
// array type.  RecordParamInfo generally shouldn't be used directly.
// Record parameters should be queried through InstanceKlass.

class RecordParamInfo {
  friend class ClassFileParser;
  friend class RecordParameterStreamBase;
  friend class JavaRecordParameterStream;
  enum ParamOffset {
    accessor_index_offset    = 0,
    param_slots              = 1
  };

private:
  u2 _shorts[param_slots];

  void set_accessor_index(u2 val)                          { _shorts[accessor_index_offset] = val;    }

public:
  static RecordParamInfo* from_record_params_array(Array<u2>* record_params, int index) {
    return ((RecordParamInfo*)record_params->adr_at(index * param_slots));
  }
  static RecordParamInfo* from_record_params_array(u2* record_params, int index) {
    return ((RecordParamInfo*)(record_params + index * param_slots));
  }

  void initialize(u2 accessor_index) {
    _shorts[accessor_index_offset] = accessor_index;
  }

  u2 accessor_index() const                                { return _shorts[accessor_index_offset];   }

  Symbol* name(const constantPoolHandle& cp) const {
    int accessor_index = _shorts[accessor_index_offset];
    int method_ref_info = cp->int_at(accessor_index);
    int name_and_type_index = extract_high_short_from_int(method_ref_info);
    int name_and_type_info = cp->int_at(name_and_type_index);
    int name_index = extract_low_short_from_int(name_and_type_info);
    return cp->symbol_at(name_index);
  }
};

#endif // SHARE_VM_OOPS_RECORDPARAMINFO_HPP
