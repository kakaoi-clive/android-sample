package io.kakaoi.connectlive.demo.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import io.kakaoi.connectlive.RemoteParticipant
import io.kakaoi.connectlive.demo.R
import io.kakaoi.connectlive.media.RemoteVideo

class PickVideosDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = requireArguments()
        val videos = arguments.get(PARAM_VIDEOS) as Map<Int, String>
        val preselected = arguments.get(PARAM_PRESELECTED) as List<Int>
        val limit = arguments.getInt(PARAM_LIMIT)

        val items = videos.values.toTypedArray()
        val checkedItems = videos.keys.map { it in preselected }.toBooleanArray()

        return AlertDialog.Builder(requireContext(), theme)
            .setTitle(getString(R.string.title_pick_videos))
            .setMultiChoiceItems(items, checkedItems) { dialog, which, isChecked ->
                check(dialog is AlertDialog)
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled =
                    checkedItems.count { it } <= limit
            }
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                val selected = videos.keys.filterIndexed { index, _ -> checkedItems[index] }
                setFragmentResult(requireNotNull(tag), createResult(selected))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    companion object {
        private const val PARAM_VIDEOS = "videos"
        private const val PARAM_PRESELECTED = "preselected"
        private const val PARAM_LIMIT = "limit"

        private const val RESULT_SELECTED = "selected"

        fun newInstance(
            remoteParticipants: Collection<RemoteParticipant>,
            preselected: Collection<RemoteVideo>,
            limit: Int
        ) = PickVideosDialogFragment().apply {
            val videos = remoteParticipants.flatMap { it.videos.values }
                .associate { it.id to "${it.owner}'s ${it.extraValue ?: it.id}" }

            arguments = bundleOf(
                PARAM_VIDEOS to videos,
                PARAM_PRESELECTED to preselected.map { it.id },
                PARAM_LIMIT to limit
            )
        }

        private fun createResult(selected: List<Int>) =
            bundleOf(RESULT_SELECTED to selected.toIntArray())

        fun handleResult(result: Bundle, onResult: (selected: List<Int>) -> Unit) {
            val selected = result.getIntArray(RESULT_SELECTED)?.toList().orEmpty()
            onResult(selected)
        }
    }
}