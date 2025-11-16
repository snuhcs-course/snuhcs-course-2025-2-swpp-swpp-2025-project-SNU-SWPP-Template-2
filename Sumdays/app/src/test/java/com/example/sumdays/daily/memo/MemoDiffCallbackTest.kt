package com.example.sumdays.daily.memo

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MemoDiffCallbackTest {

    private lateinit var diffCallback: MemoDiffCallback

    @Before
    fun setup() {
        diffCallback = MemoDiffCallback()
    }

    // areItemsTheSame 테스트 (주로 고유 ID를 비교)
    @Test
    fun areItemsTheSame_returnsTrueForSameId() {
        val oldItem = Memo(id = 10, content = "Old Content", timestamp = "T1", date = "D1", order = 1)
        // 내용이 달라도 ID가 같으면 true여야 합니다.
        val newItem = Memo(id = 10, content = "New Content", timestamp = "T2", date = "D2", order = 2)

        val result = diffCallback.areItemsTheSame(oldItem, newItem)
        assertEquals(true, result)
    }

    @Test
    fun areItemsTheSame_returnsFalseForDifferentId() {
        val oldItem = Memo(id = 10, content = "Content", timestamp = "T1", date = "D1", order = 1)
        val newItem = Memo(id = 11, content = "Content", timestamp = "T1", date = "D1", order = 1)

        val result = diffCallback.areItemsTheSame(oldItem, newItem)
        assertEquals(false, result)
    }

    // areContentsTheSame 테스트 (아이템이 같을 때 내부 내용이 변경되었는지 비교)
    @Test
    fun areContentsTheSame_returnsTrueForSameContent() {
        // data class의 기본 '==' 연산자는 모든 속성을 비교합니다.
        val oldItem = Memo(id = 5, content = "Same Content", timestamp = "T1", date = "D1", order = 1)
        val newItem = Memo(id = 5, content = "Same Content", timestamp = "T1", date = "D1", order = 1)

        val result = diffCallback.areContentsTheSame(oldItem, newItem)
        assertEquals(true, result)
    }

    @Test
    fun areContentsTheSame_returnsFalseForDifferentContent() {
        val oldItem = Memo(id = 5, content = "Old Content", timestamp = "T1", date = "D1", order = 1)
        // 내용(content)이 변경됨
        val newItem = Memo(id = 5, content = "Updated Content", timestamp = "T1", date = "D1", order = 1)

        val result = diffCallback.areContentsTheSame(oldItem, newItem)
        assertEquals(false, result)
    }

    @Test
    fun areContentsTheSame_returnsFalseForDifferentOrder() {
        val oldItem = Memo(id = 5, content = "Content", timestamp = "T1", date = "D1", order = 1)
        // 순서(order)가 변경됨
        val newItem = Memo(id = 5, content = "Content", timestamp = "T1", date = "D1", order = 2)

        val result = diffCallback.areContentsTheSame(oldItem, newItem)
        assertEquals(false, result)
    }
}