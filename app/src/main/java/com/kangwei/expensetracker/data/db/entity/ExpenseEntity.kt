package com.kangwei.expensetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("categoryId")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val description: String,
    val date: Long,
    val isIncome: Boolean,
    val createdAt: Long,
    val receiptData: ByteArray? = null,
    val categoryId: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExpenseEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
