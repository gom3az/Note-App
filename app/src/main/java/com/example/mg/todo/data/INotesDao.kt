package com.example.mg.todo.data

import androidx.room.*
import com.example.mg.todo.data.model.NoteModel

@Dao
interface INotesDao {

    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<NoteModel>

    @Query("SELECT * FROM notes WHERE id LIKE :id")
    suspend fun findNoteById(id: Long): NoteModel

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteModel): Long

    @Delete
    suspend fun deleteNote(vararg note: NoteModel): Int

    @Update
    suspend fun updateNote(vararg note: NoteModel): Int
}