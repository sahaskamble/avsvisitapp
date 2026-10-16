package com.avs.visitapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.avs.visitapp.R
import com.avs.visitapp.Visit
class CustomerAdapter(
    private var visits: List<Visit>,
    private val onVisitClicked: (Visit) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    private var currentTab: String = "Pending"

    fun setCurrentTab(tab: String) {
        currentTab = tab
        notifyDataSetChanged()
    }

    class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvMobile: TextView = itemView.findViewById(R.id.tvMobile)
        val tvBranch: TextView = itemView.findViewById(R.id.tvBranchName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val ODAmt: TextView = itemView.findViewById(R.id.tvoverdue)
        val LoanAccNo: TextView = itemView.findViewById(R.id.tvLoanAc)
        val Remark: TextView = itemView.findViewById(R.id.tvRemark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val visit = visits[position]

        holder.tvName.text = visit.CustomerName
        holder.tvMobile.text = visit.CustMobileNo ?: ""
        holder.tvBranch.text = visit.BranchName
        holder.ODAmt.text = visit.ODAmt ?: ""
        holder.LoanAccNo.text = visit.LoanAccNo ?: ""
        holder.Remark.text = visit.CallRemarks ?: ""

        // ---- FIXED STATUS HANDLING ----
        when (currentTab) {
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
        }

        holder.tvMobile.setOnClickListener {
            if (currentTab == "Pending") {
                if (!visit.CustMobileNo.isNullOrEmpty()) {
                    onVisitClicked(visit)
                }
            }
        }
    }

    override fun getItemCount(): Int = visits.size

    fun updateList(newList: List<Visit>) {
        visits = newList
        notifyDataSetChanged()
    }
}
