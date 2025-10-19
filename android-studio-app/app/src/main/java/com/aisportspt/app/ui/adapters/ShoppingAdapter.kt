package com.aisportspt.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aisportspt.app.R
import com.aisportspt.app.model.ShoppingItem

class ShoppingAdapter(private val items: List<ShoppingItem>) :
    RecyclerView.Adapter<ShoppingAdapter.ShoppingViewHolder>() {

    inner class ShoppingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView = itemView.findViewById(R.id.img_product)
        val tvName: TextView = itemView.findViewById(R.id.tv_product_name)
        val tvLevel: TextView = itemView.findViewById(R.id.tv_product_level)
        val tvPrice: TextView = itemView.findViewById(R.id.tv_product_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_card, parent, false)
        return ShoppingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingViewHolder, position: Int) {
        val item = items[position]
        holder.imgProduct.setImageResource(item.imageRes)
        holder.tvName.text = item.name
        holder.tvLevel.text = "★".repeat(item.level) + "☆".repeat(5 - item.level) + " (${item.level}/5)"
        holder.tvPrice.text = item.price
    }

    override fun getItemCount(): Int = items.size
}
