package com.example.callyzer2.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE customerId = :customerId ORDER BY updatedAt DESC")
    fun getNotesByCustomer(customerId: Long): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE callId = :callId ORDER BY updatedAt DESC")
    fun getNotesByCall(callId: Long): LiveData<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Long): LiveData<Note>
}
