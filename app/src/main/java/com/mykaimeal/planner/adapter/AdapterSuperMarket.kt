package com.mykaimeal.planner.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.mykaimeal.planner.OnItemClickListener
import com.mykaimeal.planner.OnItemSelectListener
import com.mykaimeal.planner.OnItemSelectUnSelectListener
import com.mykaimeal.planner.R
import com.mykaimeal.planner.databinding.AdapterSuperMarketItemBinding
import com.mykaimeal.planner.fragment.mainfragment.commonscreen.basketscreen.model.Store
import java.math.BigDecimal
import java.math.RoundingMode

class AdapterSuperMarket(
    private var storesData: MutableList<Store>?,
    private var requireActivity: FragmentActivity,
    private var onItemSelectListener: OnItemSelectUnSelectListener,
) : RecyclerView.Adapter<AdapterSuperMarket.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: AdapterSuperMarketItemBinding =
            AdapterSuperMarketItemBinding.inflate(inflater, parent, false);
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {

        val data = storesData?.get(position)

        if (data?.is_slected != null) {
            if (data.is_slected == 1) {
                holder.binding.cardViewMainLayout.strokeColor = Color.parseColor("#06C169")
            } else {
                holder.binding.cardViewMainLayout.strokeColor = Color.parseColor("#FFFFFF")
            }
        }

        data?.let {
            if (it.missing != "0") {
                if (it.missing != null) {
                    holder.binding.tvSuperMarketItems.setTextColor(android.graphics.Color.parseColor("#FF3232"))
                    holder.binding.tvSuperMarketItems.text = it.missing.toString() + " ITEMS MISSING"
                }
            } else {
                holder.binding.tvSuperMarketItems.setTextColor(android.graphics.Color.parseColor("#06C169"))
                holder.binding.tvSuperMarketItems.text = "ALL ITEMS"
            }

            if (it.total != null) {
                val totalValue = it.total
                val formattedTotal = if (totalValue % 1 == 0.0) {
                    totalValue.toInt().toString() // Show without decimal
                } else {
                    String.format("%.2f", totalValue) // Show two decimals
                }
                holder.binding.tvSuperMarketRupees.text = "$$formattedTotal"
            }


            it.distance?.let{ distance->
                holder.binding.tvMiles.text = "$distance miles"
            }

            it.image?.let {
                Glide.with(requireActivity)
                    .load(it)
                    .error(R.drawable.no_image)
                    .placeholder(R.drawable.no_image)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            holder.binding.layProgess.root.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            holder.binding.layProgess.root.visibility = View.GONE
                            return false
                        }
                    })
                    .into(holder.binding.imageSuperMarket)
            }?:run {
                holder.binding.layProgess.root.visibility = View.GONE
            }
        }


        holder.binding.relativeLayoutMain.setOnClickListener {
            updateSelection(position)
            onItemSelectListener.itemSelectUnSelect(position, storesData!![position].store_uuid.toString(), "SuperMarket", position)
        }
    }

    private fun updateSelection(selectedPosition: Int) {
        storesData?.forEachIndexed { index, stores ->
            stores.is_slected = if (index == selectedPosition) 1 else 0
        }
        notifyDataSetChanged()
    }

    fun updateList(list: MutableList<Store>){
        storesData = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return storesData!!.size
    }

    class ViewHolder(var binding: AdapterSuperMarketItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }
}