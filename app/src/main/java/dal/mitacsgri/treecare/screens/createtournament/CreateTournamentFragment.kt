package dal.mitacsgri.treecare.screens.createtournament

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.consts.TOURNAMENT_TYPE_DAILY_GOAL_BASED
import dal.mitacsgri.treecare.extensions.createFragmentViewWithStyle
import dal.mitacsgri.treecare.extensions.disable
import dal.mitacsgri.treecare.extensions.validate
import kotlinx.android.synthetic.main.fragment_create_tournament.view.*
import kotlinx.android.synthetic.main.fragment_create_tournament.view.toolbar
import org.joda.time.DateTime
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Created by Anirudh on 23-04-2020
 */
class CreateTournamentFragment : Fragment() {

    //private val mViewModel: CreateTournamentViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val viewModel: CreateTournamentViewModel by viewModel()
        val view = inflater.createFragmentViewWithStyle(activity,R.layout.fragment_create_tournament,R.style.AppTheme)
        view.apply {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            createTournamentButton.disable()

            viewModel.messageLiveData.observe(this@CreateTournamentFragment, Observer {
                it.toast(context)
            })

            viewModel.isFullDataValid.observe(this@CreateTournamentFragment, Observer {
                createTournamentButton.isEnabled = it
            })

//            inputTournamentStartDate.apply {
//                inputType = InputType.TYPE_NULL
//                setOnClickListener {
//                    val (day, month, year) = viewModel.getCurrentDateDestructured()
//                    val datePickerDialog = DatePickerDialog(context,
//                        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
//                            setText(viewModel.getStartDateText(year, monthOfYear, dayOfMonth))
//                        }, year, month, day
//                    )
//                    datePickerDialog.datePicker.minDate = DateTime().millis
//                    datePickerDialog.show()
//                }
//            }

            inputTournamentEndDate.apply {
                inputType = InputType.TYPE_NULL
                setOnClickListener {
                    val (day, month, year) = viewModel.getCurrentDateDestructured()
                    val datePickerDialog = DatePickerDialog(context,
                        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                            setText(viewModel.getDateText(year, monthOfYear, dayOfMonth))
                        }, year, month, day
                    )
                    datePickerDialog.datePicker.minDate = DateTime().plusDays(1).millis
                    datePickerDialog.show()
                }
            }
                inputTournamentName.validate("Please enter tournament name (Should not contain ':')") {
                    viewModel.isNameValid = it.isNotEmpty() and !it.contains(':')
                    viewModel.areAllInputFieldsValid()
                    viewModel.isNameValid
                }
                inputTournamentGoal.validate("Goal should be a multiple of 1000 greater than 9000"){
                    viewModel.isGoalValid = it.matches(viewModel.getRegexToMatchStepsGoal())
                    viewModel.areAllInputFieldsValid()
                    viewModel.isGoalValid
                }

                inputTournamentEndDate.validate("Please provide a Tournament Start date") {
                    viewModel.isStartDateValid = it.isNotEmpty()
                    viewModel.areAllInputFieldsValid()
                    viewModel.isStartDateValid
                }

                inputTournamentEndDate.validate("Please provide a Tournament end date") {
                    viewModel.isEndDateValid = it.isNotEmpty()
                    viewModel.areAllInputFieldsValid()
                    viewModel.isEndDateValid
                }

                createTournamentButton.setOnClickListener {
                    viewModel.createTournament(name = inputTournamentName.text,
                        description = inputTournamentDescription.text,
                        type = TOURNAMENT_TYPE_DAILY_GOAL_BASED,
                        goal = inputTournamentGoal.text) {
                        findNavController().navigateUp()
                    }
                }
        }
    return view
}
}
