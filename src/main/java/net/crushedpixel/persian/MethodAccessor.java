package net.crushedpixel.persian;

import ru.vyarus.java.generics.resolver.context.GenericsContext;

import java.lang.reflect.Method;

class MethodAccessor implements PropertyAccessor{

    private final Method getter, setter;

    public MethodAccessor(Method getter, Method setter) {
        this.getter = getter;
        this.setter = setter;

        getter.setAccessible(true);
        setter.setAccessible(true);
    }

    @Override
    public ClassInfo getType(GenericsContext context) {
        var m = context.method(getter);
        return new ClassInfo(m.resolveReturnClass(), m.resolveReturnTypeGenerics());
    }

    @Override
    public Object get(Object instance) throws Exception {
        return getter.invoke(instance);
    }

    @Override
    public void set(Object instance, Object value) throws Exception {
        setter.invoke(instance, value);
    }
}
