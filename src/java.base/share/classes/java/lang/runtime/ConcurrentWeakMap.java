/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.runtime;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import jdk.internal.javac.PreviewFeature;

/**
 * A {@link ConcurrentMap} where the keys are weakly referenced.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
@PreviewFeature(feature=PreviewFeature.Feature.TEMPLATED_STRINGS)
final class ConcurrentWeakMap<K, V> implements ConcurrentMap<K, V> {
    interface Key<T> {
        T get();
    }

    record LookupKey<T>(T value) implements Key<T> {
        @Override
        public T get() {
            return value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object obj) {
            if (obj instanceof Key<?> key) {
                obj = key.get();
            }
            return Objects.equals(get(), obj);
        }

        @Override
        public int hashCode() {
            return get().hashCode();
        }
    }

    static class EntryKey<T> extends WeakReference<T> implements Key<T> {
        int hashcode;

        public EntryKey(T value, ReferenceQueue<T> queue) {
            super(value, queue);
            this.hashcode = Objects.hashCode(value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Key<?> key) {
                obj = key.get();
            }
            return Objects.equals(get(), obj);
        }

        @Override
        public int hashCode() {
            return hashcode;
        }
    }

    private final ConcurrentMap<Key<K>, V> map;
    private final ReferenceQueue<EntryKey<K>> stale;

    public ConcurrentWeakMap() {
        this.map = new ConcurrentHashMap<>(512);
        this.stale = new ReferenceQueue<>();
    }

    @SuppressWarnings("unchecked")
    LookupKey<K> lookupKey(Object key) {
        return new LookupKey<>((K)key);
    }

    @SuppressWarnings("unchecked")
    EntryKey<K> entryKey(Object key) {
        return new EntryKey<>((K)key, (ReferenceQueue<K>)stale);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        Objects.requireNonNull(key, "key must not be null");
        return map.containsKey(lookupKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        Objects.requireNonNull(value, "value must not be null");
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        Objects.requireNonNull(key, "key must not be null");
        purgeReferenceQueue();
        return map.get(lookupKey(key));
    }

    @Override
    public V put(K key, V newValue) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(newValue, "value must not be null");
        purgeReferenceQueue();
        EntryKey<K> entryKey = entryKey(key);
        V oldValue = map.put(entryKey, newValue);
        if (oldValue != null) {
            entryKey.clear();
        }
        return oldValue;
    }

    @Override
    public V remove(Object key) {
        return map.remove(lookupKey(key));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            put(key, value);
        }
    }

    @Override
    public void clear() {
        map.clear();
        purgeReferenceQueue();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet()
                .stream()
                .map(Key::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.keySet()
                .stream()
                .map(Key::get)
                .filter(Objects::nonNull)
                .map(k -> new AbstractMap.SimpleEntry<>(k, get(k)))
                .collect(Collectors.toSet());
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return map.equals(o);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public V putIfAbsent(K key, V newValue) {
        purgeReferenceQueue();
        EntryKey<K> entryKey = entryKey(key);
        V oldValue = map.putIfAbsent(entryKey, newValue);
        if (oldValue != null) {
            entryKey.clear();
        }
        return oldValue;
    }

    @Override
    public boolean remove(Object key, Object value) {
        return map.remove(lookupKey(key), value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        purgeReferenceQueue();
        return map.replace(lookupKey(key), oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        purgeReferenceQueue();
        return map.replace(lookupKey(key), value);
    }

    @SuppressWarnings("unchecked")
    private void purgeReferenceQueue() {
        while (true) {
            EntryKey<K> reference = (EntryKey<K>)stale.poll();

            if (reference == null) {
                break;
            }

            map.remove(reference);
        }
    }
}
