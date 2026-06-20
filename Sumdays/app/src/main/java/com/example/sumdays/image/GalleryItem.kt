package com.example.sumdays.image

sealed class GalleryItem {
    data class Photo(val url: String) : GalleryItem()
    object Add : GalleryItem()
}
