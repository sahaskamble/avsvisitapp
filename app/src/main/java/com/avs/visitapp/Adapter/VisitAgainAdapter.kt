package com.avs.visitapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.avs.visitapp.R
import com.avs.visitapp.VisitAgainModel

class VisitAgainAdapter(
    private var visits: List<VisitAgainModel>,
    private val onVisitClicked: (VisitAgainModel) -> Unit
) : RecyclerView.Adapter<VisitAgainAdapter.VisitAgainViewHolder>() {

    inner class VisitAgainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvMobile: TextView = itemView.findViewById(R.id.tvMobile)
        val tvBranch: TextView = itemView.findViewById(R.id.tvBranchName)
        val tvOD: TextView = itemView.findViewById(R.id.tvoverdue)
        val tvLoanAcc: TextView = itemView.findViewById(R.id.tvLoanAc)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvRemark: TextView = itemView.findViewById(R.id.tvRemark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitAgainViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer, parent, false)
        return VisitAgainViewHolder(view)
    }

    override fun onBindViewHolder(holder: VisitAgainViewHolder, position: Int) {
        val visit = visits[position]

        holder.tvName.text = visit.CustomerName ?: ""
        holder.tvMobile.text = visit.CustMobileNo ?: ""
        holder.tvBranch.text = visit.BranchName ?: ""
        holder.tvOD.text = visit.ODAmt ?: ""
        holder.tvLoanAcc.text = visit.LoanAccNo ?: ""

        // Show Comments first, if empty then OtherRemarks
        holder.tvRemark.text = visit.Comments ?: visit.OtherRemarks ?: ""

        when (visit.Status) {
            "Pending" -> {
                holder.tvStatus.text = "Pending"
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.red))
            }
            "VisitAgain" -> {
                holder.tvStatus.text = "Visit Again"
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.orange))
            }
            "Done" -> {
                holder.tvStatus.text = "Done"
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.color_green))
            }
            else -> {
                holder.tvStatus.text = visit.Status ?: ""
            }
        }

        // Entire card click
        holder.itemView.setOnClickListener { onVisitClicked(visit) }
    }

    override fun getItemCount(): Int = visits.size

    fun updateList(newList: List<VisitAgainModel>) {
        visits = newList
        notifyDataSetChanged()
    }
}
