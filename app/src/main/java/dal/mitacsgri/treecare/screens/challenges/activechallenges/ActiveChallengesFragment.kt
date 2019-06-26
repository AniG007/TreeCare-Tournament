package dal.mitacsgri.treecare.screens.challenges.activechallenges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import kotlinx.android.synthetic.main.fragment_active_challenges.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ActiveChallengesFragment : Fragment() {

    private val mViewModel: ActiveChallengesViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_active_challenges, container, false)

        view.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = ActiveChallengesRecyclerViewAdapter(
                mViewModel.challengesList.value!!,
                mViewModel)
        }

        mViewModel.challengesList.observe(this, Observer {
            //There should be a better approach to this
            view.recyclerView.adapter = ActiveChallengesRecyclerViewAdapter(
                it,
                mViewModel)
        })

        mViewModel.getAllActiveChallenges()
        return view
    }
}
