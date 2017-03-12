package hudson.plugins.svn_partial_release_mgr.api.model;

import java.util.HashSet;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class UserInput {

  private final Set<Long> includedRevisionsInRelease;
  private final boolean generatePartialPatch;
  private final boolean includePreviousPatchSources;
  private final boolean generateSourcePartialPatch;
  private final boolean generatePatchForEveryIssue;
  private final boolean isFastBuild;
  private final boolean isTestBuild;

  public UserInput(Set<Long> includedRevisionsInRelease,
                   boolean generatePartialPatch,
                   boolean generateSourcePartialPatch,
                   boolean generatePatchForEveryIssue,
                   boolean isFastBuild,
                   boolean isTestBuild,
                   boolean includePreviousPatchSources) {
    this.includedRevisionsInRelease = includedRevisionsInRelease;
    this.generatePartialPatch = generatePartialPatch;
    this.generateSourcePartialPatch = generateSourcePartialPatch;
    this.generatePatchForEveryIssue = generatePatchForEveryIssue;
    this.isFastBuild = isFastBuild;
    this.isTestBuild = isTestBuild;
    this.includePreviousPatchSources = includePreviousPatchSources;
  }

  public Set<Long> getIncludedRevisionsInRelease() {
    return includedRevisionsInRelease;
  }

  public boolean isGeneratePartialPatch() {
    return generatePartialPatch;
  }

  public boolean isGenerateSourcePartialPatch() {
    return generateSourcePartialPatch;
  }

  public boolean isGeneratePatchForEveryIssue() {
    return generatePatchForEveryIssue;
  }

  public boolean isFastBuild() {
    return isFastBuild;
  }

  public boolean isTestBuild() {
    return isTestBuild;
  }

  public boolean isIncludePreviousPatchSources() {
    return includePreviousPatchSources;
  }

  public JsonObject toJson() {
    JsonArrayBuilder deploymentRevisionIds = Json.createArrayBuilder();
    for (Long revisionId : includedRevisionsInRelease) {
      deploymentRevisionIds.add(revisionId);
    }
    return Json.createObjectBuilder()
        .add("generatePartialPatch", generatePartialPatch)
        .add("generateSourcePartialPatch", generateSourcePartialPatch)
        .add("generatePatchForEveryIssue", generatePatchForEveryIssue)
        .add("isFastBuild", isFastBuild)
        .add("isTestBuild", isTestBuild)
        .add("includePreviousPatchSources", includePreviousPatchSources)
        .add("deploymentRevisionIds", deploymentRevisionIds).build();
  }

  public static UserInput fromJson(JsonObject jsonObject) {
    boolean generatePartialPatch = jsonObject.getBoolean("generatePartialPatch");
    boolean generateSourcePartialPatch = jsonObject.getBoolean("generateSourcePartialPatch");
    boolean generatePatchForEveryIssue = jsonObject.getBoolean("generatePatchForEveryIssue");
    boolean isFastBuild = jsonObject.getBoolean("isFastBuild");
    boolean isTestBuild = jsonObject.getBoolean("isTestBuild");
    boolean includePreviousPatchSources = jsonObject.getBoolean("includePreviousPatchSources");
    JsonArray jsonArray = jsonObject.getJsonArray("deploymentRevisionIds");
    Set<Long> deploymentRevisionIds = new HashSet<>();
    for (int i = 0; i < jsonArray.size(); i++) {
      deploymentRevisionIds.add(jsonArray.getJsonNumber(i).longValue());
    }
    return new UserInput(deploymentRevisionIds, generatePartialPatch
        , generateSourcePartialPatch, generatePatchForEveryIssue,
        isFastBuild, isTestBuild,includePreviousPatchSources);
  }
}
