package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.dialog.ConfirmDialogFragment
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.prefs.Prefs
import de.umass.lastfm.scrobble.ScrobbleData
import kotlinx.android.synthetic.main.activity_unsent_list.*
import kotlinx.android.synthetic.main.layout_unsent_list_item.view.*
import timber.log.Timber
import java.util.*


/**
 * Unsent list activity.
 */
class UnsentListActivity : Activity() {

    /** Preferences controller.  */
    private lateinit var prefs: Prefs

    /** Check index set.。  */
    private val checkedSet: TreeSet<Int> = TreeSet()

    /** List items.  */
    private var items: Array<ScrobbleData>? = null

    private var adapter: UnsentListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unsent_list)
        prefs = Prefs(this)

        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setTitle(R.string.title_activity_unsent_list)

        // list
        unsentListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (checkedSet.contains(position)) {
                checkedSet.remove(position)
            } else {
                checkedSet.add(position)
            }
            adapter!!.notifyDataSetChanged()
        }

        // not save
        unsentNotSaveCheckBox.setOnCheckedChangeListener { _, isChecked ->  prefs.putBoolean(R.string.prefkey_unsent_scrobble_not_save, isChecked) }
        unsentNotSaveCheckBox.isChecked = prefs.getBoolean(R.string.prefkey_unsent_scrobble_not_save)

        // check all
        unsentCheckAllButton!!.setOnClickListener {
            var checking = false
            for (i in 0 until adapter!!.count) {
                checking = checking or checkedSet.add(i)
            }
            if (!checking) {
                // Uncheck if checked
                checkedSet.clear()
            }
            adapter!!.notifyDataSetChanged()
        }

        // delete
        unsentDeleteButton.setOnClickListener(View.OnClickListener {
            if (checkedSet.isEmpty()) {
                AppUtils.showToast(applicationContext, R.string.message_unsent_check_data)
            } else {
                val dialogFragment = ConfirmDialogFragment.newInstance(getString(R.string.message_dialog_unsent_delete_confirm), getString(R.string.title_dialog_unsent_delete_confirm))
                dialogFragment.clickListener = DialogInterface.OnClickListener { _, which ->
                    if (which != DialogInterface.BUTTON_POSITIVE)
                        return@OnClickListener

                    try {
                        // delete from list
                        val checks = checkedSet.toTypedArray<Int>()
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

            unsentListView.visibility = View.INVISIBLE
            unsentNoDataTextView.visibility = View.VISIBLE
            unsentCheckAllButton.isEnabled = false
            unsentDeleteButton.isEnabled = false
        } else {
            unsentListView.visibility = View.VISIBLE
            unsentNoDataTextView.visibility = View.INVISIBLE
            unsentCheckAllButton.isEnabled = true
            unsentDeleteButton.isEnabled = true
        }

        adapter = UnsentListAdapter(this, items!!, checkedSet)
        unsentListView.adapter = adapter
        adapter!!.notifyDataSetChanged()
    }


    /**
     * List adapter
     */
    private class UnsentListAdapter(context: Context, itemList: Array<ScrobbleData>, private val checkedSet: TreeSet<Int>) : ArrayAdapter<ScrobbleData>(context, R.layout.layout_unsent_list_item, itemList) {

        override fun isEnabled(position: Int): Boolean {
            return true
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val listView = parent as ListView
            var itemView = convertView
            val holder: ListItemViewHolder
            if (itemView == null) {
                holder =ListItemViewHolder(parent.context)
                itemView = holder.itemView
            } else {
                holder = itemView.tag as ListItemViewHolder
            }

            val item = getItem(position)
            val listener : (View) -> Unit = {
                listView.performItemClick(it, position, getItemId(position))
            }
            holder.bind(item, position, listener)

            return itemView
        }

        /** List item view holder  */
        private inner class ListItemViewHolder(val context: Context) {
            val itemView = View.inflate(context, R.layout.layout_unsent_list_item, null)!!
            init {
                itemView.tag = this
            }

            fun bind(item: ScrobbleData, position: Int, listener: (View) -> Unit) {
                itemView.unsentSelectedCheckBox.setOnTouchListener { _, event ->
                    itemView.onTouchEvent(event)
                }

                itemView.setOnClickListener(listener)
                itemView.unsentSelectedCheckBox.setOnTouchListener { _, event ->
                    itemView.onTouchEvent(event)
                }

                // チェック状態更新
                itemView.unsentSelectedCheckBox.isChecked = checkedSet.contains(position)

                // データ更新
                val time = item.timestamp
                if (time > 0) {
                    if (!item.track.isNullOrEmpty())
                        itemView.unsentTitleTextView.text = item.track
                    if (!item.artist.isNullOrEmpty())
                        itemView.unsentArtistTextView.text = item.artist
                    if (item.timestamp > 0)
                        itemView.unsentTimeTextView.text = context.getString(R.string.label_unsent_played_time, DateUtils.formatDateTime(context, java.lang.Long.valueOf(item.timestamp.toLong())!! * 1000, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL))
                    itemView.isClickable = true
                } else {
                    itemView.unsentTitleTextView.text = item.track
                    itemView.unsentArtistTextView.text = null
                    itemView.unsentTimeTextView.text = null
                    itemView.unsentSelectedCheckBox.visibility = View.INVISIBLE
                    itemView.isClickable = false
                }
            }
        }
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

}
