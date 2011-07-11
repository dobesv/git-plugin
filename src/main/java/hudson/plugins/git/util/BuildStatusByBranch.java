package hudson.plugins.git.util;

import hudson.model.Action;
import hudson.model.HealthReport;
import hudson.model.Messages;
import hudson.model.Result;
import hudson.model.Job;
import hudson.model.Run;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.time.DateUtils;
import org.jvnet.localizer.Localizable;

/**
 * Project-level action that displays a table with build status separately
 * for each branch.
 */
public class BuildStatusByBranch implements Action {
    public final TreeMap<String,BranchBuildStatus> map = new TreeMap<String,BranchBuildStatus>();

    public String getDisplayName() {
        return "Per-Branch Build Status";
    }
    public String getIconFileName() {
        return "/plugin/git/icons/git-32x32.png";
    }
    public String getUrlName() {
        return "branches";
    }

    public static class BranchBuildStatus {
    	public final String branchName;
    	public final long lastSuccessTimestamp;
    	public final int lastSuccessBuildNumber;
    	public final long lastFailureTimestamp;
    	public final int lastFailureBuildNumber;
    	public final Result lastResult; 
    	public final long lastDuration;
    	public final BitSet buildStatusHistory; // 1 is success, 0 is failure, used to compute branch "health"
    	
		public BranchBuildStatus(String branchName, long lastSuccessTimestamp,
				int lastSuccessBuildNumber, long lastFailureTimestamp,
				int lastFailureBuildNumber, Result lastResult,
				long lastDuration, BitSet buildStatusHistory) {
			super();
			this.branchName = branchName;
			this.lastSuccessTimestamp = lastSuccessTimestamp;
			this.lastSuccessBuildNumber = lastSuccessBuildNumber;
			this.lastFailureTimestamp = lastFailureTimestamp;
			this.lastFailureBuildNumber = lastFailureBuildNumber;
			this.lastResult = lastResult;
			this.lastDuration = lastDuration;
			this.buildStatusHistory = buildStatusHistory;
		}

		public BranchBuildStatus(String branchName, Run<?, ?> lastBuild) {
			boolean success = lastBuild.getResult().isBetterThan(Result.FAILURE);
			this.branchName = branchName;
			this.lastSuccessTimestamp = success ? lastBuild.getTimeInMillis() : 0L;
			this.lastSuccessBuildNumber = success ? lastBuild.getNumber() : 0;
			this.lastFailureTimestamp = success ? 0L : lastBuild.getTimeInMillis();
			this.lastFailureBuildNumber = success ? 0 : lastBuild.getNumber();
			this.lastResult = lastBuild.getResult();
			this.lastDuration = lastBuild.getDuration();
			this.buildStatusHistory = new BitSet(1);
			if(success)
				buildStatusHistory.set(0);
		}

		/**
		 * Return a new status object that includes data from the given build.
		 * 
		 * This assumes that the given build was building the branch this object
		 * is storing status information for.
		 */
		public BranchBuildStatus merge(Run<?, ?> lastBuild) {
			boolean success = lastBuild.getResult().isBetterThan(Result.FAILURE);
			long newLastSuccessTimestamp = lastSuccessTimestamp;
			int newLastSuccessBuildNumber = lastSuccessBuildNumber;
			int newLastFailureBuildNumber = lastFailureBuildNumber;
			long newLastFailureTimestamp = lastFailureTimestamp;
			if(success) {
				if(lastBuild.number <= lastSuccessBuildNumber)
					return this; // already incorporated
				newLastSuccessTimestamp = lastBuild.getTimeInMillis();
				newLastSuccessBuildNumber = lastBuild.getNumber();
			} else {
				if(lastBuild.number <= lastFailureBuildNumber)
					return this; // already incorporated
				newLastFailureBuildNumber = lastFailureBuildNumber;
				newLastFailureTimestamp = lastFailureTimestamp;
			}
			return new BranchBuildStatus(branchName,
					newLastSuccessTimestamp, newLastSuccessBuildNumber,
					newLastFailureTimestamp, newLastFailureBuildNumber,
					lastBuild.getResult(), lastBuild.getDuration(),
					pushToHistory(success));
		}

		private BitSet pushToHistory(boolean success) {
			BitSet newHistory = new BitSet(Math.min(5, buildStatusHistory.length()));
			for(int i=1; i < newHistory.length(); i++) newHistory.set(i, buildStatusHistory.get(i-1));
			newHistory.set(0, success);
			return newHistory;
		}
    	
		/**
		 * Health report, but for this branch only.
		 * 
		 * TODO Currently doesn't do anything with plugin health reports like the project 
		 *      does, so this won't show bad weather if tests are failing
		 */
    	public HealthReport getHealthReport() {
    		int successes = buildStatusHistory.cardinality();
    		int totalCount = buildStatusHistory.length();
    		int failCount = totalCount - successes;
            if (totalCount > 0) {
                int score = (int) ((100.0 * (totalCount - failCount)) / totalCount);

                Localizable description;
                if (failCount == 0) {
                    description = Messages._Job_NoRecentBuildFailed();
                } else if (totalCount == failCount) {
                    // this should catch the case where totalCount == 1
                    // as failCount must be between 0 and totalCount
                    // and we can't get here if failCount == 0
                    description = Messages._Job_AllRecentBuildFailed();
                } else {
                    description = Messages._Job_NOfMFailed(failCount, totalCount);
                }
                return new HealthReport(score, Messages._Job_BuildStability(description));
            }
            return null;
    	}

		public String getBranchName() {
			return branchName;
		}

		public long getLastSuccessTimestamp() {
			return lastSuccessTimestamp;
		}

		public int getLastSuccessBuildNumber() {
			return lastSuccessBuildNumber;
		}

		public long getLastFailureTimestamp() {
			return lastFailureTimestamp;
		}

		public int getLastFailureBuildNumber() {
			return lastFailureBuildNumber;
		}

		public Result getLastResult() {
			return lastResult;
		}

		public long getLastDuration() {
			return lastDuration;
		}

		public BitSet getBuildStatusHistory() {
			return buildStatusHistory;
		}
    }
	public BuildStatusByBranch update(Job<?,?> project, Run<?,?> r, List<BuildData> buildDataList) {
		for(BuildData bd : buildDataList) {
			for(Entry<String, Build> entry : bd.getBuildsByBranchName().entrySet()) {
				String branchName = entry.getKey();
				int buildNumber = entry.getValue().getBuildNumber();
				Run<?,?> lastBuild = project.getBuildByNumber(buildNumber);
				if(lastBuild == null)
					continue; // Skip builds that are too old for the specified time range
				BranchBuildStatus branchStatus = map.get(branchName);
				BranchBuildStatus newStatus;
				if(branchStatus != null) newStatus = branchStatus.merge(lastBuild);
				else newStatus = new BranchBuildStatus(branchName, lastBuild);
				map.put(branchName, newStatus);
			}
		}
		return this;
	}
	
	/**
	 * Return a collection of builds which were built "recently enough" based on the 
	 * "days to keep branch status in view" setting.
	 */
	public Collection<BranchBuildStatus> buildsToDisplay() {
		return map.values();
	}
	
	public int numberOfBranchesWithBuildStatus() {
		return map.size();
	}
}
