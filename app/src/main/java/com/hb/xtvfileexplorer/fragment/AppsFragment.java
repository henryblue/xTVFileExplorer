package com.hb.xtvfileexplorer.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.loader.DirectoryLoader;
import com.hb.xtvfileexplorer.model.DirectoryResult;
import com.hb.xtvfileexplorer.model.DocumentInfo;
import com.hb.xtvfileexplorer.model.RootInfo;
import com.hb.xtvfileexplorer.utils.Utils;

import static com.hb.xtvfileexplorer.provider.AppsProvider.ROOT_ID_PROCESS;


public class AppsFragment extends Fragment {

	private static final int mLoaderId = 42;

	private ListView mListView;

	private DocumentsAdapter mAdapter;
	private LoaderManager.LoaderCallbacks<DirectoryResult> mCallbacks;

	private static RootInfo mRootInfo;
	private static DocumentInfo mDocInfo;
    private LinearLayout mProgressBarLayout;
	private TextView mEmptyView;

	public static void show(FragmentManager fm, RootInfo root, DocumentInfo doc) {
		mRootInfo = root;
		mDocInfo = doc;

		final FragmentTransaction ft = fm.beginTransaction();
		final AppsFragment fragment = new AppsFragment();
		ft.replace(R.id.container_directory, fragment);
		ft.commitAllowingStateLoss();
	}

	public static Fragment get(FragmentManager fm) {
		return fm.findFragmentById(R.id.container_directory);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final Context context = inflater.getContext();
        final Resources res = context.getResources();
		final View view = inflater.inflate(R.layout.fragment_app, container, false);

        mProgressBarLayout = (LinearLayout) view.findViewById(R.id.progressContainer);
		mEmptyView = (TextView) view.findViewById(R.id.internalEmpty);

		mListView = (ListView) view.findViewById(R.id.list);
		mListView.setOnItemClickListener(mItemListener);

        // Indent our list divider to align with text
        final Drawable divider = mListView.getDivider();
        final boolean insetLeft = res.getBoolean(R.bool.list_divider_inset_left);
        final int insetSize = res.getDimensionPixelSize(R.dimen.list_divider_inset);
        if (insetLeft) {
            mListView.setDivider(new InsetDrawable(divider, insetSize, 0, 0, 0));
        } else {
            mListView.setDivider(new InsetDrawable(divider, 0, 0, insetSize, 0));
        }

		return view;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final Context context = getActivity();

		mAdapter = new DocumentsAdapter();


		mCallbacks = new LoaderManager.LoaderCallbacks<DirectoryResult>() {
			@Override
			public Loader<DirectoryResult> onCreateLoader(int id, Bundle args) {
				if (mDocInfo.documentId.equals(ROOT_ID_PROCESS)) {
					mProgressBarLayout.setVisibility(View.VISIBLE);
				} else {
					mProgressBarLayout.setVisibility(View.GONE);
				}
				Uri contentsUri = DocumentsContract.buildChildDocumentsUri(mDocInfo.authority, mDocInfo.documentId);
				return new DirectoryLoader(context, contentsUri);
			}

			@Override
			public void onLoadFinished(Loader<DirectoryResult> loader, DirectoryResult result) {
				mProgressBarLayout.setVisibility(View.GONE);
				if (!isAdded())
					return;
				mAdapter.swapResult(result);

			}

			@Override
			public void onLoaderReset(Loader<DirectoryResult> loader) {
				mAdapter.swapResult(null);
			}
		};
		mListView.setAdapter(mAdapter);
		getLoaderManager().restartLoader(mLoaderId, null, mCallbacks);
	}

	private void setEmptyState() {
		if (mAdapter.isEmpty()) {
			mEmptyView.setVisibility(View.VISIBLE);
			mEmptyView.setText(R.string.app_no_data);
		} else {
			mEmptyView.setVisibility(View.GONE);
		}
	}

	private OnItemClickListener mItemListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//			final Cursor cursor = mAdapter.getItem(position);

		}
	};

	private class DocumentsAdapter extends BaseAdapter implements OnClickListener {
		private Cursor mCursor;
		private int mCursorCount;

		void swapResult(DirectoryResult result) {
			mCursor = result != null ? result.cursor : null;
			mCursorCount = mCursor != null ? mCursor.getCount() : 0;
			setEmptyState();
			notifyDataSetChanged();
		}

		@Override
		public void onClick(View v) {

		}

		@Override
		public int getCount() {
			return mCursorCount;
		}

		@Override
		public Cursor getItem(int position) {
			if (position < mCursorCount) {
				mCursor.moveToPosition(position);
				return mCursor;
			} else {
				return null;
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getDocumentView(position, convertView, parent);
		}

		private View getDocumentView(int position, View convertView, ViewGroup parent) {
			final Context context = parent.getContext();
			if (convertView == null) {
				final LayoutInflater inflater = LayoutInflater.from(context);
				int layoutId = mRootInfo.isAppProcess() ? R.layout.item_doc_process_list : R.layout.item_doc_app_list;
				convertView = inflater.inflate(layoutId, parent, false);
			}

			final Cursor cursor = getItem(position);
			final String docDisplayName = RootInfo.getCursorString(cursor, DocumentsContract.Document.COLUMN_DISPLAY_NAME);
			final long docLastModified = RootInfo.getCursorLong(cursor, DocumentsContract.Document.COLUMN_LAST_MODIFIED);
			final String docSummary = RootInfo.getCursorString(cursor, DocumentsContract.Document.COLUMN_SUMMARY);
			final long docSize = RootInfo.getCursorLong(cursor, DocumentsContract.Document.COLUMN_SIZE);

			final TextView title = (TextView) convertView.findViewById(R.id.title);
			final ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
			final TextView date = (TextView) convertView.findViewById(R.id.date);
			final TextView summary = (TextView) convertView.findViewById(R.id.summary);
			final TextView size = (TextView) convertView.findViewById(R.id.size);

			PackageManager pm = context.getPackageManager();
			try {
				PackageInfo info = pm.getPackageInfo(docSummary, 0);
				Drawable drawable = info.applicationInfo.loadIcon(pm);
				if (drawable != null) {
					icon.setImageDrawable(drawable);
				}
			} catch (PackageManager.NameNotFoundException e) {
				icon.setImageResource(R.mipmap.ic_launcher);
			}
			title.setText(docDisplayName);
			date.setText(Utils.formatTime(context, docLastModified));
			if (summary != null) {
				summary.setText(docSummary);
			}
			size.setText(Formatter.formatFileSize(context, docSize));

			return convertView;
		}
	}

}