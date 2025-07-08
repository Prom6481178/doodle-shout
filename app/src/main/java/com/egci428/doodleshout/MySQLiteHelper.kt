package com.egci428.doodleshout

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class MySQLiteHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "doodle_jump.db"
        private const val DATABASE_VERSION = 1

        // Table name
        private const val TABLE_SCORES = "scores"

        // Column names
        private const val COLUMN_ID = "id"
        private const val COLUMN_SCORE = "score"

        // Create table SQL query
        private const val CREATE_SCORES_TABLE = """
            CREATE TABLE $TABLE_SCORES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SCORE INTEGER NOT NULL
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_SCORES_TABLE)
        Log.d("DatabaseHelper", "Database created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCORES")
        onCreate(db)
        Log.d("DatabaseHelper", "Database upgraded from version $oldVersion to $newVersion")
    }

    /**
     * Insert a new score into the database
     */
    fun insertScore(score: Int): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SCORE, score)
        }

        val result = db.insert(TABLE_SCORES, null, values)
        db.close()

        Log.d("DatabaseHelper", "Score inserted: $score, Result: $result")
        return result
    }

    /**
     * Get the highest score from the database
     */
    fun getHighScore(): Int {
        val db = this.readableDatabase
        val query = "SELECT MAX($COLUMN_SCORE) FROM $TABLE_SCORES"
        val cursor = db.rawQuery(query, null)

        var highScore = 0
        if (cursor.moveToFirst()) {
            highScore = cursor.getInt(0)
        }

        cursor.close()
        db.close()

        Log.d("DatabaseHelper", "High score retrieved: $highScore")
        return highScore
    }

    /**
     * Get all scores ordered by score (highest first)
     */
    fun getAllScores(): List<ScoreEntry> {
        val scores = mutableListOf<ScoreEntry>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_SCORES ORDER BY $COLUMN_SCORE DESC"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCORE))

                scores.add(ScoreEntry(id, score))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        Log.d("DatabaseHelper", "Retrieved ${scores.size} scores")
        return scores
    }

    /**
     * Get top N scores
     */
    fun getTopScores(limit: Int = 5): List<ScoreEntry> {
        val scores = mutableListOf<ScoreEntry>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_SCORES ORDER BY $COLUMN_SCORE DESC LIMIT $limit"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCORE))

                scores.add(ScoreEntry(id, score))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return scores
    }

    /**
     * Delete all scores (for testing purposes)
     */
    fun clearAllScores() {
        val db = this.writableDatabase
        db.delete(TABLE_SCORES, null, null)
        db.close()
        Log.d("DatabaseHelper", "All scores cleared")
    }
}