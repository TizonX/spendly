package com.example.expensetracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todoItem: TodoItem): Long

    @Delete
    suspend fun delete(todo: TodoItem)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteById(id: Int)


    @Query("SELECT * FROM todo_items")
    fun getAll(): Flow<List<TodoItem>>

    @Query("""
    SELECT SUM(amount)
    FROM todo_items
    WHERE strftime('%Y-%m', createdAt / 1000, 'unixepoch') =
          strftime('%Y-%m', 'now')
""")
    fun getCurrentMonthTotal(): kotlinx.coroutines.flow.Flow<Double?>

    @Query("""
UPDATE todo_items
SET payee = :payee,
    amount = :amount,
    tag = :tag,
    updatedAt = :updatedAt,
    isEdited = 1
WHERE id = :id
""")
    suspend fun updateTodo(
        id: Int,
        payee: String,
        amount: String,
        tag: String,
        updatedAt: Long
    )

    @Query("SELECT DISTINCT tag FROM todo_items WHERE tag IS NOT NULL")
    fun getAllTags(): kotlinx.coroutines.flow.Flow<List<String>>

    @Query("""
SELECT tag
FROM todo_items
WHERE LOWER(TRIM(payee)) = LOWER(TRIM(:payee))
AND tag IS NOT NULL
AND tag != 'UPI'
AND tag != 'Other'
GROUP BY tag
ORDER BY COUNT(*) DESC
LIMIT 1
""")
    suspend fun getMostUsedTagForPayee(payee: String): String?

    @Query("""
SELECT tag
FROM (SELECT tag, amount FROM todo_items ORDER BY createdAt DESC LIMIT 50)
WHERE tag IS NOT NULL
AND tag != 'UPI'
AND tag != 'Other'
AND CAST(amount AS REAL) BETWEEN :minAmount AND :maxAmount
GROUP BY tag
ORDER BY COUNT(*) DESC
LIMIT 1
""")
    suspend fun getMostUsedTagNearAmount(minAmount: Double, maxAmount: Double): String?

    @Query("SELECT * FROM todo_items")
    suspend fun getAllList(): List<TodoItem>

    @Query("""
SELECT * FROM todo_items
WHERE createdAt < :lastTime
   OR (createdAt = :lastTime AND id < :lastId)
ORDER BY createdAt DESC, id DESC
LIMIT :limit
""")
    suspend fun getBefore(
        lastTime: Long,
        lastId: Int,
        limit: Int
    ): List<TodoItem>
}
