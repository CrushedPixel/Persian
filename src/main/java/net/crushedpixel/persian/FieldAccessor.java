package net.crushedpixel.persian;

import ru.vyarus.java.generics.resolver.context.GenericsContext;

import java.lang.reflect.Field;

class FieldAccessor implements PropertyAccessor {

    private final Field field;

    FieldAccessor(Field field) {
        this.field = field;
        field.setAccessible(true);
    }

    @Override
    public ClassInfo getType(GenericsContext context) {
        return new ClassInfo(context.resolveFieldClass(field), context.resolveFieldGenerics(field));
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
