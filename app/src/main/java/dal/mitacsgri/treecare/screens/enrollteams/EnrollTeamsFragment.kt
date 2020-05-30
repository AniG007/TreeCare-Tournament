package dal.mitacsgri.treecare.screens.enrollteams

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.createFragmentViewWithStyle
import dal.mitacsgri.treecare.extensions.toast
import kotlinx.android.synthetic.main.fragment_enroll_teams.view.*
import kotlinx.android.synthetic.main.item_enroll_teams.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class EnrollTeamsFragment : Fragment() {

    private val viewModel: EnrollTeamsViewModel by viewModel()

    val args: EnrollTeamsFragmentArgs by navArgs()
   lateinit var adapter: EnrollTeamsRecyclerViewAdapter
    lateinit var membersrv: RecyclerView


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tournamentName = args.tournamentName
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.createFragmentViewWithStyle(activity, R.layout.fragment_enroll_teams, R.style.tournament_mode_theme)

        view.apply {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            viewModel.getExistingTeams(args.tournamentName)
            enrollButton.setOnClickListener {
                viewModel.enrollTeams(args.tournamentName)
                //findNavController().navigateUp()
                viewModel.MessageStatus.observe(viewLifecycleOwner, Observer {
                    it.toast(context)
                    findNavController().navigateUp()
                })
            }

            viewModel.getTeams().observe(viewLifecycleOwner, Observer {
                recyclerView.apply {
                    layoutManager = LinearLayoutManager(context,RecyclerView.VERTICAL, false)
                    adapter = EnrollTeamsRecyclerViewAdapter(it, viewModel)
                    membersrv.adapter = adapter
                }
                adapter = recyclerView.adapter as EnrollTeamsRecyclerViewAdapter
            })

            //adapter = EnrollTeamsRecyclerViewAdapter(it,viewModel)

            membersrv = findViewById(R.id.recyclerView)
            membersrv.layoutManager = LinearLayoutManager(membersrv.context)
            //membersrv.setHasFixedSize(true)
            team_search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }
                override fun onQueryTextChange(newText: String?): Boolean {
                    adapter.filter.filter(newText)
                    return false
                }

            })
        }
        return view
    }



}
