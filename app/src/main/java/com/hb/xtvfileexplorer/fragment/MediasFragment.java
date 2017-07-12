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
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.loader.DirectoryLoader;
import com.hb.xtvfileexplorer.model.DirectoryResult;
import com.hb.xtvfileexplorer.model.DocumentInfo;
import com.hb.xtvfileexplorer.model.RootInfo;
import com.hb.xtvfileexplorer.ui.CompatTextView;
import com.hb.xtvfileexplorer.utils.Utils;


public class MediasFragment extends Fragment {

	private static RootInfo mRootInfo;
	private static DocumentInfo mDocInfo;

	private LoaderManager.LoaderCallbacks<DirectoryResult> mCallbacks;

	private CompatTextView mEmptyView;
	private DocumentsAdapter mAdapter;

	public static void show(FragmentManager fm, RootInfo root, DocumentInfo doc) {
		mRootInfo = root;
		mDocInfo = doc;

		final FragmentTransaction ft = fm.beginTransaction();
		final MediasFragment fragment = new MediasFragment();
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
		final View view = inflater.inflate(R.layout.fragment_media, container, false);
		mEmptyView = (CompatTextView) view.findViewById(android.R.id.empty);
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

				Uri contentsUri = DocumentsContract.buildChildDocumentsUri(mDocInfo.authority, mDocInfo.documentId);
				return new DirectoryLoader(context, contentsUri);
			}

			@Override
			public void onLoadFinished(Loader<DirectoryResult> loader, DirectoryResult result) {
				if (!isAdded())
					return;
				mAdapter.swapResult(result);
			}

			@Override
			public void onLoaderReset(Loader<DirectoryResult> loader) {
				mAdapter.swapResult(null);
			}
		};
		if (!mRootInfo.isManuGen) {
			//mListView.setAdapter(mAdapter);
			getLoaderManager().restartLoader(40, null, mCallbacks);
		} else {
			setEmptyState();
		}
	}

    private void setEmptyState() {
        if (mAdapter.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

	private class DocumentsAdapter extends BaseAdapter implements View.OnClickListener {
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
				int layoutId = R.layout.item_doc_list;
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