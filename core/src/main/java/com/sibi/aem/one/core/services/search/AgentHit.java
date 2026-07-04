package com.sibi.aem.one.core.services.search;

/** Single agent search result hit. */
public class AgentHit {
    private final String path;
    private final String agentName;
    private final String agentId;
    private final String specialisation;
    private final double relevanceScore;

    public AgentHit(String path, String agentName, String agentId,
                    String specialisation, double relevanceScore) {
        this.path           = path;
        this.agentName      = agentName;
        this.agentId        = agentId;
        this.specialisation = specialisation;
        this.relevanceScore = relevanceScore;
    }

    public String getPath()           { return path; }
    public String getAgentName()      { return agentName; }
    public String getAgentId()        { return agentId; }
    public String getSpecialisation() { return specialisation; }
    public double getRelevanceScore() { return relevanceScore; }
}
