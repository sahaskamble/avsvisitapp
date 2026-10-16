package com.avs.visitapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.avs.visitapp.R
import com.avs.visitapp.model.BankVisitData

class BankVisitAdapter(
    private var visits: List<BankVisitData>,
    private val onItemClicked: (BankVisitData) -> Unit
) : RecyclerView.Adapter<BankVisitAdapter.BankVisitViewHolder>() {

    private var currentTab: String = "Pending"

    fun updateList(newVisits: List<BankVisitData>) {
        visits = newVisits
        notifyDataSetChanged()
    }

    fun setCurrentTab(tabName: String) {
        currentTab = tabName
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankVisitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bank_visit, parent, false)
        return BankVisitViewHolder(view)
    }

    override fun onBindViewHolder(holder: BankVisitViewHolder, position: Int) {
        val visit = visits[position]

        holder.tvBankName.text = "Bank: ${visit.BankName ?: "N/A"}"
        holder.tvBranchName.text = "Branch: ${visit.BranchName ?: "N/A"}"

        val timeStr = if (currentTab == "Done") {
            "${visit.VisitDate?.substringBefore("T")} | In: ${visit.VisitInTime} | Out: ${visit.VisitOutTime}"
        } else {
            "${visit.VisitDate?.substringBefore("T")} | In: ${visit.VisitInTime}"
        }
        holder.tvDateAndTime.text = timeStr

        holder.tvAddress.text = "Address: ${visit.FullAddress ?: "N/A"}"
        holder.tvComments.text = "Comments: ${visit.Comments ?: "None"}"

        // If in Pending tab, whole card is clickable to open Update Form
        holder.itemView.setOnClickListener {
            if (currentTab == "Pending") {
                onItemClicked(visit)
            }
        }
    }

    override fun getItemCount(): Int = visits.size

    class BankVisitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBankName: TextView = itemView.findViewById(R.id.tvBankName)
        val tvBranchName: TextView = itemView.findViewById(R.id.tvBranchName)
        val tvDateAndTime: TextView = itemView.findViewById(R.id.tvDateAndTime)
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        val tvComments: TextView = itemView.findViewById(R.id.tvComments)
    }
}

//package com.avs.visitapp.Adapter
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.avs.visitapp.R
//import com.avs.visitapp.model.BankVisitData
//
//class BankVisitAdapter(
//    private var visits: List<BankVisitData>,
//    private val onMarkDoneClicked: (BankVisitData) -> Unit
//) : RecyclerView.Adapter<BankVisitAdapter.BankVisitViewHolder>() {
//
//    private var currentTab: String = "Pending"
//
//    fun updateList(newVisits: List<BankVisitData>) {
//        visits = newVisits
//        notifyDataSetChanged()
//    }
//
//    fun setCurrentTab(tabName: String) {
//        currentTab = tabName
//        notifyDataSetChanged()
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankVisitViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bank_visit, parent, false)
//        return BankVisitViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: BankVisitViewHolder, position: Int) {
//        val visit = visits[position]
//
//        holder.tvBankName.text = "Bank: ${visit.BankName ?: "N/A"}"
//        holder.tvBranchName.text = "Branch: ${visit.BranchName ?: "N/A"}"
//
//        // Show Date and In-Time. If completed, show Out-Time too.
//        val timeStr = if (currentTab == "Done") {
//            "${visit.VisitDate?.substringBefore("T")} | In: ${visit.VisitInTime} | Out: ${visit.VisitOutTime}"
//        } else {
//            "${visit.VisitDate?.substringBefore("T")} | In: ${visit.VisitInTime}"
//        }
//        holder.tvDateAndTime.text = timeStr
//
//        holder.tvAddress.text = "Address: ${visit.FullAddress ?: "N/A"}"
//        holder.tvComments.text = "Comments: ${visit.Comments ?: "None"}"
//
//        // Logic to show/hide the "Mark Done" button based on the active tab
//        if (currentTab == "Pending") {
//            holder.btnMarkDone.visibility = View.VISIBLE
//            holder.btnMarkDone.setOnClickListener {
//                onMarkDoneClicked(visit)
//            }
//        } else {
//            holder.btnMarkDone.visibility = View.GONE
//        }
//    }
//
//    override fun getItemCount(): Int = visits.size
//
//    class BankVisitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val tvBankName: TextView = itemView.findViewById(R.id.tvBankName)
//        val tvBranchName: TextView = itemView.findViewById(R.id.tvBranchName)
//        val tvDateAndTime: TextView = itemView.findViewById(R.id.tvDateAndTime)
//        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
//        val tvComments: TextView = itemView.findViewById(R.id.tvComments)
//        val btnMarkDone: Button = itemView.findViewById(R.id.btnMarkDone)
//    }
//}