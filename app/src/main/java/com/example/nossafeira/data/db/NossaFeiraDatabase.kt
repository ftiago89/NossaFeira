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

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // listas_feira: valorEstimado REAL → INTEGER (centavos)
        db.execSQL("""
            CREATE TABLE listas_feira_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nome TEXT NOT NULL,
                valorEstimado INTEGER NOT NULL DEFAULT 0,
                criadaEm INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO listas_feira_new (id, nome, valorEstimado, criadaEm)
            SELECT id, nome, CAST(ROUND(valorEstimado * 100) AS INTEGER), criadaEm
            FROM listas_feira
        """.trimIndent())
        db.execSQL("DROP TABLE listas_feira")
        db.execSQL("ALTER TABLE listas_feira_new RENAME TO listas_feira")

        // itens_feira: preco REAL → INTEGER (centavos)
        db.execSQL("""
            CREATE TABLE itens_feira_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                listaId INTEGER NOT NULL,
                nome TEXT NOT NULL,
                quantidade TEXT NOT NULL,
                categoria TEXT NOT NULL,
                preco INTEGER NOT NULL DEFAULT 0,
                comprado INTEGER NOT NULL DEFAULT 0,
                criadoEm INTEGER NOT NULL,
                FOREIGN KEY (listaId) REFERENCES listas_feira(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO itens_feira_new (id, listaId, nome, quantidade, categoria, preco, comprado, criadoEm)
            SELECT id, listaId, nome, quantidade, categoria, CAST(ROUND(preco * 100) AS INTEGER), comprado, criadoEm
            FROM itens_feira
        """.trimIndent())
        db.execSQL("DROP TABLE itens_feira")
        db.execSQL("ALTER TABLE itens_feira_new RENAME TO itens_feira")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_itens_feira_listaId ON itens_feira(listaId)")
    }
}

@Database(
    entities = [ListaFeira::class, ItemFeira::class],
    version = 3,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}
