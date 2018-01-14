package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.dialog.ConfirmDialogFragment
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger
import com.wa2c.android.medoly.plugin.action.lastfm.util.Prefs
import de.umass.lastfm.scrobble.ScrobbleData
import kotlinx.android.synthetic.main.activity_unsent_list.*
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
        unsentNotSaveCheckBox.setOnCheckedChangeListener { _, isChecked ->  prefs.putValue(R.string.prefkey_unsent_scrobble_not_save, isChecked) }
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
                        Logger.e(e)
                        AppUtils.showToast(applicationContext, R.string.message_unsent_delete_failure)
                    }

                    initializeListView()
                }
                dialogFragment.show(this)
            }
        })

        // items
        items = prefs.getObject<Array<ScrobbleData>>(R.string.prefkey_unsent_scrobble_data)
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
            var view = convertView
            val item = getItem(position)
            val holder: ListItemViewHolder

            if (view == null) {
                view = View.inflate(context, R.layout.layout_unsent_list_item, null)
                holder = ListItemViewHolder()
                holder.selectedCheckBox = view.findViewById(R.id.unsentSelectedCheckBox) as CheckBox
                holder.titleTextView = view.findViewById(R.id.unsentTitleTextView) as TextView
                holder.artistTextView = view.findViewById(R.id.unsentArtistTextView) as TextView
                holder.timeTextView = view.findViewById(R.id.unsentTimeTextView) as TextView
                view.tag = holder
            } else {
                holder = view.tag as ListItemViewHolder
            }

            // イベント更新
            view!!.setOnClickListener {
                v -> (parent as ListView).performItemClick(v, getPosition(item), v.id.toLong())
            }
            //val tempView = view
            holder.selectedCheckBox!!.setOnTouchListener { _, event ->
                view!!.onTouchEvent(event)
            }

            // チェック状態更新
            holder.selectedCheckBox!!.isChecked = checkedSet.contains(position)
            // データ更新
            val time = item!!.timestamp
            if (time > 0) {
                if (!item.track.isNullOrEmpty())
                    holder.titleTextView!!.text = item.track
                if (!item.artist.isNullOrEmpty())
                    holder.artistTextView!!.text = item.artist
                if (item.timestamp > 0)
                    holder.timeTextView!!.text = context.getString(R.string.label_unsent_played_time, DateUtils.formatDateTime(context, java.lang.Long.valueOf(item.timestamp.toLong())!! * 1000, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL))
                view.isClickable = true
            } else {
                holder.titleTextView!!.text = item.track
                holder.artistTextView!!.text = null
                holder.timeTextView!!.text = null
                holder.selectedCheckBox!!.visibility = View.INVISIBLE
                view.isClickable = false
            }

            return view
        }

        /** リスト項目のビュー情報を保持するHolder。  */
        internal class ListItemViewHolder {
            var selectedCheckBox: CheckBox? = null
            var titleTextView: TextView? = null
            var artistTextView: TextView? = null
            var timeTextView: TextView? = null
        }
    }


    /**
     * オプションメニュー項目選択時の処理。
     * @param item メニュー項目。
     * @return メニューの表示/非表示。
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
