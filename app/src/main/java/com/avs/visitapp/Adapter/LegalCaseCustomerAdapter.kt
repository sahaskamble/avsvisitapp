package com.avs.visitapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.avs.visitapp.R
import com.avs.visitapp.Visit

class LegalCaseCustomerAdapter(private var visits: List<Visit>) :
    RecyclerView.Adapter<LegalCaseCustomerAdapter.CustomerViewHolder>() {

    class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvMobile: TextView = itemView.findViewById(R.id.tvMobile)
        val tvBranch: TextView = itemView.findViewById(R.id.tvBranchName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val LoanAccNo: TextView = itemView.findViewById(R.id.tvLoanAc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monitoring_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val visit = visits[position]

        // Bind data
        holder.tvName.text = visit.CustomerName
        holder.tvMobile.text = visit.CustMobileNo
        holder.tvBranch.text = visit.BranchName
        holder.tvStatus.text = visit.status
        holder.LoanAccNo.text = visit.LoanAccNo

        val colorRes = when (visit.status.trim().lowercase()) {
            "pending" -> R.color.red
            "done" -> R.color.color_green
            else -> R.color.black
        }
        holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, colorRes))
    }

    override fun getItemCount(): Int = visits.size

    fun updateList(newList: List<Visit>) {
        visits = newList
        notifyDataSetChanged()
    }
}
