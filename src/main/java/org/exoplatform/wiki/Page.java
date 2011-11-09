package org.exoplatform.wiki;

import java.util.HashSet;
import java.util.Map;

public class Page {

    public String pageId;
    public String pageName;
    public String pageParentId;
    public WikiSpace space;
    public Page pageParent;

    public HashSet<String> comments = new HashSet<String>();
    public HashSet<String> attachments = new HashSet<String>();
    public HashSet<Page> childPages = new HashSet<Page>();
    public Map<String, Integer> macrosMap;
    // Body size in ko
    public long bodySize;

    public Page(WikiSpace space, String pageId, String pageName, String pageParentId) {
        this.space = space;
        this.pageId = pageId;
        this.pageName = pageName;
        this.pageParentId = pageParentId;
    }

    public void addComment(String commentId) {
        comments.add(commentId);
    }

    public void addAttachement(String attachmentId) {
        attachments.add(attachmentId);
    }

    public void setPageParent(Page pageParent) {
        this.pageParent = pageParent;
    }

    public void registerChild(Page page) {
        childPages.add(page);
    }

    public String getPath() {
        String path = pageName;
        if (pageParent != null)
            path = pageParent.getPath() + "/" + path;

        //TODO Store once computed to avoid multiple calls

        return path;
    }

}