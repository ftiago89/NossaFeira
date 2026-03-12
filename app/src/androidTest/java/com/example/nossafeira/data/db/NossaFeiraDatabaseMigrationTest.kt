package com.example.nossafeira.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NossaFeiraDatabaseMigrationTest {

    private lateinit var dbFile: File

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        dbFile = context.getDatabasePath("test_migration.db")
        dbFile.parentFile?.mkdirs()
        dbFile.delete()
    }

    @After
    fun teardown() {
        dbFile.delete()
    }

    // MIGRATION 3 → 4 -------------------------------------------------------------------------

    @Test
    fun migration_3_to_4_adiciona_campos_de_compartilhamento_com_defaults_corretos() {
        // Arrange — schema v3
        val rawDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        rawDb.execSQL("""
            CREATE TABLE listas_feira (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nome TEXT NOT NULL,
                valorEstimado INTEGER NOT NULL DEFAULT 0,
                criadaEm INTEGER NOT NULL
            )
        """.trimIndent())
        rawDb.execSQL("INSERT INTO listas_feira (id, nome, criadaEm) VALUES (1, 'Lista existente', 1000)")
        rawDb.close()

        // Act — migration 3→4
        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        db.execSQL("ALTER TABLE listas_feira ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE listas_feira ADD COLUMN isShared INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE listas_feira ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE listas_feira ADD COLUMN syncedAt INTEGER NOT NULL DEFAULT 0")

        // Assert — registro existente deve ter defaults corretos
        val cursor = db.rawQuery(
            "SELECT remoteId, isShared, updatedAt, syncedAt FROM listas_feira WHERE id = 1",
            null
        )
        assertTrue(cursor.moveToFirst())
        assertNull(cursor.getString(0))         // remoteId = NULL
        assertEquals(0, cursor.getInt(1))       // isShared = false
        assertEquals(0L, cursor.getLong(2))     // updatedAt = 0
        assertEquals(0L, cursor.getLong(3))     // syncedAt = 0
        cursor.close()
        db.close()
    }

    // MIGRATION 4 → 5 -------------------------------------------------------------------------

    @Test
    fun migration_4_to_5_remoteItemId_preenchido_com_uuid_v4_valido_e_unico() {
        // Arrange — schema v4
        val rawDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        rawDb.execSQL("""
            CREATE TABLE listas_feira (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nome TEXT NOT NULL,
                valorEstimado INTEGER NOT NULL DEFAULT 0,
                criadaEm INTEGER NOT NULL,
                remoteId TEXT,
                isShared INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                syncedAt INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        rawDb.execSQL("""
            CREATE TABLE itens_feira (
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
        rawDb.execSQL("INSERT INTO listas_feira (id, nome, criadaEm) VALUES (1, 'Lista', 1000)")
        rawDb.execSQL("INSERT INTO itens_feira (id, listaId, nome, quantidade, categoria, criadoEm) VALUES (1, 1, 'Arroz', '1kg', 'OUTROS', 1000)")
        rawDb.execSQL("INSERT INTO itens_feira (id, listaId, nome, quantidade, categoria, criadoEm) VALUES (2, 1, 'Leite', '1L', 'LATICINIOS', 1000)")
        rawDb.execSQL("INSERT INTO itens_feira (id, listaId, nome, quantidade, categoria, criadoEm) VALUES (3, 1, 'Sabão', '1un', 'LIMPEZA', 1000)")
        rawDb.close()

        // Act — migration 4→5
        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        db.execSQL("ALTER TABLE itens_feira ADD COLUMN remoteItemId TEXT NOT NULL DEFAULT ''")
        db.execSQL("""
            UPDATE itens_feira SET remoteItemId =
                lower(hex(randomblob(4))) || '-' ||
                lower(hex(randomblob(2))) || '-4' ||
                substr(lower(hex(randomblob(2))), 2) || '-' ||
                substr('89ab', abs(random()) % 4 + 1, 1) ||
                substr(lower(hex(randomblob(2))), 2) || '-' ||
                lower(hex(randomblob(6)))
        """.trimIndent())

        // Assert — formato UUID v4 e unicidade
        val cursor = db.rawQuery("SELECT remoteItemId FROM itens_feira ORDER BY id", null)
        val uuids = mutableListOf<String>()
        while (cursor.moveToNext()) {
            uuids.add(cursor.getString(0))
        }
        cursor.close()
        db.close()

        val uuidV4Regex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        assertEquals("Deve haver 3 UUIDs (um por item)", 3, uuids.size)
        uuids.forEach { uuid ->
            assertTrue("'$uuid' não é UUID v4 válido", uuid.matches(uuidV4Regex))
        }
        assertEquals("Todos os UUIDs devem ser únicos", 3, uuids.toSet().size)
    }
}
