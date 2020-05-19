package dal.mitacsgri.treecare.screens.teams.allteams

import android.util.Log
import android.view.View
import androidx.navigation.findNavController
import com.google.android.material.button.MaterialButton
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.screens.BaseViewHolder
import dal.mitacsgri.treecare.screens.teams.TeamsFragmentDirections
import kotlinx.android.synthetic.main.item_all_teams_team.view.*
import org.jetbrains.anko.toast

/**
 * Created by Devansh on 15-07-2019
 */

class AllTeamsItemViewHolder(itemView: View, private val viewModel: AllTeamsViewModel)
    : BaseViewHolder<Team>(itemView) {

    override fun bind(item: Team) {
        itemView.apply {
            nameTV.text = item.name
            descriptionTV.text = item.description
            membersCountTV.text = viewModel.getMembersCountText(item)
            captainNameTV.text = viewModel.getCaptainNameText(item)

            if (viewModel.isUserCaptain(item.captain)) {
                button.text = context.getString(R.string.invite_member)
                button.setOnClickListener {
                    val tName = nameTV.text.toString()
                    Log.d("TAG","TeamNameinAll"+tName)
                    val action = TeamsFragmentDirections.actionTeamsFragmentToJoinTeamFragment(tName)
                    //findNavController().navigate(R.id.action_teamsFragment_to_joinTeamFragment)
                    findNavController().navigate(action)
                }
            } else {
                button.text =
                    if (viewModel.isJoinRequestSent(item)) context.getString(R.string.cancel_request)
                    else context.getString(R.string.send_join_request)
                joinRequestAction(button, item)
            }
        }
    }

    private fun joinRequestAction(button: MaterialButton, team: Team) {

        var sendJoinRequestListener = View.OnClickListener {  }
        var cancelJoinRequestListener = View.OnClickListener {  }

        sendJoinRequestListener = View.OnClickListener {
            viewModel.sendJoinRequest(team.name) {
                changeButtonStateAndBehaviour(
                    it, button, sendJoinRequestListener, cancelJoinRequestListener)
            }
        }

        cancelJoinRequestListener = View.OnClickListener {
            viewModel.cancelJoinRequest(team.name) {
                changeButtonStateAndBehaviour(
                    !it, button, sendJoinRequestListener, cancelJoinRequestListener)
            }
        }

        button.setOnClickListener(
            if (viewModel.isJoinRequestSent(team)) cancelJoinRequestListener
            else sendJoinRequestListener)
    }
   /* private fun invitePlayerAction(button: MaterialButton, user: User) {

        var sendInviteListener = View.OnClickListener {  }
        var cancelInviteListener = View.OnClickListener {  }

        sendInviteListener = View.OnClickListener {
            viewModel.sendInvite(user.uid) {
                changeButtonStateAndBehaviour(
                    it, button, sendInviteListener, cancelInviteListener)
            }
        }

        cancelInviteListener = View.OnClickListener {
            viewModel.cancelInvite(user.uid) {
                changeButtonStateAndBehaviour(
                    !it, button, sendInviteListener, cancelInviteListener)
            }
        }

        button.setOnClickListener(
            if (viewModel.isInviteSent(user)) cancelInviteListener
            else sendInviteListener)
    }*/

    private fun changeButtonStateAndBehaviour(
        status: Boolean, button: MaterialButton,
        sendJoinRequestListener: View.OnClickListener, cancelJoinRequestListener: View.OnClickListener) {

        val context = button.context

        context.apply {
            if (status) {
                toast(R.string.join_request_sent)
                button.text = context.getString(R.string.cancel_request)
                button.setOnClickListener(cancelJoinRequestListener)
            } else {
                toast(R.string.join_request_cancelled)
                button.text = context.getString(R.string.send_join_request)
                button.setOnClickListener(sendJoinRequestListener)
            }
        }
    }
}