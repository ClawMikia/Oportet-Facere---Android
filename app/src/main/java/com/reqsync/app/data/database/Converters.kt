package com.reqsync.app.data.database

import androidx.room.TypeConverter
import com.reqsync.app.data.database.entities.*

class Converters {

    @TypeConverter
    fun fromRequirementStatus(value: RequirementStatus): String = value.name

    @TypeConverter
    fun toRequirementStatus(value: String): RequirementStatus =
        RequirementStatus.valueOf(value)

    @TypeConverter
    fun fromPriority(value: Priority): String = value.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)

    @TypeConverter
    fun fromAchievementCategory(value: AchievementCategory): String = value.name

    @TypeConverter
    fun toAchievementCategory(value: String): AchievementCategory =
        AchievementCategory.valueOf(value)

    @TypeConverter
    fun fromAttachmentType(value: AttachmentType): String = value.name

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType = AttachmentType.valueOf(value)
}
