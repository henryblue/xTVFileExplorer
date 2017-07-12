package com.hb.xtvfileexplorer.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.hb.xtvfileexplorer.BaseActivity;
import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.loader.DirectoryLoader;
import com.hb.xtvfileexplorer.model.DirectoryResult;
import com.hb.xtvfileexplorer.model.DocumentInfo;
import com.hb.xtvfileexplorer.model.RootInfo;
import com.hb.xtvfileexplorer.ui.CompatTextView;
import com.hb.xtvfileexplorer.ui.GridItemView;
import com.hb.xtvfileexplorer.ui.ItemView;
import com.hb.xtvfileexplorer.ui.ListItemView;
import com.hb.xtvfileexplorer.ui.xListView;
import com.hb.xtvfileexplorer.utils.Utils;

import static com.hb.xtvfileexplorer.provider.StorageProvider.MIME_TYPE_HIDDEN;


public class StorageFragment extends Fragment {

	private static final int mLoaderId = 32;
	private static final String TAG = "StorageFragment";

	private xListView mListView;

	private DocumentsAdapter mAdapter;

	private static RootInfo mRootInfo;
	private static DocumentInfo mDocInfo;
    private LinearLayout mProgressBarLayout;
	private CompatTextView mEmptyView;
	private GridView mGridView;
	private boolean mIsInternalStorage;

	public static void show(FragmentManager fm, RootInfo root, DocumentInfo doc) {
		mRootInfo = root;
		mDocInfo = doc;

		final FragmentTransaction ft = fm.beginTransaction();
		final StorageFragment fragment = new StorageFragment();
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
		final View view = inflater.inflate(R.layout.fragment_storage, container, false);

        mProgressBarLayout = (LinearLayout) view.findViewById(R.id.progressContainer);
		mEmptyView = (CompatTextView) view.findViewById(android.R.id.empty);
		mIsInternalStorage = mRootInfo.isInternalStorage();
		mListView = (xListView) view.findViewById(R.id.list);
		mGridView = (GridView) view.findViewById(R.id.grid);

		if (!mIsInternalStorage) {
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
		} else {
			mListView.setVisibility(View.GONE);
			int gridWidth = getResources().getDimensionPixelOffset(R.dimen.grid_item_width);
			mGridView.setColumnWidth(gridWidth);
			mGridView.setNumColumns(GridView.AUTO_FIT);
			mGridView.setOnItemClickListener(mItemListener);
			mGridView.setVisibility(View.VISIBLE);
		}

		return view;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final Context context = getActivity();

		mAdapter = new DocumentsAdapter();

		LoaderManager.LoaderCallbacks<DirectoryResult> mCallbacks =
				new LoaderManager.LoaderCallbacks<DirectoryResult>() {
			@Override
			public Loader<DirectoryResult> onCreateLoader(int id, Bundle args) {
				Uri contentsUri = DocumentsContract.buildChildDocumentsUri(mDocInfo.authority, mDocInfo.documentId);
				Log.i(TAG, "onCreateLoader: ===contentsUri==" + contentsUri);
				return new DirectoryLoader(context, contentsUri);
			}

			@Override
			public void onLoadFinished(Loader<DirectoryResult> loader, DirectoryResult result) {
				if (!isAdded())
					return;
				Log.i(TAG, "onLoadFinished: ==result==" + result.cursor);
				mAdapter.swapResult(result);
			}

			@Override
			public void onLoaderReset(Loader<DirectoryResult> loader) {
				mAdapter.swapResult(null);
			}
		};

		if (mIsInternalStorage) {
			mGridView.setAdapter(mAdapter);
		} else {
			mListView.setAdapter(mAdapter);
		}
		getLoaderManager().restartLoader(mLoaderId, null, mCallbacks);
	}

	private void setEmptyState() {
		if (mAdapter.isEmpty()) {
			mEmptyView.setVisibility(View.VISIBLE);
		} else {
			mEmptyView.setVisibility(View.GONE);
		}
	}

	private boolean isDocumentEnabled(String docMimeType, int docFlags) {
		if (MIME_TYPE_HIDDEN.equals(docMimeType)) {
			return false;
		}
		// Directories are always enabled
		if (Utils.isDir(docMimeType)) {
			return true;
		}

		// Read-only files are disabled when creating
		if ((docFlags & DocumentsContract.Document.FLAG_SUPPORTS_WRITE) == 0) {
			return false;
		}

		return false;
	}

	private OnItemClickListener mItemListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Cursor cursor = mAdapter.getItem(position);
            if (cursor != null) {
				final String docMimeType = RootInfo.getCursorString(cursor, DocumentsContract.Document.COLUMN_MIME_TYPE);
				final int docFlags = RootInfo.getCursorInt(cursor, DocumentsContract.Document.COLUMN_FLAGS);
				if (isDocumentEnabled(docMimeType, docFlags)) {
					final DocumentInfo doc = DocumentInfo.fromDirectoryCursor(cursor);
					((BaseActivity) getActivity()).onDocumentPicked(doc);
				}
            }
		}
	};

	private class DocumentsAdapter extends BaseAdapter {
		private Cursor mCursor;
		private int mCursorCount;

		void swapResult(DirectoryResult result) {
			mCursor = result != null ? result.cursor : null;
			mCursorCount = mCursor != null ? mCursor.getCount() : 0;
			setEmptyState();
			notifyDataSetChanged();
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
            ItemView itemView;
			if (convertView == null) {
                itemView = mIsInternalStorage ? new GridItemView(context) : new ListItemView(context);
			} else {
                itemView = (ItemView) convertView;
            }

			final Cursor cursor = getItem(position);
			final String docDisplayName = RootInfo.getCursorString(cursor, DocumentsContract.Document.COLUMN_DISPLAY_NAME);
			final long docLastModified = RootInfo.getCursorLong(cursor, DocumentsContract.Document.COLUMN_LAST_MODIFIED);
			final String docSummary = RootInfo.getCursorString(cursor, DocumentsContract.Document.COLUMN_SUMMARY);
			final long docSize = RootInfo.getCursorLong(cursor, DocumentsContract.Document.COLUMN_SIZE);
			final String mimiType = RootInfo.getCursorString(cursor, DocumentsContract.Document.COLUMN_MIME_TYPE);

			itemView.setTitle(docDisplayName);
			itemView.setDate(Utils.formatTime(context, docLastModified));
			if (mIsInternalStorage) {
				if (mimiType != null && Utils.isDir(mimiType)) {
					itemView.setIconResource(R.drawable.item_dir);
					itemView.setBackgroundColor();
				} else {
					itemView.setIconResource(R.drawable.item_file);
				}
			} else {
				itemView.setSummary(docSummary);
			}
			itemView.setSize(Formatter.formatFileSize(context, docSize));
			return itemView;
		}
	}

}