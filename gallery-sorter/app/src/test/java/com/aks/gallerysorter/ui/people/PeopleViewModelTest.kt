package com.aks.gallerysorter.ui.people

import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.FaceRow
import com.aks.gallerysorter.index.entities.LabelRow
import com.aks.gallerysorter.index.entities.PersonRow
import com.aks.gallerysorter.lock.LockState
import com.aks.gallerysorter.ui.TestIndex
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PeopleViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: AppDatabase
    private val lockState = LockState()
    private val recategorized = mutableListOf<Long>()
    private var a = 0L
    private var b = 0L

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = TestIndex.db()
        runBlocking {
            db.mediaDao().upsertAll(listOf(TestIndex.media(1), TestIndex.media(2), TestIndex.media(3)))
            // Item 2 is Sensitive: person B appears only there.
            db.labelDao().upsertAll(listOf(LabelRow(2, Category.MY_NUDES, 0.95f)))
            a = db.peopleDao().insertPerson(PersonRow(name = "A", isMe = true))
            b = db.peopleDao().insertPerson(PersonRow(name = "B"))
            db.peopleDao().insertFaces(
                listOf(
                    FaceRow(mediaId = 1, personId = a, embedding = ByteArray(4)),
                    FaceRow(mediaId = 3, personId = a, embedding = ByteArray(4)),
                    FaceRow(mediaId = 2, personId = b, embedding = ByteArray(4)),
                ),
            )
        }
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun vm() = PeopleViewModel(db = { db }, lockState = lockState, recategorize = { recategorized += it }, notify = {})

    @Test fun locked_listsOnlyPeopleWithVisibleItems() = runBlocking<Unit> {
        val locked = withTimeout(10_000) { vm().people.filterNotNull().first() }
        assertThat(locked.map { it.id }).containsExactly(a)
        assertThat(locked.single().n).isEqualTo(2)

        TestIndex.unlock(lockState, tmp.newFolder("keys"))
        val open = withTimeout(10_000) { vm().people.filterNotNull().first() }
        assertThat(open.map { it.id }).containsExactly(a, b).inOrder()
        assertThat(open.first { it.id == b }.coverId).isEqualTo(2L)
    }

    /** A list built while unlocked must not be replayed after a lock (its covers can be Sensitive). */
    @Test fun replayAfterLock_neverShowsCovers() = runBlocking<Unit> {
        TestIndex.unlock(lockState, tmp.newFolder("keys"))
        val vm = vm()
        withTimeout(10_000) { vm.people.filterNotNull().first { list -> list.any { it.id == b } } }

        lockState.lockAll()
        val replayed = vm.people.first()

        assertThat(replayed.orEmpty().map { it.coverId }).doesNotContain(2L)
    }

    @Test fun markMe_movesMe_andRecategorizesBothPeoplesItems() = runBlocking<Unit> {
        vm().markMe(b).join()

        val people = db.peopleDao().people().first()
        assertThat(people.single { it.isMe }.id).isEqualTo(b)
        assertThat(recategorized.toSet()).containsExactly(1L, 2L, 3L)
    }

    @Test fun merge_movesFacesAndMe_inOneStep() = runBlocking<Unit> {
        vm().merge(setOf(a, b), into = b).join()

        val people = db.peopleDao().people().first()
        assertThat(people.map { it.id }).containsExactly(b)
        assertThat(people.single().isMe).isTrue()
        assertThat(db.peopleDao().allFaces().map { it.personId }.toSet()).containsExactly(b)
        assertThat(recategorized.toSet()).containsExactly(1L, 2L, 3L)
    }
}
