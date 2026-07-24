package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [StockItem::class, StockTransaction::class], version = 1, exportSchema = false)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao

    companion object {
        @Volatile
        private var INSTANCE: StockDatabase? = null

        fun getDatabase(context: Context): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        StockDatabase::class.java,
                        "stock_database"
                    )
                    .fallbackToDestructiveMigration(true)
                    .build()
                } catch (e: Exception) {
                    Log.e("StockDatabase", "Failed to build database, recreating", e)
                    context.applicationContext.deleteDatabase("stock_database")
                    Room.databaseBuilder(
                        context.applicationContext,
                        StockDatabase::class.java,
                        "stock_database"
                    )
                    .fallbackToDestructiveMigration(true)
                    .build()
                }
                INSTANCE = instance
                instance
            }
        }
    }
}

