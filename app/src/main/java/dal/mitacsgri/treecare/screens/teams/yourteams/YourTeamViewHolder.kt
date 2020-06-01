package dal.mitacsgri.treecare.screens.teams.yourteams

import android.content.DialogInterface
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.navigation.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.screens.BaseViewHolder
import dal.mitacsgri.treecare.screens.teams.TeamsFragmentDirections
import kotlinx.android.synthetic.main.item_your_captained_team.view.*
import kotlinx.coroutines.delay

class YourTeamViewHolder(itemView: View, private val viewModel: YourTeamsViewModel): BaseViewHolder<Team>(itemView) {

    override fun bind(item: Team) {
        itemView.apply {
            teamNameTV.text = item.name
            membersCountTV.text = item.members.size.toString()

            if (viewModel.isUserCaptain(item.captain)) {
                buttonDelete.visibility = View.VISIBLE
                addButton.visibility = View.VISIBLE
                buttonDelete.setOnClickListener {
                    MaterialAlertDialogBuilder(context)
                            .setTitle("Delete Team")
                        .setMessage("Do you really want to delete your team '${item.name}' ?")
                        .setPositiveButton("No") { dialogInterface: DialogInterface, i: Int ->
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton("Yes") { _: DialogInterface, _: Int ->
                            viewModel.deleteTeam(item)
                           // findNavController().navigateUp()
                        }
                        .show()

                }
            }

            else {
                buttonDelete.visibility = View.INVISIBLE
                addButton.visibility = View.INVISIBLE
            }

            if (!viewModel.isUserCaptain(item.captain)) {
                buttonExit.visibility = View.VISIBLE
                buttonExit.setOnClickListener {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Exit Team")
                        .setMessage("Do you really want to exit the team '${item.name}' ?")
                        .setPositiveButton("No") { dialogInterface: DialogInterface, i: Int ->
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton("Yes") { _: DialogInterface, _: Int ->
                            viewModel.exitTeam(item)
                        }
                        .show()
                }
            }

            else{
                buttonExit.visibility = View.INVISIBLE
            }

            addButton.setOnClickListener {
                val action = TeamsFragmentDirections.actionTeamsFragmentToJoinTeamFragment(item.name)
                findNavController().navigate(action)
            }

//            viewButton.setOnClickListener {
//                val action = TeamsFragmentDirections.actionTeamsFragmentToTeamInfoFragment(item.name)
//                findNavController().navigate(action)
//            } //To be removed since card can be clicked to view team members

            teamCard.setOnClickListener{
                val action = TeamsFragmentDirections.actionTeamsFragmentToTeamInfoFragment(item.name)
                findNavController().navigate(action)
            }

        }
    }
}

           /* buttonDelete.setOnClickListener{
                MaterialAlertDialogBuilder(context)
                    .setTitle("Delete challenge")
                    .setMessage("Do you really want to delete your team '${item.name}' ?")
                    .setPositiveButton("No") { dialogInterface: DialogInterface, i: Int ->
                        dialogInterface.dismiss()
                    }
                    .setNegativeButton("Yes") { _: DialogInterface, _: Int ->
                        viewModel.deleteTeam(item.name)
                    }
                    .show()
            }*/


            //viewModel.delBtnVis(item)
            //if(status.value == true) return
            //else buttonDelete.visibility = View.INVISIBLE
