package net.crushedpixel.persian;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

class FieldAccessor implements PropertyAccessor {

    private final Field field;

    FieldAccessor(Field field) {
        this.field = field;
        field.setAccessible(true);
    }

    @Override
    public Type getGenericType() {
        return this.field.getGenericType();
    }

    @Override
    public Object get(Object instance) throws Exception {
        return field.get(instance);
    }

    @Override
    public void set(Object instance, Object value) throws Exception {
        field.set(instance, value);
    }
}
