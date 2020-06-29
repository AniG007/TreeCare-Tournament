package dal.mitacsgri.treecare.screens.tournamentleaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import kotlinx.android.synthetic.main.fragment_tournament_leaderboard.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.lang.IllegalStateException

class TournamentLeaderBoardFragment : Fragment() {
    private val mViewModel: TournamentLeaderBoardViewModel by viewModel()
    //private val args: TournamentLeaderBoardFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tournament_leaderboard, container, false)

        val args: TournamentLeaderBoardFragmentArgs = TournamentLeaderBoardFragmentArgs.fromBundle(arguments ?:
            Bundle().let {
                it.putString(view.context.getString(R.string.tournament_name), mViewModel.getTournamentName())
                it
            }
        )

        view.apply {
            headingTV.text = args.tournamentName
            backButton.setOnClickListener {
                try{
                    findNavController().navigateUp()
                }
                catch (e: IllegalStateException){
                    activity?.onBackPressed()
                }
            }

            mViewModel.getTeamList(args.tournamentName).observe(viewLifecycleOwner, Observer {
                recyclerView.apply {
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                    adapter = TournamentLeaderBoardRecyclerViewAdapter(it, mViewModel)

                    if((it.size>0) && !mViewModel.isDialogDisplayed){
                        mViewModel.isDialogDisplayed = false

                        val pos = mViewModel.getTeamPosition()

                        //dialog.show(FragmentManager(),"TournamentEndedDialog")
                        val action = TournamentLeaderBoardFragmentDirections.actionTournamentLeaderBoardFragmentToTournamentEndedDialog(pos)
                        findNavController().navigate(action)
                    }
                }
            })


        }

        return view
    }

}