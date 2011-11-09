package org.exoplatform.wiki;

import java.util.HashMap;

public class WikiSpace {

    public String spaceId;
    public String spaceName;
    public String spaceKey;

    HashMap<String, Page> pages = new HashMap<String, Page>();

    public WikiSpace(String spaceId, String spaceKey, String spaceName) {
        this.spaceId = spaceId;
        this.spaceName = spaceName;
        this.spaceKey = spaceKey;

    }

    public void registerPage(Page page) {
        pages.put(page.pageId, page);
    }
}