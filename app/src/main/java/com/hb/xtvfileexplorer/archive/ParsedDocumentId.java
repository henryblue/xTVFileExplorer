package com.hb.xtvfileexplorer.archive;

import android.support.annotation.RestrictTo;

import static android.support.annotation.RestrictTo.Scope.GROUP_ID;

@RestrictTo(GROUP_ID)
public class ParsedDocumentId {
    public final String mArchiveId;
    public final String mPath;

    public ParsedDocumentId(String archiveId, String path) {
        mArchiveId = archiveId;
        mPath = path;
    }

    static public ParsedDocumentId fromDocumentId(String documentId, char idDelimiter) {
        final int delimiterPosition = documentId.indexOf(idDelimiter);
        if (delimiterPosition == -1) {
            return new ParsedDocumentId(documentId, null);
        } else {
            return new ParsedDocumentId(documentId.substring(0, delimiterPosition),
                    documentId.substring((delimiterPosition + 1)));
        }
    }

    static public boolean hasPath(String documentId, char idDelimiter) {
        return documentId.indexOf(idDelimiter) != -1;
    }

    public String toDocumentId(char idDelimiter) {
        if (mPath == null) {
            return mArchiveId;
        } else {
            return mArchiveId + idDelimiter + mPath;
        }
    }
}
