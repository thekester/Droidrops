package com.readrops.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationsTest {

    private val dbName = "TEST-DB"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        Database::class.java
    )

    @Test
    fun migrate1To2() {
        helper.createDatabase(dbName, 1).apply {
            close()
        }

        helper.runMigrationsAndValidate(dbName, 2, true, MigrationFrom1To2).apply {
            close()
        }
    }

    @Test
    fun migrate2to3() {
        helper.createDatabase(dbName, 2).apply {
            close()
        }

        helper.runMigrationsAndValidate(dbName, 3, true, MigrationFrom2To3).apply {
            close()
        }
    }

    @Test
    fun migrate3to4() {
        helper.createDatabase(dbName, 3).apply {
            execSQL("Insert Into Account(account_type, last_modified, current_account, notifications_enabled) Values(0, 0, 0, 0)")
            execSQL("Insert Into Feed(text_color, background_color, account_id, notification_enabled) Values(0, 0, 3, 0)")
            execSQL("Insert Into Item(title, feed_id, read_time, read, starred, read_it_later, guid) values(\"test\", 12, 0, 0, 0, 0, \"guid\")")
        }

        helper.runMigrationsAndValidate(dbName, 4, true, MigrationFrom3To4).apply {
            val remoteId = compileStatement("Select remote_id From Item").simpleQueryForString()
            assertEquals("guid", remoteId)
        }
    }

    @Test
    fun migrate4To5() {
        helper.createDatabase(dbName, 4).apply {
            execSQL("Insert Into Account(account_type, last_modified, current_account, notifications_enabled) Values(0, 0, 1, 0)")
        }

        helper.runMigrationsAndValidate(dbName, 5, true, MigrationFrom4To5).apply {
            val type = compileStatement("Select type From Account").simpleQueryForString()
            assertEquals("LOCAL", type)
        }
    }

    @Test
    fun migrate6To7() {
        helper.createDatabase(dbName, 6).apply {
            close()
        }

        helper.runMigrationsAndValidate(dbName, 7, true, MigrationFrom6To7).apply {
            close()
        }
    }

    /**
     * The index on Item.remote_id is what keeps synchronization from scanning the whole
     * article table once per incoming item, so its presence after the upgrade is asserted
     * rather than merely relying on schema validation.
     */
    @Test
    fun migrate7To8() {
        helper.createDatabase(dbName, 7).apply {
            close()
        }

        helper.runMigrationsAndValidate(dbName, 8, true, MigrationFrom7To8).apply {
            val index = compileStatement(
                "Select name From sqlite_master Where type = 'index' And name = 'index_Item_remote_id'"
            ).simpleQueryForString()

            assertEquals("index_Item_remote_id", index)
            close()
        }
    }

    /**
     * This path did not exist at all, so a database left at version 5 could not be opened and
     * the application crashed on upgrade. runMigrationsAndValidate compares the result against
     * the exported schema, which is what makes this test meaningful rather than decorative.
     */
    @Test
    fun migrate5To6() {
        helper.createDatabase(dbName, 5).apply {
            close()
        }

        helper.runMigrationsAndValidate(dbName, 6, true, MigrationFrom5To6).apply {
            val tables = compileStatement(
                "Select count(*) From sqlite_master Where type = 'table' And name In ('Tag', 'TagJoin')"
            ).simpleQueryForLong()

            assertEquals(2L, tables)
            close()
        }
    }

    /**
     * The whole chain matters more than any single step: a user upgrading from an old install
     * goes through every migration in a row.
     */
    @Test
    fun migrate1To8() {
        helper.createDatabase(dbName, 1).apply {
            close()
        }

        helper.runMigrationsAndValidate(
            dbName, 8, true,
            MigrationFrom1To2, MigrationFrom2To3, MigrationFrom3To4, MigrationFrom4To5,
            MigrationFrom5To6, MigrationFrom6To7, MigrationFrom7To8
        ).apply {
            close()
        }
    }
}
