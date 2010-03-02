package ee.webmedia.alfresco.substitute;

/**
 * @author Romet Aidla
 */
public class SubstitutionInfoHolder {
     private static ThreadLocal<SubstitutionInfo> holder = new ThreadLocal<SubstitutionInfo>();

    public static void setSubstitutionInfo(SubstitutionInfo substitutionInfo) {
        holder.set(substitutionInfo);
    }

    public static SubstitutionInfo getSubstitutionInfo() {
        SubstitutionInfo subInfo = holder.get();
        return (subInfo != null) ? subInfo : new SubstitutionInfo();
    }
}
