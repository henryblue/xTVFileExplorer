package com.hb.xtvfileexplorer.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
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
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.hb.xtvfileexplorer.BaseActivity;
import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.loader.DirectoryLoader;
import com.hb.xtvfileexplorer.misc.MimePredicate;
import com.hb.xtvfileexplorer.model.DirectoryResult;
import com.hb.xtvfileexplorer.model.DocumentInfo;
import com.hb.xtvfileexplorer.model.RootInfo;
import com.hb.xtvfileexplorer.provider.AppsProvider;
import com.hb.xtvfileexplorer.ui.CompatTextView;
import com.hb.xtvfileexplorer.ui.GridItemView;
import com.hb.xtvfileexplorer.ui.ItemView;
import com.hb.xtvfileexplorer.ui.ListItemView;
import com.hb.xtvfileexplorer.ui.xListView;
import com.hb.xtvfileexplorer.utils.Utils;

import static com.hb.xtvfileexplorer.provider.AppsProvider.ROOT_ID_PROCESS;
import static com.hb.xtvfileexplorer.provider.StorageProvider.MIME_TYPE_HIDDEN;


public class DirectoryFragment extends Fragment {

	private static final String TAG = "DirectoryFragment";
	public static final String EXTRA_UI_TYPE = "type";

	public static final int MODE_LIST = 1;
	public static final int MODE_GRID = 2;

	private static final int mLoaderId = 32;

	private DocumentsAdapter mAdapter;

	private static RootInfo mRootInfo;
	private static DocumentInfo mDocInfo;
    private LinearLayout mProgressBarLayout;
	private CompatTextView mEmptyView;
	private AbsListView mAbsListView;
	private int mType;

	public static void show(FragmentManager fm, RootInfo root, DocumentInfo doc) {
		mRootInfo = root;
		mDocInfo = doc;
		final Bundle args = new Bundle();
		int type = root.isInternalStorage() ? DirectoryFragment.MODE_GRID : DirectoryFragment.MODE_LIST;
		args.putInt(EXTRA_UI_TYPE, type);

		final FragmentTransaction ft = fm.beginTransaction();
		final DirectoryFragment fragment = new DirectoryFragment();
		fragment.setArguments(args);
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
		final View view = inflater.inflate(R.layout.fragment_directory, container, false);

        mProgressBarLayout = (LinearLayout) view.findViewById(R.id.progressContainer);
		mEmptyView = (CompatTextView) view.findViewById(android.R.id.empty);
		xListView listView = (xListView) view.findViewById(R.id.list);
		GridView gridView = (GridView) view.findViewById(R.id.grid);
		mType = getArguments().getInt(EXTRA_UI_TYPE);
		if (mType == MODE_LIST) {
			listView.setOnItemClickListener(mItemListener);

			// Indent our list divider to align with text
			final Drawable divider = listView.getDivider();
			final boolean insetLeft = res.getBoolean(R.bool.list_divider_inset_left);
			final int insetSize = res.getDimensionPixelSize(R.dimen.list_divider_inset);
			if (insetLeft) {
				listView.setDivider(new InsetDrawable(divider, insetSize, 0, 0, 0));
			} else {
				listView.setDivider(new InsetDrawable(divider, 0, 0, insetSize, 0));
			}
			mAbsListView = listView;
		} else if (mType == MODE_GRID) {
			listView.setVisibility(View.GONE);
			int gridWidth = getResources().getDimensionPixelOffset(R.dimen.grid_item_width);
			gridView.setColumnWidth(gridWidth);
			gridView.setNumColumns(GridView.AUTO_FIT);
			gridView.setOnItemClickListener(mItemListener);
			gridView.setVisibility(View.VISIBLE);
			mAbsListView = gridView;
		}

		return view;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final Context context = getActivity();

		mAdapter = new DocumentsAdapter();
		final Uri contentsUri = DocumentsContract.buildChildDocumentsUri(mDocInfo.authority, mDocInfo.documentId);
		LoaderManager.LoaderCallbacks<DirectoryResult> mCallbacks =
				new LoaderManager.LoaderCallbacks<DirectoryResult>() {
			@Override
			public Loader<DirectoryResult> onCreateLoader(int id, Bundle args) {
				if (mDocInfo.documentId.equals(ROOT_ID_PROCESS)) {
					mProgressBarLayout.setVisibility(View.VISIBLE);
				} else {
					mProgressBarLayout.setVisibility(View.GONE);
				}
				return new DirectoryLoader(context, contentsUri, DirectoryLoader.SORT_ORDER_DISPLAY_NAME);
			}

			@Override
			public void onLoadFinished(Loader<DirectoryResult> loader, DirectoryResult result) {
				mProgressBarLayout.setVisibility(View.GONE);
				if (!isAdded())
					return;
				BaseActivity.mUriCache.put(contentsUri, result);
				mAdapter.swapResult(result);
				mAbsListView.requestFocus();
			}

			@Override
			public void onLoaderReset(Loader<DirectoryResult> loader) {
				mAdapter.swapResult(null);
			}
		};

		mAbsListView.setAdapter(mAdapter);
		DirectoryResult result = BaseActivity.mUriCache.get(contentsUri);
		if (result != null) {
			mAdapter.swapResult(result);
			mAbsListView.requestFocus();
		} else {
			getLoaderManager().restartLoader(mLoaderId, null, mCallbacks);
		}
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

//		// Read-only files are disabled when creating
//		if ((docFlags & DocumentsContract.Document.FLAG_SUPPORTS_WRITE) == 0) {
//			return false;
//		}

		return MimePredicate.mimeMatches("*/*", docMimeType);
	}

