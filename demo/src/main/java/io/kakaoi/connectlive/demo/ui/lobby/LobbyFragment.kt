package io.kakaoi.connectlive.demo.ui.lobby

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.google.android.material.snackbar.Snackbar
import io.kakaoi.connectlive.ConnectLive
import io.kakaoi.connectlive.demo.R
import io.kakaoi.connectlive.demo.databinding.FragmentLobbyBinding
import io.kakaoi.connectlive.demo.util.Preferences
import io.kakaoi.connectlive.demo.util.createAppSettingsIntent
import io.kakaoi.connectlive.demo.util.isGranted
import io.kakaoi.connectlive.media.LocalCamera
import io.kakaoi.connectlive.utils.AudioHelper
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LobbyFragment : Fragment() {
    private var _binding: FragmentLobbyBinding? = null
    private val binding get() = checkNotNull(_binding)

    private val panel get() = childFragmentManager.findFragmentById(R.id.panel) as Panel

    private var localCamera: LocalCamera? = null

    private val requestingPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        ::onRequestPermissionsResult
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLobbyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Preferences.cameraEnabled(requireContext())
            .onEach(::onCameraEnabled)
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        localCamera?.dispose()
    }

    private fun onRequestPermissionsResult(result: Map<String, Boolean>) {
        if (result[Manifest.permission.CAMERA] == true) {
            panel.cameraEnabled.isChecked = true
        }

        if (result[Manifest.permission.RECORD_AUDIO] == true) {
            panel.micEnabled.isChecked = true
        }
    }

    private fun onCameraEnabled(enabled: Boolean) {
        if (localCamera == null)
            localCamera = ConnectLive.createLocalCamera(panel.cameraFront.isChecked)

        localCamera?.isEnabled = enabled

        binding.camera.bind(localCamera?.takeIf { enabled })
    }

    private fun setCameraEnabled(enabled: Boolean): Boolean =
        if (!enabled) true
        else usePermission(Manifest.permission.CAMERA)

    private fun setMicEnabled(enabled: Boolean): Boolean =
        if (!enabled) true
        else usePermission(Manifest.permission.RECORD_AUDIO)

    private fun usePermission(permission: String): Boolean = when {
        requireContext().isGranted(permission) -> true
        shouldShowRequestPermissionRationale(permission) -> {
            Snackbar
                .make(
                    requireView(),
                    getString(R.string.require_permission_arg, permission.substringAfterLast('.')),
                    Snackbar.LENGTH_LONG
                )
                .setAction(R.string.app_settings) { startActivity(it.context.createAppSettingsIntent()) }
                .show()
            false
        }
        else -> {
            requestingPermission.launch(arrayOf(permission))
            false
        }
    }

    private fun setCameraFront(front: Boolean): Boolean {
        localCamera?.run {
            if (isFrontFacing == front)
                return true

            switchCamera().thenApplyAsync({
                panel.cameraFront.isChecked = it
            }, ContextCompat.getMainExecutor(requireContext()))
        }

        return false
    }

    class Panel : PreferenceFragmentCompat() {
        private val parent get() = parentFragment as LobbyFragment

        lateinit var cameraEnabled: TwoStatePreference
        lateinit var cameraFront: TwoStatePreference
        lateinit var micEnabled: TwoStatePreference

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_lobby, rootKey)

            cameraEnabled = checkNotNull(findPreference(getString(R.string.key_camera_enabled)))
            cameraFront = checkNotNull(findPreference(getString(R.string.key_camera_front)))
            micEnabled = checkNotNull(findPreference(getString(R.string.key_mic_enabled)))

            checkNotNull(findPreference<ListPreference>(getString(R.string.key_speaker_device))).apply {
                val values = AudioHelper.Device.values().map { it.name }.toTypedArray()
                entries = values
                entryValues = values
            }

            cameraEnabled.setOnPreferenceChangeListener { _, newValue ->
                parent.setCameraEnabled(newValue == true)
            }

            micEnabled.setOnPreferenceChangeListener { _, newValue ->
                parent.setMicEnabled(newValue == true)
            }

            cameraFront.setOnPreferenceChangeListener { _, newValue ->
                parent.setCameraFront(newValue == true)
            }
        }

        override fun onResume() {
            super.onResume()
            if (cameraEnabled.isChecked)
                cameraEnabled.isChecked = requireContext().isGranted(Manifest.permission.CAMERA)

            if (micEnabled.isChecked)
                micEnabled.isChecked = requireContext().isGranted(Manifest.permission.RECORD_AUDIO)
        }
    }
}