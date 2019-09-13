package net.crushedpixel.persian;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import net.crushedpixel.persian.annotations.Model;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class Serializer {

    private PropertyRegistry properties = new PropertyRegistry();
    private List<Object> modelObjects = new ArrayList<>();

    String serialize(Object obj) throws Exception {
        if (!obj.getClass().isAnnotationPresent(Model.class)) {
            throw new IllegalArgumentException("Object's class must have the @Model annotation");
        }

        // serialize the root object
        var root = serializeObject(obj, obj.getClass(), true);

        // serialize each model, adding them to a JSON array.
        // serializing models can find new model objects to serialize,
        // so we repeat this step until all models are resolved.
        List<Object> serializedModels = new ArrayList<>();
        var modelsArr = new JsonArray();

        do {
            for (var model : new ArrayList<>(modelObjects)) {
                if (serializedModels.stream().anyMatch(m -> m == model)) continue;

                JsonObject modelObj = new JsonObject();
                modelObj.add("type", new JsonPrimitive(model.getClass().getName()));
                modelObj.add("value", serializeInstance(model, model.getClass()));

                modelsArr.add(modelObj);
                serializedModels.add(model);
            }
        } while (serializedModels.size() < modelObjects.size());

        JsonObject json = new JsonObject();
        json.add("root", root);
        json.add("models", modelsArr);

        return json.toString();
    }

    private JsonElement serializeObject(Object obj, Type genericType) throws Exception {
        Class<?> type = TypeToken.get(genericType).getRawType();

        // treat collections specially
        if (Collection.class.isAssignableFrom(type)) {
            return parseCollection((Collection<?>) obj, genericType);
        }

        // check whether the field is of a model type
        var isModel = type.isAnnotationPresent(Model.class);
        return serializeObject(obj, genericType, isModel);
    }

    private JsonElement serializeObject(Object obj, Type genericType, boolean isModel) throws Exception {
        if (isModel) {
            JsonObject json = new JsonObject();
            int id = registerModel(obj);
            json.add("id", new JsonPrimitive(id));
            return json;
        }

        return serializeInstance(obj, genericType);
    }

    private static boolean isPrimitiveOrString(Object obj) throws Exception {
        Method m = JsonPrimitive.class.getDeclaredMethod("isPrimitiveOrString", Object.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, obj);
    }

    private JsonElement serializeInstance(Object obj, Type genericType) throws Exception {
        if (isPrimitiveOrString(obj)) {
            var primitive = new JsonPrimitive("");

            // use reflection to invoke JsonPrimitive#setValue(Object)
            Method setValue = JsonPrimitive.class.getDeclaredMethod("setValue", Object.class);
            setValue.setAccessible(true);
            setValue.invoke(primitive, obj);

            return primitive;
        }

        // serialize each property
        var json = new JsonObject();

        for (var entry : properties.getAccessors(TypeToken.get(genericType).getRawType()).entrySet()) {
            var name = entry.getKey();
            var accessor = entry.getValue();
            var serialized = serializeObject(accessor.get(obj), accessor.getGenericType());
            json.add(name, serialized);
        }

        return json;
    }

    private JsonArray parseCollection(Collection<?> collection, Type genericType) throws Exception {
        // the field is a collection.
        // check if elements contained are models
        boolean childrenAreModels = false;

        // try to get the type of the elements contained
        // from the collection's type arguments
        if (!(genericType instanceof ParameterizedType)
                || ((ParameterizedType) genericType).getActualTypeArguments().length < 1) {
            throw new IllegalArgumentException("Can't serialize raw collections!");
        }

        var pType = (ParameterizedType) genericType;
        var childType = TypeToken.get(pType.getActualTypeArguments()[0]).getRawType();
        childrenAreModels = childType.isAnnotationPresent(Model.class);

        // serialize the collection
        JsonArray json = new JsonArray();
        for (Object child : collection) {
            json.add(serializeObject(child, childType, childrenAreModels));
        }

        return json;
    }

    /**
     * Registers a model, if not yet registered,
     * and returns the model's id.
     *
     * @param model The model to register.
     * @return The model's id.
     */
    private int registerModel(Object model) {
        // add the model to the list of models if and only if this particular instance
        // isn't contained in the list yet. this way, each unique model instance will
        // have their own unique index in the list.
        for (int i = 0; i < modelObjects.size(); i++) {
            if (modelObjects.get(i) == model) return i;
        }

        modelObjects.add(model);
        return modelObjects.size() - 1;
    }
}
