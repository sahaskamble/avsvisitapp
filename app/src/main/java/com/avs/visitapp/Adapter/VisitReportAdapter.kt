package com.avs.visitapp.Adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.avs.visitapp.R
import com.avs.visitapp.VisitData
import com.avs.visitapp.databinding.VisitItemRecordBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*
import android.util.Base64

class VisitReportAdapter(
    private val visits: List<VisitData>,
    private val onItemClicked: (VisitData) -> Unit
) : RecyclerView.Adapter<VisitReportAdapter.VisitViewHolder>() {

    inner class VisitViewHolder(val binding: VisitItemRecordBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitViewHolder {
        val binding = VisitItemRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VisitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VisitViewHolder, position: Int) {
        val item = visits[position]

        holder.binding.apply {
            // Existing data
            tvName.text = "👤 ${item.Applicant?.Name ?: "N/A"}"

            val rawDate = item.Visit?.VisitDate ?: item.Applicant?.ApplicationDate ?: ""
            val formattedDate = try {
                val inputFormats = listOf(
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                )
                val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val parsed = inputFormats.firstNotNullOfOrNull {
                    try {
                        it.parse(rawDate)
                    } catch (_: Exception) {
                        null
                    }
                }
                outputFormat.format(parsed ?: Date())
            } catch (e: Exception) {
                rawDate
            }
            tvDate.text = formattedDate

            tvDocNo.text = item.Applicant?.ApplicationSerialNo ?: "N/A"

            // Show lat & long from VisitResponses if available (use first one)
            val firstVisitResponse = item.VisitResponses?.firstOrNull()
            if (firstVisitResponse != null) {
                tvLatitude.text = "${firstVisitResponse.location.latitude}"
                tvLongitude.text = "${firstVisitResponse.location.longitude}"
            } else {
                tvLatitude.text = "N/A"
                tvLongitude.text = "N/A"
            }

            // Load photo from VisitResponses photo_url (base64 or URL)
            val photoString = firstVisitResponse?.photo_url
            if (!photoString.isNullOrEmpty()) {
                // Check if base64 or URL (simple heuristic)
                if (photoString.startsWith("http") || photoString.startsWith("https")) {
                    // Load from URL using Glide or Picasso
                    Glide.with(imgVisitPhoto.context)
                        .load(photoString)
                        .placeholder(R.drawable.user_profile)
                        .into(imgVisitPhoto)
                } else {
                    // Assume base64 string, decode and set bitmap
                    val decodedBytes = Base64.decode(photoString, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    imgVisitPhoto.setImageBitmap(bmp)
                }
            } else {
                imgVisitPhoto.setImageResource(R.drawable.user_profile)
            }

            root.setOnClickListener {
                onItemClicked(item)
            }
        }
    }


    override fun getItemCount(): Int = visits.size
}
