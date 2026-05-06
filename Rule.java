package khModel;

/**
 * Decision rules for the KH model.
 *
 *  ATTRACTIVE  - p1: prefer the most attractive partner
 *  SIMILAR     - p2: prefer the most similar partner
 *  MIXED       - p3: 50/50 average of p1 and p2
 *  FRUSTRATION - p4: start with p1, gradually shift to p2 as frustration grows
 *  WEIGHTED    - p5 (extra credit): choosiness-weighted blend of p1 and p2;
 *                high choosiness -> near-pure p1, low choosiness -> near-pure p2
 */
public enum Rule {
    ATTRACTIVE,
    SIMILAR,
    MIXED,
    FRUSTRATION,
    WEIGHTED
}