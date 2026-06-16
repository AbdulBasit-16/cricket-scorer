package com.cricket.scorer.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cricket.scorer.data.database.daos.*
import com.cricket.scorer.data.database.entities.*

@Database(
    entities = [
        PlayerEntity::class,
        MatchEntity::class,
        InningEntity::class,
        OverEntity::class,
        DeliveryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CricketDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun inningDao(): InningDao
    abstract fun overDao(): OverDao
    abstract fun deliveryDao(): DeliveryDao

    companion object {
        @Volatile
        private var INSTANCE: CricketDatabase? = null

        fun getDatabase(context: Context): CricketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CricketDatabase::class.java,
                    "cricket_scorer_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
