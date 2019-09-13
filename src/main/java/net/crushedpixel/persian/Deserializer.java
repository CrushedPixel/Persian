package net.crushedpixel.persian;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import net.crushedpixel.persian.annotations.Model;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class Deserializer {

    private static abstract class PropertyTarget {
        final PropertyAccessor accessor;
        final Object target;

        public PropertyTarget(PropertyAccessor accessor, Object target) {
            this.accessor = accessor;
            this.target = target;
        }

        abstract void apply(Object model) throws Exception;
    }

    private static class SimplePropertyTarget extends PropertyTarget {
        public SimplePropertyTarget(PropertyAccessor accessor, Object target) {
            super(accessor, target);
        }

        @Override
        void apply(Object model) throws Exception{
            accessor.set(target, model);
        }
    }

    private static class CollectionPropertyTarget extends PropertyTarget {
        public CollectionPropertyTarget(PropertyAccessor accessor, Object target) {
            super(accessor, target);
        }

        @Override
        void apply(Object model) throws Exception {
            ((Collection) accessor.get(target)).add(model);
        }
    }

    private PropertyRegistry properties = new PropertyRegistry();

    /**
     * The model objects that were parsed, by their ID.
     */
    private Map<Integer, Object> modelObjects = new HashMap<>();

    /**
     * Placeholders for Property Targets to fill in later.
     */
    private Map<PropertyTarget, Integer> modelTargets = new HashMap<>();

    <T> T deserialize(String json, Class<T> clazz) throws Exception {
        var parser = new JsonParser();
        var obj = parser.parse(json).getAsJsonObject();

        {   // parse each model instance into the respective class
            int id = 0;
            for (JsonElement e : obj.get("models").getAsJsonArray()) {
                JsonObject modelObj = e.getAsJsonObject();
                JsonObject model = modelObj.getAsJsonObject("value");

                // TODO: respect generics when serializing/deserializing
                String typeName = modelObj.get("type").getAsString();
                Class<?> modelClass = Class.forName(typeName);

                // parse model instance and add it to the model registry
                var instance = parseObject(model, modelClass);
                modelObjects.put(id, instance);

                id++;
            }
        }

        // all models were parsed - fill in every model reference
        for (Map.Entry<PropertyTarget, Integer> entry : modelTargets.entrySet()) {
            var target = entry.getKey();
            var id = entry.getValue();
            target.apply(modelObjects.get(id));
        }

        return (T) modelObjects.get(obj.getAsJsonObject("root").get("id").getAsInt());
    }

    private Object parseValue(JsonElement element, Type genericType) throws Exception {
        if (isPrimitiveOrString(genericType)) {
            var primitive = element.getAsJsonPrimitive();

            var clazz = TypeToken.get(genericType).getRawType();
            if (clazz.isAssignableFrom(double.class)) {
                return primitive.getAsDouble();
            } else if (clazz.isAssignableFrom(float.class)) {
                return primitive.getAsFloat();
            } else if (clazz.isAssignableFrom(int.class)) {
                return primitive.getAsInt();
            } else if (clazz.isAssignableFrom(long.class)) {
                return primitive.getAsLong();
            } else if (clazz.isAssignableFrom(String.class)) {
                return primitive.getAsString();
            } else if (clazz.isAssignableFrom(boolean.class)) {
                return primitive.getAsBoolean();
            } else if (clazz.isAssignableFrom(byte.class)) {
                return primitive.getAsByte();
            } else if (clazz.isAssignableFrom(short.class)) {
                return primitive.getAsShort();
            }
        }

        return parseObject(element.getAsJsonObject(), genericType);
    }

    private Object parseObject(JsonObject obj, Type genericType) throws Exception {
        var clazz = TypeToken.get(genericType).getRawType();

        // create a new instance of the object's type
        Constructor<?> defaultConstructor;
        try {
            defaultConstructor = clazz.getConstructor();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("%s doesn't have a public no-args constructor.", clazz.getName()));
        }

        var instance = defaultConstructor.newInstance();

        // parse the model's properties
        for (var entry : properties.getAccessors(clazz).entrySet()) {
            var name = entry.getKey();
            var accessor = entry.getValue();

            var propType = entry.getValue().getGenericType();
            var propClazz = TypeToken.get(propType).getRawType();

            // treat collections specially
            if (Collection.class.isAssignableFrom(propClazz)) {
                var collection = new ArrayList<>(); // TODO: add support for more list types, sets
                accessor.set(instance, collection);

                // try to get the type of the elements contained
                // from the collection's type arguments
                if (!(propType instanceof ParameterizedType)
                        || ((ParameterizedType) propType).getActualTypeArguments().length < 1) {
                    throw new IllegalArgumentException("Can't deserialize into raw collections!");
                }

                var pType = (ParameterizedType) propType;
                var childType = TypeToken.get(pType.getActualTypeArguments()[0]).getRawType();
                boolean childrenAreModels = childType.isAnnotationPresent(Model.class);

                // deserialize the collection
                for (var e : obj.getAsJsonArray(name)) {
                    if (childrenAreModels) {
                        var id = parseModelReference(e.getAsJsonObject());
                        modelTargets.put(new CollectionPropertyTarget(accessor, instance), id);

                    } else {
                        collection.add(parseValue(e, childType));
                    }
                }

                continue;
            }

            // check whether the property is a model type
            var isModel = TypeToken.get(propType).getRawType().isAnnotationPresent(Model.class);
            if (isModel) {
                var id = parseModelReference(obj.getAsJsonObject(name));
                modelTargets.put(new SimplePropertyTarget(accessor, instance), id);
                continue;
            }

            // the property is not a model - deserialize it normally
            var prop = obj.get(name);
            var value = parseValue(prop, propType);

            accessor.set(instance, value);
        }

        return instance;
    }

    private int parseModelReference(JsonObject obj) {
        // parse the model id from the json object
        return obj.get("id").getAsInt();
    }

    private static boolean isPrimitiveOrString(Type type) throws Exception {
        var clazz = TypeToken.get(type).getRawType();
        if (String.class.isAssignableFrom(clazz)) return true;

        // read the PRIMITIVE_TYPES field from JsonPrimitive using reflection
        var primitiveTypesField = JsonPrimitive.class.getDeclaredField("PRIMITIVE_TYPES");
        primitiveTypesField.setAccessible(true);
        var PRIMITIVE_TYPES = (Class<?>[]) primitiveTypesField.get(null);

        for (var standardPrimitive : PRIMITIVE_TYPES) {
            if (standardPrimitive.isAssignableFrom(clazz)) {
                return true;
            }
        }

        return false;
    }
}
