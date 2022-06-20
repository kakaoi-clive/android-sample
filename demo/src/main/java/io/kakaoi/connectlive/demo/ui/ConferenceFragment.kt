package io.kakaoi.connectlive.demo.ui

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import io.kakaoi.connectlive.demo.ConferenceService
import io.kakaoi.connectlive.demo.R
import io.kakaoi.connectlive.demo.databinding.CellRemoteVideoBinding
import io.kakaoi.connectlive.demo.databinding.FragmentConferenceBinding
import io.kakaoi.connectlive.media.RemoteVideo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*

class ConferenceFragment : Fragment() {

    private var _binding: FragmentConferenceBinding? = null
    private val binding get() = checkNotNull(_binding)

    private lateinit var service: ConferenceService.Binder
    private val selectedVideos = MutableStateFlow(emptyArray<RemoteVideo?>())

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(REQUEST_PICK_VIDEO, ::onFragmentResult)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentConferenceBinding.inflate(inflater, container, false).also {
        _binding = it

        viewLifecycleOwner.lifecycleScope.launch {
            ConferenceService.bind(requireContext(), ::onServiceBind)
        }

        selectedVideos
            .onEach { videos ->
                binding.cells.forEachIndexed { index, cell ->
                    cell.root.isVisible = index < videos.size
                    cell.video.bind(videos.getOrNull(index))
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }.root

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.conference, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.performIdentifierAction(R.id.action_layout_2x1, 0)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_layout_1x1 -> {
                setCellCount(1 * 1)
                item.isChecked = true
            }
            R.id.action_layout_2x1 -> {
                setCellCount(2 * 1)
                item.isChecked = true
            }
            R.id.action_layout_2x2 -> {
                setCellCount(2 * 2)
                item.isChecked = true
            }
            R.id.action_layout_3x2 -> {
                setCellCount(3 * 2)
                item.isChecked = true
            }
            R.id.action_layout_4x2 -> {
                setCellCount(4 * 2)
                item.isChecked = true
            }
            R.id.action_select_videos -> PickVideosDialogFragment
                .newInstance(
                    service.remoteParticipants,
                    selectedVideos.value.filterNotNull(),
                    selectedVideos.value.size
                )
                .show(parentFragmentManager, REQUEST_PICK_VIDEO)
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onFragmentResult(requestKey: String, bundle: Bundle) {
        when (requestKey) {
            REQUEST_PICK_VIDEO -> PickVideosDialogFragment.handleResult(bundle) { selected ->
                val newSelection = service.remoteVideos.value
                    .filter { it.id in selected }
                    .toCollection(LinkedList())

                selectedVideos.value = selectedVideos.value
                    .map { video -> video?.takeIf { newSelection.remove(it) } }
                    .map { video -> video ?: newSelection.poll() }
                    .toTypedArray()
            }
        }
    }

    private fun onServiceBind(service: ConferenceService.Binder) {
        this.service = service

        service.remoteVideos
            .onEach { availables ->
                selectedVideos.value = checkNotNull(selectedVideos.value).map {
                    it?.takeIf { it in availables }
                }.toTypedArray()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setCellCount(count: Int) {
        selectedVideos.value = selectedVideos.value.copyOf(count)
    }

    companion object {
        private const val TAG = "ConferenceFragment"
        private const val REQUEST_PICK_VIDEO = "req:pick_videos"

        val FragmentConferenceBinding.cells: List<CellRemoteVideoBinding>
            get() = listOf(cell0, cell2, cell1, cell3, cell4, cell5, cell6, cell7)
    }
}