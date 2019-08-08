package dal.mitacsgri.treecare.screens.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.makeGrayscale
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.screens.MainActivity
import kotlinx.android.synthetic.main.fragment_profile.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileFragment : Fragment() {

    private val mViewModel: ProfileViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        view.apply {
            Glide.with(this)
                .load(mViewModel.getUserPhotoUrl())
                .apply(RequestOptions.circleCropTransform())
                .into(profileImageView)

            nameTV.text = mViewModel.getUserFullName()

            backButton.setOnClickListener {
                findNavController().navigateUp()
            }

            logOutButton.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle(context.getString(R.string.logout_title))
                    .setMessage(context.getString(R.string.logout_message))
                    .setPositiveButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNegativeButton("Yes") { _, _ ->
                        mViewModel.logout(context)
                        activity?.finish()
                        startActivity(Intent(activity, MainActivity::class.java))
                        "Logged out".toast(context)
                    }
                    .show()
            }

            buttonProgressReport.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_progressReportFragment)
            }

            grayscaleStreakIcons(this, mViewModel.getGoalCompletionStreakData(),
                mViewModel.getCurrentWeekDayForStreak())

            streakDescriptionTV.text = mViewModel.getDailyGoalStreakText()

            mViewModel.trophiesCountData.observe(this@ProfileFragment, Observer {
                goldAwardCountTV.text = it.first.toString()
                silverAwardCountTV.text = it.second.toString()
                bronzeAwardCount.text = it.third.toString()

                if (it.first == 0) awardFirstImage.makeGrayscale()
                if (it.second == 0) awardSecondImage.makeGrayscale()
                if (it.third == 0) awardThirdImage.makeGrayscale()
            })

            challengerModeButtonHolder.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_challengesFragment)
            }

            mViewModel.getTrophiesCount()
        }

        return view
    }

    private fun grayscaleStreakIcons(view: View, streakArray: Array<Boolean>, currentDayOfWeek: Int) {
        view.apply {
            val streakAppleImages = arrayOf(streakApple1, streakApple2, streakApple3,
                streakApple4, streakApple5, streakApple6, streakApple7)

            for(i in 0 until streakArray.size) {
                if (!streakArray[i]) {
                    streakAppleImages[i].makeGrayscale()
                }
            }

            for (i in (currentDayOfWeek+1)..6) {
                streakAppleImages[i].visibility = View.INVISIBLE
            }
        }
    }

}
