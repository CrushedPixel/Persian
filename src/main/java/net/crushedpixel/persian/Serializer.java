package net.crushedpixel.persian;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class Serializer {

    private PropertyRegistry properties = new PropertyRegistry();
    private List<Object> modelObjects = new ArrayList<>();

    String serialize(Object obj) throws Exception {
        // serialize the root object
        var root = serializeModel(obj);

        // serialize each model that is referenced by the root model,
        // adding them to a JSON array.
        // serializing models can find new model objects to serialize,
        // so we repeat this step until all models are resolved.
        List<Object> serializedModels = new ArrayList<>();
        var modelsArr = new JsonArray();

        while (serializedModels.size() < modelObjects.size()) {
            for (var model : new ArrayList<>(modelObjects)) {
                if (serializedModels.stream().anyMatch(m -> m == model)) continue;

                JsonObject modelObj = new JsonObject();
                modelObj.add("type", new JsonPrimitive(model.getClass().getName()));
                modelObj.add("value", serializeModel(model));

                modelsArr.add(modelObj);
                serializedModels.add(model);
            }
        }

        JsonObject json = new JsonObject();
        json.add("root", root);
        json.add("models", modelsArr);

        return json.toString();
    }

    /**
     * Serializes a model object, returning its JSON representation.
     */
    private JsonElement serializeModel(Object obj) throws Exception {
        // serialize each property
        var json = new JsonObject();

        for (var entry : properties.getAccessors(obj.getClass()).entrySet()) {
            var name = entry.getKey();
            var accessor = entry.getValue();
            var serialized = serializeProperty(accessor.get(obj));
            json.add(name, serialized);
        }

        return json;
    }

    /**
     * Serializes a property.
     */
    private JsonElement serializeProperty(Object obj) throws Exception {
        // treat collections specially
        if (Collection.class.isAssignableFrom(obj.getClass())) {
            return parseCollection((Collection<?>) obj);
        }

        return serializeObject(obj);
    }

    private static boolean isPrimitiveOrString(Object obj) throws Exception {
        Method m = JsonPrimitive.class.getDeclaredMethod("isPrimitiveOrString", Object.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, obj);
    }

    /**
     * Serializes a single object. If the object is a model (i.e. non-primitive),
     * it registers it for serialization and returns an element describing a reference to the object.
     */
    private JsonElement serializeObject(Object obj) throws Exception {
        if (isPrimitiveOrString(obj)) {
            var primitive = new JsonPrimitive("");

            // use reflection to invoke JsonPrimitive#setValue(Object)
            Method setValue = JsonPrimitive.class.getDeclaredMethod("setValue", Object.class);
            setValue.setAccessible(true);
            setValue.invoke(primitive, obj);

            return primitive;
        }

        JsonObject json = new JsonObject();
        int id = registerModel(obj);
        json.add("id", new JsonPrimitive(id));
        return json;
    }

    private JsonArray parseCollection(Collection<?> collection) throws Exception {
        // serialize the collection
        JsonArray json = new JsonArray();
        for (Object child : collection) {
            json.add(serializeProperty(child));
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
