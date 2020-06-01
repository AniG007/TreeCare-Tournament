package dal.mitacsgri.treecare.screens.enrollteams


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.createFragmentViewWithStyle
import dal.mitacsgri.treecare.extensions.toast
import kotlinx.android.synthetic.main.fragment_enroll_teams.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class EnrollTeamsFragment : Fragment() {

    private val viewModel: EnrollTeamsViewModel by viewModel()

    //    val args: EnrollTeamsFragmentArgs by navArgs()
    lateinit var adapter: EnrollTeamsRecyclerViewAdapter
    lateinit var membersrv: RecyclerView


//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        val tournamentName = args.tournamentName
//    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.createFragmentViewWithStyle(activity, R.layout.fragment_enroll_teams, R.style.tournament_mode_theme)

        view.apply {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
                //findNavController().navigate(R.id.action_enrollTeamsFragment_to_createTournamentFragment)
            }

            //viewModel.getExistingTeams(args.tournamentName)
            enrollButton.setOnClickListener {
                //viewModel.enrollTeams(args.tournamentName)
                //findNavController().navigateUp()
                viewModel.MessageStatus.observe(viewLifecycleOwner, Observer {
                    it.toast(context)
                    val bundle = bundleOf("teamsToAdd" to viewModel.teamsHolder.value)
                    findNavController().navigateUp()
                })
            }

            viewModel.getTeams().observe(viewLifecycleOwner, Observer {
                recyclerView.apply {
                    layoutManager = LinearLayoutManager(context,RecyclerView.VERTICAL, false)
                    adapter = EnrollTeamsRecyclerViewAdapter(it, viewModel)
                    //membersrv.adapter = adapter
                }
                adapter = recyclerView.adapter as EnrollTeamsRecyclerViewAdapter
            })

            membersrv = findViewById(R.id.recyclerView)
//            membersrv.layoutManager = LinearLayoutManager(membersrv.context)
//            membersrv.setHasFixedSize(true)

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

//    override fun onCreate(savedInstanceState: Bundle?){
//
//        //Overriding back button since the navigateUp() started crashing when I tried introducing safeArgs while navigating back
//        //stackoverflow: https://stackoverflow.com/questions/33242776/android-viewgroup-crash-attempt-to-read-from-field-int-android-view-view-mview
//        // This link concerns with answers related to animation and handling the error. I'm handling this by overriding the navigation
//        // Maybe deleting whole of createtournament and enrollteams module and putting them back may be an alternative
//        super.onCreate(savedInstanceState)
//        val callback = requireActivity().onBackPressedDispatcher.addCallback(this){
//            findNavController().popBackStack()
//            findNavController().navigate(R.id.action_enrollTeamsFragment_to_createTournamentFragment)
//        }
//    }

}