	private OnItemClickListener mItemListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Cursor cursor = mAdapter.getItem(position);
            if (cursor != null) {
				if (null != mRootInfo && mRootInfo.isApp()) {  // for app
					final String docId = RootInfo.getCursorString(cursor,
							DocumentsContract.Document.COLUMN_DOCUMENT_ID);
					String packageName = AppsProvider.getPackageForDocId(docId);
					PackageManager pm = getActivity().getPackageManager();
					Intent intent = pm.getLaunchIntentForPackage(packageName);
					if (intent != null) {
						startActivity(intent);
					}
				} else { // for storage
					final String docMimeType = RootInfo.getCursorString(cursor,
							DocumentsContract.Document.COLUMN_MIME_TYPE);
					final int docFlags = RootInfo.getCursorInt(cursor,
							DocumentsContract.Document.COLUMN_FLAGS);
					if (isDocumentEnabled(docMimeType, docFlags)) {
						final DocumentInfo doc = DocumentInfo.fromDirectoryCursor(cursor,
								mRootInfo.getAuthority());
						((BaseActivity) getActivity()).onDocumentPicked(doc);
					}
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
            if (result != null && result.exception != null) {
                mEmptyView.setDrawables(0, R.drawable.ic_dialog_alert, 0, 0);
                mEmptyView.setText(getContext().getString(R.string.query_error));
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                setEmptyState();
                notifyDataSetChanged();
            }
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
                itemView = mType == MODE_GRID ? new GridItemView(context) : new ListItemView(context);
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
			if (mRootInfo.isApp()) {   // for app
				PackageManager pm = context.getPackageManager();
				try {
					PackageInfo info = pm.getPackageInfo(docSummary, 0);
					Drawable drawable = info.applicationInfo.loadIcon(pm);
					if (drawable != null) {
						itemView.setIcon(drawable);
					}
				} catch (PackageManager.NameNotFoundException e) {
					itemView.setIconResource(R.mipmap.ic_launcher);
				}
			} else {  // for storage
				if (mimiType != null && Utils.isDir(mimiType)) {
					if (mType == MODE_GRID) {
						itemView.setIconResource(R.drawable.item_dir);
						itemView.setBackgroundColor();
					} else {
						itemView.setIconResource(R.drawable.ic_doc_folder);
					}
				} else {
					if (mType == MODE_GRID) {
						itemView.setIconResource(R.drawable.item_file);
					} else {
						itemView.setIconResource(R.drawable.ic_doc_text);
					}
				}
			}

			if (!mRootInfo.isAppProcess()) {
				itemView.setSummary(docSummary);
			}

			itemView.setSize(Formatter.formatFileSize(context, docSize));
			return itemView;
		}
	}

}