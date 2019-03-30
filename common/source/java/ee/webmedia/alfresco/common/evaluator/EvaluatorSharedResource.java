package ee.webmedia.alfresco.common.evaluator;

import java.io.Serializable;

import ee.webmedia.alfresco.common.web.BeanHelper;

public abstract class EvaluatorSharedResource<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = 1L;

    protected T t;
    private Boolean editMode;
    protected Boolean favourite;

    public T getObject() {
        return t;
    }

    public void setObject(T t) {
        if (this.t == null) {
            this.t = t;
        }
    }

    public boolean isInEditMode() {
        if (editMode == null) {
            editMode = BeanHelper.getDocumentDialogHelperBean().isInEditMode();
        }
        return editMode;
    }

    public abstract boolean isFavourite();

}
