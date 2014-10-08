package ee.webmedia.alfresco.docadmin.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.utils.CollectionComparator;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Utility class that helps to compare two latest {@link ChildrenList} objects
 * FIXME DLSeadist - at the moment contains double check - may be removed when stable
 * TODO DLSeadist - when double check is removed then it could be generalized from ChildrenList<MetadataItem> to ChildrenList<BaseObject>
 */
public class MetadataItemCompareUtil {
    private static final Comparator<FieldGroup> FIELD_GROUP_COMPARATOR = getFieldGroupComparator();
    private static final Comparator<SeparatorLine> SEPARATOR_LINE_COMPARATOR_CHAIN = getSeparatorLineComparatorChain();
    private static final Comparator<Field> FIELD_COMPARATOR = getFieldComparator();
    private static final Comparator<FieldDefinition> FIELD_DEFINITION_COMPARATOR = getFieldDefinitionComparator();

    public static <M extends MetadataItem> boolean isClidrenListChanged(ChildrenList<M> savedMetadata, ChildrenList<M> unSavedMetadata) {
        if (unSavedMetadata.size() != savedMetadata.size()) {
            return true; // at least one field/fieldGroup/separatorLine is added or removed
        }
        Set<NodeRef> savedNodeRefs = new HashSet<NodeRef>();
        for (MetadataItem metadataItem : savedMetadata) {
            Assert.isTrue(metadataItem.isSaved());
            savedNodeRefs.add(metadataItem.getNodeRef());
        }
        Map<NodeRef, MetadataItem> unSavedNodeRefOriginals = new HashMap<NodeRef, MetadataItem>();
        for (MetadataItem metadataItem : unSavedMetadata) {
            Assert.isTrue(metadataItem.isUnsaved());
            NodeRef clonedFromNodeRef = metadataItem.getCopyFromPreviousDocTypeVersion();
            if (clonedFromNodeRef == null) {
                return true; // new metadataItem is added
            }
            unSavedNodeRefOriginals.put(clonedFromNodeRef, metadataItem);
        }
        Collection<NodeRef> unSavedNodeRefOriginalRefs = unSavedNodeRefOriginals.keySet();
        if (!savedNodeRefs.containsAll(unSavedNodeRefOriginalRefs)) {
            return true; // added metadataItems
        }
        if (!unSavedNodeRefOriginalRefs.containsAll(savedNodeRefs)) {
            return true; // removed metadataItems
        }
        // all items saved before are present in latestDocumentTypeVersion, check if contents are same
        for (MetadataItem savedMetadataItem : savedMetadata) {
            MetadataItem unSavedMetadataItem = unSavedNodeRefOriginals.get(savedMetadataItem.getNodeRef());
            // XXX objectEqual must be called first, because it sets property as empty list when property is missing
            boolean objectsEqual = objectsEqual(savedMetadataItem, unSavedMetadataItem); // FIXME DLSeadist double check - may be removed when stable
            boolean propsEqual = RepoUtil.propsEqual(savedMetadataItem.getNode().getProperties(), unSavedMetadataItem.getNode().getProperties());
            if (propsEqual && savedMetadataItem instanceof FieldGroup) {
                // maybe fields under fieldGroup have changed
                FieldGroup savedFieldGroup = (FieldGroup) savedMetadataItem;
                FieldGroup unSavedFieldGroup = (FieldGroup) unSavedMetadataItem;
                ChildrenList<Field> savedFields = savedFieldGroup.getMetadata();
                ChildrenList<Field> unSavedFields = unSavedFieldGroup.getMetadata();
                propsEqual = !isClidrenListChanged(savedFields, unSavedFields);
            }
            if (propsEqual != objectsEqual) {
                throw new RuntimeException("Unexpected: propsEqual=" + propsEqual + " objectsEqual=" + objectsEqual);
            }
            if (!propsEqual) {
                return true;
            }
        }
        return false;
    }

