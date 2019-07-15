package dal.mitacsgri.treecare.screens.teams.allteams

import android.view.View
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.screens.BaseViewHolder
import kotlinx.android.synthetic.main.item_all_teams_team.view.*

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
        }
    }
}