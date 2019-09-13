package net.crushedpixel.persian;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

class MethodAccessor implements PropertyAccessor{

    private final Method getter, setter;

    public MethodAccessor(Method getter, Method setter) {
        this.getter = getter;
        this.setter = setter;

        getter.setAccessible(true);
        setter.setAccessible(true);
    }

    @Override
    public Type getGenericType() {
        return this.getter.getGenericReturnType();
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
