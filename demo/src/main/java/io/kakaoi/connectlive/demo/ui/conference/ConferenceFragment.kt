package io.kakaoi.connectlive.demo.ui.conference

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.kakaoi.connectlive.demo.ConferenceService
import io.kakaoi.connectlive.demo.databinding.CellRemoteVideoBinding
import io.kakaoi.connectlive.demo.databinding.FragmentConferenceBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ConferenceFragment : Fragment() {

    private var _binding: FragmentConferenceBinding? = null
    private val binding get() = checkNotNull(_binding)

    private lateinit var service: ConferenceService.Binder

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentConferenceBinding.inflate(inflater, container, false).also {
        _binding = it

        viewLifecycleOwner.lifecycleScope.launch {
            ConferenceService.bind(requireContext(), ::onServiceBind)
        }
    }.root

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onServiceBind(service: ConferenceService.Binder) {
        this.service = service

        service.remoteVideos
            .onEach { videos ->
                binding.cells.forEachIndexed { index, cell ->
                    val video = videos.getOrNull(index)
                    cell.video.bind(video)
                    cell.root.isVisible = video != null
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    companion object {
        val FragmentConferenceBinding.cells: List<CellRemoteVideoBinding>
            get() = listOf(cell0, cell2, cell1, cell3, cell4, cell5, cell6, cell7)
    }
}