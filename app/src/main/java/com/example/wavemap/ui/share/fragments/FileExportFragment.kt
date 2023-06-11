package com.example.wavemap.ui.share.fragments

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.wavemap.R
import com.example.wavemap.dialogs.LoadingDialog
import com.example.wavemap.dialogs.ShareExportDialog
import com.example.wavemap.dialogs.settings.MissingDiskPermissionsDialog
import com.example.wavemap.notifications.ExportDownloadNotification
import com.example.wavemap.ui.share.viewmodels.FileExportViewModel
import com.example.wavemap.utilities.Permissions
import java.io.*


class FileExportFragment : Fragment() {

    private val view_model : FileExportViewModel by viewModels()

    private var loading_dialog = LoadingDialog()

    private val permissions_check_and_save = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (!Permissions.disk.all{ permission -> ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED }) {
            MissingDiskPermissionsDialog().show(childFragmentManager, MissingDiskPermissionsDialog.TAG)
        }
        else {
            loading_dialog.show(childFragmentManager, LoadingDialog.TAG)
            view_model.downloadExport()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_file_export, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.start_file_export_button).setOnClickListener {
            // Saves export in cache
            loading_dialog.show(childFragmentManager, LoadingDialog.TAG)
            view_model.createExportInCache()
        }

        view_model.export_success.observe(viewLifecycleOwner) { export_success ->
            if (childFragmentManager.findFragmentByTag(LoadingDialog.TAG) != null) {
                loading_dialog.dismiss()
            }

            if (!export_success) {
                Toast.makeText(requireContext(), getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
            } else {
                ShareExportDialog(
                    onSave = {
                        permissions_check_and_save.launch(Permissions.disk)
                    },
                    onShare = {
                        share(view_model.export_path)
                    }
                ).show(childFragmentManager, ShareExportDialog.TAG)
            }
        }

        view_model.download_success.observe(viewLifecycleOwner) { event ->
            val download_success = event.get()
                ?: return@observe

            if (childFragmentManager.findFragmentByTag(LoadingDialog.TAG) != null) {
                loading_dialog.dismiss()
            }

            if (download_success) {
                ExportDownloadNotification.send(requireContext(), 1)
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
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

}