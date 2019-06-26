package dal.mitacsgri.treecare.screens.instructions


import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.consts.*
import dal.mitacsgri.treecare.extensions.startNextActivity
import dal.mitacsgri.treecare.screens.MainViewModel
import dal.mitacsgri.treecare.screens.treecareunityactivity.TreeCareUnityActivity
import kotlinx.android.synthetic.main.fragment_instructions.view.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class InstructionsFragment : Fragment() {

    private val mainViewModel: MainViewModel by sharedViewModel()
    private val args: InstructionsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_instructions, container, false)

        mainViewModel.setInstructionsDisplayed(args.mode, true)

        view.apply {
            changeBackgroundSolidAndStrokeColor(scrollView, "ccffffff", "ff646464")
            changeButtonColorAccordingToMode(continueButton, args.mode)

            continueButton.setOnClickListener {
                when(args.mode) {
                    STARTER_MODE -> {
                        instructionsTV.text = context.getString(R.string.starter_mode_instructions)
                        activity?.startNextActivity(TreeCareUnityActivity::class.java)
                    }
                    CHALLENGER_MODE -> {
                        instructionsTV.text = context.getString(R.string.starter_mode_instructions)
                        findNavController().navigate(R.id.action_instructionsFragment_to_challengesFragment)
                    }
                }
            }
        }

        return view
    }

    private fun changeButtonColorAccordingToMode(button: View, mode: Int) {
        when(mode) {
            STARTER_MODE -> {
                changeBackgroundSolidAndStrokeColor(button,
                    STARTER_MODE_SOLID_COLOR, STARTER_MODE_STROKE_COLOR)
            }
            CHALLENGER_MODE -> {
                changeBackgroundSolidAndStrokeColor(button,
                    CHALLENGER_MODE_SOLID_COLOR, CHALLENGER_MODE_STROKE_COLOR)
            }
            TOURNAMENT_MODE -> {
                changeBackgroundSolidAndStrokeColor(button,
                    TOURNAMENT_MODE_SOLID_COLOR, TOURNAMENT_MODE_STROKE_COLOR)
            }
        }
    }

    private fun changeBackgroundSolidAndStrokeColor(
        view: View, solidColor: String, strokeColor: String) {

        val background = view.background as GradientDrawable
        background.setColor(Color.parseColor("#$solidColor"))
        background.setStroke(
            resources.getDimension(com.intuit.sdp.R.dimen._4sdp).toInt(),
            Color.parseColor("#$strokeColor"))

    }
}
