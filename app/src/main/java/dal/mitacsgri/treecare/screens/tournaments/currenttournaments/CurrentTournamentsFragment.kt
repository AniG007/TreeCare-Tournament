package dal.mitacsgri.treecare.screens.tournaments.currenttournaments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dal.mitacsgri.treecare.R

/**
 * A simple [Fragment] subclass.
 */
class CurrentTournamentsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_current_tournaments, container, false)
    }


}
