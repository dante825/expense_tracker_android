package com.kangwei.expensetracker.data.db.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseTagCrossRef
import com.kangwei.expensetracker.data.db.entity.TagEntity

data class ExpenseWithDetails(
    @Embedded val expense: ExpenseEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ExpenseTagCrossRef::class,
            parentColumn = "expenseId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
