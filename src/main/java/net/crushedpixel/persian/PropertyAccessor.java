package net.crushedpixel.persian;

import ru.vyarus.java.generics.resolver.context.GenericsContext;

interface PropertyAccessor {

    ClassInfo getType(GenericsContext context);

    Object get(Object instance) throws Exception;

    void set(Object instance, Object value) throws Exception;

}
