package com.example.mindlog.core.common

import java.time.LocalDate
import java.time.ZoneId

object Time {
    val KST: ZoneId = ZoneId.of("Asia/Seoul")
    fun todayKST(): LocalDate = LocalDate.now(KST)
}