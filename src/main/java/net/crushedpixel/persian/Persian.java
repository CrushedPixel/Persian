package net.crushedpixel.persian;

public class Persian {

    public static String serialize(Object obj) throws Exception {
        return new Serializer().serialize(obj);
    }

    public static <T> T deserialize(String json, Class<T> type) throws Exception {
        return new Deserializer().deserialize(json, type);
    }

    private Persian() {
    }

}