    private static boolean objectsEqual(MetadataItem savedMetadataItem, MetadataItem unSavedMetadataItem) {
        @SuppressWarnings("unchecked")
        Comparator<MetadataItem> comparator = (Comparator<MetadataItem>) getComparator(savedMetadataItem.getClass());
        return comparator.compare(savedMetadataItem, unSavedMetadataItem) == 0;
    }

    @SuppressWarnings("unchecked")
    private static <M extends MetadataItem> Comparator<M> getComparator(Class<M> clazz) {
        if (clazz == Field.class) {
            return (Comparator<M>) FIELD_COMPARATOR;
        }
        if (clazz == SeparatorLine.class) {
            return (Comparator<M>) SEPARATOR_LINE_COMPARATOR_CHAIN;
        }
        if (clazz == FieldGroup.class) {
            return (Comparator<M>) FIELD_GROUP_COMPARATOR;
        }
        if (clazz == FieldDefinition.class) {
            return (Comparator<M>) FIELD_DEFINITION_COMPARATOR;
        }
        throw new IllegalArgumentException("Comparing " + clazz + " not implemented");
    }

    private static ComparatorChain getMetadataItemComparatorChain() {
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<MetadataItem>() {
            @Override
            public Comparable<?> tr(MetadataItem input) {
                return input.getOrder();
            }
        }, new NullComparator()));
        return chain;
    }

    private static Comparator<SeparatorLine> getSeparatorLineComparatorChain() {
        // no additional field to compare
        return cast(getMetadataItemComparatorChain(), SeparatorLine.class);
    }

    private static ComparatorChain getFieldAndGroupBaseComparatorChain() {
        ComparatorChain chain = getMetadataItemComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldAndGroupBase>() {
            @Override
            public Comparable<?> tr(FieldAndGroupBase input) {
                return input.getName();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldAndGroupBase>() {
            @Override
            public Comparable<?> tr(FieldAndGroupBase input) {
                return input.isSystematic();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldAndGroupBase>() {
            @Override
            public Comparable<?> tr(FieldAndGroupBase input) {
                return input.getSystematicComment();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldAndGroupBase>() {
            @Override
            public Comparable<?> tr(FieldAndGroupBase input) {
                return input.getComment();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldAndGroupBase>() {
            @Override
            public Comparable<?> tr(FieldAndGroupBase input) {
                return input.isMandatoryForDoc();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldAndGroupBase>() {
            @Override
            public Comparable<?> tr(FieldAndGroupBase input) {
                return input.isRemovableFromSystematicDocType();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldAndGroupBase>() {
            @Override
            public Comparable<?> tr(FieldAndGroupBase input) {
                return input.isMandatoryForVol();
            }
        }, new NullComparator()));
        return chain;
    }

    private static Comparator<Field> getFieldComparator() {
        return cast(getFieldComparatorChain(), Field.class);
    }

    private static ComparatorChain getFieldComparatorChain() {
        ComparatorChain chain = getFieldAndGroupBaseComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.getFieldId();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.isMandatory();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.getFieldType();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.getChangeableIf();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.getDefaultValue();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.getClassificator();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.getClassificatorDefaultValue();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.isDefaultDateSysdate();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.isDefaultUserLoggedIn();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.isDefaultSelected();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.isOnlyInGroup();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.isMandatoryChangeable();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.isChangeableIfChangeable();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.isComboboxNotRelatedToClassificator();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.isRemovableFromSystematicFieldGroup();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Field>() {
            @Override
            public Comparable<?> tr(Field input) {
                return input.getMappingRestrictionEnum();
            }
        }, new NullComparator()));
        chain.addComparator(new Comparator<Field>() {

            @Override
            public int compare(Field field1, Field field2) {
                if (field1 != null && field2 != null) {
                    return compareLists(field1.getRelatedIncomingDecElement(), field2.getRelatedIncomingDecElement());
                }
                return compareFields(field1, field2);
            }
        });
        chain.addComparator(new Comparator<Field>() {

            @Override
            public int compare(Field field1, Field field2) {
                if (field1 != null && field2 != null) {
                    return compareLists(field1.getRelatedOutgoingDecElement(), field2.getRelatedOutgoingDecElement());
                }
                return compareFields(field1, field2);
            }

        });
        return chain;
    }

    private static int compareFields(Field field1, Field field2) {
        if (field1 == null && field2 == null) {
            return 0;
        }
        return field1 == null ? -1 : 1;
    }

    private static int compareLists(List<String> list1, List<String> list2) {
        if (list1 != null) {
            return new CollectionComparator<String>(list1).compareTo(list2);
        }
        return list2 == null ? 0 : 1;
    }

    private static Comparator<FieldGroup> getFieldGroupComparator() {
        ComparatorChain chain = getFieldAndGroupBaseComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldGroup>() {
            @Override
            public Comparable<?> tr(FieldGroup input) {
                return new CollectionComparator<String>(input.getFieldDefinitionIds());
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldGroup>() {
            @Override
            public Comparable<?> tr(FieldGroup input) {
                return input.getReadonlyFieldsName();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldGroup>() {
            @Override
            public Comparable<?> tr(FieldGroup input) {
                return input.getReadonlyFieldsRule();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldGroup>() {
            @Override
            public Comparable<?> tr(FieldGroup input) {
                return input.isShowInTwoColumns();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldGroup>() {
            @Override
            public Comparable<?> tr(FieldGroup input) {
                return input.getThesaurus();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldGroup>() {
            @Override
            public Comparable<?> tr(FieldGroup input) {
                return input.isReadonlyFieldsNameChangeable();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldGroup>() {
            @Override
            public Comparable<?> tr(FieldGroup input) {
                return input.isReadonlyFieldsRuleChangeable();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldGroup>() {
            @Override
            public Comparable<?> tr(FieldGroup input) {
                return input.isShowInTwoColumnsChangeable();
            }
        }, new NullComparator()));

        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldGroup>() {
            @Override
            public Comparable<?> tr(FieldGroup input) {
                return new CollectionComparator<Field>(input.getFields(), FIELD_COMPARATOR);
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldGroup>() {
            @Override
            public Comparable<?> tr(FieldGroup input) {
                return input.isInapplicableForDoc();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldGroup>() {
            @Override
            public Comparable<?> tr(FieldGroup input) {
                return input.isInapplicableForVol();
            }
        }, new NullComparator()));
        return cast(chain, FieldGroup.class);
    }

    private static Comparator<FieldDefinition> getFieldDefinitionComparator() {
        ComparatorChain chain = getFieldComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
            @Override
            public Comparable<?> tr(FieldDefinition input) {
                return new CollectionComparator<String>(input.getDocTypes());
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
            @Override
            public Comparable<?> tr(FieldDefinition input) {
                return input.isParameterInDocSearch();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
            @Override
            public Comparable<?> tr(FieldDefinition input) {
                return input.isParameterInVolSearch();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
            @Override
            public Comparable<?> tr(FieldDefinition input) {
                return input.getParameterOrderInDocSearch();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
            @Override
            public Comparable<?> tr(FieldDefinition input) {
                return input.getParameterOrderInVolSearch();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
            @Override
            public Comparable<?> tr(FieldDefinition input) {
                return new CollectionComparator<String>(input.getVolTypes());
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
            @Override
            public Comparable<?> tr(FieldDefinition input) {
                return input.isFixedParameterInDocSearch();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
            @Override
            public Comparable<?> tr(FieldDefinition input) {
                return input.isFixedParameterInVolSearch();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
            @Override
            public Comparable<?> tr(FieldDefinition input) {
                return input.isInapplicableForDoc();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
            @Override
            public Comparable<?> tr(FieldDefinition input) {
                return input.isInapplicableForVol();
            }
        }, new NullComparator()));
        return cast(chain, FieldDefinition.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> cast(ComparatorChain chain, @SuppressWarnings("unused") Class<T> clazz) {
        return chain;
    }
}
