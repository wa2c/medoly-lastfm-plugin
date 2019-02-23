package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
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

    /** Check index set.。  */
    private val checkedSet: TreeSet<Int> = TreeSet()

    /** List items.  */
    private var items: Array<ScrobbleData>? = null

    //private lateinit var adapter: UnsentListAdapter
    private lateinit var adapter: ListAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unsent_list)
        prefs = Prefs(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_unsent_list)

        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setTitle(R.string.title_activity_unsent_list)

//        // list
//        binding.unsentListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
//            if (checkedSet.contains(position)) {
//                checkedSet.remove(position)
//            } else {
//                checkedSet.add(position)
//            }
//            adapter.notifyDataSetChanged()
//        }

        // not save
        binding.unsentNotSaveCheckBox.setOnCheckedChangeListener { _, isChecked ->  prefs.putBoolean(R.string.prefkey_unsent_scrobble_not_save, isChecked) }
        binding.unsentNotSaveCheckBox.isChecked = prefs.getBoolean(R.string.prefkey_unsent_scrobble_not_save)

        // check all
        binding.unsentCheckAllButton.setOnClickListener {
            var checking = false
            for (i in 0 until adapter.itemCount) {
                checking = checking or checkedSet.add(i)
            }
            if (!checking) {
                // Uncheck if checked
                checkedSet.clear()
            }
            adapter.notifyDataSetChanged()
        }

        // delete
        binding.unsentDeleteButton.setOnClickListener(View.OnClickListener {
            if (checkedSet.isEmpty()) {
                AppUtils.showToast(applicationContext, R.string.message_unsent_check_data)
            } else {
                val dialogFragment = ConfirmDialogFragment.newInstance(getString(R.string.message_dialog_unsent_delete_confirm), getString(R.string.title_dialog_unsent_delete_confirm))
                dialogFragment.clickListener = DialogInterface.OnClickListener { _, which ->
                    if (which != DialogInterface.BUTTON_POSITIVE)
                        return@OnClickListener

                    try {
                        // delete from list
                        val checks = checkedSet.toTypedArray()
                        val itemList =  ArrayList(items!!.toList())
                        for (i in checks.indices.reversed()) {
                            itemList.removeAt(checks[i])
                        }

                        // save result
                        val dataArray = itemList.toTypedArray<ScrobbleData>()
                        prefs.putObject(R.string.prefkey_unsent_scrobble_data, dataArray)
                        items = dataArray
                        checkedSet.clear()
                    } catch (e: Exception) {
                        Timber.e(e)
                        AppUtils.showToast(applicationContext, R.string.message_unsent_delete_failure)
                    }

                    initializeListView()
                }
                dialogFragment.show(this)
            }
        })

        // items
        items = prefs.getObjectOrNull<Array<ScrobbleData>>(R.string.prefkey_unsent_scrobble_data)
        initializeListView()
    }

    /**
     * Initialize list.
     */
    private fun initializeListView() {
        if (items == null || items!!.isEmpty()) {
            val data = ScrobbleData()
            data.track = getString(R.string.message_unsent_no_data)
            items = arrayOf(data)

            binding.unsentListView.visibility = View.INVISIBLE
            binding.unsentNoDataTextView.visibility = View.VISIBLE
            binding.unsentCheckAllButton.isEnabled = false
            binding.unsentDeleteButton.isEnabled = false
        } else {
            binding.unsentListView.visibility = View.VISIBLE
            binding.unsentNoDataTextView.visibility = View.INVISIBLE
            binding.unsentCheckAllButton.isEnabled = true
            binding.unsentDeleteButton.isEnabled = true
        }

        //adapter = UnsentListAdapter(this, items!!, checkedSet)
        adapter = ListAdapter(items!!)
        binding.unsentListView.adapter = adapter
        adapter.notifyDataSetChanged()
    }



    /**
     * On option item selected.
     * @param item A item
     * @return option item selected.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



//    /**
//     * List adapter
//     */
//    private class UnsentListAdapter(context: Context, itemList: Array<ScrobbleData>, private val checkedSet: TreeSet<Int>) : ArrayAdapter<ScrobbleData>(context, R.layout.layout_unsent_list_item, itemList) {
//
//        override fun isEnabled(position: Int): Boolean {
//            return true
//        }
//
//        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//            val listView = parent as ListView
//            var itemView = convertView
//            val holder: ListItemViewHolder
//            if (itemView == null) {
//                holder =ListItemViewHolder(parent.context)
//                itemView = holder.itemView
//            } else {
//                holder = itemView.tag as ListItemViewHolder
//            }
//
//            val item = getItem(position)
//            val listener : (View) -> Unit = {
//                listView.performItemClick(it, position, getItemId(position))
//            }
//            holder.bind(item, position, listener)
//
//            return itemView
//        }
//
//        /** List item view holder  */
//        private inner class ListItemViewHolder(val context: Context) {
//            val itemView = View.inflate(context, R.layout.layout_unsent_list_item, null)!!
//            init {
//                itemView.tag = this
//            }
//
//            fun bind(item: ScrobbleData, position: Int, listener: (View) -> Unit) {
//                itemView.unsentSelectedCheckBox.setOnTouchListener { _, event ->
//                    itemView.onTouchEvent(event)
//                }
//
//                itemView.setOnClickListener(listener)
//                itemView.unsentSelectedCheckBox.setOnTouchListener { _, event ->
//                    itemView.onTouchEvent(event)
//                }
//
//                // チェック状態更新
//                itemView.unsentSelectedCheckBox.isChecked = checkedSet.contains(position)
//
//                // データ更新
//                val time = item.timestamp
//                if (time > 0) {
//                    if (!item.track.isNullOrEmpty())
//                        itemView.unsentTitleTextView.text = item.track
//                    if (!item.artist.isNullOrEmpty())
//                        itemView.unsentArtistTextView.text = item.artist
//                    if (item.timestamp > 0)
//                        itemView.unsentTimeTextView.text = context.getString(R.string.label_unsent_played_time, DateUtils.formatDateTime(context, java.lang.Long.valueOf(item.timestamp.toLong())!! * 1000, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL))
//                    itemView.isClickable = true
//                } else {
//                    itemView.unsentTitleTextView.text = item.track
//                    itemView.unsentArtistTextView.text = null
//                    itemView.unsentTimeTextView.text = null
//                    itemView.unsentSelectedCheckBox.visibility = View.INVISIBLE
//                    itemView.isClickable = false
//                }
//            }
//        }
//    }


    /**
     * RecycleView Adapter
     * Created by k-kamiya on 2018/02/19.
     */
    inner class ListAdapter(private val itemList: Array<ScrobbleData>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val checkedSet: TreeSet<Int> = TreeSet()


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding: LayoutUnsentListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_unsent_list_item, parent,false)
            val rootView = binding.root
            rootView.tag = binding
            return object : RecyclerView.ViewHolder(rootView) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = getItem(position)
            val binding = holder.itemView.tag as LayoutUnsentListItemBinding
            val context = binding.root.context

            // チェック状態更新
            binding.unsentSelectedCheckBox.isChecked = checkedSet.contains(position)

            binding.unsentSelectedCheckBox.setOnTouchListener { _, event ->
                binding.root.onTouchEvent(event)
            }
            binding.root.setOnClickListener(View.OnClickListener {
                val check = binding.unsentSelectedCheckBox.isChecked
                if (check)
                    checkedSet.add(position)
                else
                    checkedSet.remove(position)
                AppUtils.showToast(binding.root.context, "a")
            })

            // データ更新
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

        fun getItem(position: Int): ScrobbleData {
            return itemList[position]
        }
    }

}
