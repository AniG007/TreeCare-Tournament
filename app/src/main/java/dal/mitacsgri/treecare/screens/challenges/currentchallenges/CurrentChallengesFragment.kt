package dal.mitacsgri.treecare.screens.challenges.currentchallenges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.extensions.createFragmentViewWithStyle
import kotlinx.android.synthetic.main.fragment_current_challenges.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class CurrentChallengesFragment : Fragment() {

    private val mViewModel: CurrentChallengesViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.createFragmentViewWithStyle(
            activity, R.layout.fragment_current_challenges, R.style.challenger_mode_theme)

        //This clear needs to be done here otherwise whenever this fragment is created as a result of coming
        //back from a fragment up in the navigation stack, the elements are added to the list, as a result of which,
        //the elements are duplicated
        mViewModel.challengesList.value?.clear()

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
