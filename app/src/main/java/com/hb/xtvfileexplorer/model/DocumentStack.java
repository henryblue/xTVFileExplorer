package com.hb.xtvfileexplorer.model;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.LinkedList;


public class DocumentStack extends LinkedList<DocumentInfo> {

    public RootInfo root;

    public String getTitle() {
        if (size() == 1 && root != null) {
            return root.title;
        } else if (size() > 1) {
            return peek().displayName;
        } else {
            return null;
        }
    }

    public boolean isRecents() {
        return size() == 0;
    }

    public void updateRoot(Collection<RootInfo> matchingRoots) throws FileNotFoundException {
        for (RootInfo root : matchingRoots) {
            if (root.equals(this.root)) {
                this.root = root;
                return;
            }
        }
        throw new FileNotFoundException("Failed to find matching root for " + root);
    }


    /**
     * Build key that uniquely identifies this stack. It omits most of the raw
     * details included in {@link #(DataOutputStream)}, since they change
     * too regularly to be used as a key.
     */
    public String buildKey() {
        final StringBuilder builder = new StringBuilder();
        if (root != null) {
            builder.append(root.getAuthority()).append('#');
            builder.append(root.getRootId()).append('#');
        } else {
            builder.append("[null]").append('#');
        }
        for (DocumentInfo doc : this) {
            builder.append(doc.documentId).append('#');
        }
        return builder.toString();
    }
}
