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

	public final boolean enabled;
	public final int daysToKeepBranchStatusInView;
	
	@DataBoundConstructor
	public BuildStatusByBranchProperty(boolean enabled, int daysToKeepBranchStatusInView) {
		super();
		this.enabled = enabled;
		this.daysToKeepBranchStatusInView = daysToKeepBranchStatusInView;
	}
	
	public int getDaysToKeepBranchStatusInView() {
		return daysToKeepBranchStatusInView;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	@Override
	public boolean perform(AbstractBuild<?, ?> r, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		List<BuildData> buildDataList = r.getActions(BuildData.class);
		if(!buildDataList.isEmpty())
			this.action = new BuildStatusByBranch().update(r.getParent(), r, buildDataList);
		return super.perform(r, launcher, listener);
	}
	@Override
	public Collection<? extends Action> getJobActions(Job<?,?> job) {
//		Run<?,?> r = job.getBuilds().get(0);
//		List<BuildData> buildDataList = r.getActions(BuildData.class);
//		Job<?,?> project = r.getParent();
//		BuildStatusByBranch action = project.getAction(BuildStatusByBranch.class);
////		if(doesNotHaveMoreThenOneBranch(buildDataList)) {
////			return Collections.emptySet();
////		}
//		if(action != null) {
//			action.update(project, r, buildDataList);
//		} else {
//			action = new BuildStatusByBranch().update(project, r, buildDataList);
//		}
		if(action == null) {
			try {
				perform((AbstractBuild<?, ?>)job.getBuilds().get(0), null, null);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(action != null)
			return Collections.singleton(action);
		else
			return Collections.emptySet();
	}
	
//	private boolean doesNotHaveMoreThenOneBranch(List<BuildData> buildDataList) {
//		TreeSet<String> allBranches = new TreeSet<String>();
//		for(BuildData bd : buildDataList) {
//			allBranches.addAll(bd.getBuildsByBranchName().keySet());
//		}
//		boolean hasLessThenTwoBranches = allBranches.size() <= 1;
//		return hasLessThenTwoBranches;
//	}
	
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
    	@Override
    	public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData)
    		throws hudson.model.Descriptor.FormException {
    		return super.newInstance(req, formData);
    	}
    	
    	@Override
    	public boolean configure(StaplerRequest req, JSONObject json)
    		throws hudson.model.Descriptor.FormException {
    		return super.configure(req, json);
    	}
    }
    
}
