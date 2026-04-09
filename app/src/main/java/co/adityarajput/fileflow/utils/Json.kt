package co.adityarajput.fileflow.utils

import kotlinx.serialization.json.*
import java.io.Serializable

fun JsonElement.toSerializable(): Serializable? {
    return when (this) {
        is JsonObject -> mapValues { it.value.toSerializable() } as Serializable

        is JsonArray -> mapNotNull { it.toSerializable() } as Serializable

        is JsonPrimitive -> listOfNotNull(
            booleanOrNull,
            intOrNull,
            floatOrNull,
            doubleOrNull,
            longOrNull,
            contentOrNull,
        ).firstOrNull()
    }
}
