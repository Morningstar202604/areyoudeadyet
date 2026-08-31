package com.silema.app.db

import com.silema.app.data.Contact
import com.silema.app.data.VitalRecord
import com.silema.app.data.Workout
import com.silema.app.db.entity.ContactEntity
import com.silema.app.db.entity.VitalRecordEntity
import com.silema.app.db.entity.WorkoutEntity

/**
 * Room Entity ↔ 领域模型 双向映射。
 *
 * 单独放一个文件，避免 Repository 里散落转换逻辑。
 */

// ---------- VitalRecord ----------

fun VitalRecordEntity.toDomain(): VitalRecord = VitalRecord(
    typeId = typeId,
    value = value,
    timestampMillis = timestampMillis,
    source = source
)

fun VitalRecord.toEntity(): VitalRecordEntity = VitalRecordEntity(
    typeId = typeId,
    value = value,
    timestampMillis = timestampMillis,
    source = source
)

@JvmName("vitalRecordEntitiesToDomain")
fun List<VitalRecordEntity>.toDomain(): List<VitalRecord> = map { it.toDomain() }
fun List<VitalRecord>.toEntity(): List<VitalRecordEntity> = map { it.toEntity() }

// ---------- Contact ----------

fun ContactEntity.toDomain(): Contact = Contact(name = name, phone = phone)
fun Contact.toEntity(): ContactEntity = ContactEntity(phone = phone, name = name)
@JvmName("contactEntitiesToDomain")
fun List<ContactEntity>.toDomain(): List<Contact> = map { it.toDomain() }

// ---------- Workout ----------

fun WorkoutEntity.toDomain(): Workout = Workout(
    id = id,
    type = type,
    startMillis = startMillis,
    durationMillis = durationMillis,
    distanceMeters = distanceMeters,
    caloriesKcal = caloriesKcal,
    track = Converters().toTrack(track)
)

fun Workout.toEntity(): WorkoutEntity = WorkoutEntity(
    id = id,
    type = type,
    startMillis = startMillis,
    durationMillis = durationMillis,
    distanceMeters = distanceMeters,
    caloriesKcal = caloriesKcal,
    track = Converters().fromTrack(track)
)

@JvmName("workoutEntitiesToDomain")
fun List<WorkoutEntity>.toDomain(): List<Workout> = map { it.toDomain() }
