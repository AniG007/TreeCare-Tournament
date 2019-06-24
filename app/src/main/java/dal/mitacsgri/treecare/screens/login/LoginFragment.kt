package dal.mitacsgri.treecare.screens.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.screens.MainViewModel
import kotlinx.android.synthetic.main.fragment_login.view.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class LoginFragment : Fragment() {

    //Shared ViewModel with parent activity
    private val mainViewModel: MainViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        view.signInButton.setOnClickListener {
            mainViewModel.startLoginAndConfiguration(activity!!)
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        activity?.let {
            it.window.statusBarColor = ContextCompat.getColor(context, R.color.gray)
        }
        setUpLiveDataObservers(context)
    }

    private fun setUpLiveDataObservers(context: Context) {
        mainViewModel.apply {

            userFirstName.observe(this@LoginFragment, Observer {
                "Welcome $it !".toast(context)
            })

            stepCountDataFetchedCounter.observe(this@LoginFragment, Observer {
                if (it == 2) {
                    findNavController().navigate(R.id.action_loginFragment_to_modeSelectionFragment)
                }
            })
        }
    }
}
