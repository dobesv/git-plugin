package hudson.plugins.git.util;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Job;
import hudson.model.Run;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class BuildStatusByBranchProperty extends JobProperty<Job<?,?>> {

	private BuildStatusByBranch action;

	@DataBoundConstructor
	public BuildStatusByBranchProperty(boolean enabled, int daysToKeepBranchStatusInView) {
		super();
	}
	
	@Override
	public boolean perform(AbstractBuild<?, ?> r, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		List<BuildData> buildDataList = r.getActions(BuildData.class);
		if(!buildDataList.isEmpty()) {
			this.action = new BuildStatusByBranch().update(r.getParent(), r, buildDataList);
		}
		return super.perform(r, launcher, listener);
	}
	@Override
	public Collection<? extends Action> getJobActions(Job<?,?> job) {
		if(action == null) {
			try {
				perform((AbstractBuild<?, ?>)job.getBuilds().get(0), null, null);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(action != null && action.numberOfBranchesWithBuildStatus() > 1)
			return Collections.singleton(action);
		else
			return Collections.emptySet();
	}
	
    @Extension
	public static class DescriptorImpl extends JobPropertyDescriptor {
    	@Override
    	public String getDisplayName() {
    		return "Per-branch build status";
    	}
    	
    	@Override
    	public boolean isApplicable(Class<? extends Job> jobType) {
    		return true;
    	}
    }
    
}
