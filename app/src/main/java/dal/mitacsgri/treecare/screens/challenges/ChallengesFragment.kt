package dal.mitacsgri.treecare.screens.challenges


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dal.mitacsgri.treecare.R
import kotlinx.android.synthetic.main.fragment_challenges.view.*

class ChallengesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_challenges, container, false)

        view.apply {
            viewPager.adapter = ChallengesPagerAdapter(fragmentManager!!)
            tabLayout.setupWithViewPager(viewPager)
        }

        return view
    }
}
