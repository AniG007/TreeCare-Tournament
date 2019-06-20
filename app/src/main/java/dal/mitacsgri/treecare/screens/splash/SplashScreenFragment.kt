package dal.mitacsgri.treecare.screens.splash


import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dal.mitacsgri.treecare.R
import org.koin.androidx.viewmodel.ext.android.viewModel

class SplashScreenFragment : Fragment() {

    private val splashScreenViewModel: SplashScreenViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_splash_screen, container, false)

        splashScreenViewModel.apply {
            storeDailyStepsGoal(5000)

            //testGameByManipulatingSharedPrefsData(this)
            resetDailyGoalCheckedFlag()

            if (isLoginDone) setupFitApiToGetData(view.context)

            if (isLoginDone) navigateWithDelay(R.id.action_splashScreenFragment_to_modeSelectionFragment)
            else navigateWithDelay(R.id.action_splashScreenFragment_to_loginFragment)
        }

        return view;
    }

    private fun navigateWithDelay(actionResId: Int, delay: Long = 5000L) {
        Handler().postDelayed({
            findNavController().navigate(actionResId)
        }, delay)
    }
}
