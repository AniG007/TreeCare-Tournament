package dal.mitacsgri.treecare.screens.challenges.challengesbyyou


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.disable
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.screens.challenges.ChallengesViewModel
import kotlinx.android.synthetic.main.fragment_challenges_by_you.view.*
import kotlinx.android.synthetic.main.item_active_challenge.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ChallengesByYouFragment : Fragment() {

    private val mViewModel: ChallengesViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

//        val view = inflater.createFragmentViewWithStyle(
//            activity, R.layout.fragment_challenges_by_you, R.style.challenger_mode_theme)
        val view = inflater.inflate(R.layout.fragment_challenges_by_you, container, false)

        view.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = ChallengesByYouRecyclerViewAdapter(
                mViewModel.challengesByYouList.value!!, mViewModel)
        }

        mViewModel.challengesByYouList.observe(this, Observer {
            //TODO: There should be a better approach to this
            //TODO: Active Listeners have to be implemented in the future for dynamic page updates
            view.recyclerView.adapter = ChallengesByYouRecyclerViewAdapter(it, mViewModel)
        })

        mViewModel.statusMessage.observe(this, Observer {
            if (!mViewModel.messageDisplayed) {
                it.toast(view.context)
                mViewModel.messageDisplayed = true
                if (!it.contains("error", true)) buttonJoin.disable()
            }
        })

        mViewModel.getAllCreatedChallengesChallenges(mViewModel.getCurrentUserId())

        return view
    }


}
