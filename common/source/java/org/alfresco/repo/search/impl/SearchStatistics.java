package org.alfresco.repo.search.impl;


public class SearchStatistics {

    public static Data getData() {
        return data.get();
    }

    public static boolean isEnabled() {
        return data.get() != null;
    }

    public static void enable() {
        data.set(new Data());
    }

    public static void disable() {
        data.set(null);
    }

    private static ThreadLocal<Data> data = new ThreadLocal<Data>();

    public static class Data {
        public long resultsAfterAcl = -1; // Set in AbstractSearchServiceImpl
        public long resultsBeforeAcl = -1; // Set in Hits
        public long luceneHitsTime = -1; // Set in Hits
        public long aclTime = -1; // Set in ACLEntryAfterInvocationProvider
        public long alfrescoSearchLayerOtherTime = -1; // Set in AbstractSearchServiceImpl
        public long nodeTypesTime = -1; // Set in AbstractSearchServiceImpl
        public long nodePropsTime = -1; // Set in AbstractSearchServiceImpl
        public long closeResultSetTime = -1; // Set in AbstractSearchServiceImpl
    }

}
