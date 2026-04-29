package com.todo.app.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.todo.app.R
import com.todo.app.data.repository.TaskDatabase
import com.todo.app.data.repository.TaskRepository
import com.todo.app.data.model.Task
import com.todo.app.databinding.ActivityMainBinding
import com.todo.app.ui.FilterType
import com.todo.app.ui.TaskViewModel
import com.todo.app.ui.TaskViewModelFactory
import com.todo.app.ui.components.TaskAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository: TaskRepository by lazy(LazyThreadSafetyMode.NONE) {
        TaskRepository(TaskDatabase.getDatabase(applicationContext).taskDao())
    }
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(application, repository)
    }
    private lateinit var adapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupFab()
        setupFilterChips()
        observeData()
        animateHeader()
    }

    private fun animateHeader() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_slide_in)
        binding.toolbar.startAnimation(fadeIn)
        binding.chipGroupFilter.startAnimation(fadeIn)
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter(
            onToggle = { task -> viewModel.toggleComplete(task) },
            onDelete = { task -> confirmDelete(task) },
            onClick = { task -> openEditTask(task) }
        )
        binding.recyclerView.apply {
            this.adapter = this@MainActivity.adapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            setHasFixedSize(true)
        }

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION || position >= adapter.currentList.size) {
                    adapter.notifyDataSetChanged()
                    return
                }
                val task = adapter.currentList[position]
                viewModel.deleteTask(task)
                Snackbar.make(binding.root, R.string.task_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { viewModel.addTask(task) }
                    .show()
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            val intent = Intent(this, AddEditTaskActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out)
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener { viewModel.setFilter(FilterType.ALL) }
        binding.chipActive.setOnClickListener { viewModel.setFilter(FilterType.ACTIVE) }
        binding.chipDone.setOnClickListener { viewModel.setFilter(FilterType.COMPLETED) }
    }

    private fun observeData() {
        viewModel.tasks.observe(this) { tasks ->
            adapter.submitList(tasks)
            updateEmptyState(tasks.isEmpty())
            updateTaskCount(tasks)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateTaskCount(tasks: List<Task>) {
        val total = tasks.size
        val done = tasks.count { it.isCompleted }
        binding.tvTaskCount.text = getString(R.string.task_count, done, total)
        binding.progressTasks.max = total
        binding.progressTasks.progress = done
    }

    private fun confirmDelete(task: Task) {
        viewModel.deleteTask(task)
        Snackbar.make(binding.root, R.string.task_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { viewModel.addTask(task) }
            .show()
    }

    private fun openEditTask(task: Task) {
        val intent = Intent(this, AddEditTaskActivity::class.java)
        intent.putExtra(AddEditTaskActivity.EXTRA_TASK_ID, task.id)
        intent.putExtra(AddEditTaskActivity.EXTRA_TASK_TITLE, task.title)
        intent.putExtra(AddEditTaskActivity.EXTRA_TASK_DESC, task.description)
        intent.putExtra(AddEditTaskActivity.EXTRA_TASK_PRIORITY, task.priority.name)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_completed -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.clear_completed)
                    .setMessage(R.string.clear_completed_confirm)
                    .setPositiveButton(R.string.yes) { _, _ -> viewModel.deleteCompleted() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
