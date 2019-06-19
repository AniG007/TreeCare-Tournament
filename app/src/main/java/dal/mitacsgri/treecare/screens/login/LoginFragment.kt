package dal.mitacsgri.treecare.screens.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.screens.LoginViewModel
import kotlinx.android.synthetic.main.activity_login.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class LoginFragment : Fragment() {
    //Shared ViewModel with parent activity
    private val loginViewModel: LoginViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        activity?.let {
            it.window.statusBarColor = ContextCompat.getColor(context, R.color.gray)
        }

        signInButton.setOnClickListener {
            loginViewModel.startGoogleFitApiConfiguration(context)
        }
    }
}
