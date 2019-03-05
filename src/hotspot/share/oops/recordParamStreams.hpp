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

#ifndef SHARE_VM_OOPS_RECORDPARAMSTREAMS_HPP
#define SHARE_VM_OOPS_RECORDPARAMSTREAMS_HPP

#include "oops/instanceKlass.hpp"
#include "oops/recordParamInfo.hpp"

// The is the base class for iteration over the record parameters array
// describing the parameters in a record.
class RecordParameterStreamBase : public StackObj {
 protected:
  Array<u2>*          _record_parameters;
  constantPoolHandle  _constants;
  int                 _index;
  int                 _limit;

  RecordParamInfo* record_param() const { return RecordParamInfo::from_record_params_array(_record_parameters, _index); }
  InstanceKlass* record_param_holder() const { return _constants->pool_holder(); }

  RecordParameterStreamBase(Array<u2>* record_params, const constantPoolHandle& constants, int start, int limit) {
    _record_parameters = record_params;
    _constants = constants;
    _index = start;
    int num_record_parameters = record_params->length() / RecordParamInfo::param_slots;
    if (limit < start) {
      _limit = num_record_parameters;
    } else {
      _limit = limit;
    }
  }

  RecordParameterStreamBase(Array<u2>* record_params, const constantPoolHandle& constants) {
    _record_parameters = record_params;
    _constants = constants;
    _index = 0;
    _limit = record_params->length() / RecordParamInfo::param_slots;;
  }

 public:
  RecordParameterStreamBase(InstanceKlass* klass) {
    _record_parameters = klass->record_params();
    _constants = klass->constants();
    _index = 0;
    _limit = klass->record_params_count();
    assert(klass == record_param_holder(), "");
  }

  // accessors
  int index() const                 { return _index; }

  void next() {
    _index += 1;
  }
  bool done() const { return _index >= _limit; }

  // Accessors for current record parameter
  AccessFlags access_flags() const {
    AccessFlags flags;
    flags.set_flags(record_param()->access_flags());
    return flags;
  }

  void set_access_flags(u2 flags) const {
    record_param()->set_access_flags(flags);
  }

  void set_access_flags(AccessFlags flags) const {
    set_access_flags(flags.as_short());
  }

  Symbol* name() const {
    return record_param()->name(_constants);
  }

  Symbol* descriptor() const {
    return record_param()->descriptor(_constants);
  }

  Symbol* signature() const {
    return record_param()->signature(_constants);
  }
};

// Iterate over the record parameters
class JavaRecordParameterStream : public RecordParameterStreamBase {
 public:
  JavaRecordParameterStream(const InstanceKlass* k): RecordParameterStreamBase(k->record_params(), k->constants(), 0, k->record_params_count()) {}

  int name_index() const {
    return record_param()->name_index();
  }
  void set_name_index(int index) {
    record_param()->set_name_index(index);
  }
  int descriptor_index() const {
    return record_param()->descriptor_index();
  }
  void set_descriptor_index(int index) {
    record_param()->set_descriptor_index(index);
  }
  int signature_index() const {
    return record_param()->signature_index();
  }
  void set_generic_signature_index(int index) {
    record_param()->set_signature_index(index);
  }
};

#endif // SHARE_VM_OOPS_RECORDPARAMSTREAMS_HPP
