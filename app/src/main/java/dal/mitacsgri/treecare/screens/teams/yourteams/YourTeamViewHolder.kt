package dal.mitacsgri.treecare.screens.teams.yourteams

import android.view.View
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.screens.BaseViewHolder
import kotlinx.android.synthetic.main.item_your_captained_team.view.*

class YourTeamViewHolder(itemView: View): BaseViewHolder<Team>(itemView) {

    override fun bind(item: Team) {
        itemView.apply {
            teamNameTV.text = item.name
            membersCountTV.text = item.members.size.toString()
        }
    }
}