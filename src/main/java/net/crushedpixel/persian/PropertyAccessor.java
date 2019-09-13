package net.crushedpixel.persian;

import java.lang.reflect.Type;

interface PropertyAccessor {

    Type getGenericType();

    Object get(Object instance) throws Exception;

    void set(Object instance, Object value) throws Exception;

}
