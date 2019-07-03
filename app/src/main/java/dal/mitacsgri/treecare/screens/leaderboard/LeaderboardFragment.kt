package dal.mitacsgri.treecare.screens.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import dal.mitacsgri.treecare.R
import kotlinx.android.synthetic.main.fragment_leaderboard.view.*

/**
 * A simple [Fragment] subclass.
 */
class LeaderboardFragment : Fragment() {

    private val args: LeaderboardFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_leaderboard, container, false)

        view.apply {
            headingTV.text = args.challengeName
        }

        return view
    }


}
