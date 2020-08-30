package dal.mitacsgri.treecare.screens.teaminfo

import android.content.ClipData
import android.content.DialogInterface
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.firebase.ui.auth.AuthUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.di.sharedPreferencesRepositoryModule
import dal.mitacsgri.treecare.model.TeamInfo
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.screens.BaseViewHolder
import dal.mitacsgri.treecare.screens.MainActivity
import io.grpc.Context
import kotlinx.android.synthetic.main.item_team_info.view.*
class TeamInfoViewHolder(
    itemView: View,
    private val viewModel: TeamInfoViewModel
):BaseViewHolder<TeamInfo>(itemView) {

    override fun bind(item: TeamInfo){
        itemView.apply {
            nameTV.text = item.userName
            stepsCountTV.text = item.stepsCount.toString()
            leafCountTV.text = item.leavesCount.toString()
            //stepsCountTV.text = item.stepsCount.toString()
            //leafCountTV.text = item.leavesCount.toString()

            Glide.with(this).load(item.photoUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView)

            if (viewModel.isCurrentUser(item)) {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context,
                    R.color.colorPrimaryLight))
            }

            if(item.captainId == item.uId){
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context,
                        R.color.captain_primary_color))
            }
            /*viewModel.captainHighlight(item.uId,item.teamName).observe()
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context,
                        R.color.captain_primary_color))
            }*/


            /*if (viewModel.isUserCaptain(item.uId)) {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context,
                        R.color.captain_primary_color))
            }*/

            if (viewModel.isUserCaptain(item.captainId) && (item.captainId != item.uId) ) {
                removePlayer.visibility = View.VISIBLE
                removePlayer.setOnClickListener{
                    MainActivity.playClickSound()
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Remove Team Member")
                        .setMessage("Do you really want to remove '${item.userName}' from your team?")
                        .setPositiveButton("No") { dialogInterface: DialogInterface, i: Int ->
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton("Yes") { _: DialogInterface, _: Int ->
                            viewModel.removePlayer(item)
                        }
                        .show()
                }
            }
            else{
                removePlayer.visibility = View.INVISIBLE
            }

            cardView.setOnClickListener {
                MainActivity.playClickSound()
                //viewModel.display()
            }
        }
    }

    private var membersposition = 0

    fun bind(item : TeamInfo , position: Int){
        membersposition = position + 1
        bind(item)
    }

}