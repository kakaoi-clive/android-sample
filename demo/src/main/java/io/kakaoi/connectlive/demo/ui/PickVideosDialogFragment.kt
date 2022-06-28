package io.kakaoi.connectlive.demo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import io.kakaoi.connectlive.demo.ConferenceService
import io.kakaoi.connectlive.demo.R
import io.kakaoi.connectlive.demo.databinding.DialogFragmentPickVideosBinding
import io.kakaoi.connectlive.media.RemoteVideo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PickVideosDialogFragment : DialogFragment() {

    private val viewLifecycleScope get() = viewLifecycleOwner.lifecycleScope

    private var _binding: DialogFragmentPickVideosBinding? = null
    private val binding get() = checkNotNull(_binding)

    private val selectedVideoIds = MutableStateFlow<Set<Int>>(emptySet())

    private lateinit var adapter: VideoStreamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Icl2Demo_DialogWhenLarge)

        selectedVideoIds.value =
            requireArguments().getIntArray(PARAM_PRESELECTED)?.toSet().orEmpty()

        adapter = object : VideoStreamAdapter(requireContext()) {
            override var selected: Set<Int> by selectedVideoIds::value
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = DialogFragmentPickVideosBinding.inflate(inflater, container, false).also {
        _binding = it

        viewLifecycleScope.launch {
            ConferenceService.bind(requireContext(), ::onServiceBind)
        }
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.participants.adapter = adapter

        val limit = requireArguments().getInt(PARAM_LIMIT)

        selectedVideoIds.onEach {
            binding.applyButton.isEnabled = it.size <= limit
        }.launchIn(viewLifecycleScope)

        binding.applyButton.setOnClickListener {
            setFragmentResult(requireNotNull(tag), createResult(selectedVideoIds.value))
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onServiceBind(service: ConferenceService.Binder) {
        adapter.submit(buildList {
            add(service.localParticipant)
            addAll(service.remoteParticipants)
        })
    }

    companion object {
        private const val PARAM_PRESELECTED = "preselected"
        private const val PARAM_LIMIT = "limit"

        private const val RESULT_SELECTED = "selected"

        fun newInstance(
            preselected: Collection<RemoteVideo>,
            limit: Int
        ) = PickVideosDialogFragment().apply {
            arguments = bundleOf(
                PARAM_PRESELECTED to preselected.map { it.id }.toIntArray(),
                PARAM_LIMIT to limit
            )
        }

        private fun createResult(selected: Collection<Int>) =
            bundleOf(RESULT_SELECTED to selected.toIntArray())

        fun handleResult(result: Bundle, onResult: (selected: List<Int>) -> Unit) {
            val selected = result.getIntArray(RESULT_SELECTED)?.toList().orEmpty()
            onResult(selected)
        }
    }
}
