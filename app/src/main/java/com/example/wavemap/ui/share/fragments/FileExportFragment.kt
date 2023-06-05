package com.example.wavemap.ui.share.fragments

import android.Manifest
import android.app.*
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.wavemap.R
import com.example.wavemap.dialogs.LoadingDialog
import com.example.wavemap.dialogs.MeasureFilterDialog
import com.example.wavemap.dialogs.ShareExportDialog
import com.example.wavemap.ui.share.viewmodels.FileExportViewModel
import java.io.*


class FileExportFragment : Fragment() {

    private val view_model : FileExportViewModel by viewModels()

    private var loading_dialog = LoadingDialog()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_file_export, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.start_file_export_button).setOnClickListener {
            // Save export in cache
            loading_dialog.show(childFragmentManager, LoadingDialog.TAG)
            view_model.createExportInCache()
        }

        view_model.export_success.observe(viewLifecycleOwner) { export_success ->
            loading_dialog.dismiss()

            if (!export_success) {
                Toast.makeText(requireContext(), getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
            } else {
                view_model.download_success.observe(viewLifecycleOwner) { download_success ->
                    loading_dialog.dismiss()

                    if (download_success) {
                        sendDownloadNotification()
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
                    }
                }

                ShareExportDialog(
                    onSave = {
                        loading_dialog.show(childFragmentManager, LoadingDialog.TAG)
                        view_model.downloadExport()
                    },
                    onShare = {
                        share(view_model.export_path)
                    }
                ).show(childFragmentManager, ShareExportDialog.TAG)
            }
        }

    }

    override fun onPause() {
        super.onPause()
        if (childFragmentManager.findFragmentByTag(ShareExportDialog.TAG) != null) {
            (childFragmentManager.findFragmentByTag(ShareExportDialog.TAG) as ShareExportDialog).dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        view_model.cancel()
    }

    private fun share(file: File) {
        val to_share_uri = FileProvider.getUriForFile(requireContext(), "${requireContext().applicationContext.packageName}.provider", file);
        val share_intent = Intent(Intent.ACTION_SEND)
        share_intent.type = "text/json";
        share_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        share_intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(to_share_uri.toString()))
        share_intent.clipData = ClipData(getString(R.string.wave_map_export), arrayOf( share_intent.type ), ClipData.Item(to_share_uri))
        startActivity(Intent.createChooser(share_intent, getString(R.string.wave_map_export)))
    }

    private fun sendDownloadNotification() {
        if ( ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        lateinit var notification_builder : NotificationCompat.Builder
        val channel_id = "ch_download_export"
        val channel_name: CharSequence = getString(R.string.export_download)
        val channel_desc = getString(R.string.export_download_desc)
        val channel = NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = channel_desc
        val notificationManager = getSystemService(requireContext(), NotificationManager::class.java) ?: return
        notificationManager.createNotificationChannel(channel)
        notification_builder = NotificationCompat.Builder(requireContext(), channel_id)

        notification_builder.setContentTitle(getString(R.string.export_download)).setContentText(getString(R.string.export_download_text))
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(TaskStackBuilder.create(requireContext()).run {
                addNextIntentWithParentStack(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            })
        NotificationManagerCompat.from(requireContext()).notify(1, notification_builder.build())
    }
}