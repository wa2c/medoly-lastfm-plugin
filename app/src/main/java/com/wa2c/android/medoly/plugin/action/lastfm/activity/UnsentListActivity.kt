package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.databinding.ActivityUnsentListBinding
import com.wa2c.android.medoly.plugin.action.lastfm.databinding.LayoutUnsentListItemBinding
import com.wa2c.android.medoly.plugin.action.lastfm.dialog.ConfirmDialogFragment
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.prefs.Prefs
import de.umass.lastfm.scrobble.ScrobbleData
import timber.log.Timber
import java.util.*


/**
 * Unsent list activity.
 */
class UnsentListActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var binding: ActivityUnsentListBinding

    /** List items.  */
    private  lateinit var items: Array<ScrobbleData>
    /** List adapter */
    private lateinit var adapter: UnsentListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unsent_list)
        prefs = Prefs(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_unsent_list)

        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setTitle(R.string.title_activity_unsent_list)

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
                AppUtils.showToast(applicationContext, R.string.message_unsent_check_data)
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
                        val dataArray = itemList.toTypedArray<ScrobbleData>()
                        prefs.putObject(R.string.prefkey_unsent_scrobble_data, dataArray)
                        items = dataArray
                        adapter.checkedSet.clear()
                    } catch (e: Exception) {
                        Timber.e(e)
                        AppUtils.showToast(applicationContext, R.string.message_unsent_delete_failure)
                    }

                    initializeListView()
                }

                dialogFragment.show(this)
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
        binding.unsentListView.layoutManager = LinearLayoutManager(this)
        binding.unsentListView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
