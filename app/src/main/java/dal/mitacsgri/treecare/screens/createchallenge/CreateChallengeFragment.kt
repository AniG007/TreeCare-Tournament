package dal.mitacsgri.treecare.screens.createchallenge


import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.createFragmentViewWithStyle
import kotlinx.android.synthetic.main.fragment_create_challenge.view.*
import org.joda.time.DateTime
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreateChallengeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val viewModel: CreateChallengeViewModel by viewModel()

        val view = inflater.createFragmentViewWithStyle(
            activity, R.layout.fragment_create_challenge, R.style.challenger_mode_theme)

        view.apply {
            inputChallengeEndDate.apply {
                inputType = InputType.TYPE_NULL
                setOnClickListener {
                    val (day, month, year) = viewModel.getCurrentDateDestructured()
                    val datePickerDialog = DatePickerDialog(context,
                        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                            setText(viewModel.getDateText(year, monthOfYear, dayOfMonth))
                        }, year, month, day)
                    datePickerDialog.datePicker.minDate = DateTime().plusDays(1).millis
                    datePickerDialog.show()
                }
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                challengeGoalLayout.hint = viewModel.getGoalInputHint(checkedId)
            }

            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        return view
    }


    private fun createLocalInflater(inflater: LayoutInflater)
            = inflater.cloneInContext(ContextThemeWrapper(activity, R.style.challenger_mode_theme))
}
