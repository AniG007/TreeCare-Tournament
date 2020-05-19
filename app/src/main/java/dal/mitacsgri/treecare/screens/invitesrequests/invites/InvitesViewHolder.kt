package dal.mitacsgri.treecare.screens.invitesrequests.invites

import android.view.View
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dal.mitacsgri.treecare.model.InvitesRequest
import dal.mitacsgri.treecare.screens.BaseViewHolder
import dal.mitacsgri.treecare.screens.invitesrequests.InvitesRequestViewModel
import kotlinx.android.synthetic.main.item_invites.view.*

class InvitesViewHolder(itemView: View, private val viewModel: InvitesRequestViewModel)
: BaseViewHolder<InvitesRequest>(itemView) {
    override fun bind(item:InvitesRequest){
        itemView.apply{

            captainName.text = item.userName
            teamName.text = item.teamName
            Glide.with(this).load(item.photoUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView)

            AcceptButton.setOnClickListener{
                viewModel.acceptInvite(item)
            }

            DeclineButton.setOnClickListener{
                viewModel.declineInvite(item)
            }
        }
    }
}