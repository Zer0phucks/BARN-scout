package com.vpt.scout

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vpt.scout.data.local.CollectionDao
import com.vpt.scout.data.local.Converters
import com.vpt.scout.data.local.PropertyDao
import com.vpt.scout.data.local.ScoutResultDao
import com.vpt.scout.data.local.CollectionEntity
import com.vpt.scout.data.local.CollectionPropertyEntity
import com.vpt.scout.data.local.PropertyEntity
import com.vpt.scout.data.local.ScoutResultEntity

@Database(
    entities = [
        PropertyEntity::class,
        CollectionEntity::class,
        CollectionPropertyEntity::class,
        ScoutResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ScoutDatabase : RoomDatabase() {
    
    abstract fun propertyDao(): PropertyDao
    abstract fun collectionDao(): CollectionDao
    abstract fun scoutResultDao(): ScoutResultDao
    
    companion object {
        @Volatile
        private var INSTANCE: ScoutDatabase? = null
        
        fun getDatabase(context: Context): ScoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScoutDatabase::class.java,
                    "scout_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
