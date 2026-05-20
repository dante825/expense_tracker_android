package com.kangwei.expensetracker.data.db.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.kangwei.expensetracker.data.db.entity.ExpenseEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseTagCrossRef
import com.kangwei.expensetracker.data.db.entity.TagEntity

data class TagWithExpenses(
    @Embedded val tag: TagEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ExpenseTagCrossRef::class,
            parentColumn = "tagId",
            entityColumn = "expenseId"
        )
    )
    val expenses: List<ExpenseEntity>
)
