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
import dal.mitacsgri.treecare.extensions.createFragmentViewWithStyle
import kotlinx.android.synthetic.main.fragment_challenges_by_you.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChallengesByYouFragment : Fragment() {

    private val mViewModel: ChallengesByYouViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.createFragmentViewWithStyle(
            activity, R.layout.fragment_challenges_by_you, R.style.challenger_mode_theme)

        view.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = ChallengesByYouRecyclerViewAdapter(
                mViewModel.challengesList.value!!, mViewModel)
        }

        mViewModel.challengesList.observe(this, Observer {
            //TODO: There should be a better approach to this
            view.recyclerView.adapter = ChallengesByYouRecyclerViewAdapter(it, mViewModel)
        })

        mViewModel.getAllCreatedChallengesChallenges(mViewModel.getCurrentUserId())

        return view
    }


}