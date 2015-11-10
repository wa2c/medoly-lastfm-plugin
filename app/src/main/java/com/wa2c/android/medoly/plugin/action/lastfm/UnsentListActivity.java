package com.wa2c.android.medoly.plugin.action.lastfm;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import com.wa2c.android.medoly.plugin.action.lastfm.dialog.ConfirmDialogFragment;
import com.wa2c.android.medoly.utils.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

import de.umass.lastfm.scrobble.ScrobbleData;

public class UnsentListActivity extends Activity {

    /** 設定。 */
    private SharedPreferences preferences;

    /** チェックインデックス。 */
    private TreeSet<Integer> checkedSet;

    /** リスト項目。 */
    private ScrobbleData[] items;

    private UnsentListAdapter adapter;
    private ListView unsentListView;
    private TextView unsentNoDataTextView;
    private CheckBox unsentNotSaveCheckBox;
    private Button unsentCheckAllButton;
    private Button unsentDeleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unsent_list);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        checkedSet = new TreeSet<>();

        // アクションバー
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.title_activity_unsent_list);
        }

        // リスト
        unsentListView = (ListView)findViewById(R.id.unsentListView);
        unsentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (checkedSet.contains(position)) {
                    checkedSet.remove(position);
                } else {
                    checkedSet.add(position);
                }
                adapter.notifyDataSetChanged();
            }
        });

        // データ無し
        unsentNoDataTextView = (TextView)findViewById(R.id.unsentNoDataTextView);

        // 保存しない
        unsentNotSaveCheckBox = (CheckBox)findViewById(R.id.unsentNotSaveCheckBox);
        unsentNotSaveCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(getString(R.string.prefkey_unsent_scrobble_not_save), isChecked).apply();
            }
        });
        unsentNotSaveCheckBox.setChecked(preferences.getBoolean(getString(R.string.prefkey_unsent_scrobble_not_save), false));

        // 全チェック
        unsentCheckAllButton = (Button)findViewById(R.id.unsentCheckAllButton);
        unsentCheckAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checking = false;
                for (int i = 0; i < adapter.getCount(); i++) {
                    checking |= checkedSet.add(i);
                }
                if (!checking) {
                    // 既に全チェック済みの場合は未チェックにする
                    checkedSet.clear();
                }
                adapter.notifyDataSetChanged();
            }
        });

        // 削除
        unsentDeleteButton = (Button)findViewById(R.id.unsentDeleteButton);
        unsentDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkedSet.isEmpty()) {
                    AppUtils.showToast(UnsentListActivity.this, R.string.message_unsent_check_data);
                } else {
                    ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(getString(R.string.message_dialog_unsent_delete_confirm), getString(R.string.title_dialog_unsent_delete_confirm));
                    dialogFragment.setClickListener(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which != DialogInterface.BUTTON_POSITIVE)
                                return;

                            try {
                                // リストから削除
                                Integer[] checks = checkedSet.toArray(new Integer[checkedSet.size()]);
                                ArrayList<ScrobbleData> itemList = new ArrayList<>(Arrays.asList(items));
                                for (int i = checks.length - 1; i >= 0; i--) {
                                    itemList.remove((int) checks[i]);
                                }

                                // 削除した結果を保存
                                ScrobbleData[] dataArray = itemList.toArray(new ScrobbleData[itemList.size()]);
                                if (AppUtils.saveObject(UnsentListActivity.this, getString(R.string.prefkey_unsent_scrobble_data), dataArray)) {
                                    items = dataArray;
                                    checkedSet.clear();
                                } else {
                                    throw new RuntimeException();
                                }
                            } catch (Exception e) {
                                Logger.e(e);
                                AppUtils.showToast(UnsentListActivity.this, R.string.message_unsent_delete_failure);
                            }

                            initializeListView();
                        }
                    });
                    dialogFragment.show(UnsentListActivity.this);
                }

            }
        });

        // 項目
        items = AppUtils.loadObject(this, getString(R.string.prefkey_unsent_scrobble_data), ScrobbleData[].class);
        initializeListView();
    }

    /**
     * リストを初期化する。
     */
    private void initializeListView() {
        if (items == null || items.length == 0) {
            items = new ScrobbleData[1];
            ScrobbleData data = new ScrobbleData();
            data.setTrack("未送信のデータはありません。");
            items[0] = data;

            unsentListView.setVisibility(View.INVISIBLE);
            unsentNoDataTextView.setVisibility(View.VISIBLE);
            unsentCheckAllButton.setEnabled(false);
            unsentDeleteButton.setEnabled(false);
        } else {
            unsentListView.setVisibility(View.VISIBLE);
            unsentNoDataTextView.setVisibility(View.INVISIBLE);
            unsentCheckAllButton.setEnabled(true);
            unsentDeleteButton.setEnabled(true);
        }
        adapter = new UnsentListAdapter(this, items);
        unsentListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }



    /**
     * リストアダプタ。
     */
    private class UnsentListAdapter extends ArrayAdapter<ScrobbleData> {
        /** コンストラクタ。 */
        public UnsentListAdapter(Context context, ScrobbleData[] itemList) {
            super(context, R.layout.layout_unsent_list_item, itemList);
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            final ScrobbleData item = getItem(position);
            final ListItemViewHolder holder;

            if (convertView == null) {
                convertView = View.inflate(getContext(), R.layout.layout_unsent_list_item, null);

                // ビュー参照
                holder = new ListItemViewHolder();
                holder.SelectedCheckBox = (CheckBox)convertView.findViewById(R.id.unsentSelectedCheckBox);
                holder.TitleTextView = (TextView)convertView.findViewById(R.id.unsentTitleTextView);
                holder.ArtistTextView = (TextView)convertView.findViewById(R.id.unsentArtistTextView);
                holder.TimeTextView = (TextView)convertView.findViewById(R.id.unsentTimeTextView);
                convertView.setTag(holder);
            } else {
                holder = (ListItemViewHolder) convertView.getTag();
            }

            // イベント更新
            convertView.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    ((ListView) parent).performItemClick(v, getPosition(item), (long)v.getId());
                }
            });
            final View tempView = convertView;
            holder.SelectedCheckBox.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return tempView.onTouchEvent(event);
                }
            });

            // チェック状態更新
            holder.SelectedCheckBox.setChecked(checkedSet.contains(position));
            // データ更新
            int time  = item.getTimestamp();
            if (time > 0) {
                if (!TextUtils.isEmpty(item.getTrack()))
                    holder.TitleTextView.setText(item.getTrack());
                if (!TextUtils.isEmpty(item.getArtist()))
                    holder.ArtistTextView.setText(item.getArtist());
                if (item.getTimestamp() > 0)
                    holder.TimeTextView.setText(getString(R.string.label_unsent_played_time, DateUtils.formatDateTime(getContext(), Long.valueOf(item.getTimestamp()) * 1000, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_ALL)));
                convertView.setClickable(true);
            } else {
                holder.TitleTextView.setText(item.getTrack());
                holder.ArtistTextView.setText(null);
                holder.TimeTextView.setText(null);
                holder.SelectedCheckBox.setVisibility(View.INVISIBLE);
                convertView.setClickable(false);
            }

            return convertView;
        }

        /** リスト項目のビュー情報を保持するHolder。 */
        class ListItemViewHolder {
            public CheckBox SelectedCheckBox;
            public TextView TitleTextView;
            public TextView ArtistTextView;
            public TextView TimeTextView;
        }

    }



    /**
     * オプションメニュー項目選択時の処理。
     * @param item メニュー項目。
     * @return メニューの表示/非表示。
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
