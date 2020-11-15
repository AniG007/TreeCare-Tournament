package dal.mitacsgri.treecare.screens.tournamentleaderboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dal.mitacsgri.treecare.R
import kotlinx.android.synthetic.main.activity_leaderboard.*
import kotlinx.android.synthetic.main.fragment_leaderboard.*
import kotlinx.android.synthetic.main.fragment_leaderboard.backButton
import kotlinx.android.synthetic.main.fragment_tournament_leaderboard.*

class TournamentLeaderBoardActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_leaderboard)

        //for closing the activity and transitioning to tree screen smoothly
        backButton.setOnClickListener {
            finish()
        }
    }
}
