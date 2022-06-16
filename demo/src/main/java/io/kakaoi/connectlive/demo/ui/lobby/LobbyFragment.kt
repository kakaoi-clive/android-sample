package io.kakaoi.connectlive.demo.ui.lobby

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.kakaoi.connectlive.demo.PreferencesActivity
import io.kakaoi.connectlive.demo.R
import io.kakaoi.connectlive.demo.databinding.FragmentLobbyBinding

class LobbyFragment : Fragment() {
    private val viewModel by viewModels<LobbyViewModel>()

    private var _binding: FragmentLobbyBinding? = null
    private val binding get() = checkNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLobbyBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewModel.text.observe(viewLifecycleOwner) {
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class Panel : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_lobby, rootKey)

            findPreference<Preference>(getString(R.string.key_preferences))?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), PreferencesActivity::class.java))
                true
            }
        }
    }
}