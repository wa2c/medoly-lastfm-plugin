package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softartdev.lastfm.scrobble.ScrobbleData
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.activity.component.viewBinding
import com.wa2c.android.medoly.plugin.action.lastfm.databinding.FragmentUnsentListBinding
import com.wa2c.android.medoly.plugin.action.lastfm.databinding.LayoutUnsentListItemBinding
import com.wa2c.android.medoly.plugin.action.lastfm.dialog.ConfirmDialogFragment
import com.wa2c.android.medoly.plugin.action.lastfm.util.logE
import com.wa2c.android.medoly.plugin.action.lastfm.util.toast
import com.wa2c.android.prefs.Prefs
import java.util.*


/**
 * Unsent list activity.
 */
class UnsentListFragment : Fragment(R.layout.fragment_unsent_list) {
    /** Binding */
    private val binding: FragmentUnsentListBinding by viewBinding()
    /** Prefs */
    private val prefs: Prefs by lazy { Prefs(requireContext()) }

    /** List items.  */
    private  lateinit var items: Array<ScrobbleData>
    /** List adapter */
    private lateinit var adapter: UnsentListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.title_screen_unsent_list)

        // not save
        binding.unsentNotSaveCheckBox.setOnCheckedChangeListener { _, isChecked ->  prefs.putBoolean(R.string.prefkey_unsent_scrobble_not_save, isChecked) }
        binding.unsentNotSaveCheckBox.isChecked = prefs.getBoolean(R.string.prefkey_unsent_scrobble_not_save)

        // check all
        binding.unsentCheckAllButton.setOnClickListener {
            val checkedCount = adapter.checkedSet.size
            if (checkedCount == adapter.itemCount) {
                adapter.checkedSet.clear()
            } else {
                adapter.checkedSet.addAll(Array(adapter.itemCount) {it})
            }
            adapter.notifyDataSetChanged()
        }

        // delete
        binding.unsentDeleteButton.setOnClickListener {
            if (adapter.itemList.isEmpty()) {
                toast(R.string.message_unsent_check_data)
            } else {
                val dialogFragment = ConfirmDialogFragment.newInstance(getString(R.string.message_dialog_unsent_delete_confirm), getString(R.string.title_dialog_unsent_delete_confirm))
                dialogFragment.clickListener = listener@{ _, which, _ ->
                    if (which != DialogInterface.BUTTON_POSITIVE)
                        return@listener

                    try {
                        // delete from list
                        val checks = adapter.checkedSet.toTypedArray()
                        val itemList =  ArrayList(items.toList())
                        for (i in checks.indices.reversed()) {
                            itemList.removeAt(checks[i])
                        }

                        // save result
                        val dataArray = itemList.toTypedArray()
                        prefs.putObject(R.string.prefkey_unsent_scrobble_data, dataArray)
                        items = dataArray
                        adapter.checkedSet.clear()
                    } catch (e: Exception) {
                        logE(e)
                        toast(R.string.message_unsent_delete_failure)
                    }

                    initializeListView()
                }

                dialogFragment.show(requireActivity())
            }
        }

        // items
        initializeListView()
    }

    /**
     * Initialize list.
     */
    private fun initializeListView() {
        val list = prefs.getObjectOrNull<Array<ScrobbleData>>(R.string.prefkey_unsent_scrobble_data)

        if (list == null || list.isEmpty()) {
            val data = ScrobbleData()
            data.track = getString(R.string.message_unsent_no_data)
            items = arrayOf(data)

            binding.unsentListView.visibility = View.INVISIBLE
            binding.unsentNoDataTextView.visibility = View.VISIBLE
            binding.unsentCheckAllButton.isEnabled = false
            binding.unsentDeleteButton.isEnabled = false
        } else {
            items = list

            binding.unsentListView.visibility = View.VISIBLE
            binding.unsentNoDataTextView.visibility = View.INVISIBLE
            binding.unsentCheckAllButton.isEnabled = true
            binding.unsentDeleteButton.isEnabled = true
        }

        //adapter = UnsentListAdapter(this, items!!, checkedSet)
        adapter = UnsentListAdapter(items)
        binding.unsentListView.layoutManager = LinearLayoutManager(requireContext())
        binding.unsentListView.adapter = adapter
        adapter.notifyDataSetChanged()
    }


    /**
     * Unsent list adapter
     */
    private class UnsentListAdapter(val itemList: Array<ScrobbleData>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val checkedSet: TreeSet<Int> = TreeSet()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding: LayoutUnsentListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_unsent_list_item, parent,false)
            val rootView = binding.root
            rootView.tag = binding
            return object : RecyclerView.ViewHolder(rootView) {}
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = itemList[position]
            val binding = holder.itemView.tag as LayoutUnsentListItemBinding
            val context = binding.root.context

            // Update check
            binding.unsentSelectedCheckBox.isChecked = checkedSet.contains(position)
            binding.unsentSelectedCheckBox.setOnTouchListener { _, event ->
                binding.root.onTouchEvent(event)
            }
            binding.root.setOnClickListener {
                if (checkedSet.contains(position)) {
                    checkedSet.remove(position)
                    binding.unsentSelectedCheckBox.isChecked = false
                } else {
                    checkedSet.add(position)
                    binding.unsentSelectedCheckBox.isChecked = true
                }
            }

            // update data
            val time = item.timestamp
            if (time > 0) {
                if (!item.track.isNullOrEmpty())
                    binding.unsentTitleTextView.text = item.track
                if (!item.artist.isNullOrEmpty())
                    binding.unsentArtistTextView.text = item.artist
                if (item.timestamp > 0)
                    binding.unsentTimeTextView.text = context.getString(R.string.label_unsent_played_time, DateUtils.formatDateTime(context, item.timestamp.toLong() * 1000, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL))
                binding.root.isClickable = true
            } else {
                binding.unsentTitleTextView.text = item.track
                binding.unsentArtistTextView.text = null
                binding.unsentTimeTextView.text = null
                binding.unsentSelectedCheckBox.visibility = View.INVISIBLE
                binding.root.isClickable = false
            }
        }

        override fun getItemCount(): Int {
            return itemList.size
        }

    }

}
