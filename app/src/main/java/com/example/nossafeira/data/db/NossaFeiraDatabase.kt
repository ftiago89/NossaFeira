package com.example.nossafeira.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nossafeira.data.dao.ItemFeiraDao
import com.example.nossafeira.data.dao.ListaFeiraDao
import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.data.model.ItemFeira
import com.example.nossafeira.data.model.ListaFeira

class CategoriaConverter {
    @TypeConverter
    fun fromCategoria(value: Categoria): String = value.name

    @TypeConverter
    fun toCategoria(value: String): Categoria = Categoria.valueOf(value)
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE itens_feira ADD COLUMN preco REAL NOT NULL DEFAULT 0.0")
    }
}

@Database(
    entities = [ListaFeira::class, ItemFeira::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(CategoriaConverter::class)
abstract class NossaFeiraDatabase : RoomDatabase() {

    abstract fun listaFeiraDao(): ListaFeiraDao
    abstract fun itemFeiraDao(): ItemFeiraDao

    companion object {
        private const val DATABASE_NAME = "nossa_feira.db"

        @Volatile
        private var INSTANCE: NossaFeiraDatabase? = null

        fun getInstance(context: Context): NossaFeiraDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NossaFeiraDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}
