package dal.mitacsgri.treecare.screens.createchallenge


import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.consts.CHALLENGE_TYPE_DAILY_GOAL_BASED
import dal.mitacsgri.treecare.extensions.*
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
            activity, R.layout.fragment_create_challenge, R.style.AppTheme)

        view.apply {

            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            createChallengeButton.disable()

            viewModel.messageLiveData.observe(this@CreateChallengeFragment, Observer {
                it.toast(context)
            })

            viewModel.isFullDataValid.observe(this@CreateChallengeFragment, Observer {
                createChallengeButton.enable()
            })

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

            inputTeamName.validate("Please enter name (Should not contain ':')") {
                viewModel.isNameValid = it.isNotEmpty() and !it.contains(':')
                viewModel.areAllInputFieldsValid()
                viewModel.isNameValid
            }
            inputChallengeGoal.validate("Goal should be a multiple of 1000 greater than 4000") {
                viewModel.isGoalValid = it.matches(viewModel.getRegexToMatchStepsGoal())
                viewModel.areAllInputFieldsValid()
                viewModel.isGoalValid
            }
            inputChallengeEndDate.validate("Please provide a chalenge end date") {
                viewModel.isEndDateValid = it.isNotEmpty()
                viewModel.areAllInputFieldsValid()
                viewModel.isEndDateValid
            }

            createChallengeButton.setOnClickListener {
                viewModel.createChallenge(name = inputTeamName.text,
                    description = inputTeamDescription.text,
                    type = CHALLENGE_TYPE_DAILY_GOAL_BASED,
                    goal = inputChallengeGoal.text)
            }
        }

        return view
    }


}
