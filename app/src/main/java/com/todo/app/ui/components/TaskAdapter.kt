package com.todo.app.ui.components

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.todo.app.R
import com.todo.app.data.model.Priority
import com.todo.app.data.model.Task
import com.todo.app.databinding.ItemTaskBinding

class TaskAdapter(
    private val onToggle: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private var lastAnimatedPosition = -1

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.apply {
                tvTitle.text = task.title
                tvDescription.text = if (task.description.isNotEmpty()) task.description else ""
                tvDescription.visibility = if (task.description.isNotEmpty()) View.VISIBLE else View.GONE

                if (task.isCompleted) {
                    tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    tvTitle.alpha = 0.9f
                    cardTask.alpha = 1f
                } else {
                    tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    tvTitle.alpha = 1f
                    cardTask.alpha = 1f
                }
                val surfaceColor = MaterialColors.getColor(cardTask, com.google.android.material.R.attr.colorSurface)
                val outlineColor = MaterialColors.getColor(cardTask, com.google.android.material.R.attr.colorOutlineVariant)
                cardTask.setCardBackgroundColor(
                    if (task.isCompleted) ContextCompat.getColor(root.context, R.color.task_done_card_bg) else surfaceColor
                )
                cardTask.strokeColor =
                    if (task.isCompleted) ContextCompat.getColor(root.context, R.color.task_done_card_stroke) else outlineColor
                val onSurface = MaterialColors.getColor(cardTask, com.google.android.material.R.attr.colorOnSurface)
                val onSurfaceVariant = MaterialColors.getColor(cardTask, com.google.android.material.R.attr.colorOnSurfaceVariant)
                tvTitle.setTextColor(onSurface)
                tvDescription.setTextColor(if (task.isCompleted) onSurface else onSurfaceVariant)

                val priorityColor = when (task.priority) {
                    Priority.HIGH -> ContextCompat.getColor(root.context, R.color.priority_high)
                    Priority.MEDIUM -> ContextCompat.getColor(root.context, R.color.priority_medium)
                    Priority.LOW -> ContextCompat.getColor(root.context, R.color.priority_low)
                }
                viewPriorityBar.setBackgroundColor(priorityColor)

                val priorityText = when (task.priority) {
                    Priority.HIGH -> root.context.getString(R.string.priority_high)
                    Priority.MEDIUM -> root.context.getString(R.string.priority_medium)
                    Priority.LOW -> root.context.getString(R.string.priority_low)
                }
                tvPriority.text = priorityText
                tvPriority.setTextColor(if (task.isCompleted) onSurfaceVariant else priorityColor)

                val doneColor = ContextCompat.getColor(root.context, R.color.priority_low)
                val activeColor = ContextCompat.getColor(root.context, R.color.purple_500)
                btnToggleDone.text = if (task.isCompleted) {
                    root.context.getString(R.string.mark_active)
                } else {
                    root.context.getString(R.string.mark_done)
                }
                btnToggleDone.strokeColor = ColorStateList.valueOf(if (task.isCompleted) doneColor else activeColor)
                btnToggleDone.setTextColor(if (task.isCompleted) doneColor else activeColor)

                btnToggleDone.setOnClickListener {
                    animateCheck(btnToggleDone)
                    onToggle(task)
                }

                btnDelete.setOnClickListener {
                    animateDelete(cardTask) { onDelete(task) }
                }

                cardTask.setOnClickListener { onClick(task) }
            }
        }

        private fun animateCheck(view: View) {
            val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f, 1f)
            val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.3f, 1f)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY)
                duration = 300
                interpolator = OvershootInterpolator()
                start()
            }
        }

        private fun animateDelete(view: View, onEnd: () -> Unit) {
            val translateX = ObjectAnimator.ofFloat(view, "translationX", 0f, -view.width.toFloat())
            val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
            AnimatorSet().apply {
                playTogether(translateX, alpha)
                duration = 300
                start()
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        onEnd()
                        view.translationX = 0f
                        view.alpha = 1f
                    }
                })
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
        val currentPosition = holder.bindingAdapterPosition
        if (currentPosition != RecyclerView.NO_POSITION && currentPosition > lastAnimatedPosition) {
            animateItemEntry(holder.itemView, currentPosition)
            lastAnimatedPosition = currentPosition
        }
    }

    private fun animateItemEntry(view: View, position: Int) {
        view.translationY = 80f
        view.alpha = 0f
        val translateY = ObjectAnimator.ofFloat(view, "translationY", 80f, 0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        AnimatorSet().apply {
            playTogether(translateY, alpha)
            duration = 350
            startDelay = (position * 60L).coerceAtMost(300L)
            interpolator = OvershootInterpolator(0.8f)
            start()
        }
    }
}

class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
}
