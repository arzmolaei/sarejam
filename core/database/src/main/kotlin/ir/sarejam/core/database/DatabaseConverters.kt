package ir.sarejam.core.database

import androidx.room.TypeConverter
import org.json.JSONArray

class DatabaseConverters {
    /** JSON preserves field names even if they contain commas, quotes or other delimiters. */
    @TypeConverter
    fun encodeEditedFields(fields: Set<String>): String = JSONArray(fields.sorted()).toString()

    @TypeConverter
    fun decodeEditedFields(encoded: String): Set<String> {
        val array = JSONArray(encoded)
        return buildSet {
            for (index in 0 until array.length()) {
                val field = array.get(index)
                require(field is String) { "Invalid stored edited-field set." }
                add(field)
            }
        }
    }
}
