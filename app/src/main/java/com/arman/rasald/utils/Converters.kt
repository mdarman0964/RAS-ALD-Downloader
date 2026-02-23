package com.arman.rasald.utils

import androidx.room.TypeConverter
import com.arman.rasald.data.entity.DownloadStatus
import com.arman.rasald.data.entity.DownloadType
import java.util.Date

/**
 * Type Converters for Room Database
 */
class Converters {

    // Date converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // DownloadStatus converters
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String {
        return status.name
    }

    @TypeConverter
    fun toDownloadStatus(status: String): DownloadStatus {
        return try {
            DownloadStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            DownloadStatus.PENDING
        }
    }

    // DownloadType converters
    @TypeConverter
    fun fromDownloadType(type: DownloadType): String {
        return type.name
    }

    @TypeConverter
    fun toDownloadType(type: String): DownloadType {
        return try {
            DownloadType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            DownloadType.VIDEO
        }
    }

    // List<String> converter for tags or other lists
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    }
}
