package com.example.librarytogether.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.librarytogether.feature.home.HomeViewModel
import com.example.librarytogether.feature.home.SortType
import com.example.librarytogether.feature.home.data.HomeRepository
import com.example.librarytogether.testing.MainDispatcherRule
import com.example.librarytogether.home.PostFixtures
import com.example.librarytogether.testing.getOrAwaitValue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class HomeViewModelTest {

    @get:Rule
    val instant = InstantTaskExecutorRule()
    @get:Rule
    val main = MainDispatcherRule()

    @Mock
    lateinit var repo: HomeRepository

    private fun vm() = HomeViewModel(repo)

    @Test
    fun loadFeed_success_populatesPosts_andStopsLoading() = runTest {
        // Given
        val p1 = PostFixtures.post(id = 1, likeCount = 1)
        val p2 = PostFixtures.post(id = 2, likeCount = 10)
        Mockito.`when`(repo.getFeed()).thenReturn(listOf(p2, p1))

        // When
        val vm = vm()
        advanceUntilIdle()

        // Then
        val list = vm.posts.getOrAwaitValue()
        // createdAt
        MatcherAssert.assertThat(list.first().id, CoreMatchers.`is`(p1.id))
        MatcherAssert.assertThat(vm.isLoading.getOrAwaitValue(), CoreMatchers.`is`(false))
        Mockito.verify(repo).getFeed()
    }

    @Test
    fun loadFeed_failure_setsError_andStopsLoading() = runTest {
        Mockito.`when`(repo.getFeed()).thenThrow(RuntimeException("boom"))

        val vm = vm()
        advanceUntilIdle()

        MatcherAssert.assertThat(vm.error.getOrAwaitValue(), CoreMatchers.`is`("네트워크 오류가 발생했습니다."))
        MatcherAssert.assertThat(vm.isLoading.getOrAwaitValue(), CoreMatchers.`is`(false))
    }

    @Test
    fun applySort_popular_sortsByLikeCountDescending() = runTest {
        val p1 = PostFixtures.post(id = 1, likeCount = 1)
        val p2 = PostFixtures.post(id = 2, likeCount = 10)
        Mockito.`when`(repo.getFeed()).thenReturn(listOf(p1, p2))

        val vm = vm()
        advanceUntilIdle()
        vm.applySort(SortType.POPULAR)

        val list = vm.posts.getOrAwaitValue()
        MatcherAssert.assertThat(list.first().id, CoreMatchers.`is`(p2.id))
    }

    @Test
    fun toggleLike_success_updatesOnlyTargetPost() = runTest {
        val p1 = PostFixtures.post(id = 1, liked = false, likeCount = 0)
        val p2 = PostFixtures.post(id = 2, liked = false, likeCount = 5)
        Mockito.`when`(repo.getFeed()).thenReturn(listOf(p1, p2))

        val vm = vm()
        advanceUntilIdle()

        // repo가 토글 후의 값을 반환하도록 스텁
        val updated = p2.copy(isLiked = true, likeCount = p2.likeCount + 1)
        Mockito.`when`(repo.toggleLike(2)).thenReturn(updated)

        vm.toggleLike(p2)
        advanceUntilIdle()

        val list = vm.posts.getOrAwaitValue()
        // p1은 그대로, p2만 갱신
        val after1 = list.first { it.id == 1 }
        val after2 = list.first { it.id == 2 }
        MatcherAssert.assertThat(after1.isLiked, CoreMatchers.`is`(p1.isLiked))
        MatcherAssert.assertThat(after1.likeCount, CoreMatchers.`is`(p1.likeCount))
        MatcherAssert.assertThat(after2.isLiked, CoreMatchers.`is`(true))
        MatcherAssert.assertThat(after2.likeCount, CoreMatchers.`is`(p2.likeCount + 1))
        Mockito.verify(repo).toggleLike(2)
    }

    @Test
    fun toggleLike_failure_setsError_andKeepsPostsUnchanged() = runTest {
        val p1 = PostFixtures.post(id = 1, liked = false)
        Mockito.`when`(repo.getFeed()).thenReturn(listOf(p1))

        val vm = vm()
        advanceUntilIdle()

        Mockito.`when`(repo.toggleLike(1)).thenThrow(RuntimeException("boom"))
        vm.toggleLike(p1)
        advanceUntilIdle()

        val list = vm.posts.getOrAwaitValue()
        val after = list.first { it.id == 1 }
        MatcherAssert.assertThat(after.isLiked, CoreMatchers.`is`(false))
        MatcherAssert.assertThat(
            vm.error.getOrAwaitValue(),
            CoreMatchers.`is`("좋아요를 토글하는 데 실패했습니다.")
        )
    }
}
