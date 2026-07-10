package su26.uml.be.enums;

/**
 * Numeric usage limits attached to a Plan, stored one row per key in {@code plan_features}.
 * A value of {@code -1} means unlimited.
 */
public enum PlanFeatureKey {
    MAX_PROJECTS,
    MAX_DIAGRAMS,
    AI_QUERIES,
    MAX_COLLABORATORS
}
