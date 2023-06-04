package com.example.wavemap.ui.share.fragments

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.example.wavemap.R
import com.example.wavemap.db.ShareMeasures
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.dialogs.CancelOperationDialog
import com.example.wavemap.dialogs.LoadingDialog
import com.example.wavemap.dialogs.ShareExportDialog
import com.example.wavemap.utilities.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*


class FileExportFragment : Fragment() {

    private var loading_dialog = LoadingDialog{
        if (parentFragmentManager.findFragmentByTag(CancelOperationDialog.TAG) != null) { return@LoadingDialog }

        CancelOperationDialog(R.string.cancel_export, null){
            findNavController().popBackStack()
        }.show(parentFragmentManager, CancelOperationDialog.TAG)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_file_export, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = Room.databaseBuilder(requireContext(), WaveDatabase::class.java, Constants.DATABASE_NAME).build()

        view.findViewById<Button>(R.id.start_file_export_button).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val file_name = "${System.currentTimeMillis()}.wavemap"
                val out_dir = File(requireContext().cacheDir, "/shared")
                val out_path = File(out_dir, file_name)

                // Export to cache
                try {
                    withContext(Dispatchers.Main) { loading_dialog.show(childFragmentManager, LoadingDialog.TAG) }
                    val exported_data = ShareMeasures.export(db)
                    val fout = FileOutputStream(out_path)

                    out_dir.mkdir()
                    fout.write(exported_data.toByteArray())
                    fout.close()
                }
                catch (err: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
                    }
                }

                // Dialog to save/share
                withContext(Dispatchers.Main) {
                    loading_dialog.dismiss()
                    ShareExportDialog(
                        onSave = {
                            lifecycleScope.launch { download(out_path) }
                        },
                        onShare = {
                            share(out_path)
                        }
                    ).show(childFragmentManager, ShareExportDialog.TAG)
                }
            }
        }

    }

    private fun share(file: File) {
        val to_share_uri = FileProvider.getUriForFile(requireContext(), "${requireContext().applicationContext.packageName}.provider", file);
        val share_intent = Intent(Intent.ACTION_SEND)
        share_intent.type = "text/*";
        share_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        share_intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(to_share_uri.toString()))
        share_intent.clipData = ClipData(getString(R.string.wave_map_export), arrayOf( share_intent.type ), ClipData.Item(to_share_uri))
        startActivity(Intent.createChooser(share_intent, getString(R.string.wave_map_export)))
    }

    private fun download(file: File) {
        val destination_path = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.name)

        loading_dialog.show(childFragmentManager, LoadingDialog.TAG)
        try {
            val fin: InputStream = FileInputStream(file)
            val fout: OutputStream = FileOutputStream(destination_path)
            val buf = ByteArray(1024)
            var len: Int

            while (fin.read(buf).also { len = it } > 0) {
                fout.write(buf, 0, len)
            }

            fin.close()
            fout.close()
        } catch (err: Exception) {
            Toast.makeText(requireContext(), getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
        }
        loading_dialog.dismiss()
    }

}