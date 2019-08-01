package dal.mitacsgri.treecare.screens.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.makeGrayscale
import kotlinx.android.synthetic.main.profile_fragment.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileFragment : Fragment() {

    private val mViewModel: ProfileViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.profile_fragment, container, false)

        view.apply {
            Glide.with(this)
                .load(mViewModel.getUserPhotoUrl())
                .apply(RequestOptions.circleCropTransform())
                .into(profileImageView)

            nameTV.text = mViewModel.getUserFullName()

            buttonProgressReport.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_progressReportFragment)
            }

            grayscaleStreakIcons(this, mViewModel.getDailyGoalCompletionStreakCount())
            streakDescriptionTV.text = mViewModel.getDailyGoalStreakText()

            mViewModel.trophiesCountData.observe(this@ProfileFragment, Observer {
                goldAwardCountTV.text = it.first
                silverAwardCountTV.text = it.second
                bronzeAwardCount.text = it.third
            })

            challengerModeButton.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_challengesFragment)
            }
        }

        return view
    }

    private fun grayscaleStreakIcons(view: View, streakCount: Int) {
        view.apply {
            val streakAppleImages = arrayOf(streakApple1, streakApple2, streakApple3,
                streakApple4, streakApple5, streakApple6, streakApple7)

            for (i in streakCount until streakAppleImages.size) {
                streakAppleImages[i].makeGrayscale()
            }
        }
    }

}
