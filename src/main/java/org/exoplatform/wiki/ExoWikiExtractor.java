package org.exoplatform.wiki;

import com.mysql.jdbc.Driver;

import java.sql.*;
import java.util.*;

/**
 */
public class ExoWikiExtractor {

    Connection connect;
    Statement statement;


    static final String CONTENT_TYPE_PAGE = "PAGE";
    static final String CONTENT_TYPE_BLOG = "BLOGPOST";
    static final String CONTENT_TYPE_COMMENT = "COMMENT";

    HashMap<String, WikiSpace> spacesMap = new HashMap<String, WikiSpace>();
    HashMap<String, WikiSpace> pagesMap = new HashMap<String, WikiSpace>();
    HashMap<String, Integer> macrosMap = new HashMap<String, Integer>();

    public static void main(String args[]) throws Exception {
        ExoWikiExtractor exoWikiExtractor = new ExoWikiExtractor();
        exoWikiExtractor.init();

        exoWikiExtractor.scanAttachments();
        exoWikiExtractor.scanContentForMacros();

        //Dump
        exoWikiExtractor.dumpPageCountPerSpaces();

    }



    public void init() throws Exception {
        Class.forName(Driver.class.getName());
        connect = DriverManager.getConnection("jdbc:mysql://localhost/confluence-exo", "root", "exo");

        loadSpaces();
        loadPages();
    }

    public void loadSpaces() throws Exception {
        System.out.println("Loading spacesMap ...");
        String query = "SELECT SPACEID, SPACEKEY, SPACENAME FROM SPACES";
        ResultSet resultSet = executeStatement(query);

        while (resultSet.next()) {
            String spaceId = resultSet.getString("SPACEID");
            String spaceName = resultSet.getString("SPACENAME");
            String spaceKey = resultSet.getString("SPACEKEY");
            WikiSpace space = new WikiSpace(spaceId, spaceKey, spaceName);
            spacesMap.put(spaceId, space);
        }

        System.out.println("Registered " + spacesMap.size() + " spaces.");
    }

    public String getPageQueryPart(String contentType) {
        return "CONTENT.CONTENTTYPE = '" + contentType + "' AND CONTENT.PREVVER IS NULL AND CONTENT.CONTENT_STATUS = 'current'";
    }

    public void loadPages() throws Exception {
        System.out.println("Loading pagesMap ...");

        String query = "SELECT CONTENT.SPACEID, CONTENT.CONTENTID, CONTENT.TITLE, CONTENT.PARENTID FROM CONTENT WHERE " + getPageQueryPart(CONTENT_TYPE_PAGE) + ";";

        ResultSet resultSet = executeStatement(query);

        int count = 0;
        while (resultSet.next()) {

            String pageId = resultSet.getString("CONTENTID");
            String spaceId = resultSet.getString("SPACEID");
            String pageName = resultSet.getString("TITLE");
            String pageParentId = resultSet.getString("PARENTID");

            WikiSpace space = spacesMap.get(spaceId);
            Page page = new Page(space, pageId, pageName, pageParentId);

            if (space != null) {
                space.registerPage(page);
            } else {
                System.err.println("Not found space " + spaceId + " for page " + pageId);
            }

            count++;
        }
        System.out.println("Registered " + count + " pages.");

        System.out.println("Processing pagesMap ...");
        for (WikiSpace space : spacesMap.values()) {
            for (Page page : space.pages.values()) {
                if (page.pageParentId != null) {
                    page.pageParent = space.pages.get(page.pageParentId);
                    if (page.pageParent != null) {
                        page.pageParent.registerChild(page);
                    } else {
                        System.err.println("Page " + page.pageParentId + " not found in space " + space.spaceKey);
                    }
                }
            }
        }
        System.out.println("Processed pagesMaps");

    }

    private ResultSet executeStatement(String query) throws ClassNotFoundException, SQLException {
        statement = connect.createStatement();
        return statement.executeQuery(query);
    }


    public static boolean registerAndCountMacro(Map<String, Integer> macroMap, String macro) {
        boolean newMacro = false;

        if (macroMap == null)
            return false;

        Integer count = macroMap.get(macro);
        if (count == null) {
            count = new Integer(0);
            newMacro = true;
        }
        count++;
        macroMap.put(macro, count);
        return newMacro;
    }

    public Page getPage(String pageId) {
        for (WikiSpace space : spacesMap.values()) {
            Page page = space.pages.get(pageId);
            if (page != null)
                return page;
        }
        return null;
    }

    private void scanAttachments() {
        String query = "SELECT ATTACHMENTS.ATTACHMENTID, CONTENT.CONTENTID FROM ATTACHMENTS,CONTENT WHERE ATTACHMENTS.PAGEID = CONTENT.CONTENTID AND " + getPageQueryPart(CONTENT_TYPE_PAGE) + ";";
        //ResultSet resultSet = executeStatement(query);
    }

