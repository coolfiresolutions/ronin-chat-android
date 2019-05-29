package com.coolfiresolutions.roninchat.server

import android.util.Log
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object JSONMapperUtil {
    const val TAG = "JSONMapperUtil"

    // re-use a single ObjectMapper so we're not creating multiple object mappers
    private var objectMapper: ObjectMapper? = null

    // A new reference is needed every time since JAVA date is not thread safe
    val dateFormatWithTimeZone: DateFormat
        get() {
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            df.timeZone = TimeZone.getTimeZone("GMT")
            return df
        }

    fun defaultMapper(): ObjectMapper {
        if (objectMapper == null) {
            objectMapper = ObjectMapper()
            objectMapper!!.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            objectMapper!!.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            objectMapper!!.disable(SerializationFeature.INDENT_OUTPUT)
            objectMapper!!.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            objectMapper!!.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            objectMapper!!.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            objectMapper!!.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
            objectMapper!!.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            objectMapper!!.setDefaultMergeable(true)
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            df.timeZone = TimeZone.getTimeZone("UTC")
            objectMapper!!.dateFormat = df
            objectMapper!!.setVisibility(
                    objectMapper!!.serializationConfig.defaultVisibilityChecker
                            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
            )
        }
        return objectMapper as ObjectMapper
    }

    fun <T> createObjectByJSONString(jsonMsg: String, c: Class<T>): T? {
        try {
            return defaultMapper().readValue(jsonMsg, c)
        } catch (e: InvalidTypeIdException) {
            Log.e(TAG, "Possibly failed to parse deprecated realtime message")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to parse JSON entity " + c.simpleName)
        } catch (e: NullPointerException) {
            Log.e(TAG, "Object Mapper was not created")
        }

        return null
    }

    fun <T> createObjectByJSONMap(jsonMap: Map<String, Any?>?, c: Class<T>): T {
        return defaultMapper().convertValue(jsonMap, c)
    }

    fun createJSONObjectByObject(`object`: Any): JSONObject? {
        try {
            val ow = defaultMapper().writer()
            val json = ow.writeValueAsString(`object`)
            return JSONObject(json)
        } catch (e: JSONException) {
            Log.e(TAG, e.message)
            return null
        } catch (e: JsonProcessingException) {
            Log.e(TAG, e.message)
            return null
        }

    }

    private fun convertJsonFormat(json: JSONObject): JsonNode? {
        val ret = JsonNodeFactory.instance.objectNode()
        val iterator = json.keys()

        while (iterator.hasNext()) {
            val key = iterator.next()
            val value: Any
            try {
                value = json.get(key)
            } catch (e: JSONException) {
                Log.e(TAG, "JSON Exception")
                return null
            }

            when {
                json.isNull(key) -> ret.putNull(key)
                value is String -> ret.put(key, value)
                value is Int -> ret.put(key, value)
                value is Long -> ret.put(key, value)
                value is Double -> ret.put(key, value)
                value is Boolean -> ret.put(key, value)
                value is JSONObject -> ret.put(key, convertJsonFormat(value))
                value is JSONArray -> ret.put(key, convertJsonFormat(value))
                else -> Log.e(TAG, "JSON Exception")
            }
        }
        return ret
    }

    private fun convertJsonFormat(json: JSONArray): JsonNode? {
        val ret = JsonNodeFactory.instance.arrayNode()
        for (i in 0 until json.length()) {
            val value: Any
            try {
                value = json.get(i)
            } catch (e: JSONException) {
                Log.e(TAG, "JSON Exception")
                return null
            }

            when {
                json.isNull(i) -> ret.addNull()
                value is String -> ret.add(value)
                value is Int -> ret.add(value)
                value is Long -> ret.add(value)
                value is Double -> ret.add(value)
                value is Boolean -> ret.add(value)
                value is JSONObject -> ret.add(convertJsonFormat(value))
                value is JSONArray -> ret.add(convertJsonFormat(value))
                else -> throw RuntimeException("Not prepared for converting instance of class " + value.javaClass)
            }
        }
        return ret
    }
}