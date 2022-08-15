package com.fanruan.serializer;

/**
 * @author Yichen Dai
 */
public interface Serializer {
    /**
     * Use to serialize a object to a byte array
     * @param object to be serialized
     * @return byte[] serialized data with the format of byte array
     */
    byte[] serialize(Object object);

    /**
     * Use to deserialize a byte array to a class with designate class
     * @param bytes Serialized data with the format of byte array
     * @param clazz The class of the object to be deserialized
     * @return object as designate class
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
