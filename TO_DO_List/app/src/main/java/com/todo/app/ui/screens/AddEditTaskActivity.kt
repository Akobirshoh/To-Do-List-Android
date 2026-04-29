package com.todo.app.ui.screens

import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.todo.app.R
import com.todo.app.data.model.Priority
import com.todo.app.data.model.Task
import com.todo.app.data.repository.TaskDatabase
import com.todo.app.data.repository.TaskRepository
import com.todo.app.databinding.ActivityAddEditTaskBinding
import com.todo.app.ui.TaskViewModel
import com.todo.app.ui.TaskViewModelFactory

class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTaskBinding
    private val repository: TaskRepository by lazy(LazyThreadSafetyMode.NONE) {
        TaskRepository(TaskDatabase.getDatabase(applicationContext).taskDao())
    }
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(application, repository)
    }

    private var taskId: Int = -1
    private var selectedPriority: Priority = Priority.MEDIUM

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESC = "extra_task_desc"
        const val EXTRA_TASK_PRIORITY = "extra_task_priority"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadExistingTask()
        setupPrioritySelector()
        setupSaveButton()
        animateEntrance()
    }

    private fun animateEntrance() {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
        binding.cardForm.startAnimation(slideUp)
    }

    private fun loadExistingTask() {
        taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId != -1) {
            supportActionBar?.title = getString(R.string.edit_task)
            binding.etTitle.setText(intent.getStringExtra(EXTRA_TASK_TITLE))
            binding.etDescription.setText(intent.getStringExtra(EXTRA_TASK_DESC))
            val priority = intent.getStringExtra(EXTRA_TASK_PRIORITY)
                ?.let { value -> runCatching { Priority.valueOf(value) }.getOrNull() }
                ?: Priority.MEDIUM
            selectedPriority = priority
            updatePriorityUI(priority)
        } else {
            supportActionBar?.title = getString(R.string.add_task)
        }
    }

    private fun setupPrioritySelector() {
        binding.btnPriorityLow.setOnClickListener {
            selectedPriority = Priority.LOW
            updatePriorityUI(Priority.LOW)
        }
        binding.btnPriorityMedium.setOnClickListener {
            selectedPriority = Priority.MEDIUM
            updatePriorityUI(Priority.MEDIUM)
        }
        binding.btnPriorityHigh.setOnClickListener {
            selectedPriority = Priority.HIGH
            updatePriorityUI(Priority.HIGH)
        }
        updatePriorityUI(selectedPriority)
    }

    private fun updatePriorityUI(priority: Priority) {
        val activeAlpha = 1f
        val inactiveAlpha = 0.6f
        binding.btnPriorityLow.alpha = if (priority == Priority.LOW) activeAlpha else inactiveAlpha
        binding.btnPriorityMedium.alpha = if (priority == Priority.MEDIUM) activeAlpha else inactiveAlpha
        binding.btnPriorityHigh.alpha = if (priority == Priority.HIGH) activeAlpha else inactiveAlpha

        val outline = ContextCompat.getColor(this, R.color.purple_200)
        val lowColor = ContextCompat.getColor(this, R.color.priority_low)
        val mediumColor = ContextCompat.getColor(this, R.color.priority_medium)
        val highColor = ContextCompat.getColor(this, R.color.priority_high)

        binding.btnPriorityLow.strokeColor = android.content.res.ColorStateList.valueOf(
            if (priority == Priority.LOW) lowColor else outline
        )
        binding.btnPriorityMedium.strokeColor = android.content.res.ColorStateList.valueOf(
            if (priority == Priority.MEDIUM) mediumColor else outline
        )
        binding.btnPriorityHigh.strokeColor = android.content.res.ColorStateList.valueOf(
            if (priority == Priority.HIGH) highColor else outline
        )

        val selected = when (priority) {
            Priority.LOW -> binding.btnPriorityLow
            Priority.MEDIUM -> binding.btnPriorityMedium
            Priority.HIGH -> binding.btnPriorityHigh
        }

        selected.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).withEndAction {
            selected.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            if (title.isEmpty()) {
                binding.tilTitle.error = getString(R.string.title_required)
                binding.tilTitle.requestFocus()
                return@setOnClickListener
            }
            binding.tilTitle.error = null
            val description = binding.etDescription.text.toString().trim()
            val task = Task(
                id = if (taskId != -1) taskId else 0,
                title = title,
                description = description,
                priority = selectedPriority
            )
            if (taskId != -1) viewModel.updateTask(task) else viewModel.addTask(task)

            binding.btnSave.isEnabled = false
            binding.cardForm.animate().alpha(0f).translationY(60f).setDuration(250).withEndAction {
                finish()
                overridePendingTransition(0, R.anim.slide_out_bottom)
            }.start()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.slide_out_bottom)
    }
}
