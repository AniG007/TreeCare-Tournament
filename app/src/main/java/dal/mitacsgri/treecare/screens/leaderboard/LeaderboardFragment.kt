package dal.mitacsgri.treecare.screens.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import kotlinx.android.synthetic.main.fragment_leaderboard.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class LeaderboardFragment : Fragment() {

    private val args: LeaderboardFragmentArgs by navArgs()
    private val mViewModel: LeaderboardItemViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_leaderboard, container, false)

        findNavController().navigate(R.id.action_leaderboardFragment_to_challengeCompleteDialog)

        view.apply {
            headingTV.text = args.challengeName

            mViewModel.getChallengersList(args.challengeName).observe(this@LeaderboardFragment, Observer {
                recyclerView.apply {
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                    adapter = LeaderboardRecyclerViewAdapter(it, mViewModel)
                }
            })

        }

        return view
    }


}
