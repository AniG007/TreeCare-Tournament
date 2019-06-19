package dal.mitacsgri.treecare.screens.login


import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessStatusCodes
import com.google.android.gms.fitness.data.DataType
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.getApplicationContext
import dal.mitacsgri.treecare.extensions.startNextActivity
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.provider.SharedPreferencesProvider
import dal.mitacsgri.treecare.provider.StepCountProvider
import dal.mitacsgri.treecare.screens.modeselection.ModeSelectionActivity
import kotlinx.android.synthetic.main.activity_login.*

class LoginFragment : Fragment() {

    public var authInProgress = false
    private lateinit var sharedPrefProvider: SharedPreferencesProvider
    val SIGN_IN_CODE = 1000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sharedPrefProvider = SharedPreferencesProvider(context)
        signInButton.setOnClickListener {
            startGoogleFitApiConfiguration(context)
        }

        activity?.let {
            it.window.statusBarColor = ContextCompat.getColor(context, R.color.gray)
        }
    }


}
