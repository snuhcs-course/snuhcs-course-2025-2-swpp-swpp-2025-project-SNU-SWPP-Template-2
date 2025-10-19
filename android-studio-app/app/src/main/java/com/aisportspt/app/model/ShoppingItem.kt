package com.aisportspt.app.model

//data class ShoppingItem(
//    val name: String,
//    val price: String,
//    val imageRes: Int, // drawable 리소스 ID
//    val level: Int     // 숙련도 (1~5)
//)

data class ShoppingItem(
    val name: String,
    val price: String,
    val level: Int,
    val imageRes: Int // String 또는 Int로 실제 리소스 ID
)
