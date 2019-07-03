package dal.mitacsgri.treecare.backgroundtasks.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestWorkerBuilder
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class UpdateUserChallengeDataWorkerTest {

    private lateinit var context: Context
    private lateinit var executor: Executor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
    }

    @Test
    fun testUpdateUserChallengeDataWorker() {
        val worker = TestWorkerBuilder<UpdateUserChallengeDataWorker>(
            context, executor
        ).build()

        val result = worker.doWork()
        assertThat(result, `is`(Result.success()))

        worker
    }
}