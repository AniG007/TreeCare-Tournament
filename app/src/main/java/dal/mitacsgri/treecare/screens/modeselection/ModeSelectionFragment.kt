package dal.mitacsgri.treecare.screens.modeselection

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.consts.CHALLENGER_MODE
import dal.mitacsgri.treecare.consts.STARTER_MODE
import dal.mitacsgri.treecare.consts.TOURNAMENT_MODE
import dal.mitacsgri.treecare.extensions.startNextActivity
import dal.mitacsgri.treecare.screens.MainViewModel
import dal.mitacsgri.treecare.screens.treecareunityactivity.TreeCareUnityActivity
import kotlinx.android.synthetic.main.fragment_mode_selection.view.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ModeSelectionFragment : Fragment() {

    private val mainViewModel: MainViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mode_selection, container, false)

        view.apply {
            changeBackgroundSolidAndStrokeColor(starterModeButton, "FF0189F1", "FF0000FF")
            changeBackgroundSolidAndStrokeColor(challengerModeButton, "FFFF6F00", "FFBF360C")
            changeBackgroundSolidAndStrokeColor(tournamentModeButton, "FF9C27B0", "1A237E")

            starterModeButton.setOnClickListener {
                mainViewModel.setGameMode(STARTER_MODE)
                startInstructionOrUnityActivity(STARTER_MODE,
                    mainViewModel.hasInstructionsDisplayed(STARTER_MODE))
            }

            challengerModeButton.setOnClickListener {
                mainViewModel.setGameMode(CHALLENGER_MODE)
                if (mainViewModel.hasInstructionsDisplayed(CHALLENGER_MODE))
                    findNavController().navigate(R.id.action_modeSelectionFragment_to_challengesFragment)
                else {
                    mainViewModel.setInstructionsDisplayed(CHALLENGER_MODE, true)
                    val action = ModeSelectionFragmentDirections
                        .actionModeSelectionFragmentToInstructionsFragment(CHALLENGER_MODE)
                    findNavController().navigate(action)
                }
            }

            tournamentModeButton.setOnClickListener {
                mainViewModel.setGameMode(TOURNAMENT_MODE)
                if (mainViewModel.hasInstructionsDisplayed(TOURNAMENT_MODE))
                    findNavController().navigate(R.id.action_modeSelectionFragment_to_tournamentModeFragment)
                else {
                    mainViewModel.setInstructionsDisplayed(TOURNAMENT_MODE, true)
                    val action = ModeSelectionFragmentDirections
                        .actionModeSelectionFragmentToInstructionsFragment(TOURNAMENT_MODE)
                    findNavController().navigate(action)
                }
            }
        }

        return view
    }

    private fun changeBackgroundSolidAndStrokeColor(
        button: MaterialButton, solidColor: String, strokeColor: String) {
        button.setBackgroundColor(Color.parseColor("#$solidColor"))
    }

    private fun startInstructionOrUnityActivity(mode: Int, hasInstructionsDisplayed: Boolean) {
        if (hasInstructionsDisplayed)
            activity?.startNextActivity(TreeCareUnityActivity::class.java)
        else {
            val action = ModeSelectionFragmentDirections
                .actionModeSelectionFragmentToInstructionsFragment(mode)
            findNavController().navigate(action)
        }
    }

}
