/**
 * Dialog for measurements filtering
 * */

package com.example.wavemap.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import com.example.wavemap.R


class MeasureFilterDialog(
    private val items : ArrayList<CharSequence>,
    private val onSelected : (index: Int) -> Unit
) : DialogFragment() {
    private var index_mapper : Map<Int, Int> = mutableMapOf() // Maps the index of the items of the ListView (potentially filtered) to the real indexes

    companion object {
        const val TAG = "Measure-Filter-Dialog"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val view: View = requireActivity().layoutInflater.inflate(R.layout.dialog_filter_measures, null)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, items.toCollection(ArrayList()))
            val query_list = view.findViewById<ListView>(R.id.query_list)
            val query_edittext = view.findViewById<EditText>(R.id.query_edittext)

            query_list.adapter = adapter
            query_list.setOnItemClickListener { _, _, position, _ ->
                val real_index = index_mapper[position] ?: position

                onSelected(real_index)
                this.dismiss()
            }

            query_edittext.doOnTextChanged { text, _, _, _ ->
                var query = text
                if (query == null) { query = "" }

                index_mapper = mutableMapOf()
                adapter.clear()
                for (i in 0 until items.size) {
                    if (items[i].contains(query, ignoreCase=true)) {
                        adapter.add(items[i])
                        (index_mapper as MutableMap<Int, Int>)[adapter.count-1] = i
                    }
                }
            }

            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
