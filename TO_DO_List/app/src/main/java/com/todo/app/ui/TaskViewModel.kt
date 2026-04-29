package com.todo.app.ui

import android.app.Application
import androidx.lifecycle.*
import com.todo.app.data.model.Task
import com.todo.app.data.repository.TaskRepository
import kotlinx.coroutines.launch

enum class FilterType { ALL, ACTIVE, COMPLETED }

class TaskViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {
    private val _currentFilter = MutableLiveData(FilterType.ALL)
    val currentFilter: LiveData<FilterType> = _currentFilter

    val tasks: LiveData<List<Task>> = _currentFilter.switchMap { filter ->
        when (filter) {
            FilterType.ACTIVE -> repository.activeTasks
            FilterType.COMPLETED -> repository.completedTasks
            else -> repository.allTasks
        }
    }

    fun setFilter(filter: FilterType) {
        _currentFilter.value = filter
    }

    fun addTask(task: Task) = viewModelScope.launch {
        repository.insert(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.update(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.delete(task)
    }

    fun toggleComplete(task: Task) = viewModelScope.launch {
        repository.update(task.copy(isCompleted = !task.isCompleted))
    }

    fun deleteCompleted() = viewModelScope.launch {
        repository.deleteCompleted()
    }
}

class TaskViewModelFactory(
    private val application: Application,
    private val repository: TaskRepository?
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            val injectedRepository = requireNotNull(repository) {
                "TaskRepository must be provided to TaskViewModelFactory"
            }
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application, injectedRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
