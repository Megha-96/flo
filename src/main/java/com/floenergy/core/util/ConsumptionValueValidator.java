package com.floenergy.core.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ConsumptionValueValidator {


    public static Optional<BigDecimal> parseConsumption(
            String value,
            int lineNumber,
            int intervalNumber,
            List<String> errors
    ) {
        if (value == null || value.isBlank()) {
            errors.add("Missing consumption value at line " + lineNumber +
                    ", interval " + intervalNumber);
            return Optional.empty();
        }

        if (value.contains("e") || value.contains("E")) {
            errors.add("Exponential consumption value is not allowed at line " + lineNumber +
                    ", interval " + intervalNumber + ": " + value);
            return Optional.empty();
        }

        try {
            BigDecimal consumption = new BigDecimal(value);

            if (consumption.compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Negative consumption value is not allowed at line " + lineNumber +
                        ", interval " + intervalNumber + ": " + value);
                return Optional.empty();
            }

            return Optional.of(consumption);

        } catch (NumberFormatException e) {
            errors.add("Invalid numeric consumption value at line " + lineNumber +
                    ", interval " + intervalNumber + ": " + value);
            return Optional.empty();
        }
    }
}
