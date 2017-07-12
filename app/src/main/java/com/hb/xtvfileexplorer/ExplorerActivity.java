package com.hb.xtvfileexplorer;


import android.app.FragmentManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.hb.xtvfileexplorer.archive.DocumentArchiveHelper;
import com.hb.xtvfileexplorer.fragment.AppsFragment;
import com.hb.xtvfileexplorer.fragment.MediasFragment;
import com.hb.xtvfileexplorer.fragment.RootsFragment;
import com.hb.xtvfileexplorer.fragment.StorageFragment;
import com.hb.xtvfileexplorer.model.DocumentInfo;
import com.hb.xtvfileexplorer.model.DocumentStack;
import com.hb.xtvfileexplorer.model.RootInfo;
import com.hb.xtvfileexplorer.utils.Utils;

import java.io.FileNotFoundException;

import static com.hb.xtvfileexplorer.fragment.RootsFragment.TAG;

public class ExplorerActivity extends BaseActivity {

    private Toolbar mToolbar;
    DocumentStack mDocStack;
    private RootInfo mParentRoot;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDocStack = new DocumentStack();
        setContentView(R.layout.activity_explorer);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        RootsFragment.show(getFragmentManager());
    }

    @Override
    public RootInfo getCurrentRoot() {
        return mDocStack.root;
    }

    private DocumentInfo getCurrentDirectory() {
        return mDocStack.peek();
    }

    public void onRootPicked(RootInfo root, RootInfo parentRoot) {
        mParentRoot = parentRoot;
        onRootPicked(root, true);
    }

    @Override
    public void onRootPicked(RootInfo root, boolean closeDrawer) {
        if(null == root){
            return;
        }

        mDocStack.root = root;
        mDocStack.clear();
        new PickRootTask(root).execute();

        if (closeDrawer) {
           // setRootsDrawerOpen(false);
        }
    }

    @Override
    public void onDocumentPicked(DocumentInfo doc) {
        mDocStack.push(doc);
        if (doc.isDirectory() || DocumentArchiveHelper.isSupportedArchiveType(doc.mimeType)) {
            onCurrentDirectoryChanged();
        }
    }

    public void onCurrentDirectoryChanged() {
        if(!Utils.isActivityAlive(ExplorerActivity.this)){
            return;
        }
        final FragmentManager fm = getFragmentManager();
        final RootInfo root = getCurrentRoot();
        DocumentInfo cwd = getCurrentDirectory();

        if(cwd == null){
            final Uri uri = DocumentsContract.buildDocumentUri(
                    root.getAuthority(), root.getDocumentId());
            DocumentInfo result;
            try {
                result = DocumentInfo.fromUri(getContentResolver(), uri);
                if (result != null) {
                    cwd = result;
                }
            } catch (FileNotFoundException e) {
                //
            }
        }
        if (root.isApp()) {
            AppsFragment.show(fm, root, cwd);
        } else if (root.isLibraryMedia()){
            MediasFragment.show(fm, root, cwd);
        } else if (RootInfo.isStorage(root)) {
            StorageFragment.show(fm, root, cwd);
        }

        final RootsFragment roots = RootsFragment.get(fm);
        if (roots != null) {
            roots.onCurrentRootChanged();
        }
    }

    @Override
    public void onBackPressed() {
        final int size = mDocStack.size();
        if (size > 1) {
            mDocStack.pop();
            onCurrentDirectoryChanged();
        } else if (size == 1) {
            // open root drawer once we can capture back key
            if (null != mParentRoot) {
                onRootPicked(mParentRoot, true);
                mParentRoot = null;
                return;
            }
            super.onBackPressed();
        } else {
            if (null != mParentRoot) {
                onRootPicked(mParentRoot, true);
                mParentRoot = null;
                return;
            }
            super.onBackPressed();
        }
    }

    private class PickRootTask extends AsyncTask<Void, Void, DocumentInfo> {
        private RootInfo mRoot;

        PickRootTask(RootInfo root) {
            mRoot = root;
        }

        @Override
        protected DocumentInfo doInBackground(Void... params) {
            try {
                final Uri uri = DocumentsContract.buildDocumentUri(
                        mRoot.getAuthority(), mRoot.getDocumentId());
                return DocumentInfo.fromUri(getContentResolver(), uri);
            } catch (FileNotFoundException e) {
                Log.w(TAG, "Failed to find root", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(DocumentInfo result) {
            if(!Utils.isActivityAlive(ExplorerActivity.this)){
                return;
            }
            mDocStack.push(result);
            onCurrentDirectoryChanged();
        }
    }
}
