package com.aknot.simpletimetracker.activity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aknot.simpletimetracker.R;
import com.aknot.simpletimetracker.database.TimerDBAdapter;
import com.aknot.simpletimetracker.dialog.TimerEditDialog;
import com.aknot.simpletimetracker.model.TimerRecord;
import com.aknot.simpletimetracker.utils.DateTimeUtil;

/**
 * @author Aknot
 */
public final class ReportActivity extends Activity {

	private final TimerDBAdapter timerDBAdapter = new TimerDBAdapter(this);

	private final Map<View, Integer> rowToTimerRecordRowIdMap = new HashMap<View, Integer>();

	private int chosenRowId;
	private String dateSelected;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_report);
	}

	@Override
	public void onResume() {
		super.onResume();
		fillInReport();
	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		if (view.getTag().equals("Detail")) {
			menu.add(0, 1, 0, getString(R.string.menu_edit));
			menu.add(0, 2, 0, getString(R.string.menu_del));
			chosenRowId = rowToTimerRecordRowIdMap.get(view);
		} else if (view.getTag().equals("Header")) {
			menu.add(0, 3, 0, getString(R.string.menu_add));
			menu.add(0, 4, 0, getString(R.string.menu_del));
			dateSelected = (String) ((TextView) view).getText();
		}
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			showTimerDialog(chosenRowId, 0);
			return true;
		case 2:
			buildDeleteRowDialog();
			return true;
		case 3:
			showTimerDialog(TimerRecord.NEW_TIMER, DateTimeUtil.geLongFromString(dateSelected));
			return true;
		case 4:
			buildDeleteRangeDialog();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	public void saveTimer(final TimerRecord timerToSave) {
		if (timerToSave.getRowId() == TimerRecord.NEW_TIMER) {
			timerDBAdapter.createTimer(timerToSave);
		} else {
			timerDBAdapter.updateTimer(timerToSave);
		}
		fillInReport();
	}

	private void fillInReport() {
		final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayoutReport);

		linearLayout.removeAllViews();

		final List<TimerRecord> timerRecords = timerDBAdapter.fetchTimerRecordsByDateRange(DateTimeUtil.getMinTimeMillisWeek(),
				DateTimeUtil.getMaxTimeMillisToday());

		String lastStartDate = "";
		for (final TimerRecord timerRecord : timerRecords) {
			if (!lastStartDate.equals(timerRecord.getStartDateStr())) {
				lastStartDate = timerRecord.getStartDateStr();
				linearLayout.addView(getDateHeaderView(lastStartDate));
			}
			linearLayout.addView(getTimerView(timerRecord));
		}
	}

	private TextView getDateHeaderView(final String dateText) {
		final TextView tvHeader = new TextView(this);
		tvHeader.setText(dateText);
		tvHeader.setTextColor(Color.GREEN);
		tvHeader.setTag("Header");

		registerForContextMenu(tvHeader);

		return tvHeader;
	}

	private TextView getTimerView(final TimerRecord timerRecord) {
		final TextView tvTimeRecord = new TextView(this);
		final StringBuilder reportText = new StringBuilder();
		reportText.append("  ").append(timerRecord.getTitleWithDuration());
		tvTimeRecord.setText(reportText.toString(), TextView.BufferType.SPANNABLE);
		tvTimeRecord.setTag("Detail");

		rowToTimerRecordRowIdMap.put(tvTimeRecord, timerRecord.getRowId());

		registerForContextMenu(tvTimeRecord);

		return tvTimeRecord;
	}

	private void showTimerDialog(final int rowId, final long date) {
		final TimerEditDialog timerEditDialog = new TimerEditDialog(this);
		timerEditDialog.buildEditDialog(rowId, date, this).show();
	}

	private void buildDeleteRowDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		final TimerRecord timerRecord = timerDBAdapter.fetchByRowID(chosenRowId);

		builder.setTitle(timerRecord.getTitle());

		builder.setMessage("Are you sure to delete this timer record?").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int id) {
				timerDBAdapter.delete(chosenRowId);
				fillInReport();
			}
		}).setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int id) {
				dialog.cancel();
			}
		});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	private void buildDeleteRangeDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(dateSelected);

		builder.setMessage("Are you sure to delete all timer record for this date?").setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int id) {
						final Calendar currentDate = DateTimeUtil.getCalendarFromString(dateSelected);

						final long startDate = DateTimeUtil.getMinTimeMillisToday(currentDate);
						final long endDate = DateTimeUtil.getMaxTimeMillisToday(currentDate);

						timerDBAdapter.deleteForDateRange(startDate, endDate);

						fillInReport();
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int id) {
						dialog.cancel();
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
	}
}