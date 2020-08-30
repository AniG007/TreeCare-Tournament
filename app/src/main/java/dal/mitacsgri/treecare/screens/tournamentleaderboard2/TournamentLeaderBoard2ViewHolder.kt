package dal.mitacsgri.treecare.screens.tournamentleaderboard2

import android.view.View
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.model.TournamentLB2
import dal.mitacsgri.treecare.screens.BaseViewHolder
import kotlinx.android.synthetic.main.item_team_info.view.*
import kotlinx.android.synthetic.main.item_tournament_leaderboard2.view.*
import kotlinx.android.synthetic.main.item_tournament_leaderboard2.view.imageView

class TournamentLeaderBoard2ViewHolder(itemView: View, private val viewModel: TournamentLeaderBoard2ViewModel)
    :BaseViewHolder<TournamentLB2>(itemView) {
    override fun bind(item: TournamentLB2) {
        itemView.apply {
            teamNameTV.text = item.teamName
            captainNameTV.text = item.captainName
            Glide.with(this).load(item.photoUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView)
        }
    }
}