    public void dumpPageCountPerSpaces() {
        //All pagesMap should be processed before printing the path
        System.out.println("Dump pagesMap ...");
        System.out.println("SpaceKey,PageCount,SumPageKo");
        for (WikiSpace space : spacesMap.values()) {
            long totalSizeKo = 0;
            Collection<Page> values = space.pages.values();
            for (Page page : values) {
                totalSizeKo += page.bodySize;
            }
            System.out.println(space.spaceKey + "," + values.size() + "," + totalSizeKo);

        }

    }

    public void scanContentForMacros() throws Exception {
        System.out.println("Scan pagesMap content ...");

        //TODO List used macros
        int start = 0;
        int realPageCount = 0;
        int lastCount = 1000;
//        while (lastCount == 1000) {

            String query = "SELECT BODYCONTENT.CONTENTID, BODYCONTENT.BODY FROM BODYCONTENT, CONTENT WHERE BODYCONTENT.CONTENTID = CONTENT.CONTENTID AND CONTENT.CONTENTTYPE = '" + CONTENT_TYPE_PAGE + "' AND CONTENT.PREVVER IS NULL AND CONTENT.CONTENT_STATUS = 'current';";
            ResultSet resultSet = executeStatement(query);

            int count = 0;
            while (resultSet.next()) {
                String pageid = resultSet.getString("CONTENTID");
                Page page = getPage(pageid);
                if (page != null) {
                    String body = resultSet.getString("BODY");
                    page.bodySize = body.length() / 1024;
                    Map<String, Integer> pageMacrosMap = extractMacro(macrosMap, body);
                    page.macrosMap = pageMacrosMap;
                    realPageCount++;
                }

                count++;
            }

            lastCount = count;
            System.out.println("Parsed #" + (start+lastCount) + " - total scanned " + realPageCount);
    //    }
        System.out.println("Scan pagesMap content done (" + (start+lastCount) + " pages scanned)." );

        List<String> macros = new ArrayList<String>(macrosMap.keySet());
        System.out.println("macro,count");
        Collections.sort(macros);
        for(String macro : macros){
            Integer macroCount = macrosMap.get(macro);
            System.out.println("'" + macro + "@" + macroCount);
        }

    }

    public static Map<String, Integer> extractMacro(Map<String, Integer> macrosMap, String body) {
        Map<String, Integer> foundMacros = new HashMap<String, Integer>();

        //Remove {code}content{code}
        body = removeBlocks("code", body);
        body = removeBlocks("style", body);

        String macros[] = body.split("\\{");
        String previousPiece = ""; //To check for escaped macros
        for (String piece : macros) {
            if (piece.contains("}")) {
                //Avoid escaped macros
                if (!previousPiece.endsWith("\\")) {
                    //get content before }
                    String submacros[] = piece.split("}");
                    String extractedMacro = submacros.length > 0 ? submacros[0] : "";

                    //get content before :
                    submacros = extractedMacro.split(":");
                    extractedMacro = submacros.length > 0 ? submacros[0] : "";
                    extractedMacro = extractedMacro.trim();
                    extractedMacro = extractedMacro.replaceAll("\n", "");
                    extractedMacro = extractedMacro.replaceAll("\r", "");

                    registerAndCountMacro(foundMacros, extractedMacro);

                    if (registerAndCountMacro(macrosMap, extractedMacro)) {
                        //System.out.println("New Macro : " + extractedMacro);
                    }
                }
            }
            previousPiece = piece;
        }
        return foundMacros;
    }

    public void dumpAttachments() throws Exception {
    }

    public void dumpCommentCount() throws Exception {

        //TODO list comments per spacesMap
        //ALl comments => get page => get spacesMap
        String query = "SELECT SPACES.SPACEKEY, count(CONTENT.CONTENTID) FROM CONTENT WHERE CONTENT.CONTENTTYPE = '" + CONTENT_TYPE_BLOG + "' AND CONTENT.PREVVER IS NULL AND CONTENT.CONTENT_STATUS = 'current' ORDER BY count(CONTENT.CONTENTID);";

        ResultSet resultSet = executeStatement(query);

        System.out.println("Count " + CONTENT_TYPE_BLOG + " per Space :");

        while (resultSet.next()) {
            String pageid = resultSet.getString("PAGEID");
            String pageCount = resultSet.getString("count(CONTENT.CONTENTID)");
        }


    }


    static String removeBlocks(String tag, String result) {
        String[] split = result.split("\\{" + tag);
        boolean matching = true;
        boolean starting = true;
        String content = "";
        for (String piece : split) {
            if (matching) {
                if (!starting) {
                    //remove content until }
                    piece = piece.substring(piece.indexOf("}") + 1);
                }
                content += piece;
            } else {
                content += "{" + tag + "}";
            }
            matching = !matching;
            starting = false;
        }
        return content;
    }
}
