package net.crushedpixel.persian;

import net.crushedpixel.persian.annotations.Access;
import net.crushedpixel.persian.annotations.Access.AccessType;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

class PropertyRegistry {

    /**
     * The property accessors to use for each class type.
     */
    private Map<Class<?>, Map<String, PropertyAccessor>> propertyAccessors = new HashMap<>();

    Map<String, PropertyAccessor> getAccessors(Class<?> clazz) {
        Map<String, PropertyAccessor> allAccessors = new HashMap<>();

        // for each level of the class hierarchy,
        // add its property accessors to the list of all accessors.
        while (clazz.getSuperclass() != null) {
            Class<?> finalClazz = clazz;
            var a = propertyAccessors.computeIfAbsent(clazz, k -> {
                // resolve the accessors for each of the classes properties
                var accessors = new HashMap<String, PropertyAccessor>();

                // determine the default access type for this classes properties
                var access = finalClazz.getAnnotation(Access.class);
                var defaultAccess = access == null
                        ? AccessType.FIELD
                        : access.value();

                for (Field field : finalClazz.getDeclaredFields()) {
                    // exclude static fields from serialization
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    // exclude transient fields from serialization
                    if (Modifier.isTransient(field.getModifiers())) continue;

                    if (getAccessType(field, defaultAccess) == AccessType.FIELD) {
                        accessors.put(field.getName(), new FieldAccessor(field));
                    }
                }

                for (Method getter : finalClazz.getDeclaredMethods()) {
                    // exclude static methods from serialization
                    if (Modifier.isStatic(getter.getModifiers())) continue;
                    // iterate over all getters
                    if (!getter.getName().startsWith("get")) continue;

                    String propertyName = getter.getName().substring("get".length());
                    if (propertyName.isEmpty()) continue;

                    String setterName = "set" + propertyName;

                    // de-capitalize the name of the property
                    propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);

                    // try to find the getter's respective setter
                    Method setter;
                    try {
                        setter = finalClazz.getDeclaredMethod(setterName, getter.getReturnType());
                    } catch (NoSuchMethodException e) {
                        continue;
                    }

                    // check if either getter or setter have an explicit Access annotation specifying METHOD access,
                    // or METHOD is the default access type and there's no field
                    // that overrides the access type for this property.
                    if (getAccessType(getter, AccessType.FIELD) == AccessType.METHOD
                            || getAccessType(setter, AccessType.FIELD) == AccessType.METHOD
                            || (defaultAccess == AccessType.METHOD && !accessors.containsKey(propertyName))) {

                        accessors.put(propertyName, new MethodAccessor(getter, setter));
                    }
                }

                return accessors;
            });
            allAccessors.putAll(a);

            clazz = clazz.getSuperclass();
        }

        return allAccessors;
    }

    private static AccessType getAccessType(AccessibleObject member, AccessType fallback) {
        if (!member.isAnnotationPresent(Access.class)) return fallback;
        return member.getAnnotation(Access.class).value();
    }
}
