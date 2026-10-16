package com.avs.visitapp.Adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.avs.visitapp.R
import com.avs.visitapp.model.TaskModel

class TaskAdapter(
    private var taskList: List<TaskModel>,
    private val mode: String,                     // ADD THIS
    private val onTaskClick: (TaskModel) -> Unit
)
 : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvBranch: TextView = itemView.findViewById(R.id.tvBranch)
        val tvWork: TextView = itemView.findViewById(R.id.tvWork)
        val tvTaskDate: TextView = itemView.findViewById(R.id.tvTaskDate)
        val tvCompletion: TextView = itemView.findViewById(R.id.tvCompletion)
        val tvRemarks: TextView = itemView.findViewById(R.id.tvRemarks)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

//    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
//
//        val task = taskList[position]
//
//        // Merge data already done in TaskListActivity
//        holder.tvUserName.text = "User: ${task.AssignedToName ?: "Unknown"}"
//        holder.tvBranch.text = "Branch: ${task.BranchName ?: "Unknown"}"
//        holder.tvWork.text = "Work: ${task.TaskDescription}"
//        holder.tvTaskDate.text = "Task Date: ${task.TaskDate.take(10)}"
//        holder.tvCompletion.text = "Completion: ${task.CompletionDescription ?: "-"}"
//        holder.tvRemarks.text = "Remarks: ${task.OtherRemarks ?: "-"}"
//
//        // Status styling
//        holder.tvStatus.text = task.Status
//
//        when (task.Status) {
//            "Pending" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.orange))
//            "In Progress" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.blue))
//            "Completed" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.color_green))
//        }
//
//        // Enable click only for edit modes
//        holder.itemView.isEnabled = task.Status != "Completed"
//        holder.itemView.alpha = if (task.Status == "Completed") 0.6f else 1.0f
//
//        holder.itemView.setOnClickListener {
//            if (task.Status == "Pending" || task.Status == "In Progress") {
//                onTaskClick(task)
//            }
//        }
//    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {

        val task = taskList[position]

        // User details
        holder.tvUserName.text = "User: ${task.AssignedToName ?: "Unknown"}"
        holder.tvBranch.text = "Branch: ${task.BranchName ?: "Unknown"}"
        holder.tvWork.text = "Work: ${task.TaskDescription}"
        holder.tvTaskDate.text = "Task Date: ${task.TaskDate.take(10)}"

        // 🔥 SHOW/HIDE Completion & Remarks based on MODE
        if (mode == "MY_TASK") {
            holder.tvCompletion.visibility = View.GONE
            holder.tvRemarks.visibility = View.GONE
        } else {
            holder.tvCompletion.visibility = View.VISIBLE
            holder.tvRemarks.visibility = View.VISIBLE
            holder.tvCompletion.text = "Completion: ${task.CompletionDescription ?: "-"}"
            holder.tvRemarks.text = "Remarks: ${task.OtherRemarks ?: "-"}"
        }

        // Status styling
        holder.tvStatus.text = task.Status

        when (task.Status.uppercase()) {
            "PENDING" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.orange))
            "IN PROGRESS" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.blue))
            "DONE", "COMPLETED" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.color_green))
        }

        // Disable only for Assigned By / Assigned To modes
        if (mode == "MY_TASK") {
            holder.itemView.isEnabled = true
            holder.itemView.alpha = 1.0f
        } else {
            holder.itemView.isEnabled = task.Status != "Completed" && task.Status != "DONE"
            holder.itemView.alpha = if (task.Status == "Completed" || task.Status == "DONE") 0.6f else 1.0f
        }


        holder.itemView.setOnClickListener {
            if (task.Status == "Pending" || task.Status == "In Progress") {
                onTaskClick(task)
            }
        }
    }


    override fun getItemCount(): Int = taskList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateTasks(newTasks: List<TaskModel>) {
        taskList = newTasks
        notifyDataSetChanged()
    }
}



//package com.avs.visitapp.Adapter
//
//import android.annotation.SuppressLint
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.avs.visitapp.R
//import com.avs.visitapp.model.TaskModel
//
//class TaskAdapter(private var taskList: List<TaskModel>) :
//    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
//
//    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
//        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
//        val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
//        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
//        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_task, parent, false)
//        return TaskViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
//        val task = taskList[position]
//        holder.tvTitle.text = task.TaskTitle
//        holder.tvDescription.text = task.Description
//        holder.tvPriority.text = "Priority: ${task.Priority}"
//        holder.tvStatus.text = "Status: ${task.Status}"
//        holder.tvDate.text = task.CreatedDate.take(10)
//    }
//
//    override fun getItemCount(): Int = taskList.size
//
//    @SuppressLint("NotifyDataSetChanged")
//    fun updateTasks(newTasks: List<TaskModel>) {
//        taskList = newTasks
//        notifyDataSetChanged()
//    }
//}