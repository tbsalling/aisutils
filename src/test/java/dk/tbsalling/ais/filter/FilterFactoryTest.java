package dk.tbsalling.ais.filter;

import dk.tbsalling.aismessages.ais.messages.AISMessage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class FilterFactoryTest {

    @Test
    void newExpressionFilter_withValidExpression_createsFilter() {
        // Arrange
        String expression = "msgid=1";

        // Act
        Predicate<AISMessage> filter = FilterFactory.newExpressionFilter(expression);

        // Assert
        assertNotNull(filter);
        assertInstanceOf(ExpressionFilter.class, filter);
    }

    @Test
    void newExpressionFilter_withComplexExpression_createsFilter() {
        // Arrange
        String expression = "msgid=1 and mmsi=123456789";

        // Act
        Predicate<AISMessage> filter = FilterFactory.newExpressionFilter(expression);

        // Assert
        assertNotNull(filter);
        assertInstanceOf(ExpressionFilter.class, filter);
    }

    @Test
    void newDoubletFilter_withoutParameters_createsFilter() {
        // Arrange & Act
        Predicate<AISMessage> filter = FilterFactory.newDoubletFilter();

        // Assert
        assertNotNull(filter);
        assertInstanceOf(DoubletFilter.class, filter);
    }

    @Test
    void newDoubletFilter_withDurationAndUnit_createsFilter() {
        // Arrange
        long duration = 200;
        TimeUnit unit = TimeUnit.MILLISECONDS;

        // Act
        Predicate<AISMessage> filter = FilterFactory.newDoubletFilter(duration, unit);

        // Assert
        assertNotNull(filter);
        assertInstanceOf(DoubletFilter.class, filter);
    }

    @Test
    void newDoubletFilter_withSecondsUnit_createsFilter() {
        // Arrange
        long duration = 5;
        TimeUnit unit = TimeUnit.SECONDS;

        // Act
        Predicate<AISMessage> filter = FilterFactory.newDoubletFilter(duration, unit);

        // Assert
        assertNotNull(filter);
        assertInstanceOf(DoubletFilter.class, filter);
    }
}
