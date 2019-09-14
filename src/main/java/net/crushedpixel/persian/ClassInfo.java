package net.crushedpixel.persian;

import java.util.List;

class ClassInfo {

    final Class<?> clazz;
    final List<Class<?>> generics;

    ClassInfo(Class<?> clazz, List<Class<?>> generics) {
        this.clazz = clazz;
        this.generics = generics;
    }
}
