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

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.model.DirectoryResult;
import com.hb.xtvfileexplorer.model.DocumentInfo;
import com.hb.xtvfileexplorer.model.RootInfo;

import java.util.ArrayList;


/**
 * Display the documents inside a single directory.
 */
public class DirectoryFragment extends ListFragment {

	public static final String EXTRA_ROOT = "root";
	public static final String EXTRA_DOC = "doc";
	public static final String EXTRA_QUERY = "query";

	private final int mLoaderId = 42;

	private CompatTextView mEmptyView;
	private ListView mListView;

	private AbsListView mCurrentView;

	private DocumentsAdapter mAdapter;
	private LoaderManager.LoaderCallbacks<DirectoryResult> mCallbacks;

	private static RootInfo mRootInfo;
	private static DocumentInfo mDocInfo;
	private boolean isApp;
    private int mDefaultColor;
    private ProgressBar mProgressBar;
    private boolean isOperationSupported;

	private static void show(FragmentManager fm, RootInfo root, DocumentInfo doc, String query) {
//		final Bundle args = new Bundle();
//		args.putParcelable(EXTRA_ROOT, root);
//		args.putParcelable(EXTRA_DOC, doc);
//		args.putString(EXTRA_QUERY, query);
		mRootInfo = root;
		mDocInfo = doc;

		final FragmentTransaction ft = fm.beginTransaction();
		final DirectoryFragment fragment = new DirectoryFragment();
		ft.replace(R.id.container_directory, fragment);
		ft.commitAllowingStateLoss();
	}

	public static Fragment get(FragmentManager fm) {
		// TODO: deal with multiple directories shown at once
		return fm.findFragmentById(R.id.container_directory);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final Context context = inflater.getContext();
        final Resources res = context.getResources();
		final View view = inflater.inflate(R.layout.fragment_directory, container, false);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

		mEmptyView = (CompatTextView)view.findViewById(android.R.id.empty);

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
	public void onDestroyView() {
		super.onDestroyView();

		final ViewGroup target =  mListView;
		final int count = target.getChildCount();
		for (int i = 0; i < count; i++) {
			final View view = target.getChildAt(i);
//			mRecycleListener.onMovedToScrapHeap(view);
		}

		// Tear down any selection in progress
		mListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final Context context = getActivity();


		isApp = mRootInfo != null && mRootInfo.isApp();

		mAdapter = new DocumentsAdapter();


		mCallbacks = new LoaderManager.LoaderCallbacks<DirectoryResult>() {
			@Override
			public Loader<DirectoryResult> onCreateLoader(int id, Bundle args) {
				final String query = getArguments().getString(EXTRA_QUERY);

				Uri contentsUri;
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
		setListAdapter(mAdapter);
		setListShown(false);
		getLoaderManager().restartLoader(mLoaderId, null, mCallbacks);
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onResume() {
		super.onResume();
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

		private ArrayList<Footer> mFooters = new ArrayList<>();

		public void swapResult(DirectoryResult result) {
			mCursor = result != null ? result.cursor : null;
			mCursorCount = mCursor != null ? mCursor.getCount() : 0;

			notifyDataSetChanged();
		}

		@Override
		public void onClick(View v) {

		}

		@Override
		public int getCount() {
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return null;
		}
	}

}