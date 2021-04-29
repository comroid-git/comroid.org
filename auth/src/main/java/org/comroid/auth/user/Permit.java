package org.comroid.auth.user;

import org.comroid.api.BitmaskEnum;
import org.comroid.api.ValueBox;
import org.comroid.api.ValueType;
import org.comroid.util.Bitmask;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public enum Permit implements BitmaskEnum<Permit> {
    EMAIL, DEV, ADMIN;

    public static Set valueOf(int mask) {
        return new Set(BitmaskEnum.valueOf(mask, Permit.class));
    }

    public static Set valueOf(String... scopes) {
        return new Set(Stream.of(scopes)
                .map(String::toUpperCase)
                .map(Permit::valueOf)
                .collect(Collectors.toSet()));
    }

    public static final class Set extends HashSet<Permit> implements ValueBox<Integer> {
        @Override
        public Integer getValue() {
            return Bitmask.combine(toArray(new Permit[0]));
        }

        @Override
        public ValueType<? extends Integer> getHeldType() {
            return StandardValueType.INTEGER;
        }

        public Set() {
            this(new HashSet<>());
        }

        public Set(java.util.Set<Permit> values) {
            super(values);
        }

        @Override
        public String toString() {
            return String.join(" ", stream()
                    .map(Permit::getName)
                    .map(String::toLowerCase)
                    .toArray(String[]::new));
        }

        @Override
        public @NotNull Permit[] toArray() {
            return super.toArray(new Permit[0]);
        }

        public String[] toStringArray() {
            return stream().map(Enum::name)
                    .map(String::toLowerCase)
                    .toArray(String[]::new);
        }
    }
}
