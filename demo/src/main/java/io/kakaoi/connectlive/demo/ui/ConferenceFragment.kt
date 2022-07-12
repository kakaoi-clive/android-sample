package io.kakaoi.connectlive.demo.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.CompoundButton
import android.widget.EditText
import androidx.activity.result.ActivityResultCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import io.kakaoi.connectlive.VideoCapturerFactory
import io.kakaoi.connectlive.demo.ConferenceService
import io.kakaoi.connectlive.demo.R
import io.kakaoi.connectlive.demo.databinding.CellRemoteVideoBinding
import io.kakaoi.connectlive.demo.databinding.FragmentConferenceBinding
import io.kakaoi.connectlive.media.RemoteVideo
import io.kakaoi.connectlive.utils.showStats
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*

class ConferenceFragment : Fragment() {

    private val viewLifecycleScope get() = viewLifecycleOwner.lifecycleScope

    private var _binding: FragmentConferenceBinding? = null
    private val binding get() = checkNotNull(_binding)

    private val _service = CompletableDeferred<ConferenceService.Binder>()
    private val service get() = _service.getCompleted()

    private val selectedVideos = MutableStateFlow(emptyArray<RemoteVideo?>())

    private val shareScreen = registerForActivityResult(
        VideoCapturerFactory.CreateScreenCapture,
        ActivityResultCallback(::onScreenCaptureResult)
    )

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

        viewLifecycleScope.launch {
            ConferenceService.bind(requireContext(), ::onServiceBind)
        }

        binding.cells.forEach { binding ->
            binding.video.onFrameInfoListener = {
                binding.frameInfo.text = it.entries
                    .joinToString("\n") { (key, value) -> "$key=$value" }
            }
        }

        selectedVideos
            .onEach { videos ->
                binding.cells.forEachIndexed { index, cell ->
                    cell.root.isVisible = index < videos.size
                    val video = videos.getOrNull(index)
                    cell.video.bind(video)
                    cell.streamInfo.text =
                        video?.run { "owner=$owner\nid=$id\n${extraValue.orEmpty()}" }
                    cell.streamInfo.setOnClickListener { video?.showStats(it.context) }
                    cell.frameInfo.text = null
                }
            }
            .launchIn(viewLifecycleScope)

        with(binding.localMedia) {
            camera.setZOrderOnTop(true)
            screen.setZOrderOnTop(true)

            cameraEnabled.setOnClickListener { button ->
                check(button is CompoundButton)
                service.setCameraEnabled(button.isChecked)
            }

            cameraEnabled.setOnCheckedChangeListener { _, isChecked ->
                cameraFacing.isEnabled = isChecked
            }

            cameraFacing.setOnClickListener { button ->
                check(button is CompoundButton)

                viewLifecycleScope.launch {
                    button.isChecked = service.switchCamera()
                }
            }

            screenShared.setOnClickListener { button ->
                check(button is CompoundButton)
                if (button.isChecked)
                    shareScreen.launch(Unit)
                else
                    service.stopShareScreen()
            }

            audioEnabled.setOnClickListener { button ->
                check(button is CompoundButton)
                service.setAudioEnabled(button.isChecked)
            }

            audioEnabled.setOnCheckedChangeListener { _, isChecked ->
                audioAlwaysOn.isEnabled = isChecked
            }

            audioAlwaysOn.setOnClickListener { button ->
                check(button is CompoundButton)
                service.setAudioAlwaysOn(button.isChecked)
            }

            sendMessage.setOnClickListener { view ->
                val input = EditText(view.context).apply {
                    hint = context.getString(R.string.please_input_message)
                    inputType = InputType.TYPE_CLASS_TEXT
                }
                AlertDialog.Builder(view.context)
                    .setTitle(getString(R.string.send_message))
                    .setView(input)
                    .setPositiveButton(getString(R.string.send)) { _, _ ->
                        service.sendUserMessage(input.text.toString())
                    }
                    .setNegativeButton(getString(R.string.close)) { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
        }
    }.root

    override fun onResume() {
        super.onResume()
        runCatching {
            service.localCamera.value?.start()
        }
    }

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

    private fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            REQUEST_PICK_VIDEO -> PickVideosDialogFragment.handleResult(result) { selected ->
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
        _service.complete(service)

        binding.localMedia.myPid.text =
            getString(R.string.my_pid_is_arg, service.localParticipant.id)

        binding.localMedia.cameraEnabled.isChecked = service.isCameraEnabled
        binding.localMedia.audioEnabled.isChecked = service.isAudioEnabled
        binding.localMedia.audioAlwaysOn.isChecked = service.isAudioAlwaysOn

        service.localCamera
            .onEach { camera ->
                binding.localMedia.camera.bind(camera)
                binding.localMedia.cameraFacing.isChecked = camera?.isFrontFacing == true
                binding.localMedia.camera.setOnClickListener {
                    camera?.showStats(it.context)
                }
            }
            .launchIn(viewLifecycleScope)

        service.localScreen
            .onEach { screen ->
                binding.localMedia.screen.apply {
                    isVisible = screen != null
                    bind(screen?.video)
                }
                binding.localMedia.screenShared.isChecked = screen != null
                binding.localMedia.screen.setOnClickListener {
                    screen?.video?.showStats(it.context)
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        service.remoteVideos
            .onEach { availables ->
                selectedVideos.value = checkNotNull(selectedVideos.value).map {
                    it?.takeIf { it in availables }
                }.toTypedArray()
            }
            .launchIn(viewLifecycleScope)

        service.audioLevels
            .observe(viewLifecycleOwner) { levels ->
                binding.cells.zip(selectedVideos.value) { cell, video ->
                    cell.audioLevel.text = levels[video?.owner]?.let { "Audio Level=$it" }
                }
            }
    }

    private fun onScreenCaptureResult(data: Intent?) =
        if (data != null) service.shareScreen(data)
        else binding.localMedia.screenShared.isChecked = false

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