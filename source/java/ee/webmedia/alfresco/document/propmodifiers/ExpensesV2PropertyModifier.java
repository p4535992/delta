<<<<<<< HEAD
package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.service.DocumentService.PropertiesModifierCallback;

public class ExpensesV2PropertyModifier extends PropertiesModifierCallback {

    @Override
    public QName getAspectName() {
        return DocumentSpecificModel.Aspects.EXPENSES_V2;
    }

    @Override
    public void doWithProperties(Map<QName, Serializable> properties) {
        @SuppressWarnings("unchecked")
        List<Double> expenses = (List<Double>) properties.get(DocumentSpecificModel.Props.EXPECTED_EXPENSE_SUM);
        BigDecimal sum = new BigDecimal("0.0");
        if (expenses != null) {
            for (Double expense : expenses) {
                sum = sum.add(BigDecimal.valueOf(expense));
            }
        }

        properties.put(DocumentSpecificModel.Props.EXPENSES_TOTAL_SUM, sum.doubleValue());
    }

}
=======
package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.service.DocumentService.PropertiesModifierCallback;

public class ExpensesV2PropertyModifier extends PropertiesModifierCallback {

    @Override
    public QName getAspectName() {
        return DocumentSpecificModel.Aspects.EXPENSES_V2;
    }

    @Override
    public void doWithProperties(Map<QName, Serializable> properties) {
        @SuppressWarnings("unchecked")
        List<Double> expenses = (List<Double>) properties.get(DocumentSpecificModel.Props.EXPECTED_EXPENSE_SUM);
        BigDecimal sum = new BigDecimal("0.0");
        if (expenses != null) {
            for (Double expense : expenses) {
                sum = sum.add(BigDecimal.valueOf(expense));
            }
        }

        properties.put(DocumentSpecificModel.Props.EXPENSES_TOTAL_SUM, sum.doubleValue());
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
