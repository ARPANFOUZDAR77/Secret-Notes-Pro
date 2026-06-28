package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun getNote(id: Int): Flow<Note?> = noteDao.getNoteById(id)

    suspend fun insert(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun update(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteById(id: Int) {
        noteDao.deleteNoteById(id)
    }

    suspend fun deleteAll() {
        noteDao.deleteAllNotes()
    }
}
