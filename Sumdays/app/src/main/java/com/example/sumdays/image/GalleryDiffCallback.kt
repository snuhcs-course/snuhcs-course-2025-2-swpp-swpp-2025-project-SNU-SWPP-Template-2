package com.example.sumdays.image

import androidx.recyclerview.widget.DiffUtil

class GalleryDiffCallback : DiffUtil.ItemCallback<GalleryItem>() {

    override fun areItemsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
        return (oldItem is GalleryItem.Photo && newItem is GalleryItem.Photo && oldItem.url == newItem.url) ||
                (oldItem is GalleryItem.Add && newItem is GalleryItem.Add)
    }

    override fun areContentsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
        return oldItem == newItem
    }
}
