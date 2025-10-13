package com.example.librarytogether.feature.home

import androidx.recyclerview.widget.DiffUtil
import com.example.librarytogether.feature.home.data.Post

object PostDiff : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean =
        oldItem == newItem
}
