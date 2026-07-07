package com.example.expensetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expensetracker.budget.BudgetDao
import com.example.expensetracker.budget.BudgetEntity
import com.example.expensetracker.tag.TagDao
import com.example.expensetracker.tag.TagEntity
import com.example.expensetracker.data.charts.ExpenseChartDao

@Database(
    entities = [
        TodoItem::class,
        TagEntity::class,
        BudgetEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao
    abstract fun tagDao(): TagDao
    abstract fun budgetDao(): BudgetDao

    abstract fun expenseChartDao(): ExpenseChartDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budgets` " +
                    "(`tagName` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                    "PRIMARY KEY(`tagName`))"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
