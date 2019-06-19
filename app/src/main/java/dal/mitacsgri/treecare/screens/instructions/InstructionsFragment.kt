package dal.mitacsgri.treecare.screens.instructions


import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.startNextActivity
import dal.mitacsgri.treecare.screens.MainViewModel
import dal.mitacsgri.treecare.unity.UnityPlayerActivity
import kotlinx.android.synthetic.main.fragment_instructions.view.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class InstructionsFragment : Fragment() {

    private val mainViewModel: MainViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_instructions, container, false)

        mainViewModel.setInstructionsDisplayedStatus(true)
        view.apply {
            changeBackgroundSolidAndStrokeColor(scrollView, "ccffffff", "ff646464")
            changeBackgroundSolidAndStrokeColor(continueButton, "FF0189F1", "FF0000FF")
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        view?.continueButton?.setOnClickListener {
            activity?.startNextActivity(UnityPlayerActivity::class.java)
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
