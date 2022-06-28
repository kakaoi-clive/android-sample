package io.kakaoi.connectlive.demo.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.kakaoi.connectlive.LocalParticipant
import io.kakaoi.connectlive.Participant
import io.kakaoi.connectlive.ProfileType
import io.kakaoi.connectlive.demo.databinding.DialogItemParticipantBinding
import io.kakaoi.connectlive.demo.databinding.DialogItemVideoBinding
import io.kakaoi.connectlive.media.RemoteVideo
import io.kakaoi.connectlive.media.VideoContent

abstract class VideoStreamAdapter(context: Context) :
    ListAdapter<Any, VideoStreamAdapter.ViewHolder<*>>(ItemCallback) {
    private object ItemCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean = when {
            oldItem is Participant && newItem is Participant -> oldItem.id == newItem.id
            oldItem is RemoteVideo && newItem is RemoteVideo -> oldItem.id == newItem.id
            else -> false
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean = false
    }

    protected abstract var selected: Set<Int>

    private val videoProfiles = buildList {
        add("(Auto)")
        addAll(ProfileType.values())
    }

    private val videoProfileAdapter =
        ArrayAdapter(context, android.R.layout.simple_spinner_item, videoProfiles).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    fun submit(participants: Collection<Participant>) {
        submitList(
            buildList {
                participants.forEach {
                    add(it)
                    addAll(it.videos.values)
                }
            }
        )
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is Participant -> Type.PARTICIPANT.ordinal
        is VideoContent -> Type.VIDEO.ordinal
        else -> throw IllegalStateException()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder<*> = when (Type.values()[viewType]) {
        Type.PARTICIPANT -> ParticipantViewHolder(parent)
        Type.VIDEO -> VideoViewHolder(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder<*>, position: Int) {
        holder.unsafeBind(getItem(position))
    }

    abstract class ViewHolder<T>(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {

        private var _item: Any? = null

        @Suppress("UNCHECKED_CAST")
        protected val item
            get() = _item as T

        fun unsafeBind(item: Any) {
            this._item = item
            bind(this.item)
        }

        abstract fun bind(item: T)
    }

    private class ParticipantViewHolder(private val binding: DialogItemParticipantBinding) :
        ViewHolder<Participant>(binding) {

        constructor(parent: ViewGroup) : this(
            DialogItemParticipantBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun bind(item: Participant) {
            binding.id.text = buildString {
                append(item.id)

                if (item is LocalParticipant) {
                    append(" [ME]")
                }

                if (item.audios.any { (_, audio) -> audio.isAlwaysOn }) {
                    append(" [AO]")
                }
            }
        }
    }

    private inner class VideoViewHolder(private val binding: DialogItemVideoBinding) :
        ViewHolder<VideoContent>(binding) {
        constructor(parent: ViewGroup) : this(
            DialogItemVideoBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        )

        init {
            binding.profile.adapter = videoProfileAdapter

            binding.videoName.setOnClickListener { button ->
                check(button is CompoundButton)

                (item as? RemoteVideo)?.let {
                    selected =
                        if (button.isChecked) selected + it.id
                        else selected - it.id
                }
            }

            binding.profile.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        // String "Auto" becomes null
                        onVideoProfileSelected(videoProfileAdapter.getItem(position) as? ProfileType)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
        }

        override fun bind(item: VideoContent) {
            binding.videoName.text = buildString {
                append(item.extraValue ?: "camera")
                append("(id=")
                append(item.id)
                append(")")
            }
            binding.videoName.isChecked = item.id in selected
            binding.videoName.isEnabled = item is RemoteVideo
            binding.profile.isEnabled = item is RemoteVideo

            if (item is RemoteVideo)
                binding.profile.setSelection(
                    item.demandedProfile?.let(videoProfiles::indexOf) ?: 0
                )
        }

        private fun onVideoProfileSelected(profileType: ProfileType?) {
            (item as? RemoteVideo)?.demandedProfile = profileType
        }
    }

    private enum class Type {
        PARTICIPANT, VIDEO
    }
}
