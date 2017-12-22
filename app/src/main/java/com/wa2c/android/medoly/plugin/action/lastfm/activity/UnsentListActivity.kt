package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.dialog.ConfirmDialogFragment
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger
import de.umass.lastfm.scrobble.ScrobbleData
import java.util.*


class UnsentListActivity : Activity() {

    /** A preference.  */
    private lateinit var preferences: SharedPreferences

    /** Check index set.。  */
    private val checkedSet: TreeSet<Int> = TreeSet()

    /** List items.  */
    private var items: Array<ScrobbleData>? = null

    private var adapter: UnsentListAdapter? = null
    private var unsentListView: ListView? = null
    private var unsentNoDataTextView: TextView? = null
    private var unsentNotSaveCheckBox: CheckBox? = null
    private var unsentCheckAllButton: Button? = null
    private var unsentDeleteButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unsent_list)
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)



        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setTitle(R.string.title_activity_unsent_list)

        // リスト
        unsentListView = findViewById(R.id.unsentListView) as ListView
        unsentListView!!.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (checkedSet.contains(position)) {
                checkedSet.remove(position)
            } else {
                checkedSet.add(position)
            }
            adapter!!.notifyDataSetChanged()
        }

        // データ無し
        unsentNoDataTextView = findViewById(R.id.unsentNoDataTextView) as TextView

        // 保存しない
        unsentNotSaveCheckBox = findViewById(R.id.unsentNotSaveCheckBox) as CheckBox
        unsentNotSaveCheckBox!!.setOnCheckedChangeListener { _, isChecked -> preferences.edit().putBoolean(getString(R.string.prefkey_unsent_scrobble_not_save), isChecked).apply() }
        unsentNotSaveCheckBox!!.isChecked = preferences.getBoolean(getString(R.string.prefkey_unsent_scrobble_not_save), false)

        // 全チェック
        unsentCheckAllButton = findViewById(R.id.unsentCheckAllButton) as Button
        unsentCheckAllButton!!.setOnClickListener {
            var checking = false
            for (i in 0 until adapter!!.count) {
                checking = checking or checkedSet.add(i)
            }
            if (!checking) {
                // 既に全チェック済みの場合は未チェックにする
                checkedSet.clear()
            }
            adapter!!.notifyDataSetChanged()
        }

        // 削除
        unsentDeleteButton = findViewById(R.id.unsentDeleteButton) as Button
        unsentDeleteButton!!.setOnClickListener(View.OnClickListener {
            if (checkedSet.isEmpty()) {
                AppUtils.showToast(applicationContext, R.string.message_unsent_check_data)
            } else {
                val dialogFragment = ConfirmDialogFragment.newInstance(getString(R.string.message_dialog_unsent_delete_confirm), getString(R.string.title_dialog_unsent_delete_confirm))
                dialogFragment.clickListener = DialogInterface.OnClickListener { _, which ->
                    if (which != DialogInterface.BUTTON_POSITIVE)
                        return@OnClickListener

                    try {
                        // リストから削除
                        val checks = checkedSet.toTypedArray<Int>()
                        val itemList = ArrayList(Arrays.asList(*items!!))
                        for (i in checks.indices.reversed()) {
                            itemList.removeAt(checks[i])
                        }

                        // 削除した結果を保存
                        val dataArray = itemList.toTypedArray<ScrobbleData>()
                        if (AppUtils.saveObject(applicationContext, getString(R.string.prefkey_unsent_scrobble_data), dataArray)) {
                            items = dataArray
                            checkedSet.clear()
                        } else {
                            throw RuntimeException()
                        }
                    } catch (e: Exception) {
                        Logger.e(e)
                        AppUtils.showToast(applicationContext, R.string.message_unsent_delete_failure)
                    }

                    initializeListView()
                }
                dialogFragment.show(this)
            }
        })

        // 項目
        items = AppUtils.loadObject<Array<ScrobbleData>>(applicationContext, getString(R.string.prefkey_unsent_scrobble_data))
        initializeListView()
    }

    /**
     * リストを初期化する。
     */
    private fun initializeListView() {

        if (items == null || items!!.isEmpty()) {
            val data = ScrobbleData()
            data.track = getString(R.string.message_unsent_no_data)
            items = arrayOf(data)

            unsentListView!!.visibility = View.INVISIBLE
            unsentNoDataTextView!!.visibility = View.VISIBLE
            unsentCheckAllButton!!.isEnabled = false
            unsentDeleteButton!!.isEnabled = false
        } else {
            unsentListView!!.visibility = View.VISIBLE
            unsentNoDataTextView!!.visibility = View.INVISIBLE
            unsentCheckAllButton!!.isEnabled = true
            unsentDeleteButton!!.isEnabled = true
        }

        adapter = UnsentListAdapter(this, items!!, checkedSet)
        unsentListView!!.adapter = adapter
        adapter!!.notifyDataSetChanged()
    }


    /**
     * リストアダプタ。
     */
    private class UnsentListAdapter
    /** コンストラクタ。  */
    (context: Context, itemList: Array<ScrobbleData>, private val checkedSet: TreeSet<Int>) : ArrayAdapter<ScrobbleData>(context, R.layout.layout_unsent_list_item, itemList) {

        override fun isEnabled(position: Int): Boolean {
            return true
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            val item = getItem(position)
            val holder: ListItemViewHolder

            if (view == null) {
                view = View.inflate(context, R.layout.layout_unsent_list_item, null)
                // ビュー参照
                holder = ListItemViewHolder()

                holder.SelectedCheckBox = view!!.findViewById(R.id.unsentSelectedCheckBox) as CheckBox
                holder.TitleTextView = view.findViewById(R.id.unsentTitleTextView) as TextView
                holder.ArtistTextView = view.findViewById(R.id.unsentArtistTextView) as TextView
                holder.TimeTextView = view.findViewById(R.id.unsentTimeTextView) as TextView
                view.tag = holder
            } else {
                holder = view.tag as ListItemViewHolder
            }

            // イベント更新
            view.setOnClickListener { v -> (parent as ListView).performItemClick(v, getPosition(item), v.id.toLong()) }
            val tempView = view
            holder.SelectedCheckBox!!.setOnTouchListener { _, event -> tempView.onTouchEvent(event) }

            // チェック状態更新
            holder.SelectedCheckBox!!.isChecked = checkedSet.contains(position)
            // データ更新
            val time = item!!.timestamp
            if (time > 0) {
                if (!TextUtils.isEmpty(item.track))
                    holder.TitleTextView!!.text = item.track
                if (!TextUtils.isEmpty(item.artist))
                    holder.ArtistTextView!!.text = item.artist
                if (item.timestamp > 0)
                    holder.TimeTextView!!.text = context.getString(R.string.label_unsent_played_time, DateUtils.formatDateTime(context, java.lang.Long.valueOf(item.timestamp.toLong())!! * 1000, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL))
                view.isClickable = true
            } else {
                holder.TitleTextView!!.text = item.track
                holder.ArtistTextView!!.text = null
                holder.TimeTextView!!.text = null
                holder.SelectedCheckBox!!.visibility = View.INVISIBLE
                view.isClickable = false
            }

            return view
        }

        /** リスト項目のビュー情報を保持するHolder。  */
        internal class ListItemViewHolder {
            var SelectedCheckBox: CheckBox? = null
            var TitleTextView: TextView? = null
            var ArtistTextView: TextView? = null
            var TimeTextView: TextView? = null
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
