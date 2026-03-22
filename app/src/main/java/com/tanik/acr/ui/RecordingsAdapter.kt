package com.tanik.acr.ui

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.tanik.acr.R
import com.tanik.acr.data.RecordingFile
import java.text.DateFormat
import java.util.Date

class RecordingsAdapter(
    private val onPlayToggle: (RecordingFile) -> Unit,
    private val onShare: (RecordingFile) -> Unit,
    private val onDelete: (RecordingFile) -> Unit
) : ListAdapter<RecordingFile, RecordingsAdapter.RecordingViewHolder>(DiffCallback) {

    private var playingUri: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view, onPlayToggle, onShare, onDelete)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, item.uri.toString() == playingUri)
    }

    fun setPlayingUri(uri: String?) {
        if (playingUri == uri) {
            return
        }
        playingUri = uri
        notifyDataSetChanged()
    }

    class RecordingViewHolder(
        itemView: View,
        private val onPlayToggle: (RecordingFile) -> Unit,
        private val onShare: (RecordingFile) -> Unit,
        private val onDelete: (RecordingFile) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val fileNameText: TextView = itemView.findViewById(R.id.fileNameText)
        private val fileMetaText: TextView = itemView.findViewById(R.id.fileMetaText)
        private val filePathText: TextView = itemView.findViewById(R.id.filePathText)
        private val playButton: MaterialButton = itemView.findViewById(R.id.playButton)
        private val shareButton: MaterialButton = itemView.findViewById(R.id.shareButton)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)
        private val dateFormatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

        fun bind(item: RecordingFile, isPlaying: Boolean) {
            fileNameText.text = item.displayName
            val size = Formatter.formatShortFileSize(itemView.context, item.sizeBytes)
            val date = dateFormatter.format(Date(item.modifiedAt))
            val duration = formatDuration(item.durationMs)
            fileMetaText.text = "$date • $duration • $size"
            filePathText.text = item.pathLabel
            playButton.setText(if (isPlaying) R.string.stop_playback else R.string.play)

            playButton.setOnClickListener { onPlayToggle(item) }
            shareButton.setOnClickListener { onShare(item) }
            deleteButton.setOnClickListener { onDelete(item) }
        }

        private fun formatDuration(durationMs: Long): String {
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<RecordingFile>() {
        override fun areItemsTheSame(oldItem: RecordingFile, newItem: RecordingFile): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: RecordingFile, newItem: RecordingFile): Boolean {
            return oldItem == newItem
        }
    }
}
