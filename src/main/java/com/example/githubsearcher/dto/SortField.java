package com.example.githubsearcher.dto;

/**
 * Supported sort fields with safe mappings between client inputs,
 * GitHub API parameters, and database entity properties.
 */
public enum SortField {
    STARS("stars", "stars"),
    FORKS("forks", "forks"),
    UPDATED("updated", "lastUpdated");

    private final String clientValue;
    private final String databaseProperty;

    SortField(String clientValue, String databaseProperty) {
        this.clientValue = clientValue;
        this.databaseProperty = databaseProperty;
    }

    public String getClientValue() {
        return clientValue;
    }

    public String getDatabaseProperty() {
        return databaseProperty;
    }

    /**
     * Resolves a sort field from user input or throws an IllegalArgumentException if unsupported.
     */
    public static SortField fromString(String value) {
        if (value == null || value.isBlank()) {
            return STARS;
        }
        String trimmed = value.trim().toLowerCase();
        for (SortField field : values()) {
            if (field.clientValue.equalsIgnoreCase(trimmed) || field.databaseProperty.equalsIgnoreCase(trimmed)) {
                return field;
            }
        }
        throw new IllegalArgumentException(
                "Invalid sort parameter '" + value + "'. Supported values are: stars, forks, updated"
        );
    }
}

