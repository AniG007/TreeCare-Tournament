package dal.mitacsgri.treecare.screens.modeselection

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.startNextActivity
import dal.mitacsgri.treecare.screens.MainViewModel
import dal.mitacsgri.treecare.unity.UnityPlayerActivity
import kotlinx.android.synthetic.main.fragment_mode_selection.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ModeSelectionFragment : Fragment() {

    private val mainViewModel: MainViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mode_selection, container, false)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        view.apply {
            changeBackgroundSolidAndStrokeColor(starterModeButton, "FF0189F1", "FF0000FF")
            changeBackgroundSolidAndStrokeColor(challengerModeButton, "FFFF6F00", "FFBF360C")
            changeBackgroundSolidAndStrokeColor(tournamentModeButton, "FF9C27B0", "1A237E")

            starterModeButton.setOnClickListener {
                startInstructionOrUnityActivity()
            }
        }
    }

    private fun changeBackgroundSolidAndStrokeColor(
        button: Button, solidColor: String, strokeColor: String) {

        val background = button.background as GradientDrawable
        background.setColor(Color.parseColor("#$solidColor"))
        background.setStroke(
            resources.getDimension(com.intuit.sdp.R.dimen._4sdp).toInt(),
            Color.parseColor("#$strokeColor"))
    }

    private fun startInstructionOrUnityActivity() {
        if (mainViewModel.haveInstructionsDisplayed.value!!)
            activity?.startNextActivity(UnityPlayerActivity::class.java)
        else
            findNavController().navigate(R.id.action_modeSelectionFragment_to_instructionsFragment)
    }

}
