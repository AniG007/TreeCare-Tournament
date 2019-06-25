package dal.mitacsgri.treecare.screens.currentchallenges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.data.Challenge
import kotlinx.android.synthetic.main.fragment_current_challenges.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class CurrentChallengesFragment : Fragment() {

    private val mViewModel: CurrentChallengesViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_current_challenges, container, false)

        view.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = CurrentChallengesRecyclerViewAdapter(mViewModel.challengesList.value!!, mViewModel)
        }

        mViewModel.challengesList.observe(this, Observer {
            view.recyclerView.adapter?.notifyDataSetChanged()
        })

        mViewModel.getCurrentChallengesForUser()
        return view
    }

    private fun setUpRecyclerView(recyclerView: RecyclerView, challengesList: List<Challenge>) {
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = CurrentChallengesRecyclerViewAdapter(challengesList, mViewModel)
        }
    }
}
