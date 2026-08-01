package com.htmltoapk.studio.data.local.converter

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromBoolean(v: Boolean?): Int? = v?.let { if (it) 1 else 0 }
    @TypeConverter fun toBoolean(v: Int?): Boolean? = v?.let { it == 1 }
}
