package com.example.callyzer2.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.callyzer2.data.AppDatabase
import com.example.callyzer2.data.Note
import com.example.callyzer2.databinding.ActivityAddNoteBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNoteBinding
    private lateinit var database: AppDatabase
    private var customerId: Long = 0
    private var callId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        customerId = intent.getLongExtra("customer_id", 0)
        callId = intent.getLongExtra("call_id", 0).takeIf { it != 0L }

        setupToolbar()
        setupSaveButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Note"
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveNote()
        }
    }

    private fun saveNote() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()

        if (title.isEmpty()) {
            binding.etTitle.error = "Title is required"
            return
        }

        if (content.isEmpty()) {
            binding.etContent.error = "Content is required"
            return
        }

        val note = Note(
            customerId = customerId,
            title = title,
            content = content,
            callId = callId
        )

        CoroutineScope(Dispatchers.IO).launch {
            database.noteDao().insertNote(note)
            finish()
        }
    }
}
